"""Mezcla de audio y mp4 del estilo A3: python3 -I mezclar.py <cuadros_dir> <voz.wav> <sonidos.json> <salida.mp4>
Los efectos son los oficiales de _base/assets/sfx; «whoosh» y «thump» se sintetizan aquí (sin archivos)."""
import json, os, subprocess, sys, tempfile
cuadros, voz, sonidos, salida = sys.argv[1:5]
AQUI = os.path.dirname(os.path.abspath(__file__))
SFX = os.path.join(AQUI, "..", "_base", "assets", "sfx")
S = json.load(open(sonidos, encoding="utf-8"))
tmp = tempfile.mkdtemp()
wh, th = os.path.join(tmp, "whoosh.wav"), os.path.join(tmp, "thump.wav")
subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "anoisesrc=d=0.6:c=pink:r=44100", "-af", "bandpass=f=900:w=1.5,afade=t=in:d=0.3,afade=t=out:st=0.3:d=0.3,volume=1.6", wh], check=True)
subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=f=55:d=0.5", "-af", "afade=t=out:d=0.45,volume=3", th], check=True)
ruta = lambda n: wh if n == "whoosh" else th if n == "thump" else os.path.join(SFX, n + ".mp3")
ins, fc, lab = ["-i", voz], ["[1]volume=1.0[v]"], ["[v]"]
for i, (n, ms, vol) in enumerate(S["eventos"], 2):
    ins += ["-i", ruta(n)]
    fc.append(f"[{i}]adelay={ms}:all=1,volume={vol}[e{i}]")
    lab.append(f"[e{i}]")
st, d = S["fade_out"]
fc.append("".join(lab) + f"amix=inputs={len(lab)}:normalize=0,alimiter=limit=0.95,afade=t=out:st={st}:d={d}[a]")
subprocess.run(["ffmpeg", "-v", "error", "-y", "-framerate", "30", "-i", os.path.join(cuadros, "%04d.png")] + ins +
               ["-filter_complex", ";".join(fc), "-map", "0:v", "-map", "[a]", "-c:v", "libx264", "-profile:v", "high", "-pix_fmt", "yuv420p",
                "-crf", "19", "-c:a", "aac", "-b:a", "192k", "-t", str(S["duracion"]), "-movflags", "+faststart", salida], check=True)
print("listo:", salida)
