"""Genera index.html del video «Cómo funciona Checa» (HyperFrames, 1080x1920).

Los tiempos salen de la voz (assets/voz/NN.json, alineación por letra de ElevenLabs).
Uso: python3 -I generar.py
"""
import json, os

AQUI = os.path.dirname(os.path.abspath(__file__))
W, H = 1080, 1920
PAUSA = 0.45          # aire entre líneas de voz
INICIO = 0.25         # silencio al arrancar
COLA = 1.6            # el logo se queda al final

C = dict(fondo="#000000", tarjeta="#181C1A", tarjeta2="#222826", borde="#2E3532", texto="#F2F5F3",
         texto2="#B4BCB7", tenue="#7D8781", verde="#22C55E", amarillo="#F5B70A", rojo="#EF4444",
         oro="#E8B84A", rosa="#F43F5E", cian="#22D3EE", morado="#A78BFA")
ACCION = dict(verde="ACEPTAR", amarillo="CONSIDERAR", rojo="RECHAZAR")


# ---------- tiempos ----------
def palabras(n):
    d = json.load(open(os.path.join(AQUI, f"assets/voz/{n:02d}.json")))
    out, act, ini, fin = [], "", None, 0
    for ch, s, e in zip(d["c"], d["s"], d["e"]):
        if ch.isspace():
            if act:
                out.append((act, ini, fin))
            act, ini = "", None
            continue
        if ini is None:
            ini = s
        act += ch
        fin = e
    if act:
        out.append((act, ini, fin))
    return out, d["e"][-1]


ESC = []
t = INICIO
for n in range(1, 14):
    pal, dur = palabras(n)
    d = dur + PAUSA + (COLA if n == 13 else 0)
    ESC.append(dict(n=n, s=round(t, 3), d=round(d, 3), voz=round(dur, 3), pal=[(p, round(t + a, 3), round(t + b, 3)) for p, a, b in pal]))
    t += dur + PAUSA
TOTAL = round(ESC[-1]["s"] + ESC[-1]["d"], 3)


def cuando(esc, palabra, k=0):
    """Segundo absoluto en que la voz dice `palabra` (k-ésima coincidencia)."""
    hits = [a for p, a, b in esc["pal"] if palabra.lower() in p.lower()]
    return hits[k] if len(hits) > k else esc["s"]


# ---------- piezas ----------
def popup(pid, nivel="verde", neta="118", viaje="150", gas="32", fichas=None, oro=False, apagada=False, ancho=860, app="UBER"):
    u = ancho / 860
    col = C[nivel]
    txt = "#FFFFFF" if nivel == "rojo" else "#08130C"
    fichas = fichas or [("$321/h", "por hora", "ok"), ("$9.4/km", "por km", "ok"), ("28 min", "16 km total", "")]
    borde = f"{10*u:.0f}px solid {C['oro']}" if oro else f"{3*u:.0f}px solid {C['borde']}"
    estrella = (f'<svg width="{36*u:.0f}" height="{36*u:.0f}" viewBox="0 0 24 24"><path d="M12 2l2.9 6.6 7.1.6-5.4 4.7 1.6 7-6.2-3.7-6.2 3.7 1.6-7L2 9.2l7.1-.6z" fill="{txt}"/></svg>' if oro else "")
    fs = []
    for i, (v, l, c) in enumerate(fichas):
        cv = C["verde"] if c == "ok" else C["rojo"] if c == "mal" else C["texto"]
        bg = "#381818" if c == "mal" else C["tarjeta"]
        bd = C["rojo"] if c == "mal" else "transparent"
        fs.append(f'<div id="{pid}-f{i}" class="ficha" style="height:{156*u:.0f}px;border-radius:{28*u:.0f}px;background:{bg};border:{3*u:.0f}px solid {bd}">'
                  f'<div class="num" style="font-size:{48*u:.0f}px;color:{cv}">{v}</div>'
                  f'<div style="font-size:{28*u:.0f}px;color:{C["tenue"]};margin-top:{4*u:.0f}px">{l}</div></div>')
    return (f'<div id="{pid}" class="popup" style="width:{ancho}px;border-radius:{40*u:.0f}px;border:{borde}">'
            f'<div class="banda" style="height:{100*u:.0f}px;background:{col};opacity:{0.5 if apagada else 1};padding:0 {34*u:.0f}px;color:{txt}">'
            f'<div style="width:{190*u:.0f}px;display:flex;align-items:center;gap:{8*u:.0f}px">{estrella}<span class="pastilla" style="font-size:{26*u:.0f}px;padding:{6*u:.0f}px {16*u:.0f}px">{app}</span></div>'
            f'<div style="font-size:{50*u:.0f}px;font-weight:700;letter-spacing:{5*u:.0f}px">{ACCION[nivel]}</div>'
            f'<div style="width:{190*u:.0f}px;display:flex;justify-content:flex-end"><svg width="{44*u:.0f}" height="{44*u:.0f}" viewBox="0 0 24 24"><path d="M6 6 L18 18 M18 6 L6 18" stroke="{txt}" stroke-width="2.4" stroke-linecap="round"/></svg></div></div>'
            f'<div style="padding:{34*u:.0f}px {30*u:.0f}px {30*u:.0f}px;text-align:center">'
            f'<div class="num" style="font-size:{124*u:.0f}px;line-height:1">${neta}<span style="font-size:{50*u:.0f}px;color:{C["texto2"]}">&nbsp;netos</span></div>'
            f'<div style="font-size:{38*u:.0f}px;color:{C["texto2"]};margin-top:{22*u:.0f}px">Viaje ${viaje}&nbsp;·&nbsp;Gasolina −${gas}</div>'
            f'<div class="fichas" style="gap:{20*u:.0f}px;margin-top:{40*u:.0f}px">{"".join(fs)}</div></div></div>')


def oferta(oid, ancho=760):
    """Oferta genérica de una app de viajes (no imita ninguna marca)."""
    u = ancho / 760
    html = (f'<div id="{oid}" class="oferta" style="width:{ancho}px;border-radius:{36*u:.0f}px;padding:{40*u:.0f}px {44*u:.0f}px">'
            f'<div style="font-size:{30*u:.0f}px;color:{C["tenue"]};letter-spacing:{3*u:.0f}px">NUEVA SOLICITUD</div>'
            f'<div class="num" style="font-size:{120*u:.0f}px;margin:{14*u:.0f}px 0 {22*u:.0f}px">$150.00</div>'
            f'<div class="tramo" style="font-size:{36*u:.0f}px"><i style="background:{C["texto"]}"></i>A 6 min (3 km)</div>'
            f'<div class="tramo" style="font-size:{36*u:.0f}px;margin-top:{14*u:.0f}px"><i style="background:{C["tenue"]}"></i>Viaje: 22 min (13 km)</div>'
            f'<div class="boton" style="margin-top:{34*u:.0f}px;height:{96*u:.0f}px;border-radius:{24*u:.0f}px;font-size:{36*u:.0f}px">Aceptar</div></div>')
    if oid == "o4":  # en la escena 4 el pop-up tapa la oferta a propósito
        html = html.replace("<div ", "<div data-layout-allow-overlap data-layout-allow-occlusion ")
    return html


def etiqueta(eid, texto, color):
    return f'<div id="{eid}" class="etq" style="color:{color};border-color:{color}">{texto}</div>'


def ejemplo(eid):
    return f'<div id="{eid}" class="ejemplo">Números de ejemplo</div>'


# ---------- escenas ----------
HTML, JS = [], []


def anim(linea):
    JS.append(linea)


def escena(e, tono, cuerpo, kicker, kcolor):
    n, s, d = e["n"], e["s"], e["d"]
    HTML.append(f'<section id="e{n}" class="clip escena" data-start="{s}" data-duration="{d}" data-track-index="1">'
                f'<div class="tinte" style="background:radial-gradient(circle at 50% 42%, {tono}, transparent 62%)"></div>'
                f'<div id="e{n}-cam" class="cam">{cuerpo}</div>' +
                (f'<div id="e{n}-k" class="kicker" style="color:{kcolor}"><b style="background:{kcolor}"></b>{kicker}</div>' if kicker else '<div id="e%d-k"></div>' % n) + '</section>')
    # entrada suave + empuje de cámara lento (cambio de encuadre a mitad si la escena es larga)
    anim(f'tl.fromTo("#e{n}-cam",{{opacity:0,scale:0.96}},{{opacity:1,scale:1,duration:0.35,ease:"power2.out"}},{s});')
    if d > 3.6:
        anim(f'tl.to("#e{n}-cam",{{scale:1.05,duration:0.5,ease:"power2.inOut"}},{s + d / 2:.3f});')
    anim(f'tl.fromTo("#e{n}-k",{{opacity:0,x:-30}},{{opacity:1,x:0,duration:0.4,ease:"power3.out"}},{s + 0.1:.3f});')


def pop(sel, t0, desde=0.6, dur=0.5, ease="back.out(1.7)"):
    anim(f'tl.fromTo("{sel}",{{opacity:0,scale:{desde}}},{{opacity:1,scale:1,duration:{dur},ease:"{ease}"}},{t0:.3f});')


def sube(sel, t0, y=60, dur=0.5):
    anim(f'tl.fromTo("{sel}",{{opacity:0,y:{y}}},{{opacity:1,y:0,duration:{dur},ease:"power3.out"}},{t0:.3f});')


E = ESC
# 1 — ¿Te conviene?
e = E[0]
escena(e, "rgba(244,63,94,0.20)", f'<div class="centro">{oferta("o1")}<div id="q1" class="interroga">?</div></div>', "LA DUDA", C["rosa"])
sube("#o1", e["s"] + 0.15)
pop("#q1", cuando(e, "conviene") - 0.2, 0.2, 0.6, "elastic.out(1,0.5)")
anim(f'tl.to("#q1",{{rotation:12,duration:0.25,yoyo:true,repeat:3,ease:"sine.inOut"}},{cuando(e, "conviene") + 0.4:.3f});')

# 2 — $150 → ¿?
e = E[1]
escena(e, "rgba(250,204,21,0.16)", '<div class="centro col">'
       '<div id="m2" class="gigante">$150</div>'
       '<svg id="fl2" width="160" height="200" viewBox="0 0 160 200"><path id="fl2p" d="M80 10 V170 M30 120 L80 175 L130 120" stroke="#FACC15" stroke-width="14" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>'
       '<div id="q2" class="gigante" style="color:#FACC15">¿$?</div>' + ejemplo("x2") + '</div>', "LA OFERTA", "#FACC15")
pop("#m2", cuando(e, "150") - 0.1, 0.5)
anim(f'tl.fromTo("#fl2p",{{strokeDasharray:400,strokeDashoffset:400}},{{strokeDashoffset:0,duration:0.6,ease:"power2.out"}},{cuando(e, "pero"):.3f});')
pop("#q2", cuando(e, "cuánto"), 0.3, 0.6, "elastic.out(1,0.5)")
sube("#x2", e["s"] + 0.4, 20)

# 3 — Checa lee la oferta (teléfono + escáner)
e = E[2]
escena(e, "rgba(34,211,238,0.18)", '<div class="centro"><div id="tel3" class="tel">' + oferta("o3", 600) +
       '<div id="scan3" class="scan"></div></div></div>', "PASO 1 · LEE", C["cian"])
sube("#tel3", e["s"] + 0.1, 120, 0.6)
anim(f'tl.fromTo("#scan3",{{y:-40,opacity:0}},{{y:900,opacity:1,duration:1.4,ease:"sine.inOut"}},{cuando(e, "lee"):.3f});')
anim(f'tl.to("#scan3",{{y:-40,duration:1.0,ease:"sine.inOut"}},{cuando(e, "lee") + 1.4:.3f});')

# 4 — aviso flotante
e = E[3]
escena(e, "rgba(34,197,94,0.20)", '<div class="centro"><div id="tel4" class="tel" data-layout-allow-overlap data-layout-allow-occlusion>' + oferta("o4", 600) +
       '</div><div id="pp4" class="flota" data-layout-allow-overlap data-layout-allow-occlusion>' + popup("p4", ancho=880) + '</div></div>', "PASO 2 · AVISA", C["verde"])
anim(f'tl.fromTo("#tel4",{{opacity:1,scale:1}},{{opacity:0.35,scale:0.94,duration:0.5}},{cuando(e, "inmediato"):.3f});')
anim(f'tl.fromTo("#pp4",{{opacity:0,y:-260,scale:0.8}},{{opacity:1,y:0,scale:1,duration:0.7,ease:"back.out(1.4)"}},{cuando(e, "inmediato"):.3f});')

# 5 — $/h y $/km
e = E[4]
escena(e, "rgba(34,197,94,0.18)", '<div class="centro col">'
       '<div id="c5a" class="chip"><div class="num" style="color:#22C55E">$321<small>/h</small></div><div class="chipl">por hora</div></div>'
       '<div id="c5b" class="chip"><div class="num" style="color:#22C55E">$9.4<small>/km</small></div><div class="chipl">por kilómetro</div></div>'
       + ejemplo("x5") + '</div>', "LO QUE GANAS", C["verde"])
pop("#c5a", cuando(e, "hora"), 0.5)
pop("#c5b", cuando(e, "kilómetro"), 0.5)
sube("#x5", e["s"] + 0.3, 20)

# 6 — ticket neta
e = E[5]
escena(e, "rgba(34,197,94,0.16)", '<div class="centro col"><div id="t6" class="ticket">'
       '<div class="fila"><span>Viaje</span><span class="num">$150</span></div>'
       '<div id="g6" class="fila" style="color:#EF4444"><span>Gasolina</span><span class="num">−$32</span></div>'
       '<div id="r6" class="raya"></div>'
       '<div id="n6" class="fila grande"><span>Te queda</span><span class="num" style="color:#22C55E">$<span id="n6v">118</span></span></div></div>'
       + ejemplo("x6") + '</div>', "LO QUE TE QUEDA", C["verde"])
sube("#t6", e["s"] + 0.1, 80)
sube("#g6", cuando(e, "gasolina") - 0.2, 20)
anim(f'tl.fromTo("#r6",{{scaleX:0}},{{scaleX:1,duration:0.4,ease:"power2.out"}},{cuando(e, "neta"):.3f});')
sube("#n6", cuando(e, "neta"), 20)
anim(f'tl.fromTo("#n6v",{{textContent:150}},{{textContent:118,duration:0.9,snap:{{textContent:1}},ease:"power2.out"}},{cuando(e, "neta"):.3f});')
sube("#x6", e["s"] + 0.3, 20)

# 7 — semáforo
e = E[6]
filas = "".join(f'<div id="s7{k}" class="luz"><i style="background:{C[k]};box-shadow:0 0 60px {C[k]}"></i><span style="color:{C[k]}">{ACCION[k]}</span></div>'
                for k in ("verde", "amarillo", "rojo"))
escena(e, "rgba(245,183,10,0.14)", f'<div class="centro col"><div class="semaforo">{filas}</div></div>', "EL SEMÁFORO", C["amarillo"])
for k, w in (("verde", "verde"), ("amarillo", "amarillo"), ("rojo", "rojo")):
    anim(f'tl.fromTo("#s7{k}",{{opacity:0,y:30}},{{opacity:0.3,y:0,duration:0.4}},{e["s"] + 0.2:.3f});')
    anim(f'tl.to("#s7{k}",{{opacity:1,scale:1.08,duration:0.3,ease:"back.out(2)"}},{cuando(e, w) - 0.05:.3f});')
    anim(f'tl.to("#s7{k}",{{scale:1,opacity:0.45,duration:0.3}},{cuando(e, w) + 0.95:.3f});')

# 8 — tus metas
e = E[7]
def meta(mid, titulo, valor):
    return (f'<div id="{mid}" class="meta"><div><div class="metat">{titulo}</div><div class="num metav">${valor}</div></div>'
            f'<div class="mas"><span>−</span><span style="background:#22C55E;color:#08130C">+</span></div></div>')
escena(e, "rgba(167,139,250,0.18)", '<div class="centro col">' + meta("m8a", "Meta por km", '<span id="m8av">6</span>') +
       meta("m8b", "Meta por hora", '<span id="m8bv">120</span>') + '</div>', "TUS METAS", C["morado"])
sube("#m8a", e["s"] + 0.1)
sube("#m8b", e["s"] + 0.3)
anim(f'tl.fromTo("#m8av",{{textContent:6}},{{textContent:8,duration:0.8,snap:{{textContent:1}}}},{cuando(e, "metas"):.3f});')
anim(f'tl.fromTo("#m8bv",{{textContent:120}},{{textContent:150,duration:0.9,snap:{{textContent:10}}}},{cuando(e, "metas") + 0.3:.3f});')

# 9 — recogida lejos
e = E[8]
escena(e, "rgba(239,68,68,0.20)", '<div class="centro">' + popup("p9", "rojo", "71", "120", "49",
       [("$212/h", "por hora", "ok"), ("$4.9/km", "por km", "mal"), ("6 km", "recogida", "mal")], ancho=900) + '</div>', "RECOGIDA LEJOS", C["rojo"])
pop("#p9", e["s"] + 0.1, 0.85)
anim(f'tl.fromTo("#p9-f2",{{scale:1}},{{scale:1.18,duration:0.3,yoyo:true,repeat:3,ease:"sine.inOut"}},{cuando(e, "lejos"):.3f});')

# 10 — dos ofertas, la mejor
e = E[9]
escena(e, "rgba(232,184,74,0.18)", '<div class="centro col dos">' +
       f'<div id="d10a">{popup("p10a", "verde", "118", "150", "32", oro=True, ancho=740)}</div>' +
       f'<div id="d10b">{popup("p10b", "amarillo", "84", "112", "28", [("$268/h", "por hora", "ok"), ("$7.1/km", "por km", "mal"), ("25 min", "14 km total", "")], apagada=True, ancho=740, app="DIDI")}</div>'
       + '</div>', "LA MEJOR", C["oro"])
sube("#d10a", e["s"] + 0.1, -80)
sube("#d10b", e["s"] + 0.25, 80)
anim(f'tl.to("#d10a",{{scale:1.06,duration:0.4,ease:"back.out(2)"}},{cuando(e, "mejor"):.3f});')
anim(f'tl.to("#d10b",{{opacity:0.45,scale:0.94,duration:0.4}},{cuando(e, "mejor"):.3f});')

# 11 — mejores horas
e = E[10]
HORAS = [("6", "r"), ("7", "a"), ("8", "v"), ("9", "v"), ("10", "a"), ("11", "r"), ("12", "a"), ("13", "v"),
         ("14", "a"), ("15", "r"), ("16", "a"), ("17", "v"), ("18", "v"), ("19", "v"), ("20", "a"), ("21", "r")]
col = dict(v=C["verde"], a=C["amarillo"], r=C["rojo"])
cuadros = "".join(f'<div id="h11-{i}" class="hora" style="background:{col[c]}{"" if c == "v" else "CC"};color:{"#08130C" if c != "r" else "#fff"}">{h}</div>' for i, (h, c) in enumerate(HORAS))
escena(e, "rgba(34,197,94,0.14)", f'<div class="centro col"><div class="horas">{cuadros}</div><div id="h11t" class="pie">Lo mejor que viene: <b>17–19 h</b></div></div>', "MEJORES HORAS", C["verde"])
for i in range(len(HORAS)):
    pop(f"#h11-{i}", e["s"] + 0.1 + i * 0.05, 0.4, 0.35)
for i in (11, 12, 13):
    anim(f'tl.to("#h11-{i}",{{scale:1.15,duration:0.3,yoyo:true,repeat:1}},{cuando(e, "mejor"):.3f});')
sube("#h11t", cuando(e, "pagan"), 20)

# 12 — más dinero
e = E[11]
BARRAS = [180, 260, 340, 460, 600]
barras = "".join(f'<div class="barra"><div id="b12-{i}" style="height:{h}px;background:{C["verde"] if i == 4 else "#1F6B3D"}"></div></div>' for i, h in enumerate(BARRAS))
escena(e, "rgba(34,197,94,0.22)", f'<div class="centro col"><div class="barras">{barras}</div><div id="t12" class="lema"><div>MÁS DINERO</div><span>en tu cuenta</span></div></div>', "EL RESULTADO", C["verde"])
for i in range(len(BARRAS)):
    anim(f'tl.fromTo("#b12-{i}",{{scaleY:0}},{{scaleY:1,duration:0.6,ease:"power3.out"}},{e["s"] + 0.15 + i * 0.12:.3f});')
pop("#t12", cuando(e, "más") - 0.1, 0.6)

# 13 — cierre
e = E[12]
escena(e, "rgba(34,197,94,0.26)", '<div class="centro col">'
       '<div id="l13" class="logo"><div class="semaf"><i style="background:#4A2424"></i><i style="background:#4A3E1A"></i><i id="l13v" style="background:#22C55E;box-shadow:0 0 50px #22C55E"></i></div><span>Checa</span></div>'
       '<div id="g13" class="gratis">Pruébala <b>7 días</b> sin costo</div></div>', "", C["verde"])
pop("#l13", e["s"] + 0.1, 0.5, 0.7)
sube("#g13", cuando(e, "pruébala"), 40)

# ---------- subtítulos (1–3 palabras) ----------
CLAVE = {"conviene", "150", "limpio", "checa", "flotante", "hora", "kilómetro", "neta", "gasolina", "verde", "amarillo", "rojo",
         "metas", "lejos", "mejor", "pagan", "dinero", "7", "días", "costo", "semáforo"}
SUBS = []
for e in ESC:
    pal = e["pal"]
    grupos, g = [], []
    for p in pal:
        g.append(p)
        if len(g) == 3 or p[0][-1] in ",.?;:…" or (len(g) == 2 and len(p[0]) > 7):
            grupos.append(g); g = []
    if g:
        grupos.append(g)
    for gi, g in enumerate(grupos):
        ini = g[0][1]
        fin = grupos[gi + 1][0][1] if gi + 1 < len(grupos) else e["s"] + e["voz"] + 0.3
        txt = " ".join((f'<em>{w}</em>' if w.strip("¿?¡!.,;:…").lower() in CLAVE else w) for w, a, b in g)
        txt = txt.replace("...", "…")
        sid = f"sub{len(SUBS)}"
        SUBS.append(f'<div id="{sid}" class="clip sub" data-start="{ini:.3f}" data-duration="{max(0.2, fin - ini):.3f}" data-track-index="2"><span id="{sid}-t">{txt}</span></div>')
        anim(f'tl.fromTo("#{sid}-t",{{scale:0.7,opacity:0}},{{scale:1,opacity:1,duration:0.18,ease:"back.out(2.5)"}},{ini:.3f});')

# ---------- audio ----------
AUD = [f'<audio id="voz{e["n"]:02d}" src="assets/voz/{e["n"]:02d}.mp3" data-start="{e["s"]}" data-duration="{e["voz"] + 0.05:.3f}" data-track-index="3" data-volume="1"></audio>' for e in ESC]
AUD.append(f'<audio id="bgm" src="assets/audio/musica-calma.mp3" data-start="0" data-duration="{TOTAL}" data-track-index="4" data-volume="0.13"></audio>')
for k, sel in ((4, "inmediato"), (10, "mejor")):
    e = ESC[k - 1]
    AUD.append(f'<audio id="sfx{k}" src="assets/audio/pop.mp3" data-start="{cuando(e, sel):.3f}" data-duration="0.6" data-track-index="5" data-volume="0.5"></audio>')
AUD.append(f'<audio id="sfx13" src="assets/audio/ding.wav" data-start="{ESC[12]["s"] + 0.15:.3f}" data-duration="1.2" data-track-index="5" data-volume="0.45"></audio>')

CSS = open(os.path.join(AQUI, "estilo.css")).read()
doc = f'''<!doctype html>
<html lang="es">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width={W}, height={H}" />
    <title>Cómo funciona Checa</title>
    <script src="https://cdn.jsdelivr.net/npm/gsap@3.14.2/dist/gsap.min.js"></script>
    <style>
{CSS}
    </style>
  </head>
  <body>
    <div id="root" data-composition-id="main" data-start="0" data-duration="{TOTAL}" data-width="{W}" data-height="{H}">
      <div id="fondo" class="clip" data-start="0" data-duration="{TOTAL}" data-track-index="0"><div class="puntos"></div></div>
      {"".join(HTML)}
      {"".join(SUBS)}
      {"".join(AUD)}
    </div>
    <script>
      const tl = gsap.timeline({{ paused: true }});
      {chr(10).join("      " + j for j in JS).strip()}
      window.__timelines["main"] = tl;
    </script>
  </body>
</html>
'''
open(os.path.join(AQUI, "index.html"), "w").write(doc)
print("duración", TOTAL, "s ·", len(SUBS), "subtítulos")
