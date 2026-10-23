#!/usr/bin/env python3
"""Exporta el calendario de videos a la página `calendario-…/` de checa-web.

Uso:  python3 -I video/scripts/exportar_calendario.py /ruta/a/checa-web/calendario-d4f81c

Lee `marketing/plan/calendario.md` (tablas CHECA y PERSONAL) y, por cada video con fecha,
la carpeta `marketing/videos/<CUENTA>/NN - Nombre/`: copy.txt, dm.txt, portada.jpg y video.mp4.
Escribe en el destino:
  data.json   un objeto por video (cuenta, número, fecha programada, estado, copy, dm, rutas)
  t/<C|P>NN.jpg   portada en miniatura
  v/<C|P>NN.mp4   video final (solo si no está «en proceso»; los pendientes se rehacen y no se suben)
No toca index.html (vive en checa-web). Después: commit y push en checa-web.
Estado: ✅ en Publicado → pub; ✅ en Hecho → listo; si no → proceso.
"""
import json, os, re, shutil, subprocess, sys

RAIZ = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
CAL = os.path.join(RAIZ, "marketing", "plan", "calendario.md")
VID = os.path.join(RAIZ, "marketing", "videos")
MESES = {m: i + 1 for i, m in enumerate("ene feb mar abr may jun jul ago sep oct nov dic".split())}


def anio(mes):
    return 2027 if mes <= 3 else 2026


def fecha(txt):
    m = re.search(r"(\d{1,2})\s+(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)", txt)
    if not m:
        return None
    d, mes = int(m.group(1)), MESES[m.group(2)]
    return f"{anio(mes)}-{mes:02d}-{d:02d}"


def filas():
    cuenta = None
    for linea in open(CAL, encoding="utf-8"):
        if linea.startswith("# CHECA"):
            cuenta = "C"
        elif linea.startswith("# PERSONAL"):
            cuenta = "P"
        elif cuenta and linea.startswith("|") and re.match(r"\|\s*\d+\s*\|", linea):
            c = [x.strip() for x in linea.strip().strip("|").split("|")]
            if len(c) < 6:
                continue
            num, fe, tipo, carpeta, hecho, pub = c[:6]
            f = fecha(fe)
            m = re.search(r"`([^`]+)`", carpeta)
            if not f or not m or "descartado" in tipo.lower() and "publicado" not in tipo.lower():
                continue
            yield cuenta, num.zfill(2), f, re.split(r"\s*\(", tipo)[0], m.group(1), "✅" in hecho, "✅" in pub, pub


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    dest = os.path.abspath(sys.argv[1])
    os.makedirs(os.path.join(dest, "t"), exist_ok=True)
    os.makedirs(os.path.join(dest, "v"), exist_ok=True)
    out = []
    for cuenta, num, f, tipo, carpeta, hecho, pub, pubtxt in filas():
        d = os.path.join(VID, "CHECA" if cuenta == "C" else "PERSONAL", carpeta)
        if not os.path.isdir(d):
            continue
        rd = lambda n: open(os.path.join(d, n), encoding="utf-8").read().strip() if os.path.exists(os.path.join(d, n)) else None
        estado = "pub" if pub else ("listo" if hecho else "proceso")
        clave = f"{cuenta}{num}"
        th = vd = None
        pj = os.path.join(d, "portada.jpg")
        if os.path.exists(pj):
            subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", pj, "-vf", "scale=216:384", "-q:v", "5", os.path.join(dest, "t", clave + ".jpg")], check=True)
            th = f"t/{clave}.jpg"
        mp = os.path.join(d, "video.mp4")
        peso = None
        if os.path.exists(mp) and estado != "proceso":
            shutil.copyfile(mp, os.path.join(dest, "v", clave + ".mp4"))
            vd = f"v/{clave}.mp4"
            peso = round(os.path.getsize(mp) / 1048576, 1)
        out.append(dict(c=cuenta, n=num, t=carpeta.split(" - ", 1)[1], f=f, e=estado, p=fecha(pubtxt) if pub else None, tipo=tipo,
                        copy=rd("copy.txt"), dm=rd("dm.txt"), th=th, v=vd, mb=peso,
                        files=dict(portada=th is not None, video=os.path.exists(mp), copy=rd("copy.txt") is not None, dm=rd("dm.txt") is not None)))
    json.dump(out, open(os.path.join(dest, "data.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"{len(out)} videos, {sum(1 for o in out if o['v'])} con archivo → {dest}")


if __name__ == "__main__":
    main()
