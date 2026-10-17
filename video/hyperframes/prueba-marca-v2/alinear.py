"""Pone el texto exacto de SCRIPT.md en las palabras de audio_meta.json usando los tiempos de
transcripcion/NN/transcript.json (whisper small, español). Correr después de audio.mjs / fetch-sfx."""
import json, difflib, re
m = json.load(open("audio_meta.json"))
lines = [l.strip() for l in open("SCRIPT.md") if l.startswith("    ")]
norm = lambda s: re.sub(r"[^\wáéíóúñü]", "", s.lower())
for v in m["voices"]:
    n = v["frame"]; s = lines[n - 1].split()
    t = json.load(open(f"transcripcion/{n:02d}/transcript.json"))
    w = t if isinstance(t, list) else t.get("words", t)
    w = [{"text": x.get("text", x.get("word", "")), "start": x["start"], "end": x["end"]} for x in w]
    a = [norm(x["text"]) for x in w]; b = [norm(x) for x in s]
    out = [None] * len(s)
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, a, b).get_opcodes():
        if tag == "equal" or (i2 - i1 == j2 - j1):
            for k in range(j2 - j1): out[j1 + k] = (w[i1 + k]["start"], w[i1 + k]["end"])
        elif i2 > i1:
            st, en = w[i1]["start"], w[i2 - 1]["end"]; L = sum(len(x) for x in s[j1:j2]); c = st
            for k in range(j1, j2):
                d = (en - st) * len(s[k]) / L; out[k] = (c, c + d); c += d
    for k in range(len(s)):
        if out[k] is None:
            p = next((out[j][1] for j in range(k - 1, -1, -1) if out[j]), 0.0)
            q = next((out[j][0] for j in range(k + 1, len(s)) if out[j]), v["duration_s"])
            out[k] = (p, q)
    v["words"] = [{"id": f"w{k}", "text": s[k], "start": round(out[k][0], 3), "end": round(out[k][1], 3)} for k in range(len(s))]
json.dump(m, open("audio_meta.json", "w"), ensure_ascii=False, indent=2)
print("ok", len(m["voices"]), "líneas")
