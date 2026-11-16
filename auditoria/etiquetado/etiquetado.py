#!/usr/bin/env python3
"""E4 — Etiquetado HUMANO verificable, con doble revisión.

  hoja      Elige una muestra estratificada (app × origen, semilla fija) y escribe una hoja CIEGA por revisor:
            caso, captura y su sha256 (del manifiesto del respaldo E0), sin lo que leyó el lector ni la IA.
  importar  Lee las hojas de 2 revisores, valida cada fila (captura con el sha256 del manifiesto, coherencia),
            mide el acuerdo (por campo y kappa de la clase), separa referencia y conflictos y genera el SQL de
            auditoria.veredictos (fuente 'humano', estado 'validado'). No aplica nada.

Las capturas NO están en el repo. `--manifiesto` acepta el manifiesto.csv de un respaldo **o** la carpeta con las
capturas «<id>.webp» que se bajan para la revisión del banco (fuera de git). Ver GUIA.md. Uso:
  python3 etiquetado.py hoja --manifiesto CARPETA_CAPTURAS --n 120 --semilla 20261115 --salida DIR
  python3 etiquetado.py importar --manifiesto CARPETA_CAPTURAS --hoja A.csv --hoja B.csv --salida DIR
"""
import argparse
import csv
import json
import os
import random
import re
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import metricas  # noqa: E402
import origen_banco  # noqa: E402

GUIA_VERSION = "1.0"
CAMPOS_HOJA = ["caso", "captura", "captura_sha256", "revisor", "guia_version", "clase", "tarifa", "km", "min", "tarjeta", "confianza", "nota"]
CLASES = ("oferta", "no_oferta", "varias", "ilegible", "cambio_pantalla")
RE_REVISOR = re.compile(r"^[a-z0-9_]{2,32}$")


def capturas_de_manifiesto(ruta):
    """{id_enlace(int): (nombre_relativo, sha256)} de las capturas dudosas («<id>.webp»).
    `ruta` puede ser el manifiesto.csv de un respaldo E0 **o** una carpeta con las capturas (p. ej. las que se bajan
    para la revisión del banco): en ese caso el sha256 se calcula aquí, de los bytes que el revisor va a mirar.
    (E0 se descartó por decisión del propietario el 16-nov: el etiquetado ya no depende del respaldo.)"""
    out = {}
    if os.path.isdir(ruta):
        import hashlib
        for n in sorted(os.listdir(ruta)):
            m = re.fullmatch(r"(\d+)\.webp", n)
            p = os.path.join(ruta, n)
            if m and os.path.isfile(p) and not os.path.islink(p):
                out[int(m.group(1))] = (n, hashlib.sha256(open(p, "rb").read()).hexdigest())
        return out
    with open(ruta, encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            if r.get("tipo") == "captura" and r.get("origen") == "dudosa" and r.get("id_enlace"):
                out[int(r["id_enlace"])] = (r["nombre"], r["sha256"])
    return out


def muestra(casos, n, semilla):
    """Estratificada por (app, origen), proporcional, al menos 1 por estrato con casos. Determinista."""
    rnd = random.Random(semilla)
    estratos = {}
    for c in sorted(casos, key=lambda x: x["caso"]):
        estratos.setdefault((c["app"], c["origen"]), []).append(c)
    total = len(casos)
    elegidos = []
    for k in sorted(estratos):
        lista = estratos[k]
        cuota = max(1, round(n * len(lista) / total)) if total else 0
        elegidos += rnd.sample(lista, min(cuota, len(lista)))
    rnd.shuffle(elegidos)  # el orden no revela el estrato
    return elegidos[:max(n, len(estratos))]


def hoja(a):
    caps = capturas_de_manifiesto(a.manifiesto) if a.manifiesto else {}
    casos = origen_banco.clasificar()
    con_captura = [c for c in casos if int(c["caso"][:4]) in caps]
    sin = len(casos) - len(con_captura)
    sel = muestra(con_captura, a.n, a.semilla)
    os.makedirs(a.salida, exist_ok=True)
    for rev in a.revisor:
        if not RE_REVISOR.match(rev):
            raise SystemExit(f"revisor inválido: {rev!r} (usa un seudónimo: minúsculas, números, _)")
        with open(os.path.join(a.salida, f"hoja-{rev}.csv"), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=CAMPOS_HOJA, lineterminator="\n")
            w.writeheader()
            for c in sel:
                nombre, sha = caps[int(c["caso"][:4])]
                w.writerow({"caso": c["caso"], "captura": nombre, "captura_sha256": sha, "revisor": rev, "guia_version": GUIA_VERSION})
    sel_info = {"semilla": a.semilla, "n_pedida": a.n, "n": len(sel), "casos_sin_captura_excluidos": sin,
                "estratos": sorted({f"{c['app']}/{c['origen']}" for c in sel}), "casos": sorted(c["caso"] for c in sel)}
    with open(os.path.join(a.salida, "muestra.json"), "w", encoding="utf-8") as f:
        json.dump(sel_info, f, ensure_ascii=False, indent=2)
    print(f"Muestra: {len(sel)} casos con captura ({sin} del banco sin captura en el respaldo, excluidos). Hojas: {', '.join(a.revisor)}")
    return 0


def _num(v):
    v = (v or "").strip().replace(",", ".")
    if not v:
        return None
    x = float(v)
    if not math_finito(x) or x <= 0:
        raise ValueError("número inválido")
    return x


def math_finito(x):
    return x == x and x not in (float("inf"), float("-inf"))


def validar_fila(r, caps):
    """Devuelve (fila_normalizada, None) o (None, error)."""
    try:
        if r.get("guia_version") != GUIA_VERSION:
            return None, "guia_version"
        if not RE_REVISOR.match(r.get("revisor", "")):
            return None, "revisor"
        caso = r.get("caso", "")
        if not re.fullmatch(r"\d{4}-(uber|didi|otra)", caso):
            return None, "caso"
        cap = caps.get(int(caso[:4]))
        if not cap or cap[0] != r.get("captura") or cap[1] != r.get("captura_sha256"):
            return None, "captura_no_coincide_con_manifiesto"
        clase = (r.get("clase") or "").strip()
        if clase not in CLASES:
            return None, "clase"
        t, k, m = _num(r.get("tarifa")), _num(r.get("km")), _num(r.get("min"))
        if clase == "oferta" and None in (t, k, m):
            return None, "oferta_sin_datos"
        if clase != "oferta" and any(x is not None for x in (t, k, m)):
            return None, "datos_sin_oferta"
        tj = (r.get("tarjeta") or "").strip()
        conf = (r.get("confianza") or "media").strip()
        if conf not in ("alta", "media", "baja"):
            return None, "confianza"
        return {"caso": caso, "revisor": r["revisor"], "clase": clase, "tarifa": t, "km": k, "min": m,
                "tarjeta": int(tj) if tj else None, "confianza": conf, "sha": cap[1], "nota": (r.get("nota") or "")[:280]}, None
    except (ValueError, TypeError):
        return None, "formato"


def iguales(x, y):
    if x["clase"] != y["clase"] or x["tarjeta"] != y["tarjeta"]:
        return False
    return x["clase"] != "oferta" or all(abs(x[c] - y[c]) <= t + 1e-9 for c, t in metricas.TOL.items())


def sql_lit(v):
    if v is None:
        return "null"
    if isinstance(v, (int, float)):
        return repr(v)
    return "'" + str(v).replace("'", "''") + "'"


def importar(a):
    caps = capturas_de_manifiesto(a.manifiesto)
    por_rev, errores = {}, []
    for ruta in a.hoja:
        with open(ruta, encoding="utf-8", newline="") as f:
            for i, r in enumerate(csv.DictReader(f), 2):
                if not (r.get("clase") or "").strip():
                    continue  # sin contestar
                fila, err = validar_fila(r, caps)
                if err:
                    errores.append({"hoja": os.path.basename(ruta), "linea": i, "error": err})
                    continue
                if fila["caso"] in por_rev.setdefault(fila["revisor"], {}):
                    errores.append({"hoja": os.path.basename(ruta), "linea": i, "error": "caso_repetido"})
                    continue
                por_rev[fila["revisor"]][fila["caso"]] = fila
    revs = sorted(por_rev)
    if len(revs) != 2:
        raise SystemExit(f"Se necesitan exactamente 2 revisores distintos; hay {revs}.")
    A, B = por_rev[revs[0]], por_rev[revs[1]]
    comunes = sorted(set(A) & set(B))
    ref, conf = [], []
    for c in comunes:
        (ref if iguales(A[c], B[c]) else conf).append(c)
    acuerdo = {"revisores": revs, "casos_comunes": len(comunes), "solo_uno": len(set(A) ^ set(B)),
               "acuerdo_total": metricas.proporcion(len(ref), len(comunes)),
               "kappa_clase": metricas.kappa([A[c]["clase"] for c in comunes], [B[c]["clase"] for c in comunes]),
               "acuerdo_por_campo": {}, "errores_de_hoja": errores}
    for campo, tol in metricas.TOL.items():
        ambos = [c for c in comunes if A[c]["clase"] == B[c]["clase"] == "oferta"]
        k = sum(abs(A[c][campo] - B[c][campo]) <= tol + 1e-9 for c in ambos)
        acuerdo["acuerdo_por_campo"][campo] = metricas.proporcion(k, len(ambos))
    os.makedirs(a.salida, exist_ok=True)
    with open(os.path.join(a.salida, "referencia_humana.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["caso", "clase", "tarifa", "km", "min", "tarjeta", "n_revisores"])
        for c in ref:
            x = A[c]
            w.writerow([c, x["clase"], x["tarifa"] or "", x["km"] or "", x["min"] or "", x["tarjeta"] or "", 2])
    with open(os.path.join(a.salida, "conflictos.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["caso"] + [f"{r}_{k}" for r in revs for k in ("clase", "tarifa", "km", "min", "tarjeta")])
        for c in conf:
            w.writerow([c] + [X[c][k] for X in (A, B) for k in ("clase", "tarifa", "km", "min", "tarjeta")])
    with open(os.path.join(a.salida, "veredictos_humanos.sql"), "w", encoding="utf-8") as f:
        f.write("-- Veredictos humanos validados (generado por etiquetado.py importar). Aplicar solo con autorización.\n")
        for X in (A, B):
            for c in sorted(X):
                x = X[c]
                f.write("insert into auditoria.veredictos (caso_banco, fuente, autor, guia_version, clase, tarifa, km, min, tarjeta, "
                        "confianza, estado, evidencia_sha256, nota) values (" + ", ".join(sql_lit(v) for v in (
                            c, "humano", x["revisor"], GUIA_VERSION, x["clase"], x["tarifa"], x["km"], x["min"], x["tarjeta"],
                            x["confianza"], "validado", x["sha"], x["nota"] or None)) + ");\n")
    with open(os.path.join(a.salida, "acuerdo.json"), "w", encoding="utf-8") as f:
        json.dump(acuerdo, f, ensure_ascii=False, indent=2)
    print(f"Comunes {len(comunes)}: referencia {len(ref)}, conflictos {len(conf)}; kappa clase {acuerdo['kappa_clase']}; "
          f"filas con error {len(errores)}")
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    h = sub.add_parser("hoja")
    h.add_argument("--manifiesto", required=True)
    h.add_argument("--n", type=int, default=120)
    h.add_argument("--semilla", type=int, default=20261115)
    h.add_argument("--revisor", action="append", default=None)
    h.add_argument("--salida", required=True)
    i = sub.add_parser("importar")
    i.add_argument("--manifiesto", required=True)
    i.add_argument("--hoja", action="append", required=True)
    i.add_argument("--salida", required=True)
    a = ap.parse_args(argv)
    if a.cmd == "hoja":
        a.revisor = a.revisor or ["revisor_a", "revisor_b"]
        return hoja(a)
    return importar(a)


if __name__ == "__main__":
    sys.exit(main())
