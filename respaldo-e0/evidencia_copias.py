#!/usr/bin/env python3
"""Empaquetado, restauración de prueba y registro de evidencia de las dos copias cifradas (sin red, sin llaves de Supabase).

Pasos (ver README.md):
  1. empaquetar  : carpeta del respaldo → .tar determinista + .empaquetado.json (sha256 del tar en claro).
  2. (tú) cifras el .tar DOS veces con age o gpg y guardas cada copia en un medio distinto; destruyes el .tar en claro.
  3. restaurar   : descifra la COPIA B en una carpeta nueva, comprueba que el tar descifrado tiene el sha256
                   anotado y corre verificar.py sobre lo restaurado → RESTAURACION-B.json.
  4. registrar   : junta todo en EVIDENCIA-COPIAS.json (sin rutas ni nombres de archivo).
Lo que NO puede comprobar ningún script: que los dos medios sean físicamente independientes ni que conserves
las copias. Eso lo declaras tú (--medio-a, --medio-b, --confirmo-conservar) y queda registrado como declaración.
"""
import argparse
import io
import json
import os
import subprocess
import sys
import tarfile
import tempfile
from datetime import datetime, timezone

import comun
import verificar


def ahora():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def formato_cifrado(ruta):
    with open(ruta, "rb") as f:
        cab = f.read(512)
    if cab.startswith(b"age-encryption.org/"):
        return "age"
    if cab.startswith(b"-----BEGIN PGP MESSAGE-----"):
        return "gpg"
    if cab and cab[0] & 0x80:
        tag = (cab[0] & 0x3F) if cab[0] & 0x40 else ((cab[0] >> 2) & 0x0F)
        if tag in (1, 3, 9, 18):
            return "gpg"
    return "desconocido"  # tar, gzip, zip, texto… no se aceptan como «cifrado»


def _fuera_de_repo(ruta):
    import respaldar
    if respaldar.carpeta_en_repo(ruta):
        raise SystemExit("ERROR: la ruta está dentro de un repositorio git. Usa una carpeta fuera.")


def empaquetar(carpeta, salida):
    _fuera_de_repo(salida)
    if os.path.exists(salida):
        raise SystemExit("ERROR: el archivo de salida ya existe.")
    man = comun.leer_manifiesto(os.path.join(carpeta, "manifiesto.csv"))
    res = json.load(open(os.path.join(carpeta, "resumen.json"), encoding="utf-8"))
    base = os.path.basename(os.path.normpath(carpeta))
    rutas = ["manifiesto.csv", "resumen.json"] + sorted(f["nombre"] for f in man)
    if os.path.exists(os.path.join(carpeta, "VERIFICACION.json")):
        rutas.append("VERIFICACION.json")
    fd = os.open(salida, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "wb") as f, tarfile.open(fileobj=f, mode="w", format=tarfile.PAX_FORMAT) as tar:
        for rel in sorted(set(rutas)):
            p = os.path.join(carpeta, *rel.split("/"))
            ti = tarfile.TarInfo(f"{base}/{rel}")
            ti.size, ti.mtime, ti.mode, ti.uid, ti.gid, ti.uname, ti.gname = os.path.getsize(p), 0, 0o600, 0, 0, "", ""
            with open(p, "rb") as g:
                tar.addfile(ti, g)
    info = {"esquema": comun.ESQUEMA, "empaquetado_utc": ahora(), "tar_sha256": comun.sha256_archivo(salida),
            "tar_bytes": os.path.getsize(salida), "archivos": len(set(rutas)), "manifiesto_sha256": res["manifiesto_sha256"]}
    with open(salida + ".empaquetado.json", "wb") as f:
        f.write(comun.json_estable(info))
    print(f"tar sha256 {info['tar_sha256']}  ({info['archivos']} archivos)", file=sys.stderr)
    return 0


def _descifrar(ruta, formato, identidad, frase_archivo, destino_tar):
    if formato == "age":
        cmd = ["age", "-d"] + (["-i", identidad] if identidad else []) + [ruta]
    else:
        cmd = ["gpg", "--quiet", "--decrypt"] + (["--batch", "--pinentry-mode", "loopback", "--passphrase-file", frase_archivo] if frase_archivo else []) + [ruta]
    try:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    except FileNotFoundError:
        raise SystemExit(f"ERROR: no está instalado «{cmd[0]}».")
    import hashlib
    h, total = hashlib.sha256(), 0
    with open(destino_tar, "wb") as f:
        for bloque in iter(lambda: p.stdout.read(1 << 20), b""):
            f.write(bloque)
            h.update(bloque)
            total += len(bloque)
    if p.wait() != 0:
        raise SystemExit("ERROR: el descifrado falló (¿clave o frase incorrecta, o archivo dañado?).")
    return h.hexdigest(), total


def _extraer_seguro(tar_ruta, destino):
    os.makedirs(destino, mode=0o700)
    raiz = os.path.realpath(destino)
    n = 0
    with tarfile.open(tar_ruta, "r:") as tar:
        for m in tar.getmembers():
            objetivo = os.path.realpath(os.path.join(raiz, m.name))
            if m.name.startswith("/") or ".." in m.name.split("/") or not (objetivo == raiz or objetivo.startswith(raiz + os.sep)):
                raise SystemExit(f"ERROR: entrada insegura en el tar: {m.name!r}")
            if m.isdir():
                os.makedirs(objetivo, exist_ok=True)
            elif m.isreg():
                os.makedirs(os.path.dirname(objetivo), exist_ok=True)
                with tar.extractfile(m) as src, open(objetivo, "wb") as dst:
                    dst.write(src.read())
                os.chmod(objetivo, 0o600)
                n += 1
            else:
                raise SystemExit(f"ERROR: tipo de entrada no permitido en el tar: {m.name!r}")
    return n


def restaurar(copia, etiqueta, destino, empaquetado, identidad, frase_archivo, ref_objetos, ref_tablas, salida_json):
    _fuera_de_repo(destino)
    if os.path.exists(destino):
        raise SystemExit("ERROR: la carpeta de restauración ya existe; debe ser nueva (no se toca el original).")
    formato = formato_cifrado(copia)
    if formato == "desconocido":
        raise SystemExit("ERROR: el archivo no parece cifrado con age ni gpg.")
    info = json.load(open(empaquetado, encoding="utf-8"))
    copia_sha, copia_bytes = comun.sha256_archivo(copia), os.path.getsize(copia)
    tmp = tempfile.mkdtemp(prefix="e0-restaurar-")
    tar_tmp = os.path.join(tmp, "descifrado.tar")
    try:
        sha_tar, bytes_tar = _descifrar(copia, formato, identidad, frase_archivo, tar_tmp)
        n = _extraer_seguro(tar_tmp, destino)
    finally:
        try:
            os.remove(tar_tmp)
            os.rmdir(tmp)
        except OSError:
            pass
    tops = [d for d in os.listdir(destino) if os.path.isdir(os.path.join(destino, d))]
    if len(tops) != 1:
        raise SystemExit("ERROR: el tar debe contener una sola carpeta de respaldo.")
    v = verificar.verificar(os.path.join(destino, tops[0]), ref_objetos, ref_tablas)
    r = {"esquema": comun.ESQUEMA, "restaurado_utc": ahora(), "etiqueta_copia": etiqueta, "copia_sha256": copia_sha, "copia_bytes": copia_bytes,
         "formato_cifrado": formato, "tar_descifrado_sha256": sha_tar, "tar_esperado_sha256": info["tar_sha256"],
         "tar_coincide": sha_tar == info["tar_sha256"] and bytes_tar == info["tar_bytes"], "archivos_extraidos": n,
         "verificacion": {k: v[k] for k in ("resultado", "manifiesto_sha256", "resumen_sha256", "conteos", "huellas")},
         "fallos": [c["id"] for c in v["checks"] if c["estado"] == "fallo"],
         "manifiesto_original_sha256": info["manifiesto_sha256"],
         "manifiesto_coincide_con_original": v["manifiesto_sha256"] == info["manifiesto_sha256"],
         "original_alterado": False, "nota": "La restauración se hizo en carpeta nueva; el original solo se lee para empaquetar."}
    r["restauracion_ok"] = r["tar_coincide"] and r["manifiesto_coincide_con_original"] and v["resultado"] in ("APROBADO", "APROBADO_CON_ADVERTENCIAS")
    with open(salida_json, "wb") as f:
        f.write(comun.json_estable(r))
    print(f"Restauración {etiqueta}: {'OK' if r['restauracion_ok'] else 'FALLÓ'} (tar_coincide={r['tar_coincide']}, verificación={v['resultado']})", file=sys.stderr)
    return 0 if r["restauracion_ok"] else 1


def registrar(args):
    ev = {"esquema": comun.ESQUEMA, "registrado_utc": ahora(), "copias": {}, "declaraciones_del_propietario": {}}
    for et, ruta, medio in (("A", args.copia_a, args.medio_a), ("B", args.copia_b, args.medio_b)):
        fmt = formato_cifrado(ruta)
        ev["copias"][et] = {"sha256": comun.sha256_archivo(ruta), "bytes": os.path.getsize(ruta), "formato_cifrado": fmt,
                            "cifrada_detectada": fmt != "desconocido", "medio_declarado": medio.strip()}
    a, b = ev["copias"]["A"], ev["copias"]["B"]
    ev["cifrados_distintos"] = a["sha256"] != b["sha256"]
    # D1: el mismo archivo (o un enlace duro) pasado dos veces no son dos copias.
    ev["archivos_distintos"] = not os.path.samefile(args.copia_a, args.copia_b)
    ev["medios_declarados_distintos"] = bool(a["medio_declarado"]) and bool(b["medio_declarado"]) and a["medio_declarado"].lower() != b["medio_declarado"].lower()
    rb = json.load(open(args.restauracion_b, encoding="utf-8"))
    ev["restauracion_b"] = rb
    ev["restauracion_b_corresponde_a_copia_b"] = rb.get("etiqueta_copia") == "B" and rb.get("copia_sha256") == b["sha256"]
    if args.restauracion_a:
        ra = json.load(open(args.restauracion_a, encoding="utf-8"))
        ev["restauracion_a"] = ra
        ev["restauracion_a_corresponde_a_copia_a"] = ra.get("etiqueta_copia") == "A" and ra.get("copia_sha256") == a["sha256"]
    ev["declaraciones_del_propietario"] = {"conserva_ambas_copias": bool(args.confirmo_conservar), "texto": "Declaración del propietario; no verificable por script."}
    aqui = os.path.dirname(os.path.abspath(__file__))
    ev["scripts_sha256"], dif = comun.comprobar_scripts(aqui)
    ev["scripts_integros"] = not dif
    ev["resultado"] = ("COMPLETA" if all([a["cifrada_detectada"], b["cifrada_detectada"], ev["medios_declarados_distintos"],
                                          ev["cifrados_distintos"], ev["archivos_distintos"], ev["scripts_integros"],
                                          ev["restauracion_b_corresponde_a_copia_b"], rb.get("restauracion_ok") is True,
                                          args.confirmo_conservar]) else "INCOMPLETA")
    with open(args.salida, "wb") as f:
        f.write(comun.json_estable(ev))
    print("Evidencia de copias:", ev["resultado"], file=sys.stderr)
    return 0 if ev["resultado"] == "COMPLETA" else 1


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    e = sub.add_parser("empaquetar")
    e.add_argument("--dir", required=True)
    e.add_argument("--salida", required=True)
    r = sub.add_parser("restaurar")
    r.add_argument("--copia", required=True)
    r.add_argument("--etiqueta", required=True, choices=["A", "B"])
    r.add_argument("--destino", required=True)
    r.add_argument("--empaquetado", required=True)
    r.add_argument("--identidad", help="age: archivo de identidad (si cifraste con clave en vez de frase)")
    r.add_argument("--frase-archivo", help="gpg: archivo con la frase (solo automatización/pruebas; normalmente te la pide gpg)")
    r.add_argument("--ref-objetos")
    r.add_argument("--ref-tablas")
    r.add_argument("--salida", required=True)
    g = sub.add_parser("registrar")
    g.add_argument("--copia-a", required=True)
    g.add_argument("--copia-b", required=True)
    g.add_argument("--medio-a", required=True, help="texto libre sin datos sensibles, p. ej. «disco USB en casa»")
    g.add_argument("--medio-b", required=True)
    g.add_argument("--restauracion-b", required=True)
    g.add_argument("--restauracion-a")
    g.add_argument("--confirmo-conservar", action="store_true")
    g.add_argument("--salida", default="EVIDENCIA-COPIAS.json")
    a = ap.parse_args(argv)
    if a.cmd == "empaquetar":
        return empaquetar(a.dir, a.salida)
    if a.cmd == "restaurar":
        return restaurar(a.copia, a.etiqueta, a.destino, a.empaquetado, a.identidad, a.frase_archivo, a.ref_objetos, a.ref_tablas, a.salida)
    return registrar(a)


if __name__ == "__main__":
    sys.exit(main())
