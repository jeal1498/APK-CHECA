"""Tiempos ESTIMADOS por palabra (borrador sin la voz). Con la grabación real se reemplaza palabras.json
por la transcripción de Whisper (mismo formato: [[palabra, ini, fin], ...]). Uso: python3 -I estimar_tiempos.py"""
import json, os
FRASES = ["Puede que estés dejando dinero sin darte cuenta.",
          "Cuando te llega una oferta, no te fijes solo en cuánto paga.",
          "Ten en cuenta los kilómetros, el tiempo y la gasolina que vas a gastar.",
          "Un pago alto no siempre significa que te va a quedar más dinero.",
          "¿Qué dato tomas en cuenta antes de aceptar?"]
t, out = 0.25, []
for f in FRASES:
    for w in f.split():
        d = 0.17 + 0.045 * len(w.strip("¿?.,"))
        out.append([w, round(t, 2), round(t + d, 2)])
        t += d + (0.22 if w.endswith(",") else 0.03)
    t += 0.45
json.dump({"estimado": True, "palabras": out, "total": round(t + 1.2, 1)},
          open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "palabras.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
print("total", round(t + 1.2, 1))
