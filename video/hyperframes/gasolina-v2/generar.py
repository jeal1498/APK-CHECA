"""Arma compositions/frames/*.html con los tiempos de audio_meta.json (voz de Fernando).
Uso: python3 -I generar.py  (después de alinear.py). Video 06 v2 (cuenta personal): «Gasolina cara»."""
import json, re

m = json.load(open("audio_meta.json"))
V = {v["frame"]: v for v in m["voices"]}
N = len(V)
PAD, PAD_FIN = 0.15, 2.0
LEAD = 0.25  # las entradas se adelantan a la palabra para que se vean completas cuando la voz la dice


def dur(n):
    return round(V[n]["duration_s"] + (PAD_FIN if n == N else PAD), 3)


def _w(n, k):
    return V[n]["words"][k]["text"].lower().strip("¿¡.,:;…!?")


def t(n, palabra, k=0):
    """Inicio de la k-ésima palabra que empieza con `palabra` en la línea n (menos LEAD)."""
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡").startswith(palabra.lower())]
    return round(max(0.0, ws[k]["start"] - LEAD), 3)


def tend(n, palabra, k=0):
    """Fin de la k-ésima palabra que empieza con `palabra` en la línea n."""
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡").startswith(palabra.lower())]
    return round(ws[k]["end"], 3)


def te(n, palabra, k=0):
    """Igual que t, pero la palabra debe ser exacta (cien ≠ ciento)."""
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡.,:;…!?") == palabra.lower()]
    return round(max(0.0, ws[k]["start"] - LEAD), 3)


FUENTES = """
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Regular.ttf') format('truetype'); font-weight: 400; font-display: block; }
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Medium.ttf') format('truetype'); font-weight: 500 600; font-display: block; }
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Bold.ttf') format('truetype'); font-weight: 700 900; font-display: block; }
    @font-face{ font-family: 'Fira Code'; src: url('assets/fonts/FiraCode.ttf') format('truetype'); font-weight: 300 700; font-display: block; }
    #root { position: absolute; inset: 0; width: 1080px; height: 1920px; overflow: hidden; font-family: 'Fira Sans', sans-serif; color: #F1F5F2; }
    .clip { position: absolute; }
    .P-ground { inset: 0; background: #0D0D0D; }
    .P-stage { left: 0; top: 0; width: 1080px; height: 1600px; transform-origin: 50% 45%; }
    .P-glow { position: absolute; left: 140px; top: 420px; width: 800px; height: 800px; border-radius: 50%; background: radial-gradient(circle, rgba(34,197,94,.20), rgba(34,197,94,0) 65%); }
    .P-mask { position: absolute; left: 0; right: 0; overflow: hidden; text-align: center; }
    .P-mask > div { font-weight: 800; text-transform: lowercase; letter-spacing: -.04em; line-height: 1.02; white-space: nowrap; }
    .P-ej { position: absolute; left: 0; right: 0; top: 1500px; text-align: center; font-family: 'Fira Code', monospace; font-size: 26px; letter-spacing: .12em; text-transform: uppercase; color: #6B736F; }
    .P-num { font-family: 'Fira Code', monospace; font-weight: 700; font-variant-numeric: tabular-nums; }
    .P-card { position: absolute; width: 620px; border-radius: 36px; background: #F4F5F4; color: #111; padding: 48px 52px 44px; box-shadow: 0 30px 80px rgba(0,0,0,.7); }
    .P-card .k { font-family: 'Fira Code', monospace; font-weight: 500; font-size: 28px; letter-spacing: .14em; text-transform: uppercase; color: #5C635F; }
    .P-card .f { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 112px; letter-spacing: -.03em; line-height: 1; margin-top: 22px; }
    .P-card .r { display: flex; align-items: center; gap: 22px; font-size: 40px; font-weight: 500; margin-top: 28px; color: #222; }
    .P-card .d { width: 20px; height: 20px; border-radius: 50%; background: #111; flex: none; }
    .P-card .s { width: 20px; height: 20px; background: #111; flex: none; }
    .P-card .b { margin-top: 38px; height: 104px; border-radius: 20px; background: #111; color: #F4F5F4; display: flex; align-items: center; justify-content: center; font-size: 44px; font-weight: 700; }
"""


def card(fare, a, v):
    return (f'<div class="k">Nueva solicitud</div><div class="f">{fare}</div>'
            f'<div class="r"><span class="d"></span>{a}</div><div class="r"><span class="s"></span>{v}</div><div class="b">Aceptar</div>')


CARD90 = card("$90.00", "A 8 min (5 km)", "Viaje: 25 min (10 km)")  # 15 km · 33 min en total
CARD_ROJA = card("$180.00", "A 8 min (3 km)", "Viaje: 67 min (37 km)")      # 40 km · 75 min en total
CARD_VERDE = card("$96.00", "A 3 min (1 km)", "Viaje: 14 min (7 km)")        # 8 km · 17 min en total


# ---------- ayudas de animación (JSON en vez de llaves a mano) ----------
def ft(sel, a, b, at):
    return f'      tl.fromTo("{sel}", {json.dumps(a)}, {json.dumps(b)}, {at});\n'


def to(sel, b, at):
    return f'      tl.to("{sel}", {json.dumps(b)}, {at});\n'


def st(sel, a, at):
    return f'      tl.set("{sel}", {json.dumps(a)}, {at});\n'


def mask_in(sel, at, d=0.3):
    return ft(sel, {"yPercent": 220}, {"yPercent": 0, "duration": d, "ease": "power3.out"}, at)


def push(c, at, s1=1.06):
    return f'      tl.fromTo("#{c}-stage", {{ scale: 1 }}, {{ scale: {s1}, duration: D - {at}, ease: "none" }}, {at});\n'


def shake(sel, at, amp=14):
    xs = [amp, -amp * .85, amp * .6, -amp * .4, amp * .2, 0]
    return "".join(to(sel, {"x": x, "duration": 0.05, "ease": "none"}, round(at + i * 0.05, 3)) for i, x in enumerate(xs))


def frame(n, cid, css, body, js, ground="#0D0D0D"):
    d = dur(n)
    css = css.replace("§", cid)
    body = body.replace("§", cid)
    return f"""<template>
  <style>{FUENTES}
    #{cid}-ground {{ background: {ground}; }}
{css}
  </style>

  <div id="root" data-composition-id="{cid}" data-width="1080" data-height="1920" data-start="0" data-duration="{d}">
    <div id="{cid}-ground" class="clip P-ground" data-start="0" data-duration="{d}" data-track-index="0"></div>
    <div id="{cid}-stage" class="clip P-stage" data-start="0" data-duration="{d}" data-track-index="1">
{body}
    </div>
  </div>

  <script src="https://cdn.jsdelivr.net/npm/gsap@3.14.2/dist/gsap.min.js"></script>
  <script>
    (function () {{
      window.__timelines = window.__timelines || {{}};
      var tl = gsap.timeline({{ paused: true }});
      var D = {d};
{js.replace("§", cid)}
      window.__timelines["{cid}"] = tl;
    }})();
  </script>
</template>
"""


# ---------- pop-up real de Checa (assets/checa-popup.html) ----------
POP_CSS_RAW = open("assets/checa-popup.html").read().split("<style>")[1].split("</style>")[0]
X = '<svg width="44" height="44" viewBox="0 0 24 24"><path d="M6 6 L18 18 M18 6 L6 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>'


def popcss(c):
    return re.sub(r"(^|\})\s*\.", lambda mm: mm.group(1) + f"\n    #{c}-pw .", POP_CSS_RAW)


def pop(c, i, lv, ac, neta, lin, fichas, u=1.0, app="UBER"):
    fich = "".join(f'<div class="cp-ficha {k}"><b>{b}</b><span>{s}</span></div>' for k, b, s in fichas)
    return (f'<div id="{c}-p{i}" class="cp cp-{lv}" style="position:absolute;left:0;top:0;--u:{u}"><div class="cp-banda"><div class="cp-lado"><span class="cp-app">{app}</span></div>'
            f'<div class="cp-accion">{ac}</div><div class="cp-lado der">{X}</div></div><div class="cp-cuerpo"><div class="cp-neta">{neta}<small>&nbsp;netos</small></div>'
            f'<div class="cp-linea">{lin}</div><div class="cp-fichas">{fich}</div></div></div>')


# números de ejemplo (gasolina $2/km; metas $8/km y $150/h; km y min totales incluyen la recogida)
ROJO = ("rojo", "RECHAZAR", "$100", "Viaje $180&nbsp;·&nbsp;Gasolina −$80",
        [("cp-mal", "$144/h", "por hora"), ("cp-mal", "$4.5/km", "por km"), ("", "75 min", "40 km total")])
VERDE = ("verde", "ACEPTAR", "$80", "Viaje $96&nbsp;·&nbsp;Gasolina −$16",
         [("cp-ok", "$339/h", "por hora"), ("cp-ok", "$12.0/km", "por km"), ("", "17 min", "8 km total")])
AMARILLO06 = ("amarillo", "CONSIDERAR", "$60", "Viaje $90&nbsp;·&nbsp;Gasolina −$30",
            [("cp-ok", "$164/h", "por hora"), ("cp-mal", "$6.0/km", "por km"), ("", "33 min", "15 km total")])
AMARILLO = ("amarillo", "CONSIDERAR", "$72", "Viaje $100&nbsp;·&nbsp;Gasolina −$28",
            [("cp-ok", "$171/h", "por hora"), ("cp-mal", "$7.1/km", "por km"), ("", "35 min", "14 km total")])

S = 'fill="none" stroke="currentColor" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'
CARRO = f'<svg viewBox="0 0 200 200"><path {S} d="M30 120 L45 75 Q50 62 64 62 L136 62 Q150 62 155 75 L170 120 Z M24 120 L176 120 L176 150 L24 150 Z"/><circle {S} cx="58" cy="152" r="16"/><circle {S} cx="142" cy="152" r="16"/></svg>'
OJO = f'<svg viewBox="0 0 200 200"><path {S} d="M16 100 Q100 14 184 100 Q100 186 16 100 Z"/><circle {S} cx="100" cy="100" r="30"/><path d="M36 176 L164 24" stroke="#F43F5E" stroke-width="10" stroke-linecap="round"/></svg>'
BOCINA = f'<svg viewBox="0 0 220 200"><path {S} d="M30 78 L72 78 L124 36 L124 164 L72 122 L30 122 Z"/><path class="w1" {S} d="M148 72 Q176 100 148 128"/><path class="w2" {S} d="M166 48 Q212 100 166 152"/></svg>'

F = {}
SEG = "display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; color: #04120A;"

# 1 — Hook: Cargas gasolina el lunes y el jueves ya andas en la reserva.
c = "f01-medidor"
F["01-medidor"] = frame(1, c, """
    #§-chips { position: absolute; left: 0; right: 0; top: 230px; display: flex; justify-content: center; gap: 28px; }
    .§-chip { padding: 16px 40px; border-radius: 999px; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 54px; border: 4px solid; }
    #§-c1 { color: #22C55E; border-color: #22C55E; } #§-c2 { color: #F43F5E; border-color: #F43F5E; }
    #§-card { position: absolute; left: 130px; top: 420px; width: 820px; height: 640px; border-radius: 44px; background: #151918; border: 3px solid #2A2E2C; box-shadow: 0 30px 80px rgba(0,0,0,.6); }
    #§-gauge { position: absolute; left: 70px; top: 60px; width: 680px; height: 442px; }
    #§-lab { position: absolute; left: 0; right: 0; bottom: 36px; text-align: center; font-family: 'Fira Code', monospace; font-size: 34px; letter-spacing: .16em; color: #8A938E; text-transform: uppercase; }
    #§-tx { top: 1130px; height: 180px; } #§-tx > div { font-size: 150px; color: #F43F5E; }
    #§-flash { position: absolute; inset: 0; background: #F43F5E; opacity: 0; }
""", """      <div id="§-flash"></div>
      <div id="§-chips"><span id="§-c1" class="§-chip">LUN</span><span id="§-c2" class="§-chip">JUE</span></div>
      <div id="§-card"><svg id="§-gauge" viewBox="0 0 400 260">
        <path d="M40 220 A160 160 0 0 1 360 220" fill="none" stroke="#2A2E2C" stroke-width="26" stroke-linecap="round"/>
        <path id="§-arc" d="M40 220 A160 160 0 0 1 360 220" fill="none" stroke="#22C55E" stroke-width="26" stroke-linecap="round"/>
        <text x="30" y="256" fill="#F43F5E" font-family="Fira Code" font-weight="700" font-size="30">E</text>
        <text x="352" y="256" fill="#22C55E" font-family="Fira Code" font-weight="700" font-size="30">F</text>
        <g id="§-needle"><line x1="200" y1="220" x2="200" y2="90" stroke="#F1F5F2" stroke-width="10" stroke-linecap="round"/><circle cx="200" cy="220" r="16" fill="#F1F5F2"/></g>
      </svg><div id="§-lab">gasolina</div></div>
      <div id="§-tx" class="P-mask"><div>en reserva</div></div>""",
    """      var arc = document.getElementById("§-arc"); arc.style.strokeDasharray = "503";
"""
    + ft("#§-card", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0)
    + ft("#§-c1", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(1, "lunes"))
    + ft("#§-c2", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(1, "jueves"))
    + st("#§-c1", {"textDecoration": "line-through", "opacity": 0.4}, t(1, "jueves"))
    + f'      tl.fromTo("#§-needle", {{ rotation: 78, svgOrigin: "200 220" }}, {{ rotation: -80, svgOrigin: "200 220", duration: 1.5, ease: "power2.in" }}, {round(t(1, "jueves") - 0.2, 3)});\n'
    + f'      tl.fromTo(arc, {{ strokeDashoffset: 0 }}, {{ strokeDashoffset: 503, duration: 1.5, ease: "power2.in" }}, {round(t(1, "jueves") - 0.2, 3)});\n'
    + to("#§-arc", {"stroke": "#F43F5E", "duration": 0.6}, round(t(1, "jueves") + 0.3, 3))
    + mask_in("#§-tx > div", t(1, "reserva"), 0.35)
    + ft("#§-flash", {"opacity": 0.3}, {"opacity": 0, "duration": 0.45, "ease": "power2.out", "immediateRender": False}, t(1, "reserva"))
    + shake("#§-stage", t(1, "reserva"), 14)
    + push(c, 0, 1.05))

# 2 — Micro-hook: Cada kilómetro te cuesta más o menos dos pesos de pura gasolina.
c = "f02-km"
F["02-km"] = frame(2, c, """
    #§-t { position: absolute; left: 150px; top: 330px; width: 780px; border-radius: 36px; background: #151918; border: 3px solid #2A2E2C; padding: 48px 54px 54px; }
    #§-t .h { font-family: 'Fira Code', monospace; font-size: 30px; letter-spacing: .16em; color: #8A938E; text-transform: uppercase; }
    #§-big { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 220px; line-height: 1; color: #FACC15; margin-top: 24px; text-shadow: 0 0 60px rgba(250,204,21,.35); }
    #§-per { font-size: 46px; font-weight: 600; line-height: 1; color: #AFB7B2; margin-top: 12px; }
    .§-row { display: flex; justify-content: space-between; align-items: baseline; margin-top: 44px; font-size: 48px; font-weight: 600; line-height: 1; }
    .§-row b { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 76px; }
    #§-bar { height: 22px; border-radius: 11px; background: #2A2E2C; margin-top: 38px; overflow: hidden; } #§-fill { height: 100%; width: 0%; background: #FACC15; }
""", """      <div id="§-t"><div class="h">Recibo · gasolina</div>
        <div id="§-big">$2.00</div><div id="§-per">por kilómetro</div>
        <div class="§-row"><span>km recorridos</span><b id="§-km">0</b></div>
        <div class="§-row"><span>gasolina</span><b id="§-gas" style="color:#F43F5E">$0.00</b></div>
        <div id="§-bar"><div id="§-fill"></div></div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    """      var o = { k: 0 }; var kmE = document.getElementById("§-km"); var gasE = document.getElementById("§-gas");
"""
    + ft("#§-t", {"y": 100, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.05)
    + ft("#§-big", {"scale": 0.5, "opacity": 0, "transformOrigin": "0% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(2, "dos"))
    + f'      tl.to(o, {{ k: 5, duration: 1.4, ease: "none", onUpdate: function () {{ kmE.textContent = Math.round(o.k); gasE.textContent = "$" + (Math.round(o.k) * 2).toFixed(2); }} }}, {t(2, "pesos")});\n'
    + ft("#§-fill", {"width": "0%"}, {"width": "100%", "duration": 1.4, "ease": "none"}, t(2, "pesos"))
    + push(c, 0, 1.04))

# 3 — Open loop: Imagina un viaje que paga noventa pesos.
c = "f03-oferta"
F["03-oferta"] = frame(3, c, """
    #§-l1 { top: 170px; height: 170px; } #§-l1 > div { font-size: 120px; color: #AFB7B2; }
    #§-card { left: 230px; top: 520px; }
""", f"""      <div class="P-glow"></div>
      <div id="§-l1" class="P-mask"><div>imagina un viaje</div></div>
      <div id="§-cw" style="position:absolute;inset:0"><div id="§-card" class="P-card">{CARD90}</div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in("#§-l1 > div", 0.05, 0.35)
    + ft("#§-cw", {"y": -900, "rotation": 8, "opacity": 0}, {"y": 0, "rotation": -3, "opacity": 1, "duration": 0.6, "ease": "circ.out"}, t(3, "viaje"))
    + to("#§-card .f", {"color": "#16A34A", "scale": 1.1, "transformOrigin": "0% 50%", "duration": 0.25, "ease": "power2.out"}, t(3, "noventa"))
    + push(c, 0, 1.05))

# 4 — Giro: Entre la recogida y el viaje, la gasolina se lleva treinta.
c = "f04-pierde"
F["04-pierde"] = frame(4, c, """
    #§-tot { position: absolute; left: 0; right: 0; top: 300px; text-align: center; font-family: 'Fira Code', monospace; font-size: 44px; letter-spacing: .14em; color: #8A938E; text-transform: uppercase; }
    #§-bar { position: absolute; left: 90px; top: 400px; width: 900px; height: 150px; display: flex; gap: 14px; }
    .§-seg { flex: 1; border-radius: 24px; background: #22C55E; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 64px; color: #04120A; }
    #§-blk { position: absolute; left: 290px; top: 760px; width: 500px; height: 400px; border-radius: 36px; background: #1A0F12; border: 4px solid #F43F5E; box-shadow: 0 0 70px rgba(244,63,94,.4); display: flex; flex-direction: column; align-items: center; justify-content: center; }
    #§-blk .k { font-size: 36px; line-height: 1; letter-spacing: .14em; color: #AFB7B2; text-transform: uppercase; font-family: 'Fira Code', monospace; }
    #§-blk .v { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 190px; line-height: 1; color: #F43F5E; margin-top: 16px; }
    #§-flash { position: absolute; inset: 0; background: #F43F5E; opacity: 0; }
""", """      <div id="§-flash"></div>
      <div id="§-tot">viaje $90</div>
      <div id="§-bar"><div class="§-seg" id="§-s1">$30</div><div class="§-seg" id="§-s2">$30</div><div class="§-seg" id="§-s3">$30</div></div>
      <div id="§-blk"><div class="k">gasolina</div><div class="v">−$30</div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-bar", {"scaleX": 0.2, "opacity": 0, "transformOrigin": "50% 50%"}, {"scaleX": 1, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0)
    + ft("#§-tot", {"opacity": 0}, {"opacity": 1, "duration": 0.3}, 0.1)
    + st("#§-blk", {"opacity": 0}, 0)
    + to("#§-s3", {"backgroundColor": "#F43F5E", "color": "#fff", "duration": 0.2}, t(4, "gasolina"))
    + ft("#§-blk", {"y": -380, "scale": 0.5, "opacity": 0}, {"y": 0, "scale": 1, "opacity": 1, "duration": 0.5, "ease": "back.out(1.5)"}, t(4, "lleva"))
    + ft("#§-flash", {"opacity": 0.3}, {"opacity": 0, "duration": 0.45, "ease": "power2.out", "immediateRender": False}, t(4, "treinta"))
    + shake("#§-stage", t(4, "treinta"), 14)
    + push(c, 0, 1.04))

# 5 — Pausa dramática: Te quedan sesenta.
c = "f05-sesenta"
F["05-sesenta"] = frame(5, c, """
    #§-k { position: absolute; left: 0; right: 0; top: 380px; text-align: center; font-size: 70px; font-weight: 600; color: #AFB7B2; }
    #§-n { top: 520px; height: 520px; } #§-n > div { font-family: 'Fira Code', monospace; text-transform: none; font-size: 420px; color: #22C55E; text-shadow: 0 0 90px rgba(34,197,94,.45); letter-spacing: -.04em; }
""", """      <div class="P-glow"></div>
      <div id="§-k">te quedan</div>
      <div id="§-n" class="P-mask"><div>$60</div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-k", {"opacity": 0, "y": 20}, {"opacity": 1, "y": 0, "duration": 0.3}, t(5, "quedan"))
    + mask_in("#§-n > div", t(5, "sesenta") - 0.05, 0.4)
    + push(c, 0, 1.12))

# 6 — Giro emocional: Un tercio de tu viaje se fue al tanque y ni lo notaste.
c = "f06-tanque"
F["06-tanque"] = frame(6, c, """
    #§-l1 { top: 170px; height: 190px; } #§-l1 > div { font-size: 170px; color: #FACC15; }
    #§-bar { position: absolute; left: 90px; top: 470px; width: 900px; height: 140px; display: flex; gap: 14px; }
    .§-seg { flex: 1; border-radius: 24px; background: #22C55E; }
    #§-pump { position: absolute; left: 340px; top: 740px; width: 400px; height: 480px; }
""", """      <div id="§-l1" class="P-mask"><div>un tercio</div></div>
      <div id="§-bar"><div class="§-seg" id="§-s1"></div><div class="§-seg" id="§-s2"></div><div class="§-seg" id="§-s3"></div></div>
      <svg id="§-pump" viewBox="0 0 200 240" fill="none" stroke="#F1F5F2" stroke-width="9" stroke-linecap="round" stroke-linejoin="round">
        <rect id="§-fuel" x="34" y="116" width="92" height="90" rx="6" fill="#F43F5E" stroke="none" opacity="0"/>
        <rect x="30" y="20" width="100" height="190" rx="14"/><rect x="46" y="38" width="68" height="52" rx="8"/>
        <path d="M130 70 C170 70 172 130 162 148 L162 196"/><path d="M14 214 H146"/></svg>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in("#§-l1 > div", t(6, "tercio") - 0.05, 0.4)
    + ft("#§-bar", {"y": -60, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0)
    + ft("#§-pump", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.1)
    + to("#§-s3", {"backgroundColor": "#F43F5E", "scale": 1.08, "duration": 0.25, "ease": "back.out(2)"}, t(6, "tercio"))
    + to("#§-s3", {"y": 360, "x": -150, "scale": 0.4, "opacity": 0, "duration": 0.6, "ease": "power2.in"}, round(t(6, "tanque") - 0.2, 3))
    + ft("#§-fuel", {"opacity": 0}, {"opacity": 0.9, "duration": 0.4}, round(t(6, "tanque") + 0.2, 3))
    + to("#§-pump", {"filter": "drop-shadow(0 0 30px rgba(244,63,94,.7))", "duration": 0.4}, round(t(6, "tanque") + 0.2, 3))
    + push(c, 0, 1.04))

# 7 — La app resta: La app Checa hace esa resta antes de que aceptes y te dice si el viaje conviene.
c = "f07-app"
F["07-app"] = frame(7, c, popcss(c) + """
    #§-tit { top: 110px; height: 160px; } #§-tit > div { font-size: 118px; } #§-tit em { font-style: normal; color: #22C55E; }
    #§-phone { position: absolute; left: 290px; top: 330px; width: 500px; height: 1000px; border-radius: 64px; background: #0B0D0C; border: 6px solid #2A2E2C; overflow: hidden; }
    #§-map { position: absolute; inset: 0; background: repeating-linear-gradient(0deg, #141816 0 2px, transparent 2px 90px), repeating-linear-gradient(90deg, #141816 0 2px, transparent 2px 90px); }
    #§-card { left: 20px; top: 380px; transform: scale(.74); transform-origin: 0 0; }
    #§-scan { position: absolute; left: 0; right: 0; top: 0; height: 10px; background: #22C55E; box-shadow: 0 0 40px 14px rgba(34,197,94,.55); opacity: 0; }
    #§-pw { position: absolute; left: 303px; top: 560px; width: 473px; height: 380px; }
""", f"""      <div class="P-glow"></div>
      <div id="§-tit" class="P-mask"><div>la app <em>checa</em></div></div>
      <div id="§-phone"><div id="§-map"></div><div id="§-card" class="P-card" data-layout-allow-overlap>{CARD90}</div><div id="§-scan"></div></div>
      <div id="§-pw" data-layout-allow-overlap>{pop(c, 0, *AMARILLO06, u=0.55)}</div>""",
    ft("#§-phone", {"y": 900}, {"y": 0, "duration": 0.5, "ease": "power3.out"}, 0)
    + mask_in("#§-tit > div", t(7, "app") - 0.1, 0.4)
    + ft("#§-scan", {"y": 0, "opacity": 1}, {"y": 990, "opacity": 1, "duration": 0.7, "ease": "power1.inOut"}, t(7, "hace"))
    + to("#§-scan", {"opacity": 0, "duration": 0.15}, round(t(7, "hace") + 0.7, 3))
    + ft("#§-card", {"boxShadow": "0 30px 80px rgba(0,0,0,.7)"}, {"boxShadow": "0 0 0 8px #22C55E", "duration": 0.2, "yoyo": True, "repeat": 1}, t(7, "hace") + 0.25)
    + ft("#§-pw", {"y": -600, "opacity": 0, "scale": 0.9}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.5, "ease": "back.out(1.3)"}, t(7, "resta"))
    + to("#§-phone", {"y": 12, "duration": 0.1, "yoyo": True, "repeat": 1, "ease": "power2.out"}, round(t(7, "resta") + 0.38, 3))
    + push(c, 0, 1.05)
    + to("#§-pw", {"scale": 1.06, "duration": 0.15, "yoyo": True, "repeat": 1}, t(7, "conviene")))

# 8 — Beneficio: Así la gasolina no se come tu ganancia.
c = "f08-escudo"
F["08-escudo"] = frame(8, c, """
    #§-tx { top: 200px; height: 180px; } #§-tx > div { font-size: 150px; color: #F1F5F2; } #§-tx em { font-style: normal; color: #22C55E; }
    #§-bar { position: absolute; left: 90px; top: 520px; width: 900px; height: 130px; display: flex; gap: 14px; }
    .§-seg { flex: 1; border-radius: 24px; background: #22C55E; }
    #§-s3 { background: #F43F5E; }
    #§-sh { position: absolute; left: 400px; top: 740px; width: 280px; height: 336px; }
    #§-sem { position: absolute; left: 0; right: 0; top: 1160px; display: flex; justify-content: center; gap: 36px; }
    .§-d { width: 84px; height: 84px; border-radius: 50%; background: #2A2E2C; }
""", """      <div class="P-glow"></div>
      <div id="§-tx" class="P-mask"><div>tu <em>ganancia</em></div></div>
      <div id="§-bar"><div class="§-seg"></div><div class="§-seg"></div><div class="§-seg" id="§-s3"></div></div>
      <svg id="§-sh" viewBox="0 0 200 240" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <path id="§-shp" d="M100 12 L180 44 V120 C180 180 140 218 100 232 C60 218 20 180 20 120 V44 Z" fill="rgba(34,197,94,.12)" stroke="#22C55E" stroke-width="10"/>
        <path d="M62 124 L92 154 L142 92" stroke="#22C55E" stroke-width="12"/></svg>
      <div id="§-sem"><span class="§-d" id="§-d1"></span><span class="§-d" id="§-d2"></span><span class="§-d" id="§-d3"></span></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-bar", {"y": -60, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0)
    + ft("#§-sh", {"y": 300, "scale": 0.5, "opacity": 0, "transformOrigin": "50% 50%"}, {"y": 0, "scale": 1, "opacity": 1, "duration": 0.5, "ease": "back.out(1.6)"}, t(8, "gasolina"))
    + to("#§-s3", {"opacity": 0.25, "duration": 0.3}, round(t(8, "gasolina") + 0.4, 3))
    + mask_in("#§-tx > div", t(8, "ganancia") - 0.1, 0.4)
    + to("#§-d3", {"backgroundColor": "#22C55E", "boxShadow": "0 0 40px rgba(34,197,94,.7)", "duration": 0.3}, t(8, "ganancia"))
    + to("#§-shp", {"strokeWidth": 14, "duration": 0.2, "yoyo": True, "repeat": 1}, t(8, "ganancia"))
    + push(c, 0, 1.05))

# 9 — Cierre estándar: Descarga Checa y checa cuánto te queda de cada viaje.
c = "f09-cierre"
LOGO = """<svg id="§-mark" viewBox="0 0 22 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <defs><filter id="§-gb" x="-150%" y="-150%" width="400%" height="400%"><feGaussianBlur stdDeviation="2.4" /></filter></defs>
          <path id="§-body" d="M 7 1 L 15 1 Q 21 1 21 7 L 21 31 Q 21 37 15 37 L 7 37 Q 1 37 1 31 L 1 7 Q 1 1 7 1 Z" fill="#111413" fill-opacity="0" stroke="#AFB7B2" stroke-width="0.9" stroke-linejoin="round" />
          <circle id="§-glow" cx="11" cy="28.5" r="6" fill="#22C55E" filter="url(#§-gb)" opacity="0" />
          <circle id="§-red" cx="11" cy="9.5" r="4.2" fill="#F43F5E" />
          <circle id="§-yellow" cx="11" cy="19" r="4.2" fill="#FACC15" />
          <circle id="§-green" cx="11" cy="28.5" r="4.2" fill="#22C55E" />
        </svg>"""
F["09-cierre"] = frame(9, c, """
    #§-bloom { position: absolute; left: -10px; top: 20px; width: 1100px; height: 1100px; border-radius: 50%; background: radial-gradient(circle at 50% 50%, rgba(34,197,94,0.42) 0%, rgba(34,197,94,0.16) 34%, rgba(34,197,94,0) 68%); }
    #§-lock { position: absolute; left: 103px; top: 150px; width: 874px; height: 397px; }
    #§-mark { position: absolute; left: 0; top: 0; width: 230px; height: 397px; z-index: 2; overflow: visible; }
    #§-wm { position: absolute; left: 274px; top: 0; width: 600px; height: 397px; overflow: hidden; z-index: 1; }
    #§-word { position: absolute; left: 0; top: 78px; font-size: 220px; line-height: 1; font-weight: 700; letter-spacing: -0.03em; color: #FFFFFF; white-space: nowrap; }
    #§-pill { position: absolute; left: 0; right: 0; top: 620px; margin: 0 auto; width: 860px; height: 130px; border-radius: 65px; background: #22C55E; display: flex; align-items: center; justify-content: center; gap: 22px; font-size: 62px; font-weight: 700; letter-spacing: -0.01em; color: #000; white-space: nowrap; }
    #§-pill b { font-family: 'Fira Code', monospace; }
    #§-sub { position: absolute; left: 0; right: 0; top: 790px; text-align: center; font-size: 54px; font-weight: 600; color: #F1F5F2; }
    #§-sub em { font-style: normal; color: #22C55E; }
    .§-row { position: absolute; left: 0; right: 0; text-align: center; }
    .§-row .l { font-family: 'Fira Code', monospace; font-weight: 500; font-size: 28px; letter-spacing: .16em; text-transform: uppercase; color: #8A938E; }
    .§-row .h { font-weight: 800; letter-spacing: -.02em; color: #F1F5F2; margin-top: 8px; line-height: 1.05; }
    .§-row .h i { font-style: normal; color: #22C55E; }
    #§-rA { top: 930px; } #§-rA .h { font-size: 84px; }
    #§-rB { top: 1110px; } #§-rB .h { font-size: 64px; }
    #§-rC { top: 1270px; } #§-rC .h { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 40px; letter-spacing: 0; color: #22C55E; }
    #§-black { position: absolute; inset: 0; background: #0D0D0D; opacity: 0; }
""", f"""      <div id="§-bloom"></div>
      <div id="§-lock">
        <div id="§-wm"><div id="§-word">checa</div></div>
        {LOGO}
      </div>
      <div id="§-pill"><span id="§-w1">Descarga</span><b id="§-w2">CHECA</b></div>
      <div id="§-sub"><span id="§-s1">y checa cuánto te <em>queda</em></span></div>
      <div id="§-rA" class="§-row"><div class="l">síguenos en tiktok</div><div class="h"><i>@</i>checa.driver</div></div>
      <div id="§-rB" class="§-row"><div class="l">creador de checa</div><div class="h"><i>@</i>Eduardoalv9802</div></div>
      <div id="§-rC" class="§-row"><div class="l">descarga en la web · solo Android</div><div class="h">jeal1498.github.io/checa-web</div></div>
      <div id="§-black"></div>""",
    """      var body = document.getElementById("§-body"); var len = body.getTotalLength() * 1.05; body.style.strokeDasharray = String(len);
"""
    + ft("#§-bloom", {"opacity": 0, "scale": 0.7}, {"opacity": 0.8, "scale": 1, "duration": 0.6, "ease": "power2.out"}, 0)
    + '      tl.fromTo(body, { strokeDashoffset: len }, { strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }, 0);\n'
    + '      tl.to(body, { attr: { "fill-opacity": 1 }, duration: 0.2, ease: "power1.out" }, 0.2);\n'
    + "".join(ft(f"#§-{k}", {"scale": 0, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.25, "ease": "back.out(2)"}, at)
              for k, at in [("red", 0.12), ("yellow", 0.2), ("green", 0.28)])
    + ft("#§-glow", {"opacity": 0}, {"opacity": 0.7, "duration": 0.3}, 0.4)
    + ft("#§-mark", {"x": 322}, {"x": 0, "duration": 0.45, "ease": "power3.inOut"}, 0.35)
    + ft("#§-word", {"x": -620}, {"x": 0, "duration": 0.45, "ease": "power3.inOut"}, 0.35)
    + ft("#§-pill", {"y": 90, "opacity": 0, "scale": 0.92}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.4, "ease": "power3.out"}, 0.1)
    + ft("#§-w1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, 0.15)
    + ft("#§-w2", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(9, "checa"))
    + to("#§-w2", {"scale": 1.12, "duration": 0.14, "ease": "power2.out"}, round(t(9, "checa") + 0.2, 3))
    + to("#§-w2", {"scale": 1, "duration": 0.25, "ease": "power2.inOut"}, round(t(9, "checa") + 0.35, 3))
    + ft("#§-s1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, tend(9, "checa", 0))
    + ft("#§-rA", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(9, "cuánto"))
    + ft("#§-rB", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(9, "queda"))
    + ft("#§-rC", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(9, "viaje"))
    + '      tl.fromTo("#§-black", { opacity: 0 }, { opacity: 1, duration: 0.4, ease: "power1.in" }, D - 0.4);\n')

for k, html in F.items():
    old = re.search(r'data-composition-id="([^"]+)"', html).group(1)
    html = html.replace(f'data-composition-id="{old}"', f'data-composition-id="{k}"').replace(f'__timelines["{old}"]', f'__timelines["{k}"]')
    open(f"compositions/frames/{k}.html", "w", encoding="utf-8").write(html)
print("ok", len(F), "escenas", [dur(n) for n in range(1, N + 1)], round(sum(dur(n) for n in range(1, N + 1)), 2))
