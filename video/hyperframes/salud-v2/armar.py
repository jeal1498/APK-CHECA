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
    (1, "impact-bass-1", 0.0, .55), (1, "glitch-1", at(1, "ya", 0, -0.1), .22),
    (2, "whoosh-short", 0.0, .35), (2, "click-soft", at(2, "servicio", 0, 0.0), .4), (2, "error", at(2, "casi", 0, 0.0), .22),
    (3, "pop", at(3, "hábitos"), .4), (3, "ping", at(3, "cuarto"), .45), (3, "impact-bass-1", at(3, "accidente", 0, -0.15), .5),
    (4, "click-soft", 0.0, .4), (4, "ping", at(4, "cinco"), .35),
    (5, "pop", at(5, "agua"), .4), (5, "whoosh-short", at(5, "ves"), .3),
    (6, "click", at(6, "pega"), .5), (6, "chime", at(6, "arrancar", 0, -0.1), .4),
    (7, "impact-bass-1", 0.0, .6),
    (8, "glitch-1", at(8, "cierran"), .3), (8, "impact-bass-1", at(8, "orilla"), .4),
    (9, "whoosh-short", at(9, "vale"), .3), (9, "chime", at(9, "casa", 0, -0.1), .4),
    (10, "pop", 0.0, .35), (10, "sparkle", at(10, "sígueme"), .4),
    (11, "pop", 0.0, .3), (11, "chime", at(11, "checa", 0, -0.1), .4), (11, "sparkle", at(11, "queda", 0, -0.1), .3),
]
def fdur(f):
    return round(float(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f"assets/sfx/{f}.mp3"]).strip()), 2)
meta = {"bgm": {"path": "assets/audio/bgm.mp3", "source": "host", "volume": 0.12}, "voices": V,
        "sfx": [{"frame": n, "file": f"assets/sfx/{f}.mp3", "offset_s": o, "duration_s": fdur(f), "volume": vol} for n, f, o, vol in SFX]}
json.dump(meta, open("audio_meta.json", "w"), ensure_ascii=False, indent=2)
TRANS = {1: "cut", 2: "crossfade", 3: "crossfade", 4: "cut", 5: "cut", 6: "cut", 7: "cut", 8: "cut", 9: "crossfade", 10: "crossfade", 11: "crossfade"}
NOM = {1: "Seis de la tarde", 2: "Servicio", 3: "Cuatro hábitos", 4: "Uno · caminar", 5: "Dos · agua", 6: "Tres · respaldo", 7: "Cuatro", 8: "Orilla y descansa", 9: "A casa", 10: "Sígueme", 11: "Cierre estándar"}
tecnica = {1: "Hook Inicial", 2: "Micro-hook (contraste)", 3: "Open Loop", 4: "Re-enganche visual", 5: "Giro", 6: "Cambio de ritmo", 7: "Pausa dramática", 8: "Payoff + re-enganche", 9: "Giro emocional", 10: "CTA específico", 11: "CTA estándar"}
D = {n: round(V[n - 1]["duration_s"] + (PAD_FIN if n == len(V) else PAD), 3) for n in range(1, len(V) + 1)}
tot = round(sum(D.values()), 2)
nom = {f"{n:02d}": f"{n:02d}-{k}" for n, k in {1: "tarde", 2: "servicio", 3: "habitos", 4: "uno", 5: "dos", 6: "tres", 7: "cuatro", 8: "orilla", 9: "casa", 10: "sigueme", 11: "cierre"}.items()}
sb = f"""---
format: 1080x1920
duration: {tot}s
message: "Cuatro hábitos para aguantar el turno sin acabar con la espalda"
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
