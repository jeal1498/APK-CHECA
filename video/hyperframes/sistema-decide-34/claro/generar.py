"""Genera index.html y sonidos.json del estilo claro (referencia: mapa de nodos). Uso: python3 -I generar.py"""
import json, os
AQUI = os.path.dirname(os.path.abspath(__file__))
T = json.load(open(os.path.join(AQUI, "..", "tiempos.json"), encoding="utf-8"))
fin = max(s["start"] + s["words"][-1][2] for s in T["scenes"])
T["total"] = round(fin + 1.65, 1)
F = "../../_base/assets/fonts/"
css = "".join(f'@font-face{{font-family:"{n}";font-weight:{w};src:url({F}{f}-latin-{w}-normal.woff2)}}' for n, f, w in [
    ("Bricolage Grotesque", "bricolage-grotesque", 800), ("DM Sans", "dm-sans", 400), ("DM Sans", "dm-sans", 500), ("DM Sans", "dm-sans", 700),
    ("JetBrains Mono", "jetbrains-mono", 500), ("JetBrains Mono", "jetbrains-mono", 700)])
js = open(os.path.join(AQUI, "pagina.js"), encoding="utf-8").read()
html = ('<!doctype html><html><head><meta charset="utf-8"><style>' + css +
        '*{box-sizing:border-box;margin:0;padding:0}html,body{width:1080px;height:1920px;overflow:hidden;background:#fff;font-family:"DM Sans",sans-serif}'
        '#st{position:absolute;inset:0;overflow:hidden}</style></head><body><div id="st"></div><script>\nconst D=' + json.dumps(T, ensure_ascii=False) +
        ';\n' + js + '</script></body></html>')
open(os.path.join(AQUI, "index.html"), "w", encoding="utf-8").write(html)
# sonidos: [nombre, ms, volumen]
def t(i, w):
    sc = T["scenes"][i - 1]
    for x, a, b in sc["words"]:
        if x.lower().strip(":,.") == w: return sc["start"] + a
    raise SystemExit(f"{w} no está en la línea {i}")
ev = [("thump", 0, .8)]
for i in (2, 3, 5, 8): ev.append(("whoosh", int(T["scenes"][i - 1]["start"] * 1000), .4))
ev += [("popup-entra", int((t(4, "144") - .05) * 1000), .4), ("rechazar", int(t(4, "rechazar") * 1000), .5), ("sello", int(t(6, "gana") * 1000), .5),
       ("aceptar", int(t(7, "aceptar") * 1000), .5), ("numero-sube", int(t(8, "día") * 1000), .4), ("sello", int(t(9, "suposiciones") * 1000), .45),
       ("logo-semaforo", int(T["scenes"][9]["start"] * 1000 + 150), .7)]
ev.sort(key=lambda e: e[1])
json.dump({"duracion": T["total"], "fade_out": [round(T["total"] - .25, 2), .25], "eventos": [list(e) for e in ev]}, open(os.path.join(AQUI, "sonidos.json"), "w"), indent=1)
print("total", T["total"])
