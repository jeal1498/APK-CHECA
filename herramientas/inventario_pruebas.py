#!/usr/bin/env python3
"""Reconstruye el inventario de pruebas a partir de los logs COMPLETOS de herramientas/probar_todo.sh.
Uso: python3 herramientas/inventario_pruebas.py DIR_LOGS [inventario_anterior.txt] > INVENTARIO-PRUEBAS.md"""
import collections, re, sys
L = sys.argv[1].rstrip("/") + "/"
def leer(n):
    return open(L + n, encoding="utf-8", errors="replace").read()
def py(log):
    t = leer(log)
    nombres = [m.group(2) + "." + m.group(1) if not m.group(2).endswith("." + m.group(1)) else m.group(2)
               for m in re.finditer(r"^(test\w+) \(([\w.]+)\)", t, flags=re.M)]
    ran = re.search(r"^Ran (\d+) tests", t, flags=re.M)
    estado = "OK" if re.search(r"^OK( \(.*\))?$", t, flags=re.M) else "FALLA"
    omitidas = len(re.findall(r"\.\.\. skipped", t))
    return nombres, int(ran.group(1)) if ran else None, estado, omitidas
def tap(log):
    t = leer(log)
    n = [m.group(2) for m in re.finditer(r"^(ok|not ok) \d+ - (.*)$", t, flags=re.M)]
    return n, len(n), "OK" if not re.search(r"^not ok", t, flags=re.M) else "FALLA", len(re.findall(r"# SKIP", t))
def jvm(log):
    t = leer(log)
    n = [m.group(2) for m in re.finditer(r"^(ok|FALLA)\s+(\w+\.\w+)", t, flags=re.M)]
    return n, len(n), "OK" if not re.search(r"^FALLA", t, flags=re.M) else "FALLA", 0
suites = [("E0 respaldo-e0/pruebas", "unittest · integración contra servidor SIMULADO", py("e0-respaldo.log"), True),
          ("auditoria/pruebas en PostgreSQL 16", "unittest · integración en Postgres LOCAL con stubs de Supabase", py("auditoria-pg16.log"), True),
          ("auditoria/pruebas en PostgreSQL 17", "las mismas pruebas en otro motor", py("auditoria-pg17.log"), False),
          ("E2 registrar.test.ts", "node:test · unitarias con dependencias simuladas", tap("e2-node.log"), True),
          ("E3 Kotlin en JVM", "sustituto mínimo de JUnit · unitarias, NO Android", jvm("e3-jvm.log"), True),
          ("E6–E7 admin_ui.test.js", "node:test + Playwright · cliente Supabase SIMULADO", tap("e6-e7-ui.log"), True)]
print("# Inventario reconciliado de pruebas — ejecución definitiva\n")
print("Reconstruido de los logs completos (" + leer("entorno.txt").splitlines()[0] + "), no del código: cada nombre es una prueba que se ejecutó.\n")
print("| Suite | Tipo | Nombres en el log | Únicos | Repetidos | «Ran N» del ejecutor | Omitidas | Resultado |\n|---|---|---|---|---|---|---|---|")
total, todos = 0, set()
for nom, tipo, (n, ran, est, om), cuenta in suites:
    c = collections.Counter(n)
    print(f"| {nom} | {tipo} | {len(n)} | {len(c)} | {sum(v - 1 for v in c.values())} | {ran} | {om} | {est} |")
    assert ran == len(c), (nom, ran, len(c))
    if cuenta:
        total += len(c); todos |= set(c)
iguales = set(suites[1][2][0]) == set(suites[2][2][0])
print(f"\n**Pruebas únicas: {total}.** Las de `auditoria/pruebas` se cuentan una vez aunque corren en dos motores (mismos nombres en 16 y 17: {'sí' if iguales else 'NO'}).\n")
if len(sys.argv) > 2:
    corto = lambda x: x.replace("coherencia: ", "coherencia: ")  # nombres completos (módulo.Clase.prueba)
    viejo = {corto(m) for m in re.findall(r"^  (\S.*)$", open(sys.argv[2], encoding="utf-8").read().split("## No son pruebas")[0], flags=re.M)}
    nuevo = {corto(x) for x in todos}
    print(f"## Comparación con el inventario anterior ({len(viejo)} nombres)\n")
    print(f"- Siguen ejecutándose con el mismo nombre: **{len(viejo & nuevo)}**")
    print(f"- Ya no existen con ese nombre: **{len(viejo - nuevo)}**" + "".join(f"\n  - `{x}`" for x in sorted(viejo - nuevo)))
    print(f"- Nuevas: **{len(nuevo - viejo)}**" + "".join(f"\n  - `{x}`" for x in sorted(nuevo - viejo)))
print("\n## Lista completa por suite")
for nom, tipo, (n, ran, est, om), cuenta in suites:
    if cuenta:
        print(f"\n### {nom} ({len(set(n))})\n" + "\n".join(f"- {x}" for x in sorted(set(n))))
