#!/usr/bin/env python3
"""E4: clasifica el ORIGEN de la respuesta esperada de cada caso del banco (app/src/test/resources/banco).
Lee solo los comentarios de cabecera; no lee ni copia líneas. Escribe banco_origen.csv (sin datos personales).
Ninguna etiqueta del banco es verdad humana verificada: este archivo lo deja explícito."""
import csv, os, re, sys
RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BANCO = os.path.join(RAIZ, "app", "src", "test", "resources", "banco")
ORIGENES = [
    ("veredicto_humano", re.compile(r"según veredicto humano")),  # lo escribe veredictos_a_banco.py desde el Admin
    ("revision_ia_ia_bien", re.compile(r"según la revisión \(veredicto ia_bien\)")),
    ("lector_propio_ia_mal", re.compile(r"según el lector del teléfono \(veredicto ia_mal\)")),
    ("captura_haiku_10_77", re.compile(r"confirmada con la captura \(Haiku")),
    ("lineas_revision_manual", re.compile(r"según revisión manual de las líneas")),
]
CIRCULAR = {"lector_propio_ia_mal"}  # la «verdad» es la salida del propio lector → no sirve para medirlo

def clasificar(carpeta=BANCO):
    filas = []
    for f in sorted(os.listdir(carpeta)):
        m = re.fullmatch(r"(\d{4})-(uber|didi|otra)\.txt", f)
        if not m:
            continue
        cab = open(os.path.join(carpeta, f), encoding="utf-8").read().split("\n---\n")[0]
        coment = "\n".join(l for l in cab.splitlines() if l.startswith("#"))
        ver = re.search(r"·\s*(\d+\.\d+)\s*·", coment)
        origen = next((o for o, rx in ORIGENES if rx.search(coment)), "desconocido")
        esperado = open(os.path.join(carpeta, f), encoding="utf-8").read().split("\n---\n")[-1].strip()
        filas.append({"caso": f[:-4], "app": m.group(2), "version": ver.group(1) if ver else "",
                      "origen": origen, "fuente_tipo": "humano" if origen == "veredicto_humano" else "heredado", "circular": "si" if origen in CIRCULAR else "no",
                      "humano_verificado": "si" if origen == "veredicto_humano" else "no", "esperado_nada": "si" if esperado == "nada" else "no"})
    return filas

def main():
    filas = clasificar()
    salida = os.path.join(os.path.dirname(os.path.abspath(__file__)), "banco_origen.csv")
    with open(salida, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=list(filas[0]), lineterminator="\n"); w.writeheader(); w.writerows(filas)
    cuenta = {}
    for r in filas:
        cuenta[r["origen"]] = cuenta.get(r["origen"], 0) + 1
    for k, v in sorted(cuenta.items(), key=lambda x: -x[1]):
        print(f"{v:4d}  {k}")
    print(f"{len(filas)} casos; humano verificado: 0; circulares: {sum(r['circular'] == 'si' for r in filas)}")
    return 0 if all(r["origen"] != "desconocido" for r in filas) else 1

if __name__ == "__main__":
    sys.exit(main())
