#!/usr/bin/env python3
"""Une varios clips cortos de Eduardo en un solo video 1080x1920 listo para componer.
Uso: python3 unir_clips.py <carpeta_trabajo> <plan.json>

plan.json:
 {"lufs": -14,
  "clips": [{"archivo": "c1.mp4", "words": "w1.json", "ini": 0.90, "fin": 5.15, "fin_en_silencio": [2.40, 2.70]}],
  "cierre": {"voz": "99.mp3", "json": "99.json", "ini_voz": 0.5, "pad_fin": 2.0}}
 - ini / fin: segundos del clip original que se conservan (el corte entre clips es directo).
 - fin_en_silencio [a, b]: en vez de "fin", corta en el instante más callado entre a y b (para quitar una frase sin dejar rastro).
 - quitar [[a, b], ...]: tramos del clip que se eliminan (una pausa larga). Las dos orillas se unen con un fundido corto
   ("fundido_corte", 0.08 s por defecto: más largo se ve doble la imagen si la cámara se movió) y los subtítulos y tiempos posteriores se recorren solos. Elige a y b donde la imagen casi no cambie.
 - words: transcripción por palabra de ese clip ([{"t","s","e"}], de transcribir.py).
 - cierre (opcional): la voz del cierre estándar (video/scripts/cierre_con_pausa.py). Se agrega al final sobre el último cuadro congelado,
   con el mismo volumen que el habla de Eduardo, y sus palabras entran a words.json para los subtítulos.
Escribe: public/input-video.mp4, words.json y union.json (inicio de cada clip, cierre_ini y duracion total)."""
import json, os, re, subprocess, sys
import numpy as np

W, plan = sys.argv[1], json.load(open(sys.argv[2]))
os.makedirs(f'{W}/public', exist_ok=True)
T = f'{W}/_union'; os.makedirs(T, exist_ok=True)
FADE = 0.01   # fundido de 10 ms en cada corte para que no suene un clic


def run(*a, **k): return subprocess.run(a, check=True, **k)
def dur(f): return float(run('ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', f, capture_output=True, text=True).stdout)


def lufs(f):
    r = subprocess.run(['ffmpeg', '-nostats', '-i', f, '-af', 'loudnorm=print_format=json', '-f', 'null', '-'], capture_output=True, text=True).stderr
    return json.loads(re.search(r'\{[^{}]*"input_i"[^{}]*\}', r, re.S).group(0))


def corte_en_silencio(f, a, b):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', f, '-vn', '-f', 'f32le', '-ac', '1', '-ar', '16000', '-'], capture_output=True).stdout
    x = np.frombuffer(raw, dtype=np.float32); w = 160   # ventanas de 10 ms
    mejor = min(range(int(a*16000), int(b*16000) - w, w), key=lambda i: float(np.sqrt(np.mean(x[i:i+w]**2))))
    return mejor / 16000 + 0.01


segs, words, off = [], [], 0.0


def segmento(archivo, p0, p1, out, entra, sale):
    L = p1 - p0
    af = 'aresample=48000' + (f',afade=t=in:d={FADE}' if entra else '') + (f',afade=t=out:st={L-FADE:.3f}:d={FADE}' if sale else '')
    run('ffmpeg', '-v', 'error', '-y', '-ss', f'{p0}', '-i', archivo, '-t', f'{L:.3f}',
        '-vf', 'scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,fps=30,format=yuv420p', '-af', af,
        '-c:v', 'libx264', '-crf', '17', '-g', '30', '-keyint_min', '30', '-c:a', 'aac', '-ar', '48000', '-ac', '2', '-b:a', '192k', out)
    return L


for i, c in enumerate(plan['clips']):
    ini, fin = c['ini'], c.get('fin')
    if c.get('fin_en_silencio'):
        fin = corte_en_silencio(c['archivo'], *c['fin_en_silencio'])
    partes, cur = [], ini
    for a, b in sorted(c.get('quitar', [])):
        partes.append((cur, a)); cur = b
    partes.append((cur, fin))
    D = c.get('fundido_corte', 0.08)
    out0 = f'{T}/seg{i}_0.mp4'
    L = segmento(c['archivo'], partes[0][0], partes[0][1], out0, True, len(partes) == 1)
    actual, mapa = out0, [(partes[0][0], partes[0][1], 0.0)]
    for k in range(1, len(partes)):
        nxt = f'{T}/seg{i}_{k}.mp4'; Lk = segmento(c['archivo'], partes[k][0], partes[k][1], nxt, False, k == len(partes) - 1)
        unido = f'{T}/seg{i}_u{k}.mp4'; ox = L - D
        run('ffmpeg', '-v', 'error', '-y', '-i', actual, '-i', nxt, '-filter_complex',
            f'[0:v][1:v]xfade=transition=fade:duration={D}:offset={ox:.3f},format=yuv420p[v];[0:a][1:a]acrossfade=d={D}[a]',
            '-map', '[v]', '-map', '[a]', '-c:v', 'libx264', '-crf', '17', '-g', '30', '-keyint_min', '30', '-c:a', 'aac', '-ar', '48000', '-ac', '2', '-b:a', '192k', unido)
        mapa.append((partes[k][0], partes[k][1], ox)); actual = unido; L = L + Lk - D
    for w in json.load(open(c['words'])):
        for p0, p1, o in mapa:   # una palabra que apenas empieza antes del corte no se queda
            if p0 - 0.01 <= w['s'] < p1 - 0.08:
                words.append({'t': w['t'], 's': round(off + o + w['s'] - p0, 2), 'e': round(min(off + L, off + o + w['e'] - p0), 2)}); break
    if words and words[-1]['t'][-1] not in '.?!,':
        words[-1]['t'] += '.'   # el final de cada clip cierra el subtítulo, aunque se haya cortado una frase
    segs.append((actual, round(off, 3), round(L, 3))); off += L
habla = off
cierre = plan.get('cierre')
if cierre:
    voz = cierre['voz']; ini_voz = cierre.get('ini_voz', 0.5); Dv = dur(voz); Pf = ini_voz + Dv + cierre.get('pad_fin', 2.0)
    # la voz del cierre al mismo volumen que el habla de Eduardo
    lista0 = f'{T}/habla.txt'; open(lista0, 'w').write(''.join(f"file '{os.path.abspath(s)}'\n" for s, _, _ in segs))
    run('ffmpeg', '-v', 'error', '-y', '-f', 'concat', '-safe', '0', '-i', lista0, '-c', 'copy', f'{T}/habla.mp4')
    gan = float(lufs(f'{T}/habla.mp4')['input_i']) - float(lufs(voz)['input_i'])
    run('ffmpeg', '-v', 'error', '-y', '-sseof', '-0.1', '-i', segs[-1][0], '-frames:v', '1', f'{T}/ultimo.png')
    ms = int(ini_voz * 1000)
    run('ffmpeg', '-v', 'error', '-y', '-loop', '1', '-framerate', '30', '-i', f'{T}/ultimo.png', '-i', voz, '-filter_complex',
        f'[1:a]volume={gan:.2f}dB,adelay={ms}|{ms},apad=whole_dur={Pf:.3f},aresample=48000,pan=stereo|c0=c0|c1=c0[a]',
        '-map', '0:v', '-map', '[a]', '-t', f'{Pf:.3f}', '-c:v', 'libx264', '-crf', '17', '-g', '30', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-ar', '48000', '-b:a', '192k', f'{T}/cierre.mp4')
    segs.append((f'{T}/cierre.mp4', round(habla, 3), round(Pf, 3)))
    j = json.load(open(cierre['json'])); txt = ''.join(j['c']); k = 0
    for m in re.finditer(r'\S+', txt):   # palabras a partir de los tiempos por carácter
        words.append({'t': m.group(0), 's': round(habla + ini_voz + j['s'][m.start()], 2), 'e': round(habla + ini_voz + j['e'][m.end()-1], 2)})
lista = f'{T}/todo.txt'; open(lista, 'w').write(''.join(f"file '{os.path.abspath(s)}'\n" for s, _, _ in segs))
run('ffmpeg', '-v', 'error', '-y', '-f', 'concat', '-safe', '0', '-i', lista, '-c', 'copy', f'{T}/todo.mp4')
m = lufs(f'{T}/todo.mp4'); I = plan.get('lufs', -14)
af = (f"loudnorm=I={I}:TP=-1.5:LRA=11:measured_I={m['input_i']}:measured_TP={m['input_tp']}:measured_LRA={m['input_lra']}:"
      f"measured_thresh={m['input_thresh']}:offset={m['target_offset']}:linear=true,aresample=48000")
run('ffmpeg', '-v', 'error', '-y', '-i', f'{T}/todo.mp4', '-c:v', 'copy', '-af', af, '-c:a', 'aac', '-ar', '48000', '-b:a', '192k', '-movflags', '+faststart', f'{W}/public/input-video.mp4')
json.dump(words, open(f'{W}/words.json', 'w'), ensure_ascii=False)
total = dur(f'{W}/public/input-video.mp4')
json.dump({'clips': [{'inicio': o, 'dur': l} for _, o, l in segs], 'cierre_ini': round(habla, 3) if cierre else None, 'duracion': round(total, 2)},
          open(f'{W}/union.json', 'w'), indent=1)
print(f'{len(plan["clips"])} clips, habla {habla:.2f} s' + (f', cierre desde {habla:.2f} s' if cierre else '') + f', total {total:.2f} s, {len(words)} palabras')
for s, o, l in segs: print(f'  {os.path.basename(s)}: entra en {o:.2f} s, dura {l:.2f} s')
