"""Herramienta única para producir los videos de Checa (skill video-checa).

Uso: python3 -I video/scripts/checa.py <comando> ...

  nuevo <nombre> [--desde <proyecto>]     crea video/hyperframes/<nombre>/ (solo SCRIPT.md, escenas.json, portada/portada.json)
  construir <proyecto> [--check|--render]  copia la base (_base) y corre el motor compartido construir.py
  portada <proyecto>                       portada/portada.json → portada/portada.jpg (revisa que el sello no se delate)
  revisar <video.mp4> [hoja.png]           ffprobe (yuv420p + High) y hoja de 10 cuadros
  entregar <proyecto> <CHECA|PERSONAL> <NN> portada como primer cuadro → marketing/videos/.../video.mp4 y portada.jpg
  dm <carpeta>                             escribe dm.txt con su utm
  tabla <guion.md> [--guardar]             tabla de animaciones a partir del «Guión Desglosado»
  hecho <carpeta> <AAAA-MM-DD> [tipo]      registra el video «listo» en el calendario web y en calendario.md
  publicado <carpeta> <AAAA-MM-DD>         lo marca «Publicado» en los dos calendarios
  calidad <carpeta> [proyecto] [--camara]  control de calidad automático de un video terminado (falla si algo no cumple)
  subir "<mensaje>"                        commit y push (main y rama actual) en APK-CHECA y PWA-Checa-Multimedia

<proyecto> es un nombre (video/hyperframes/<nombre>) o una ruta. <carpeta> es marketing/videos/CHECA|PERSONAL/NN - Nombre.
"""
import json, os, re, shutil, subprocess, sys, unicodedata
from datetime import date

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
BASE = os.path.join(ROOT, "video", "hyperframes", "_base")
PWA = os.path.join(os.path.dirname(ROOT), "PWA-Checa-Multimedia")
CAL_WEB = os.path.join(PWA, "calendario-d4f81c")
CAL_MD = os.path.join(ROOT, "marketing", "plan", "calendario.md")

# Lo que se copia de la base en cada `construir` (git lo ignora dentro del proyecto).
SYNC = ["assets/checa-popup.html", "assets/logo-checa.svg", "assets/fonts", "assets/sfx", "compositions/captions.html",
        "hyperframes.json", "frame.md", "alinear.py"]
GITIGNORE = SYNC + ["compositions/", "index.html", "package.json", "meta.json", "portada/portada.jpg", "portada/_portada.html",
                    "renders/", ".hyperframes/", "node_modules/"]
DIAS = ["lun", "mar", "mié", "jue", "vie", "sáb", "dom"]
MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"]


def fallar(msg):
    sys.exit("ERROR: " + msg)


def proyecto(p):
    d = p if os.path.isdir(p) else os.path.join(ROOT, "video", "hyperframes", p)
    if not os.path.isdir(d):
        fallar(f"no existe el proyecto {p}")
    return os.path.abspath(d)


def carpeta(p):
    d = p if os.path.isabs(p) else os.path.join(ROOT, p)
    if not os.path.isdir(d):
        fallar(f"no existe la carpeta {p}")
    cuenta = os.path.basename(os.path.dirname(d))
    m = re.match(r"(\d+) - (.+)", os.path.basename(d))
    if cuenta not in ("CHECA", "PERSONAL") or not m:
        fallar("la carpeta debe ser marketing/videos/CHECA|PERSONAL/NN - Nombre")
    return d, cuenta, m.group(1), m.group(2)


def buscar_carpeta(cuenta, nn):
    raiz = os.path.join(ROOT, "marketing", "videos", cuenta)
    hay = [x for x in os.listdir(raiz) if x.startswith(f"{nn} - ")]
    if len(hay) != 1:
        fallar(f"esperaba una carpeta {cuenta}/{nn} - …, encontré {hay}")
    return os.path.join(raiz, hay[0])


def slug(texto):
    t = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")


def fecha_bonita(f):
    d = date.fromisoformat(f)
    return f"{DIAS[d.weekday()]} {d.day} {MESES[d.month - 1]}"


# ---------- proyecto ----------
def cmd_nuevo(args):
    if not args:
        fallar("falta el nombre")
    nombre = args[0]
    d = os.path.join(ROOT, "video", "hyperframes", nombre)
    if os.path.exists(d):
        fallar(f"ya existe {d}")
    os.makedirs(os.path.join(d, "portada"))
    os.makedirs(os.path.join(d, "assets", "voice"))
    if "--desde" in args:
        o = proyecto(args[args.index("--desde") + 1])
        for f in ["SCRIPT.md", "escenas.json", "portada/portada.json"]:
            if os.path.exists(os.path.join(o, f)):
                shutil.copy(os.path.join(o, f), os.path.join(d, f))
    else:
        open(os.path.join(d, "SCRIPT.md"), "w", encoding="utf-8").write(f"# SCRIPT — {nombre}\n\n    (una línea de voz por escena, con 4 espacios; montos sin moneda)\n")
        json.dump([{"tipo": "cierre", "pil": "Comenta CHECA", "link": False}], open(os.path.join(d, "escenas.json"), "w"), ensure_ascii=False, indent=1)
        json.dump({"etiqueta": "OFERTA DE 110", "titulo": ["Te ofrecen 110", "y te quedan 54"], "precio": "$110.00", "linea": "28 km en total",
                   "recogida": "A 6 min (3 km)", "viaje": "Viaje: 40 min (25 km)", "sello_texto": "CADA KM VALE", "sello_valor": "3.93"},
                  open(os.path.join(d, "portada", "portada.json"), "w"), ensure_ascii=False, indent=1)
    open(os.path.join(d, ".gitignore"), "w").write("\n".join(GITIGNORE) + "\n")
    print(f"listo: {d}")


def sincronizar(d):
    for rel in SYNC:
        o, t = os.path.join(BASE, rel), os.path.join(d, rel)
        os.makedirs(os.path.dirname(t), exist_ok=True)
        if os.path.isdir(o):
            shutil.copytree(o, t, dirs_exist_ok=True)
        else:
            shutil.copy(o, t)
    nombre = os.path.basename(d)
    json.dump({"id": nombre, "name": nombre, "createdAt": "2026-10-30T00:00:00.000Z"}, open(os.path.join(d, "meta.json"), "w"), indent=2)
    pk = {"name": nombre, "private": True, "type": "module",
          "scripts": {c: f"npx --yes hyperframes@0.8.154 {c if c != 'dev' else 'preview'}" for c in ["dev", "check", "render", "publish"]}}
    json.dump(pk, open(os.path.join(d, "package.json"), "w"), indent=2)
    gi = os.path.join(d, ".gitignore")
    if not os.path.exists(gi):
        open(gi, "w").write("\n".join(GITIGNORE) + "\n")


def cmd_construir(args):
    d = proyecto(args[0])
    sincronizar(d)
    r = subprocess.run(["python3", "-I", os.path.join(BASE, "construir.py"), d] + args[1:])
    sys.exit(r.returncode)


def cmd_portada(args):
    d = proyecto(args[0])
    c = json.load(open(os.path.join(d, "portada", "portada.json"), encoding="utf-8"))
    titulo = c["titulo"] if isinstance(c["titulo"], list) else [c["titulo"]]
    largo = max(len(x) for x in titulo)
    px = c.get("titulo_px") or (164 if largo <= 11 else 128 if largo <= 14 else 110)
    valor = str(c["sello_valor"])
    visibles = " ".join([c["etiqueta"], " ".join(titulo), c["precio"], c["linea"], c["recogida"], c["viaje"]])
    solo = lambda s: re.sub(r"[^0-9]", "", s)
    if re.search(r"(?<![0-9.])" + re.escape(valor) + r"(?![0-9])", visibles) or (len(solo(valor)) >= 3 and solo(valor) in solo(visibles)):
        fallar(f"la cifra del sello ({valor}) ya aparece en la portada; el sello debe esconder otra")
    borroso = c.get("sello_borroso") or "$" + re.sub(r"[0-9]", "▒", valor)
    h = open(os.path.join(BASE, "portada", "portada.html"), encoding="utf-8").read()
    rep = {"titulo_px": str(px), "titulo_lh": ".92" if px >= 150 else "1.0", "etiqueta": c["etiqueta"], "titulo": "<br>".join(titulo),
           "precio": c["precio"], "linea": c["linea"], "recogida": c["recogida"], "viaje": c["viaje"],
           "sello_texto": c["sello_texto"], "sello_borroso": borroso, "fuentes": "file://" + os.path.join(BASE, "assets", "fonts")}
    for k, v in rep.items():
        h = h.replace("{{" + k + "}}", v)
    tmp = os.path.join(d, "portada", "_portada.html")
    open(tmp, "w", encoding="utf-8").write(h)
    png = os.path.join(d, "renders", "portada.png")
    os.makedirs(os.path.dirname(png), exist_ok=True)
    env = dict(os.environ, NODE_PATH=subprocess.check_output(["npm", "root", "-g"]).decode().strip())
    subprocess.run(["node", os.path.join(BASE, "portada", "r.js"), tmp, png], check=True, env=env)
    jpg = os.path.join(d, "portada", "portada.jpg")
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", png, "-q:v", "3", jpg], check=True)
    print(f"listo: {jpg} (título {px} px)")


# ---------- revisión y entrega ----------
def cmd_revisar(args):
    mp4 = args[0]
    info = json.loads(subprocess.check_output(["ffprobe", "-v", "error", "-show_entries", "stream=codec_type,pix_fmt,profile,width,height:format=duration", "-of", "json", mp4]))
    v = next(s for s in info["streams"] if s["codec_type"] == "video")
    dur = float(info["format"]["duration"])
    print(f"{mp4}: {dur:.1f} s, {v['width']}x{v['height']}, {v['pix_fmt']}, {v.get('profile')}")
    hoja = args[1] if len(args) > 1 else os.path.join(os.path.dirname(os.path.abspath(mp4)), "hoja.png")
    cuadros = []
    for i in range(10):
        t = round(0.3 + i * (dur - 0.6) / 9, 2)
        f = f"{hoja}.{i}.png"
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-ss", str(t), "-i", mp4, "-frames:v", "1", "-vf", "scale=240:-1", f], check=True)
        cuadros.append(f)
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y"] + sum([["-i", f] for f in cuadros], []) + ["-filter_complex", "hstack=inputs=10", hoja], check=True)
    for f in cuadros:
        os.remove(f)
    print(f"hoja: {hoja}")
    if v["pix_fmt"] != "yuv420p" or v.get("profile") != "High":
        fallar("el mp4 debe ser yuv420p con perfil High (el teléfono de Eduardo no reproduce otro)")


def cmd_entregar(args):
    d, cuenta, nn = proyecto(args[0]), args[1], args[2]
    dest = buscar_carpeta(cuenta, nn)
    video, portada = os.path.join(d, "renders", "video.mp4"), os.path.join(d, "portada", "portada.jpg")
    for f in (video, portada):
        if not os.path.exists(f):
            fallar(f"falta {f}")
    salida = os.path.join(dest, "video.mp4")
    subprocess.run(["bash", os.path.join(ROOT, "video", "scripts", "portada_primer_cuadro.sh"), video, portada, salida], check=True)
    shutil.copy(portada, os.path.join(dest, "portada.jpg"))
    cmd_revisar([salida, os.path.join(d, "renders", "hoja-final.png")])
    print(f"listo: {salida}")


def cmd_dm(args):
    d, cuenta, nn, nombre = carpeta(args[0])
    fuente = "tiktok" if cuenta == "CHECA" else "tiktok-personal"
    hola = "¡Hola! 👋 Gracias por comentar." if cuenta == "CHECA" else "¡Hola! 👋 Soy Eduardo, gracias por comentar."
    texto = (f"{hola} Aquí está tu lugar para probar Checa en Google Play:\n\n"
             f"1️⃣ Regístrate con tu Gmail (el de tu Play Store):\n"
             f"https://jeal1498.github.io/checa-web/registro/?utm_source={fuente}&utm_campaign={slug(nombre)}\n\n"
             f"2️⃣ Al terminar te lleva a nuestra comunidad de WhatsApp. Entra: ahí te aviso cuando tu acceso esté habilitado y te paso el enlace para descargar.\n\n"
             f"📲 Solo Android.\n")
    open(os.path.join(d, "dm.txt"), "w", encoding="utf-8").write(texto)
    print(texto)


def cmd_tabla(args):
    g = open(args[0], encoding="utf-8").read()
    filas = re.findall(r"^- `(\d+:\d\d) - (\d+:\d\d)` \| Audio: (.*?) \| (?:Remotion / )?Visual[^:]*: \[(.*?)\]", g, re.M)
    if not filas:
        fallar("no encontré líneas del «Guión Desglosado»")
    t = "| Toma | Frase | Animación |\n|---|---|---|\n" + "".join(f"| {a}–{b} | {au.strip()} | {vi.strip()} |\n" for a, b, au, vi in filas)
    print(t)
    if "--guardar" in args:
        open(args[0], "a", encoding="utf-8").write("\n## Tabla de animaciones por toma (sacada del desglose)\n" + t)


# ---------- calendarios ----------
def fila_md(cuenta, nn, nombre, nueva, viejo_re=None):
    s = open(CAL_MD, encoding="utf-8").read().split("\n")
    ini = next(i for i, l in enumerate(s) if l.startswith("# " + cuenta))
    fin = next((i for i in range(ini + 1, len(s)) if s[i].startswith("# ")), len(s))
    for i in range(ini, fin):
        if s[i].startswith(f"| {nn} |") and f"`{nn} - {nombre}`" in s[i]:
            s[i] = nueva(s[i])
            break
    else:
        ult = max(i for i in range(ini, fin) if s[i].startswith("| "))
        s.insert(ult + 1, nueva(None))
    open(CAL_MD, "w", encoding="utf-8").write("\n".join(s))


def leer_web():
    return json.load(open(os.path.join(CAL_WEB, "data.json"), encoding="utf-8"))


def guardar_web(d):
    json.dump(d, open(os.path.join(CAL_WEB, "data.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)


def cmd_hecho(args):
    d, cuenta, nn, nombre = carpeta(args[0])
    f, tipo = args[1], (args[2] if len(args) > 2 else "Venta")
    c = "C" if cuenta == "CHECA" else "P"
    for x in ("video.mp4", "portada.jpg", "copy.txt", "dm.txt"):
        if not os.path.exists(os.path.join(d, x)):
            fallar(f"falta {x} en {d}")
    v, th = f"v/{c}{nn}.mp4", f"t/{c}{nn}.jpg"
    shutil.copy(os.path.join(d, "video.mp4"), os.path.join(CAL_WEB, v))
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(d, "portada.jpg"), "-vf", "scale=360:-1", "-q:v", "4", os.path.join(CAL_WEB, th)], check=True)
    leer = lambda x: open(os.path.join(d, x), encoding="utf-8").read().strip()
    e = {"c": c, "n": nn, "t": nombre, "f": f, "e": "listo", "p": None, "tipo": tipo, "copy": leer("copy.txt"), "dm": leer("dm.txt"),
         "th": th, "v": v, "mb": round(os.path.getsize(os.path.join(CAL_WEB, v)) / 1048576, 1),
         "files": {"portada": True, "video": True, "copy": True, "dm": True}}
    datos = [x for x in leer_web() if not (x["c"] == c and x["n"] == nn)]
    pos = max((i for i, x in enumerate(datos) if x["c"] == c), default=len(datos) - 1) + 1
    datos.insert(pos, e)
    guardar_web(datos)
    hoy = f"✅ (hecho {fecha_bonita(date.today().isoformat())[4:]})"
    fila_md(cuenta, nn, nombre, lambda viejo: f"| {nn} | {fecha_bonita(f)} | {tipo}{'' if cuenta == 'CHECA' else ' (v2)'} | `{nn} - {nombre}` | {hoy} | |")
    print(f"listo: {c}{nn} «{nombre}» para el {fecha_bonita(f)}")


def cmd_publicado(args):
    d, cuenta, nn, nombre = carpeta(args[0])
    f, c = args[1], ("C" if cuenta == "CHECA" else "P")
    datos = leer_web()
    hay = [x for x in datos if x["c"] == c and x["n"] == nn]
    if not hay:
        fallar(f"{c}{nn} no está en el calendario web; corre antes «hecho»")
    for x in hay:
        x["e"], x["p"] = "pub", f
    guardar_web(datos)

    def marcar(viejo):
        if viejo is None:
            fallar(f"{nn} no está en calendario.md")
        partes = viejo.split("|")
        partes[-2] = f" ✅ {fecha_bonita(f)[4:]} "
        return "|".join(partes)
    fila_md(cuenta, nn, nombre, marcar)
    print(f"publicado: {c}{nn} el {fecha_bonita(f)}")


def cmd_subir(args):
    msg = args[0]
    for repo in (ROOT, PWA):
        if not os.path.isdir(repo):
            continue
        g = lambda *a: subprocess.run(["git", "-C", repo] + list(a), check=True, capture_output=True, text=True).stdout
        if g("status", "--porcelain").strip():
            g("add", "-A")
            g("commit", "-q", "-m", msg)
        rama = g("branch", "--show-current").strip()
        g("push", "-q", "origin", "HEAD:main")
        if rama and rama != "main":
            g("push", "-q", "-u", "origin", rama)
        print(f"subido: {os.path.basename(repo)} (main{', ' + rama if rama and rama != 'main' else ''})")


# ---------- control de calidad ----------
CTA_VOZ = "Comenta CHECA y te paso el acceso a la prueba en Google Play"


def cmd_calidad(args):
    camara = "--camara" in args
    args = [a for a in args if a != "--camara"]
    d, cuenta, nn, nombre = carpeta(args[0])
    proy = proyecto(args[1]) if len(args) > 1 else None
    res = []
    ok = lambda cond, txt: res.append((bool(cond), txt))
    leer = lambda f: open(f, encoding="utf-8").read() if os.path.exists(f) else ""
    for x in ("video.mp4", "portada.jpg", "copy.txt", "dm.txt", "guion.md"):
        ok(os.path.exists(os.path.join(d, x)), f"existe {x}")
    mp4, por = os.path.join(d, "video.mp4"), os.path.join(d, "portada.jpg")
    if os.path.exists(mp4):
        info = json.loads(subprocess.check_output(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "stream=pix_fmt,profile,width,height:format=duration", "-of", "json", mp4]))
        v, dur = info["streams"][0], float(info["format"]["duration"])
        ok(v["pix_fmt"] == "yuv420p" and v.get("profile") == "High", f"mp4 yuv420p High ({v['pix_fmt']}, {v.get('profile')})")
        ok((v["width"], v["height"]) == (1080, 1920), f"mp4 1080x1920 ({v['width']}x{v['height']})")
        ok(20 <= dur <= 60, f"duración razonable ({dur:.1f} s)")
        if os.path.exists(por):
            r = subprocess.run(["ffmpeg", "-i", mp4, "-i", por, "-lavfi", "[0:v]trim=end_frame=1,setpts=PTS-STARTPTS[a];[1:v]scale=1080:1920[b];[a][b]psnr", "-f", "null", "-"], capture_output=True, text=True)
            m = re.search(r"average:([0-9.]+|inf)", r.stderr)
            psnr = float("inf") if m and m.group(1) == "inf" else float(m.group(1)) if m else 0
            ok(psnr > 25, f"la portada es el primer cuadro (PSNR {psnr:.1f})")
    copy = leer(os.path.join(d, "copy.txt"))
    ok("Comenta «CHECA»" in copy and "Google Play" in copy, "copy con el CTA vigente (Comenta «CHECA»… Google Play)")
    ok("ejemplo" in copy.lower(), "copy dice «números de ejemplo»")
    ok(camara or not re.search(r"soy eduardo|armé checa", copy, re.I), "copy sin «Soy Eduardo» (video sin su cara)")
    ok(not re.search(r"gratis|sin costo|\$\s?\d+\s?(al mes|/mes)", copy, re.I), "copy sin «gratis» ni precios")
    dm = leer(os.path.join(d, "dm.txt"))
    fuente = "tiktok" if cuenta == "CHECA" else "tiktok-personal"
    ok(f"utm_source={fuente}&utm_campaign={slug(nombre)}" in dm, f"dm con utm_source={fuente} y utm_campaign={slug(nombre)}")
    if proy:
        voz = [l.strip() for l in leer(os.path.join(proy, "SCRIPT.md")).split("\n") if l.startswith("    ")]
        ok(voz and not any("$" in l for l in voz), "voz sin «$» (montos sin moneda)")
        ok(voz and voz[-1].rstrip(".") == CTA_VOZ, "la última línea de voz es el CTA vigente")
        esc = json.loads(leer(os.path.join(proy, "escenas.json")) or "[]")
        pops = []
        for e in esc:
            pops += ([e["popup"]] if "popup" in e else []) + e.get("popups", [])
        ok(all(p.get("app", "OFERTA") == "OFERTA" for p in pops), "pop-ups con «OFERTA» (sin nombres de apps)")
        ok(all(p["viaje"] - p["gasolina"] == p["neta"] for p in pops if "neta" in p), "neta = viaje − gasolina en todos los pop-ups")
        ok(esc and esc[-1]["tipo"] == "cierre", "termina con la pantalla final")
        pj = os.path.join(proy, "portada", "portada.json")
        if os.path.exists(pj):
            c = json.load(open(pj, encoding="utf-8"))
            vis = " ".join([c["etiqueta"], " ".join(c["titulo"] if isinstance(c["titulo"], list) else [c["titulo"]]), c["precio"], c["linea"], c["recogida"], c["viaje"]])
            ok(str(c["sello_valor"]) not in vis, "el sello de la portada esconde una cifra que no está a la vista")
    for b, t in res:
        print(("✓ " if b else "✗ ") + t)
    malos = sum(1 for b, _ in res if not b)
    print(f"{cuenta} {nn} «{nombre}»: {len(res) - malos}/{len(res)} bien")
    if malos:
        sys.exit(1)


COMANDOS = {"nuevo": cmd_nuevo, "construir": cmd_construir, "portada": cmd_portada, "revisar": cmd_revisar, "entregar": cmd_entregar,
            "dm": cmd_dm, "tabla": cmd_tabla, "hecho": cmd_hecho, "publicado": cmd_publicado, "subir": cmd_subir, "calidad": cmd_calidad}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMANDOS:
        sys.exit(__doc__)
    COMANDOS[sys.argv[1]](sys.argv[2:])
