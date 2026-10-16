"""Voz del cierre estándar con una pausa entre «Descarga Checa» y «y checa cuánto te queda…».
Uso: python3 -I video/scripts/cierre_con_pausa.py <carpeta assets/voice> <NN> [pausa_seg=0.45]
Genera NN.mp3 y NN.json (mismo formato que elevenlabs.py) para que armar.py no vuelva a pedir esa línea."""
import json, os, subprocess, sys
carpeta, nn = sys.argv[1], sys.argv[2]
pausa = float(sys.argv[3]) if len(sys.argv) > 3 else 0.45
EL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "elevenlabs.py")
A, B = "Descarga Checa", "y checa cuánto te queda de cada viaje."
for suf, txt in (("a", A), ("b", B)):
    subprocess.run(["python3", "-I", EL, "voz", carpeta, nn + suf, txt, "dlGxemPxFMTY7iXagmOj", "1.0"], check=True)
dur = lambda f: float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f]).strip())
da = dur(f"{carpeta}/{nn}a.mp3")
sil = f"{carpeta}/{nn}_sil.mp3"
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "lavfi", "-i", "anullsrc=r=44100:cl=mono", "-t", str(pausa), "-q:a", "9", sil], check=True)
lista = f"{carpeta}/{nn}_lista.txt"
open(lista, "w").write("".join(f"file '{os.path.abspath(x)}'\n" for x in (f"{carpeta}/{nn}a.mp3", sil, f"{carpeta}/{nn}b.mp3")))
subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", lista, "-ar", "44100", "-ac", "1", f"{carpeta}/{nn}.mp3"], check=True)
ja, jb = (json.load(open(f"{carpeta}/{nn}{x}.json")) for x in "ab")
off = da + pausa
out = {"c": ja["c"] + [" "] + jb["c"], "s": ja["s"] + [ja["e"][-1]] + [t + off for t in jb["s"]], "e": ja["e"] + [off] + [t + off for t in jb["e"]]}
json.dump(out, open(f"{carpeta}/{nn}.json", "w"), ensure_ascii=False)
for x in ("a", "b"):
    for ext in ("mp3", "json"):
        os.remove(f"{carpeta}/{nn}{x}.{ext}")
os.remove(sil); os.remove(lista)
print(nn, "con pausa de", pausa, "s")
