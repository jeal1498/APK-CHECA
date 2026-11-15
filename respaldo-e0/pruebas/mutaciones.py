"""Pruebas de mutación del procedimiento E0 (v2, incluye D1–D3).
Aplica cada mutación a una COPIA de respaldo-e0/, vuelve a fijar SCRIPTS.sha256 en la copia (para que la
mutación no se detecte solo por la huella) y corre la suite. Una mutación debe hacer fallar la suite.
Uso: python3 pruebas/mutaciones.py [DIR_respaldo-e0]   (no modifica el original). Sale con 1 si alguna sobrevive."""
import hashlib, os, re, shutil, subprocess, sys, tempfile
AQUI = os.path.dirname(os.path.abspath(__file__))
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
sobreviven = 0
for nombre, f, o, n in M:
    t, d = copia()
    p = os.path.join(d, f); s = open(p, encoding="utf-8").read(); assert s.count(o) == 1, (nombre, s.count(o))
    open(p, "w", encoding="utf-8").write(s.replace(o, n, 1)); fijar(d)
    code, out = suite(d)
    fallan = re.findall(r"^(?:FAIL|ERROR): (\S+)", out, flags=re.M)
    sobreviven += code == 0
    print(f"{nombre}: {'DETECTADA' if code else 'SOBREVIVE'}; fallan {len(fallan)}: {fallan[:3]}", flush=True)
    shutil.rmtree(t)
print(f"{len(M)} mutaciones, {len(M) - sobreviven} detectadas, {sobreviven} sobreviven.")
sys.exit(1 if sobreviven else 0)
