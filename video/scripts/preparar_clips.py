# Recorta silencios, pasa a 1080x1920/30 fps, iguala volumen y genera lineasBeta.ts.
# Uso: python3 -I scripts/preparar_clips.py <carpeta_clips> <palabras.json>  (edita CLIPS con nombre y texto exacto de cada clip)
import json, subprocess, sys
U = sys.argv[1].rstrip("/") + "/"  # carpeta con los clips originales
P = json.load(open(sys.argv[2]))  # palabras.json de transcribir.py
OUT = "/home/user/Mi-Ruta/video/public/beta/"
CLIPS = [
 ("3528a320-HnVideoEditor_2026_10_13_120323872", "Aceptas un viaje a ciegas y cuando te das cuenta, ya perdiste dinero."),
 ("e31d288b-VID_20261013_1206192", "Por eso, hice una app para que esto no nos vuelva a pasar."),
 ("142b3775-VID_20261013_121702", "Miren cómo funciona. Caen las ofertas en las aplicaciones y Checa te dice si te conviene o no te conviene. Por ejemplo, ese viaje de Uber sí nos convenía."),
 ("56209412-VID_20261013_1229572", "Te da el pago exacto por kilómetro y si te conviene o no."),
 ("c3be608f-VID_20261013_123243", "Busco 50 conductores que la utilicen sin costo por tres meses."),
 ("beaf5ec0-VID_20261013_1233452", "Y si la compartes con un amigo, les doy un mes gratis a cada uno."),
 ("e479997d-VID_20261013_1234192", "Coméntame la palabra yo aquí abajo o mándame un mensaje privado y te paso los accesos."),
]
lineas = []
for k, (n, texto) in enumerate(CLIPS, 1):
    w = P[n + ".wav"]
    dur = float(subprocess.check_output(["ffprobe","-v","error","-show_entries","format=duration","-of","csv=p=0",U+n+".mp4"]))
    a = max(0, w[0]["s"] - 0.12); b = min(dur, w[-1]["e"] + 0.25)
    subprocess.run(["ffmpeg","-loglevel","error","-y","-ss",f"{a:.2f}","-to",f"{b:.2f}","-i",U+n+".mp4",
      "-vf","scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,fps=30,setsar=1",
      "-af","loudnorm=I=-14:TP=-1.5:LRA=7,aresample=48000","-ac","2","-c:v","libx264","-crf","20","-preset","veryfast","-c:a","aac","-b:a","160k",OUT+f"c{k}.mp4"], check=True)
    sw = texto.split(); nw = len(w)
    pal = []
    for j, t in enumerate(sw):
        idx = round(j * (nw - 1) / max(1, len(sw) - 1))
        pal.append({"w": t, "s": round(max(0, w[idx]["s"] - a), 3)})
    lineas.append({"archivo": f"c{k}", "dur": round(b - a, 3), "palabras": pal})
    print(k, round(b - a, 2))
with open("/home/user/Mi-Ruta/video/src/Checa/lineasBeta.ts", "w") as f:
    f.write("// Clips de Eduardo (campaña beta), recortados; tiempos de palabra aproximados de faster-whisper\n")
    f.write("export const CLIPS: { archivo: string; dur: number; palabras: { w: string; s: number }[] }[] = " + json.dumps(lineas, ensure_ascii=False) + ";\n")
