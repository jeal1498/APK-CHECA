"""Arma compositions/frames/*.html con los tiempos de audio_meta.json (voz de Fernando).
Uso: python3 -I generar.py  (después de alinear.py). La escena 12 sale de 13-cta de checa-promo."""
import json, re

m = json.load(open("audio_meta.json"))
V = {v["frame"]: v for v in m["voices"]}
PAD, PAD_FIN = 0.15, 1.0
LEAD = 0.25  # las entradas se adelantan a la palabra para que se vean completas cuando la voz la dice


def dur(n):
    return round(V[n]["duration_s"] + (PAD_FIN if n == 12 else PAD), 3)


def t(n, palabra, k=0):
    """Inicio de la k-ésima palabra que empieza con `palabra` en la línea n."""
    ws = [w for w in V[n]["words"] if w["text"].lower().strip("¿¡").startswith(palabra.lower())]
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
    .P-kick { position: absolute; left: 0; right: 0; top: 210px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 500; font-size: 30px; letter-spacing: .14em; text-transform: uppercase; color: #22C55E; }
    .P-kick:before { content: ""; display: block; width: 36px; height: 2px; background: #22C55E; margin: 0 auto 18px; }
    .P-mask { position: absolute; left: 0; right: 0; overflow: hidden; text-align: center; }
    .P-mask > div { font-weight: 800; text-transform: lowercase; letter-spacing: -.04em; line-height: 1.02; white-space: nowrap; }
    .P-ej { position: absolute; left: 0; right: 0; top: 1500px; text-align: center; font-family: 'Fira Code', monospace; font-size: 26px; letter-spacing: .12em; text-transform: uppercase; color: #6B736F; }
    .P-num { font-family: 'Fira Code', monospace; font-weight: 700; font-variant-numeric: tabular-nums; }
    /* oferta genérica (sin logos) */
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


def mask_in(sel, at, dur=0.3):
    return f'      tl.fromTo("{sel}", {{ yPercent: 105 }}, {{ yPercent: 0, duration: {dur}, ease: "power3.out" }}, {at});\n'


def push(cid, at, s1=1.06):
    return f'      tl.fromTo("#{cid}-stage", {{ scale: 1 }}, {{ scale: {s1}, duration: D - {at}, ease: "none" }}, {at});\n'


F = {}

# 1 — La plataforma te dice cuánto te paga…
c = "f01-paga"
F["01-paga"] = frame(1, c, f"""
    #{c}-l1 {{ top: 190px; height: 150px; }} #{c}-l1 > div {{ font-size: 128px; color: #AFB7B2; }}
    #{c}-l2 {{ top: 340px; height: 250px; }} #{c}-l2 > div {{ font-size: 230px; color: #F1F5F2; }}
    #{c}-card {{ left: 230px; top: 720px; }}
    #{c}-hl {{ position: absolute; left: 225px; top: 860px; width: 450px; height: 120px; border-radius: 16px; background: #FACC15; opacity: .9; transform-origin: 0 50%; }}
""", f"""      <div class="P-glow"></div>
      <div id="{c}-l1" class="P-mask"><div>la plataforma</div></div>
      <div id="{c}-l2" class="P-mask"><div>te paga…</div></div>
      <div id="{c}-cw" style="position:absolute;inset:0">
        <div id="{c}-card" class="P-card">{CARD}</div>
      </div>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in(f"#{c}-l1 > div", t(1, "plataforma") - 0.05)
    + f'      tl.fromTo("#{c}-cw", {{ y: 700, rotation: 8, opacity: 0 }}, {{ y: 0, rotation: -3, opacity: 1, duration: 0.6, ease: "circ.out" }}, {t(1, "dice")});\n'
    + mask_in(f"#{c}-l2 > div", t(1, "paga") - 0.08, 0.4)
    + f'      tl.to("#{c}-card .f", {{ color: "#16A34A", scale: 1.08, transformOrigin: "0% 50%", duration: 0.25, ease: "power2.out" }}, {t(1, "paga")});\n'
    + push(c, 0.3))

# 2 — pero no cuánto ganas tú.
c = "f02-ganas"
F["02-ganas"] = frame(2, c, f"""
    #{c}-a {{ top: 420px; height: 190px; }} #{c}-a > div {{ font-size: 160px; color: #8A938E; text-decoration: line-through; text-decoration-thickness: 10px; }}
    #{c}-neq {{ position: absolute; left: 0; right: 0; top: 640px; text-align: center; font-size: 260px; font-weight: 800; color: #F43F5E; line-height: 1; }}
    #{c}-b {{ top: 930px; height: 300px; }} #{c}-b > div {{ font-size: 220px; color: #0A0D0B; }}
    #{c}-q {{ position: absolute; left: 0; right: 0; top: 1260px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 64px; color: #16A34A; }}
""", f"""      <div id="{c}-a" class="P-mask"><div>te pagan</div></div>
      <div id="{c}-neq">≠</div>
      <div id="{c}-b" class="P-mask"><div>ganas tú</div></div>
      <div id="{c}-q">¿cuánto?</div>""",
    mask_in(f"#{c}-a > div", 0.0, 0.3)
    + f'      tl.fromTo("#{c}-neq", {{ scale: 0, rotation: -30 }}, {{ scale: 1, rotation: 0, duration: 0.35, ease: "back.out(2)" }}, {t(2, "no")});\n'
    + f'      tl.fromTo("#{c}-a", {{ y: 0 }}, {{ y: -40, duration: 0.25, ease: "power2.out" }}, {t(2, "no") + 0.1});\n'
    + mask_in(f"#{c}-b > div", t(2, "ganas") - 0.12, 0.35)
    + f'      tl.fromTo("#{c}-q", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.25, ease: "power3.out" }}, {t(2, "cuánto")});\n'
    + push(c, 0, 1.05), ground="#F1F5F2")

# 3 — Te avienta la tarifa, los minutos y los kilómetros a las carreras.
c = "f03-avienta"
F["03-avienta"] = frame(3, c, f"""
    #{c}-phone {{ position: absolute; left: 290px; top: 330px; width: 500px; height: 1000px; border-radius: 64px; background: #0B0D0C; border: 6px solid #2A2E2C; overflow: hidden; }}
    #{c}-map {{ position: absolute; inset: 0; background: repeating-linear-gradient(0deg, #141816 0 2px, transparent 2px 90px), repeating-linear-gradient(90deg, #141816 0 2px, transparent 2px 90px); }}
    #{c}-card {{ left: 20px; top: 380px; transform: scale(.74); transform-origin: 0 0; }}
    .{c}-chip {{ position: absolute; padding: 22px 34px; border-radius: 26px; background: #181C1A; border: 3px solid #2E3532; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 58px; white-space: nowrap; box-shadow: 0 20px 50px rgba(0,0,0,.6); }}
    .{c}-chip span {{ display: block; font-family: 'Fira Sans', sans-serif; font-weight: 500; font-size: 26px; color: #8A938E; letter-spacing: .1em; text-transform: uppercase; }}
    #{c}-c1 {{ left: 60px; top: 420px; color: #FACC15; }}
    #{c}-c2 {{ left: 640px; top: 660px; }}
    #{c}-c3 {{ left: 80px; top: 960px; }}
    #{c}-zz {{ position: absolute; left: 0; right: 0; top: 1360px; text-align: center; font-weight: 800; font-size: 96px; letter-spacing: -.03em; color: #F43F5E; }}
""", f"""      <div class="P-glow"></div>
      <div id="{c}-phone"><div id="{c}-map"></div><div id="{c}-card" class="P-card">{CARD}</div></div>
      <div id="{c}-c1" class="{c}-chip"><span>tarifa</span>$85.00</div>
      <div id="{c}-c2" class="{c}-chip"><span>minutos</span>22 min</div>
      <div id="{c}-c3" class="{c}-chip"><span>kilómetros</span>9 km</div>
      <div id="{c}-zz">¡a las carreras!</div>
      <div class="P-ej">Números de ejemplo</div>""",
    f'      tl.fromTo("#{c}-phone", {{ y: 900 }}, {{ y: 0, duration: 0.5, ease: "power3.out" }}, 0);\n'
    + f'      tl.fromTo("#{c}-card", {{ y: -500, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.35, ease: "power4.out" }}, {t(3, "avienta")});\n'
    + "".join(f'      tl.fromTo("#{c}-c{i}", {{ x: {dx}, scale: .3, opacity: 0 }}, {{ x: 0, scale: 1, opacity: 1, duration: 0.35, ease: "back.out(1.6)" }}, {t(3, w)});\n'
              for i, w, dx in [(1, "tarifa", 250), (2, "minutos", -250), (3, "kilómetros", 250)])
    + f'      tl.fromTo("#{c}-zz", {{ scale: 1.6, opacity: 0 }}, {{ scale: 1, opacity: 1, duration: 0.3, ease: "power4.out" }}, {t(3, "carreras") - 0.2});\n'
    + "".join(f'      tl.to("#{c}-stage", {{ x: {x}, duration: 0.05, ease: "none" }}, {round(t(3, "carreras") - 0.2 + i * 0.05, 3)});\n'
              for i, x in enumerate([14, -12, 9, -6, 3, 0]))
    + f'      tl.fromTo("#{c}-phone, #{c}-c1, #{c}-c2, #{c}-c3", {{ scale: 1 }}, {{ scale: 1.03, duration: D - 0.5, ease: "none" }}, 0.5);\n')

# 4 — Pero no te quita la gasolina,
c = "f04-gas"
F["04-gasolina"] = frame(4, c, f"""
    #{c}-t {{ position: absolute; left: 150px; top: 470px; width: 780px; border-radius: 28px; background: #101312; border: 3px solid #2A2E2C; padding: 56px 60px; }}
    .{c}-row {{ display: flex; justify-content: space-between; align-items: baseline; font-size: 52px; font-weight: 600; }}
    .{c}-row b {{ font-family: 'Fira Code', monospace; font-weight: 700; font-size: 70px; }}
    #{c}-r2 {{ margin-top: 46px; color: #F43F5E; }}
    #{c}-sep {{ height: 3px; background: #2A2E2C; margin: 46px 0 34px; }}
    #{c}-r3 b {{ color: #8A938E; }}
    #{c}-q {{ position: absolute; left: 790px; top: 150px; font-size: 280px; font-weight: 800; color: #F43F5E; line-height: 1; }}
    #{c}-tit {{ top: 1130px; height: 140px; }} #{c}-tit > div {{ font-size: 112px; }}
    #{c}-tit em {{ font-style: normal; color: #F43F5E; }}
""", f"""      <div id="{c}-t">
        <div class="{c}-row"><span>Viaje</span><b>$85.00</b></div>
        <div id="{c}-r2" class="{c}-row"><span>− Gasolina</span><b>$??</b></div>
        <div id="{c}-sep"></div>
        <div id="{c}-r3" class="{c}-row"><span>Te queda</span><b>¿?</b></div>
      </div>
      <div id="{c}-q">?</div>
      <div id="{c}-tit" class="P-mask"><div>sin <em>gasolina</em></div></div>
      <div class="P-ej">Números de ejemplo</div>""",
    f'      tl.fromTo("#{c}-t", {{ y: 80, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.35, ease: "power3.out" }}, 0);\n'
    + f'      tl.fromTo("#{c}-r2", {{ x: -120, opacity: 0 }}, {{ x: 0, opacity: 1, duration: 0.3, ease: "power3.out" }}, {t(4, "gasolina") - 0.15});\n'
    + mask_in(f"#{c}-tit > div", t(4, "gasolina") - 0.1, 0.35)
    + f'      tl.fromTo("#{c}-q", {{ scale: 0, rotation: -20 }}, {{ scale: 1, rotation: 10, duration: 0.4, ease: "back.out(2)" }}, {t(4, "quita")});\n'
    + f'      tl.to("#{c}-r2 b", {{ opacity: 0.2, duration: 0.12, repeat: 5, yoyo: true, ease: "none" }}, {t(4, "gasolina")});\n'
    + push(c, 0, 1.05))

# 5 — no te dice cuánto ganas por hora contando la recogida,
c = "f05-hora"
F["05-hora"] = frame(5, c, f"""
    #{c}-chip {{ position: absolute; left: 190px; top: 380px; width: 700px; height: 340px; border-radius: 40px; background: #181C1A; border: 4px solid #2E3532; display: flex; flex-direction: column; align-items: center; justify-content: center; }}
    #{c}-chip b {{ font-family: 'Fira Code', monospace; font-weight: 700; font-size: 140px; line-height: 1; }}
    #{c}-chip b i {{ font-style: normal; color: #FACC15; }}
    #{c}-chip span {{ font-size: 38px; color: #8A938E; margin-top: 16px; }}
    #{c}-map {{ position: absolute; left: 140px; top: 820px; width: 800px; height: 520px; }}
    #{c}-lbl {{ position: absolute; left: 0; right: 0; top: 1360px; text-align: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 46px; color: #F43F5E; }}
""", f"""      <div class="P-glow"></div>
      <div id="{c}-chip"><b>$<i>?</i>/h</b><span>por hora</span></div>
      <svg id="{c}-map" viewBox="0 0 800 520">
        <path id="{c}-rec" d="M 90 440 C 200 300, 330 420, 420 260" fill="none" stroke="#F43F5E" stroke-width="12" stroke-linecap="round" stroke-dasharray="4 28"/>
        <path id="{c}-via" d="M 420 260 C 520 120, 640 220, 720 80" fill="none" stroke="#22C55E" stroke-width="12" stroke-linecap="round"/>
        <rect x="60" y="410" width="60" height="60" rx="14" fill="#F1F5F2"/>
        <circle id="{c}-pin" cx="420" cy="260" r="26" fill="#F43F5E" stroke="#000" stroke-width="6"/>
        <rect x="694" y="54" width="52" height="52" rx="8" fill="#22C55E"/>
      </svg>
      <div id="{c}-lbl">+ la recogida: 4 min · 1.5 km</div>
      <div class="P-ej">Números de ejemplo</div>""",
    f'      var rec = document.getElementById("{c}-via"); var L = rec.getTotalLength(); rec.style.strokeDasharray = L;\n'
    + f'      tl.fromTo("#{c}-chip", {{ y: -300, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.45, ease: "power3.out" }}, 0);\n'
    + f'      tl.fromTo("#{c}-chip i", {{ scale: 0 }}, {{ scale: 1, duration: 0.3, ease: "back.out(2)", transformOrigin: "50% 60%" }}, {t(5, "hora")});\n'
    + f'      tl.fromTo("#{c}-map", {{ opacity: 0 }}, {{ opacity: 1, duration: 0.3 }}, {t(5, "por")});\n'
    + f'      tl.fromTo(rec, {{ strokeDashoffset: L }}, {{ strokeDashoffset: 0, duration: 0.5, ease: "power2.out" }}, {t(5, "por")});\n'
    + f'      tl.fromTo("#{c}-rec", {{ strokeDashoffset: 300 }}, {{ strokeDashoffset: 0, duration: 1.0, ease: "none" }}, {t(5, "contando")});\n'
    + f'      tl.fromTo("#{c}-pin", {{ scale: 0, transformOrigin: "420px 260px" }}, {{ scale: 1, duration: 0.3, ease: "back.out(2)" }}, {t(5, "recogida")});\n'
    + f'      tl.fromTo("#{c}-lbl", {{ y: 40, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.3, ease: "power3.out" }}, {t(5, "recogida")});\n'
    # cámara: de la ficha a la recogida
    + f'      tl.fromTo("#{c}-stage", {{ scale: 1.08, y: 120 }}, {{ scale: 1.08, y: -160, duration: 0.8, ease: "power2.inOut" }}, {t(5, "contando") - 0.2});\n'
    + f'      tl.set("#{c}-stage", {{ scale: 1.08, y: 120 }}, 0);\n')

# 6 — ni si ese viaje cumple tu meta.
c = "f06-meta"
F["06-meta"] = frame(6, c, f"""
    #{c}-p {{ position: absolute; left: 160px; top: 430px; width: 760px; border-radius: 36px; background: #101312; border: 3px solid #2A2E2C; padding: 50px 56px; }}
    #{c}-p .k {{ font-family: 'Fira Code', monospace; font-size: 30px; letter-spacing: .14em; text-transform: uppercase; color: #22C55E; }}
    .{c}-m {{ display: flex; justify-content: space-between; align-items: baseline; margin-top: 34px; font-size: 48px; font-weight: 600; }}
    .{c}-m b {{ font-family: 'Fira Code', monospace; font-weight: 700; font-size: 76px; }}
    #{c}-pill {{ position: absolute; left: 290px; top: 960px; width: 500px; height: 140px; border-radius: 999px; background: #FACC15; color: #0A0D0B; display: flex; align-items: center; justify-content: center; font-size: 76px; font-weight: 800; letter-spacing: -.02em; }}
""", f"""      <div class="P-glow"></div>
      <div id="{c}-p"><div class="k">Tu meta</div>
        <div class="{c}-m"><span>Por hora</span><b>$150/h</b></div>
        <div class="{c}-m"><span>Por km</span><b>$8/km</b></div>
      </div>
      <div id="{c}-pill">¿cumple?</div>
      <div class="P-ej">Números de ejemplo</div>""",
    f'      tl.fromTo("#{c}-p", {{ y: 120, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.4, ease: "power3.out" }}, 0);\n'
    + f'      tl.fromTo(".{c}-m", {{ x: -60, opacity: 0 }}, {{ x: 0, opacity: 1, duration: 0.3, stagger: 0.12, ease: "power3.out" }}, {t(6, "viaje")});\n'
    + f'      tl.fromTo("#{c}-pill", {{ y: 160, scale: .5, opacity: 0 }}, {{ y: 0, scale: 1, opacity: 1, duration: 0.4, ease: "back.out(1.8)" }}, {t(6, "cumple")});\n'
    + f'      tl.to("#{c}-p", {{ y: -14, duration: 0.12, ease: "power2.out", yoyo: true, repeat: 1 }}, {t(6, "cumple") + 0.25});\n'
    + f'      tl.to("#{c}-pill", {{ rotation: -4, duration: 0.5, ease: "sine.inOut" }}, {t(6, "meta")});\n'
    + push(c, 0, 1.05))

# 7 — ¿Y esa cuenta? La haces de cabeza…
c = "f07-cabeza"
ops = [("85 − 18 = ?", 90, 360, -6), ("85 ÷ 22 × 60 = ?", 300, 560, 4), ("85 ÷ 9 = ?", 520, 760, -3), ("¿150/h?", 130, 960, 7), ("¿8/km?", 600, 1080, -8)]
F["07-cabeza"] = frame(7, c, f"""
    .{c}-op {{ position: absolute; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 64px; color: #F1F5F2; white-space: nowrap; padding: 14px 26px; border-radius: 18px; background: #141816; border: 2px solid #2A2E2C; }}
    #{c}-tit {{ top: 1240px; height: 200px; }} #{c}-tit > div {{ font-size: 170px; color: #FACC15; }}
""", "      <div class=\"P-glow\"></div>\n" + "".join(
        f'      <div id="{c}-o{i}" class="{c}-op" style="left:{x}px;top:{y}px;transform:rotate({r}deg)">{s}</div>\n'
        for i, (s, x, y, r) in enumerate(ops)) + f'      <div id="{c}-tit" class="P-mask"><div>de cabeza…</div></div>',
    "".join(f'      tl.fromTo("#{c}-o{i}", {{ scale: 0, opacity: 0 }}, {{ scale: 1, opacity: 1, duration: 0.3, ease: "back.out(2)" }}, {round(0.1 + i * 0.32, 3)});\n'
            f'      tl.to("#{c}-o{i}", {{ x: {(-1) ** i * 40}, y: {(-1) ** (i + 1) * 30}, duration: D, ease: "sine.inOut" }}, {round(0.1 + i * 0.32, 3)});\n'
            for i in range(len(ops)))
    + mask_in(f"#{c}-tit > div", t(7, "cabeza") - 0.25, 0.4)
    + f'      tl.to(".{c}-op", {{ opacity: 0.35, duration: 0.3 }}, {t(7, "cabeza")});\n'
    + push(c, 0, 1.08))

# 8 — mientras cuidas el tráfico, la moto, el semáforo y que nadie se te cierre.
c = "f08-trafico"
S = 'fill="none" stroke="currentColor" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"'
ICON = {
    "carro": f'<svg viewBox="0 0 200 200"><path {S} d="M30 120 L45 75 Q50 62 64 62 L136 62 Q150 62 155 75 L170 120 Z M24 120 L176 120 L176 150 L24 150 Z"/><circle {S} cx="58" cy="152" r="16"/><circle {S} cx="142" cy="152" r="16"/></svg>',
    "moto": f'<svg viewBox="0 0 200 200"><circle {S} cx="50" cy="135" r="30"/><circle {S} cx="150" cy="135" r="30"/><path {S} d="M50 135 L88 95 L125 95 L150 135 M120 70 L138 70 L150 135 M80 95 L70 78 L100 78"/></svg>',
    "semaforo": f'<svg viewBox="0 0 200 200"><rect {S} x="70" y="20" width="60" height="150" rx="18"/><circle cx="100" cy="52" r="15" fill="#F43F5E"/><circle cx="100" cy="95" r="15" fill="#FACC15"/><circle cx="100" cy="138" r="15" fill="#22C55E"/><path {S} d="M100 170 L100 190"/></svg>',
    "cierre": f'<svg viewBox="0 0 200 200"><path {S} d="M20 150 L180 150 M20 60 L180 60" stroke-dasharray="14 16"/><path {S} d="M40 120 L60 95 L120 95 L140 120 Z M34 120 L146 120 L146 138 L34 138 Z" transform="rotate(-14 90 115)"/><path {S} d="M168 78 L168 108 M168 122 L168 126" stroke="#F43F5E"/></svg>',
}
grid = [("carro", "tráfico", "tráfico", 120, 380), ("moto", "la moto", "moto", 580, 380), ("semaforo", "el semáforo", "semáforo", 120, 860), ("cierre", "que no se te cierren", "nadie", 580, 860)]
F["08-trafico"] = frame(8, c, f"""
    .{c}-ic {{ position: absolute; width: 380px; height: 420px; border-radius: 36px; background: #101312; border: 3px solid #2A2E2C; color: #F1F5F2; display: flex; flex-direction: column; align-items: center; justify-content: center; }}
    .{c}-ic svg {{ width: 230px; height: 230px; }}
    .{c}-ic span {{ font-size: 40px; font-weight: 700; margin-top: 18px; text-align: center; line-height: 1.1; padding: 0 20px; }}
    #{c}-i3 {{ border-color: #F43F5E; }} #{c}-i3 svg {{ color: #F43F5E; }}
""", "".join(f'      <div id="{c}-i{i}" class="{c}-ic" style="left:{x}px;top:{y}px">{ICON[k]}<span>{lab}</span></div>\n'
             for i, (k, lab, w, x, y) in enumerate(grid)),
    "".join(f'      tl.fromTo("#{c}-i{i}", {{ scale: .4, opacity: 0, rotation: {(-1) ** i * 8} }}, {{ scale: 1, opacity: 1, rotation: 0, duration: 0.35, ease: "back.out(1.7)" }}, {round(t(8, w) - 0.1, 3)});\n'
            for i, (k, lab, w, x, y) in enumerate(grid))
    + "".join(f'      tl.to("#{c}-stage", {{ x: {x}, duration: 0.05, ease: "none" }}, {round(t(8, "cierre") + i * 0.05, 3)});\n'
              for i, x in enumerate([18, -16, 12, -8, 4, 0]))
    # la cámara sigue cada ícono
    + "".join(f'      tl.to("#{c}-stage", {{ scale: 1.1, transformOrigin: "{ox}px {oy}px", duration: 0.5, ease: "power2.inOut" }}, {round(t(8, w) - 0.1, 3)});\n'
              for (k, lab, w, x, y) in grid for ox, oy in [(x + 190, y + 210)]))

# 9 — ¡Todo en unos cuantos segundos!
c = "f09-seg"
F["09-segundos"] = frame(9, c, f"""
    #{c}-ring {{ position: absolute; left: 240px; top: 330px; width: 600px; height: 600px; }}
    #{c}-n {{ position: absolute; left: 240px; top: 330px; width: 600px; height: 600px; display: flex; align-items: center; justify-content: center; font-family: 'Fira Code', monospace; font-weight: 700; font-size: 200px; color: #F1F5F2; }}
    #{c}-tit {{ top: 1050px; height: 200px; }} #{c}-tit > div {{ font-size: 150px; }}
    #{c}-tit em {{ font-style: normal; color: #F43F5E; }}
""", f"""      <svg id="{c}-ring" viewBox="0 0 600 600"><circle cx="300" cy="300" r="260" fill="none" stroke="#1C201E" stroke-width="40"/>
        <circle id="{c}-arc" cx="300" cy="300" r="260" fill="none" stroke="#22C55E" stroke-width="40" stroke-linecap="round" transform="rotate(-90 300 300)"/></svg>
      <div id="{c}-n">15 s</div>
      <div id="{c}-tit" class="P-mask"><div>en <em>segundos</em></div></div>""",
    f'      var a = document.getElementById("{c}-arc"); var L = 2 * Math.PI * 260; a.style.strokeDasharray = L;\n'
    f'      var o = {{ s: 15 }}; var n = document.getElementById("{c}-n");\n'
    f'      tl.fromTo(a, {{ strokeDashoffset: 0 }}, {{ strokeDashoffset: L * 0.93, duration: D - 0.1, ease: "power1.in" }}, 0.05);\n'
    f'      tl.to(a, {{ stroke: "#F43F5E", duration: 0.6 }}, 0.6);\n'
    f'      tl.to(o, {{ s: 1, duration: D - 0.1, ease: "power1.in", onUpdate: function () {{ n.textContent = Math.ceil(o.s) + " s"; }} }}, 0.05);\n'
    + mask_in(f"#{c}-tit > div", t(9, "segundos") - 0.2, 0.35)
    + push(c, 0, 1.1))

# 10 — La app Checa hace la cuenta por ti: … verde, amarillo o rojo.
c = "f10-checa"
POP_CSS = open("assets/checa-popup.html").read().split("<style>")[1].split("</style>")[0]
POP_CSS = re.sub(r"(^|\})\s*\.", lambda mm: mm.group(1) + f"\n    #{c}-pw .", POP_CSS)
NIV = [("verde", "ACEPTAR", "$67", "Viaje $85&nbsp;·&nbsp;Gasolina −$18", [("cp-ok", "$232/h", "por hora"), ("cp-ok", "$9.4/km", "por km"), ("", "22 min", "9 km total")]),
       ("amarillo", "CONSIDERAR", "$42", "Viaje $60&nbsp;·&nbsp;Gasolina −$18", [("cp-ok", "$150/h", "por hora"), ("cp-mal", "$6.7/km", "por km"), ("", "24 min", "9 km total")]),
       ("rojo", "RECHAZAR", "$25", "Viaje $45&nbsp;·&nbsp;Gasolina −$20", [("cp-mal", "$96/h", "por hora"), ("cp-mal", "$4.5/km", "por km"), ("", "28 min", "10 km total")])]
X = '<svg width="44" height="44" viewBox="0 0 24 24"><path d="M6 6 L18 18 M18 6 L6 18" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"/></svg>'
pops = ""
for i, (lv, ac, neta, lin, fi) in enumerate(NIV):
    fichas = "".join(f'<div class="cp-ficha {k}"><b>{b}</b><span>{s}</span></div>' for k, b, s in fi)
    pops += (f'        <div id="{c}-p{i}" class="cp cp-{lv}" style="position:absolute;left:0;top:0"><div class="cp-banda"><div class="cp-lado"><span class="cp-app">UBER</span></div>'
             f'<div class="cp-accion">{ac}</div><div class="cp-lado der">{X}</div></div><div class="cp-cuerpo"><div class="cp-neta">{neta}<small>&nbsp;netos</small></div>'
             f'<div class="cp-linea">{lin}</div><div class="cp-fichas">{fichas}</div></div></div>\n')
F["10-checa"] = frame(10, c, POP_CSS + f"""
    #{c}-pw {{ position: absolute; left: 110px; top: 560px; width: 860px; height: 640px; }}
    #{c}-tit {{ top: 260px; height: 150px; }} #{c}-tit > div {{ font-size: 120px; }}
    #{c}-tit em {{ font-style: normal; color: #22C55E; }}
    #{c}-sub {{ position: absolute; left: 0; right: 0; top: 420px; text-align: center; font-size: 48px; font-weight: 600; color: #AFB7B2; }}
""", f"""      <div class="P-glow"></div>
      <div id="{c}-tit" class="P-mask"><div>la app <em>checa</em></div></div>
      <div id="{c}-sub">hace la cuenta por ti</div>
      <div id="{c}-pw">
{pops}      </div>
      <div class="P-ej">Números de ejemplo</div>""",
    mask_in(f"#{c}-tit > div", t(10, "app") - 0.1, 0.4)
    + f'      tl.fromTo("#{c}-sub", {{ opacity: 0, y: 30 }}, {{ opacity: 1, y: 0, duration: 0.3, ease: "power3.out" }}, {t(10, "hace")});\n'
    + f'      tl.set("#{c}-p1, #{c}-p2", {{ opacity: 0 }}, 0);\n'
    + f'      tl.fromTo("#{c}-p0", {{ y: -500, opacity: 0, scale: .9 }}, {{ y: 0, opacity: 1, scale: 1, duration: 0.5, ease: "back.out(1.3)" }}, {t(10, "cuenta")});\n'
    + f'      tl.fromTo("#{c}-p0 .cp-neta", {{ scale: 1 }}, {{ scale: 1.12, duration: 0.15, yoyo: true, repeat: 1, ease: "power2.out" }}, {t(10, "limpio")});\n'
    + f'      tl.set("#{c}-p0", {{ opacity: 0 }}, {t(10, "amarillo")}); tl.set("#{c}-p1", {{ opacity: 1 }}, {t(10, "amarillo")});\n'
    + f'      tl.set("#{c}-p1", {{ opacity: 0 }}, {t(10, "rojo")}); tl.set("#{c}-p2", {{ opacity: 1 }}, {t(10, "rojo")});\n'
    + "".join(f'      tl.fromTo("#{c}-pw", {{ scale: 1.06 }}, {{ scale: 1, duration: 0.3, ease: "power3.out" }}, {t(10, w)});\n' for w in ["verde", "amarillo", "rojo"])
    + push(c, 0, 1.04))

# 11 — Tú solo ocúpate de manejar y de darle a tu pasaje el mejor servicio.
c = "f11-serv"
STAR = '<svg viewBox="0 0 24 24"><path d="M12 2l2.9 6.6 7.1.6-5.4 4.7 1.6 7-6.2-3.7-6.2 3.7 1.6-7L2 9.2l7.1-.6z" fill="currentColor"/></svg>'
F["11-servicio"] = frame(11, c, f"""
    #{c}-l1 {{ top: 330px; height: 190px; }} #{c}-l1 > div {{ font-size: 170px; color: #04120A; }}
    #{c}-l2 {{ top: 540px; height: 150px; }} #{c}-l2 > div {{ font-size: 120px; color: #04120A; opacity: .75; }}
    #{c}-l3 {{ top: 760px; height: 150px; }} #{c}-l3 > div {{ font-size: 112px; color: #04120A; }}
    #{c}-l4 {{ top: 910px; height: 150px; }} #{c}-l4 > div {{ font-size: 112px; color: #F1F5F2; }}
    #{c}-stars {{ position: absolute; left: 0; right: 0; top: 1150px; display: flex; justify-content: center; gap: 22px; }}
    #{c}-stars svg {{ width: 140px; height: 140px; color: #04120A; opacity: .25; }}
""", f"""      <div id="{c}-l1" class="P-mask"><div>tú solo</div></div>
      <div id="{c}-l2" class="P-mask"><div>maneja</div></div>
      <div id="{c}-l3" class="P-mask"><div>y dale a tu pasaje</div></div>
      <div id="{c}-l4" class="P-mask"><div>el mejor servicio</div></div>
      <div id="{c}-stars">{STAR * 5}</div>""",
    mask_in(f"#{c}-l1 > div", 0.05)
    + mask_in(f"#{c}-l2 > div", t(11, "manejar") - 0.1)
    + mask_in(f"#{c}-l3 > div", t(11, "darle") - 0.1)
    + mask_in(f"#{c}-l4 > div", t(11, "mejor") - 0.15)
    + f'      tl.to("#{c}-stars svg", {{ opacity: 1, color: "#FACC15", scale: 1.15, duration: 0.2, stagger: 0.1, ease: "back.out(2)" }}, {t(11, "mejor")});\n'
    + f'      tl.to("#{c}-stars svg", {{ scale: 1, duration: 0.3, stagger: 0.1, ease: "power2.out" }}, {t(11, "mejor") + 0.2});\n'
    + push(c, 0, 1.05), ground="#22C55E")

# 12 — CTA (de 13-cta de checa-promo)
cta = open("../checa-promo/compositions/frames/13-cta.html").read()
d12 = dur(12)
cta = cta.replace('"13-cta"', '"f12-cta"').replace('data-duration="2.821"', f'data-duration="{d12}"').replace("const D = 2.821;", f"const D = {d12};")
cta = cta.replace("frame-13-cta", "f12cta")
cta = cta.replace('<span id="f12cta-w3">sin costo</span>', '<span id="f12cta-w3">sin costo</span>\n      </div>\n      <div id="f12cta-com">comenta <b>CHECA</b><small>solo Android</small>')
cta = cta.replace("</style>", """  #f12cta-com { position: absolute; left: 0; right: 0; top: 1210px; text-align: center; font-size: 72px; font-weight: 700; color: #F1F5F2; }
    #f12cta-com b { font-family: 'Fira Code', monospace; color: #22C55E; }
    #f12cta-com small { display: block; margin-top: 20px; font-family: 'Fira Code', monospace; font-weight: 500; font-size: 30px; letter-spacing: .14em; text-transform: uppercase; color: #8A938E; }
  </style>""", 1)
cta = cta.replace('}, 2.0);\n', '}, 2.0);\n' + f'      tl.fromTo("#f12cta-com", {{ y: 60, opacity: 0 }}, {{ y: 0, opacity: 1, duration: 0.4, ease: "power3.out" }}, {t(12, "comenta")});\n', 1)
for a, b in [("1.18", t(12, "pruébala")), ("1.5)", f'{t(12, "7")})'), ("1.6)", f'{t(12, "7") + 0.1})'), ("1.75)", f'{t(12, "7") + 0.25})'), ("1.1);", f'{t(12, "pruébala") - 0.08});')]:
    cta = cta.replace(f"}}, {a}", f"}}, {b}", 1)
cta = cta.replace("}, 2.0);", f'}}, {t(12, "sin")});', 1)
F["12-cta"] = cta

for k, html in F.items():
    old = re.search(r'data-composition-id="([^"]+)"', html).group(1)
    html = html.replace(f'data-composition-id="{old}"', f'data-composition-id="{k}"').replace(f'__timelines["{old}"]', f'__timelines["{k}"]')
    open(f"compositions/frames/{k}.html", "w").write(html)
print("ok", len(F), "escenas", [dur(n) for n in range(1, 13)], round(sum(dur(n) for n in range(1, 13)), 2))
