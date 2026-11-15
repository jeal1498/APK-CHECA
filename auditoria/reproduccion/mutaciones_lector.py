#!/usr/bin/env python3
"""E5: demuestra que la regresión por caso DETECTA errores conocidos del lector.
Aplica cada mutación a una COPIA de Parser.kt, la reproduce sobre las entradas ciegas y la compara con la base.
Una mutación debe producir al menos una regresión (comparar sale con 1). No toca app/.
Uso: python3 mutaciones_lector.py ENTRADAS_DIR ETIQUETAS.jsonl BASE_predicciones.jsonl SALIDA_DIR"""
import os, shutil, subprocess, sys, tempfile
AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
M = os.path.join(RAIZ, "app", "src", "main", "java", "com", "karen", "miruta")
MUT = [
    ("L1 km sin la recogida", "        var km = tramosReales.sumOf { it.first }", "        var km = tramosReales.drop(1).sumOf { it.first }.takeIf { it > 0 } ?: tramosReales.sumOf { it.first }"),
    ("L2 minutos solo del primer tramo", "        var min = tramosReales.sumOf { it.second }", "        var min = tramosReales.first().second"),
    ("L3 sin detectar ofertas caídas", "        if (pares.any { ofertaCaida.containsMatchIn(it) }) return true", "        if (false) return true"),
    ("L4 sin tope de $/km plausible", "        if (f != null && (o.porKm * f > 80 * holgura || o.porHora * f > 3000 * holgura)) return null", "        if (false) return null"),
    ("L5 resumen no corrige km", "            if (Math.abs(km - est) > tol && Math.abs(resumen.first - est) <= tol) {", "            if (false) {"),
]
ent, etq, base, sal = sys.argv[1:5]
os.makedirs(sal, exist_ok=True)
sobrevive = 0
for nombre, o, n in MUT:
    d = tempfile.mkdtemp(prefix="mutlec-")
    for f in ("Parser", "Moneda", "Tarjetas"):
        shutil.copy(os.path.join(M, f + ".kt"), d)
    p = os.path.join(d, "Parser.kt"); s = open(p, encoding="utf-8").read()
    assert s.count(o) == 1, (nombre, s.count(o))
    open(p, "w", encoding="utf-8").write(s.replace(o, n))
    out = os.path.join(sal, nombre.split()[0])
    subprocess.run([os.path.join(AQUI, "reproducir.sh"), d, ent, out], check=True, capture_output=True)
    r = subprocess.run([sys.executable, os.path.join(AQUI, "evaluar.py"), "comparar", "--base", base, "--candidata",
                        os.path.join(out, "predicciones.jsonl"), "--etiquetas", etq, "--salida", os.path.join(out, "comparacion.json")],
                       capture_output=True, text=True)
    sobrevive += r.returncode == 0
    print(f"{nombre}: {'DETECTADA' if r.returncode else 'SOBREVIVE'} — {r.stdout.strip()}", flush=True)
    shutil.rmtree(d)
print(f"{len(MUT)} mutaciones, {len(MUT) - sobrevive} detectadas")
sys.exit(1 if sobrevive else 0)
