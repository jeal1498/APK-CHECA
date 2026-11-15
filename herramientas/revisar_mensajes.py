#!/usr/bin/env python3
"""D5: revisa mensajes de commit (o cualquier texto) en busca de URLs de sesión de Claude, llaves o correos.

Uso:
  python3 herramientas/revisar_mensajes.py RANGO          # p. ej. origin/main..HEAD (revisa cada commit)
  python3 herramientas/revisar_mensajes.py --archivo F    # como hook commit-msg: .git/hooks/commit-msg
Sale con 1 si hay hallazgos. Solo lee; no reescribe historial.
"""
import re
import subprocess
import sys

PATRONES = [
    ("url_sesion_claude", re.compile(r"claude\.ai/code/session_[A-Za-z0-9]+")),
    ("url_sesion_claude", re.compile(r"Claude-Session:", re.I)),
    ("jwt", re.compile(r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}")),
    ("llave_supabase", re.compile(r"sb_(secret|publishable)_[A-Za-z0-9_-]+")),
    ("llave_privada", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY")),
    ("llave_age", re.compile(r"AGE-SECRET-KEY-")),
    ("correo", re.compile(r"[A-Za-z0-9._%+-]+@(?!anthropic\.com)[A-Za-z0-9.-]+\.[A-Za-z]{2,}")),
]


def hallazgos(texto):
    """Lista de (tipo, línea) sin repetir el valor completo encontrado."""
    res = []
    for n, linea in enumerate(texto.splitlines(), 1):
        for tipo, rx in PATRONES:
            if rx.search(linea):
                res.append((tipo, n))
    return res


def main(argv):
    if len(argv) == 2 and argv[0] == "--archivo":
        h = hallazgos(open(argv[1], encoding="utf-8").read())
        for tipo, n in h:
            print(f"mensaje: línea {n}: {tipo}", file=sys.stderr)
        return 1 if h else 0
    if len(argv) != 1:
        print(__doc__, file=sys.stderr)
        return 2
    shas = subprocess.run(["git", "rev-list", argv[0]], capture_output=True, text=True, check=True).stdout.split()
    total = 0
    for sha in shas:
        msg = subprocess.run(["git", "log", "-1", "--format=%B", sha], capture_output=True, text=True, check=True).stdout
        for tipo, n in hallazgos(msg):
            print(f"{sha[:12]}: línea {n}: {tipo}")
            total += 1
    print(f"{len(shas)} commits revisados, {total} hallazgos.", file=sys.stderr)
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
