#!/usr/bin/env python3
"""Arma public/index.html (HyperFrames) para un video de Eduardo a cámara con la marca Checa v2.
Uso: python3 componer.py <carpeta_trabajo> <spec.json>
Necesita en <carpeta_trabajo>: words.json, public/input-video.mp4, public/gsap.min.js, public/fonts/*.woff2 (los deja preparar.sh)
Escribe: public/index.html y subtitulos.json

Spec (ver ejemplos/spec-escenas.json):
  id, duracion, ajustes, correcciones, colores {palabra: verde|amarillo|rojo}, zooms [[ini, fin, escala]]
  gancho   {texto, resalta?, ini, fin}
  nametag  {nombre, detalle, ini, fin}
  escenas  [{id, tipo, ini, fin, nivel?, ...}]  tipos: dato, lista, comparar, pasos, numero, popup
  cierre   {ini}   (pantalla final estándar de Checa)
Reutiliza de video/hyperframes/checa-marca-v2/construir.py: el pop-up (sin rediseñar), el logo animado y el agrupado de subtítulos."""
import json, math, re, html, sys, os, importlib.util, contextlib

work, specp = sys.argv[1], sys.argv[2]
SK = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.abspath(os.path.join(SK, '..', '..', '..'))
TPL = os.path.join(ROOT, 'video', 'hyperframes', 'checa-marca-v2')

@contextlib.contextmanager
def en(d):
    old = os.getcwd(); os.chdir(d)
    try: yield
    finally: os.chdir(old)

_old = os.getcwd()
_s = importlib.util.spec_from_file_location('construir_v2', os.path.join(TPL, 'construir.py'))
M = importlib.util.module_from_spec(_s); _s.loader.exec_module(M); os.chdir(_old)

spec = json.load(open(specp)); W = json.load(open(f'{work}/words.json'))
AJ = spec.get('ajustes', {})   # {"1.16": null, "1.88": "qué"}: quitar o cambiar la palabra que empieza en ese segundo
W = [w for w in W if AJ.get(f"{w['s']:.2f}", w['t']) is not None]
for w in W: w['t'] = AJ.get(f"{w['s']:.2f}", spec.get('correcciones', {}).get(w['t'], w['t']))
FPS = 30; q = lambda x: round(round(x*FPS)/FPS, 4); esc = html.escape
norm = lambda t: re.sub(r'[.,¿?¡!«»"]', '', t).lower()
def wt(word, after=0, off=0.0):
    for w in W:
        if w['s'] >= after and norm(w['t']) == word.lower(): return w['s'] + off
    raise KeyError(f'palabra no encontrada: {word} (después de {after})')
anc = lambda a: wt(a[0], a[1], a[2] if len(a) > 2 else 0.0)
DUR = spec['duracion']
NIV = {'verde': ('#22C55E', '#04210F'), 'amarillo': ('#FACC15', '#2B2200'), 'rojo': ('#EF4444', '#1F0000'), 'hueso': ('#F4F4EF', '#0C0C10')}
col = lambda n: NIV[n or 'hueso'][0]
T = 0.3   # lo que tarda la raya de carretera en cruzar la pantalla
H = [f'<div id="video-wrap"><video id="bg-video" src="input-video.mp4" playsinline data-has-audio="true" data-start="0" data-duration="{DUR}" data-track-index="1"></video></div>']
J = ["const tl=gsap.timeline({paused:true});", "gsap.set('#video-wrap',{transformOrigin:'50% 28%'});"]
fin = lambda sel, t, y=24, d=0.35: J.append(f"tl.fromTo('{sel}',{{opacity:0,y:{y}}},{{opacity:1,y:0,duration:{d},ease:'power3.out'}},{q(t)});")
fout = lambda sel, t, d=0.25: J.append(f"tl.to('{sel}',{{opacity:0,duration:{d},ease:'power2.in'}},{q(t)});")
ocupado = []   # (ini, fin, nombre) para avisar de empalmes

# ---- gancho (0–3 s): la pregunta que se hace el conductor, con sus palabras ----
g = spec.get('gancho')
if g:
    txt = esc(g['texto'])
    if g.get('resalta'): txt = txt.replace(esc(g['resalta']), f'<em>{esc(g["resalta"])}</em>', 1)
    H.append(f'<div class="gancho clip" id="gh" data-start="{g["ini"]}" data-duration="{round(g["fin"]-g["ini"]+0.4,2)}" data-track-index="2">{txt}</div>')
    fin('#gh', g['ini'], -24); fout('#gh', g['fin']); ocupado.append((g['ini'], g['fin'], 'gancho'))
nt = spec.get('nametag')
if nt:
    H.append(f'<div class="nt clip" id="nt" data-start="{nt["ini"]}" data-duration="{round(nt["fin"]-nt["ini"]+0.4,2)}" data-track-index="2"><b>{esc(nt["nombre"])}</b><span>{esc(nt["detalle"])}</span></div>')
    fin('#nt', nt['ini'], 20); fout('#nt', nt['fin']); ocupado.append((nt['ini'], nt['fin'], 'rótulo'))

# ---- escenas gráficas: sustituyen las tomas grabadas; la voz y los subtítulos no se cortan ----
POPCSS = ''
def sweep(c, ini, fin_, entra=True, sale=True):
    # la raya con su luz verde cruza de izquierda a derecha y deja la escena a su paso; al salir, regresa igual
    sw = f'#{c}-sw'
    J.append(f"tl.set('{sw}',{{opacity:0}},0);")
    if entra:
        J.append(f"tl.set('{sw}',{{opacity:1}},{q(ini)});")
        J.append(f"tl.fromTo('{sw}',{{x:0}},{{x:1080,duration:{T},ease:'power3.out'}},{q(ini)});")
        J.append(f"tl.fromTo('#{c}',{{clipPath:'inset(0 100% 0 0)'}},{{clipPath:'inset(0 0% 0 0)',duration:{T},ease:'power3.out'}},{q(ini)});")
        J.append(f"tl.set('{sw}',{{opacity:0}},{q(ini+T)});")
    if sale:
        J.append(f"tl.set('{sw}',{{opacity:1}},{q(fin_)});")
        J.append(f"tl.fromTo('{sw}',{{x:1080}},{{x:0,duration:{T},ease:'power3.in',immediateRender:false}},{q(fin_)});")
        J.append(f"tl.fromTo('#{c}',{{clipPath:'inset(0 0% 0 0)'}},{{clipPath:'inset(0 100% 0 0)',duration:{T},ease:'power3.in',immediateRender:false}},{q(fin_)});")
        J.append(f"tl.set('{sw}',{{opacity:0}},{q(fin_+T)});")

for e in spec.get('escenas', []):
    c = e['id']; ini, fin_, tipo = e['ini'], e['fin'], e['tipo']
    if not 2.5 <= fin_ - ini <= 6: print(f'AVISO {c}: dura {fin_-ini:.1f} s (lo normal es 3 a 5 s)')
    ocupado.append((ini, fin_, c))
    base = ini + T + 0.2; items = []
    def part(cls, inner, t, style='', kind='fade'):
        k = len(items); items.append((t, kind)); return f'<div class="e-it {cls}" id="{c}-{k}" style="{style}">{inner}</div>'
    when = lambda it, i: anc(it['en']) - 0.1 if it.get('en') else base + 0.25*i
    nv = e.get('nivel'); body = ''
    br = lambda s: esc(s).replace('\n', '<br>')
    if tipo == 'dato':
        body += part('e-etq', esc(e['etiqueta']), base)
        body += part('e-tit', br(e['titulo']), base + 0.08, f'color:{col(nv)}')
        body += part('e-raya', '', base + 0.25, '', 'barra')
        if e.get('texto'): body += part('e-txt', br(e['texto']), base + 0.4)
    elif tipo == 'lista':
        body += part('e-h', br(e['titulo']), base)
        n = len(e['items']); paso = min(150, 540 // max(n, 1))
        for i, it in enumerate(e['items']):
            body += part('e-li', f'<i style="background:{col(it.get("nivel"))}"></i><span>{esc(it["texto"])}</span>', when(it, i + 1), f'top:{540 + i*paso}px')
    elif tipo == 'comparar':
        b = e['b']; fondo, tinta = NIV[b.get('nivel', 'rojo')]
        body += part('e-a', f'<div class="e-etq">{esc(e["a"]["etiqueta"])}</div><div class="e-q">{br(e["a"]["texto"])}</div>', base)
        body += part('e-b', f'<div class="e-etq" style="color:{tinta}">{esc(b["etiqueta"])}</div><div class="e-q">{br(b["texto"])}</div>',
                     anc(e['b_en']) - 0.1 if e.get('b_en') else base + 1.0, f'background:{fondo};color:{tinta}')
    elif tipo == 'pasos':
        body += part('e-h', br(e['titulo']), base)
        n = len(e['pasos']); y0, y1 = 570, 1060; paso = (y1 - y0) / max(n - 1, 1)
        body += part('e-pl', f'<svg width="20" height="{y1-y0+40}" viewBox="0 0 20 {y1-y0+40}"><line x1="10" y1="10" x2="10" y2="{y1-y0+30}" stroke="#FACC15" stroke-width="8" stroke-linecap="round" stroke-dasharray="1 24"/></svg>', base + 0.1, f'top:{y0-20}px;height:{y1-y0+40}px', 'linea')
        for i, it in enumerate(e['pasos']):
            y = y0 + i*paso; ultimo = i == n - 1
            body += part('e-nodo', '', when(it, i + 1), f'top:{y-20:.0f}px' + (';background:#22C55E' if ultimo else ''))
            body += part('e-pas', esc(it['texto']), when(it, i + 1) + 0.05, f'top:{y-30:.0f}px')
    elif tipo == 'numero':
        body += part('e-etq', esc(e['etiqueta']), base, 'text-align:center')
        body += part('e-big', esc(e['numero']), base + 0.08, f'color:{col(nv)}')
        if e.get('texto'): body += part('e-sub', br(e['texto']), base + 0.3)
        if e.get('ejemplo', True): body += part('e-pie', 'Números de ejemplo', base + 0.35)
    elif tipo == 'popup':
        M.validar_popup(e['popup'])
        if not POPCSS:
            with en(TPL): POPCSS = M.css_popup()
        body += part('e-pop', M.popup(e['popup'], e.get('u', 1.0)), base)
        body += part('e-popnota', 'Números de ejemplo', base + 0.3)
    else: raise SystemExit(f'tipo de escena desconocido: {tipo}')
    H.append(f'<div class="escena clip" id="{c}" data-layout-allow-occlusion data-start="{ini}" data-duration="{round(fin_-ini+T+0.1,2)}" data-track-index="4">{body}</div>')
    H.append(f'<div class="barrido clip" id="{c}-sw" data-layout-allow-occlusion data-start="{ini}" data-duration="{round(fin_-ini+T+0.1,2)}" data-track-index="4"></div>')
    sweep(c, ini, fin_)
    for k, (t, kind) in enumerate(items):
        if kind == 'barra': J.append(f"tl.fromTo('#{c}-{k}',{{opacity:1,clipPath:'inset(0 100% 0 0)'}},{{clipPath:'inset(0 0% 0 0)',duration:0.4,ease:'power3.out'}},{q(t)});")
        elif kind == 'linea': J.append(f"tl.fromTo('#{c}-{k}',{{opacity:1,clipPath:'inset(0 0 100% 0)'}},{{clipPath:'inset(0 0 0% 0)',duration:0.5,ease:'power2.inOut'}},{q(t)});")
        else: fin(f'#{c}-{k}', t, 20)

# ---- pantalla final estándar: logo, Descarga Checa, cuentas y link ----
cz = spec.get('cierre')
if cz:
    ini = cz['ini']; ocupado.append((ini, DUR, 'cierre'))
    t0 = ini + T
    inner = (f'<div class="e-it" id="cz-lgw" style="left:465px;top:190px">{M.logo_svg("cz", 150, 250)}</div>'
             '<div class="e-it nombre" id="cz-nom">Checa</div><div class="e-it pil" id="cz-pil">Descarga Checa</div>'
             '<div class="e-it sa" id="cz-sa">Solo Android</div>'
             '<div class="e-it cu" id="cz-cu">síguenos en tiktok<br><b>@checa.driver</b><br>creador de Checa<br><b>@Eduardoalv9802</b></div>'
             '<div class="e-it li" id="cz-li">jeal1498.github.io/checa-web</div>')
    H.append(f'<div class="escena cierre-c clip" id="cz" data-layout-allow-occlusion data-start="{ini}" data-duration="{round(DUR-ini,2)}" data-track-index="4">{inner}</div>')
    H.append(f'<div class="barrido clip" id="cz-sw" data-layout-allow-occlusion data-start="{ini}" data-duration="{round(T+0.1,2)}" data-track-index="4"></div>')
    sweep('cz', ini, DUR, sale=False)
    fin('#cz-lgw', t0, 30, 0.3); J.append(M.js_logo('cz', t0))
    fin('#cz-nom', t0 + 1.2, 30, 0.45); fin('#cz-pil', t0 + 1.3, 30, 0.4); fin('#cz-sa', t0 + 1.6, 0, 0.3)
    fin('#cz-cu', t0 + 2.1, 20, 0.4); fin('#cz-li', t0 + 2.6, 0, 0.4)

# ---- avisos de empalme y de proporción ----
ocupado.sort()
for a, b in zip(ocupado, ocupado[1:]):
    if b[0] < a[1]: print(f'AVISO: {a[2]} ({a[0]}–{a[1]}) se empalma con {b[2]} ({b[0]}–{b[1]})')
tot = sum(x['fin'] - x['ini'] for x in spec.get('escenas', []))
if tot / DUR > 0.40: print(f'AVISO: las escenas suman {100*tot/DUR:.0f} % del video (el tope es 40 %)')

# ---- zooms (punch-ins) ----
for (t0_, t1_, s) in spec.get('zooms', []): J.append(f"tl.to('#video-wrap',{{scale:{s},duration:{q(t1_-t0_)},ease:'power2.inOut'}},{t0_});")

# ---- subtítulos v2: sin placa, sombra oscura, palabra por palabra (activa en bloque amarillo; verde = ganas, rojo = pierdes) ----
CL = {k.lower(): v for k, v in spec.get('colores', {}).items()}
grupos = M.grupos_subtitulo([(w['t'], w['s'], w['e']) for w in W])
sub = []
for gi, gr in enumerate(grupos):
    s0, e0 = gr[0][1], gr[-1][2] + 0.25
    if gi + 1 < len(grupos): e0 = min(e0, grupos[gi + 1][0][1] - 0.03)
    spans = ''.join(f'<span class="w" id="c{gi}w{i}">{esc(t)}</span>' for i, (t, _, _) in enumerate(gr))
    H.append(f'<div class="cap clip" id="c{gi}" data-start="{q(s0)}" data-duration="{q(e0-s0)}" data-track-index="5"><div class="ln">{spans}</div></div>')
    J.append(f"tl.fromTo('#c{gi} .ln',{{opacity:0,y:10}},{{opacity:1,y:0,duration:0.15,ease:'power2.out'}},{q(s0)});")
    for i, (t, ws, we) in enumerate(gr):
        nv = CL.get(norm(t)); hecho = col(nv) if nv else '#F4F4EF'   # activa = bloque amarillo con texto oscuro (estilo A); dicha = hueso, o verde/rojo si es cifra o pérdida
        J.append(f"tl.set('#c{gi}w{i}',{{color:'#14141A',backgroundColor:'#FACC15',textShadow:'none'}},{q(ws)});")
        J.append(f"tl.set('#c{gi}w{i}',{{color:'{hecho}',backgroundColor:'rgba(250,204,21,0)',textShadow:'0 2px 0 #0C0C10, 0 0 14px #0C0C10'}},{q(max(we, ws + 0.1))});")
    sub.append({'ini': round(s0, 2), 'fin': round(e0, 2), 'texto': ' '.join(t for t, _, _ in gr)})
J.append(f"window.__timelines=window.__timelines||{{}};window.__timelines['{spec['id']}']=tl;")
css = open(f'{SK}/plantillas/estilo.css').read() + POPCSS
doc = f'''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=1080, height=1920">
<style>
@font-face{{font-family:"Bricolage Grotesque";src:url("fonts/bricolage-grotesque-latin-800-normal.woff2") format("woff2");font-weight:800;font-display:block}}
@font-face{{font-family:"DM Sans";src:url("fonts/dm-sans-latin-400-normal.woff2") format("woff2");font-weight:400;font-display:block}}
@font-face{{font-family:"DM Sans";src:url("fonts/dm-sans-latin-500-normal.woff2") format("woff2");font-weight:500;font-display:block}}
@font-face{{font-family:"DM Sans";src:url("fonts/dm-sans-latin-700-normal.woff2") format("woff2");font-weight:700;font-display:block}}
@font-face{{font-family:"JetBrains Mono";src:url("fonts/jetbrains-mono-latin-500-normal.woff2") format("woff2");font-weight:500;font-display:block}}
@font-face{{font-family:"JetBrains Mono";src:url("fonts/jetbrains-mono-latin-700-normal.woff2") format("woff2");font-weight:700;font-display:block}}
{css}
</style></head><body>
<div id="root" data-composition-id="{spec['id']}" data-start="0" data-duration="{DUR}" data-width="1080" data-height="1920" data-fps="30">
{chr(10).join(H)}
<script src="gsap.min.js"></script>
<script>(function(){{
{chr(10).join(J)}
}})();</script>
</div></body></html>'''
open(f'{work}/public/index.html', 'w').write(doc)
json.dump(sub, open(f'{work}/subtitulos.json', 'w'), ensure_ascii=False, indent=1)
print(len(sub), 'subtítulos →', f'{work}/public/index.html')
