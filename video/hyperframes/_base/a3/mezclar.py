"""Mezcla de audio y mp4 del estilo A3: python3 -I mezclar.py <cuadros_dir> <sonidos.json> <salida.mp4> (--voz voz.wav | --proyecto <dir>)
--proyecto arma la voz con assets/voice/NN.wav puestos en el segundo de cada línea (tiempos.json).
Los efectos son los oficiales de _base/assets/sfx; «whoosh» y «thump» se sintetizan aquí."""
import json, os, subprocess, sys, tempfile
a = sys.argv[1:]
cuadros, sonidos, salida = a[0], a[1], a[2]
AQUI = os.path.dirname(os.path.abspath(__file__))
SFX = os.path.join(AQUI, "..", "assets", "sfx")
S = json.load(open(sonidos, encoding="utf-8"))
tmp = tempfile.mkdtemp()
wh, th = os.path.join(tmp, "whoosh.wav"), os.path.join(tmp, "thump.wav")
subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "anoisesrc=d=0.6:c=pink:r=44100", "-af", "bandpass=f=900:w=1.5,afade=t=in:d=0.3,afade=t=out:st=0.3:d=0.3,volume=1.6", wh], check=True)
subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=f=55:d=0.5", "-af", "afade=t=out:d=0.45,volume=3", th], check=True)
ruta = lambda n: wh if n == "whoosh" else th if n == "thump" else os.path.join(SFX, n + ".mp3")
ins, fc, lab = [], [], []
if "--voz" in a:
    ins += ["-i", a[a.index("--voz") + 1]]
    fc.append("[1]volume=1.0[v]")
    lab.append("[v]")
    base = 2
else:
    d = a[a.index("--proyecto") + 1]
    T = json.load(open(os.path.join(d, "tiempos.json"), encoding="utf-8"))
    for i, sc in enumerate(T["scenes"]):
        ins += ["-i", os.path.join(d, "assets", "voice", f"{sc['n']:02d}.wav")]
        fc.append(f"[{i + 1}]adelay={int(round(sc['start'] * 1000))}:all=1[v{i}]")
        lab.append(f"[v{i}]")
    base = len(T["scenes"]) + 1
for i, (n, ms, vol) in enumerate(S["eventos"], base):
    ins += ["-i", ruta(n)]
    fc.append(f"[{i}]adelay={ms}:all=1,volume={vol}[e{i}]")
    lab.append(f"[e{i}]")
st, d_ = S["fade_out"]
fc.append("".join(lab) + f"amix=inputs={len(lab)}:normalize=0,alimiter=limit=0.95,afade=t=out:st={st}:d={d_}[a]")
subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", "30", "-i", os.path.join(cuadros, "%04d.png")] + ins +
               ["-filter_complex", ";".join(fc), "-map", "0:v", "-map", "[a]", "-c:v", "libx264", "-profile:v", "high", "-pix_fmt", "yuv420p",
                "-crf", "19", "-c:a", "aac", "-b:a", "192k", "-t", str(S["duracion"]), "-movflags", "+faststart", salida], check=True)
print("listo:", salida)
