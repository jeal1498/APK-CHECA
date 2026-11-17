#!/usr/bin/env python3
"""Comprueba el enlace línea ↔ captura por HUELLA (10.82). Solo lectura; no sube ni modifica nada.
  python3 -I verificar_enlace_capturas.py ENLACES.json CARPETA_CAPTURAS [SALIDA.json]
ENLACES.json: lista de {"caso_id": 123, "captura_sha256": "<64 hex>"} (de auditoria.enlace_capturas / admin_error.enlace).
CARPETA_CAPTURAS: imágenes «<caso_id>.webp» bajadas de Storage (traen datos de pasajeros: no se suben a ningún lado).
Resultados por caso: coincide (la imagen es la que leyó el lector), distinta (otra imagen: NO enlazar), sin_imagen, sin_huella."""
import hashlib, json, os, sys


def verificar(enlaces, carpeta):
    out = []
    for e in enlaces:
        cid, sha = e["caso_id"], e.get("captura_sha256")
        ruta = os.path.join(carpeta, f"{cid}.webp")
        if not sha:
            r = "sin_huella"
        elif not os.path.isfile(ruta):
            r = "sin_imagen"
        else:
            r = "coincide" if hashlib.sha256(open(ruta, "rb").read()).hexdigest() == sha else "distinta"
        out.append({"caso_id": cid, "resultado": r})
    return out


if __name__ == "__main__":
    res = verificar(json.load(open(sys.argv[1], encoding="utf-8")), sys.argv[2])
    cuenta = {}
    for x in res:
        cuenta[x["resultado"]] = cuenta.get(x["resultado"], 0) + 1
    print(json.dumps(cuenta, sort_keys=True))
    if len(sys.argv) > 3:
        json.dump(res, open(sys.argv[3], "w", encoding="utf-8"), indent=2)
