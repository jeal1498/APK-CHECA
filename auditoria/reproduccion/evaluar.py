#!/usr/bin/env python3
"""E5 — Reproducción histórica, regresión por caso y evaluación CIEGA.

Protocolo ciego (el orden lo impone `registro.json`; cada paso se niega si el anterior no está):
  1. preparar   separa un banco en ENTRADAS (solo líneas, sin «---» ni comentarios) y ETIQUETAS (+ origen).
  2. sellar     anota el sha256 de las etiquetas en el registro ANTES de correr el lector.
  3. (lector)   reproducir.sh corre la versión sobre las entradas → predicciones.jsonl. Nunca ve las etiquetas.
  4. comprometer anota el sha256 de las predicciones. Ya no se pueden cambiar.
  5. revelar    comprueba que etiquetas y predicciones son las selladas y recién entonces califica.
Regresión:  comparar BASE CANDIDATA ETIQUETAS → casos que antes acertaban y ahora no (sale con 1), mejoras y cambios.
"""
import argparse
import hashlib
import json
import os
import re
import sys
from datetime import datetime, timezone

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.dirname(os.path.dirname(AQUI))
sys.path.insert(0, os.path.join(RAIZ, "auditoria", "etiquetado"))
import metricas  # noqa: E402
import origen_banco  # noqa: E402

# Las mismas tolerancias que Banco.kt (las del servidor para «coinciden»). Las de metricas.TOL son las humanas, más estrictas.
TOL_BANCO = {"tarifa": 0.99, "km": 0.15, "min": 1.1, "recKm": 0.15, "recMin": 1.1, "multi": 0.01, "dinamica": 0.01}


def sha(p):
    return hashlib.sha256(open(p, "rb").read()).hexdigest()


def ahora():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


def leer_jsonl(p):
    return [json.loads(l) for l in open(p, encoding="utf-8") if l.strip()]


def parsear_esperado(s):
    s = s.strip()
    if s == "nada":
        return {"clase": "no_oferta"}
    d = {"clase": "oferta"}
    for par in s.split():
        k, v = par.split("=")
        if k not in TOL_BANCO:
            raise ValueError(f"dato desconocido {k}")
        d[k] = float(v)
    return d


def acierta_banco(pred, esp):
    """Misma regla que Banco.falla (tolerancias del banco, todos los datos esperados)."""
    if esp["clase"] == "no_oferta":
        return pred["resultado"] == "nada"
    if pred["resultado"] != "oferta":
        return False
    return all(abs(float(pred.get(k, 0.0)) - v) <= TOL_BANCO[k] for k, v in esp.items() if k != "clase")


# ---------------------------------------------------------------- preparar
def preparar(corpus, salida):
    """corpus: lista de (prefijo, carpeta, origen_por_defecto). El banco usa origen_banco; los demás, el dado."""
    ent = os.path.join(salida, "entradas")
    os.makedirs(ent, exist_ok=False)
    etiquetas = []
    for prefijo, carpeta, origen_def in corpus:
        origen = {c["caso"]: c for c in origen_banco.clasificar(carpeta)} if origen_def == "banco" else {}
        for f in sorted(os.listdir(carpeta)):
            if not f.endswith(".txt") or f == "minimo.txt":
                continue
            partes = open(os.path.join(carpeta, f), encoding="utf-8").read().replace("\r", "").split("\n---\n")
            if len(partes) != 2:
                raise SystemExit(f"{f}: falta ---")
            lineas = [l for l in partes[0].splitlines() if l.strip() and not l.startswith("#")]
            caso = (prefijo + "_" if prefijo else "") + f[:-4]
            with open(os.path.join(ent, caso + ".txt"), "w", encoding="utf-8") as g:
                g.write("\n".join(lineas) + "\n")
            o = origen.get(f[:-4], {})
            app = "uber" if "uber" in f else "didi" if "didi" in f else "otra"
            etiquetas.append({"caso": caso, "app": app, "esperado": parsear_esperado(partes[1]), "corpus": prefijo or "banco",
                              "origen": o.get("origen", origen_def if origen_def != "banco" else "desconocido"),
                              "circular": o.get("circular") == "si", "fuente": "sintetico" if origen_def.startswith("sintetico") else "heredado"})
    with open(os.path.join(salida, "etiquetas.jsonl"), "w", encoding="utf-8") as g:
        for e in etiquetas:
            g.write(json.dumps(e, ensure_ascii=False, sort_keys=True) + "\n")
    print(f"{len(etiquetas)} casos: entradas/ (ciegas) y etiquetas.jsonl")
    return 0


# ---------------------------------------------------------------- protocolo ciego
def _reg(p):
    return json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {"pasos": []}


def _guardar(p, r):
    with open(p, "w", encoding="utf-8") as f:
        json.dump(r, f, ensure_ascii=False, indent=2)


def sellar(etiquetas, registro):
    r = _reg(registro)
    if r["pasos"]:
        raise SystemExit("El registro ya tiene pasos: usa uno nuevo por evaluación.")
    r["pasos"].append({"paso": "sellar", "utc": ahora(), "etiquetas_sha256": sha(etiquetas), "n": len(leer_jsonl(etiquetas))})
    _guardar(registro, r)
    print("Etiquetas selladas:", r["pasos"][-1]["etiquetas_sha256"])
    return 0


def comprometer(predicciones, corrida, registro):
    r = _reg(registro)
    if [p["paso"] for p in r["pasos"]] != ["sellar"]:
        raise SystemExit("Hay que sellar las etiquetas antes (y comprometer una sola vez).")
    c = json.load(open(corrida, encoding="utf-8"))
    if c["predicciones_sha256"] != sha(predicciones):
        raise SystemExit("Las predicciones no son las de la corrida.")
    r["pasos"].append({"paso": "comprometer", "utc": ahora(), "predicciones_sha256": sha(predicciones), "corrida": c})
    _guardar(registro, r)
    print("Predicciones comprometidas:", r["pasos"][-1]["predicciones_sha256"])
    return 0


def calificar(preds, etqs, tol=None):
    por = {p["caso"]: p for p in preds}
    faltan = [e["caso"] for e in etqs if e["caso"] not in por]
    if faltan:
        raise SystemExit(f"Faltan predicciones para {len(faltan)} casos: {faltan[:5]}")
    def bloque(lista):
        ac = sum(acierta_banco(por[e["caso"]], e["esperado"]) for e in lista)
        pares = [(e["caso"], e["app"], por[e["caso"]], e["esperado"]) for e in lista]
        return {"n": len(lista), "acuerdo_banco": metricas.proporcion(ac, len(lista)),
                "categorias": metricas.resumir(pares, {k: TOL_BANCO[k] for k in ("tarifa", "km", "min")})["total"]}
    origenes = sorted({e["origen"] for e in etqs})
    return {
        "aviso": "Etiquetas HEREDADAS (IA / lector / líneas), no humanas: esto es acuerdo con esas etiquetas, no exactitud.",
        "total": bloque(etqs),
        "sin_circulares": bloque([e for e in etqs if not e["circular"]]),
        "por_app": {a: bloque([e for e in etqs if e["app"] == a]) for a in sorted({e["app"] for e in etqs})},
        "por_origen": {o: bloque([e for e in etqs if e["origen"] == o]) for o in origenes},
        "por_corpus": {c: bloque([e for e in etqs if e.get("corpus", "banco") == c]) for c in sorted({e.get("corpus", "banco") for e in etqs})},
        "fallas": sorted(e["caso"] for e in etqs if not acierta_banco(por[e["caso"]], e["esperado"])),
    }


def revelar(etiquetas, predicciones, registro, salida):
    r = _reg(registro)
    if [p["paso"] for p in r["pasos"]] != ["sellar", "comprometer"]:
        raise SystemExit("Orden inválido: sellar → (correr lector) → comprometer → revelar.")
    if sha(etiquetas) != r["pasos"][0]["etiquetas_sha256"]:
        raise SystemExit("Las etiquetas cambiaron después de sellarlas.")
    if sha(predicciones) != r["pasos"][1]["predicciones_sha256"]:
        raise SystemExit("Las predicciones cambiaron después de comprometerlas.")
    inf = calificar(leer_jsonl(predicciones), leer_jsonl(etiquetas))
    inf["registro"] = json.loads(json.dumps(r))  # copia: el paso «revelar» se agrega después, solo al registro
    r["pasos"].append({"paso": "revelar", "utc": ahora(), "informe_sha256": hashlib.sha256(
        json.dumps(inf, sort_keys=True, ensure_ascii=False).encode()).hexdigest()})
    _guardar(registro, r)
    with open(salida, "w", encoding="utf-8") as f:
        json.dump(inf, f, ensure_ascii=False, indent=2, sort_keys=True)
    t = inf["total"]["acuerdo_banco"]
    print(f"Acuerdo con etiquetas heredadas: {t['k']}/{t['n']} ({t['valor']}); sin circulares: "
          f"{inf['sin_circulares']['acuerdo_banco']['k']}/{inf['sin_circulares']['acuerdo_banco']['n']}")
    return 0


# ---------------------------------------------------------------- regresión
def comparar(base, cand, etiquetas, protegidos=None, salida=None):
    b, c = {p["caso"]: p for p in leer_jsonl(base)}, {p["caso"]: p for p in leer_jsonl(cand)}
    e = {x["caso"]: x for x in leer_jsonl(etiquetas)}
    if set(b) != set(c):
        raise SystemExit("Base y candidata no tienen los mismos casos.")
    reg, mej, cambios = [], [], []
    for k in sorted(b):
        if b[k] != c[k]:
            cambios.append(k)
        if k in e:
            ab, ac = acierta_banco(b[k], e[k]["esperado"]), acierta_banco(c[k], e[k]["esperado"])
            if ab and not ac:
                reg.append(k)
            if ac and not ab:
                mej.append(k)
    prot = set(open(protegidos).read().split()) if protegidos else set()
    rotos_prot = sorted(set(reg) & prot) if prot else reg
    r = {"casos": len(b), "cambian_salida": cambios, "regresiones": reg, "mejoras": mej,
         "aciertos_base": sum(acierta_banco(b[k], e[k]["esperado"]) for k in b if k in e),
         "aciertos_candidata": sum(acierta_banco(c[k], e[k]["esperado"]) for k in c if k in e),
         "protegidos_rotos": rotos_prot}
    if salida:
        with open(salida, "w", encoding="utf-8") as f:
            json.dump(r, f, ensure_ascii=False, indent=2)
    print(f"{len(b)} casos: {len(cambios)} cambian, {len(reg)} regresiones, {len(mej)} mejoras "
          f"({r['aciertos_base']} → {r['aciertos_candidata']})")
    return 1 if rotos_prot else 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    s = ap.add_subparsers(dest="cmd", required=True)
    x = s.add_parser("preparar"); x.add_argument("--corpus", action="append", required=True, help="prefijo=carpeta:origen (origen «banco» = según cabeceras)"); x.add_argument("--salida", required=True)
    x = s.add_parser("sellar"); x.add_argument("--etiquetas", required=True); x.add_argument("--registro", required=True)
    x = s.add_parser("comprometer"); x.add_argument("--predicciones", required=True); x.add_argument("--corrida", required=True); x.add_argument("--registro", required=True)
    x = s.add_parser("revelar"); x.add_argument("--etiquetas", required=True); x.add_argument("--predicciones", required=True); x.add_argument("--registro", required=True); x.add_argument("--salida", required=True)
    x = s.add_parser("comparar"); x.add_argument("--base", required=True); x.add_argument("--candidata", required=True); x.add_argument("--etiquetas", required=True); x.add_argument("--protegidos"); x.add_argument("--salida")
    a = ap.parse_args(argv)
    if a.cmd == "preparar":
        corpus = []
        for c in a.corpus:
            pre, resto = c.split("=", 1)
            carpeta, origen = resto.rsplit(":", 1)
            corpus.append((pre, carpeta, origen))
        return preparar(corpus, a.salida)
    if a.cmd == "sellar":
        return sellar(a.etiquetas, a.registro)
    if a.cmd == "comprometer":
        return comprometer(a.predicciones, a.corrida, a.registro)
    if a.cmd == "revelar":
        return revelar(a.etiquetas, a.predicciones, a.registro, a.salida)
    return comparar(a.base, a.candidata, a.etiquetas, a.protegidos, a.salida)


if __name__ == "__main__":
    sys.exit(main())
