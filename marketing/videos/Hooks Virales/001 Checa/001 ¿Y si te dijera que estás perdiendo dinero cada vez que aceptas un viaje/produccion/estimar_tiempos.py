"""Tiempos ESTIMADOS por palabra (borrador sin la voz). Uso: python3 -I estimar_tiempos.py"""
import json, os
FRASES = ["¿Y si te dijera que algunos viajes te dejan mucho menos dinero del que piensas?",
          "Cuando recibes una oferta, no basta con mirar cuánto vas a cobrar.",
          "También cuentan los kilómetros, el tiempo y la gasolina.",
          "Con Checa puedes ver datos clave de cada oferta antes de tomar una decisión.",
          "Porque no se trata de aceptar más viajes. Se trata de elegir mejor.",
          "Tu trabajo vale. Asegúrate de que cada viaje también."]
t, out = 0.25, []
for f in FRASES:
    for w in f.split():
        d = 0.15 + 0.04 * len(w.strip("¿?.,"))
        out.append([w, round(t, 2), round(t + d, 2)])
        t += d + (0.22 if w[-1] in ",." else 0.03)
    t += 0.3
json.dump({"estimado": True, "palabras": out, "total": round(t + 1.4, 1)},
          open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "palabras.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=0)
print("total", round(t + 1.4, 1))
