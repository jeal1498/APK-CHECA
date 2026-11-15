#!/usr/bin/env python3
"""Revisión independiente de los artefactos sanitizados que entrega el propietario
(resumen.json, manifiesto.csv, VERIFICACION.json). Sin red, sin imágenes, sin llaves.

No repite lo que solo puede comprobar quien tiene las imágenes (hash de cada archivo); comprueba lo que
SÍ se puede recomputar desde el manifiesto: coherencia entre los tres archivos, huellas, conteos,
ids requeridos (con la lista del repositorio, no la que traiga el resumen), ausencia de datos personales y
presencia/resultado de la comparación con la referencia SQL.

Uso: python3 verificar_entrega.py --dir ENTREGA [--ids ids_requeridos_10.77.txt] [--minimo-objetos 738]
"""
import argparse
import json
import os
import re
import sys

import comun

AQUI = os.path.dirname(os.path.abspath(__file__))
HEX64 = re.compile(r"^[0-9a-f]{64}$")


def verificar_entrega(carpeta, ids_path, minimo_objetos=0):
    chk = []

    def add(id_, estado, txt):
        chk.append({"id": id_, "estado": estado, "detalle": txt})

    def cond(id_, c, ok, mal):
        add(id_, "ok" if c else "fallo", ok if c else mal)

    rutas = {n: os.path.join(carpeta, n) for n in ("resumen.json", "manifiesto.csv", "VERIFICACION.json")}
    falta = [n for n, p in rutas.items() if not os.path.isfile(p)]
    cond("E01a", not falta, "Están los tres artefactos.", f"Faltan: {falta}")
    if falta:
        return {"resultado": "FALLO", "checks": chk}
    textos = {n: open(p, encoding="utf-8").read() for n, p in rutas.items()}
    for n, t in textos.items():
        m = comun.RE_SECRETOS.search(t)
        cond(f"E01b-{n}", m is None, f"{n}: sin llaves, correos ni UUID de usuario.", f"{n}: contiene un patrón sensible (tipo detectado, no se reproduce).")
    res, ver = json.loads(textos["resumen.json"]), json.loads(textos["VERIFICACION.json"])
    man = comun.leer_manifiesto(rutas["manifiesto.csv"])
    man_sha = comun.sha256_archivo(rutas["manifiesto.csv"])
    cond("E02", man_sha == res.get("manifiesto_sha256") == ver.get("manifiesto_sha256")
         and comun.sha256_archivo(rutas["resumen.json"]) == ver.get("resumen_sha256"),
         "sha256 de manifiesto.csv y resumen.json coinciden en los tres archivos.", "Los sha256 cruzados entre resumen, manifiesto y VERIFICACION no coinciden.")

    nombres = [f["nombre"] for f in man]
    estructura = len(nombres) == len(set(nombres)) and all(HEX64.match(f["sha256"]) and f["bytes"].isdigit() and int(f["bytes"]) > 0 for f in man)
    caps = [f for f in man if f["tipo"] == "captura"]
    estructura = estructura and all(comun.clasificar(f["nombre"][len("capturas/"):])[0] == f["origen"] for f in caps)
    cond("E03", estructura, f"Manifiesto bien formado: {len(man)} filas, sin repetidos, sha256 válidos, origen coherente con el nombre.", "Manifiesto mal formado, con repetidos o con origen incoherente.")

    pares = [(f["nombre"][len("capturas/"):], int(f["bytes"])) for f in caps]
    trios = [(f["nombre"][len("capturas/"):], int(f["bytes"]), f["sha256"]) for f in caps]
    h1, h2 = comun.huella_listado(pares), comun.huella_objetos(trios)
    rc = res.get("capturas", {})
    cond("E04", h1 == rc.get("huella_listado") == ver.get("huellas", {}).get("listado") and h2 == rc.get("huella_objetos") == ver.get("huellas", {}).get("objetos"),
         f"Huellas recalculadas desde el manifiesto = resumen = VERIFICACION (listado {h1[:12]}…, objetos {h2[:12]}…).", "Las huellas recalculadas desde el manifiesto NO coinciden.")
    cond("E05", len(caps) == rc.get("descargadas") == ver.get("conteos", {}).get("capturas"), f"Conteo de capturas coherente: {len(caps)}.", "El conteo de capturas no coincide entre manifiesto, resumen y VERIFICACION.")
    cond("E05b", len(caps) >= minimo_objetos, f"{len(caps)} capturas ≥ mínimo esperado ({minimo_objetos}).", f"Solo {len(caps)} capturas; se esperaban ≥ {minimo_objetos} (o el conteo actualizado, justificado).")

    req = comun.leer_ids(ids_path)
    sha_req = comun.sha256_archivo(ids_path)
    cond("E06a", res.get("requeridos", {}).get("archivo_sha256") == sha_req, f"La lista de ids usada en el respaldo es la del repositorio ({len(req)} ids).",
         "La lista de ids usada en el respaldo NO es la del repositorio (¿lista distinta o desactualizada?).")
    nom = {n for n, _ in pares}
    faltan = [i for i in req if f"{i}.webp" not in nom]
    cond("E06b", not faltan, f"Los {len(req)} ids requeridos están en el manifiesto.", f"Faltan {len(faltan)} ids requeridos en el manifiesto: {faltan[:20]}")
    for t in comun.TABLAS:
        fm = next((f for f in man if f["nombre"] == f"tablas/{t}.jsonl"), None)
        cond(f"E07-{t}", fm is not None and int(fm["filas"]) == res.get("tablas", {}).get(t, {}).get("filas"), f"{t}: filas del manifiesto = resumen.", f"{t}: filas del manifiesto ≠ resumen.")
    pv = {}
    for f in caps:
        pv.setdefault(f["version"], {}).setdefault(f["origen"], 0)
        pv[f["version"]][f["origen"]] += 1
    cond("E08", {k: dict(sorted(v.items())) for k, v in sorted(pv.items())} == rc.get("por_version_y_origen"), "Conteo por versión y origen recalculado = resumen.", "El conteo por versión/origen no coincide con el manifiesto.")
    v77 = pv.get(comun.VERSION_BANCO, {})
    add("E08b", "ok", f"Informativo: capturas 10.77 en el respaldo: {v77}. Compara con el número de la consulta SQL.")

    checks_ver = ver.get("checks", [])
    mal = [c["id"] for c in checks_ver if c["estado"] == "fallo"]
    cond("E09a", ver.get("resultado") in ("APROBADO", "APROBADO_CON_ADVERTENCIAS") and not mal, f"VERIFICACION.json: {ver.get('resultado')}, sin comprobaciones fallidas.", f"VERIFICACION.json reporta {ver.get('resultado')}; fallos: {mal}")
    avisos = [(c["id"], c["detalle"]) for c in checks_ver if c["estado"] == "advertencia"]
    add("E09b", "ok" if not avisos else "advertencia", "Sin advertencias." if not avisos else f"Advertencias que debe explicar el propietario: {avisos}")
    refok = all(ver.get("referencia", {}).get(k) for k in ("objetos_csv_sha256", "tablas_csv_sha256")) and not any(c["id"] == "R00" for c in checks_ver)
    cond("E10", refok, "La verificación incluyó la consulta de referencia SQL.", "No hay referencia SQL en VERIFICACION.json: no se cumple el criterio de conteos/huella del mismo momento.")
    ts = (res.get("generado_utc"), ver.get("verificado_utc"), ver.get("referencia", {}).get("consultado_utc"))
    add("E11", "ok", f"Informativo: respaldo generado {ts[0]}, verificado {ts[1]}, referencia SQL consultada {ts[2]}. Revisar que estén separados por minutos, no horas.")
    try:
        _, dif_local = comun.comprobar_scripts(AQUI)
        p_res = comun.huellas_registradas_validas(res.get("scripts_sha256"), AQUI)
        p_ver = comun.huellas_registradas_validas(ver.get("scripts_sha256"), AQUI)
    except comun.ScriptsAlterados as e:
        dif_local, p_res, p_ver = [str(e)], [], []
    cond("E12", not dif_local, "verificar_entrega corre con los scripts fijados en SCRIPTS.sha256.", f"Scripts locales alterados: {dif_local}")
    cond("E12b", not p_res, "resumen.json: respaldo hecho con todos los scripts fijados.", f"resumen.json: {p_res}")
    cond("E12c", not p_ver, "VERIFICACION.json: verificación hecha con todos los scripts fijados.", f"VERIFICACION.json: {p_ver}")
    cond("E13", res.get("solo_lectura") is True and res.get("proyecto_ref") == comun.PROYECTO_REF, "Declara solo lectura y el proyecto esperado.", "No declara solo lectura o el proyecto no es el esperado.")
    ruta_ev = os.path.join(carpeta, "EVIDENCIA-COPIAS.json")
    if not os.path.isfile(ruta_ev):
        add("C00", "advertencia", "PENDIENTE: no hay EVIDENCIA-COPIAS.json. E0 NO puede cerrarse sin dos copias cifradas y restauración de prueba de la copia B.")
    else:
        tev = open(ruta_ev, encoding="utf-8").read()
        ev = json.loads(tev)
        cond("C01a", comun.sin_trazas_de_secretos(tev), "EVIDENCIA-COPIAS.json sin patrones sensibles.", "EVIDENCIA-COPIAS.json contiene un patrón sensible.")
        c = ev.get("copias", {})
        cond("C01", ev.get("resultado") == "COMPLETA", "EVIDENCIA-COPIAS.json declara COMPLETA.", f"EVIDENCIA-COPIAS.json: {ev.get('resultado')}.")
        cond("C02", all(c.get(k, {}).get("cifrada_detectada") for k in "AB") and len(c) == 2, "Ambas copias tienen cabecera de cifrado (age/gpg).", "Alguna copia no se detecta como cifrada.")
        cond("C03", ev.get("medios_declarados_distintos") is True, f"Medios declarados distintos (declaración, no verificable): A={c.get('A',{}).get('medio_declarado')!r}, B={c.get('B',{}).get('medio_declarado')!r}.", "Los medios declarados faltan o son iguales.")
        rb = ev.get("restauracion_b", {})
        cond("C04", ev.get("restauracion_b_corresponde_a_copia_b") is True and rb.get("etiqueta_copia") == "B" and rb.get("copia_sha256") == c.get("B", {}).get("sha256"),
             "La restauración se hizo desde el archivo de la copia B (mismo sha256).", "La restauración no está ligada al sha256 de la copia B.")
        vb = rb.get("verificacion", {})
        cond("C05", rb.get("restauracion_ok") is True and rb.get("tar_coincide") is True and vb.get("manifiesto_sha256") == res.get("manifiesto_sha256") and vb.get("resultado") in ("APROBADO", "APROBADO_CON_ADVERTENCIAS"),
             "Lo restaurado desde B verifica y su manifiesto = el del respaldo original.", "Lo restaurado desde B no verifica o su manifiesto difiere del original.")
        cond("C06", vb.get("huellas", {}).get("listado") == h1 and vb.get("huellas", {}).get("objetos") == h2 and vb.get("conteos", {}).get("capturas") == len(caps),
             "Huellas y conteo de lo restaurado = los del manifiesto entregado.", "Huellas/conteo de lo restaurado ≠ manifiesto entregado.")
        cond("C07", ev.get("declaraciones_del_propietario", {}).get("conserva_ambas_copias") is True, "El propietario declara conservar ambas copias (declaración).", "Falta la declaración de conservar las copias.")
        cond("C08", ev.get("cifrados_distintos") is True and c.get("A", {}).get("sha256") != c.get("B", {}).get("sha256") and ev.get("archivos_distintos") is True,
             "A y B son cifrados distintos (sha256 distinto, archivos distintos); la independencia física es declarada, no comprobable.",
             "A y B no son cifrados distintos (mismo sha256 o mismo archivo): no hay dos copias cifradas independientes.")
        p_ev = comun.huellas_registradas_validas(ev.get("scripts_sha256"), AQUI)
        cond("C09", not p_ev, "EVIDENCIA-COPIAS.json: generada con los scripts fijados.", f"EVIDENCIA-COPIAS.json: {p_ev}")
    estado = "FALLO" if any(c["estado"] == "fallo" for c in chk) else ("APROBADO_CON_ADVERTENCIAS" if any(c["estado"] == "advertencia" for c in chk) else "APROBADO")
    return {"resultado": estado, "checks": chk}


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dir", required=True)
    ap.add_argument("--ids", default=os.path.join(AQUI, "ids_requeridos_10.77.txt"))
    ap.add_argument("--minimo-objetos", type=int, default=738)
    a = ap.parse_args(argv)
    r = verificar_entrega(a.dir, a.ids, a.minimo_objetos)
    for c in r["checks"]:
        print(f"[{c['estado'].upper():11}] {c['id']}: {c['detalle']}")
    print("RESULTADO:", r["resultado"])
    return 1 if r["resultado"] == "FALLO" else 0


if __name__ == "__main__":
    sys.exit(main())
