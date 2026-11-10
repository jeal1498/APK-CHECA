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
for i in (2, 4, 5, 7): ev.append(("whoosh", int(T["scenes"][i - 1]["start"] * 1000), .4))
ms = lambda s: int(s * 1000)
ev += [("thump", ms(t(1, "ganaste")), .6), ("rechazar", ms(t(2, "400")), .5), ("numero-sube", ms(t(2, "gasolina")), .4),
       ("thump", ms(t(3, "desgaste")), .5), ("sello", ms(t(4, "mismo")), .55), ("aceptar", ms(t(4, "ganar")), .4),
       ("sello", ms(t(5, "ciegas")), .5), ("popup-entra", ms(t(7, "checa")), .5), ("whoosh", ms(t(7, "32")), .35),
       ("sello", ms(t(8, "118")), .45), ("considerar", ms(t(9, "decides")), .45),
       ("logo-semaforo", ms(T["scenes"][9]["start"] + .15), .7)]
ev.sort(key=lambda e: e[1])
json.dump({"duracion": T["total"], "fade_out": [round(T["total"] - .25, 2), .25], "eventos": [list(e) for e in ev]}, open(os.path.join(AQUI, "sonidos.json"), "w"), indent=1)
print("total", T["total"])
