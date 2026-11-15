"""Evidencia de mutación: aplica cada mutación a una COPIA del código del commit y corre la suite.
Uso: python3 ejecutar_mutaciones.py DIR_EXTRAIDO_DEL_COMMIT/respaldo-e0   (no modifica el directorio original)"""
import os, re, shutil, subprocess, sys, tempfile
BASE = sys.argv[1]
M = [
 ("M1 sigue redirecciones (reenviaría la llave)", "respaldar.py", "return None  # → HTTPError", "return super().redirect_request(*a, **k)  # →", None),
 ("M2 no valida cabecera WebP", "respaldar.py", "or not comun.es_webp(cab)", "or False", None),
 ("M3 no compara sha256 en verificar", "verificar.py", 'or sha != f["sha256"]', "or False", None),
 ("M4 ids requeridos siempre presentes (verificar_entrega)", "verificar_entrega.py", 'faltan = [i for i in req if f"{i}.webp" not in nom]', "faltan = []", "test_lista_de_ids_del_repo_con_un_id_que_el_respaldo_no_tiene"),
 ("M5 acepta llave anon", "respaldar.py", 'if rol != "service_role":', "if False:", None),
 ("M6 listado sin paginar", "respaldar.py", "        offset += len(pagina)", "        break", None),
 ("M7 restauración no ligada al sha256 de B", "evidencia_copias.py", 'rb.get("copia_sha256") == b["sha256"]', "True", "test_restauracion_etiquetada_B_pero_hecha_con_otro_archivo"),
]
def suite(d, extra=()):
    p = subprocess.run([sys.executable, "-W", "ignore", "-m", "unittest", "discover", "-s", "pruebas", *extra], cwd=d, capture_output=True, text=True)
    return p.returncode, p.stdout + p.stderr
def copia():
    t = tempfile.mkdtemp(prefix="mut-"); d = os.path.join(t, "respaldo-e0"); shutil.copytree(BASE, d, ignore=shutil.ignore_patterns("__pycache__")); return t, d
def mutar(d, f, o, n):
    p = os.path.join(d, f); s = open(p, encoding="utf-8").read(); assert s.count(o) >= 1, o; open(p, "w", encoding="utf-8").write(s.replace(o, n, 1))
def quitar_prueba(d, nombre):
    p = os.path.join(d, "pruebas", "test_e0.py"); s = open(p, encoding="utf-8").read()
    s2 = re.sub(r"    def %s\(self.*?(?=\n    def |\nclass |\nif __name__)" % nombre, "", s, flags=re.S); assert s2 != s; open(p, "w", encoding="utf-8").write(s2)
for nombre, f, o, n, refuerzo in M:
    t, d = copia(); mutar(d, f, o, n)
    code, out = suite(d)
    fallos = re.findall(r"^(?:FAIL|ERROR): (\S+)", out, flags=re.M)
    print(f"{nombre}\n  suite final con mutación -> {'DETECTADA' if code else 'SOBREVIVE'}; pruebas que fallan: {fallos[:4]}")
    if refuerzo:
        t2, d2 = copia(); quitar_prueba(d2, refuerzo); mutar(d2, f, o, n); c2, _ = suite(d2)
        print(f"  sin la prueba de refuerzo «{refuerzo}» -> {'DETECTADA' if c2 else 'SOBREVIVE (así sobrevivió antes de reforzar)'}"); shutil.rmtree(t2)
    shutil.rmtree(t)
