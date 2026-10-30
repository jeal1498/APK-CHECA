"""Arma compositions/frames/*.html con los tiempos de audio_meta.json (voz de Fernando).
Uso: python3 -I generar.py  (después de alinear.py). Video 07 v2 (cuenta personal): «Pasajeros y cancelaciones»."""
import json, re

m = json.load(open("audio_meta.json"))
V = {v["frame"]: v for v in m["voices"]}
N = len(V)
PAD, PAD_FIN = 0.15, 2.0
EXTRA = {7: 0.5}  # silencio antes del payoff (igual que armar.py)
LEAD = 0.25  # las entradas se adelantan a la palabra para que se vean completas cuando la voz la dice


def dur(n):
    return round(V[n]["duration_s"] + (PAD_FIN if n == N else PAD) + EXTRA.get(n, 0), 3)


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


S = 'fill="none" stroke="currentColor" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'
PIN = lambda col: f'<svg viewBox="0 0 200 260" fill="none"><path d="M100 14 C58 14 30 46 30 86 C30 140 100 238 100 238 C100 238 170 140 170 86 C170 46 142 14 100 14 Z" fill="{col}" stroke="#0D0D0D" stroke-width="8"/><circle cx="100" cy="86" r="30" fill="#0D0D0D"/></svg>'
F = {}

# 1 — Hook: Llegas al punto de recogida y el pasajero ya canceló. Ahí te quedas, con el carro parado y sin un peso.
c = "f01-cancelado"
F["01-cancelado"] = frame(1, c, """
    #§-map { position: absolute; left: 130px; top: 250px; width: 820px; height: 620px; border-radius: 44px; overflow: hidden; background: #111514 repeating-linear-gradient(0deg, #1A1F1D 0 2px, transparent 2px 100px), repeating-linear-gradient(90deg, #1A1F1D 0 2px, transparent 2px 100px); border: 3px solid #2A2E2C; box-shadow: 0 30px 80px rgba(0,0,0,.6); }
    #§-pin { position: absolute; left: 340px; top: 150px; width: 140px; height: 182px; }
    #§-ring { position: absolute; left: 290px; top: 380px; width: 240px; height: 80px; border-radius: 50%; border: 5px solid #F43F5E; opacity: 0; }
    #§-tag { position: absolute; left: 30px; top: 420px; width: 760px; height: 150px; border-radius: 30px; background: #F43F5E; color: #fff; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 78px; letter-spacing: .06em; box-shadow: 0 0 70px rgba(244,63,94,.5); }
    #§-gan { position: absolute; left: 230px; top: 980px; width: 620px; padding: 40px 0 44px; border-radius: 36px; background: #151918; border: 3px solid #2A2E2C; text-align: center; }
    #§-gan .k { font-family: 'Fira Code', monospace; font-size: 34px; letter-spacing: .16em; color: #8A938E; text-transform: uppercase; }
    #§-gan .v { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 190px; line-height: 1; color: #AFB7B2; margin-top: 10px; }
    #§-flash { position: absolute; inset: 0; background: #F43F5E; opacity: 0; }
""", f"""      <div id="§-flash"></div>
      <div id="§-map"><div id="§-pin">{PIN('#22C55E')}</div><div id="§-ring"></div><div id="§-tag">CANCELADO</div></div>
      <div id="§-gan"><div class="k">Ganado</div><div class="v" id="§-v">$0</div></div>
      """,
    ft("#§-map", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0)
    + ft("#§-pin", {"y": -260, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.5, "ease": "bounce.out"}, t(1, "recogida"))
    + ft("#§-ring", {"scale": 0.3, "opacity": 0.9}, {"scale": 1.6, "opacity": 0, "duration": 0.7, "ease": "power2.out"}, round(t(1, "recogida") + 0.4, 3))
    + st("#§-tag", {"opacity": 0}, 0)
    + ft("#§-tag", {"scale": 0.4, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(1, "canceló"))
    + ft("#§-flash", {"opacity": 0.3}, {"opacity": 0, "duration": 0.45, "ease": "power2.out", "immediateRender": False}, t(1, "canceló"))
    + shake("#§-stage", t(1, "canceló"), 14)
    + ft("#§-gan", {"y": 100, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(1, "ahí"))
    + to("#§-v", {"color": "#F43F5E", "scale": 1.12, "duration": 0.25, "ease": "back.out(2)"}, t(1, "peso"))
    + push(c, 0, 1.04))

# 2 — Open loop: Te dejo 4 jugadas… La cuarta es la que te cuida a ti.
c = "f02-cuatro"
F["02-cuatro"] = frame(2, c, """
    #§-n { top: 250px; height: 620px; } #§-n > div { font-family: 'Fira Code', monospace; text-transform: none; font-size: 560px; color: #F1F5F2; letter-spacing: -.04em; }
    #§-lab { position: absolute; left: 0; right: 0; top: 930px; text-align: center; font-family: 'Fira Code', monospace; font-size: 46px; letter-spacing: .18em; color: #8A938E; text-transform: uppercase; }
    #§-row { position: absolute; left: 0; right: 0; top: 1070px; display: flex; justify-content: center; gap: 50px; }
    .§-d { width: 130px; height: 130px; border-radius: 50%; background: #1B201E; border: 5px solid #2A2E2C; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 58px; color: #4B534F; }
""", """      <div class="P-glow"></div>
      <div id="§-n" class="P-mask"><div>4</div></div>
      <div id="§-lab">jugadas</div>
      <div id="§-row"><span class="§-d" id="§-d1">1</span><span class="§-d" id="§-d2">2</span><span class="§-d" id="§-d3">3</span><span class="§-d" id="§-d4">4</span></div>""",
    mask_in("#§-n > div", t(2, "4") - 0.05, 0.4)
    + ft("#§-lab", {"opacity": 0}, {"opacity": 1, "duration": 0.3}, t(2, "jugadas"))
    + ft("#§-row", {"y": 60, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(2, "jugadas"))
    + "".join(to(f"#§-d{i}", {"borderColor": "#22C55E", "color": "#22C55E", "duration": 0.2}, round(t(2, "jugadas") + 0.3 + 0.35 * (i - 1), 3)) for i in (1, 2, 3))
    + to("#§-d4", {"borderColor": "#F43F5E", "color": "#F43F5E", "boxShadow": "0 0 50px rgba(244,63,94,.6)", "duration": 0.2}, t(2, "cuarta"))
    + to("#§-d4", {"scale": 1.18, "duration": 0.35, "yoyo": True, "repeat": 3, "ease": "sine.inOut"}, round(t(2, "cuarta") + 0.2, 3))
    + push(c, 0, 1.04))

# 3 — Uno: antes de que te toque, aprende cuánto espera tu app.
c = "f03-espera"
F["03-espera"] = frame(3, c, """
    #§-chip { position: absolute; left: 440px; top: 200px; width: 200px; height: 200px; border-radius: 50%; border: 8px solid #22C55E; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 130px; color: #22C55E; }
    #§-card { position: absolute; left: 130px; top: 520px; width: 820px; height: 800px; border-radius: 44px; background: #151918; border: 3px solid #2A2E2C; box-shadow: 0 30px 80px rgba(0,0,0,.6); }
    #§-card .h { position: absolute; left: 0; right: 0; top: 44px; text-align: center; font-family: 'Fira Code', monospace; font-size: 34px; letter-spacing: .16em; color: #8A938E; text-transform: uppercase; }
    #§-hg { position: absolute; left: 300px; top: 140px; width: 220px; height: 400px; }
    #§-bar { position: absolute; left: 80px; right: 80px; top: 620px; height: 34px; border-radius: 17px; background: #2A2E2C; overflow: hidden; } #§-fill { height: 100%; width: 0%; background: #FACC15; }
    #§-card .l { position: absolute; left: 0; right: 0; top: 690px; text-align: center; font-size: 46px; font-weight: 600; color: #AFB7B2; }
""", """      <div id="§-chip">1</div>
      <div id="§-card"><div class="h">Reglas de espera</div>
        <svg id="§-hg" viewBox="0 0 120 220" fill="none" stroke="#F1F5F2" stroke-width="8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 10 H106 M14 210 H106"/><path d="M22 10 C22 80 60 90 60 110 C60 130 22 140 22 210 M98 10 C98 80 60 90 60 110 C60 130 98 140 98 210"/>
          <path id="§-top" d="M34 24 H86 C80 60 64 80 60 100 C56 80 40 60 34 24 Z" fill="#FACC15" stroke="none"/>
          <path id="§-bot" d="M60 120 L96 200 H24 Z" fill="#FACC15" stroke="none" opacity="0"/></svg>
        <div id="§-bar"><div id="§-fill"></div></div><div class="l">tiempo de espera de tu app</div></div>""",
    ft("#§-chip", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(3, "uno"))
    + ft("#§-card", {"y": 100, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, round(t(3, "uno") + 0.15, 3))
    + to("#§-top", {"scaleY": 0.05, "transformOrigin": "50% 100%", "duration": 2.4, "ease": "none"}, t(3, "antes"))
    + to("#§-bot", {"opacity": 1, "duration": 0.3}, t(3, "antes"))
    + ft("#§-fill", {"width": "0%"}, {"width": "100%", "duration": 2.6, "ease": "none"}, t(3, "aprende"))
    + push(c, 0, 1.04))

# 4 — Cumplido ese tiempo, cancela tú y checa si te toca cobro.
c = "f04-cancela"
F["04-cancela"] = frame(4, c, """
    #§-tx { top: 190px; height: 180px; } #§-tx > div { font-size: 150px; color: #F1F5F2; } #§-tx em { font-style: normal; color: #22C55E; }
    #§-btn { position: absolute; left: 190px; top: 620px; width: 700px; height: 190px; border-radius: 40px; background: #F43F5E; color: #fff; display: flex; align-items: center; justify-content: center; gap: 24px; font-size: 68px; font-weight: 700; box-shadow: 0 0 70px rgba(244,63,94,.4); }
    #§-ck { width: 0; height: 80px; overflow: hidden; display: flex; align-items: center; } #§-ck svg { width: 80px; height: 80px; flex: none; }
    #§-chip { position: absolute; left: 240px; top: 960px; width: 600px; height: 150px; border-radius: 75px; border: 6px solid #FACC15; color: #FACC15; display: flex; align-items: center; justify-content: center; font-size: 62px; font-weight: 700; background: rgba(250,204,21,.08); }
""", """      <div class="P-glow"></div>
      <div id="§-tx" class="P-mask"><div>cancela <em>tú</em></div></div>
      <div id="§-btn"><span id="§-ck"><svg viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12 L10 18 L20 6"/></svg></span><span>Cancelar viaje</span></div>
      <div id="§-chip">¿te toca cobro?</div>""",
    mask_in("#§-tx > div", t(4, "cancela") - 0.1, 0.4)
    + ft("#§-btn", {"y": 90, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.1)
    + to("#§-btn", {"backgroundColor": "#22C55E", "boxShadow": "0 0 80px rgba(34,197,94,.55)", "duration": 0.25}, t(4, "cancela"))
    + to("#§-ck", {"width": 80, "duration": 0.25, "ease": "power2.out"}, t(4, "cancela"))
    + to("#§-btn", {"scale": 1.06, "duration": 0.15, "yoyo": True, "repeat": 1}, t(4, "cancela"))
    + ft("#§-chip", {"scale": 0.4, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(4, "checa"))
    + push(c, 0, 1.04))

# 5 — Dos: si te cancelan estando ahí, captura de pantalla y reporte, en ese momento.
c = "f05-captura"
F["05-captura"] = frame(5, c, """
    #§-chip { position: absolute; left: 440px; top: 190px; width: 200px; height: 200px; border-radius: 50%; border: 8px solid #22C55E; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 130px; color: #22C55E; }
    #§-shot { position: absolute; left: 290px; top: 470px; width: 500px; height: 520px; border-radius: 36px; background: #F4F5F4; color: #111; padding: 44px 40px; box-shadow: 0 30px 80px rgba(0,0,0,.7); transform-origin: 50% 100%; }
    #§-shot .k { font-family: 'Fira Code', monospace; font-size: 28px; letter-spacing: .14em; color: #5C635F; text-transform: uppercase; }
    #§-shot .t { margin-top: 26px; height: 96px; border-radius: 20px; background: #F43F5E; color: #fff; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 46px; }
    #§-shot .ln { margin-top: 26px; height: 22px; border-radius: 11px; background: #D5D9D7; }
    #§-fol { position: absolute; left: 240px; top: 1100px; width: 600px; height: 330px; border-radius: 32px; background: #151918; border: 4px solid #22C55E; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 36px; font-size: 66px; font-weight: 700; color: #22C55E; box-shadow: 0 0 60px rgba(34,197,94,.25); }
    #§-ok { position: absolute; left: 440px; top: 1130px; width: 200px; height: 200px; opacity: 0; }
    #§-flash { position: absolute; inset: 0; background: #F4F5F4; opacity: 0; }
""", """      <div id="§-flash"></div>
      <div id="§-chip">2</div>
      <div id="§-shot"><div class="k">Nueva solicitud</div><div class="t">CANCELADO</div><div class="ln"></div><div class="ln" style="width:70%"></div><div class="ln" style="width:85%"></div></div>
      <div id="§-fol">Reportar</div>
      <svg id="§-ok" viewBox="0 0 24 24" fill="none" stroke="#22C55E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M7 12.5 L10.5 16 L17 8.5"/></svg>""",
    ft("#§-chip", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(5, "dos"))
    + ft("#§-shot", {"y": 100, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(5, "cancelan"))
    + ft("#§-fol", {"y": 120, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(5, "cancelan"))
    + ft("#§-flash", {"opacity": 0.6}, {"opacity": 0, "duration": 0.35, "ease": "power2.out", "immediateRender": False}, t(5, "captura"))
    + to("#§-shot", {"y": 620, "scale": 0.35, "opacity": 0.0, "duration": 0.6, "ease": "power2.in"}, t(5, "reporte"))
    + to("#§-fol", {"scale": 1.06, "duration": 0.15, "yoyo": True, "repeat": 1}, round(t(5, "reporte") + 0.5, 3))
    + ft("#§-ok", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(5, "momento"))
    + st("#§-fol", {"color": "#22C55E"}, 0)
    + push(c, 0, 1.04))

# 6 — Tres: nombre y destino, antes de quitar los seguros.
c = "f06-ficha"
F["06-ficha"] = frame(6, c, """
    #§-chip { position: absolute; left: 440px; top: 190px; width: 200px; height: 200px; border-radius: 50%; border: 8px solid #22C55E; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 130px; color: #22C55E; }
    #§-card { position: absolute; left: 140px; top: 470px; width: 800px; height: 640px; border-radius: 44px; background: #151918; border: 3px solid #2A2E2C; box-shadow: 0 30px 80px rgba(0,0,0,.6); }
    .§-r { position: absolute; left: 60px; right: 60px; height: 150px; border-radius: 28px; background: #1B201E; border: 3px solid #2A2E2C; display: flex; align-items: center; justify-content: space-between; padding: 0 44px; font-size: 54px; font-weight: 600; color: #AFB7B2; }
    .§-r svg { width: 84px; height: 84px; opacity: 0; }
    #§-r1 { top: 70px; } #§-r2 { top: 270px; }
    #§-lk { position: absolute; left: 340px; top: 1180px; width: 400px; height: 400px; }
""", """      <div id="§-chip">3</div>
      <div id="§-card"><div id="§-r1" class="§-r"><span>Nombre</span><svg id="§-k1" viewBox="0 0 24 24" fill="none" stroke="#22C55E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M7 12.5 L10.5 16 L17 8.5"/></svg></div>
        <div id="§-r2" class="§-r"><span>Destino</span><svg id="§-k2" viewBox="0 0 24 24" fill="none" stroke="#22C55E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M7 12.5 L10.5 16 L17 8.5"/></svg></div></div>
      <svg id="§-lk" viewBox="0 0 200 200" fill="none" stroke="#F1F5F2" stroke-width="10" stroke-linecap="round" stroke-linejoin="round">
        <rect x="40" y="88" width="120" height="96" rx="16"/><g id="§-sh"><path d="M64 88 V62 C64 26 136 26 136 62 V88"/></g></svg>""",
    ft("#§-chip", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(6, "tres"))
    + ft("#§-card", {"y": 100, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, round(t(6, "tres") + 0.1, 3))
    + ft("#§-lk", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, round(t(6, "tres") + 0.2, 3))
    + ft("#§-k1", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(6, "nombre"))
    + ft("#§-k2", {"scale": 0, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(6, "destino"))
    + to("#§-sh", {"y": -34, "rotation": -22, "svgOrigin": "64 88", "duration": 0.4, "ease": "back.out(2)"}, t(6, "seguros"))
    + to("#§-lk", {"stroke": "#22C55E", "duration": 0.3}, t(6, "seguros"))
    + push(c, 0, 1.04))

# 7 — Pausa dramática: Cuatro, la que no se negocia:
c = "f07-negocia"
F["07-negocia"] = frame(7, c, """
    #§-n { top: 360px; height: 720px; } #§-n > div { font-family: 'Fira Code', monospace; text-transform: none; font-size: 680px; color: #F43F5E; text-shadow: 0 0 110px rgba(244,63,94,.5); letter-spacing: -.04em; }
    #§-t { position: absolute; left: 0; right: 0; top: 1180px; text-align: center; font-size: 76px; font-weight: 700; color: #F1F5F2; }
""", """      <div id="§-n" class="P-mask"><div>4</div></div>
      <div id="§-t">la que no se negocia</div>""",
    mask_in("#§-n > div", 0.0, 0.4)
    + ft("#§-t", {"opacity": 0, "y": 20}, {"opacity": 1, "y": 0, "duration": 0.3}, t(7, "negocia"))
    + to("#§-n > div", {"scale": 1.05, "transformOrigin": "50% 50%", "duration": 0.4, "yoyo": True, "repeat": 3, "ease": "sine.inOut"}, 0.5)
    + push(c, 0, 1.08), ground="#000000")

# 8 — Payoff: Si el pasajero se pone pesado, no discutas. Búscate un lugar seguro y cancela.
c = "f08-seguro"
F["08-seguro"] = frame(8, c, """
    #§-tx { top: 160px; height: 180px; } #§-tx > div { font-size: 150px; color: #F43F5E; }
    #§-bub { position: absolute; left: 340px; top: 400px; width: 400px; height: 340px; }
    #§-pin { position: absolute; left: 400px; top: 800px; width: 280px; height: 364px; }
    #§-lab { position: absolute; left: 190px; top: 1230px; width: 700px; height: 130px; border-radius: 65px; border: 6px solid #22C55E; color: #22C55E; display: flex; align-items: center; justify-content: center; font-size: 62px; font-weight: 700; background: rgba(34,197,94,.08); }
    #§-flash { position: absolute; inset: 0; background: #22C55E; opacity: 0; }
""", f"""      <div id="§-flash"></div>
      <div id="§-tx" class="P-mask"><div>no discutas</div></div>
      <svg id="§-bub" viewBox="0 0 200 170" fill="none" stroke="#F1F5F2" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"><path d="M24 20 H176 Q190 20 190 34 V110 Q190 124 176 124 H98 L56 156 V124 H24 Q10 124 10 110 V34 Q10 20 24 20 Z"/><path d="M44 56 H156 M44 84 H120" stroke-width="7"/><path id="§-x" d="M14 150 L186 8" stroke="#F43F5E" stroke-width="12"/></svg>
      <div id="§-pin">{PIN('#22C55E')}</div>
      <div id="§-lab">Lugar seguro</div>""",
    mask_in("#§-tx > div", t(8, "no") - 0.05, 0.35)
    + ft("#§-bub", {"scale": 0.5, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(8, "pesado"))
    + f'      var xl = document.getElementById("§-x"); var xln = xl.getTotalLength(); xl.style.strokeDasharray = String(xln);\n'
    + f'      tl.fromTo(xl, {{ strokeDashoffset: xln }}, {{ strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }}, {t(8, "discutas")});\n'
    + ft("#§-pin", {"y": -500, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.5, "ease": "bounce.out"}, t(8, "lugar"))
    + ft("#§-lab", {"scale": 0.5, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.35, "ease": "back.out(2)"}, t(8, "seguro"))
    + ft("#§-flash", {"opacity": 0.3}, {"opacity": 0, "duration": 0.45, "ease": "power2.out", "immediateRender": False}, t(8, "cancela"))
    + push(c, 0, 1.04))

# 9 — Giro emocional: Ningún viaje vale una pelea.
c = "f09-pelea"
F["09-pelea"] = frame(9, c, """
    #§-l1 { top: 170px; height: 160px; } #§-l1 > div { font-size: 118px; color: #AFB7B2; }
    #§-l2 { top: 330px; height: 190px; } #§-l2 > div { font-size: 150px; color: #FACC15; }
    #§-house { position: absolute; left: 270px; top: 640px; width: 540px; height: 540px; }
    #§-win { opacity: 0; }
""", """      <div id="§-l1" class="P-mask"><div>ningún viaje</div></div>
      <div id="§-l2" class="P-mask"><div>vale una pelea</div></div>
      <svg id="§-house" viewBox="0 0 200 200" fill="none" stroke="#F1F5F2" stroke-width="7" stroke-linecap="round" stroke-linejoin="round">
        <path d="M20 96 L100 28 L180 96"/><path d="M36 84 V172 H164 V84"/><path d="M84 172 V126 H116 V172"/>
        <g id="§-win"><rect x="48" y="104" width="26" height="26" rx="3" fill="#FACC15" stroke="#FACC15"/></g></svg>""",
    mask_in("#§-l1 > div", t(9, "ningún") - 0.05, 0.35)
    + mask_in("#§-l2 > div", t(9, "vale") - 0.05, 0.4)
    + ft("#§-house", {"y": 80, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.1)
    + ft("#§-win", {"opacity": 0}, {"opacity": 1, "duration": 0.35}, t(9, "vale"))
    + to("#§-house", {"filter": "drop-shadow(0 0 40px rgba(250,204,21,.45))", "duration": 0.5}, t(9, "vale"))
    + push(c, 0, 1.05))

# 10 — CTA: Mándaselo a un compañero que maneje de noche. Sígueme para más.
c = "f10-avion"
F["10-avion"] = frame(10, c, """
    #§-tx { top: 190px; height: 190px; } #§-tx > div { font-size: 160px; color: #F1F5F2; } #§-tx em { font-style: normal; color: #22C55E; }
    #§-pl { position: absolute; left: 60px; top: 640px; width: 240px; height: 240px; }
    #§-moon { position: absolute; left: 740px; top: 430px; width: 200px; height: 200px; opacity: 0; }
    #§-btn { position: absolute; left: 290px; top: 1060px; width: 500px; height: 150px; border-radius: 75px; background: #22C55E; color: #000; display: flex; align-items: center; justify-content: center; font-size: 64px; font-weight: 700; }
""", """      <div class="P-glow"></div>
      <div id="§-tx" class="P-mask"><div>mándaselo</div></div>
      <svg id="§-moon" viewBox="0 0 24 24" fill="#FACC15"><path d="M20 14.5 A8.5 8.5 0 1 1 9.5 4 A7 7 0 0 0 20 14.5 Z"/></svg>
      <svg id="§-pl" viewBox="0 0 24 24" fill="none" stroke="#F1F5F2" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2 L2 10 L10 13 L13 21 Z"/><path d="M22 2 L10 13"/></svg>
      <div id="§-btn">+ Seguir</div>""",
    mask_in("#§-tx > div", t(10, "mándaselo") - 0.05, 0.35)
    + ft("#§-pl", {"x": 0, "y": 80, "opacity": 0}, {"x": 0, "y": 0, "opacity": 1, "duration": 0.3}, t(10, "mándaselo"))
    + ft("#§-pl", {"x": 0, "y": 0}, {"x": 480, "y": 150, "duration": 0.9, "ease": "power2.inOut", "immediateRender": False}, t(10, "compañero"))
    + ft("#§-moon", {"scale": 0.4, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.4, "ease": "back.out(2)"}, t(10, "noche"))
    + ft("#§-btn", {"scale": 0.6, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.4, "ease": "back.out(2)"}, t(10, "sígueme"))
    + to("#§-btn", {"scale": 1.08, "duration": 0.2, "yoyo": True, "repeat": 1}, round(t(10, "sígueme") + 0.5, 3))
    + push(c, 0, 1.04))

# 11 — Cierre estándar: Descarga Checa y checa cuánto te queda de cada viaje.
c = "f11-cierre"
LOGO = """<svg id="§-mark" viewBox="0 0 22 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <defs><filter id="§-gb" x="-150%" y="-150%" width="400%" height="400%"><feGaussianBlur stdDeviation="2.4" /></filter></defs>
          <path id="§-body" d="M 7 1 L 15 1 Q 21 1 21 7 L 21 31 Q 21 37 15 37 L 7 37 Q 1 37 1 31 L 1 7 Q 1 1 7 1 Z" fill="#111413" fill-opacity="0" stroke="#AFB7B2" stroke-width="0.9" stroke-linejoin="round" />
          <circle id="§-glow" cx="11" cy="28.5" r="6" fill="#22C55E" filter="url(#§-gb)" opacity="0" />
          <circle id="§-red" cx="11" cy="9.5" r="4.2" fill="#F43F5E" />
          <circle id="§-yellow" cx="11" cy="19" r="4.2" fill="#FACC15" />
          <circle id="§-green" cx="11" cy="28.5" r="4.2" fill="#22C55E" />
        </svg>"""
F["11-cierre"] = frame(11, c, """
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
      <div id="§-rC" class="§-row"><div class="l">descarga en la web · solo Android</div><div class="h">jeal1498.github.io/PWA-Checa-Multimedia</div></div>
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
    + ft("#§-w2", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(11, "checa"))
    + to("#§-w2", {"scale": 1.12, "duration": 0.14, "ease": "power2.out"}, round(t(11, "checa") + 0.2, 3))
    + to("#§-w2", {"scale": 1, "duration": 0.25, "ease": "power2.inOut"}, round(t(11, "checa") + 0.35, 3))
    + ft("#§-s1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, tend(11, "checa", 0))
    + ft("#§-rA", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "cuánto"))
    + ft("#§-rB", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "queda"))
    + ft("#§-rC", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "viaje"))
    + '      tl.fromTo("#§-black", { opacity: 0 }, { opacity: 1, duration: 0.4, ease: "power1.in" }, D - 0.4);\n')

for k, html in F.items():
    old = re.search(r'data-composition-id="([^"]+)"', html).group(1)
    html = html.replace(f'data-composition-id="{old}"', f'data-composition-id="{k}"').replace(f'__timelines["{old}"]', f'__timelines["{k}"]')
    open(f"compositions/frames/{k}.html", "w", encoding="utf-8").write(html)
print("ok", len(F), "escenas", [dur(n) for n in range(1, N + 1)], round(sum(dur(n) for n in range(1, N + 1)), 2))
