#!/usr/bin/env python3
"""Guardia E3: el lector (Parser.kt, Tarjetas.kt, Moneda.kt) no puede cambiar en un release de trazabilidad.
Compara con herramientas/parser.sha256 (fijado desde main a7a732f). Sale con 1 si alguno cambió.
Para un cambio AUTORIZADO del lector (línea L1–L4, release aparte): actualizar parser.sha256 en ese commit."""
import hashlib, os, sys
RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
def main():
    mal = []
    for linea in open(os.path.join(RAIZ, "herramientas", "parser.sha256"), encoding="utf-8"):
        h, ruta = linea.split()
        actual = hashlib.sha256(open(os.path.join(RAIZ, ruta), "rb").read()).hexdigest()
        print(("ok    " if actual == h else "CAMBIÓ ") + ruta)
        if actual != h: mal.append(ruta)
    return 1 if mal else 0
if __name__ == "__main__":
    sys.exit(main())
