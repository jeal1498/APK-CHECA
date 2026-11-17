#!/usr/bin/env python3
"""Verificación SIN red y SIN llave de un respaldo ya hecho (o de una copia restaurada).

Recalcula desde los archivos: tamaños, sha256, formato WebP, filas de las tablas, huellas e IDs
requeridos, y los contrasta con manifiesto.csv, resumen.json y (opcional) con la consulta de
referencia hecha con `referencia.sql`. Escribe VERIFICACION.json (sin rutas, sin datos personales).

Uso: python3 verificar.py --dir RESPALDO [--ref-objetos ref_objetos.csv --ref-tablas ref_tablas.csv]
                          [--salida VERIFICACION.json]
Código de salida: 0 = APROBADO (o con advertencias), 1 = FALLO.
"""
import argparse
import csv
import json
import os
import sys
from datetime import datetime, timezone

import comun

AQUI = os.path.dirname(os.path.abspath(__file__))
SOLO_RAIZ = ("resumen.json", "manifiesto.csv")


class Checks:
    def __init__(self):
        self.lista = []

    def add(self, id_, estado, texto, **extra):
        self.lista.append({"id": id_, "estado": estado, "detalle": texto, **extra})

    def ok(self, id_, texto, **e):
        self.add(id_, "ok", texto, **e)

    def fallo(self, id_, texto, **e):
        self.add(id_, "fallo", texto, **e)

    def aviso(self, id_, texto, **e):
        self.add(id_, "advertencia", texto, **e)

    def cond(self, id_, cond, texto_ok, texto_mal, **e):
        (self.ok if cond else self.fallo)(id_, texto_ok if cond else texto_mal, **e)

    def resultado(self):
        if any(c["estado"] == "fallo" for c in self.lista):
            return "FALLO"
        if any(c["estado"] == "advertencia" for c in self.lista):
            return "APROBADO_CON_ADVERTENCIAS"
        return "APROBADO"


def _csv(ruta):
    with open(ruta, "r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def verificar(carpeta, ref_objetos=None, ref_tablas=None):
    c = Checks()
    p_man, p_res = os.path.join(carpeta, "manifiesto.csv"), os.path.join(carpeta, "resumen.json")
    if not (os.path.isfile(p_man) and os.path.isfile(p_res)):
        c.fallo("V00", "Faltan manifiesto.csv o resumen.json.")
        return {"resultado": c.resultado(), "checks": c.lista}
    with open(p_res, "rb") as f:
        crudo_res = f.read()
    resumen = json.loads(crudo_res)
    man_sha = comun.sha256_archivo(p_man)
    c.cond("V01", man_sha == resumen.get("manifiesto_sha256"), "El sha256 de manifiesto.csv coincide con resumen.json.",
           "manifiesto.csv no coincide con el sha256 anotado en resumen.json (¿fue modificado?).")
    try:
        filas = comun.leer_manifiesto(p_man)
    except Exception as e:
        c.fallo("V01b", f"manifiesto.csv ilegible: {e}")
        return {"resultado": c.resultado(), "checks": c.lista}

    nombres = [f["nombre"] for f in filas]
    c.cond("V02", len(nombres) == len(set(nombres)), "Sin nombres repetidos en el manifiesto.", "Nombres repetidos en el manifiesto.")

    # V03 archivos del manifiesto: existen, tamaño, sha256
    malos, sha_real = [], {}
    for f in filas:
        ruta = os.path.join(carpeta, *f["nombre"].split("/"))
        if not os.path.isfile(ruta):
            malos.append((f["nombre"], "falta"))
            continue
        tam, sha = os.path.getsize(ruta), comun.sha256_archivo(ruta)
        sha_real[f["nombre"]] = (tam, sha)
        if str(tam) != f["bytes"] or sha != f["sha256"]:
            malos.append((f["nombre"], "tamaño o sha256 distinto"))
    c.cond("V03", not malos, f"{len(filas)} archivos del manifiesto: existen, con el tamaño y sha256 anotados.",
           f"{len(malos)} archivos fallan (primeros: {malos[:5]}).", archivos=len(filas), fallidos=len(malos))

    # V04 archivos de más
    previstos = set(nombres) | set(SOLO_RAIZ)
    extra = []
    for raiz, _, archivos in os.walk(carpeta):
        for a in archivos:
            rel = os.path.relpath(os.path.join(raiz, a), carpeta).replace(os.sep, "/")
            if rel not in previstos and not rel.startswith("VERIFICACION"):
                extra.append(rel)
    c.cond("V04", not extra, "No hay archivos fuera del manifiesto.", f"Archivos que no están en el manifiesto: {extra[:5]}.")

    # V05 WebP
    caps = [f for f in filas if f["tipo"] == "captura"]
    no_webp = []
    for f in caps:
        ruta = os.path.join(carpeta, *f["nombre"].split("/"))
        if os.path.isfile(ruta):
            with open(ruta, "rb") as h:
                if not comun.es_webp(h.read(12)):
                    no_webp.append(f["nombre"])
    c.cond("V05", not no_webp, f"Las {len(caps)} capturas tienen cabecera WebP.", f"No son WebP: {no_webp[:5]}.")

    # V06 tablas
    ids_tabla, versiones = {}, {}
    for t in comun.TABLAS:
        ruta = os.path.join(carpeta, "tablas", f"{t}.jsonl")
        fila_m = next((f for f in filas if f["nombre"] == f"tablas/{t}.jsonl"), None)
        if not os.path.isfile(ruta) or fila_m is None:
            c.fallo(f"V06-{t}", "Falta la tabla en disco o en el manifiesto.")
            continue
        try:
            reg = comun.contar_lineas_jsonl(ruta)
        except Exception as e:
            c.fallo(f"V06-{t}", f"JSONL ilegible: {e}")
            continue
        ids = [r.get("id") for r in reg]
        res_t = resumen.get("tablas", {}).get(t, {})
        ok = (len(reg) == int(fila_m["filas"]) == res_t.get("filas") and len(set(ids)) == len(ids)
              and comun.huella_ids(ids) == res_t.get("huella_ids"))
        c.cond(f"V06-{t}", ok, f"{t}: {len(reg)} filas, ids únicos, filas y huella de ids coinciden con manifiesto y resumen.",
               f"{t}: filas ({len(reg)}), ids o huella de ids no coinciden con manifiesto/resumen.", filas=len(reg))
        ids_tabla[t] = set(ids)
        versiones[t] = {r["id"]: r.get("version") for r in reg}

    # V07 huellas del conjunto
    cap_pares = [(f["nombre"][len("capturas/"):], int(f["bytes"])) for f in caps]
    cap_trios = [(n, b, next(x["sha256"] for x in caps if x["nombre"] == "capturas/" + n)) for n, b in cap_pares]
    h_lis, h_obj = comun.huella_listado(cap_pares), comun.huella_objetos(cap_trios)
    rc = resumen.get("capturas", {})
    c.cond("V07", h_lis == rc.get("huella_listado") and h_obj == rc.get("huella_objetos") and len(caps) == rc.get("descargadas"),
           "Huellas del conjunto recalculadas desde el manifiesto = resumen.json.",
           "Las huellas o el conteo recalculados no coinciden con resumen.json.", huella_listado=h_lis, huella_objetos=h_obj, capturas=len(caps))

    # V08 IDs requeridos
    p_ids = os.path.join(carpeta, "ids_requeridos.txt")
    req = []
    if os.path.isfile(p_ids):
        req = comun.leer_ids(p_ids)
    rq = resumen.get("requeridos", {})
    c.cond("V08a", os.path.isfile(p_ids) and comun.sha256_archivo(p_ids) == rq.get("archivo_sha256") and len(req) == rq.get("esperados"),
           f"Lista de ids requeridos íntegra ({len(req)} ids).", "La lista de ids requeridos falta o no coincide con resumen.json.")
    nombres_cap = {n for n, _ in cap_pares}
    faltan = [i for i in req if f"{i}.webp" not in nombres_cap]
    c.cond("V08b", not faltan and req, f"Los {len(req)} ids requeridos tienen su captura respaldada.", f"Faltan {len(faltan)} capturas requeridas: {faltan[:20]}.")
    el = versiones.get("errores_lector", {})
    sin_fila = [i for i in req if i not in el]
    distinta = [i for i in req if i in el and el[i] != comun.VERSION_BANCO]
    c.cond("V08c", not sin_fila and not distinta, f"Los {len(req)} ids requeridos tienen fila en errores_lector con versión {comun.VERSION_BANCO}.",
           f"Ids sin fila: {sin_fila[:10]}; con versión distinta de {comun.VERSION_BANCO}: {distinta[:10]}.")

    # V09 enlaces captura ↔ fila
    sin_enlace = [f["nombre"] for f in caps if f["origen"] in ("dudosa", "normal_n") and
                  int(f["id_enlace"]) not in ids_tabla.get("errores_lector" if f["origen"] == "dudosa" else "capturas_ok", set())]
    if sin_enlace:
        c.aviso("V09", f"{len(sin_enlace)} capturas sin fila enlazada (¿fila borrada?).", ejemplos=sin_enlace[:5])
    else:
        c.ok("V09", "Toda captura con id enlazado tiene su fila.")
    sin_n = sum(1 for f in caps if f["origen"] == "normal_sin_n")
    c.ok("V09b", f"{sin_n} capturas normales sin -n<id> (10.78–10.79) no se enlazan a ninguna fila: es lo esperado.")

    # V10 estado declarado por el respaldo
    est = resumen.get("resultado_respaldo")
    if est == "COMPLETO":
        c.ok("V10", "El respaldo se declaró COMPLETO.")
    elif est == "COMPLETO_CON_ADVERTENCIAS":
        c.aviso("V10", "El respaldo se declaró COMPLETO_CON_ADVERTENCIAS: objetos que desaparecieron durante la descarga.",
                desaparecidas=resumen.get("capturas", {}).get("desaparecidas_antes_de_descargar", [])[:20])
    else:
        c.fallo("V10", f"El respaldo se declaró {est}.")
    ld = resumen.get("listado", {})
    if ld.get("agregados_durante") or ld.get("quitados_durante") or ld.get("tamano_cambiado_durante"):
        c.aviso("V10b", "El bucket cambió durante el respaldo.", agregados=len(ld.get("agregados_durante", [])),
                quitados=len(ld.get("quitados_durante", [])), cambiados=len(ld.get("tamano_cambiado_durante", [])))

    em = rc.get("etag_md5", {})
    if em.get("discrepancias"):
        c.aviso("V10c", f"{len(em['discrepancias'])} capturas con md5 distinto del eTag del servidor (formato de eTag no confirmado).", ejemplos=em["discrepancias"][:5])
    else:
        c.ok("V10c", f"Contraste md5 con el eTag del servidor: {em.get('comprobados', 0)} comprobadas, 0 discrepancias (best-effort).")

    # V11 versión de los scripts
    actual = comun.hash_scripts(AQUI)
    reg = resumen.get("scripts_sha256", {})
    difieren = [k for k in ("comun.py", "respaldar.py") if k in reg and reg[k] != actual.get(k)]
    if difieren:
        c.aviso("V11", f"Los scripts actuales difieren de los que hicieron el respaldo: {difieren}.")
    else:
        c.ok("V11", "comun.py y respaldar.py coinciden con los usados en el respaldo.")

    ref = _referencia(c, caps, cap_pares, resumen, ref_objetos, ref_tablas, h_lis, ids_tabla, versiones)
    return {
        "esquema": comun.ESQUEMA, "verificado_utc": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "resultado": c.resultado(), "manifiesto_sha256": man_sha, "resumen_sha256": comun.sha256_bytes(crudo_res),
        "conteos": {"capturas": len(caps), "filas_errores_lector": len(ids_tabla.get("errores_lector", ())),
                    "filas_capturas_ok": len(ids_tabla.get("capturas_ok", ())), "ids_requeridos": len(req),
                    "ids_requeridos_presentes": len(req) - len(faltan)},
        "huellas": {"listado": h_lis, "objetos": h_obj},
        "referencia": ref, "scripts_sha256": actual, "checks": c.lista,
    }


def _referencia(c, caps, cap_pares, resumen, ref_objetos, ref_tablas, h_lis, ids_tabla, versiones):
    ref = {"objetos_csv_sha256": None, "tablas_csv_sha256": None}
    if not ref_objetos or not ref_tablas:
        c.aviso("R00", "Sin consulta de referencia (referencia.sql): no se cumple el criterio de conteos y huella de E0.")
        return ref
    ref["objetos_csv_sha256"], ref["tablas_csv_sha256"] = comun.sha256_archivo(ref_objetos), comun.sha256_archivo(ref_tablas)
    ref["consultado_utc"] = _una(ref_tablas, "consultado_utc")
    ref["resumen_sql"] = {k: _fila(ref_tablas).get(k) for k in ("objetos", "errores_lector", "errores_lector_10_77", "capturas_ok", "huella_listado")}
    objs = {r["name"]: int(r["bytes"]) for r in _csv(ref_objetos)}
    mio = dict(cap_pares)
    desap = set(resumen.get("capturas", {}).get("desaparecidas_antes_de_descargar", []))
    faltan = sorted(set(objs) - set(mio))
    sin_explicar = [n for n in faltan if n not in desap]
    c.cond("R01", not sin_explicar, f"Los {len(objs)} objetos de la referencia están respaldados (o explicados).",
           f"{len(sin_explicar)} objetos de la referencia no están en el respaldo: {sin_explicar[:10]}.")
    if set(faltan) & desap:
        c.aviso("R01b", f"{len(set(faltan) & desap)} objetos de la referencia expiraron/desaparecieron durante la descarga.")
    tam_mal = [n for n in objs if n in mio and objs[n] != mio[n]]
    c.cond("R02", not tam_mal, "Los tamaños coinciden con la referencia.", f"Tamaños distintos de la referencia: {tam_mal[:10]}.")
    extras = sorted(set(mio) - set(objs))
    if extras:
        c.aviso("R03", f"{len(extras)} objetos respaldados no estaban en la referencia (subidos o referencia previa).", ejemplos=extras[:5])
    if not faltan and not extras and not tam_mal:
        c.cond("R04", h_lis == _una(ref_tablas, "huella_listado"), "huella_listado del respaldo = huella de la consulta SQL.",
               "huella_listado del respaldo ≠ huella de la consulta SQL aunque los conjuntos coinciden.")
    else:
        c.aviso("R04", "Huella del listado no comparable porque los conjuntos difieren (ver R01–R03).")
    t = _fila(ref_tablas)
    for tabla, col, col77 in (("errores_lector", "errores_lector", "huella_ids_errores_lector"), ("capturas_ok", "capturas_ok", "huella_ids_capturas_ok")):
        n_ref, n_mio = int(t.get(col, -1)), len(ids_tabla.get(tabla, ()))
        c.cond(f"R05-{tabla}", n_mio >= n_ref, f"{tabla}: {n_mio} filas respaldadas ≥ {n_ref} de la referencia.",
               f"{tabla}: {n_mio} filas respaldadas < {n_ref} de la referencia.")
        if n_mio == n_ref:
            c.cond(f"R06-{tabla}", comun.huella_ids(ids_tabla[tabla]) == (t.get(col77) or None), f"{tabla}: huella de ids = referencia.",
                   f"{tabla}: huella de ids distinta de la referencia con el mismo conteo.")
        else:
            c.aviso(f"R06-{tabla}", f"{tabla}: hay {n_mio - n_ref} filas más que en la referencia (actividad posterior).")
    v77 = [i for i, v in versiones.get("errores_lector", {}).items() if v == comun.VERSION_BANCO]
    n77 = int(t.get("errores_lector_10_77", -1))
    c.cond("R07", len(v77) >= n77, f"errores_lector 10.77: {len(v77)} ≥ {n77} de la referencia.", f"errores_lector 10.77: {len(v77)} < {n77} de la referencia.")
    if len(v77) == n77:
        c.cond("R08", comun.huella_ids(v77) == (t.get("huella_ids_errores_lector_10_77") or None), "Huella de ids 10.77 = referencia.", "Huella de ids 10.77 ≠ referencia.")
    return ref


def _fila(ruta):
    filas = _csv(ruta)
    if len(filas) != 1:
        raise ValueError("ref_tablas.csv debe tener una sola fila")
    return filas[0]


def _una(ruta, col):
    return _fila(ruta).get(col) or None


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dir", required=True)
    ap.add_argument("--ref-objetos")
    ap.add_argument("--ref-tablas")
    ap.add_argument("--salida", help="por defecto DIR/VERIFICACION.json")
    a = ap.parse_args(argv)
    r = verificar(a.dir, a.ref_objetos, a.ref_tablas)
    datos = comun.json_estable(r)
    destino = a.salida or os.path.join(a.dir, "VERIFICACION.json")
    with open(destino, "wb") as f:
        f.write(datos)
    for ch in r["checks"]:
        if ch["estado"] != "ok":
            print(f"[{ch['estado'].upper()}] {ch['id']}: {ch['detalle']}", file=sys.stderr)
    print(f"Resultado: {r['resultado']} ({sum(1 for x in r['checks'] if x['estado']=='ok')}/{len(r['checks'])} comprobaciones ok). "
          f"Escrito {destino}", file=sys.stderr)
    return 1 if r["resultado"] == "FALLO" else 0


if __name__ == "__main__":
    sys.exit(main())
