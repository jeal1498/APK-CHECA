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
    (Así E4 puede empezar con las capturas de la revisión del banco, sin esperar al respaldo E0.)"""
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
        if caps is None:   # sin la carpeta de capturas: nombre coherente con el id y sha256 con forma válida
            if os.path.basename(r.get("captura", "")) != f"{int(caso[:4])}.webp" or not re.fullmatch(r"[0-9a-f]{64}", r.get("captura_sha256", "")):
                return None, "captura_invalida"
            cap = (r["captura"], r["captura_sha256"])
        else:
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
        return {"caso": caso, "id": int(caso[:4]), "app": caso[5:], "revisor": r["revisor"], "clase": clase, "tarifa": t, "km": k, "min": m,
                "tarjeta": int(tj) if tj else None, "confianza": conf, "sha": cap[1], "nota": (r.get("nota") or "")[:280]}, None
    except (ValueError, TypeError):
        return None, "formato"


def iguales(x, y):
    if x["clase"] != y["clase"] or x["tarjeta"] != y["tarjeta"] or x["app"] != y["app"]:
        return False
    return x["clase"] != "oferta" or all(abs(x[c] - y[c]) <= t + 1e-9 for c, t in metricas.TOL.items())


def sql_lit(v):
    if v is None:
        return "null"
    if isinstance(v, (int, float)):
        return repr(v)
    return "'" + str(v).replace("'", "''") + "'"


def importar(a):
    """Empareja a los dos revisores por el ID de la captura (no por «id-app»: si uno marca DiDi y el otro Uber es un
    desacuerdo en el campo app, no dos casos distintos). Con --sin-capturas no hace falta la carpeta de imágenes: cada
    hoja trae el sha256 que su navegador calculó de la imagen, y los dos revisores deben traer el MISMO."""
    caps = None if a.sin_capturas else capturas_de_manifiesto(a.manifiesto)
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
                if fila["id"] in por_rev.setdefault(fila["revisor"], {}):
                    errores.append({"hoja": os.path.basename(ruta), "linea": i, "error": "caso_repetido"})
                    continue
                por_rev[fila["revisor"]][fila["id"]] = fila
    revs = sorted(por_rev)
    if len(revs) != 2:
        raise SystemExit(f"Se necesitan exactamente 2 revisores distintos; hay {revs}.")
    A, B = por_rev[revs[0]], por_rev[revs[1]]
    distintas = sorted(c for c in set(A) & set(B) if A[c]["sha"] != B[c]["sha"])
    for c in distintas:   # no vieron la misma imagen: ninguna de las dos filas sirve
        errores.append({"hoja": "(cruce)", "linea": c, "error": "captura_distinta_entre_revisores"})
        del A[c], B[c]
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
        w.writerow(["caso", "clase", "app", "tarifa", "km", "min", "tarjeta", "n_revisores"])
        for c in ref:
            x = A[c]
            w.writerow([f"{c:04d}", x["clase"], x["app"], x["tarifa"] or "", x["km"] or "", x["min"] or "", x["tarjeta"] or "", 2])
    with open(os.path.join(a.salida, "conflictos.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["caso"] + [f"{r}_{k}" for r in revs for k in ("clase", "app", "tarifa", "km", "min", "tarjeta")])
        for c in conf:
            w.writerow([f"{c:04d}"] + [X[c][k] for X in (A, B) for k in ("clase", "app", "tarifa", "km", "min", "tarjeta")])
    with open(os.path.join(a.salida, "veredictos_humanos.sql"), "w", encoding="utf-8") as f:
        f.write("-- Veredictos humanos validados (generado por etiquetado.py importar). Aplicar solo con autorización.\n")
        for X in (A, B):
            for c in sorted(X):
                x = X[c]
                f.write("insert into auditoria.veredictos (error_lector_id, fuente, autor, guia_version, clase, tarifa, km, min, tarjeta, "
                        "confianza, estado, evidencia_sha256, nota) values (" + ", ".join(sql_lit(v) for v in (
                            c, "humano", x["revisor"], GUIA_VERSION, x["clase"], x["tarifa"], x["km"], x["min"], x["tarjeta"],
                            x["confianza"], "validado", x["sha"], x["nota"] or None)) + ");\n")
    with open(os.path.join(a.salida, "acuerdo.json"), "w", encoding="utf-8") as f:
        json.dump(acuerdo, f, ensure_ascii=False, indent=2)
    print(f"Comunes {len(comunes)}: referencia {len(ref)}, conflictos {len(conf)}; kappa clase {acuerdo['kappa_clase']}; "
          f"filas con error {len(errores)}")
    return 0


def importar_revision(a):
    """Una persona revisó propuestas de la IA en revisar.html (aprobar / corregir / rechazar).
    Resultado: referencia «IA revisada por persona» (no es doble revisión humana a ciegas). Las filas de control que
    la persona aprobó sin detectar la alteración se EXCLUYEN y se reportan: miden si la revisión fue real."""
    caps = capturas_de_manifiesto(a.manifiesto)
    filas = list(csv.DictReader(open(a.hoja, encoding="utf-8", newline="")))
    if not filas or "decision" not in filas[0]:
        raise SystemExit("La hoja no viene de revisar.html (falta la columna «decision»).")
    revs = {r.get("revisor") for r in filas}
    if len(revs) != 1:
        raise SystemExit(f"Una hoja de revisión debe ser de una sola persona; hay {sorted(revs)}.")
    cuenta = {"aprobado": 0, "corregido": 0, "rechazado": 0, "pendiente": 0}
    ctl = {"total": 0, "detectados": 0, "no_detectados": []}
    validas, ia, errores = [], [], []
    for i, r in enumerate(filas, 2):
        d = r.get("decision", "pendiente")
        cuenta[d] = cuenta.get(d, 0) + 1
        try:
            propuesta = json.loads(r.get("propuesta_ia") or "{}")
        except ValueError:
            propuesta = {}
        cap = caps.get(int(r["caso"][:4])) if re.fullmatch(r"\d{4}-(uber|didi|otra)?", r.get("caso", "")) else None
        if propuesta and cap and cap[0] == r.get("captura") and cap[1] == r.get("captura_sha256"):
            ia.append((r["caso"][:4], propuesta, cap[1]))
        if r.get("control") == "si":
            ctl["total"] += 1
            if d == "aprobado":
                ctl["no_detectados"].append(r["captura"])
                continue                      # aprobó una propuesta alterada: esta fila no vale
            ctl["detectados"] += 1
        if d not in ("aprobado", "corregido"):
            continue
        fila, err = validar_fila(r, caps)
        if err:
            errores.append({"linea": i, "error": err})
            continue
        fila["decision"] = d
        validas.append(fila)
    rev = revs.pop()
    os.makedirs(a.salida, exist_ok=True)
    with open(os.path.join(a.salida, "veredictos_revision.sql"), "w", encoding="utf-8") as f:
        f.write("-- Revisión de propuestas IA por una persona (revisar.html). Aplicar solo con autorización.\n"
                "-- Persona: fuente 'humano', revisor_version 'revision_propuesta_ia' (UN revisor: queda en 'referencia_humana',\n"
                "-- no en 'referencia_doble'). IA: fuente 'ia', estado 'propuesto'.\n")
        for x in validas:
            f.write("insert into auditoria.veredictos (error_lector_id, fuente, autor, revisor_version, guia_version, clase, tarifa, km, min, "
                    "tarjeta, confianza, estado, evidencia_sha256, nota) values (" + ", ".join(sql_lit(v) for v in (
                        int(x["caso"][:4]), "humano", x["revisor"], "revision_propuesta_ia:" + x["decision"], GUIA_VERSION, x["clase"],
                        x["tarifa"], x["km"], x["min"], x["tarjeta"], x["confianza"], "validado", x["sha"], x["nota"] or None)) + ");\n")
        for idl, p, sha in ia:
            clase = p.get("clase") if p.get("clase") in CLASES else "ilegible"
            of = clase == "oferta" and all(isinstance(p.get(k), (int, float)) and p.get(k) > 0 for k in ("tarifa", "km", "min"))
            clase = clase if clase != "oferta" or of else "ilegible"
            f.write("insert into auditoria.veredictos (error_lector_id, fuente, autor, revisor_version, clase, tarifa, km, min, confianza, estado, evidencia_sha256) values ("
                    + ", ".join(sql_lit(v) for v in (int(idl), "ia", "claude-opus", "prompt-opus-v1", clase,
                                                      p.get("tarifa") if of else None, p.get("km") if of else None, p.get("min") if of else None,
                                                      p.get("confianza") if p.get("confianza") in ("alta", "media", "baja") else "media", "propuesto", sha)) + ");\n")
    res = {"metodo": "revision_propuesta_ia", "revisor": rev, "filas": len(filas), "decisiones": cuenta,
           # Acuerdo IA–persona solo en filas SIN control (en las de control lo mostrado no era lo que dijo la IA).
           "acuerdo_ia_persona": metricas.proporcion(sum(1 for r in filas if r.get("control") != "si" and r.get("decision") == "aprobado"),
                                                     sum(1 for r in filas if r.get("control") != "si" and r.get("decision") in ("aprobado", "corregido", "rechazado"))),
           "controles": {"total": ctl["total"], "detectados": ctl["detectados"], "no_detectados": ctl["no_detectados"]},
           "revision_confiable": ctl["total"] > 0 and not ctl["no_detectados"],
           "validas": len(validas), "errores_de_hoja": errores,
           "aviso": "Propuesta de IA revisada por UNA persona: no es doble revisión humana a ciegas."}
    with open(os.path.join(a.salida, "revision.json"), "w", encoding="utf-8") as f:
        json.dump(res, f, ensure_ascii=False, indent=2)
    print(f"{rev}: {cuenta}; controles detectados {ctl['detectados']}/{ctl['total']}; válidas {len(validas)}; errores {len(errores)}")
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
    i.add_argument("--manifiesto", help="carpeta de capturas o manifiesto.csv (obligatorio salvo con --sin-capturas)")
    i.add_argument("--sin-capturas", action="store_true", help="no hay carpeta aquí: se confía en el sha256 que trae cada hoja y se exige que coincida entre los dos revisores")
    i.add_argument("--hoja", action="append", required=True)
    i.add_argument("--salida", required=True)
    r = sub.add_parser("importar-revision")
    r.add_argument("--manifiesto", required=True)
    r.add_argument("--hoja", required=True)
    r.add_argument("--salida", required=True)
    a = ap.parse_args(argv)
    if a.cmd == "importar-revision":
        return importar_revision(a)
    if a.cmd == "importar" and not a.sin_capturas and not a.manifiesto:
        raise SystemExit("Falta --manifiesto (o usa --sin-capturas).")
    if a.cmd == "hoja":
        a.revisor = a.revisor or ["revisor_a", "revisor_b"]
        return hoja(a)
    return importar(a)


if __name__ == "__main__":
    sys.exit(main())
