#!/usr/bin/env python3
"""Respaldo de SOLO LECTURA del almacén `capturas` y de las tablas errores_lector y capturas_ok.

Lo ejecuta el propietario en su equipo, con su llave `service_role` en la variable de entorno
SUPABASE_SERVICE_ROLE_KEY (nunca como argumento, nunca en un archivo del repo). Garantías que el
código impone (y que las pruebas comprueban):
  * Solo hace GET a /storage/v1/object/authenticated/... y /rest/v1/<tabla>, y POST a
    /storage/v1/object/list/... (listar). No existe ningún DELETE, PUT, PATCH ni POST de escritura.
  * La llave solo viaja al host del proyecto (https, *.supabase.co); no sigue redirecciones.
  * No imprime ni escribe la llave; los mensajes de error se limpian de ella.
  * La carpeta de salida debe ser nueva y quedar fuera de cualquier repositorio git.
Uso: python3 respaldar.py --salida RUTA            (respaldo)
     python3 respaldar.py --solo-listar            (prueba sin descargar ni escribir nada)
"""
import argparse
import base64
import hashlib
import http.client
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

import comun

AQUI = os.path.dirname(os.path.abspath(__file__))
PERMITIDO = (
    ("POST", "/storage/v1/object/list/"),
    ("GET", "/storage/v1/object/authenticated/"),
    ("GET", "/rest/v1/errores_lector"),
    ("GET", "/rest/v1/capturas_ok"),
)


class ErrorRespaldo(Exception):
    pass


def ahora():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def validar_clave(clave):
    """Acepta JWT con role=service_role o llaves `sb_secret_`. No devuelve ni imprime la llave."""
    if not clave:
        raise ErrorRespaldo("Falta la variable de entorno SUPABASE_SERVICE_ROLE_KEY.")
    if clave.startswith("sb_secret_"):
        return
    partes = clave.split(".")
    if len(partes) != 3:
        raise ErrorRespaldo("La llave no tiene formato de service_role (JWT o sb_secret_).")
    try:
        carga = partes[1] + "=" * (-len(partes[1]) % 4)
        rol = json.loads(base64.urlsafe_b64decode(carga)).get("role")
    except Exception:
        raise ErrorRespaldo("La llave no se pudo interpretar como JWT.")
    if rol != "service_role":
        raise ErrorRespaldo(f"La llave tiene rol «{rol}», no service_role. Usa la llave de servicio.")


def validar_url(url, permitir_local):
    u = urllib.parse.urlparse(url)
    if permitir_local:
        if u.scheme == "http" and u.hostname in ("127.0.0.1", "localhost"):
            return url.rstrip("/")
        raise ErrorRespaldo("--permitir-local solo acepta http://127.0.0.1 o http://localhost (pruebas).")
    esperado = f"{comun.PROYECTO_REF}.supabase.co"
    if u.scheme != "https" or u.hostname != esperado or u.path not in ("", "/") or u.port:
        raise ErrorRespaldo(f"La URL debe ser https://{esperado} (la llave solo se envía a ese host).")
    return f"https://{esperado}"


class _SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None  # → HTTPError 3xx: la llave nunca se reenvía a otro host


class Cliente:
    def __init__(self, base, clave, espera=1.0):
        self.base, self._clave, self.espera = base, clave, espera
        self.opener = urllib.request.build_opener(_SinRedireccion())
        self.peticiones = 0

    def _limpiar(self, texto):
        return str(texto).replace(self._clave, "***")

    def pedir(self, metodo, ruta, cuerpo=None, cabeceras=None, destino=None):
        """Devuelve (status, cabeceras, bytes|None). Con `destino` guarda el cuerpo en ese archivo
        y devuelve (status, cabeceras, (bytes_leidos, sha256))."""
        path = ruta.split("?")[0]
        if not any(metodo == m and path.startswith(p) for m, p in PERMITIDO):
            raise ErrorRespaldo(f"Petición no permitida por diseño: {metodo} {path}")
        ultimo = None
        for intento in range(4):
            if intento:
                time.sleep(self.espera * (2 ** (intento - 1)))
            req = urllib.request.Request(self.base + ruta, method=metodo)
            req.add_header("Authorization", "Bearer " + self._clave)
            req.add_header("apikey", self._clave)
            req.add_header("User-Agent", "checa-respaldo-e0/1")
            for k, v in (cabeceras or {}).items():
                req.add_header(k, v)
            if cuerpo is not None:
                req.data = json.dumps(cuerpo).encode("utf-8")
                req.add_header("Content-Type", "application/json")
            self.peticiones += 1
            try:
                with self.opener.open(req, timeout=60) as r:
                    if destino is None:
                        datos = r.read()
                        largo = r.headers.get("Content-Length")
                        if largo is not None and int(largo) != len(datos):
                            raise urllib.error.URLError("cuerpo incompleto")
                        return r.status, r.headers, datos
                    h, total, m5 = hashlib.sha256(), 0, hashlib.md5()
                    with open(destino, "wb") as f:
                        for bloque in iter(lambda: r.read(1 << 16), b""):
                            f.write(bloque)
                            h.update(bloque)
                            m5.update(bloque)
                            total += len(bloque)
                    largo = r.headers.get("Content-Length")
                    if largo is not None and int(largo) != total:
                        raise urllib.error.URLError("descarga incompleta")
                    return r.status, r.headers, (total, h.hexdigest(), m5.hexdigest())
            except urllib.error.HTTPError as e:
                if e.code in (429, 500, 502, 503, 504):
                    ultimo = f"HTTP {e.code}"
                    continue
                return e.code, e.headers, None
            except (urllib.error.URLError, http.client.HTTPException, ConnectionError, TimeoutError, OSError) as e:
                ultimo = self._limpiar(e)
                continue
        raise ErrorRespaldo(f"Sin respuesta tras 4 intentos ({ultimo}) en {metodo} {path}")


def listar(cli, bucket, prefijo=""):
    """Lista recursiva y paginada. Devuelve {nombre: {bytes, creado}}."""
    objetos, offset, vistos_pagina = {}, 0, 0
    while True:
        st, _, datos = cli.pedir("POST", f"/storage/v1/object/list/{bucket}", cuerpo={
            "prefix": prefijo, "limit": 100, "offset": offset, "sortBy": {"column": "name", "order": "asc"}})
        if st != 200:
            raise ErrorRespaldo(f"Listado falló: HTTP {st}")
        pagina = json.loads(datos)
        if not isinstance(pagina, list):
            raise ErrorRespaldo("Listado con formato inesperado.")
        if not pagina:
            break
        nuevos = 0
        for it in pagina:
            nombre = prefijo + it["name"]
            if it.get("id") is None:  # carpeta
                for k, v in listar(cli, bucket, nombre + "/").items():
                    if k in objetos:
                        raise ErrorRespaldo(f"Objeto repetido en el listado: {k}")
                    objetos[k] = v
                    nuevos += 1
                continue
            tam = (it.get("metadata") or {}).get("size")
            if not isinstance(tam, int):
                raise ErrorRespaldo(f"El listado no trae tamaño de {nombre}; no se puede verificar.")
            if nombre in objetos:
                raise ErrorRespaldo(f"Objeto repetido en el listado: {nombre}")
            objetos[nombre] = {"bytes": tam, "creado": it.get("created_at"),
                               "etag": str((it.get("metadata") or {}).get("eTag") or (it.get("metadata") or {}).get("etag") or "").strip('"').lower()}
            nuevos += 1
        if nuevos == 0:
            raise ErrorRespaldo("La paginación del listado no avanza.")
        offset += len(pagina)
    return objetos


def leer_tabla(cli, tabla, solo_contar=False):
    filas, ini, paso, total = [], 0, 1000, None
    while True:
        fin = ini if solo_contar else ini + paso - 1
        st, cab, datos = cli.pedir("GET", f"/rest/v1/{tabla}?select=*&order=id.asc", cabeceras={
            "Range-Unit": "items", "Range": f"{ini}-{fin}", "Prefer": "count=exact"})
        if st == 416 and ini == 0:
            return [] if not solo_contar else 0
        if st not in (200, 206):
            raise ErrorRespaldo(f"Lectura de {tabla} falló: HTTP {st}")
        cr = cab.get("Content-Range", "")
        try:
            total = int(cr.split("/")[1])
        except Exception:
            raise ErrorRespaldo(f"{tabla}: Content-Range ilegible ({cr!r}).")
        if solo_contar:
            return total
        pagina = json.loads(datos)
        filas += pagina
        if not pagina or len(filas) >= total:
            break
        ini += len(pagina)
    if len(filas) != total:
        raise ErrorRespaldo(f"{tabla}: se leyeron {len(filas)} filas pero el servidor reporta {total}.")
    ids = [f["id"] for f in filas]
    if ids != sorted(set(ids)):
        raise ErrorRespaldo(f"{tabla}: ids repetidos o fuera de orden.")
    return filas


def carpeta_en_repo(ruta):
    p = os.path.realpath(ruta)
    while True:
        if os.path.exists(os.path.join(p, ".git")):
            return True
        padre = os.path.dirname(p)
        if padre == p:
            return False
        p = padre


def escribir(ruta, datos):
    with open(ruta, "wb") as f:
        f.write(datos)
    os.chmod(ruta, 0o600)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--salida", help="carpeta NUEVA, fuera de cualquier repositorio git")
    ap.add_argument("--solo-listar", action="store_true", help="prueba: lista y cuenta, no descarga ni escribe")
    ap.add_argument("--url", default=f"https://{comun.PROYECTO_REF}.supabase.co")
    ap.add_argument("--ids-requeridos", default=os.path.join(AQUI, "ids_requeridos_10.77.txt"))
    ap.add_argument("--permitir-local", action="store_true", help=argparse.SUPPRESS)  # solo pruebas
    ap.add_argument("--espera", type=float, default=1.0, help=argparse.SUPPRESS)
    a = ap.parse_args(argv)
    clave = os.environ.get("SUPABASE_SERVICE_ROLE_KEY", "")
    try:
        try:
            _, dif = comun.comprobar_scripts(AQUI)
        except comun.ScriptsAlterados as e:
            raise ErrorRespaldo(f"Integridad de scripts: {e}.")
        if dif:
            raise ErrorRespaldo(f"Integridad de scripts: {dif} no coinciden con SCRIPTS.sha256. No se ejecuta.")
        validar_clave(clave)
        base = validar_url(a.url, a.permitir_local)
        requeridos = comun.leer_ids(a.ids_requeridos)
        if not a.solo_listar:
            if not a.salida:
                raise ErrorRespaldo("Falta --salida.")
            if carpeta_en_repo(a.salida):
                raise ErrorRespaldo("La salida está dentro de un repositorio git. Elige una carpeta fuera.")
            if os.path.exists(a.salida) and os.listdir(a.salida):
                raise ErrorRespaldo("La carpeta de salida ya existe y no está vacía.")
        cli = Cliente(base, clave, a.espera)
        return _ejecutar(cli, a, requeridos)
    except ErrorRespaldo as e:
        print("ERROR:", str(e).replace(clave, "***") if clave else e, file=sys.stderr)
        return 1


def _ejecutar(cli, a, requeridos):
    t0 = ahora()
    lista0 = listar(cli, comun.BUCKET)
    print(f"Listado: {len(lista0)} objetos.", file=sys.stderr)
    if a.solo_listar:
        faltan = [i for i in requeridos if f"{i}.webp" not in lista0]
        print(json.dumps({
            "objetos": len(lista0),
            "errores_lector_filas": leer_tabla(cli, "errores_lector", True),
            "capturas_ok_filas": leer_tabla(cli, "capturas_ok", True),
            "requeridos": len(requeridos), "requeridos_ausentes": len(faltan)}, indent=2))
        return 0 if not faltan else 2

    os.makedirs(os.path.join(a.salida, "capturas"), mode=0o700, exist_ok=True)
    os.makedirs(os.path.join(a.salida, "tablas"), mode=0o700, exist_ok=True)
    os.chmod(a.salida, 0o700)
    manifiesto, tablas_res, filas_por_tabla = [], {}, {}

    for t in comun.TABLAS:
        filas = leer_tabla(cli, t)
        filas_por_tabla[t] = filas
        datos = "".join(json.dumps(f, ensure_ascii=False, sort_keys=True) + "\n" for f in filas).encode("utf-8")
        escribir(os.path.join(a.salida, "tablas", f"{t}.jsonl"), datos)
        pv = {}
        for f in filas:
            pv[f.get("version") or "(sin versión)"] = pv.get(f.get("version") or "(sin versión)", 0) + 1
        tablas_res[t] = {"filas": len(filas), "sha256_jsonl": comun.sha256_bytes(datos),
                         "huella_ids": comun.huella_ids(f["id"] for f in filas), "por_version": dict(sorted(pv.items())),
                         "huella_ids_10_77": comun.huella_ids(f["id"] for f in filas if f.get("version") == comun.VERSION_BANCO)}
        manifiesto.append({"tipo": "tabla", "nombre": f"tablas/{t}.jsonl", "bytes": len(datos),
                           "sha256": tablas_res[t]["sha256_jsonl"], "filas": len(filas)})
        print(f"Tabla {t}: {len(filas)} filas.", file=sys.stderr)

    version_de = {t: {f["id"]: f.get("version") for f in filas_por_tabla[t]} for t in comun.TABLAS}
    descargados, fallidos, desaparecidos, etag_ok, etag_mal = {}, [], [], [], []
    for n, (nombre, meta) in enumerate(sorted(lista0.items()), 1):
        ruta = os.path.join(a.salida, "capturas", nombre)
        if os.path.realpath(ruta) != os.path.join(os.path.realpath(os.path.join(a.salida, "capturas")), nombre):
            raise ErrorRespaldo(f"Nombre de objeto inseguro: {nombre!r}")
        os.makedirs(os.path.dirname(ruta), exist_ok=True)
        parcial = ruta + ".part"
        st, _, res = cli.pedir("GET", f"/storage/v1/object/authenticated/{comun.BUCKET}/" + urllib.parse.quote(nombre),
                               destino=parcial)
        if st == 404 or (st in (400,) and res is None):
            desaparecidos.append(nombre)
            _quitar(parcial)
            continue
        if st != 200:
            raise ErrorRespaldo(f"Descarga de {nombre} falló: HTTP {st}")
        total, sha, md5 = res
        if re.fullmatch(r"[0-9a-f]{32}", meta.get("etag", "")):
            etag_ok.append(nombre) if md5 == meta["etag"] else etag_mal.append(nombre)
        with open(parcial, "rb") as f:
            cab = f.read(12)
        if total != meta["bytes"] or not comun.es_webp(cab):
            fallidos.append({"nombre": nombre, "motivo": "tamaño distinto al listado" if total != meta["bytes"] else "no es WebP"})
            _quitar(parcial)
            continue
        os.replace(parcial, ruta)
        os.chmod(ruta, 0o600)
        descargados[nombre] = (total, sha)
        if n % 100 == 0:
            print(f"  {n}/{len(lista0)}", file=sys.stderr)

    lista1 = listar(cli, comun.BUCKET)
    t1 = ahora()
    agregados = sorted(set(lista1) - set(lista0))
    quitados = sorted(set(lista0) - set(lista1))
    cambiados = sorted(k for k in set(lista0) & set(lista1) if lista0[k]["bytes"] != lista1[k]["bytes"])

    por_version = {}
    for nombre, (tam, sha) in descargados.items():
        origen, idl = comun.clasificar(nombre)
        tabla = {"dudosa": "errores_lector", "normal_n": "capturas_ok"}.get(origen)
        ver = version_de[tabla].get(idl) if tabla and idl in version_de[tabla] else None
        manifiesto.append({"tipo": "captura", "nombre": "capturas/" + nombre, "bytes": tam, "sha256": sha,
                           "origen": origen, "id_enlace": "" if idl is None else idl,
                           "version": ver if ver else ("(sin fila)" if tabla else "(sin versión)")})
        clave_v = ver if ver else ("(sin fila)" if tabla else "(sin versión)")
        por_version.setdefault(clave_v, {}).setdefault(origen, 0)
        por_version[clave_v][origen] += 1

    shalista = comun.sha256_archivo(a.ids_requeridos)
    with open(a.ids_requeridos, "rb") as f:
        escribir(os.path.join(a.salida, "ids_requeridos.txt"), f.read())
    manifiesto.append({"tipo": "lista", "nombre": "ids_requeridos.txt", "bytes": os.path.getsize(a.ids_requeridos),
                       "sha256": shalista, "filas": len(requeridos)})

    faltantes = [i for i in requeridos if f"{i}.webp" not in descargados]
    el = version_de["errores_lector"]
    sin_fila = [i for i in requeridos if i not in el]
    version_distinta = [i for i in requeridos if i in el and el[i] != comun.VERSION_BANCO]

    datos_man = comun.escribir_manifiesto(manifiesto)
    escribir(os.path.join(a.salida, "manifiesto.csv"), datos_man)
    motivos = []
    if faltantes:
        motivos.append(f"{len(faltantes)} ids requeridos sin captura")
    if fallidos:
        motivos.append(f"{len(fallidos)} capturas fallidas")
    if sin_fila:
        motivos.append(f"{len(sin_fila)} ids requeridos sin fila en errores_lector")
    if version_distinta:
        motivos.append(f"{len(version_distinta)} ids requeridos con versión distinta de {comun.VERSION_BANCO}")
    if motivos:  # D2: antes sin_fila y version_distinta no entraban en la decisión
        resultado = "INCOMPLETO"
    elif desaparecidos:
        resultado = "COMPLETO_CON_ADVERTENCIAS"
    else:
        resultado = "COMPLETO"
    resumen = {
        "esquema": comun.ESQUEMA, "generado_utc": ahora(), "proyecto_ref": comun.PROYECTO_REF, "bucket": comun.BUCKET,
        "solo_lectura": True, "peticiones": cli.peticiones, "scripts_sha256": comun.hash_scripts(AQUI),
        "listado": {"t0_utc": t0, "t1_utc": t1, "objetos_t0": len(lista0), "objetos_t1": len(lista1),
                    "agregados_durante": agregados, "quitados_durante": quitados, "tamano_cambiado_durante": cambiados},
        "capturas": {"descargadas": len(descargados), "bytes_total": sum(v[0] for v in descargados.values()),
                     "desaparecidas_antes_de_descargar": desaparecidos, "fallidas": fallidos,
                     "etag_md5": {"comprobados": len(etag_ok), "discrepancias": etag_mal},
                     "huella_listado": comun.huella_listado([(k, v[0]) for k, v in descargados.items()]),
                     "huella_objetos": comun.huella_objetos([(k, v[0], v[1]) for k, v in descargados.items()]),
                     "por_version_y_origen": {k: dict(sorted(v.items())) for k, v in sorted(por_version.items())}},
        "tablas": tablas_res,
        "requeridos": {"archivo_sha256": shalista, "esperados": len(requeridos), "presentes": len(requeridos) - len(faltantes),
                       "faltantes": faltantes, "sin_fila_en_errores_lector": sin_fila, "version_distinta_de_10_77": version_distinta},
        "manifiesto_sha256": comun.sha256_bytes(datos_man),
        "resultado_respaldo": resultado,
        "motivos_incompleto": motivos,
        "scripts_integros": True,
    }
    escribir(os.path.join(a.salida, "resumen.json"), comun.json_estable(resumen))
    print(f"Resultado: {resultado}. {len(descargados)} capturas, "
          f"{sum(v['filas'] for v in tablas_res.values())} filas de tablas.", file=sys.stderr)
    for m in motivos:
        print("INCOMPLETO:", m, file=sys.stderr)
    if faltantes:
        print(f"FALTAN {len(faltantes)} ids requeridos: {faltantes[:20]}", file=sys.stderr)
    return {"COMPLETO": 0, "COMPLETO_CON_ADVERTENCIAS": 0, "INCOMPLETO": 2}[resultado]


def _quitar(ruta):
    try:
        os.remove(ruta)
    except FileNotFoundError:
        pass


if __name__ == "__main__":
    sys.exit(main())
