"""Voz de Fernando (ElevenLabs) con tiempos reales por palabra → voz/NN.mp3 (commiteados), renders/voz.wav y palabras.json.
Una llamada por frase; si voz/NN.mp3 y NN.json ya existen no se vuelve a pedir. Uso: python3 -I voz.py"""
import json, os, subprocess, importlib.util
AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.abspath(os.path.join(AQUI, *[".."] * 6))
EL = os.path.join(RAIZ, "video", "scripts", "elevenlabs.py")
sp = importlib.util.spec_from_file_location("est", os.path.join(AQUI, "estimar_tiempos.py"))
src = open(os.path.join(AQUI, "estimar_tiempos.py"), encoding="utf-8").read()
FRASES = eval(src[src.index("FRASES = ") + 9:src.index("]\n", src.index("FRASES = ")) + 1])
PAUSA = 0.28
os.makedirs(os.path.join(AQUI, "voz"), exist_ok=True); os.makedirs(os.path.join(AQUI, "renders"), exist_ok=True)
t, out, partes = 0.25, [], []
for n, f in enumerate(FRASES, 1):
    b = os.path.join(AQUI, "voz", f"{n:02d}")
    if not (os.path.exists(b + ".mp3") and os.path.exists(b + ".json")):
        subprocess.run(["python3", "-I", EL, "voz", os.path.join(AQUI, "voz"), f"{n:02d}", f, "dlGxemPxFMTY7iXagmOj", "1.0"], check=True)
    a = json.load(open(b + ".json", encoding="utf-8"))
    cur, s0, e0 = "", None, None
    for c, s, e in zip(a["c"], a["s"], a["e"]):
        if c.isspace():
            if cur: out.append([cur, round(t + s0, 2), round(t + e0, 2)]); cur = ""
        else:
            if not cur: s0 = s
            cur += c; e0 = e
    if cur: out.append([cur, round(t + s0, 2), round(t + e0, 2)])
    dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", b + ".mp3"], capture_output=True, text=True).stdout)
    partes.append((b + ".mp3", t)); t += dur + PAUSA
total = round(t + 1.2, 1)
cmd = ["ffmpeg", "-y", "-loglevel", "error"]
for p, _ in partes: cmd += ["-i", p]
fc = "".join(f"[{i}:a]adelay={int(s*1000)}:all=1[a{i}];" for i, (_, s) in enumerate(partes)) + "".join(f"[a{i}]" for i in range(len(partes))) + f"amix=inputs={len(partes)}:normalize=0,apad=whole_dur={total},loudnorm=I=-14:TP=-1.5:LRA=11[o]"
subprocess.run(cmd + ["-filter_complex", fc, "-map", "[o]", "-ar", "48000", os.path.join(AQUI, "renders", "voz.wav")], check=True)
json.dump({"estimado": False, "palabras": out, "total": total}, open(os.path.join(AQUI, "palabras.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
print("total", total)
