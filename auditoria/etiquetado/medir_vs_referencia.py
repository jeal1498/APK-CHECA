#!/usr/bin/env python3
"""Mide al lector contra la REFERENCIA HUMANA (referencia_final.csv). Solo lectura.
  python3 -I medir_vs_referencia.py REFERENCIA_FINAL.csv PREDICCIONES.jsonl SALIDA.json
Solo se miden los casos de la referencia que tienen predicción (líneas del OCR en el banco); el resto se informa como «sin líneas».
Tolerancias humanas de la guía. «varias» se cuenta aparte (lo correcto es abstenerse); «ilegible» queda fuera."""
import csv, json, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import metricas


def main(ref_csv, pred_jsonl, salida):
    ref = {}
    for r in csv.DictReader(open(ref_csv, encoding="utf-8")):
        ref[int(r["caso"])] = {"clase": r["clase"], "app": r["app"], "origen": r["origen"],
                               **{k: float(r[k]) for k in ("tarifa", "km", "min") if r[k]}}
    pred = {}
    for l in open(pred_jsonl, encoding="utf-8"):
        p = json.loads(l)
        if p["caso"][:4].isdigit():
            pred[int(p["caso"][:4])] = p
    sin_lineas = sorted(set(ref) - set(pred))
    pares, varias, errores = [], {"pop_up_sobre_varias": [], "se_abstuvo": []}, []
    for i in sorted(set(ref) & set(pred)):
        r, p = ref[i], pred[i]
        if r["clase"] == "varias":
            varias["pop_up_sobre_varias" if p["resultado"] == "oferta" else "se_abstuvo"].append(i)
            continue
        if r["clase"] == "ilegible":
            continue
        pares.append((f"{i:04d}", r["app"], p, r))
        cat, malos = metricas.categoria(p, r)
        if cat not in ("acierto", "verdadero_negativo"):
            errores.append({"caso": f"{i:04d}", "app": r["app"], "origen_ref": r["origen"], "categoria": cat, "campos": malos,
                            "lector": {k: p.get(k) for k in ("resultado", "tarifa", "km", "min")},
                            "humano": {k: r.get(k) for k in ("clase", "tarifa", "km", "min")}})
    res = metricas.resumir(pares)
    out = {"n_referencia": len(ref), "con_lineas": len(set(ref) & set(pred)), "sin_lineas": len(sin_lineas),
           "lector": res["total"], "por_app": res["por_app"], "referencia_varias": varias, "errores_del_lector": errores,
           "advertencias": ["Muestra de casos DIFÍCILES (errores_lector 10.77), no de producción.",
                            "La predicción sale de las líneas guardadas por el teléfono, no de la imagen.",
                            "Referencia adjudicada no es doble revisión a ciegas.",
                            "n < 30: insuficiente para concluir."]}
    json.dump(out, open(salida, "w", encoding="utf-8"), ensure_ascii=False, indent=2, sort_keys=True)
    t = res["total"]
    print(f"Referencia {len(ref)}; con líneas {out['con_lineas']}; evaluables {t['evaluables']}; conteos {t['conteos']}")
    for k in ("precision_ofertas", "tasa_fp", "tasa_fn", "exactitud"):
        v = t[k]; print(f"{k}: {v['k']}/{v['n']} = {v['valor']} IC95 {v['ic95']}")
    print("varias:", {k: len(v) for k, v in varias.items()})
    for e in errores: print(e["caso"], e["categoria"], e["campos"], e["lector"], e["humano"])


if __name__ == "__main__":
    main(*sys.argv[1:4])
