"""Pruebas de mutación del procedimiento E0 (v2, incluye D1–D3).
Aplica cada mutación a una COPIA de respaldo-e0/, vuelve a fijar SCRIPTS.sha256 en la copia (para que la
mutación no se detecte solo por la huella) y corre la suite. Una mutación debe hacer fallar la suite.
Uso: python3 pruebas/mutaciones.py [DIR_respaldo-e0]   (no modifica el original). Sale con 1 si alguna sobrevive."""
import hashlib, os, re, shutil, subprocess, sys, tempfile
AQUI = os.path.dirname(os.path.abspath(__file__))
# Opcional: --solo M18,M19 para repetir solo algunas mutaciones.
SOLO = None
if "--solo" in sys.argv:
    i = sys.argv.index("--solo"); SOLO = set(sys.argv[i + 1].split(",")); del sys.argv[i:i + 2]
BASE = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(AQUI)
sys.path.insert(0, BASE)
import comun  # noqa: E402
M = [
 ("M1 sigue redirecciones", "respaldar.py", "return None  # → HTTPError", "return super().redirect_request(*a, **k)  # →"),
 ("M2 no valida WebP", "respaldar.py", "or not comun.es_webp(cab)", "or False"),
 ("M3 no compara sha256 en verificar", "verificar.py", 'or sha != f["sha256"]', "or False"),
 ("M4 ids requeridos siempre presentes (entrega)", "verificar_entrega.py", 'faltan = [i for i in req if f"{i}.webp" not in nom]', "faltan = []"),
 ("M5 acepta llave anon", "respaldar.py", 'if rol != "service_role":', "if False:"),
 ("M6 listado sin paginar", "respaldar.py", "        offset += len(pagina)", "        break"),
 ("M7 restauración no ligada a B", "evidencia_copias.py", 'rb.get("copia_sha256") == b["sha256"]', "True"),
 ("M8 D1 COMPLETA sin exigir cifrados distintos", "evidencia_copias.py", '                                          ev["cifrados_distintos"], ev["archivos_distintos"],', '                                          True, ev["archivos_distintos"],'),
 ("M9 D1 mismo archivo cuenta como dos", "evidencia_copias.py", 'not os.path.samefile(args.copia_a, args.copia_b)', 'True'),
 ("M10 D1 C08 de la entrega siempre ok", "verificar_entrega.py", 'cond("C08", ev.get("cifrados_distintos") is True', 'cond("C08", True or ev.get("cifrados_distintos") is True'),
 ("M11 D2 sin fila no cuenta", "respaldar.py", "    if sin_fila:\n        motivos.append", "    if False:\n        motivos.append"),
 ("M12 D2 versión distinta no cuenta", "respaldar.py", "    if version_distinta:\n        motivos.append", "    if False:\n        motivos.append"),
 ("M13 D3 respaldar ignora la integridad", "respaldar.py", "        if dif:\n            raise ErrorRespaldo(f\"Integridad", "        if False:\n            raise ErrorRespaldo(f\"Integridad"),
 ("M14 D3 V11b siempre ok", "verificar.py", 'c.cond("V11b", not prob,', 'c.cond("V11b", True,'),
 ("M15 D3 script faltante se omite", "comun.py", '            raise ScriptsAlterados(f"falta el script {n}")', '            continue'),
 ("M16 D3 E12b siempre ok", "verificar_entrega.py", 'cond("E12b", not p_res,', 'cond("E12b", True,'),
 ("M17 H1 verificar acepta rutas inseguras", "verificar.py", "        except comun.RutaInsegura as e:\n            inseguras.append(str(e))", "        except comun.RutaInsegura as e:\n            pass"),
 ("M18 H1 nombre_seguro acepta ..", "comun.py", 'if p in ("", ".", "..") or', 'if p in ("", ".") or'),
 ("M18b H1 nombre_seguro sin reglas de punto ni «..»", "comun.py", 'if p in ("", ".", "..") or not _RE_COMPONENTE.match(p) or p.endswith((" ", ".")) or p.startswith(" "):', 'if p == "" or not _RE_COMPONENTE.match(p):'),
 ("M19 H1 ruta_en no comprueba la ruta real (enlaces)", "comun.py", "    if os.path.realpath(p) != p or not p.startswith(raiz + os.sep):", "    if False:"),
 ("M20 H2 empaquetar sin verificar", "evidencia_copias.py", '    if v["resultado"] == "FALLO":\n        raise SystemExit("ERROR: el respaldo no verifica', '    if False:\n        raise SystemExit("ERROR: el respaldo no verifica'),
 ("M21 H3 COMPLETA sin comprobar A", "evidencia_copias.py", '                                          ev["copia_a_comprobada"], ev["versiones_coherentes"],', '                                          True, ev["versiones_coherentes"],'),
 ("M22 H3 comprobación A no ligada al archivo A", "evidencia_copias.py", 'ra.get("etiqueta_copia") == "A" and ra.get("copia_sha256") == a["sha256"] \\', 'ra.get("etiqueta_copia") == "A" \\'),
 ("M23 H7 versiones no se comparan", "evidencia_copias.py", '                                          ev["copia_a_comprobada"], ev["versiones_coherentes"],', '                                          ev["copia_a_comprobada"], True,'),
 ("M24 H7 V02c siempre ok", "verificar.py", 'c.cond("V02c", resumen.get("procedimiento") == comun.VERSION_PROCEDIMIENTO,', 'c.cond("V02c", True,'),
 ("M25 H6 no informa ids fuera de lista", "verificar.py", "    if no_listados:\n        c.aviso", "    if False:\n        c.aviso"),
 ("M26 H8 tolera archivos VERIFICACION*", "verificar.py", 'rel != "VERIFICACION.json":', 'not rel.startswith("VERIFICACION"):'),
 ("M27 H2 tar acepta entradas repetidas", "evidencia_copias.py", "            if m.name in vistos:", "            if False:"),
]
def fijar(d):
    with open(os.path.join(d, "SCRIPTS.sha256"), "w") as f:
        for n in comun.SCRIPTS:
            f.write(hashlib.sha256(open(os.path.join(d, n), "rb").read()).hexdigest() + "  " + n + "\n")
def suite(d):
    p = subprocess.run([sys.executable, "-W", "ignore", "-m", "unittest", "discover", "-s", "pruebas"], cwd=d, capture_output=True, text=True)
    return p.returncode, p.stdout + p.stderr
def copia():
    t = tempfile.mkdtemp(prefix="mut-"); d = os.path.join(t, "respaldo-e0")
    shutil.copytree(BASE, d, ignore=shutil.ignore_patterns("__pycache__")); return t, d
t, d = copia(); code, out = suite(d); shutil.rmtree(t)
print(f"BASE sin mutar: {'OK' if code == 0 else 'FALLA'}", flush=True)
if code:
    print(out[-2000:]); sys.exit(2)
# Mutaciones EQUIVALENTES conocidas: no cambian el comportamiento, así que ninguna prueba puede detectarlas.
# M18: quitar «..» de la lista no cambia nada porque la misma condición rechaza todo componente que termina en punto.
EQUIVALENTES = {"M18"}
sobreviven = 0
if SOLO:
    M = [m for m in M if m[0].split()[0] in SOLO]
for nombre, f, o, n in M:
    t, d = copia()
    p = os.path.join(d, f); s = open(p, encoding="utf-8").read(); assert s.count(o) == 1, (nombre, s.count(o))
    open(p, "w", encoding="utf-8").write(s.replace(o, n, 1)); fijar(d)
    code, out = suite(d)
    fallan = re.findall(r"^(?:FAIL|ERROR): (\S+)", out, flags=re.M)
    equiv = nombre.split()[0] in EQUIVALENTES
    sobreviven += code == 0 and not equiv
    print(f"{nombre}: {'DETECTADA' if code else ('EQUIVALENTE (no detectable; ver comentario)' if equiv else 'SOBREVIVE')}; fallan {len(fallan)}: {fallan[:3]}", flush=True)
    shutil.rmtree(t)
eq = sum(1 for m in M if m[0].split()[0] in EQUIVALENTES)
print(f"{len(M)} mutaciones: {len(M) - sobreviven - eq} detectadas, {eq} equivalentes, {sobreviven} sobreviven.")
sys.exit(1 if sobreviven else 0)
