"""Arma un video completo de Checa en MARCA V2 desde SCRIPT.md y escenas.json.

Uso (dentro de la carpeta del video, copiada de checa-marca-v2):
  python3 -I construir.py            # voz (si falta), tiempos, escenas, subtítulos e index.html
  python3 -I construir.py --check    # además corre `hyperframes check`
  python3 -I construir.py --render   # además renderiza a renders/video.mp4

SCRIPT.md: una línea de voz por escena, con 4 espacios al inicio.
escenas.json: lista con un objeto por línea, en el mismo orden. Tipos:
  {"tipo": "gancho", "lineas": ["¿Aceptas", "viajes sin", "hacer la", "cuenta?"], "resalta": 3}
      Frase grande que entra por líneas (cada línea aparece en la palabra donde empieza) + raya de carretera.
      "resalta": índice de la línea en amarillo (por defecto la última).
  {"tipo": "numero", "etiqueta": "te quedan", "numero": "$97", "pie": "Números de ejemplo", "color": "verde|amarillo|rojo|hueso"}
  {"tipo": "popup", "popup": POPUP, "ejemplo": true}
  {"tipo": "estados", "popups": [POPUP, POPUP, POPUP], "en_palabra": [0, 1, 3], "texto": "Tú decides.", "texto_en": 4}
      Tres pop-ups del mismo tamaño que se reemplazan en el mismo lugar.
  {"tipo": "cierre"}   (pantalla final estándar: logo animado, Descarga Checa, cuentas y link)
  POPUP = {"nivel": "verde|amarillo|rojo", "app": "UBER", "neta": 118, "viaje": 150, "gasolina": 32,
           "chips": [["$321/h", "por hora", "ok"], ["$9.4/km", "por km", "ok"], ["28 min", "16 km total", ""]]}
          (chip: "ok" cumple, "mal" no cumple, "" neutro). Las cifras deben cuadrar: neta = viaje − gasolina.

Marca: jeal1498/checa-web, carpeta marca/ (skill marca-checa). Reglas: ningún texto es solo un monto,
los tres pop-ups miden lo mismo, «Números de ejemplo» donde haya cifras, sin logos de Uber/DiDi.
La voz es Fernando Martínez de ElevenLabs (clave en ELEVENLABS_API_KEY); no vuelve a pedir una línea si ya existe assets/voice/NN.mp3.
"""
import json, os, re, subprocess, sys

AQUI = os.path.dirname(os.path.abspath(__file__))
os.chdir(AQUI)
ROOT = os.path.abspath(os.path.join(AQUI, "..", "..", ".."))
EL = os.path.join(ROOT, "video", "scripts", "elevenlabs.py")
VOZ_ID = "dlGxemPxFMTY7iXagmOj"
PAD, PAD_FIN = 0.15, 2.0
BANDA_TOP = 1160  # los subtítulos ocupan y = 1160..1480; nada del cuadro va ahí

FONTS = """@font-face{ font-family: 'Bricolage Grotesque'; font-weight: 800; font-display: block; src: url('assets/fonts/bricolage-grotesque-latin-800-normal.woff2') format('woff2'); }
    @font-face{ font-family: 'DM Sans'; font-weight: 400; font-display: block; src: url('assets/fonts/dm-sans-latin-400-normal.woff2') format('woff2'); }
    @font-face{ font-family: 'DM Sans'; font-weight: 700; font-display: block; src: url('assets/fonts/dm-sans-latin-700-normal.woff2') format('woff2'); }
    @font-face{ font-family: 'JetBrains Mono'; font-weight: 500; font-display: block; src: url('assets/fonts/jetbrains-mono-latin-500-normal.woff2') format('woff2'); }
    @font-face{ font-family: 'JetBrains Mono'; font-weight: 700; font-display: block; src: url('assets/fonts/jetbrains-mono-latin-700-normal.woff2') format('woff2'); }"""
BASE = """#root { position: absolute; inset: 0; width: 1080px; height: 1920px; overflow: hidden; font-family: 'DM Sans', Arial, sans-serif; color: #F4F4EF; }
    .clip { position: absolute; }
    .G { inset: 0; background: #0C0C10; }
    .etq { position: absolute; left: 0; right: 0; text-align: center; font-family: 'JetBrains Mono', monospace; font-weight: 700; font-size: 28px; letter-spacing: .12em; text-transform: uppercase; color: #A3A3AD; }
    .mask { position: absolute; left: 0; right: 0; overflow: hidden; text-align: center; }
    .mask > div { font-family: 'Bricolage Grotesque', 'Arial Black', sans-serif; font-weight: 800; letter-spacing: -0.045em; line-height: 1; white-space: nowrap; }"""
GSAP = '<script src="https://cdn.jsdelivr.net/npm/gsap@3.14.2/dist/gsap.min.js"></script>'
COLOR = {"verde": "#22C55E", "amarillo": "#FACC15", "rojo": "#EF4444", "hueso": "#F4F4EF"}
ACCION = {"verde": "ACEPTAR", "amarillo": "CONSIDERAR", "rojo": "RECHAZAR"}


# ---------- voz y tiempos ----------
def leer_guion():
    return [l.strip() for l in open("SCRIPT.md", encoding="utf-8") if l.startswith("    ")]


def probe(f):
    return round(float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f]).strip()), 3)


def tiempos(lineas):
    os.makedirs("assets/voice", exist_ok=True)
    out, t = [], 0.0
    for n, txt in enumerate(lineas, 1):
        b = f"assets/voice/{n:02d}"
        if not os.path.exists(b + ".mp3"):
            subprocess.run(["python3", "-I", EL, "voz", "assets/voice", f"{n:02d}", txt, VOZ_ID, "1.0"], check=True)
        a = json.load(open(b + ".json"))
        ch, st, en = a["c"], a["s"], a["e"]
        words, cur, s0, e0 = [], "", None, None
        for c, s, e in zip(ch, st, en):
            if c.isspace():
                if cur:
                    words.append((cur, s0, e0)); cur, s0 = "", None
            else:
                if s0 is None:
                    s0 = s
                cur += c; e0 = e
        if cur:
            words.append((cur, s0, e0))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", b + ".mp3", "-t", str(round(en[-1] + 0.12, 3)), "-ar", "44100", "-ac", "1", b + ".wav"], check=True)
        voz = probe(b + ".wav")
        dur = round(voz + (PAD_FIN if n == len(lineas) else PAD), 3)
        out.append({"n": n, "start": round(t, 3), "dur": dur, "voice": voz,
                    "words": [(w, round(s, 2), round(e, 2)) for w, s, e in words]})
        t += dur
    return {"total": round(t, 3), "scenes": out}


# ---------- componentes ----------
def css_popup():
    s = open("assets/checa-popup.html", encoding="utf-8").read()
    return re.search(r"<style>(.*?)</style>", s, re.S).group(1)


def popup(p, u=1.0):
    nivel = p.get("nivel", "verde")
    chips = "".join(f'<div class="cp-ficha{(" cp-" + c) if c else ""}"><b>{a}</b><span>{b}</span></div>' for a, b, c in p["chips"])
    et = '<span class="cp-etiqueta">MEJOR</span>' if p.get("mejor") else ""
    mejor = " cp-mejor" if p.get("mejor") else ""
    return (f'<div class="cp cp-{nivel}{mejor}" style="--u:{u}">{et}'
            f'<div class="cp-banda"><div class="cp-lado"><span class="cp-app">{p.get("app", "UBER")}</span></div>'
            f'<div class="cp-accion">{ACCION[nivel]}</div>'
            f'<div class="cp-lado der"><svg width="44" height="44" viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6 L18 18 M18 6 L6 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg></div></div>'
            f'<div class="cp-cuerpo"><div class="cp-neta">${p["neta"]}<small>&nbsp;netos</small></div>'
            f'<div class="cp-linea">Viaje ${p["viaje"]}&nbsp;·&nbsp;Gasolina −${p["gasolina"]}</div>'
            f'<div class="cp-fichas">{chips}</div></div></div>')


def validar_popup(p):
    assert p["viaje"] - p["gasolina"] == p["neta"], f"La neta no cuadra: {p['viaje']} − {p['gasolina']} ≠ {p['neta']}"


def marco(nombre, D, style, inner, js):
    return f"""<template>
  <style>
    {FONTS}
    {BASE}
    {style}
  </style>

  <div id="root" data-composition-id="{nombre}" data-width="1080" data-height="1920" data-start="0" data-duration="{D}">
    <div id="{nombre}-g" class="clip G" data-start="0" data-duration="{D}" data-track-index="0"></div>
{inner}
  </div>

  {GSAP}
  <script>
    (function () {{
      window.__timelines = window.__timelines || {{}};
      var tl = gsap.timeline({{ paused: true }});
      var D = {D};
{js}
      window.__timelines["{nombre}"] = tl;
    }})();
  </script>
</template>
"""


# ---------- escenas ----------
def esc_gancho(sc, e, nombre):
    D, w = sc["dur"], sc["words"]
    lineas = e["lineas"]; res = e.get("resalta", len(lineas) - 1)
    inicio, k = [], 0
    for l in lineas:
        inicio.append(min(k, len(w) - 1)); k += len(l.split())
    alto = 176
    inner = "".join(f'    <div id="{nombre}-q{i}" class="mask clip" style="top:{300 + i * alto}px;height:{alto}px" data-start="0" data-duration="{D}" data-track-index="{i + 1}"><div style="font-size:170px;{"color:#FACC15;" if i == res else ""}">{l}</div></div>\n' for i, l in enumerate(lineas))
    y = 300 + len(lineas) * alto + 40
    inner += f'    <svg id="{nombre}-raya" class="clip" style="left:0;top:{y}px" width="1080" height="120" viewBox="0 0 1080 120" data-start="0" data-duration="{D}" data-track-index="9" aria-hidden="true"><defs><clipPath id="{nombre}-rc"><rect id="{nombre}-rcr" x="0" y="0" width="0" height="120"/></clipPath></defs><g clip-path="url(#{nombre}-rc)"><path d="M-20 90C220 90 300 20 540 28C780 36 860 100 1100 70" fill="none" stroke="#FACC15" stroke-width="8" stroke-dasharray="2 22" stroke-linecap="round"/><circle cx="540" cy="28" r="14" fill="#22C55E"/></g></svg>'
    js = "".join(f'      tl.fromTo("#{nombre}-q{i} > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.45, ease: "power4.out" }}, {max(0, round(w[inicio[i]][1] - 0.05, 2))});\n' for i in range(len(lineas)))
    js += f'      tl.fromTo("#{nombre}-rcr", {{ attr: {{ width: 0 }} }}, {{ attr: {{ width: 1080 }}, duration: 1.4, ease: "power2.out" }}, 0.3);\n'
    return marco(nombre, D, "", inner, js)


def esc_numero(sc, e, nombre):
    D = sc["dur"]; col = COLOR[e.get("color", "hueso")]
    inner = f"""    <div id="{nombre}-k" class="etq clip" style="top:420px" data-start="0" data-duration="{D}" data-track-index="1">{e["etiqueta"]}</div>
    <div id="{nombre}-n" class="mask clip" style="top:500px;height:340px" data-start="0" data-duration="{D}" data-track-index="2"><div style="font-size:300px;color:{col}">{e["numero"]}</div></div>
    <div class="etq clip" style="top:900px" data-start="0" data-duration="{D}" data-track-index="3">{e.get("pie", "Números de ejemplo")}</div>"""
    js = f'      tl.fromTo("#{nombre}-k", {{ opacity: 0, y: 20 }}, {{ opacity: 1, y: 0, duration: 0.3 }}, 0);\n      tl.fromTo("#{nombre}-n > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.5, ease: "power4.out" }}, 0.2);\n'
    return marco(nombre, D, "", inner, js)


def esc_popup(sc, e, nombre):
    D = sc["dur"]; validar_popup(e["popup"])
    inner = f'    <div id="{nombre}-p" class="clip" style="left:110px;top:340px" data-start="0" data-duration="{D}" data-track-index="1">{popup(e["popup"])}</div>\n'
    if e.get("ejemplo", True):
        inner += f'    <div class="etq clip" style="top:1040px" data-start="0" data-duration="{D}" data-track-index="2">Números de ejemplo</div>'
    js = f'      tl.fromTo("#{nombre}-p", {{ y: 90, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.45, ease: "power4.out" }}, 0);\n      tl.fromTo("#{nombre}-p", {{ scale: 1 }}, {{ scale: 1.04, duration: D, ease: "none" }}, 0);\n'
    return marco(nombre, D, css_popup(), inner, js)


def esc_estados(sc, e, nombre):
    D, w = sc["dur"], sc["words"]; ps = e["popups"]
    for p in ps:
        validar_popup(p)
    tiempo = [0.0 if i == 0 else max(0, round(w[min(k, len(w) - 1)][1] - 0.05, 2)) for i, k in enumerate(e["en_palabra"])]
    inner = "".join(f'    <div id="{nombre}-p{i}" class="clip" style="left:131px;top:300px" data-start="0" data-duration="{D}" data-track-index="{i + 1}">{popup(p, 0.95)}</div>\n' for i, p in enumerate(ps))
    js = "".join(f'      tl.set("#{nombre}-p{i}", {{ opacity: 0 }}, 0);\n' for i in range(1, len(ps)))
    for i, t in enumerate(tiempo):
        js += f'      tl.fromTo("#{nombre}-p{i}", {{ y: 60, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.3, ease: "power4.out" }}, {t});\n'
        if i + 1 < len(tiempo):
            js += f'      tl.to("#{nombre}-p{i}", {{ opacity: 0, duration: 0.12 }}, {round(tiempo[i + 1] - 0.02, 2)});\n'
    if e.get("texto"):
        inner += f'    <div id="{nombre}-t" class="mask clip" style="top:960px;height:130px" data-start="0" data-duration="{D}" data-track-index="9"><div style="font-size:120px">{e["texto"]}</div></div>'
        js += f'      tl.fromTo("#{nombre}-t > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.45, ease: "power4.out" }}, {max(0, round(w[min(e.get("texto_en", 0), len(w) - 1)][1] - 0.05, 2))});\n'
    return marco(nombre, D, css_popup(), inner, js)


def esc_cierre(sc, e, nombre):
    D = sc["dur"]
    logo = '<svg id="N-lg" width="150" height="250" viewBox="0 0 60 100" aria-hidden="true"><rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" stroke-width="1.6"/><circle cx="30" cy="26" r="9" fill="#4A2222"/><circle cx="30" cy="50" r="9" fill="#4A3C16"/><circle cx="30" cy="74" r="12" fill="#1D3A27"/><circle id="N-lr" cx="30" cy="26" r="9" fill="#EF4444" opacity="0"/><circle id="N-la" cx="30" cy="50" r="9" fill="#FACC15" opacity="0"/><circle id="N-lv" cx="30" cy="74" r="12" fill="#22C55E" opacity="0"/><path id="N-lk" d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke="#04210F" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="20" stroke-dashoffset="20"/></svg>'.replace("N-", nombre + "-")
    inner = f"""    <div id="{nombre}-lgw" class="clip" style="left:465px;top:190px" data-start="0" data-duration="{D}" data-track-index="1">{logo}</div>
    <div id="{nombre}-nom" class="mask clip" style="top:470px;height:150px" data-start="0" data-duration="{D}" data-track-index="2"><div style="font-size:140px">Checa</div></div>
    <div id="{nombre}-pil" class="clip" style="left:290px;top:660px;width:500px;height:92px;border-radius:999px;background:#F4F4EF;color:#0C0C10;display:flex;align-items:center;justify-content:center;font-family:'DM Sans',sans-serif;font-weight:700;font-size:44px" data-start="0" data-duration="{D}" data-track-index="3">Descarga Checa</div>
    <div id="{nombre}-sa" class="etq clip" style="top:780px" data-start="0" data-duration="{D}" data-track-index="4">Solo Android</div>
    <div id="{nombre}-cu" class="clip" style="left:140px;right:140px;top:840px;text-align:center;font-family:'DM Sans',sans-serif;font-size:34px;line-height:1.5;color:#A3A3AD" data-start="0" data-duration="{D}" data-track-index="5">síguenos en tiktok<br><b style="font-family:'JetBrains Mono',monospace;font-size:42px;color:#F4F4EF">@checa.driver</b><br>creador de Checa<br><b style="font-family:'JetBrains Mono',monospace;font-size:42px;color:#F4F4EF">@Eduardoalv9802</b></div>
    <div id="{nombre}-li" class="clip" style="left:0;right:0;top:1100px;text-align:center;font-family:'JetBrains Mono',monospace;font-weight:700;font-size:36px;color:#FACC15" data-start="0" data-duration="{D}" data-track-index="6">jeal1498.github.io/checa-web</div>"""
    n = nombre
    js = f"""      tl.fromTo("#{n}-lgw", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power4.out" }}, 0.0);
      tl.set("#{n}-lr", {{ opacity: 1 }}, 0.1); tl.set("#{n}-lr", {{ opacity: 0 }}, 0.4);
      tl.set("#{n}-la", {{ opacity: 1 }}, 0.4); tl.set("#{n}-la", {{ opacity: 0 }}, 0.7);
      tl.set("#{n}-lv", {{ opacity: 1 }}, 0.7);
      tl.to("#{n}-lk", {{ strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }}, 0.9);
      tl.fromTo("#{n}-nom > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.45, ease: "power4.out" }}, 1.2);
      tl.fromTo("#{n}-pil", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 1.3);
      tl.fromTo("#{n}-sa", {{ opacity: 0 }}, {{ opacity: 1, duration: 0.3 }}, 1.6);
      tl.fromTo("#{n}-cu", {{ opacity: 0, y: 20 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 2.1);
      tl.fromTo("#{n}-li", {{ opacity: 0 }}, {{ opacity: 1, duration: 0.4 }}, 2.6);
"""
    return marco(nombre, D, "", inner, js)


TIPOS = {"gancho": esc_gancho, "numero": esc_numero, "popup": esc_popup, "estados": esc_estados, "cierre": esc_cierre}


# ---------- subtítulos ----------
def grupos_subtitulo(palabras):
    """Frases por puntuación (máx. 5 palabras por pantalla); una palabra suelta se une a la siguiente."""
    frases, cur = [], []
    for w in palabras:
        cur.append(w)
        if re.search(r"[.,;:?!]$", w[0]):
            frases.append(cur); cur = []
    if cur:
        frases.append(cur)
    unidas = []
    for f in frases:
        if unidas and len(unidas[-1]) == 1 and len(unidas[-1]) + len(f) <= 5:
            unidas[-1] = unidas[-1] + f
        else:
            unidas.append(f)
    res = []
    for f in unidas:
        k = -(-len(f) // 5)
        t = -(-len(f) // k)
        res += [f[i:i + t] for i in range(0, len(f), t)]
    return res


def armar_captions(T):
    groups = []
    for sc in T["scenes"]:
        for ch in grupos_subtitulo(sc["words"]):
            g = len(groups)
            words = [{"id": f"caption-word-{g}-{i}", "text": w[0], "start": round(sc["start"] + w[1], 3), "end": round(sc["start"] + w[2], 3)} for i, w in enumerate(ch)]
            groups.append({"id": f"caption-group-{g}", "frame": sc["n"], "start": words[0]["start"], "end": round(words[-1]["end"] + 0.25, 3),
                           "text": " ".join(x["text"] for x in words), "words": words})
    cap = open("compositions/captions.html", encoding="utf-8").read()
    cap = re.sub(r"var GROUPS = \[.*?\];\n", lambda m: "var GROUPS = " + json.dumps(groups, ensure_ascii=False, separators=(",", ":")) + ";\n", cap, count=1, flags=re.S)
    cap = re.sub(r"var DURATION = [0-9.]+;", f"var DURATION = {T['total']};", cap, count=1)
    cap = re.sub(r'(id="captions-root"[^>]*?data-duration=")[0-9.]+(")', lambda m: m.group(1) + str(T["total"]) + m.group(2), cap, count=1, flags=re.S)
    open("compositions/captions.html", "w", encoding="utf-8").write(cap)
    return groups


# ---------- index ----------
def armar_index(T, nombres):
    h = f"""<!doctype html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=1080, height=1920" />
    {GSAP}
    <style>
      * {{ margin: 0; padding: 0; box-sizing: border-box; }}
      html, body {{ width: 1080px; height: 1920px; overflow: hidden; background: #0C0C10; }}
      #root {{ position: relative; width: 1080px; height: 1920px; overflow: hidden; background: #0C0C10; }}
      .scene {{ position: absolute; inset: 0; width: 100%; height: 100%; }}
    </style>
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="{T['total']}" data-width="1080" data-height="1920">
"""
    for sc, nm in zip(T["scenes"], nombres):
        n = sc["n"]
        h += f'''      <div id="el-{nm}" class="scene" data-composition-id="{nm}" data-composition-src="compositions/frames/{nm}.html" data-start="{sc["start"]}" data-duration="{sc["dur"]}" data-track-index="{n % 2}"></div>
      <audio id="el-{nm}-voice" data-audio-group="voiceover" src="assets/voice/{n:02d}.wav" data-start="{sc["start"]}" data-duration="{sc["voice"]}" data-track-index="10" data-volume="1"></audio>
'''
    h += f'''      <div id="el-captions" class="scene" data-composition-id="captions" data-composition-src="compositions/captions.html" data-start="0" data-duration="{T['total']}" data-track-index="20" data-track-kind="captions"></div>
    </div>
    <script>
      window.__timelines = window.__timelines || {{}};
      window.__timelines["main"] = gsap.timeline({{ paused: true }});
      window.__timelines["main"].to({{}}, {{ duration: {T['total']} }}, 0);
    </script>
  </body>
</html>
'''
    open("index.html", "w", encoding="utf-8").write(h)


def main():
    args = sys.argv[1:]
    lineas = leer_guion()
    escenas = json.load(open("escenas.json", encoding="utf-8"))
    assert len(lineas) == len(escenas), f"SCRIPT.md tiene {len(lineas)} líneas y escenas.json {len(escenas)} escenas"
    assert escenas[-1]["tipo"] == "cierre", "La última escena debe ser el cierre estándar"
    T = tiempos(lineas)
    json.dump(T, open("tiempos.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    os.makedirs("compositions/frames", exist_ok=True)
    for f in os.listdir("compositions/frames"):
        if f.endswith(".html"):
            os.remove(os.path.join("compositions/frames", f))
    nombres = []
    for sc, e in zip(T["scenes"], escenas):
        nm = f"e{sc['n']:02d}-{e['tipo']}"  # los ids deben empezar con letra (selectores CSS)
        nombres.append(nm)
        open(f"compositions/frames/{nm}.html", "w", encoding="utf-8").write(TIPOS[e["tipo"]](sc, e, nm))
    g = armar_captions(T)
    armar_index(T, nombres)
    print(f"{len(T['scenes'])} escenas, {len(g)} subtítulos, {T['total']} s")
    if "--check" in args or "--render" in args:
        subprocess.run(["npx", "--yes", "hyperframes@0.8.154", "check"], check=True)
    if "--render" in args:
        os.makedirs("renders", exist_ok=True)
        subprocess.run(["npx", "--yes", "hyperframes@0.8.154", "render", "--quality", "high", "--output", "renders/video.mp4"], check=True)


if __name__ == "__main__":
    main()
