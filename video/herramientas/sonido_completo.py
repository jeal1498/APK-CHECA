"""Sonido del video EA-Completo-001 (identidad PERSONAL, 25 nov 2026).
Música «pulso suave» (100 bpm: golpe grave por tiempo, chasquido fino a contratiempo) bajita,
más un efecto por golpe visual (gota, palabra clave, ✓ que cae, cambio de escena), nunca dos a la vez.
Uso: python3 -I sonido_completo.py salida.wav  (48 kHz, mono). Los fotogramas salen de Completo.tsx y Final.tsx.
"""
import sys, wave
import numpy as np

SR = 48000
FPS = 30
DUR = 790 / FPS
rng = np.random.default_rng(7)


def env(n, ataque=0.005, caida=None):
    t = np.arange(n) / SR
    a = np.clip(t / ataque, 0, 1)
    d = np.exp(-t / (caida or (n / SR / 4)))
    return a * d


def tono(f0, f1, dur, forma="sine", vol=0.3):
    n = int(SR * dur)
    t = np.arange(n) / SR
    f = f0 * (f1 / f0) ** (t / dur)
    fase = 2 * np.pi * np.cumsum(f) / SR
    if forma == "sine":
        x = np.sin(fase)
    elif forma == "tri":
        x = 2 / np.pi * np.arcsin(np.sin(fase))
    else:
        x = np.sign(np.sin(fase))
    return vol * x * env(n)


def ruido(dur, vol=0.2, lo=400, hi=4000):
    n = int(SR * dur)
    x = rng.standard_normal(n)
    # filtro pasabanda sencillo que barre de lo a hi (dos polos)
    y = np.zeros(n)
    a1 = a2 = 0.0
    for i in range(n):
        fc = lo * (hi / lo) ** (i / n)
        w = 2 * np.pi * fc / SR
        r = 0.9
        b = (1 - r)
        yi = b * x[i] + 2 * r * np.cos(w) * a1 - r * r * a2
        a2, a1 = a1, yi
        y[i] = yi
    y /= np.max(np.abs(y)) + 1e-9
    t = np.arange(n) / SR
    forma = np.sin(np.pi * t / dur) ** 2
    return vol * y * forma


EFECTOS = {
    "gota": lambda: tono(1100, 320, 0.16, "sine", 0.32),
    "palabra": lambda: tono(1800, 1500, 0.06, "tri", 0.16),
    "paloma": lambda: tono(160, 60, 0.24, "sine", 0.55) + np.pad(tono(1400, 1400, 0.06, "tri", 0.12), (0, int(SR * 0.18))),
    "barrido": lambda: ruido(0.32, 0.20),
}

# (fotograma, efecto). Un efecto por golpe; las gotas que chocan con otro golpe se omiten.
EVENTOS = [
    (57, "palabra"), (102, "paloma"), (129, "palabra"), (158, "barrido"), (206, "palabra"),
    (256, "barrido"), (337, "palabra"), (386, "barrido"), (408, "barrido"), (447, "barrido"),
    (519, "palabra"), (585, "palabra"),
    (640 + 8, "barrido"), (640 + 33, "palabra"), (640 + 64, "paloma"), (640 + 108, "barrido"),
]
GOTAS = [61, 84, 100, 116, 130, 142, 152, 161, 169, 176]
ocupados = [f for f, _ in EVENTOS]
for g in GOTAS:
    if all(abs(g - o) > 3 for o in ocupados):
        EVENTOS.append((g, "gota"))

n = int(SR * DUR)
mix = np.zeros(n + SR)

# Música: pulso suave a 100 bpm (0.6 s por tiempo)
musica = np.zeros_like(mix)
paso = 0.6
i = 0
t = 0.0
while t < DUR:
    k = int(t * SR)
    golpe = tono(110, 45, 0.25, "sine", 0.5)
    musica[k:k + len(golpe)] += golpe
    if i % 2:
        h = ruido(0.05, 0.08, 6000, 8000)
        musica[k:k + len(h)] += h
    if i % 4 == 2:
        c = tono(330, 330, 0.12, "tri", 0.06)
        musica[k:k + len(c)] += c
    i += 1
    t += paso
musica *= 10 ** (-20 / 20) / (np.max(np.abs(musica)) + 1e-9)  # pico a −20 dBFS

efectos = np.zeros_like(mix)
for fr, nombre in EVENTOS:
    x = EFECTOS[nombre]()
    k = int(fr / FPS * SR)
    efectos[k:k + len(x)] += x
efectos *= 10 ** (-7 / 20) / (np.max(np.abs(efectos)) + 1e-9)  # pico a −7 dBFS

out = (musica + efectos)[:n]
# entrada y salida de 10 ms para que no truene en el bucle
r = int(SR * 0.01)
out[:r] *= np.linspace(0, 1, r)
out[-r:] *= np.linspace(1, 0, r)
pcm = np.int16(np.clip(out, -1, 1) * 32767)
with wave.open(sys.argv[1], "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
print("ok", len(EVENTOS), "efectos", round(DUR, 2), "s")
