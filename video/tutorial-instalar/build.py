# Tutorial con capturas: cada línea de voz muestra una o más capturas fijas del video original,
# con zoom suave al botón, señalamiento que aparece con su palabra y un "toque" al decir la acción.
import json, subprocess, sys, math, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
F4 = "../f4"; W, H = 1080, 1920; PX, PY, PW, PH = 185, 170, 710, 1588; FPS = 30
G = (34, 197, 94); BG = (14, 17, 16); PAD = 0.6; TAIL = 2.0; DL = 0.15; XF = 0.18
T = 14
def B(n, t): return ("PASO %d DE %d" % (n, T), t)
def P(t, fx=.5, fy=.5, z=1.0, sw=None, blur=()): return dict(t=t, fx=fx, fy=fy, z=min(z, 1.45), sw=sw, blur=blur)
def C(p, x, y, w, h, lab="", show=None, tap=None, k='r'): return dict(p=p, x=x, y=y, w=w, h=h, lab=lab, show=show, tap=tap, k=k)
BL_MENU = [(.55, .165, .75, .06)]
BL_CUENTAS = [(.58, .66, .75, .30)]
BL_LISTO = [(.5, .345, .9, .03)]
BL_PERFIL = [(.5, .165, .8, .075)]
# (banner1, banner2, planos, llamadas) — una entrada por línea de voz (sin la del cierre)
S = [
("ANTES DE EMPEZAR", "Ve a tu ritmo", [P(3.0)], []),
(*B(1, "DESCARGA LA APP"), [P(6.0, .5, .82, 1.5)], [C(0, .5, .90, .92, .065, "Toca aquí", "verde", "Descargar")]),
(*B(1, "DESCARGA LA APP"), [P(14.0, .5, .2, 1.6)], [C(0, .5, .14, .95, .09, "Descargando", "aviso")]),
(*B(2, "INSTÁLALA"), [P(24.0, .75, .15, 1.6), P(26.5, .6, .45, 1.4, sw="luego")], [C(0, .93, .08, .13, .055, "Tres puntitos", "toca", "arriba", 'c'), C(1, .6, .55, .72, .06, "Descargas", "luego", "Descargas")]),
(*B(2, "INSTÁLALA"), [P(32.25, .5, .32, 1.5)], [C(0, .5, .32, .95, .07, "Toca el archivo", "archivo", "Checa")]),
(*B(2, "INSTÁLALA"), [P(33.75, .6, .5, 1.5)], [C(0, .70, .56, .30, .06, "Instalar", "toca", "Instalar")]),
(*B(2, "INSTÁLALA"), [P(38.0, .5, .55, 1.3)], [C(0, .5, .705, .75, .06, "Entendido", "Toca", "Entendido")]),
(*B(2, "INSTÁLALA"), [P(40.25, .5, .5, 1.4)], [C(0, .5, .56, .40, .06, "Toca Listo", "toca", "Listo")]),
(*B(3, "APAGA PLAY PROTECT"), [P(50.0, .75, .12, 1.6)], [C(0, .90, .08, .13, .06, "Tu foto", "foto", "derecha", 'c')]),
(*B(3, "APAGA PLAY PROTECT"), [P(53.0, .5, .65, 1.4, blur=BL_MENU)], [C(0, .5, .72, .92, .06, "Play Protect", "Toca", "Protect")]),
(*B(3, "APAGA PLAY PROTECT"), [P(56.5, .75, .15, 1.6)], [C(0, .93, .08, .13, .06, "Engranaje", "toca", "engranaje", 'c')]),
(*B(3, "APAGA PLAY PROTECT"), [P(58.5, .6, .3, 1.5)], [C(0, .89, .23, .17, .06, "Apágala", "Apaga", "opción", 'c')]),
(*B(3, "APAGA PLAY PROTECT"), [P(59.75, .5, .55, 1.3), P(61.0, .5, .75, 1.3, sw="confirma")], [C(0, .5, .675, .80, .06, "Desactivar", "toca", "Desactivar"), C(1, .5, .89, .20, .09, "Tu huella", "huella", None, 'c')]),
(*B(3, "APAGA PLAY PROTECT"), [P(62.75, .6, .3, 1.5)], [C(0, .89, .285, .17, .17, "Apagadas", "opciones")]),
(*B(4, "INSTALA OTRA VEZ"), [P(65.5, .5, .32, 1.5)], [C(0, .5, .32, .95, .07, "Toca el archivo", "archivo", "Checa")]),
(*B(4, "INSTALA OTRA VEZ"), [P(67.25, .6, .5, 1.5), P(69.0, .5, .5, 1.3, sw="espera")], [C(0, .70, .56, .30, .06, "Instalar", "Toca", "Instalar")]),
(*B(4, "INSTALA OTRA VEZ"), [P(71.0, .6, .5, 1.5)], [C(0, .70, .56, .30, .06, "Abrir", "toca", "Abrir")]),
(*B(5, "ENTRA CON GOOGLE"), [P(74.0, .5, .8, 1.3)], [C(0, .5, .89, .92, .06, "Continuar con Google", "Toca", "Google")]),
(*B(5, "ENTRA CON GOOGLE"), [P(78.0, .5, .6, 1.3, blur=BL_CUENTAS)], [C(0, .5, .55, .95, .07, "Tu cuenta", "Elige", "cuenta")]),
(*B(5, "ENTRA CON GOOGLE"), [P(82.5), P(85.0, .5, .55, 1.3, sw="toca", blur=BL_LISTO)], [C(1, .5, .695, .90, .06, "Empezar", "toca", "Empezar")]),
(*B(6, "DA EL PERMISO"), [P(87.5, .5, .7, 1.3)], [C(0, .5, .82, .90, .06, "Abrir Accesibilidad", "Toca", "Accesibilidad")]),
(*B(6, "DA EL PERMISO"), [P(90.0), P(93.25, .5, .8, 1.4, sw="toca")], [C(0, .5, .62, .5, .1, "Desliza hacia abajo", "Baja", None, 'sw'), C(1, .5, .885, .95, .06, "Checa", "toca", "ofertas")]),
(*B(6, "DA EL PERMISO"), [P(94.5, .75, .15, 1.6), P(100.0, .5, .75, 1.3, sw="Te")], [C(0, .86, .16, .17, .06, "Interruptor", "Toca", "interruptor", 'c'), C(1, .5, .875, .80, .06, "Toca Cerrar", "Cerrar", "Cerrar")]),
(*B(7, "QUITA LA RESTRICCIÓN"), [P(109.0, .75, .45, 1.5)], [C(0, .875, .48, .17, .09, "Déjalo presionado", "presionado", "Checa", 'c')]),
(*B(7, "QUITA LA RESTRICCIÓN"), [P(113.0, .5, .4, 1.5)], [C(0, .43, .375, .20, .08, "Información", "Toca", "app", 'c')]),
(*B(7, "QUITA LA RESTRICCIÓN"), [P(114.5, .7, .15, 1.6)], [C(0, .80, .15, .36, .06, "Eliminar restricción", "toca", "restricción")]),
(*B(7, "QUITA LA RESTRICCIÓN"), [P(116.0, .6, .8, 1.4)], [C(0, .70, .88, .34, .06, "Eliminar", "Toca", "Eliminar")]),
(*B(8, "ACTIVA EL LECTOR"), [P(123.5, .75, .15, 1.6)], [C(0, .87, .16, .17, .06, "Interruptor", "toca", "interruptor", 'c')]),
(*B(8, "ACTIVA EL LECTOR"), [P(124.5, .6, .8, 1.4)], [C(0, .70, .88, .32, .06, "Aceptar", "Toca", "Aceptar")]),
(*B(8, "ACTIVA EL LECTOR"), [P(126.0, .75, .15, 1.6)], [C(0, .87, .16, .17, .06, "Activado", "activo", None, 'c')]),
(*B(9, "TUS METAS"), [P(140.0, .5, .5, 1.3)], [C(0, .5, .415, .92, .11, "Por kilómetro", "kilómetro"), C(0, .5, .57, .92, .11, "Por hora", "hora")]),
(*B(9, "TUS METAS"), [P(150.0, .5, .45, 1.5)], [C(0, .16, .415, .12, .055, "Menos", "menos", None, 'c'), C(0, .83, .415, .12, .055, "Más", "más", None, 'c')]),
(*B(9, "TUS METAS"), [P(162.0, .5, .7, 1.3)], [C(0, .5, .82, .90, .06, "Siguiente", "Toca", "Siguiente")]),
(*B(10, "TU AUTO"), [P(165.5, .5, .5, 1.3)], [C(0, .5, .415, .92, .10, "Km por litro", "kilómetros"), C(0, .5, .57, .92, .10, "Precio por litro", "pagas")]),
(*B(10, "TU AUTO"), [P(165.5, .5, .7, 1.3)], [C(0, .5, .67, .92, .06, "Costo por km", "cuesta"), C(0, .5, .82, .90, .06, "Siguiente", "Toca", "Siguiente")]),
(*B(10, "TU AUTO"), [P(195.0, .5, .55, 1.2)], [C(0, .5, .43, .93, .19, "Así se verá", "aviso"), C(0, .5, .82, .90, .06, "Empezar a leer ofertas", "Toca", "ofertas")]),
(*B(11, "GRABAR PANTALLA"), [P(200.0, .5, .7, 1.25)], []),
(*B(11, "GRABAR PANTALLA"), [P(200.0, .5, .75, 1.3), P(218.75, .5, .75, 1.3, sw="Te")], [C(0, .5, .80, .92, .06, "Toda la pantalla", "Toda", "pantalla"), C(1, .5, .80, .92, .06, "Toda la pantalla", "Te")]),
(*B(11, "GRABAR PANTALLA"), [P(230.5, .5, .75, 1.3)], [C(0, .5, .80, .92, .06, "", "Toda"), C(0, .70, .875, .36, .06, "Permitir", "toca", "Permitir")]),
(*B(11, "GRABAR PANTALLA"), [P(232.5, .5, .75, 1.3)], [C(0, .5, .82, .80, .06, "Permitir", "toca", "Permitir")]),
(*B(11, "GRABAR PANTALLA"), [P(234.0)], [C(0, .5, .88, .18, .08, "Leyendo", "Checa", None, 'c')]),
(*B(12, "AJUSTES"), [P(240.0, .5, .55, 1.2), P(246.0, .5, .3, 1.3, sw="ver")], [C(0, .5, .79, .90, .06, "Probar en pantalla", "toca", "pantalla"), C(1, .5, .18, .98, .22, "Así se ve", "ver")]),
(*B(12, "AJUSTES"), [P(265.0, .5, .5, 1.4)], [C(0, .86, .42, .17, .06, "Decirlo con voz", "Decirlo", None, 'c'), C(0, .5, .62, .92, .06, "Pruébalas", "Aceptar")]),
(*B(12, "AJUSTES"), [P(276.0, .5, .65, 1.4)], [C(0, .5, .70, .92, .15, "Uber · DiDi", "apps")]),
(*B(13, "FILTROS Y CUENTA"), [P(285.0, .5, .55, 1.4)], [C(0, .5, .565, .92, .06, "Recogida máxima", "kilómetros", "pasajero")]),
(*B(13, "FILTROS Y CUENTA"), [P(294.0, .5, .65, 1.4)], [C(0, .5, .74, .92, .06, "Duración máxima", "tiempo", "viaje")]),
(*B(13, "FILTROS Y CUENTA"), [P(310.5, .5, .35, 1.3, blur=BL_PERFIL)], [C(0, .5, .47, .95, .12, "Tus celulares", "celulares")]),
(*B(14, "MEJORES HORAS"), [P(320.0, .6, .55, 1.4)], [C(0, .77, .595, .42, .045, "Ver toda la semana", "Ver", "semana")]),
(*B(14, "MEJORES HORAS"), [P(336.0, .5, .5, 1.3)], [C(0, .52, .50, .90, .27, "Verde · Amarillo · Rojo", "hora")]),
(*B(14, "MEJORES HORAS"), [P(345.0, .5, .55, 1.25)], [C(0, .5, .705, .92, .07, "Todavía sin datos", "vacío")]),
("LISTO", "Para terminar", [P(360.0, .5, .75, 1.3)], [C(0, .5, .88, .18, .08, "Detener", "toca", "Detener", 'c')]),
("LISTO", "Eso es todo", [P(364.0)], []),
]
N = len(S)
def dur(f): return float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f]))
def palabras(i):
    j = json.load(open(f"voz/l{i+1:02d}.json")); c, s, e = j["c"], j["s"], j["e"]
    ws = []; cur = ""; a = b = 0
    for ch, x, y in zip(c, s, e):
        if ch == " ":
            if cur: ws.append((cur, a, b)); cur = ""
            continue
        if not cur: a = x
        cur += ch; b = y
    if cur: ws.append((cur, a, b))
    return ws
VD = [dur(f"voz/l{i+1:02d}.mp3") for i in range(N + 1)]
WS = [palabras(i) for i in range(N + 1)]
D = [VD[i] + PAD for i in range(N)] + [VD[N] + TAIL + 0.4]
OFF = [sum(D[:i]) for i in range(N + 1)]; TOTAL = OFF[N] + D[N]

def pal(i, w, fin=False):
    w, _, n = w.partition("#"); n = int(n or 1)
    for tok, a, b in WS[i]:
        if tok.strip(".,:¿?").lower().startswith(w.lower()):
            n -= 1
            if n == 0: return DL + (b if fin else a)
    raise SystemExit(f"línea {i+1}: no encuentro «{w}» en {[x[0] for x in WS[i]]}")

# tiempos locales: inicio de cada plano, aparición y toque de cada llamada
for i, (_, _, pl, cl) in enumerate(S):
    for k, p in enumerate(pl): p["ini"] = 0.0 if k == 0 else pal(i, p["sw"]) - 0.05
    for c in cl:
        p0 = pl[c["p"]]["ini"]; p1 = pl[c["p"] + 1]["ini"] if c["p"] + 1 < len(pl) else D[i]
        c["on"] = max(p0 + 0.35, pal(i, c["show"]) - 0.25) if c["show"] else p0 + 0.35
        c["tt"] = pal(i, c["tap"], fin=True) if c["tap"] else None
        c["off"] = min(p1, c["tt"] + 0.6) if c["tt"] else p1

SUBS = []
for i in range(N + 1):
    g = []
    for wd in WS[i]:
        g.append(wd)
        if len(g) >= 3 or wd[0][-1] in ".,?:":
            SUBS.append((OFF[i] + DL + g[0][1], OFF[i] + DL + g[-1][2] + .12, " ".join(x[0] for x in g))); g = []
    if g: SUBS.append((OFF[i] + DL + g[0][1], OFF[i] + DL + g[-1][2] + .12, " ".join(x[0] for x in g)))

if "audio" in sys.argv:
    os.makedirs("seg", exist_ok=True)
    for i in range(N + 1):
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", f"voz/l{i+1:02d}.mp3", "-af", f"adelay={int(DL*1000)}|{int(DL*1000)},apad=whole_dur={D[i]:.4f},atrim=0:{D[i]:.4f}", "-ar", "44100", "-ac", "1", f"seg/a{i:02d}.wav"], check=True)
    open("seg/alist.txt", "w").write("".join(f"file 'a{i:02d}.wav'\n" for i in range(N + 1)))
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "concat", "-i", "seg/alist.txt", "-af", "loudnorm=I=-14:TP=-1.5:LRA=7", "-ar", "44100", "audio.wav"], check=True)
    print("audio", round(dur("audio.wav"), 2), "esperado", round(TOTAL, 2)); sys.exit()

Fn = lambda s: ImageFont.truetype("FiraSans-Bold.ttf", s)
fB, fL, fS, f1 = Fn(42), Fn(46), Fn(70), Fn(32)
mask = Image.new("L", (W, H), 255); ImageDraw.Draw(mask).rounded_rectangle((PX, PY, PX + PW - 1, PY + PH - 1), 56, fill=0)
CORN = Image.new("RGBA", (W, H), BG + (255,)); CORN.putalpha(mask)
ImageDraw.Draw(CORN).rounded_rectangle((PX - 3, PY - 3, PX + PW + 2, PY + PH + 2), 58, outline=(42, 58, 48, 255), width=6)
FIN = Image.open("fin.png").convert("RGB")
SRC = {}
def captura(p):
    key = (p["t"], tuple(p["blur"]))
    if key not in SRC:
        im = Image.open(f"{F4}/{int(round(p['t'] * 4)) + 1:05d}.jpg").convert("RGB")
        sw, sh = im.size
        for (x, y, w, h) in p["blur"]:
            bx = (int((x - w / 2) * sw), int((y - h / 2) * sh), int((x + w / 2) * sw), int((y + h / 2) * sh))
            im.paste(im.crop(bx).filter(ImageFilter.GaussianBlur(14)), bx[:2])
        SRC[key] = im
    return SRC[key]
def ease(u): u = max(0, min(1, u)); return u * u * (3 - 2 * u)
def vista(p, lt):  # zoom actual y ventana (x0,y0,ancho) en fracción de la captura
    z = 1 + (p["z"] - 1) * ease((lt - p["ini"] - 0.25) / 1.1)
    w = 1 / z; x0 = min(max(p["fx"] - w / 2, 0), 1 - w); y0 = min(max(p["fy"] - w / 2, 0), 1 - w)
    return z, x0, y0, w
def contenido(p, lt):
    im = captura(p); sw, sh = im.size; z, x0, y0, w = vista(p, lt)
    return im.crop((x0 * sw, y0 * sh, (x0 + w) * sw, (y0 + w) * sh)).resize((PW, PH), Image.BICUBIC)
def mapea(p, lt, x, y):
    z, x0, y0, w = vista(p, lt)
    return PX + (x - x0) / w * PW, PY + (y - y0) / w * PH, z
def tl(d, s, f): return d.textlength(s, font=f)

def frame(t):
    i = max(j for j in range(N + 1) if OFF[j] <= t + 1e-9)
    if i == N: return FIN.copy()
    lt = t - OFF[i]; b1, b2, pl, cl = S[i]
    k = max(j for j in range(len(pl)) if pl[j]["ini"] <= lt + 1e-9); p = pl[k]
    cont = contenido(p, lt)
    if lt - p["ini"] < XF:   # fundido corto desde el plano anterior (o la línea anterior)
        if k > 0: prev = contenido(pl[k - 1], p["ini"])
        elif i > 0: pp = S[i - 1][2]; prev = contenido(pp[-1], D[i - 1])
        else: prev = None
        if prev is not None: cont = Image.blend(prev, cont, ease((lt - p["ini"]) / XF))
    im = Image.new("RGBA", (W, H), BG + (255,)); im.paste(cont, (PX, PY)); im.alpha_composite(CORN); im = im.convert("RGB")
    act = [c for c in cl if c["p"] == k and c["on"] <= lt < c["off"]]
    geo = []
    for c in act:
        cx, cy, z = mapea(p, lt, c["x"], c["y"]); geo.append((c, cx, cy, c["w"] * PW * z, c["h"] * PH * z))
    if any(c["k"] != 'sw' for c in act):
        a = max(min(1, (lt - c["on"]) / .25) for c in act)
        m = Image.new("L", (W, H), 0); md = ImageDraw.Draw(m)
        md.rectangle((PX, PY, PX + PW, PY + PH), fill=int(120 * a))
        for (c, cx, cy, w, h) in geo:
            if c["k"] != 'sw': md.rounded_rectangle((cx - w / 2 - 8, cy - h / 2 - 8, cx + w / 2 + 8, cy + h / 2 + 8), min(h / 2 + 8, w / 2 + 8, 36), fill=0)
        im.paste(Image.new("RGB", (W, H), (0, 0, 0)), (0, 0), m)
    d = ImageDraw.Draw(im, "RGBA")
    tw = max(tl(d, b1, f1), tl(d, b2, fB)); x0 = (W - tw) / 2 - 44
    d.rounded_rectangle((x0, 22, x0 + tw + 88, 150), 40, fill=(24, 28, 26, 255), outline=G + (255,), width=3)
    d.text((W / 2, 56), b1, font=f1, fill=G + (255,), anchor="mm"); d.text((W / 2, 108), b2, font=fB, fill=(241, 245, 242, 255), anchor="mm")
    for (c, cx, cy, ww, hh) in geo:
        e0 = lt - c["on"]; a = min(1, e0 / .25)
        if c["k"] == 'sw':   # flecha de deslizar
            yy = cy - 120 + 160 * ((e0 * 0.8) % 1)
            d.line((cx, yy - 90, cx, yy), fill=(255, 255, 255, int(230 * a)), width=14)
            d.polygon([(cx - 36, yy - 10), (cx + 36, yy - 10), (cx, yy + 34)], fill=(255, 255, 255, int(230 * a)))
            tw2 = tl(d, c["lab"], fL); ly = cy + 110
            d.rounded_rectangle((cx - tw2 / 2 - 26, ly, cx + tw2 / 2 + 26, ly + 68), 34, fill=G + (int(255 * a),))
            d.text((cx, ly + 36), c["lab"], font=fL, fill=(4, 18, 10, int(255 * a)), anchor="mm"); continue
        sc = 1 + .12 * (1 - a); ww *= sc; hh *= sc
        box = (cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2)
        rad = min(hh / 2, ww / 2, 28) if c["k"] == 'r' else min(hh / 2, ww / 2) - 1
        d.rounded_rectangle(box, rad, fill=G + (int(28 * a),), outline=G + (int(255 * a),), width=10)
        ph = e0 % 1; ex = 34 * ph
        d.rounded_rectangle((box[0] - ex, box[1] - ex, box[2] + ex, box[3] + ex), rad + ex, outline=G + (int(190 * (1 - ph) * a),), width=6)
        fx = cx + min(ww * 0.36, ww / 2 - 8); fy = cy + hh * 0.3
        r = 30
        if c["tt"] is not None and c["tt"] - 0.15 <= lt:   # toque: el dedo se hunde y sale una onda
            u = (lt - (c["tt"] - 0.15)) / 0.5
            r = 30 * (1 - 0.3 * math.sin(min(u, 1) * math.pi))
            if u < 1:
                rr = 30 + 90 * u; d.ellipse((fx - rr, fy - rr, fx + rr, fy + rr), fill=(255, 255, 255, int(120 * (1 - u))))
        d.ellipse((fx - r - 6, fy - r, fx + r + 6, fy + r + 12), fill=(0, 0, 0, int(90 * a)))
        d.ellipse((fx - r, fy - r, fx + r, fy + r), fill=(255, 255, 255, int(215 * a)), outline=G + (int(255 * a),), width=6)
        if c["lab"]:
            tw2 = tl(d, c["lab"], fL); lx = min(max(cx - tw2 / 2, PX + 34), PX + PW - tw2 - 34)
            ly = box[1] - 86 if box[1] > PY + 140 else box[3] + 22
            d.rounded_rectangle((lx - 26, ly, lx + tw2 + 26, ly + 68), 34, fill=G + (int(255 * a),))
            d.text((lx + tw2 / 2, ly + 36), c["lab"], font=fL, fill=(4, 18, 10, int(255 * a)), anchor="mm")
    for (s0, s1, tx) in SUBS:
        if s0 <= t < s1:
            tw3 = tl(d, tx, fS); x = (W - tw3) / 2; y = 1836
            d.rounded_rectangle((x - 30, y - 56, x + tw3 + 30, y + 56), 30, fill=(0, 0, 0, 185))
            d.text((W / 2, y), tx, font=fS, fill=(255, 255, 255, 255), anchor="mm"); break
    return im

if "fotos" in sys.argv:   # hoja de revisión: cada plano justo antes de su toque (o a media llamada)
    os.makedirs("qa", exist_ok=True); n = 0
    for i, (_, _, pl, cl) in enumerate(S):
        for k, p in enumerate(pl):
            cs = [c for c in cl if c["p"] == k]
            if cs:
                c = cs[-1]; lt = (c["tt"] - 0.2) if c["tt"] else (c["on"] + c["off"]) / 2
            else:
                lt = (p["ini"] + (pl[k + 1]["ini"] if k + 1 < len(pl) else D[i])) / 2
            frame(OFF[i] + lt).resize((360, 640)).save(f"qa/q{n:03d}.jpg"); n += 1
    print(n, "planos"); sys.exit()
if "info" in sys.argv:
    print("total", round(TOTAL, 1)); sys.exit()

cmd = ["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-i", "audio.wav",
       "-map", "0:v", "-map", "1:a", "-c:v", "libx264", "-crf", "19", "-preset", "medium", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "160k", "-shortest", "-movflags", "+faststart", "out.mp4"]
p = subprocess.Popen(cmd, stdin=subprocess.PIPE)
for k in range(int(TOTAL * FPS)):
    p.stdin.write(frame(k / FPS).tobytes())
p.stdin.close(); p.wait(); print("ok", round(TOTAL, 1))
