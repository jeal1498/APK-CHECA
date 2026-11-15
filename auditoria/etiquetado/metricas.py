"""Métricas del lector contra una REFERENCIA (E4/E5/E7). Biblioteca sin dependencias.

Cada caso compara la predicción del lector con la referencia y cae en UNA categoría:
  acierto            lector y referencia dicen oferta y tarifa, km y min coinciden (tolerancias abajo)
  error_datos        los dos dicen oferta pero algún dato difiere (se detalla qué campo)
  falso_positivo     el lector dio oferta y la referencia dice no_oferta
  falso_negativo     el lector no dio nada y la referencia dice oferta
  verdadero_negativo el lector no dio nada y la referencia dice no_oferta
  abstencion         el lector se abstuvo («varias»/«abstencion») — no es acierto ni error
  sin_referencia     la referencia es ilegible, cambio_pantalla, conflicto o falta → fuera de los denominadores
Denominadores (definiciones explícitas, ver auditoria/METRICAS.md):
  precision_ofertas  = acierto / (acierto + error_datos + falso_positivo)      «cuando muestra un pop-up, ¿es correcto?»
  exactitud          = (acierto + verdadero_negativo) / evaluables_no_abstenidos
  tasa_fp            = falso_positivo / referencias no_oferta
  tasa_fn            = falso_negativo / referencias oferta
  abstenciones       = abstencion / evaluables
Cada proporción lleva n e intervalo de Wilson al 95 %; con n < MIN_N se marca «insuficiente».
"""
import math

TOL = {"tarifa": 0.01, "km": 0.05, "min": 0.5}
MIN_N = 30
CATEGORIAS = ("acierto", "error_datos", "falso_positivo", "falso_negativo", "verdadero_negativo", "abstencion", "sin_referencia")


def wilson(k, n, z=1.96):
    if n == 0:
        return None
    p = k / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    m = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return [round(max(0.0, c - m), 4), round(min(1.0, c + m), 4)]


def proporcion(k, n):
    return {"k": k, "n": n, "valor": None if n == 0 else round(k / n, 4), "ic95": wilson(k, n),
            "suficiente": n >= MIN_N}


def categoria(pred, ref):
    """pred: {'resultado': 'oferta'|'nada'|'varias'|'abstencion', tarifa, km, min}; ref: {'clase', tarifa, km, min} o None."""
    if not ref or ref.get("clase") not in ("oferta", "no_oferta"):
        return "sin_referencia", []
    r = (pred or {}).get("resultado", "nada")
    if r in ("varias", "abstencion"):
        return "abstencion", []
    if ref["clase"] == "no_oferta":
        return ("falso_positivo", []) if r == "oferta" else ("verdadero_negativo", [])
    if r != "oferta":
        return "falso_negativo", []
    malos = [c for c, t in TOL.items() if pred.get(c) is None or ref.get(c) is None or abs(float(pred[c]) - float(ref[c])) > t + 1e-9]
    return ("acierto", []) if not malos else ("error_datos", malos)


def resumir(pares):
    """pares: lista de (caso, app, pred, ref). Devuelve métricas globales y por app."""
    def bloque(lista):
        c = {k: 0 for k in CATEGORIAS}
        campos = {k: 0 for k in TOL}
        for _, _, p, r in lista:
            cat, malos = categoria(p, r)
            c[cat] += 1
            for m in malos:
                campos[m] += 1
        evaluables = sum(c.values()) - c["sin_referencia"]
        ref_of = c["acierto"] + c["error_datos"] + c["falso_negativo"]
        ref_no = c["falso_positivo"] + c["verdadero_negativo"]
        mostrados = c["acierto"] + c["error_datos"] + c["falso_positivo"]
        return {
            "conteos": c, "errores_por_campo": campos, "evaluables": evaluables,
            "precision_ofertas": proporcion(c["acierto"], mostrados),
            "exactitud": proporcion(c["acierto"] + c["verdadero_negativo"], evaluables - c["abstencion"]),
            "tasa_fp": proporcion(c["falso_positivo"], ref_no),
            "tasa_fn": proporcion(c["falso_negativo"], ref_of),
            "abstenciones": proporcion(c["abstencion"], evaluables),
        }
    apps = sorted({a for _, a, _, _ in pares})
    return {"total": bloque(pares), "por_app": {a: bloque([x for x in pares if x[1] == a]) for a in apps}, "min_n": MIN_N}


def kappa(a, b):
    """Kappa de Cohen entre dos listas de etiquetas del mismo largo (acuerdo más allá del azar)."""
    if len(a) != len(b) or not a:
        return None
    n = len(a)
    po = sum(x == y for x, y in zip(a, b)) / n
    cats = set(a) | set(b)
    pe = sum((a.count(k) / n) * (b.count(k) / n) for k in cats)
    return 1.0 if pe == 1 else round((po - pe) / (1 - pe), 4)
