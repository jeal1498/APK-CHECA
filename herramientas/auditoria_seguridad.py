#!/usr/bin/env python3
"""Auditoría estática de seguridad del conjunto de cambios respecto de una base (por defecto main a7a732f).
Solo lee. Revisa: secretos y tokens, URLs externas, mensajes de commit, SQL dinámico, ejecución de shell,
sumideros de inyección en el Admin, enlaces/archivos binarios, cambios de dependencias y de permisos Android.
Uso: python3 herramientas/auditoria_seguridad.py [BASE] > informe.md    Sale con 1 si hay hallazgos críticos."""
import os, re, subprocess, sys
RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = sys.argv[1] if len(sys.argv) > 1 else "a7a732fb3fc874e42cda16b79592137a241bc9d9"
g = lambda *a: subprocess.run(["git", "-C", RAIZ, *a], capture_output=True, text=True, check=True).stdout
SECRETOS = [
    ("jwt", r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{10,}"),
    ("llave supabase", r"sb_(?:secret|publishable)_[A-Za-z0-9_-]{16,}"),
    ("llave privada", r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    ("llave age", r"AGE-SECRET-KEY-1[A-Z0-9]{20,}"),
    ("token github", r"gh[pousr]_[A-Za-z0-9]{30,}"),
    ("clave de API genérica", r"(?i)(?:api[_-]?key|secret|password|passwd|token)\s*[:=]\s*['\"][A-Za-z0-9_\-]{20,}['\"]"),
    ("url de sesión", r"claude\.ai/code/session_[A-Za-z0-9]{10,}"),
]
# Cadenas de PRUEBA conocidas (falsas a propósito); cualquier otra coincidencia es hallazgo.
FALSOS = ["eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoic2VydmljZV9yb2xlIn0.x", "session_01ABCdef"]
cambiados = [l.split("\t") for l in g("diff", "--name-status", BASE, "HEAD").splitlines()]
rutas = [p[-1] for p in cambiados if p[0] != "D"]
criticos, notas = [], []
urls, correos = {}, {}
for r in rutas:
    p = os.path.join(RAIZ, r)
    if os.path.islink(p):
        criticos.append(f"enlace simbólico versionado: {r}"); continue
    try:
        t = open(p, encoding="utf-8").read()
    except UnicodeDecodeError:
        notas.append(f"binario (no revisado como texto): {r}"); continue
    for nombre, rx in SECRETOS:
        for m in re.finditer(rx, t):
            if any(f in m.group(0) or m.group(0) in f for f in FALSOS):
                continue
            if nombre == "jwt":   # una llave «anon» de Supabase es pública por diseño; cualquier otro rol es crítico
                try:
                    import base64, json
                    c = m.group(0).split(".")[1]
                    rol = json.loads(base64.urlsafe_b64decode(c + "=" * (-len(c) % 4))).get("role")
                except Exception:
                    rol = "?"
                if rol == "anon":
                    en_base = subprocess.run(["git", "-C", RAIZ, "show", f"{BASE}:{r}"], capture_output=True, text=True).stdout
                    notas.append(f"llave anon de Supabase (pública por diseño) en {r}" + (" — ya estaba en la base" if m.group(0) in en_base else " — NUEVA en este cambio"))
                    continue
                nombre = f"jwt con rol «{rol}»"
            criticos.append(f"{nombre} en {r}")
    for u in re.findall(r"https?://[A-Za-z0-9.-]+", t):
        urls.setdefault(u, set()).add(r.split("/")[0])
    for c in re.findall(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}", t):
        correos.setdefault(c, set()).add(r)
    if r.endswith(".sql"):
        for n, l in enumerate(t.splitlines(), 1):
            ll = l.strip().lower()
            if re.search(r"\bexecute\s+(format|'|\w+\s*\|\|)|\bexecute\s+\w+;", ll) and not ll.startswith(("grant ", "revoke ", "--")):
                notas.append(f"SQL dinámico {r}:{n}: {l.strip()[:110]}")
    if r.endswith(".py"):
        for n, l in enumerate(t.splitlines(), 1):
            if re.search(r"shell\s*=\s*True|os\.system\(|\beval\(|pickle\.loads|yaml\.load\(", l) and "re.search" not in l:
                criticos.append(f"ejecución insegura {r}:{n}")
    if r.startswith("admin/"):
        codigo = "\n".join(l for l in t.splitlines() if not l.strip().startswith("//"))
        if re.search(r"innerHTML|outerHTML|document\.write|\beval\(|new Function|insertAdjacentHTML", codigo):
            criticos.append(f"sumidero de inyección en {r}")
mensajes = g("log", "--format=%H%n%B%n==", f"{BASE}..HEAD")
con_url = sorted({b.splitlines()[0][:12] for b in mensajes.split("\n==\n") if b.strip() and re.search(SECRETOS[-1][1], b)})
gradle = g("diff", BASE, "HEAD", "--", "app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts", "gradle.properties")
deps = [l for l in gradle.splitlines() if l.startswith(("+", "-")) and not l.startswith(("+++", "---"))]
manif = g("diff", "--stat", BASE, "HEAD", "--", "app/src/main/AndroidManifest.xml", "app/proguard-rules.pro").strip()
wf = g("diff", "--stat", BASE, "HEAD", "--", ".github").strip()
print(f"# Auditoría estática de seguridad\n\nBase `{BASE[:12]}` → `{g('rev-parse', 'HEAD').strip()[:12]}` · {len(rutas)} archivos revisados ({sum(p[0] == 'A' for p in cambiados)} creados, {sum(p[0] == 'M' for p in cambiados)} modificados, {sum(p[0] == 'D' for p in cambiados)} eliminados).\n")
print("## Hallazgos críticos\n" + ("\n".join(f"- {c}" for c in sorted(set(criticos))) or "- Ninguno."))
print("\n## Mensajes de commit con URL de sesión\n" + ("- " + ", ".join(con_url) + " (ya publicados antes de esta auditoría; no se reescribe historial)" if con_url else "- Ninguno."))
print("\n## Dominios externos citados (código, pruebas y documentación)")
for u in sorted(urls):
    print(f"- {u} — en: {', '.join(sorted(urls[u]))}")
print("\n## Correos presentes\n" + ("\n".join(f"- {c} — {len(v)} archivo(s)" for c, v in sorted(correos.items())) or "- Ninguno."))
print("\n## Cambios en Gradle (dependencias y versión)\n" + ("\n".join(f"    {l}" for l in deps) or "- Sin cambios."))
print("\n## AndroidManifest / ProGuard\n- " + (manif or "Sin cambios (ningún permiso nuevo)."))
print("\n## Workflows\n- " + (wf or "Sin cambios en `.github/`."))
print("\n## Notas (revisadas a mano)\n" + ("\n".join(f"- {n}" for n in notas) or "- Ninguna."))
sys.exit(1 if criticos else 0)
