"""Arma un video completo de Checa en MARCA V2 desde SCRIPT.md y escenas.json.

Uso (dentro de la carpeta del video, copiada de checa-marca-v2):
  python3 -I construir.py            # voz (si falta), tiempos, escenas, subtítulos e index.html
  python3 -I construir.py --check    # además corre `hyperframes check`
  python3 -I construir.py --render   # además renderiza a renders/video.mp4
  python3 -I construir.py --sin-sfx  # sin efectos de sonido (por defecto se agregan los oficiales)

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

Efectos de sonido oficiales (assets/sfx, de checa-web/marca/sonido): se agregan solos según el tipo de escena:
  popup → popup-entra y el sonido de su nivel (aceptar / considerar / rechazar) · estados → el sonido de cada nivel
  numero → numero-sube · cierre y logo → logo-semaforo. Las demás escenas no llevan sonido por defecto.
  Para cambiarlo, una escena puede traer "sfx": false  o  "sfx": [["sello", 1.2], ["popup-entra", 0.0, 0.4]]  (archivo, segundo —o palabra de la voz— dentro
  de la escena y, opcional, volumen). Reglas: un sonido por evento visual, máximo 3 por escena, volumen 0.3 a 0.5 con voz.

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


# ---------- escenas adicionales (v2 del 08, 17 oct 2026) ----------
def pal(sc, k, defecto=None):
    """Segundo en que entra algo ligado a una palabra (0.25 s antes, para que se sienta sincronizado)."""
    w = sc["words"]
    if isinstance(k, int):
        i = k
    else:
        n = re.sub(r"[^\w]", "", k.lower())
        i = next((j for j, x in enumerate(w) if re.sub(r"[^\w]", "", x[0].lower()).startswith(n)), None)
        assert i is not None, f"No encuentro «{k}» en la línea {sc['n']}: {[x[0] for x in w]}"
    return max(0, round(w[min(i, len(w) - 1)][1] - 0.25, 2))


ICONOS = {
    "gas": '<path d="M5 20V5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v15M4 20h11M14 9h2.5a1.5 1.5 0 0 1 1.5 1.5V16a1.5 1.5 0 0 0 3 0V8l-3-3M7 8h5"/>',
    "hora": '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    "km": '<circle cx="6" cy="18" r="2.5"/><circle cx="18" cy="6" r="2.5"/><path d="M8.5 18H15a3 3 0 0 0 0-6H9a3 3 0 0 1 0-6h6.5"/>',
    "dinero": '<rect x="3" y="6" width="18" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/>',
    "voz": '<path d="M4 9v6h4l5 4V5L8 9H4zM16.5 9a4 4 0 0 1 0 6M19 6.5a8 8 0 0 1 0 11"/>',
}


def ic(nombre, px, color="#F4F4EF"):
    return f'<svg width="{px}" height="{px}" viewBox="0 0 24 24" fill="none" stroke="{color}" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{ICONOS[nombre]}</svg>'


OF_CSS = """.of { position: relative; width: 640px; height: 420px; border-radius: 28px; background: #16161C; border: 2px solid #3A3A45; box-shadow: 0 30px 60px rgba(0,0,0,.6); }
    .of .l { position: absolute; left: 40px; top: 30px; font-family: 'JetBrains Mono', monospace; font-weight: 700; font-size: 24px; letter-spacing: .14em; color: #A3A3AD; }
    .of .f { position: absolute; left: 36px; top: 64px; font-family: 'Bricolage Grotesque', sans-serif; font-weight: 800; font-size: 150px; letter-spacing: -.05em; line-height: 1; }
    .of .d { position: absolute; left: 40px; top: 214px; font-family: 'JetBrains Mono', monospace; font-weight: 700; font-size: 28px; color: #C9C9D1; line-height: 1.5; }
    .of .b { position: absolute; left: 40px; top: 318px; background: #22C55E; color: #04210F; border-radius: 999px; padding: 10px 36px; font-family: 'Bricolage Grotesque', sans-serif; font-weight: 800; font-size: 38px; letter-spacing: .06em; }"""


def oferta():
    return '<div class="of"><div class="l">NUEVA OFERTA</div><div class="f">$120</div><div class="d">5 min (1.8 km)<br>18 min (9 km)</div><div class="b">ACEPTAR</div></div>'


def logo_svg(n, w, h):
    return f'<svg width="{w}" height="{h}" viewBox="0 0 60 100" aria-hidden="true"><rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" stroke-width="1.6"/><circle cx="30" cy="26" r="9" fill="#4A2222"/><circle cx="30" cy="50" r="9" fill="#4A3C16"/><circle cx="30" cy="74" r="12" fill="#1D3A27"/><circle id="{n}-lr" cx="30" cy="26" r="9" fill="#EF4444" opacity="0"/><circle id="{n}-la" cx="30" cy="50" r="9" fill="#FACC15" opacity="0"/><circle id="{n}-lv" cx="30" cy="74" r="12" fill="#22C55E" opacity="0"/><path id="{n}-lk" d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke="#04210F" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="20" stroke-dashoffset="20"/></svg>'


def js_logo(n, t0=0.0):
    return f"""      tl.set("#{n}-lr", {{ opacity: 1 }}, {t0 + 0.1}); tl.set("#{n}-lr", {{ opacity: 0 }}, {t0 + 0.4});
      tl.set("#{n}-la", {{ opacity: 1 }}, {t0 + 0.4}); tl.set("#{n}-la", {{ opacity: 0 }}, {t0 + 0.7});
      tl.set("#{n}-lv", {{ opacity: 1 }}, {t0 + 0.7});
      tl.to("#{n}-lk", {{ strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }}, {t0 + 0.9});
"""


def clip(i, idd, style, html, D, extra=""):
    return f'    <div id="{idd}" class="clip" style="{style}" data-start="0" data-duration="{D}" data-track-index="{i}"{extra}>{html}</div>\n'


def esc_hook(sc, e, n):
    D = sc["dur"]; tap = pal(sc, e.get("tap", "pensarlo")); cae = pal(sc, e.get("cae", "perdiste"))
    inner = clip(1, f"{n}-btn", "left:230px;top:330px;width:620px;height:150px;border-radius:999px;background:#22C55E;color:#04210F;display:flex;align-items:center;justify-content:center;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:76px;letter-spacing:.06em", "ACEPTAR", D)
    inner += clip(2, f"{n}-rip", "left:490px;top:355px;width:100px;height:100px;border-radius:50%;border:6px solid #F4F4EF;opacity:0", "", D)
    inner += clip(3, f"{n}-card", "left:140px;top:640px;width:800px;height:300px;border-radius:28px;background:#EF4444;color:#1F0000;display:flex;align-items:center;justify-content:center;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:92px;letter-spacing:-.04em;white-space:nowrap;opacity:0", "Perdiste dinero", D)
    js = f"""      tl.fromTo("#{n}-btn", {{ opacity: 0, y: -40 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.to("#{n}-btn", {{ scale: 0.94, duration: 0.1 }}, {tap}); tl.to("#{n}-btn", {{ scale: 1, duration: 0.2 }}, {round(tap + 0.1, 2)});
      tl.fromTo("#{n}-rip", {{ scale: 0.3, opacity: 0.9 }}, {{ scale: 2.4, opacity: 0, duration: 0.5, ease: "power2.out" }}, {tap});
      tl.fromTo("#{n}-card", {{ y: 120, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.35, ease: "power4.out" }}, {cae});
      tl.to("#{n}-card", {{ x: 14, duration: 0.05, yoyo: true, repeat: 5 }}, {round(cae + 0.35, 2)});
"""
    return marco(n, D, "", inner, js)


def esc_anillo(sc, e, n):
    D = sc["dur"]
    inner = clip(1, f"{n}-of", "left:220px;top:250px;width:640px;height:420px", oferta(), D)
    inner += clip(2, f"{n}-ring", "left:390px;top:740px;width:300px;height:300px", f'<svg width="300" height="300" viewBox="0 0 300 300" aria-hidden="true"><circle cx="150" cy="150" r="120" fill="none" stroke="#3A3A45" stroke-width="22"/><circle id="{n}-rp" cx="150" cy="150" r="120" fill="none" stroke="#EF4444" stroke-width="22" stroke-linecap="round" stroke-dasharray="754" stroke-dashoffset="0" transform="rotate(-90 150 150)"/></svg>', D)
    inner += f'    <div class="etq clip" style="top:1090px" data-start="0" data-duration="{D}" data-track-index="3">Números de ejemplo</div>\n'
    js = f"""      tl.fromTo("#{n}-of", {{ opacity: 0, y: 60 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-ring", {{ opacity: 0 }}, {{ opacity: 1, duration: 0.3 }}, 0.3);
      tl.to("#{n}-rp", {{ strokeDashoffset: 754, duration: {round(D - 0.8, 2)}, ease: "none" }}, 0.4);
      tl.to("#{n}-of", {{ opacity: 0.35, duration: {round(D - 1.2, 2)}, ease: "none" }}, 0.8);
"""
    return marco(n, D, OF_CSS, inner, js)


def esc_datos(sc, e, n):
    D = sc["dur"]
    filas = [("dinero", "Tarifa", "tarifa"), ("km", "Kilómetros", "kil"), ("hora", "Minutos de recogida", "minutos")]
    inner, js = "", ""
    for i, (ico, txt, pw) in enumerate(filas):
        inner += clip(i + 1, f"{n}-r{i}", f"left:140px;top:{250 + i * 150}px;width:800px;height:120px;border-radius:28px;background:#16161C;border:2px solid #3A3A45;display:flex;align-items:center;gap:28px;padding-left:40px;box-sizing:border-box;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:56px;letter-spacing:-.03em", ic(ico, 64) + f"<span>{txt}</span>", D)
        js += f'      tl.fromTo("#{n}-r{i}", {{ x: -80, opacity: 0 }}, {{ x: 0, opacity: 1, duration: 0.35, ease: "power4.out" }}, {pal(sc, pw)});\n'
    inner += clip(4, f"{n}-g", "left:140px;top:730px;width:800px;height:220px;border-radius:28px;background:#3A1414;border:3px solid #EF4444;box-sizing:border-box;padding:34px 40px", ic("gas", 70, "#EF4444") + "<span style=\"position:absolute;left:140px;top:30px;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:64px;letter-spacing:-.03em\">Gasolina</span>" + f'<div style="position:absolute;left:40px;right:40px;top:150px;height:26px;border-radius:13px;background:#3A3A45"><div style="width:8%;height:100%;border-radius:13px;background:#EF4444"></div></div>', D)
    t = pal(sc, "gasolina")
    js += f'      tl.fromTo("#{n}-g", {{ y: 60, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.4, ease: "power4.out" }}, {t});\n      tl.to("#{n}-g", {{ opacity: 0.45, duration: 0.18, yoyo: true, repeat: 5 }}, {round(t + 0.5, 2)});\n'
    return marco(n, D, "", inner, js)


def esc_logo(sc, e, n):
    D = sc["dur"]
    inner = clip(1, f"{n}-lgw", "left:420px;top:240px", logo_svg(n, 240, 400), D)
    inner += f'    <div id="{n}-nom" class="mask clip" style="top:690px;height:200px" data-start="0" data-duration="{D}" data-track-index="2"><div style="font-size:180px">Checa</div></div>\n'
    js = f'      tl.fromTo("#{n}-lgw", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power4.out" }}, 0);\n' + js_logo(n, 0.0)
    js += f'      tl.fromTo("#{n}-nom > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.45, ease: "power4.out" }}, 1.2);\n'
    return marco(n, D, "", inner, js)


def esc_escaneo(sc, e, n):
    D = sc["dur"]; ts, tp, tg, tn = pal(sc, "lee"), pal(sc, "cuánto"), pal(sc, "gasolina"), pal(sc, "descontada")
    inner = clip(1, f"{n}-of", "left:220px;top:230px;width:640px;height:420px", oferta(), D)
    inner += clip(2, f"{n}-sc", "left:200px;top:230px;width:680px;height:8px;border-radius:4px;background:#22C55E;box-shadow:0 0 30px #22C55E;opacity:0", "", D)
    fila = "position:absolute;left:44px;right:44px;display:flex;justify-content:space-between;align-items:baseline;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;letter-spacing:-.03em"
    panel = (f'<div id="{n}-v" style="{fila};top:36px;font-size:56px"><span>Viaje</span><span style="font-family:\'JetBrains Mono\',monospace;font-size:52px">$120</span></div>'
             f'<div id="{n}-ga" style="{fila};top:118px;font-size:56px;color:#EF4444"><span>Gasolina</span><span style="font-family:\'JetBrains Mono\',monospace;font-size:52px">−$22</span></div>'
             f'<div style="position:absolute;left:44px;right:44px;top:208px;height:3px;background:#3A3A45"></div>'
             f'<div id="{n}-ne" style="{fila};top:228px;font-size:64px;color:#22C55E"><span>Te quedan</span><span id="{n}-nn" style="font-size:120px">$98</span></div>')
    inner += clip(3, f"{n}-pn", "left:140px;top:700px;width:800px;height:370px;border-radius:28px;background:#16161C;border:2px solid #3A3A45", panel, D)
    inner += f'    <div class="etq clip" style="top:1100px" data-start="0" data-duration="{D}" data-track-index="4">Números de ejemplo</div>\n'
    js = f"""      tl.fromTo("#{n}-of", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-sc", {{ opacity: 1, y: 0 }}, {{ opacity: 1, y: 410, duration: 1.1, ease: "power1.inOut" }}, {ts});
      tl.to("#{n}-sc", {{ opacity: 0, duration: 0.2 }}, {round(ts + 1.1, 2)});
      tl.fromTo("#{n}-pn", {{ opacity: 0, y: 50 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, {tp});
      tl.set("#{n}-ga", {{ opacity: 0 }}, 0); tl.set("#{n}-ne", {{ opacity: 0 }}, 0);
      tl.fromTo("#{n}-ga", {{ opacity: 0, x: -40 }}, {{ opacity: 1, x: 0, duration: 0.35, ease: "power4.out" }}, {tg});
      tl.fromTo("#{n}-ne", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, {tn});
      var num = {{ v: 120 }};
      tl.fromTo(num, {{ v: 120 }}, {{ v: 98, duration: 0.6, ease: "power2.out", onUpdate: function () {{ document.getElementById("{n}-nn").textContent = "$" + Math.round(num.v); }} }}, {tn});
"""
    return marco(n, D, OF_CSS, inner, js)


def esc_voz(sc, e, n):
    D = sc["dur"]; ta, tc, tr = pal(sc, "aceptar"), pal(sc, "considerar"), pal(sc, "rechazar")
    inner = clip(1, f"{n}-sp", "left:465px;top:230px", ic("voz", 150), D)
    barras, js = "", ""
    for i in range(15):
        amp = 60 + (i * 53) % 120; dur = round(0.25 + ((i * 7) % 5) * 0.04, 2); rep = int(D / dur)
        barras += f'<div id="{n}-b{i}" style="position:absolute;left:{i * 52}px;top:{110 - 20}px;width:28px;height:40px;border-radius:14px;background:#F4F4EF"></div>'
        js += f'      tl.fromTo("#{n}-b{i}", {{ height: 40, y: 0 }}, {{ height: {amp}, y: {-(amp - 40) // 2}, duration: {dur}, yoyo: true, repeat: {rep}, ease: "sine.inOut" }}, 0);\n'
    inner += clip(2, f"{n}-bars", "left:162px;top:480px;width:756px;height:220px", barras, D)
    pills = [("ACEPTAR", "#22C55E", "#04210F", ta), ("CONSIDERAR", "#FACC15", "#2B2200", tc), ("RECHAZAR", "#EF4444", "#1F0000", tr)]
    for i, (txt, bg, fg, t) in enumerate(pills):
        inner += clip(3 + i, f"{n}-p{i}", f"left:260px;top:{770 + i * 120}px;width:560px;height:96px;border-radius:999px;background:{bg};color:{fg};display:flex;align-items:center;justify-content:center;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:50px;letter-spacing:.08em;opacity:.22", txt, D)
        js += f'      tl.to("#{n}-p{i}", {{ opacity: 1, scale: 1.06, duration: 0.2 }}, {t}); tl.to("#{n}-p{i}", {{ scale: 1, duration: 0.2 }}, {round(t + 0.25, 2)});\n'
        js += f'      tl.to("#{n}-bars > div", {{ backgroundColor: "{bg}", duration: 0.15 }}, {t});\n'
    js += f'      tl.fromTo("#{n}-sp", {{ opacity: 0, y: 20 }}, {{ opacity: 1, y: 0, duration: 0.3 }}, 0);\n'
    return marco(n, D, "", inner, js)


def esc_popup_oferta(sc, e, n):
    D = sc["dur"]; validar_popup(e["popup"])
    inner = clip(1, f"{n}-p", "left:273px;top:220px", popup(e["popup"], 0.62), D)
    inner += clip(2, f"{n}-of", "left:220px;top:690px;width:640px;height:420px", oferta(), D)
    inner += f'    <div class="etq clip" style="top:1120px" data-start="0" data-duration="{D}" data-track-index="3">Números de ejemplo</div>\n'
    js = f"""      tl.fromTo("#{n}-of", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-p", {{ y: -70, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.45, ease: "power4.out" }}, {pal(sc, e.get("entra", 0))});
"""
    return marco(n, D, css_popup() + "\n    " + OF_CSS, inner, js)


def esc_reloj(sc, e, n):
    D = sc["dur"]; t1 = pal(sc, "hecha"); t0 = 0.1; t1 = max(t1, t0 + 1.2)
    inner = clip(1, f"{n}-rg", "left:330px;top:260px;width:420px;height:420px", f'<svg width="420" height="420" viewBox="0 0 420 420" aria-hidden="true"><circle cx="210" cy="210" r="170" fill="none" stroke="#3A3A45" stroke-width="26"/><circle id="{n}-rp" cx="210" cy="210" r="170" fill="none" stroke="#FACC15" stroke-width="26" stroke-linecap="round" stroke-dasharray="1068" stroke-dashoffset="1068" transform="rotate(-90 210 210)"/></svg><div id="{n}-tx" style="position:absolute;left:0;right:0;top:150px;text-align:center;font-family:\'JetBrains Mono\',monospace;font-weight:700;font-size:112px">0:00</div>', D)
    inner += clip(2, f"{n}-ok", "left:465px;top:760px;width:150px;height:150px;border-radius:50%;background:#22C55E;opacity:0", '<svg width="150" height="150" viewBox="0 0 24 24" fill="none" stroke="#04210F" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path id="' + n + '-ck" d="M6.5 12.5l4 4L17.5 8" stroke-dasharray="16" stroke-dashoffset="16"/></svg>', D)
    js = f"""      var seg = {{ v: 0 }};
      tl.to("#{n}-rp", {{ strokeDashoffset: 0, duration: {round(t1 - t0, 2)}, ease: "none" }}, {t0});
      tl.to(seg, {{ v: 3, duration: {round(t1 - t0, 2)}, ease: "none", onUpdate: function () {{ document.getElementById("{n}-tx").textContent = "0:0" + Math.min(3, Math.floor(seg.v)); }} }}, {t0});
      tl.to("#{n}-rp", {{ stroke: "#22C55E", duration: 0.2 }}, {round(t1, 2)});
      tl.fromTo("#{n}-ok", {{ opacity: 0, scale: 0.6 }}, {{ opacity: 1, scale: 1, duration: 0.3, ease: "power4.out" }}, {round(t1, 2)});
      tl.to("#{n}-ck", {{ strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }}, {round(t1 + 0.15, 2)});
"""
    return marco(n, D, "", inner, js)


def esc_seguir(sc, e, n):
    D = sc["dur"]
    inner = f'    <div class="etq clip" style="top:330px" data-start="0" data-duration="{D}" data-track-index="1">creador de Checa</div>\n'
    inner += clip(2, f"{n}-u", "left:0;right:0;top:400px;text-align:center;font-family:'JetBrains Mono',monospace;font-weight:700;font-size:64px", "@Eduardoalv9802", D)
    inner += clip(3, f"{n}-bt", "left:300px;top:580px;width:480px;height:140px;border-radius:999px;background:#F4F4EF;color:#0C0C10;display:flex;align-items:center;justify-content:center;font-family:'Bricolage Grotesque',sans-serif;font-weight:800;font-size:66px;letter-spacing:-.02em", "+ Seguir", D)
    js = f"""      tl.fromTo("#{n}-u", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-bt", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0.25);
      tl.to("#{n}-bt", {{ scale: 1.06, duration: 0.4, yoyo: true, repeat: {max(1, int(D / 0.4) - 2)}, ease: "sine.inOut" }}, 0.8);
"""
    return marco(n, D, "", inner, js)


# ---------- efectos de sonido ----------
NIVEL_SFX = {"verde": "aceptar", "amarillo": "considerar", "rojo": "rechazar"}


def sfx_escena(sc, e):
    """Efectos de la escena: lista de (archivo, segundo dentro de la escena, volumen)."""
    if "sfx" in e:
        if not e["sfx"]:
            return []
        return [(x[0], pal(sc, x[1]) if isinstance(x[1], str) else float(x[1]), float(x[2]) if len(x) > 2 else 0.45) for x in e["sfx"]]
    tipo = e["tipo"]
    if tipo == "popup":
        return [("popup-entra", 0.0, 0.4), (NIVEL_SFX[e["popup"].get("nivel", "verde")], 0.45, 0.45)]
    if tipo == "estados":
        w = sc["words"]  # mismos tiempos que esc_estados
        tiempo = [0.0 if i == 0 else max(0, round(w[min(k, len(w) - 1)][1] - 0.05, 2)) for i, k in enumerate(e["en_palabra"])]
        return [(NIVEL_SFX[p.get("nivel", "verde")], round(t0 + 0.15, 2), 0.45) for p, t0 in zip(e["popups"], tiempo)]
    if tipo == "numero":
        return [("numero-sube", 0.2, 0.4)]
    if tipo in ("cierre", "logo"):
        return [("logo-semaforo", 0.0, 0.5)]
    return []


def juntar_sfx(T, escenas):
    out = []
    for sc, e in zip(T["scenes"], escenas):
        for archivo, seg, vol in sfx_escena(sc, e):
            ruta = f"assets/sfx/{archivo}.mp3"
            assert os.path.exists(ruta), f"Falta {ruta} (copiar los mp3 de checa-web/marca/sonido/mp3)"
            out.append((ruta, round(sc["start"] + seg, 3), vol))
    return out


# ---------- escenas del video 12 (comisión y tarifas) ----------
ICONOS["candado"] = '<rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/>'
ICONOS["puntero"] = '<path d="M5 3l14 7-6 2-2 6z"/>'

MONO = "font-family:'JetBrains Mono',monospace;font-weight:700"
BRIC = "font-family:'Bricolage Grotesque',sans-serif;font-weight:800"
EJEMPLO = '    <div class="etq clip" style="top:{top}px" data-start="0" data-duration="{D}" data-track-index="{i}">Números de ejemplo</div>\n'

OC_CSS = f""".oc {{ position: relative; width: 440px; height: 100%; border-radius: 28px; background: #16161C; border: 3px solid #3A3A45; box-shadow: 0 30px 60px rgba(0,0,0,.6); box-sizing: border-box; }}
    .oc .l {{ position: absolute; left: 30px; top: 26px; {MONO}; font-size: 22px; letter-spacing: .14em; color: #A3A3AD; }}
    .oc .f {{ position: absolute; left: 26px; top: 58px; {BRIC}; font-size: 120px; letter-spacing: -.05em; line-height: 1; }}
    .oc .bar {{ position: absolute; left: 30px; top: 250px; height: 14px; width: 0; border-radius: 7px; background: #C9C9D1; }}
    .oc .km {{ position: absolute; left: 30px; top: 290px; {MONO}; font-size: 46px; color: #F4F4EF; opacity: 0; }}
    .oc .g {{ position: absolute; left: 30px; right: 30px; top: 214px; display: flex; justify-content: space-between; align-items: baseline; {MONO}; font-size: 32px; color: #EF4444; opacity: 0; }}
    .oc .ln {{ position: absolute; left: 30px; right: 30px; top: 288px; height: 3px; background: #3A3A45; }}
    .oc .q {{ position: absolute; left: 30px; top: 316px; {MONO}; font-size: 26px; letter-spacing: .12em; color: #A3A3AD; opacity: 0; }}
    .oc .n {{ position: absolute; left: 26px; top: 356px; {BRIC}; font-size: 170px; letter-spacing: -.05em; line-height: 1; opacity: 0; }}
    .oc .k2 {{ position: absolute; left: 30px; top: 556px; {MONO}; font-size: 34px; color: #A3A3AD; }}"""


def esc_billete(sc, e, n):
    D = sc["dur"]; t = pal(sc, "deja"); tin = round(max(0.45, t - 0.8), 2)
    inner = f'    <div class="etq clip" style="top:250px" data-start="0" data-duration="{D}" data-track-index="1">tarifa del viaje</div>\n'
    inner += clip(2, f"{n}-bill", f"left:190px;top:330px;width:700px;height:380px;border-radius:28px;background:#16161C;border:3px solid #F4F4EF;box-sizing:border-box;display:flex;align-items:center;justify-content:center;{BRIC};font-size:230px;letter-spacing:-.05em", "$100", D)
    chip = f"width:330px;height:104px;border-radius:999px;background:#3A1414;border:3px solid #EF4444;box-sizing:border-box;color:#EF4444;display:flex;align-items:center;justify-content:center;{MONO};font-size:38px;opacity:0"
    inner += clip(3, f"{n}-c1", f"left:190px;top:662px;{chip}", "− Comisión", D)
    inner += clip(4, f"{n}-c2", f"left:560px;top:662px;{chip}", "− Gasolina", D)
    inner += EJEMPLO.format(top=1010, D=D, i=5)
    js = f"""      tl.fromTo("#{n}-bill", {{ opacity: 0, y: 50 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-c1", {{ opacity: 0, y: -20 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power3.out" }}, {tin});
      tl.fromTo("#{n}-c2", {{ opacity: 0, y: -20 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power3.out" }}, {round(tin + 0.2, 2)});
      tl.to("#{n}-c1", {{ x: -300, y: 260, rotation: -14, opacity: 0, duration: 0.6, ease: "power2.in" }}, {t});
      tl.to("#{n}-c2", {{ x: 300, y: 260, rotation: 14, opacity: 0, duration: 0.6, ease: "power2.in" }}, {round(t + 0.1, 2)});
      tl.to("#{n}-bill", {{ scale: 0.86, y: 30, opacity: 0.7, borderColor: "#3A3A45", duration: 0.5, ease: "power2.out" }}, {round(t + 0.2, 2)});
"""
    return marco(n, D, "", inner, js)


def esc_ofertas(sc, e, n):
    D = sc["dur"]
    tA, tB = pal(sc, "100"), pal(sc, "otra")
    bA, bB = pal(sc, "14"), pal(sc, "solo")
    inner = ""
    for i, (x, km, anchoBarra) in enumerate([(60, "14 km total", 376), (580, "8 km total", 215)]):
        html = f'<div class="oc"><div class="l">NUEVA OFERTA</div><div class="f">$100</div><div class="bar" id="{n}-b{i}"></div><div class="km" id="{n}-k{i}">{km}</div></div>'
        inner += clip(1 + i, f"{n}-o{i}", f"left:{x}px;top:300px;width:440px;height:420px", html, D)
    inner += EJEMPLO.format(top=780, D=D, i=3)
    js = f"""      tl.fromTo("#{n}-o0", {{ opacity: 0, x: -80 }}, {{ opacity: 1, x: 0, duration: 0.4, ease: "power4.out" }}, {tA});
      tl.fromTo("#{n}-o1", {{ opacity: 0, x: 80 }}, {{ opacity: 1, x: 0, duration: 0.4, ease: "power4.out" }}, {tB});
      tl.to("#{n}-b0", {{ width: 376, duration: 0.7, ease: "power2.out" }}, {bA});
      tl.to("#{n}-k0", {{ opacity: 1, duration: 0.3 }}, {round(bA + 0.2, 2)});
      tl.to("#{n}-b1", {{ width: 215, duration: 0.5, ease: "power2.out" }}, {bB});
      tl.to("#{n}-k1", {{ opacity: 1, duration: 0.3 }}, {round(bB + 0.2, 2)});
"""
    return marco(n, D, OC_CSS, inner, js)


def esc_neto(sc, e, n):
    D = sc["dur"]
    tG, tA, tB = pal(sc, "gasolina"), pal(sc, "primera"), pal(sc, "segunda")
    datos = [(60, "14 km total", 28, 72), (580, "8 km total", 16, 84)]
    inner = ""
    for i, (x, km, gas, neta) in enumerate(datos):
        assert 100 - gas == neta, "La neta no cuadra"
        html = (f'<div class="oc" id="{n}-c{i}"><div class="l">NUEVA OFERTA</div><div class="f">$100</div>'
                f'<div class="g" id="{n}-g{i}"><span>Gasolina</span><span>−${gas}</span></div><div class="ln"></div>'
                f'<div class="q" id="{n}-q{i}">TE QUEDAN</div><div class="n" id="{n}-n{i}">$100</div><div class="k2">{km}</div></div>')
        inner += clip(1 + i, f"{n}-o{i}", f"left:{x}px;top:230px;width:440px;height:640px", html, D)
    inner += EJEMPLO.format(top=920, D=D, i=3)
    js = f"""      tl.fromTo("#{n}-o0", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-o1", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power4.out" }}, 0.12);
      tl.fromTo("#{n}-g0", {{ opacity: 0, x: -30 }}, {{ opacity: 1, x: 0, duration: 0.35, ease: "power4.out" }}, {tG});
      tl.fromTo("#{n}-g1", {{ opacity: 0, x: -30 }}, {{ opacity: 1, x: 0, duration: 0.35, ease: "power4.out" }}, {round(tG + 0.12, 2)});
      var a = {{ v: 100 }}, b = {{ v: 100 }};
      tl.to("#{n}-q0", {{ opacity: 1, duration: 0.2 }}, {tA}); tl.to("#{n}-n0", {{ opacity: 1, duration: 0.2 }}, {tA});
      tl.to(a, {{ v: 72, duration: 0.6, ease: "power2.out", onUpdate: function () {{ document.getElementById("{n}-n0").textContent = "$" + Math.round(a.v); }} }}, {tA});
      tl.to("#{n}-q1", {{ opacity: 1, duration: 0.2 }}, {tB}); tl.to("#{n}-n1", {{ opacity: 1, duration: 0.2 }}, {tB});
      tl.to(b, {{ v: 84, duration: 0.6, ease: "power2.out", onUpdate: function () {{ document.getElementById("{n}-n1").textContent = "$" + Math.round(b.v); }} }}, {tB});
      tl.to("#{n}-n1", {{ color: "#22C55E", duration: 0.25 }}, {round(tB + 0.6, 2)});
      tl.to("#{n}-c1", {{ borderColor: "#22C55E", duration: 0.25 }}, {round(tB + 0.6, 2)});
"""
    return marco(n, D, OC_CSS, inner, js)


def esc_diferencia(sc, e, n):
    D = sc["dur"]; t = pal(sc, "12")
    inner = f'    <div class="etq clip" style="top:330px" data-start="0" data-duration="{D}" data-track-index="1">misma tarifa</div>\n'
    inner += f'    <div id="{n}-big" class="mask clip" style="top:410px;height:380px" data-start="0" data-duration="{D}" data-track-index="2"><div style="font-size:340px;color:#22C55E">+$12</div></div>\n'
    inner += f'    <div id="{n}-pie" class="etq clip" style="top:830px" data-start="0" data-duration="{D}" data-track-index="3">más en tu bolsa por cada viaje</div>\n'
    inner += EJEMPLO.format(top=910, D=D, i=4)
    js = f"""      tl.fromTo("#{n}-big > div", {{ yPercent: 120 }}, {{ yPercent: 0, duration: 0.45, ease: "power4.out" }}, {t});
      tl.fromTo("#{n}-pie", {{ opacity: 0, y: 20 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power3.out" }}, {round(t + 0.3, 2)});
"""
    return marco(n, D, "", inner, js)


def esc_rejilla(sc, e, n):
    D = sc["dur"]; t0 = pal(sc, "20"); t1 = max(pal(sc, "240"), t0 + 1.0)
    dt = round((t1 - t0) / 20, 3)
    inner, js = "", ""
    for i in range(20):
        c, r = i % 5, i // 5
        inner += clip(1, f"{n}-d{i}", f"left:{242 + c * 124}px;top:{250 + r * 124}px;width:100px;height:100px;border-radius:50%;background:#16161C;border:3px solid #3A3A45;box-sizing:border-box", "", D)
        js += f'      tl.to("#{n}-d{i}", {{ backgroundColor: "#EF4444", borderColor: "#EF4444", scale: 1.08, duration: 0.12 }}, {round(t0 + i * dt, 3)}); tl.to("#{n}-d{i}", {{ scale: 1, duration: 0.12 }}, {round(t0 + i * dt + 0.12, 3)});\n'
    inner += f'    <div class="etq clip" style="top:190px" data-start="0" data-duration="{D}" data-track-index="2">cada punto es un viaje</div>\n'
    inner += clip(3, f"{n}-num", f"left:0;right:0;top:800px;text-align:center;{BRIC};font-size:220px;letter-spacing:-.05em;line-height:1;color:#EF4444", "$0", D)
    inner += f'    <div class="etq clip" style="top:1040px" data-start="0" data-duration="{D}" data-track-index="4">se quedan en el camino</div>\n'
    inner += EJEMPLO.format(top=1090, D=D, i=5)
    js = f"""      var s = {{ v: 0 }};
      tl.to(s, {{ v: 240, duration: {round(20 * dt, 2)}, ease: "none", onUpdate: function () {{ document.getElementById("{n}-num").textContent = "$" + Math.round(s.v); }} }}, {t0});
""" + js
    return marco(n, D, "", inner, js)


def esc_control(sc, e, n):
    D = sc["dur"]; tC, tV, tD = pal(sc, "cambiar"), pal(sc, "viaje"), pal(sc, "decides")
    fila = f"width:800px;height:150px;border-radius:28px;background:#16161C;border:3px solid #3A3A45;box-sizing:border-box;display:flex;align-items:center;gap:28px;padding-left:44px;{BRIC};font-size:64px;letter-spacing:-.03em"
    inner = clip(1, f"{n}-r0", f"left:140px;top:300px;{fila}", f'<span id="{n}-i0">{ic("candado", 76)}</span><span>Comisión</span>', D)
    inner += clip(2, f"{n}-r1", f"left:140px;top:500px;{fila}", f'<span id="{n}-i1">{ic("puntero", 76)}</span><span>Tus viajes</span>', D)
    js = f"""      tl.fromTo("#{n}-r0", {{ opacity: 0, x: -60 }}, {{ opacity: 1, x: 0, duration: 0.4, ease: "power4.out" }}, 0);
      tl.to("#{n}-r0", {{ opacity: 0.5, duration: 0.3 }}, {tC});
      tl.to("#{n}-i0", {{ x: 8, duration: 0.05, yoyo: true, repeat: 5 }}, {tC});
      tl.fromTo("#{n}-r1", {{ opacity: 0, x: 60 }}, {{ opacity: 1, x: 0, duration: 0.4, ease: "power4.out" }}, {tV});
      tl.to("#{n}-r1", {{ borderColor: "#22C55E", backgroundColor: "#0F2A19", duration: 0.3 }}, {tD});
"""
    return marco(n, D, "", inner, js)


def esc_dospopups(sc, e, n):
    D = sc["dur"]; tV = pal(sc, "marca")
    p1 = {"nivel": "amarillo", "app": "UBER", "neta": 72, "viaje": 100, "gasolina": 28,
          "chips": [["$173/h", "por hora", "ok"], ["$5.1/km", "por km", "mal"], ["25 min", "14 km total", ""]]}
    p2 = {"nivel": "verde", "app": "UBER", "neta": 84, "viaje": 100, "gasolina": 16, "mejor": True,
          "chips": [["$296/h", "por hora", "ok"], ["$10.5/km", "por km", "ok"], ["17 min", "8 km total", ""]]}
    for p in (p1, p2):
        validar_popup(p)
    inner = clip(1, f"{n}-p0", "left:273px;top:210px", popup(p1, 0.62), D)
    inner += clip(2, f"{n}-p1", "left:273px;top:650px", popup(p2, 0.62), D)
    inner += EJEMPLO.format(top=1085, D=D, i=3)
    js = f"""      tl.fromTo("#{n}-p0", {{ y: -60, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.45, ease: "power4.out" }}, 0);
      tl.fromTo("#{n}-p1", {{ y: -60, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.45, ease: "power4.out" }}, {tV});
"""
    return marco(n, D, css_popup(), inner, js)


def esc_comentarios(sc, e, n):
    D = sc["dur"]; t14, t8 = pal(sc, "14"), pal(sc, "8")
    tc = pal(sc, "comentarios")
    inner = f'    <div class="etq clip" style="top:250px" data-start="0" data-duration="{D}" data-track-index="1">responde en los comentarios</div>\n'
    tarjeta = (f'<div style="position:absolute;left:36px;top:34px;width:84px;height:84px;border-radius:50%;background:#2A2A33"></div>'
               f'<div style="position:absolute;left:144px;top:44px;{MONO};font-size:28px;color:#A3A3AD">@tu_comentario</div>'
               f'<div style="position:absolute;left:144px;top:92px;{BRIC};font-size:60px;letter-spacing:-.03em">¿Cuál aceptas?</div>')
    inner += clip(2, f"{n}-cm", "left:140px;top:330px;width:800px;height:190px;border-radius:28px;background:#16161C;border:3px solid #3A3A45;box-sizing:border-box", tarjeta, D)
    pill = f"width:380px;height:130px;border-radius:999px;background:#16161C;border:3px solid #F4F4EF;box-sizing:border-box;display:flex;align-items:center;justify-content:center;{BRIC};font-size:64px;letter-spacing:-.02em"
    inner += clip(3, f"{n}-a", f"left:140px;top:590px;{pill}", "14 km", D)
    inner += clip(4, f"{n}-b", f"left:560px;top:590px;{pill}", "8 km", D)
    inner += clip(5, f"{n}-in", f"left:140px;top:800px;width:800px;height:110px;border-radius:999px;background:#16161C;border:3px solid #3A3A45;box-sizing:border-box;display:flex;align-items:center;padding-left:40px;{MONO};font-size:34px;color:#A3A3AD", f'Escribe tu respuesta<span id="{n}-cur" style="display:inline-block;width:3px;height:40px;margin-left:8px;background:#FACC15"></span>', D)
    js = f"""      tl.fromTo("#{n}-cm", {{ opacity: 0, y: 40 }}, {{ opacity: 1, y: 0, duration: 0.4, ease: "power4.out" }}, 0.1);
      tl.fromTo("#{n}-a", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power4.out" }}, {t14});
      tl.fromTo("#{n}-b", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power4.out" }}, {t8});
      tl.fromTo("#{n}-in", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.35, ease: "power4.out" }}, {round(t8 + 0.3, 2)});
      tl.to("#{n}-cur", {{ opacity: 0, duration: 0.4, yoyo: true, repeat: {max(1, int(D / 0.4))}, ease: "none" }}, {round(t8 + 0.3, 2)});
"""
    return marco(n, D, "", inner, js)


TIPOS_V12 = {"comentarios": esc_comentarios, "billete": esc_billete, "ofertas": esc_ofertas, "neto": esc_neto, "diferencia": esc_diferencia, "rejilla": esc_rejilla, "control": esc_control, "dospopups": esc_dospopups}

TIPOS = {"hook": esc_hook, "anillo": esc_anillo, "datos": esc_datos, "logo": esc_logo, "escaneo": esc_escaneo, "voz": esc_voz, "popup_oferta": esc_popup_oferta, "reloj": esc_reloj, "seguir": esc_seguir, "gancho": esc_gancho, "numero": esc_numero, "popup": esc_popup, "estados": esc_estados, "cierre": esc_cierre}
TIPOS.update(TIPOS_V12)


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
def armar_index(T, nombres, sfx=None):
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
    for i, (ruta, t0, vol) in enumerate(sfx or []):
        h += f'      <audio id="el-sfx-{i}" src="{ruta}" data-start="{t0}" data-duration="{probe(ruta)}" data-track-index="{30 + i}" data-volume="{vol}"></audio>\n'
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
    sfx = [] if "--sin-sfx" in args else juntar_sfx(T, escenas)
    armar_index(T, nombres, sfx)
    print(f"{len(T['scenes'])} escenas, {len(g)} subtítulos, {len(sfx)} efectos de sonido, {T['total']} s")
    if "--check" in args or "--render" in args:
        subprocess.run(["npx", "--yes", "hyperframes@0.8.154", "check"], check=True)
    if "--render" in args:
        os.makedirs("renders", exist_ok=True)
        subprocess.run(["npx", "--yes", "hyperframes@0.8.154", "render", "--quality", "high", "--output", "renders/video.mp4"], check=True)


if __name__ == "__main__":
    main()
