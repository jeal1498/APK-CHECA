"""ElevenLabs para los videos de Checa: voz con tiempos, música y efectos.

La clave se lee de la variable ELEVENLABS_API_KEY o del archivo indicado en ELEVENLABS_KEY_FILE
(por defecto /tmp/claude-0/.el_key). Nunca se guarda en el repositorio.

Uso:
  python3 -I scripts/elevenlabs.py voz   <carpeta> <archivo> "<texto>"   [VOZ_ID] [velocidad]
  python3 -I scripts/elevenlabs.py musica <carpeta> <archivo> "<prompt>" <segundos>
  python3 -I scripts/elevenlabs.py sfx    <carpeta> <archivo> "<texto>"  <segundos>
"""
import base64, json, os, ssl, sys, urllib.request

VOZ = "dlGxemPxFMTY7iXagmOj"  # Fernando Martínez, voz del narrador elegida por Eduardo
CA = "/root/.ccr/ca-bundle.crt"


def clave():
    k = os.environ.get("ELEVENLABS_API_KEY")
    if k:
        return k.strip()
    return open(os.environ.get("ELEVENLABS_KEY_FILE", "/tmp/claude-0/.el_key")).read().strip()


def post(ruta, cuerpo):
    req = urllib.request.Request("https://api.elevenlabs.io" + ruta, data=json.dumps(cuerpo).encode(),
                                 headers={"xi-api-key": clave(), "Content-Type": "application/json"})
    ctx = ssl.create_default_context(cafile=CA) if os.path.exists(CA) else None
    return urllib.request.urlopen(req, context=ctx).read()


def main():
    modo, carpeta, archivo, texto = sys.argv[1:5]
    os.makedirs(carpeta, exist_ok=True)
    base = os.path.join(carpeta, archivo)
    if modo == "voz":
        voz = sys.argv[5] if len(sys.argv) > 5 else VOZ
        vel = float(sys.argv[6]) if len(sys.argv) > 6 else 1.15
        d = json.loads(post(f"/v1/text-to-speech/{voz}/with-timestamps?output_format=mp3_44100_128", {
            "text": texto, "model_id": "eleven_multilingual_v2", "language_code": "es",
            "voice_settings": {"stability": 0.4, "similarity_boost": 0.8, "style": 0.35, "speed": vel}}))
        open(base + ".mp3", "wb").write(base64.b64decode(d["audio_base64"]))
        a = d["alignment"]
        json.dump({"c": a["characters"], "s": a["character_start_times_seconds"], "e": a["character_end_times_seconds"]},
                  open(base + ".json", "w"), ensure_ascii=False)
        print(archivo, round(a["character_end_times_seconds"][-1], 2), "s")
    elif modo == "musica":
        seg = float(sys.argv[5])
        open(base + ".mp3", "wb").write(post("/v1/music", {"prompt": texto, "music_length_ms": int(seg * 1000), "force_instrumental": True}))
        print(archivo, "música", seg, "s")
    elif modo == "sfx":
        seg = float(sys.argv[5])
        open(base + ".mp3", "wb").write(post("/v1/sound-generation", {"text": texto, "duration_seconds": seg}))
        print(archivo, "efecto", seg, "s")
    else:
        sys.exit("modo: voz | musica | sfx")


main()
