"""Arma compositions/frames/*.html con los tiempos de audio_meta.json (voz de Fernando).
Uso: python3 -I generar.py  (después de alinear.py). Video 19: «Checa lee la oferta por ti»."""
import json, re

m = json.load(open("audio_meta.json"))
V = {v["frame"]: v for v in m["voices"]}
N = len(V)
PAD, PAD_FIN = 0.15, 1.0
LEAD = 0.25  # las entradas se adelantan a la palabra para que se vean completas cuando la voz la dice


def dur(n):
    return round(V[n]["duration_s"] + (PAD_FIN if n == N else PAD), 3)


def _w(n, k):
    return V[n]["words"][k]["text"].lower().strip("¿¡.,:;…!?")


def t(n, palabra, k=0):
    """Inicio de la k-ésima palabra que empieza con `palabra` en la línea n (menos LEAD)."""
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡").startswith(palabra.lower())]
    return round(max(0.0, ws[k]["start"] - LEAD), 3)


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
    .P-ground { inset: 0; background: #000; }
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
    return ft(sel, {"yPercent": 105}, {"yPercent": 0, "duration": d, "ease": "power3.out"}, at)


def push(c, at, s1=1.06):
    return f'      tl.fromTo("#{c}-stage", {{ scale: 1 }}, {{ scale: {s1}, duration: D - {at}, ease: "none" }}, {at});\n'


def shake(sel, at, amp=14):
    xs = [amp, -amp * .85, amp * .6, -amp * .4, amp * .2, 0]
    return "".join(to(sel, {"x": x, "duration": 0.05, "ease": "none"}, round(at + i * 0.05, 3)) for i, x in enumerate(xs))


def frame(n, cid, css, body, js, ground="#000"):
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
AMARILLO = ("amarillo", "CONSIDERAR", "$72", "Viaje $100&nbsp;·&nbsp;Gasolina −$28",
            [("cp-ok", "$171/h", "por hora"), ("cp-mal", "$7.1/km", "por km"), ("", "35 min", "14 km total")])

S = 'fill="none" stroke="currentColor" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'
CARRO = f'<svg viewBox="0 0 200 200"><path {S} d="M30 120 L45 75 Q50 62 64 62 L136 62 Q150 62 155 75 L170 120 Z M24 120 L176 120 L176 150 L24 150 Z"/><circle {S} cx="58" cy="152" r="16"/><circle {S} cx="142" cy="152" r="16"/></svg>'
OJO = f'<svg viewBox="0 0 200 200"><path {S} d="M16 100 Q100 14 184 100 Q100 186 16 100 Z"/><circle {S} cx="100" cy="100" r="30"/><path d="M36 176 L164 24" stroke="#F43F5E" stroke-width="10" stroke-linecap="round"/></svg>'
BOCINA = f'<svg viewBox="0 0 220 200"><path {S} d="M30 78 L72 78 L124 36 L124 164 L72 122 L30 122 Z"/><path class="w1" {S} d="M148 72 Q176 100 148 128"/><path class="w2" {S} d="M166 48 Q212 100 166 152"/></svg>'

F = {}

# 1 — Gancho: Aceptas un viaje porque se ve buen billete… y a la hora de la verdad sales perdiendo.
c = "f01-gancho"
F["01-gancho"] = frame(1, c, """
    #§-l1 { top: 170px; height: 150px; } #§-l1 > div { font-size: 124px; color: #AFB7B2; }
    #§-l2 { top: 320px; height: 230px; } #§-l2 > div { font-size: 210px; color: #F43F5E; }
    #§-card { left: 230px; top: 660px; }
    #§-pill { position: absolute; left: 0; right: 0; top: 1290px; text-align: center; }
    #§-pill span { display: inline-block; padding: 22px 52px; border-radius: 999px; background: #F43F5E; color: #fff; font-weight: 800; font-size: 72px; letter-spacing: -.01em; }
    #§-pill b { font-family: 'Fira Code', monospace; }
""", f"""      <div class="P-glow"></div>
      <div id="§-l1" class="P-mask"><div>se veía bueno.</div></div>
      <div id="§-l2" class="P-mask"><div>perdiste.</div></div>
      <div id="§-cw" style="position:absolute;inset:0"><div id="§-card" class="P-card">{CARD_ROJA}</div></div>
      <div id="§-pill"><span>te quedan <b>$100</b></span></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-cw", {"y": -900, "rotation": 8, "opacity": 0}, {"y": 0, "rotation": -3, "opacity": 1, "duration": 0.6, "ease": "circ.out"}, 0)
    + mask_in("#§-l1 > div", 0.3)
    + to("#§-card .f", {"color": "#16A34A", "scale": 1.08, "transformOrigin": "0% 50%", "duration": 0.25, "ease": "power2.out"}, t(1, "billete"))
    + mask_in("#§-l2 > div", t(1, "sales"), 0.4)
    + to("#§-card .f", {"color": "#F43F5E", "duration": 0.2}, t(1, "sales") + 0.1)
    + shake("#§-stage", t(1, "sales") + 0.1)
    + ft("#§-pill", {"scale": 0.4, "opacity": 0, "y": 40}, {"scale": 1, "opacity": 1, "y": 0, "duration": 0.35, "ease": "back.out(2)"}, t(1, "perdiendo"))
    + push(c, 0.3))

# 2 — Tensión: Casi siempre pasa igual: la oferta te cae manejando y no hay tiempo de calcular.
c = "f02-tension"
F["02-tension"] = frame(2, c, """
    #§-l1 { top: 150px; height: 170px; } #§-l1 > div { font-size: 150px; color: #F1F5F2; }
    #§-l2 { top: 330px; height: 170px; } #§-l2 > div { font-size: 150px; color: #F43F5E; }
    #§-road { position: absolute; left: 340px; top: 600px; width: 400px; height: 900px; background: #0F1211; border-left: 6px solid #2A2E2C; border-right: 6px solid #2A2E2C; overflow: hidden; }
    #§-lane { position: absolute; left: 187px; top: -160px; width: 26px; height: 1300px; background: repeating-linear-gradient(180deg, #F1F5F2 0 80px, transparent 80px 160px); opacity: .45; }
    #§-car { position: absolute; left: 350px; top: 1060px; width: 380px; height: 380px; color: #F1F5F2; }
    #§-card { left: 348px; top: 640px; transform: scale(.62); transform-origin: 0 0; }
    #§-ring { position: absolute; left: 790px; top: 700px; width: 240px; height: 240px; }
    #§-n { position: absolute; left: 790px; top: 700px; width: 240px; height: 240px; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 68px; }
""", f"""      <div id="§-l1" class="P-mask"><div>manejando</div></div>
      <div id="§-l2" class="P-mask"><div>sin tiempo</div></div>
      <div id="§-road"><div id="§-lane"></div></div>
      <div id="§-car">{CARRO}</div>
      <div id="§-card" class="P-card">{CARD_ROJA}</div>
      <svg id="§-ring" viewBox="0 0 600 600"><circle cx="300" cy="300" r="260" fill="none" stroke="#1C201E" stroke-width="44"/>
        <circle id="§-arc" cx="300" cy="300" r="260" fill="none" stroke="#FACC15" stroke-width="44" stroke-linecap="round" transform="rotate(-90 300 300)"/></svg>
      <div id="§-n">15 s</div>""",
    ft("#§-lane", {"y": 0}, {"y": 480, "duration": 4.6, "ease": "none"}, 0)
    + mask_in("#§-l1 > div", t(2, "manejando"))
    + ft("#§-card", {"y": -900, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.45, "ease": "power4.out"}, t(2, "oferta"))
    + ft("#§-car", {"y": 0}, {"y": 22, "duration": 0.1, "yoyo": True, "repeat": 1, "ease": "power2.out"}, round(t(2, "oferta") + 0.4, 3))
    + shake("#§-stage", round(t(2, "oferta") + 0.4, 3), 8)
    + ft("#§-ring", {"opacity": 0, "scale": 0.4, "transformOrigin": "50% 50%"}, {"opacity": 1, "scale": 1, "duration": 0.35, "ease": "back.out(1.8)"}, t(2, "hay"))
    + ft("#§-n", {"opacity": 0}, {"opacity": 1, "duration": 0.25}, t(2, "hay"))
    + mask_in("#§-l2 > div", t(2, "tiempo"), 0.35)
    + """      var a = document.getElementById("§-arc"); var L = 2 * Math.PI * 260; a.style.strokeDasharray = L;
      var o = { s: 15 }; var n = document.getElementById("§-n");
"""
    + f'      tl.fromTo(a, {{ strokeDashoffset: 0 }}, {{ strokeDashoffset: L * 0.93, duration: D - {t(2, "hay")}, ease: "power1.in" }}, {t(2, "hay")});\n'
    + f'      tl.to(a, {{ stroke: "#F43F5E", duration: 0.6 }}, {round(t(2, "hay") + 0.5, 3)});\n'
    + f'      tl.to(o, {{ s: 1, duration: D - {t(2, "hay")}, ease: "power1.in", onUpdate: function () {{ n.textContent = Math.ceil(o.s) + " s"; }} }}, {t(2, "hay")});\n'
    + push(c, 0, 1.05))

# 3 — Mira: Para eso existe la app Checa. Mira qué hace con esa oferta.
c = "f03-mira"
F["03-mira"] = frame(3, c, popcss(c) + """
    #§-tit { top: 110px; height: 160px; } #§-tit > div { font-size: 118px; } #§-tit em { font-style: normal; color: #22C55E; }
    #§-phone { position: absolute; left: 290px; top: 330px; width: 500px; height: 1000px; border-radius: 64px; background: #0B0D0C; border: 6px solid #2A2E2C; overflow: hidden; }
    #§-map { position: absolute; inset: 0; background: repeating-linear-gradient(0deg, #141816 0 2px, transparent 2px 90px), repeating-linear-gradient(90deg, #141816 0 2px, transparent 2px 90px); }
    #§-card { left: 20px; top: 380px; transform: scale(.74); transform-origin: 0 0; }
    #§-scan { position: absolute; left: 0; right: 0; top: 0; height: 10px; background: #22C55E; box-shadow: 0 0 40px 14px rgba(34,197,94,.55); opacity: 0; }
    #§-pw { position: absolute; left: 303px; top: 560px; width: 473px; height: 380px; }
""", f"""      <div class="P-glow"></div>
      <div id="§-tit" class="P-mask"><div>la app <em>checa</em></div></div>
      <div id="§-phone"><div id="§-map"></div><div id="§-card" class="P-card" data-layout-allow-overlap>{CARD_ROJA}</div><div id="§-scan"></div></div>
      <div id="§-pw" data-layout-allow-overlap>{pop(c, 0, *ROJO, u=0.55)}</div>""",
    ft("#§-phone", {"y": 900}, {"y": 0, "duration": 0.5, "ease": "power3.out"}, 0)
    + mask_in("#§-tit > div", t(3, "app") - 0.1, 0.4)
    + ft("#§-scan", {"y": 0, "opacity": 1}, {"y": 990, "opacity": 1, "duration": 0.7, "ease": "power1.inOut"}, t(3, "mira"))
    + to("#§-scan", {"opacity": 0, "duration": 0.15}, round(t(3, "mira") + 0.7, 3))
    + ft("#§-card", {"boxShadow": "0 30px 80px rgba(0,0,0,.7)"}, {"boxShadow": "0 0 0 8px #22C55E", "duration": 0.2, "yoyo": True, "repeat": 1}, t(3, "mira") + 0.25)
    + ft("#§-pw", {"y": -600, "opacity": 0, "scale": 0.9}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.5, "ease": "back.out(1.3)"}, t(3, "hace"))
    + to("#§-phone", {"y": 12, "duration": 0.1, "yoyo": True, "repeat": 1, "ease": "power2.out"}, round(t(3, "hace") + 0.38, 3))
    + push(c, 0, 1.05))

# 4 — Neta: Lee la oferta por ti y te dice cuánto te queda limpio, ya sin la gasolina.
c = "f04-neta"
F["04-neta"] = frame(4, c, """
    #§-tl { top: 150px; height: 250px; } #§-tl > div { font-size: 210px; color: #22C55E; }
    #§-t { position: absolute; left: 150px; top: 470px; width: 780px; border-radius: 28px; background: #101312; border: 3px solid #2A2E2C; padding: 56px 60px; }
    .§-row { display: flex; justify-content: space-between; align-items: baseline; font-size: 52px; font-weight: 600; }
    .§-row b { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 70px; }
    #§-r2 { margin-top: 46px; color: #F43F5E; }
    #§-sep { height: 3px; background: #2A2E2C; margin: 46px 0 34px; }
    #§-r3 b { color: #F1F5F2; font-size: 96px; }
    #§-tit { top: 1160px; height: 140px; } #§-tit > div { font-size: 112px; } #§-tit em { font-style: normal; color: #F43F5E; }
""", """      <div class="P-glow"></div>
      <div id="§-tl" class="P-mask"><div>limpio</div></div>
      <div id="§-t">
        <div class="§-row"><span>Viaje</span><b>$180.00</b></div>
        <div id="§-r2" class="§-row"><span>− Gasolina</span><b>−$80.00</b></div>
        <div id="§-sep"></div>
        <div id="§-r3" class="§-row"><span>Te queda</span><b id="§-net">$180</b></div>
      </div>
      <div id="§-tit" class="P-mask"><div>sin <em>gasolina</em></div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-t", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, 0)
    + st("#§-r2", {"opacity": 0}, 0) + st("#§-r3", {"opacity": 0}, 0)
    + ft("#§-r3", {"x": -100, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(4, "queda"))
    + mask_in("#§-tl > div", t(4, "limpio"), 0.4)
    + ft("#§-r2", {"x": -120, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(4, "sin"))
    + mask_in("#§-tit > div", t(4, "sin"), 0.35)
    + """      var o = { v: 180 }; var net = document.getElementById("§-net");
"""
    + f'      tl.to(o, {{ v: 100, duration: 0.7, ease: "power2.inOut", onUpdate: function () {{ net.textContent = "$" + Math.round(o.v); }} }}, {t(4, "sin") + 0.1});\n'
    + to("#§-net", {"color": "#22C55E", "scale": 1.15, "transformOrigin": "100% 50%", "duration": 0.25, "ease": "back.out(2)"}, round(t(4, "sin") + 0.85, 3))
    + push(c, 0, 1.05))

# 5 — Rojo: Ciento ochenta se ven bien, pero te quedan cien en una hora y cuarto. Rojo.
c = "f05-rojo"
F["05-rojo"] = frame(5, c, popcss(c) + """
    #§-a { top: 120px; height: 250px; } #§-a > div { font-family: 'Fira Code', monospace; text-transform: none; font-size: 230px; color: #F1F5F2; }
    #§-strike { position: absolute; left: 290px; top: 250px; width: 500px; height: 16px; border-radius: 8px; background: #F43F5E; transform-origin: 0 50%; }
    #§-kick { position: absolute; left: 0; right: 0; top: 395px; text-align: center; font-size: 46px; font-weight: 600; color: #AFB7B2; }
    #§-b { top: 440px; height: 270px; } #§-b > div { font-family: 'Fira Code', monospace; text-transform: none; font-size: 250px; color: #F43F5E; }
    #§-chip { position: absolute; left: 290px; top: 740px; width: 500px; height: 96px; border-radius: 48px; border: 4px solid #FACC15; color: #FACC15; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 46px; }
    #§-pw { position: absolute; left: 153px; top: 890px; width: 774px; height: 580px; }
    #§-flash { position: absolute; inset: 0; background: #F43F5E; opacity: 0; }
""", f"""      <div id="§-flash"></div>
      <div id="§-a" class="P-mask"><div>$180</div></div>
      <div id="§-strike"></div>
      <div id="§-kick">te quedan</div>
      <div id="§-b" class="P-mask"><div>$100</div></div>
      <div id="§-chip">en 1 h 15 min</div>
      <div id="§-pw">{pop(c, 0, *ROJO, u=0.9)}</div>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in("#§-a > div", 0, 0.3)
    + ft("#§-strike", {"scaleX": 0}, {"scaleX": 1, "duration": 0.3, "ease": "power3.out"}, t(5, "pero"))
    + to("#§-a", {"opacity": 0.45, "duration": 0.2}, t(5, "pero"))
    + ft("#§-kick", {"opacity": 0, "y": 20}, {"opacity": 1, "y": 0, "duration": 0.25}, t(5, "quedan"))
    + mask_in("#§-b > div", te(5, "cien") - 0.05, 0.35)
    + ft("#§-chip", {"scale": 0.5, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(5, "una"))
    + ft("#§-pw", {"y": -700, "opacity": 0, "scale": 0.92}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.5, "ease": "back.out(1.3)"}, t(5, "rojo"))
    + ft("#§-flash", {"opacity": 0.3}, {"opacity": 0, "duration": 0.45, "ease": "power2.out"}, round(t(5, "rojo") + 0.3, 3))
    + shake("#§-stage", round(t(5, "rojo") + 0.3, 3), 12)
    + push(c, 0, 1.04))

# 6 — Verde: Otro, de noventa y seis pesos, te deja ochenta en diecisiete minutos. Verde.
c = "f06-verde"
F["06-verde"] = frame(6, c, popcss(c) + """
    .§-m { box-sizing: border-box; position: absolute; top: 300px; width: 470px; height: 560px; border-radius: 36px; background: #101312; border: 4px solid #2A2E2C; padding: 40px 36px; }
    #§-mA { left: 50px; } #§-mB { left: 560px; }
    .§-m .k { font-family: 'Fira Code', monospace; font-size: 30px; letter-spacing: .14em; text-transform: uppercase; color: #8A938E; }
    .§-m .f { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 110px; margin-top: 20px; }
    .§-m .q { font-size: 40px; font-weight: 600; color: #AFB7B2; margin-top: 18px; }
    .§-m .n { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 96px; margin-top: 6px; }
    .§-m .t { display: inline-block; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 52px; margin-top: 28px; padding: 10px 24px; border-radius: 16px; border: 4px solid; }
    #§-mA .n { color: #F43F5E; } #§-mA .t { color: #F43F5E; border-color: #F43F5E; }
    #§-mB .n { color: #22C55E; } #§-mB .t { color: #22C55E; border-color: #22C55E; }
    #§-pw { position: absolute; left: 205px; top: 930px; width: 671px; height: 520px; }
""", f"""      <div class="P-glow"></div>
      <div id="§-mA" class="§-m"><div class="k">viaje a</div><div class="f">$180</div><div class="q">te quedan</div><div class="n">$100</div><div class="t">75 min</div></div>
      <div id="§-mB" class="§-m"><div class="k">viaje b</div><div class="f" id="§-fB">$96</div><div class="q">te quedan</div><div class="n" id="§-nB">$80</div><div class="t" id="§-tB">17 min</div></div>
      <div id="§-pw">{pop(c, 0, *VERDE, u=0.78)}</div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-mA", {"y": 60, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, 0)
    + ft("#§-mB", {"x": 700, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, t(6, "otro"))
    + to("#§-mA", {"x": -26, "opacity": 0.5, "duration": 0.15, "ease": "power2.out"}, round(t(6, "otro") + 0.35, 3))
    + to("#§-mA", {"x": 0, "duration": 0.3, "ease": "back.out(2)"}, round(t(6, "otro") + 0.5, 3))
    + to("#§-fB", {"color": "#FACC15", "scale": 1.1, "transformOrigin": "0% 50%", "duration": 0.2, "yoyo": True, "repeat": 1}, t(6, "noventa"))
    + ft("#§-nB", {"opacity": 0, "scale": 0.6, "transformOrigin": "0% 50%"}, {"opacity": 1, "scale": 1, "duration": 0.3, "ease": "back.out(2)"}, te(6, "ochenta"))
    + ft("#§-tB", {"opacity": 0, "y": 20}, {"opacity": 1, "y": 0, "duration": 0.3, "ease": "power3.out"}, t(6, "diecisiete"))
    + st("#§-nB", {"opacity": 0}, 0) + st("#§-tB", {"opacity": 0}, 0)
    + to("#§-mB", {"borderColor": "#22C55E", "boxShadow": "0 0 60px rgba(34,197,94,.45)", "duration": 0.3}, t(6, "verde"))
    + ft("#§-pw", {"y": -600, "opacity": 0, "scale": 0.9}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.5, "ease": "back.out(1.3)"}, t(6, "verde"))
    + to("#§-stage", {"scale": 1.06, "transformOrigin": "620px 700px", "duration": 0.6, "ease": "power2.inOut"}, round(t(6, "otro") + 0.1, 3))
    + st("#§-pw", {"opacity": 0}, 0))

# 7 — Metas: Tú pones la vara: cuánto quieres ganar por kilómetro y por hora.
c = "f07-metas"
F["07-metas"] = frame(7, c, """
    #§-l1 { top: 140px; height: 150px; } #§-l1 > div { font-size: 118px; color: #F1F5F2; }
    #§-l2 { top: 290px; height: 190px; } #§-l2 > div { font-size: 160px; color: #FACC15; }
    #§-p { position: absolute; left: 110px; top: 600px; width: 860px; border-radius: 36px; background: #101312; border: 3px solid #2A2E2C; padding: 46px 52px 54px; }
    #§-p .k { font-family: 'Fira Code', monospace; font-size: 30px; letter-spacing: .14em; text-transform: uppercase; color: #22C55E; }
    .§-m { display: flex; justify-content: space-between; align-items: center; margin-top: 52px; }
    .§-lbl { font-size: 46px; font-weight: 600; }
    .§-ctl { display: flex; align-items: center; gap: 20px; }
    .§-btn { width: 88px; height: 88px; border-radius: 26px; background: #1C201E; border: 3px solid #2E3532; display: flex; align-items: center; justify-content: center; font-size: 58px; font-weight: 700; color: #F1F5F2; }
    .§-val { width: 250px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 66px; }
""", """      <div class="P-glow"></div>
      <div id="§-l1" class="P-mask"><div>tú pones</div></div>
      <div id="§-l2" class="P-mask"><div>la vara</div></div>
      <div id="§-p"><div class="k">Tus metas</div>
        <div id="§-m1" class="§-m"><span class="§-lbl">Por km</span><div class="§-ctl"><div class="§-btn">−</div><div id="§-v1" class="§-val">$8/km</div><div id="§-pl1" class="§-btn">+</div></div></div>
        <div id="§-m2" class="§-m"><span class="§-lbl">Por hora</span><div class="§-ctl"><div class="§-btn">−</div><div id="§-v2" class="§-val">$150/h</div><div id="§-pl2" class="§-btn">+</div></div></div>
      </div>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in("#§-l1 > div", 0, 0.3)
    + mask_in("#§-l2 > div", t(7, "vara"), 0.4)
    + ft("#§-p", {"y": 120, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(7, "cuánto"))
    + ft("#§-m1", {"x": -60, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(7, "ganar"))
    + ft("#§-m2", {"x": -60, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, te(7, "hora") - 0.5)
    + "".join(
        to(f"#§-pl{r}", {"scale": 0.86, "background": "#22C55E", "color": "#04120A", "duration": 0.08, "yoyo": True, "repeat": 1}, at)
        + st(f"#§-v{r}", {"textContent": txt}, round(at + 0.08, 3))
        for r, at, txt in [(1, round(t(7, "kilómetro") + 0.3, 3), "$9/km"), (1, round(t(7, "kilómetro") + 0.7, 3), "$10/km"),
                           (2, round(te(7, "hora") + 0.15, 3), "$175/h"), (2, round(te(7, "hora") + 0.55, 3), "$200/h")])
    + push(c, 0, 1.04))

# 8 — Voz: Y ni miras la pantalla: la app te dice en voz alta aceptar, considerar o rechazar.
c = "f08-voz"
F["08-voz"] = frame(8, c, popcss(c) + """
    #§-eye { position: absolute; left: 330px; top: 420px; width: 420px; height: 420px; color: #F1F5F2; }
    #§-sp { position: absolute; left: 330px; top: 420px; width: 420px; height: 420px; color: #22C55E; }
    #§-tit { top: 900px; height: 150px; } #§-tit > div { font-size: 124px; } #§-tit em { font-style: normal; color: #22C55E; }
    #§-pw { position: absolute; left: 153px; top: 620px; width: 774px; height: 580px; }
""", f"""      <div class="P-glow"></div>
      <div id="§-eye">{OJO}</div>
      <div id="§-sp">{BOCINA}</div>
      <div id="§-tit" class="P-mask"><div>en <em>voz alta</em></div></div>
      <div id="§-pw">
        {pop(c, 0, *VERDE, u=0.9)}
        {pop(c, 1, *AMARILLO, u=0.9)}
        {pop(c, 2, *ROJO, u=0.9)}
      </div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-eye", {"scale": 0.5, "opacity": 0, "rotation": -10}, {"scale": 1, "opacity": 1, "rotation": 0, "duration": 0.4, "ease": "back.out(1.7)"}, 0)
    + st("#§-sp", {"opacity": 0}, 0)
    + st("#§-pw", {"opacity": 0}, 0)
    + to("#§-eye", {"opacity": 0, "scale": 0.6, "duration": 0.25}, t(8, "voz"))
    + ft("#§-sp", {"opacity": 0, "scale": 0.6}, {"opacity": 1, "scale": 1, "duration": 0.35, "ease": "back.out(2)"}, t(8, "voz"))
    + mask_in("#§-tit > div", t(8, "voz") + 0.05, 0.4)
    + "".join(ft(f"#§-sp .{w}", {"opacity": 0.25}, {"opacity": 1, "duration": 0.2, "yoyo": True, "repeat": 3, "ease": "none"}, at)
              for w, at in [("w1", t(8, "voz")), ("w2", round(t(8, "voz") + 0.12, 3))])
    + to("#§-tit", {"opacity": 0, "duration": 0.2}, t(8, "aceptar") - 0.05)
    + to("#§-sp", {"y": -330, "scale": 0.7, "duration": 0.45, "ease": "power3.inOut"}, t(8, "aceptar") - 0.05)
    + st("#§-pw", {"opacity": 1}, t(8, "aceptar"))
    + ft("#§-p0", {"y": -700, "opacity": 0, "scale": 0.92}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.5, "ease": "back.out(1.3)"}, t(8, "aceptar"))
    + st("#§-p1, #§-p2", {"opacity": 0}, 0)
    + st("#§-p0", {"opacity": 0}, t(8, "considerar")) + st("#§-p1", {"opacity": 1}, t(8, "considerar"))
    + st("#§-p1", {"opacity": 0}, t(8, "rechazar")) + st("#§-p2", {"opacity": 1}, t(8, "rechazar"))
    + "".join(ft("#§-pw", {"scale": 1.06}, {"scale": 1, "duration": 0.3, "ease": "power3.out"}, t(8, w)) for w in ["considerar", "rechazar"])
    + push(c, 0, 1.04))

# 9 — Tú decides: Ojo: Checa no acepta los viajes por ti, solo te dice cuál conviene y cuál no.
c = "f09-decides"
F["09-decides"] = frame(9, c, popcss(c) + """
    #§-tit { top: 120px; height: 190px; } #§-tit > div { font-size: 150px; } #§-tit em { font-style: normal; color: #22C55E; }
    #§-pw { position: absolute; left: 273px; top: 330px; width: 533px; height: 420px; }
    #§-card { left: 230px; top: 820px; }
    #§-no { position: absolute; left: 790px; top: 1190px; width: 150px; height: 150px; }
    #§-fin { position: absolute; left: 495px; top: 1235px; width: 130px; height: 130px; border-radius: 50%; background: rgba(241,245,242,.92); box-shadow: 0 12px 40px rgba(0,0,0,.6); }
""", f"""      <div class="P-glow"></div>
      <div id="§-tit" class="P-mask"><div>tú <em>decides</em></div></div>
      <div id="§-pw">{pop(c, 0, *VERDE, u=0.62)}{pop(c, 1, *ROJO, u=0.62)}</div>
      <div id="§-cw" style="position:absolute;inset:0"><div id="§-card" class="P-card">{CARD_VERDE}</div></div>
      <svg id="§-no" viewBox="0 0 150 150"><circle cx="75" cy="75" r="60" fill="#181C1A" stroke="#F43F5E" stroke-width="12"/><path d="M33 117 L117 33" stroke="#F43F5E" stroke-width="12" stroke-linecap="round"/></svg>
      <div id="§-fin"></div>
      <div class="P-ej">Números de ejemplo</div>""",
    ft("#§-cw", {"y": 700, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, 0)
    + st("#§-p1", {"opacity": 0}, 0) + st("#§-no", {"opacity": 0}, 0) + st("#§-pw", {"opacity": 0}, 0)
    + st("#§-pw", {"opacity": 1}, t(9, "checa"))
    + ft("#§-p0", {"y": -500, "opacity": 0, "scale": 0.9}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.45, "ease": "back.out(1.3)"}, t(9, "checa"))
    + ft("#§-no", {"scale": 0, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(9, "acepta"))
    + shake("#§-pw", round(t(9, "acepta") + 0.15, 3), 10)
    # el dedo (tú) llega al botón Aceptar y lo toca
    + ft("#§-fin", {"x": 300, "y": 260, "opacity": 0}, {"x": 0, "y": 0, "opacity": 1, "duration": 0.5, "ease": "power3.out"}, t(9, "viajes"))
    + to("#§-no", {"opacity": 0, "scale": 0.6, "duration": 0.2}, round(t(9, "viajes") + 0.3, 3))
    + to("#§-fin", {"scale": 0.8, "duration": 0.1, "yoyo": True, "repeat": 1}, round(t(9, "ti") + 0.1, 3))
    + to("#§-card .b", {"background": "#16A34A", "scale": 0.95, "duration": 0.12, "yoyo": True, "repeat": 1}, round(t(9, "ti") + 0.1, 3))
    + mask_in("#§-tit > div", t(9, "ti"), 0.4)
    + to("#§-pw", {"scale": 1.08, "duration": 0.15, "yoyo": True, "repeat": 1}, t(9, "conviene"))
    + st("#§-p0", {"opacity": 0}, t(9, "cuál", 1)) + st("#§-p1", {"opacity": 1}, t(9, "cuál", 1))
    + to("#§-pw", {"scale": 1.08, "duration": 0.15, "yoyo": True, "repeat": 1}, t(9, "cuál", 1))
    # la cámara sigue el dedo y luego vuelve al aviso
    + to("#§-stage", {"scale": 1.08, "transformOrigin": "560px 1250px", "duration": 0.5, "ease": "power2.inOut"}, t(9, "viajes"))
    + to("#§-stage", {"scale": 1.0, "transformOrigin": "540px 650px", "duration": 0.6, "ease": "power2.inOut"}, t(9, "solo")))

# 10 — CTA: Descarga Checa y checa cuánto te queda de cada viaje.
c = "f10-cta"
LOGO = """<svg id="§-mark" viewBox="0 0 22 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <defs><filter id="§-gb" x="-150%" y="-150%" width="400%" height="400%"><feGaussianBlur stdDeviation="2.4" /></filter></defs>
          <path id="§-body" d="M 7 1 L 15 1 Q 21 1 21 7 L 21 31 Q 21 37 15 37 L 7 37 Q 1 37 1 31 L 1 7 Q 1 1 7 1 Z" fill="#111413" fill-opacity="0" stroke="#AFB7B2" stroke-width="0.9" stroke-linejoin="round" />
          <circle id="§-glow" cx="11" cy="28.5" r="6" fill="#22C55E" filter="url(#§-gb)" opacity="0" />
          <circle id="§-red" cx="11" cy="9.5" r="4.2" fill="#F43F5E" />
          <circle id="§-yellow" cx="11" cy="19" r="4.2" fill="#FACC15" />
          <circle id="§-green" cx="11" cy="28.5" r="4.2" fill="#22C55E" />
        </svg>"""
F["10-cta"] = frame(10, c, """
    #§-bloom { position: absolute; left: -10px; top: 60px; width: 1100px; height: 1100px; border-radius: 50%; background: radial-gradient(circle at 50% 50%, rgba(34,197,94,0.42) 0%, rgba(34,197,94,0.16) 34%, rgba(34,197,94,0) 68%); }
    #§-lock { position: absolute; left: 103px; top: 300px; width: 874px; height: 397px; }
    #§-mark { position: absolute; left: 0; top: 0; width: 230px; height: 397px; z-index: 2; overflow: visible; }
    #§-wm { position: absolute; left: 274px; top: 0; width: 600px; height: 397px; overflow: hidden; z-index: 1; }
    #§-word { position: absolute; left: 0; top: 78px; font-size: 220px; line-height: 1; font-weight: 700; letter-spacing: -0.03em; color: #FFFFFF; white-space: nowrap; }
    #§-pill { position: absolute; left: 0; right: 0; top: 820px; margin: 0 auto; width: 860px; height: 130px; border-radius: 65px; background: #22C55E; display: flex; align-items: center; justify-content: center; gap: 22px; font-size: 62px; font-weight: 700; letter-spacing: -0.01em; color: #000; white-space: nowrap; }
    #§-pill b { font-family: 'Fira Code', monospace; }
    #§-sub { position: absolute; left: 0; right: 0; top: 1010px; text-align: center; font-size: 56px; font-weight: 600; color: #F1F5F2; line-height: 1.25; }
    #§-sub em { font-style: normal; color: #22C55E; }
    #§-and { position: absolute; left: 0; right: 0; top: 1220px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 500; font-size: 32px; letter-spacing: .16em; text-transform: uppercase; color: #8A938E; }
    #§-black { position: absolute; inset: 0; background: #000; opacity: 0; }
""", f"""      <div id="§-bloom"></div>
      <div id="§-lock">
        <div id="§-wm"><div id="§-word">checa</div></div>
        {LOGO}
      </div>
      <div id="§-pill"><span id="§-w1">Descarga</span><b id="§-w2">CHECA</b></div>
      <div id="§-sub"><span id="§-s1">y <em>checa</em> cuánto te queda</span><br><span id="§-s2">de cada viaje</span></div>
      <div id="§-and">solo Android</div>
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
    + ft("#§-pill", {"y": 90, "opacity": 0, "scale": 0.92}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.4, "ease": "power3.out"}, 0.15)
    + ft("#§-w1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, 0.2)
    + ft("#§-w2", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(10, "checa"))
    + to("#§-w2", {"scale": 1.12, "duration": 0.14, "ease": "power2.out"}, round(t(10, "checa") + 0.2, 3))
    + to("#§-w2", {"scale": 1, "duration": 0.25, "ease": "power2.inOut"}, round(t(10, "checa") + 0.35, 3))
    + ft("#§-s1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(10, "checa", 1))
    + ft("#§-s2", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, te(10, "de"))
    + ft("#§-and", {"opacity": 0}, {"opacity": 1, "duration": 0.3}, t(10, "viaje"))
    + '      tl.fromTo("#§-black", { opacity: 0 }, { opacity: 1, duration: 0.4, ease: "power1.in" }, D - 0.4);\n')

for k, html in F.items():
    old = re.search(r'data-composition-id="([^"]+)"', html).group(1)
    html = html.replace(f'data-composition-id="{old}"', f'data-composition-id="{k}"').replace(f'__timelines["{old}"]', f'__timelines["{k}"]')
    open(f"compositions/frames/{k}.html", "w").write(html)
print("ok", len(F), "escenas", [dur(n) for n in range(1, N + 1)], round(sum(dur(n) for n in range(1, N + 1)), 2))
