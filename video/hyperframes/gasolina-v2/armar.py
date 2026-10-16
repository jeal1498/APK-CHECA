"""Voz de Fernando por línea (ElevenLabs con tiempos), wav recortado, audio_meta.json y STORYBOARD.md.
Uso: python3 -I armar.py   (no vuelve a pedir voz si ya existe assets/voice/NN.mp3)"""
import json, os, subprocess
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__) or ".", "..", "..", ".."))
EL = os.path.join(ROOT, "video", "scripts", "elevenlabs.py")
LINES = [l.strip() for l in open("SCRIPT.md", encoding="utf-8") if l.startswith("    ")]
PAD, PAD_FIN, LEAD = 0.15, 2.0, 0.25
os.makedirs("assets/voice", exist_ok=True)
V = []
for n, txt in enumerate(LINES, 1):
    b = f"assets/voice/{n:02d}"
    if not os.path.exists(b + ".mp3"):
        subprocess.run(["python3", "-I", EL, "voz", "assets/voice", f"{n:02d}", txt, "dlGxemPxFMTY7iXagmOj", "1.0"], check=True)
    a = json.load(open(b + ".json"))
    ch, st, en = a["c"], a["s"], a["e"]
    words, cur, s0, e0 = [], "", None, None
    for c, s, e in zip(ch, st, en):
        if c.isspace():
            if cur: words.append((cur, s0, e0)); cur, s0 = "", None
        else:
            if s0 is None: s0 = s
            cur += c; e0 = e
    if cur: words.append((cur, s0, e0))
    dur = round(en[-1] + 0.12, 3)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", b + ".mp3", "-t", str(dur), "-ar", "44100", "-ac", "1", b + ".wav"], check=True)
    dur = round(float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", b + ".wav"]).strip()), 3)
    V.append({"frame": n, "path": b + ".wav", "duration_s": dur,
              "words": [{"id": f"w{k}", "text": w, "start": round(s, 3), "end": round(e, 3)} for k, (w, s, e) in enumerate(words)]})
W = {v["frame"]: v["words"] for v in V}
def at(n, palabra, k=0, off=-LEAD):
    ws = [w for w in W[n] if w["text"].lower().strip("¿¡.,:").startswith(palabra.lower())]
    return round(max(0.0, ws[k]["start"] + off), 2)
SFX = [  # (frame, archivo, segundo dentro de la escena, volumen)
    (1, "impact-bass-1", 0.0, .5), (1, "error", at(1, "reserva"), .25),
    (2, "click-soft", at(2, "dos"), .4), (2, "ping", at(2, "pura"), .3),
    (3, "whoosh-short", 0.0, .3), (3, "pop", at(3, "noventa"), .35),
    (4, "glitch-1", at(4, "lleva"), .3), (4, "impact-bass-1", at(4, "treinta"), .4),
    (5, "ping", 0.1, .3),
    (6, "whoosh-short", at(6, "fue"), .3), (6, "error", at(6, "tanque"), .25),
    (7, "whoosh-short", at(7, "hace"), .3), (7, "pop", at(7, "resta"), .4),
    (8, "chime", at(8, "ganancia", 0, -0.1), .4),
    (9, "pop", 0.0, .3), (9, "chime", at(9, "checa", 0, -0.1), .4), (9, "sparkle", at(9, "queda", 0, -0.1), .3),
]
def fdur(f):
    return round(float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f"assets/sfx/{f}.mp3"]).strip()), 2)
meta = {"bgm": {"path": "assets/audio/bgm.mp3", "source": "host", "volume": 0.12}, "voices": V,
        "sfx": [{"frame": n, "file": f"assets/sfx/{f}.mp3", "offset_s": o, "duration_s": fdur(f), "volume": vol} for n, f, o, vol in SFX]}
json.dump(meta, open("audio_meta.json", "w"), ensure_ascii=False, indent=2)
TRANS = {1: "cut", 2: "cut", 3: "cut", 4: "cut", 5: "cut", 6: "cut", 7: "crossfade", 8: "cut", 9: "crossfade"}
NOM = {1: "Medidor", 2: "Costo por km", 3: "Oferta de $90", 4: "La gasolina se lleva $30", 5: "Te quedan $60", 6: "Al tanque", 7: "La app resta", 8: "Escudo", 9: "Cierre estándar"}
tecnica = {1: "Hook Inicial", 2: "Micro-hook", 3: "Open Loop", 4: "Giro", 5: "Pausa dramática", 6: "Giro emocional", 7: "Re-enganche visual", 8: "Beneficio", 9: "Cierre estándar"}
D = {n: round(V[n - 1]["duration_s"] + (PAD_FIN if n == len(V) else PAD), 3) for n in range(1, len(V) + 1)}
tot = round(sum(D.values()), 2)
nom = {f"{n:02d}": f"{n:02d}-{k}" for n, k in {1: "medidor", 2: "km", 3: "oferta", 4: "pierde", 5: "sesenta", 6: "tanque", 7: "app", 8: "escudo", 9: "cierre"}.items()}
sb = f"""---
format: 1080x1920
duration: {tot}s
message: "La gasolina se come tu ganancia: Checa hace la resta antes de que aceptes"
audience: conductores de Uber y DiDi (cuenta personal de Eduardo)
mode: autonomous
music: none
---

## Video direction

Estilo Checa (negro OLED, verde #22C55E, amarillo #FACC15, rojo #F43F5E, Fira Sans / Fira Code). Sin logo de Checa (cuenta personal). Texto grande, máscara y sincronía con la voz.

"""
for n in range(1, len(V) + 1):
    sb += f"""## Frame {n} — {NOM[n]}

- duration: {D[n]}s
- voiceover: "{LINES[n-1]}"
- transition_in: {TRANS[n]}
- status: animated
- src: compositions/frames/{nom[f'{n:02d}']}.html
- sfx: {", ".join(sorted({s['file'].split('/')[-1][:-4] for s in meta['sfx'] if s['frame'] == n}))}

[{tecnica[n]}]

"""
open("STORYBOARD.md", "w", encoding="utf-8").write(sb)
print("ok", tot, "s", D)
