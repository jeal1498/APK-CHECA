"""Arma compositions/frames/*.html con los tiempos de audio_meta.json (voz de Fernando). Uso: python3 -I generar.py"""
import json, re

m = json.load(open("audio_meta.json"))
V = {v["frame"]: v for v in m["voices"]}
PAD, PAD_FIN = 0.15, 2.0
LEAD = 0.25
N = len(V)


def dur(n):
    return round(V[n]["duration_s"] + (PAD_FIN if n == N else PAD), 3)


def t(n, palabra, k=0, off=-LEAD):
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡.,:").startswith(palabra.lower())]
    return round(max(0.0, ws[k]["start"] + off), 3)


def tend(n, palabra, k=0):
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡.,:").startswith(palabra.lower())]
    return round(ws[k]["end"], 3)


FUENTES = """
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Regular.ttf') format('truetype'); font-weight: 400; font-display: block; }
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Medium.ttf') format('truetype'); font-weight: 500 600; font-display: block; }
    @font-face{ font-family: 'Fira Sans'; src: url('assets/fonts/FiraSans-Bold.ttf') format('truetype'); font-weight: 700 900; font-display: block; }
    @font-face{ font-family: 'Fira Code'; src: url('assets/fonts/FiraCode.ttf') format('truetype'); font-weight: 300 700; font-display: block; }
    #root { position: absolute; inset: 0; width: 1080px; height: 1920px; overflow: hidden; font-family: 'Fira Sans', sans-serif; color: #F1F5F2; }
    .clip { position: absolute; }
    .P-ground { inset: 0; background: #000; }
    .P-stage { left: 0; top: 0; width: 1080px; height: 1600px; transform-origin: 50% 45%; }
    .P-kick { position: absolute; left: 0; right: 0; top: 150px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 500; font-size: 32px; letter-spacing: .16em; text-transform: uppercase; color: #22C55E; }
    .P-kick:before { content: ""; display: block; width: 36px; height: 2px; background: currentColor; margin: 0 auto 18px; }
    .P-mask { position: absolute; left: 0; right: 0; overflow: hidden; text-align: center; }
    .P-mask > div { font-weight: 800; text-transform: lowercase; letter-spacing: -.04em; line-height: 1.02; white-space: nowrap; }
    .P-ej { position: absolute; left: 0; right: 0; top: 1500px; text-align: center; font-family: 'Fira Code', monospace; font-size: 26px; letter-spacing: .12em; text-transform: uppercase; color: #6B736F; }
    .P-card { position: absolute; width: 620px; border-radius: 36px; background: #F4F5F4; color: #111; padding: 48px 52px 44px; box-shadow: 0 30px 80px rgba(0,0,0,.7); }
    .P-card .k { font-family: 'Fira Code', monospace; font-weight: 500; font-size: 28px; letter-spacing: .14em; text-transform: uppercase; color: #5C635F; }
    .P-card .f { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 112px; letter-spacing: -.03em; line-height: 1; margin-top: 22px; }
    .P-card .r { display: flex; align-items: center; gap: 22px; font-size: 40px; font-weight: 500; margin-top: 28px; color: #222; }
    .P-card .d { width: 20px; height: 20px; border-radius: 50%; background: #111; flex: none; }
    .P-card .s { width: 20px; height: 20px; background: #111; flex: none; }
    .P-card .b { margin-top: 38px; height: 104px; border-radius: 20px; background: #111; color: #F4F5F4; display: flex; align-items: center; justify-content: center; font-size: 44px; font-weight: 700; }
"""
CARD = """<div class="k">Nueva solicitud</div><div class="f">$85.00</div>
          <div class="r"><span class="d"></span>A 4 min (1.5 km)</div>
          <div class="r"><span class="s"></span>Viaje: 18 min (7.5 km)</div>
          <div class="b">Aceptar</div>"""


def frame(n, cid, css, body, js, ground="#000"):
    d = dur(n)
    css, body, js = (x.replace("§", cid) for x in (css, body, js))
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
{js}
      window.__timelines["{cid}"] = tl;
    }})();
  </script>
</template>
"""


def FT(sel, a, b, at):
    return f'      tl.fromTo("{sel}", {json.dumps(a)}, {json.dumps(b)}, {at});\n'


def TO(sel, b, at):
    return f'      tl.to("{sel}", {json.dumps(b)}, {at});\n'


def SET(sel, b, at):
    return f'      tl.set("{sel}", {json.dumps(b)}, {at});\n'


def mask(sel, at, d=0.3):
    return FT(sel, {"yPercent": 220}, {"yPercent": 0, "duration": d, "ease": "power3.out"}, at)


def push(at=0, s1=1.06):
    return f'      tl.fromTo("#§-stage", {{ scale: 1 }}, {{ scale: {s1}, duration: D - {at}, ease: "none" }}, {at});\n'


def shake(at, amp=14):
    xs = [amp, -amp * .85, amp * .6, -amp * .4, amp * .2, 0]
    return "".join(f'      tl.to("#§-stage", {{ x: {round(x, 1)}, duration: 0.05, ease: "none" }}, {round(at + i * 0.05, 3)});\n' for i, x in enumerate(xs))


F = {}
RED, GREEN, YEL, GRAY = "#F43F5E", "#22C55E", "#FACC15", "#2A2E2C"

# ---------- espina (escena 1 y 10) ----------
OFF = [10, 18, 22, 18, 8, -6, -14, -10]
SPINE_CSS = "".join(f"    #§-v{i} {{ position: absolute; left: {455 + OFF[i]}px; top: {520 + 76 * i}px; width: 170px; height: 58px; border-radius: 29px; background: {GRAY}; }}\n" for i in range(8))
SPINE_HTML = "".join(f'      <div id="§-v{i}"></div>\n' for i in range(8))

# 1 — Seis de la tarde. La espalda ya no da más y te faltan horas.
F["01-tarde"] = frame(1, "f01-tarde", """
    #§-pm { top: 170px; height: 290px; } #§-pm > div { font-family: 'Fira Code', monospace; font-weight: 700; font-size: 250px; text-transform: none; letter-spacing: -.05em; word-spacing: -.3em; color: #FACC15; }
    #§-glow { position: absolute; left: 190px; top: 420px; width: 700px; height: 800px; border-radius: 50%; background: radial-gradient(circle, rgba(244,63,94,.28), rgba(244,63,94,0) 65%); opacity: 0; }
    #§-tx { top: 1210px; height: 150px; } #§-tx > div { font-size: 112px; color: #F1F5F2; } #§-tx em { font-style: normal; color: #F43F5E; }
""" + SPINE_CSS, """      <div id="§-glow"></div>
      <div id="§-pm" class="P-mask"><div>6 PM</div></div>
""" + SPINE_HTML + """      <div id="§-tx" class="P-mask"><div>te faltan <em>horas</em></div></div>""",
    mask("#§-pm > div", 0.0, 0.35)
    + FT("#§-v0,#§-v1,#§-v2,#§-v3,#§-v4,#§-v5,#§-v6,#§-v7", {"scaleX": 0.3, "opacity": 0}, {"scaleX": 1, "opacity": 1, "duration": 0.3, "stagger": 0.04, "ease": "back.out(1.6)"}, 0.05)
    + "".join(TO(f"#§-v{i}", {"backgroundColor": RED, "duration": 0.15, "ease": "none"}, round(t(1, "espalda") + (7 - i) * 0.07, 3)) for i in range(8))
    + TO("#§-glow", {"opacity": 1, "duration": 0.5, "ease": "power2.out"}, t(1, "espalda"))
    + mask("#§-tx > div", t(1, "faltan"), 0.35)
    + shake(t(1, "más", 0, 0.0), 16)
    + push(0, 1.05))

# 2 — Al carro le haces servicio. A tu cuerpo, casi nunca.
CK = '<svg viewBox="0 0 24 24" width="56" height="56"><circle cx="12" cy="12" r="11" fill="#22C55E"/><path d="M6.5 12.5l3.6 3.6 7.4-7.7" fill="none" stroke="#04120A" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
DS = '<svg viewBox="0 0 24 24" width="56" height="56"><circle cx="12" cy="12" r="11" fill="none" stroke="#6B736F" stroke-width="2"/><path d="M7 12h10" stroke="#6B736F" stroke-width="2.6" stroke-linecap="round"/></svg>'
XX = '<svg viewBox="0 0 24 24" width="56" height="56"><circle cx="12" cy="12" r="11" fill="#F43F5E"/><path d="M8 8l8 8M16 8l-8 8" stroke="#fff" stroke-width="2.6" stroke-linecap="round"/></svg>'
rowsA = "".join(f'<div class="§-row" id="§-a{i}"><span>{x}</span><i>{CK}</i></div>' for i, x in enumerate(["Aceite", "Llantas", "Frenos"]))
rowsB = "".join(f'<div class="§-row" id="§-b{i}"><span>{x}</span><i><u>{DS}</u><s>{XX}</s></i></div>' for i, x in enumerate(["Espalda", "Piernas", "Descanso"]))
F["02-servicio"] = frame(2, "f02-servicio", """
    .§-card { position: absolute; left: 130px; width: 820px; border-radius: 40px; background: #0B0D0C; border: 3px solid #2A2E2C; padding: 44px 56px 36px; }
    .§-card h3 { font-family: 'Fira Code', monospace; font-weight: 600; font-size: 34px; letter-spacing: .16em; text-transform: uppercase; margin-bottom: 14px; }
    .§-row { display: flex; justify-content: space-between; align-items: center; height: 100px; font-size: 58px; font-weight: 600; color: #F1F5F2; }
    .§-row i { position: relative; width: 56px; height: 56px; display: block; } .§-row i u, .§-row i s { position: absolute; inset: 0; text-decoration: none; }
    #§-ca { top: 190px; } #§-ca h3 { color: #22C55E; }
    #§-cb { top: 770px; } #§-cb h3 { color: #F43F5E; }
    #§-tx { top: 1240px; height: 190px; } #§-tx > div { font-size: 170px; color: #F43F5E; }
""", f"""      <div id="§-ca" class="§-card"><h3>El carro</h3>{rowsA}</div>
      <div id="§-cb" class="§-card"><h3>Tú</h3>{rowsB}</div>
      <div id="§-tx" class="P-mask"><div>casi nunca</div></div>""",
    FT("#§-ca", {"x": -900, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, 0.0)
    + "".join(FT(f"#§-a{i} i", {"scale": 0}, {"scale": 1, "duration": 0.25, "ease": "back.out(2.4)"}, round(t(2, "haces") + 0.12 * i, 3)) for i in range(3))
    + FT("#§-cb", {"x": 900, "opacity": 0}, {"x": 0, "opacity": 1, "duration": 0.45, "ease": "power3.out"}, t(2, "a", 1, -0.45))
    + "".join(SET(f"#§-b{i} i s", {"opacity": 0}, 0) for i in range(3))
    + "".join(TO(f"#§-b{i} i u", {"opacity": 0, "duration": 0.1}, round(t(2, "casi") + 0.1 * i, 3)) + FT(f"#§-b{i} i s", {"opacity": 0, "scale": 0.3}, {"opacity": 1, "scale": 1, "duration": 0.22, "ease": "back.out(2.4)"}, round(t(2, "casi") + 0.1 * i, 3)) for i in range(3))
    + mask("#§-tx > div", t(2, "nunca", 0, -0.2), 0.4)
    + push(0, 1.04), ground="#F1F5F2")
# la escena clara necesita color de texto oscuro en la máscara roja (ya es rojo); sin cambios

# 3 — Te dejo 4 hábitos. El cuarto puede evitarte un accidente.
circ = "".join(f'<div class="§-c" id="§-c{i}">{i}</div>' for i in range(1, 5))
WARN = '<svg viewBox="0 0 24 24" width="150" height="150"><path d="M12 3L2.5 20h19L12 3z" fill="#F43F5E" stroke="#F43F5E" stroke-width="1.5" stroke-linejoin="round"/><path d="M12 9.5v5" stroke="#fff" stroke-width="2.2" stroke-linecap="round"/><circle cx="12" cy="17.2" r="1.3" fill="#fff"/></svg>'
F["03-habitos"] = frame(3, "f03-habitos", """
    #§-tt { top: 250px; height: 230px; } #§-tt > div { font-size: 200px; } #§-tt em { font-style: normal; color: #22C55E; }
    .§-c { position: absolute; top: 640px; width: 190px; height: 190px; border-radius: 50%; border: 8px solid #3A403D; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 110px; color: #F1F5F2; background: #0B0D0C; }
    #§-c1 { left: 115px; } #§-c2 { left: 335px; } #§-c3 { left: 555px; } #§-c4 { left: 775px; }
    #§-w { position: absolute; left: 790px; top: 440px; opacity: 0; }
    #§-l1 { top: 1010px; height: 130px; } #§-l1 > div { font-size: 76px; color: #AFB7B2; }
    #§-l2 { top: 1130px; height: 190px; } #§-l2 > div { font-size: 150px; color: #F43F5E; }
""", f"""      <div id="§-tt" class="P-mask"><div><em>4</em> hábitos</div></div>
      {circ}
      <div id="§-w">{WARN}</div>
      <div id="§-l1" class="P-mask"><div>el cuarto puede evitarte</div></div>
      <div id="§-l2" class="P-mask"><div>un accidente</div></div>""",
    mask("#§-tt > div", t(3, "4", 0, -0.2), 0.35)
    + FT("#§-c1,#§-c2,#§-c3,#§-c4", {"scale": 0}, {"scale": 1, "duration": 0.3, "stagger": 0.1, "ease": "back.out(2)"}, t(3, "hábitos", 0, -0.15))
    + TO("#§-c1,#§-c2,#§-c3", {"opacity": 0.3, "duration": 0.25}, t(3, "cuarto"))
    + TO("#§-c4", {"borderColor": RED, "color": RED, "scale": 1.28, "duration": 0.3, "ease": "back.out(2)"}, t(3, "cuarto"))
    + TO("#§-c4", {"backgroundColor": "rgba(244,63,94,.18)", "duration": 0.25}, t(3, "cuarto"))
    + mask("#§-l1 > div", t(3, "puede", 0, -0.1), 0.35)
    + mask("#§-l2 > div", t(3, "accidente", 0, -0.15), 0.4)
    + FT("#§-w", {"scale": 0, "opacity": 0, "rotation": -20}, {"scale": 1, "opacity": 1, "rotation": 0, "duration": 0.35, "ease": "back.out(2.4)"}, t(3, "accidente", 0, -0.15))
    + shake(t(3, "accidente", 0, -0.1), 12)
    + push(0, 1.05))

# 4 — Uno: cada dos horas, bájate y camina cinco minutos.
TICKS = "".join(f'<line x1="200" y1="22" x2="200" y2="{40 if i % 3 == 0 else 32}" stroke="#6B736F" stroke-width="{6 if i % 3 == 0 else 3}" transform="rotate({i * 30} 200 200)"/>' for i in range(12))
F["04-uno"] = frame(4, "f04-uno", """
    #§-clock { position: absolute; left: 330px; top: 270px; width: 420px; height: 420px; }
    #§-h2 { position: absolute; left: 0; right: 0; top: 700px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 100px; color: #FACC15; }
    #§-fig { position: absolute; left: 340px; top: 820px; width: 400px; height: 480px; }
    #§-gnd { position: absolute; left: 140px; width: 800px; top: 1290px; height: 6px; background: repeating-linear-gradient(90deg, #3A403D 0 46px, transparent 46px 92px); }
    #§-chip { position: absolute; left: 330px; top: 1370px; width: 420px; height: 130px; border-radius: 65px; background: #FACC15; color: #04120A; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 74px; letter-spacing: .02em; }
""", f"""      <div class="P-kick">hábito 1 / 4</div>
      <svg id="§-clock" viewBox="0 0 400 400"><circle cx="200" cy="200" r="180" fill="#0B0D0C" stroke="#F1F5F2" stroke-width="10"/>{TICKS}
        <g id="§-hand"><line x1="200" y1="200" x2="200" y2="64" stroke="#FACC15" stroke-width="12" stroke-linecap="round"/></g>
        <circle cx="200" cy="200" r="16" fill="#F1F5F2"/></svg>
      <div id="§-h2">2 h</div>
      <svg id="§-fig" viewBox="0 0 400 480" fill="none" stroke="#22C55E" stroke-width="22" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="205" cy="60" r="38" fill="#22C55E" stroke="none"/>
        <path d="M200 110 L196 250"/>
        <g id="§-ar1"><path d="M200 140 L200 235"/></g>
        <g id="§-ar2"><path d="M200 140 L200 235"/></g>
        <g id="§-lg1"><path d="M196 250 L196 390"/></g>
        <g id="§-lg2"><path d="M196 250 L196 390"/></g></svg>
      <div id="§-gnd"></div>
      <div id="§-chip">5 min</div>""",
    FT("#§-clock", {"scale": 0.4, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.4, "ease": "back.out(1.8)"}, 0.0)
    + FT("#§-hand", {"rotation": 0, "svgOrigin": "200 200"}, {"rotation": 720, "svgOrigin": "200 200", "duration": round(tend(4, "horas") - t(4, "dos") + 0.2, 3), "ease": "power1.inOut"}, t(4, "dos"))
    + FT("#§-h2", {"scale": 0.4, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.3, "ease": "back.out(2)"}, t(4, "dos"))
    + FT("#§-fig", {"y": 120, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, t(4, "bájate", 0, -0.1))
    + "".join(f'      tl.fromTo("#§-{n}", {{ rotation: {a}, svgOrigin: "{o}" }}, {{ rotation: {b}, svgOrigin: "{o}", duration: 0.3, repeat: 9, yoyo: true, ease: "sine.inOut" }}, {t(4, "bájate", 0, 0.1)});\n' for n, a, b, o in [("lg1", -26, 26, "196 250"), ("lg2", 26, -26, "196 250"), ("ar1", 22, -22, "200 140"), ("ar2", -22, 22, "200 140")])
    + f'      tl.fromTo("#§-gnd", {{ backgroundPosition: "0px 0px" }}, {{ backgroundPosition: "-368px 0px", duration: D - {t(4, "bájate", 0, 0.1)}, ease: "none" }}, {t(4, "bájate", 0, 0.1)});\n'
    + FT("#§-chip", {"scale": 0, "rotation": -8}, {"scale": 1, "rotation": 0, "duration": 0.35, "ease": "back.out(2.2)"}, t(4, "cinco"))
    + push(0, 1.04))

# 5 — Dos: agua a la mano. Si no la ves, no la tomas.
EYE = '<svg viewBox="0 0 24 24" width="300" height="300" fill="none" stroke="#F43F5E" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M1.5 12S5.5 5 12 5s10.5 7 10.5 7-4 7-10.5 7S1.5 12 1.5 12z"/><circle cx="12" cy="12" r="3.2"/><path d="M3 21L21 3" stroke-width="2.2"/></svg>'
F["05-dos"] = frame(5, "f05-dos", """
    #§-ring { position: absolute; left: 270px; top: 330px; width: 540px; height: 540px; border-radius: 50%; border: 6px dashed #22C55E; opacity: 0; }
    #§-bt { position: absolute; left: 430px; top: 300px; width: 220px; }
    #§-cap { position: absolute; left: 60px; top: 0; width: 100px; height: 60px; border-radius: 14px 14px 6px 6px; background: #F1F5F2; }
    #§-neck { position: absolute; left: 40px; top: 52px; width: 140px; height: 60px; border: 8px solid #F1F5F2; border-bottom: none; border-radius: 30px 30px 0 0; }
    #§-body { position: absolute; left: 0; top: 108px; width: 220px; height: 440px; border: 8px solid #F1F5F2; border-radius: 40px 40px 56px 56px; overflow: hidden; background: #0B0D0C; }
    #§-wat { position: absolute; left: 0; right: 0; bottom: 0; height: 0%; background: #38BDF8; }
    #§-eye { position: absolute; left: 390px; top: 420px; opacity: 0; }
    #§-t1 { top: 980px; height: 160px; } #§-t1 > div { font-size: 130px; color: #22C55E; }
    #§-t2 { top: 980px; height: 130px; } #§-t2 > div { font-size: 100px; color: #AFB7B2; }
    #§-t3 { top: 1110px; height: 190px; } #§-t3 > div { font-size: 150px; color: #F43F5E; }
""", f"""      <div class="P-kick">hábito 2 / 4</div>
      <div id="§-ring"></div>
      <div id="§-bt"><div id="§-cap"></div><div id="§-neck"></div><div id="§-body"><div id="§-wat"></div></div></div>
      <div id="§-eye">{EYE}</div>
      <div id="§-t1" class="P-mask"><div>a la mano</div></div>
      <div id="§-t2" class="P-mask"><div>no la ves…</div></div>
      <div id="§-t3" class="P-mask"><div>no la tomas</div></div>""",
    FT("#§-bt", {"y": 140, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.0)
    + FT("#§-wat", {"height": "0%"}, {"height": "82%", "duration": 0.8, "ease": "power2.out"}, t(5, "agua"))
    + FT("#§-ring", {"scale": 0.7, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.4, "ease": "back.out(1.6)"}, t(5, "mano"))
    + mask("#§-t1 > div", t(5, "mano", 0, -0.15), 0.35)
    + TO("#§-t1 > div", {"yPercent": -110, "duration": 0.22, "ease": "power2.in"}, t(5, "si", 0, -0.13))
    + TO("#§-ring", {"opacity": 0, "duration": 0.2}, t(5, "si", 0, -0.1))
    + TO("#§-bt", {"opacity": 0.18, "y": 90, "duration": 0.35, "ease": "power2.in"}, t(5, "ves"))
    + FT("#§-eye", {"scale": 0.2, "opacity": 0, "rotation": -15}, {"scale": 1, "opacity": 1, "rotation": 0, "duration": 0.35, "ease": "back.out(2)"}, t(5, "ves"))
    + mask("#§-t2 > div", t(5, "ves", 0, -0.1), 0.3)
    + mask("#§-t3 > div", t(5, "no", 1, -0.15), 0.4)
    + shake(t(5, "tomas", 0, -0.1), 10)
    + push(0, 1.05))

# 6 — Tres, el más fácil: pega la espalda al respaldo antes de arrancar.
POWER = '<svg viewBox="0 0 24 24" width="96" height="96" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M12 3v9"/><path d="M6.4 6.6a8 8 0 1 0 11.2 0"/></svg>'
F["06-tres"] = frame(6, "f06-tres", """
    #§-chip { position: absolute; left: 330px; top: 290px; width: 420px; height: 96px; border-radius: 48px; background: #22C55E; color: #04120A; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 54px; letter-spacing: -.01em; text-transform: lowercase; }
    #§-seat { position: absolute; left: 220px; top: 420px; width: 600px; height: 800px; }
    #§-btn { position: absolute; left: 450px; top: 1290px; width: 180px; height: 180px; border-radius: 50%; border: 8px solid #3A403D; display: flex; align-items: center; justify-content: center; color: #6B736F; background: #0B0D0C; }
""", f"""      <div class="P-kick">hábito 3 / 4</div>
      <div id="§-chip">el más fácil</div>
      <svg id="§-seat" viewBox="0 0 600 800" fill="none" stroke-linecap="round" stroke-linejoin="round">
        <rect x="40" y="160" width="76" height="470" rx="38" fill="#2A2E2C" transform="rotate(-6 78 600)"/>
        <rect x="40" y="600" width="470" height="84" rx="42" fill="#2A2E2C"/>
        <g id="§-bad" stroke="#F43F5E" stroke-width="48"><path d="M170 600 C 250 520, 330 400, 300 250"/><circle cx="318" cy="170" r="52" fill="#F43F5E" stroke="none"/></g>
        <g id="§-ok" stroke="#22C55E" stroke-width="48" opacity="0"><path d="M150 600 L 130 270"/><circle cx="128" cy="180" r="52" fill="#22C55E" stroke="none"/></g>
      </svg>
      <div id="§-btn">{POWER}</div>""",
    FT("#§-chip", {"scale": 0, "rotation": -6}, {"scale": 1, "rotation": 0, "duration": 0.35, "ease": "back.out(2.2)"}, t(6, "fácil", 0, -0.35))
    + FT("#§-seat", {"y": 120, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.4, "ease": "power3.out"}, 0.0)
    + f'      tl.to("#§-bad", {{ x: 10, duration: 0.2, repeat: 7, yoyo: true, ease: "sine.inOut" }}, 0.4);\n'
    + TO("#§-bad", {"opacity": 0, "duration": 0.2}, t(6, "pega", 0, -0.05))
    + FT("#§-ok", {"opacity": 0, "x": 30}, {"opacity": 1, "x": 0, "duration": 0.3, "ease": "back.out(1.8)"}, t(6, "pega", 0, -0.05))
    + TO("#§-btn", {"borderColor": GREEN, "color": GREEN, "scale": 1.12, "duration": 0.3, "ease": "back.out(2)"}, t(6, "arrancar", 0, -0.1))
    + TO("#§-btn", {"boxShadow": "0 0 90px rgba(34,197,94,.7)", "duration": 0.3}, t(6, "arrancar", 0, -0.1))
    + shake(t(6, "pega", 0, -0.05), 8)
    + push(0, 1.04))

# 7 — Cuatro, el que no se negocia:
F["07-cuatro"] = frame(7, "f07-cuatro", """
    #§-glow { position: absolute; left: 140px; top: 300px; width: 800px; height: 800px; border-radius: 50%; background: radial-gradient(circle, rgba(244,63,94,.34), rgba(244,63,94,0) 65%); }
    #§-n { position: absolute; left: 0; right: 0; top: 260px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 760px; line-height: 1; color: #F43F5E; letter-spacing: -.06em; }
    #§-tx { top: 1150px; height: 150px; } #§-tx > div { font-size: 130px; color: #F1F5F2; } #§-tx em { font-style: normal; color: #F43F5E; }
    #§-bar { position: absolute; left: 340px; top: 1110px; width: 400px; height: 8px; background: #F43F5E; transform-origin: 50% 50%; }
""", """      <div id="§-glow"></div>
      <div class="P-kick" style="color:#F43F5E">hábito 4 / 4</div>
      <div id="§-n">4</div>
      <div id="§-bar"></div>
      <div id="§-tx" class="P-mask"><div><em>no</em> se negocia</div></div>""",
    FT("#§-n", {"scale": 3, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.28, "ease": "power4.out"}, 0.0)
    + FT("#§-glow", {"opacity": 0}, {"opacity": 1, "duration": 0.4}, 0.0)
    + FT("#§-bar", {"scaleX": 0}, {"scaleX": 1, "duration": 0.35, "ease": "power3.out"}, t(7, "no"))
    + mask("#§-tx > div", t(7, "no", 0, -0.15), 0.35)
    + shake(0.18, 20)
    + push(0.3, 1.06))

# 8 — Si bostezas o se te cierran los ojos, orilla y descansa.
OCT = "polygon(30% 0,70% 0,100% 30%,100% 70%,70% 100%,30% 100%,0 70%,0 30%)"
F["08-orilla"] = frame(8, "f08-orilla", f"""
    #§-eye {{ position: absolute; left: 190px; top: 190px; width: 700px; height: 460px; }}
    #§-oct {{ position: absolute; left: 330px; top: 700px; width: 420px; height: 420px; clip-path: {OCT}; background: #F43F5E; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 128px; letter-spacing: .02em; color: #fff; opacity: 0; }}
    #§-tx {{ top: 1220px; height: 160px; }} #§-tx > div {{ font-size: 104px; color: #F1F5F2; }} #§-tx em {{ font-style: normal; color: #22C55E; }}
""", """      <svg id="§-eye" viewBox="0 0 600 400">
        <defs><clipPath id="§-clip"><path d="M30 200 Q300 -20 570 200 Q300 420 30 200 Z"/></clipPath></defs>
        <g clip-path="url(#§-clip)">
          <rect x="0" y="0" width="600" height="400" fill="#F1F5F2"/>
          <circle cx="300" cy="200" r="104" fill="#1E3A5F"/><circle cx="300" cy="200" r="48" fill="#06080A"/>
          <rect id="§-lid" x="0" y="-410" width="600" height="410" fill="#3A403D"/>
        </g>
        <path id="§-out" d="M30 200 Q300 -20 570 200 Q300 420 30 200 Z" fill="none" stroke="#F1F5F2" stroke-width="14" stroke-linejoin="round"/>
      </svg>
      <div id="§-oct">ALTO</div>
      <div id="§-tx" class="P-mask"><div>orilla y <em>descansa</em></div></div>""",
    FT("#§-eye", {"scale": 0.7, "opacity": 0}, {"scale": 1, "opacity": 1, "duration": 0.4, "ease": "back.out(1.6)"}, 0.0)
    + f'      tl.fromTo("#§-lid", {{ y: 0 }}, {{ y: 150, duration: 0.25, repeat: 1, yoyo: true, ease: "sine.inOut" }}, {t(8, "bostezas", 0, 0.05)});\n'
    + TO("#§-lid", {"y": 410, "duration": 0.7, "ease": "power2.inOut"}, t(8, "cierran", 0, -0.1))
    + TO("#§-out", {"stroke": RED, "duration": 0.3}, t(8, "ojos", 0, -0.2))
    + TO("#§-eye", {"scale": 0.72, "y": -60, "opacity": 0.45, "duration": 0.3, "ease": "power2.out"}, t(8, "orilla", 0, -0.1))
    + FT("#§-oct", {"scale": 0.2, "opacity": 0, "rotation": -25}, {"scale": 1, "opacity": 1, "rotation": 0, "duration": 0.35, "ease": "back.out(2.2)"}, t(8, "orilla", 0, -0.1))
    + f'      tl.to("#§-oct", {{ opacity: 0.55, duration: 0.14, repeat: 3, yoyo: true, ease: "none" }}, {round(t(8, "orilla", 0, -0.1) + 0.4, 3)});\n'
    + mask("#§-tx > div", t(8, "orilla", 0, -0.05), 0.35)
    + shake(t(8, "orilla", 0, -0.1), 16)
    + push(0, 1.04))

# 9 — Ninguna oferta vale más que llegar bien a casa.
HOUSE = '<svg viewBox="0 0 400 400" width="560" height="560" fill="none" stroke="#F1F5F2" stroke-width="14" stroke-linecap="round" stroke-linejoin="round"><path d="M40 190 L200 50 L360 190"/><path d="M80 170 V350 H320 V170"/><rect x="155" y="250" width="90" height="100" rx="8"/><rect id="§-win" x="105" y="205" width="60" height="46" rx="6" fill="#0B0D0C" stroke="#F1F5F2" stroke-width="8"/><rect id="§-win2" x="260" y="205" width="40" height="46" rx="6" fill="#0B0D0C" stroke="#F1F5F2" stroke-width="8"/></svg>'
F["09-casa"] = frame(9, "f09-casa", """
    #§-glow { position: absolute; left: 190px; top: 340px; width: 700px; height: 700px; border-radius: 50%; background: radial-gradient(circle, rgba(250,204,21,.38), rgba(250,204,21,0) 65%); opacity: 0; }
    #§-cw { position: absolute; inset: 0; } #§-card { left: 230px; top: 330px; }
    #§-house { position: absolute; left: 260px; top: 400px; opacity: 0; }
    #§-l1 { top: 1090px; height: 140px; } #§-l1 > div { font-size: 108px; color: #AFB7B2; }
    #§-l2 { top: 1210px; height: 220px; } #§-l2 > div { font-size: 190px; color: #FACC15; }
""", f"""      <div id="§-glow"></div>
      <div id="§-cw"><div id="§-card" class="P-card">{CARD}</div></div>
      <div id="§-house">{HOUSE}</div>
      <div id="§-l1" class="P-mask"><div>llegar bien</div></div>
      <div id="§-l2" class="P-mask"><div>a casa</div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    FT("#§-cw", {"y": -700, "rotation": 8, "opacity": 0}, {"y": 0, "rotation": -3, "opacity": 1, "duration": 0.55, "ease": "circ.out"}, 0.05)
    + TO("#§-cw", {"x": -330, "y": 120, "scale": 0.7, "rotation": -3, "opacity": 0.35, "duration": 0.5, "ease": "power3.inOut"}, t(9, "vale"))
    + FT("#§-house", {"x": 420, "scale": 0.6, "opacity": 0}, {"x": 0, "scale": 1, "opacity": 1, "duration": 0.55, "ease": "power3.out"}, t(9, "que", 0, 0.0))
    + mask("#§-l1 > div", t(9, "llegar", 0, -0.1), 0.35)
    + mask("#§-l2 > div", t(9, "casa", 0, -0.2), 0.45)
    + TO("#§-glow", {"opacity": 1, "duration": 0.6, "ease": "power2.out"}, t(9, "casa", 0, -0.15))
    + TO("#§-win,#§-win2", {"fill": "#FACC15", "duration": 0.3}, t(9, "casa", 0, -0.1))
    + push(0, 1.05))

# 10 — Mándaselo a quien maneja de más. Sígueme para más.
PLANE = '<svg viewBox="0 0 24 24" width="260" height="260" fill="none" stroke="#22C55E" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M22 2L11 13"/><path d="M22 2l-7 20-4-9-9-4 20-7z"/></svg>'
F["10-sigueme"] = frame(10, "f10-sigueme", """
    #§-pl { position: absolute; left: 410px; top: 220px; width: 260px; height: 260px; }
    #§-l1 { top: 540px; height: 170px; } #§-l1 > div { font-size: 150px; color: #F1F5F2; }
    #§-l2 { top: 700px; height: 130px; } #§-l2 > div { font-size: 100px; color: #AFB7B2; }
    #§-l3 { top: 810px; height: 190px; } #§-l3 > div { font-size: 170px; color: #FACC15; }
    #§-pill { position: absolute; left: 190px; top: 1120px; width: 700px; height: 150px; border-radius: 75px; background: #22C55E; color: #04120A; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 74px; letter-spacing: -.01em; }
""", """      <div id="§-pl">""" + PLANE + """</div>
      <div id="§-l1" class="P-mask"><div>mándaselo</div></div>
      <div id="§-l2" class="P-mask"><div>a quien maneja</div></div>
      <div id="§-l3" class="P-mask"><div>de más</div></div>
      <div id="§-pill">+ Seguir</div>""",
    FT("#§-pl", {"x": -300, "y": 300, "rotation": -40, "opacity": 0}, {"x": 0, "y": 0, "rotation": 0, "opacity": 1, "duration": 0.5, "ease": "power3.out"}, 0.0)
    + mask("#§-l1 > div", 0.05, 0.35)
    + mask("#§-l2 > div", t(10, "quien", 0, -0.15), 0.3)
    + mask("#§-l3 > div", t(10, "de", 0, -0.15), 0.4)
    + FT("#§-pill", {"scale": 0, "rotation": -6}, {"scale": 1, "rotation": 0, "duration": 0.4, "ease": "back.out(2)"}, t(10, "sígueme", 0, -0.2))
    + TO("#§-pill", {"scale": 1.06, "duration": 0.2, "repeat": 1, "yoyo": True}, round(tend(10, "para") + 0.1, 3)))

# 11 — Cierre estándar: Descarga Checa y checa cuánto te queda de cada viaje.
LOGO = """<svg id="§-mark" viewBox="0 0 22 38" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <defs><filter id="§-gb" x="-150%" y="-150%" width="400%" height="400%"><feGaussianBlur stdDeviation="2.4" /></filter></defs>
          <path id="§-body" d="M 7 1 L 15 1 Q 21 1 21 7 L 21 31 Q 21 37 15 37 L 7 37 Q 1 37 1 31 L 1 7 Q 1 1 7 1 Z" fill="#111413" fill-opacity="0" stroke="#AFB7B2" stroke-width="0.9" stroke-linejoin="round" />
          <circle id="§-glow" cx="11" cy="28.5" r="6" fill="#22C55E" filter="url(#§-gb)" opacity="0" />
          <circle id="§-red" cx="11" cy="9.5" r="4.2" fill="#F43F5E" />
          <circle id="§-yellow" cx="11" cy="19" r="4.2" fill="#FACC15" />
          <circle id="§-green" cx="11" cy="28.5" r="4.2" fill="#22C55E" />
        </svg>"""
F["11-cierre"] = frame(11, "f11-cierre", """
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
    #§-black { position: absolute; inset: 0; background: #000; opacity: 0; }
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
    + FT("#§-bloom", {"opacity": 0, "scale": 0.7}, {"opacity": 0.8, "scale": 1, "duration": 0.6, "ease": "power2.out"}, 0)
    + '      tl.fromTo(body, { strokeDashoffset: len }, { strokeDashoffset: 0, duration: 0.3, ease: "power2.out" }, 0);\n'
    + '      tl.to(body, { attr: { "fill-opacity": 1 }, duration: 0.2, ease: "power1.out" }, 0.2);\n'
    + "".join(FT(f"#§-{k}", {"scale": 0, "opacity": 0, "transformOrigin": "50% 50%"}, {"scale": 1, "opacity": 1, "duration": 0.25, "ease": "back.out(2)"}, at)
              for k, at in [("red", 0.12), ("yellow", 0.2), ("green", 0.28)])
    + FT("#§-glow", {"opacity": 0}, {"opacity": 0.7, "duration": 0.3}, 0.4)
    + FT("#§-mark", {"x": 322}, {"x": 0, "duration": 0.45, "ease": "power3.inOut"}, 0.35)
    + FT("#§-word", {"x": -620}, {"x": 0, "duration": 0.45, "ease": "power3.inOut"}, 0.35)
    + FT("#§-pill", {"y": 90, "opacity": 0, "scale": 0.92}, {"y": 0, "opacity": 1, "scale": 1, "duration": 0.4, "ease": "power3.out"}, 0.1)
    + FT("#§-w1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, 0.15)
    + FT("#§-w2", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, t(11, "checa"))
    + TO("#§-w2", {"scale": 1.12, "duration": 0.14, "ease": "power2.out"}, round(t(11, "checa") + 0.2, 3))
    + TO("#§-w2", {"scale": 1, "duration": 0.25, "ease": "power2.inOut"}, round(t(11, "checa") + 0.35, 3))
    + FT("#§-s1", {"y": 30, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.3, "ease": "power3.out"}, tend(11, "checa", 0))
    + FT("#§-rA", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "cuánto"))
    + FT("#§-rB", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "queda"))
    + FT("#§-rC", {"y": 40, "opacity": 0}, {"y": 0, "opacity": 1, "duration": 0.35, "ease": "power3.out"}, t(11, "viaje"))
    + '      tl.fromTo("#§-black", { opacity: 0 }, { opacity: 1, duration: 0.4, ease: "power1.in" }, D - 0.4);\n')

for k, html in F.items():
    old = re.search(r'data-composition-id="([^"]+)"', html).group(1)
    html = html.replace(f'data-composition-id="{old}"', f'data-composition-id="{k}"').replace(f'__timelines["{old}"]', f'__timelines["{k}"]')
    open(f"compositions/frames/{k}.html", "w", encoding="utf-8").write(html)
print("ok", len(F), "escenas", [dur(n) for n in range(1, N + 1)], round(sum(dur(n) for n in range(1, N + 1)), 2))
