# Transcribe con faster-whisper (modelo small) los .wav de una carpeta y guarda palabras.json con tiempos.
# Uso: pip install faster-whisper; ffmpeg -i clip.mp4 -ac 1 -ar 16000 clip.wav; python3 -I scripts/transcribir.py <carpeta>
import sys, json, glob, os
from faster_whisper import WhisperModel
m = WhisperModel("small", device="cpu", compute_type="int8")
out = {}
for f in sorted(glob.glob(sys.argv[1] + "/*.wav")):
    import wave, numpy as np
    w_ = wave.open(f); a = np.frombuffer(w_.readframes(w_.getnframes()), dtype=np.int16).astype(np.float32) / 32768
    segs, _ = m.transcribe(a, language="es", word_timestamps=True, vad_filter=False)
    words = [{"w": w.word.strip(), "s": round(w.start, 2), "e": round(w.end, 2)} for s in segs for w in s.words]
    out[os.path.basename(f)] = words
    print(os.path.basename(f), " ".join(x["w"] for x in words), "|", words[0]["s"] if words else None, words[-1]["e"] if words else None)
json.dump(out, open(sys.argv[1] + "/palabras.json", "w"), ensure_ascii=False)
