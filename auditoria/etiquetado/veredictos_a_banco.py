#!/usr/bin/env python3
"""Convierte los veredictos HUMANOS dados en el Admin en casos del banco del lector
(app/src/test/resources/banco/<id>-<app>.txt).

Por qué: hasta ahora la «respuesta esperada» de los casos nuevos salía de lo que leía Haiku sobre la captura
(una IA). Un veredicto humano validado contra la captura es una referencia mejor. El caso se escribe con el
mismo formato de siempre (líneas guardadas, `---`, `tarifa= km= min=` o `nada`) y con una cabecera que
`origen_banco.py` reconoce como «veredicto_humano» (humano_verificado = si).

Uso:
  1) En el editor SQL de Supabase, ejecuta y exporta el resultado como JSON:
       select coalesce(json_agg(t), '[]') from (
         select e.id, e.app, e.version, e.tipo, e.lineas, v.estado, v.clase, v.tarifa, v.km, v.min
         from public.errores_lector e
         join auditoria.veredicto_vigente v on v.error_lector_id = e.id
         where v.estado in ('referencia_humana', 'referencia_doble', 'referencia_adjudicada')
       ) t;
  2) python3 auditoria/etiquetado/veredictos_a_banco.py veredictos.json [--simular] [--reemplazar]

Reglas:
  - Solo estados de referencia humana; `solo_ia` y `heredado_*` nunca entran.
  - clase `oferta` (con tarifa, km y min > 0) → `tarifa= km= min=`; clase `no_oferta` → `nada`.
  - `varias`, `ilegible` y `cambio_pantalla` NO se agregan: la respuesta no se saca de las líneas guardadas.
  - No sobrescribe un caso que ya existe (usa --reemplazar para sustituirlo a propósito).
  - No toca `minimo.txt`: súbelo tú después de medir (ver docs/banco/prompt-correccion.md).
Las líneas se copian tal como están guardadas, igual que en los casos anteriores del banco."""
import argparse, json, os, re, sys

RAIZ = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
BANCO = os.path.join(RAIZ, "app", "src", "test", "resources", "banco")
REFERENCIAS = {"referencia_humana", "referencia_doble", "referencia_adjudicada"}
APPS = {"uber": "Uber", "didi": "DiDi", "otra": "Otra"}


def _num(v):
    return ("%f" % float(v)).rstrip("0").rstrip(".")


def _lineas(fila):
    ls = fila.get("lineas")
    if isinstance(ls, str):
        try:
            ls = json.loads(ls)
        except ValueError:
            ls = ls.splitlines()
    return [str(l).strip() for l in (ls or []) if str(l).strip()]


def caso(fila):
    """Devuelve (nombre_de_archivo, texto) o (None, motivo)."""
    if fila.get("estado") not in REFERENCIAS:
        return None, "sin referencia humana (%s)" % fila.get("estado")
    clase = fila.get("clase")
    if clase not in ("oferta", "no_oferta"):
        return None, "clase %s: no se saca de las líneas" % clase
    if clase == "oferta":
        try:
            t, k, m = (float(fila[x]) for x in ("tarifa", "km", "min"))
        except (KeyError, TypeError, ValueError):
            return None, "oferta sin tarifa, km o min"
        if not (t > 0 and k > 0 and m > 0):
            return None, "oferta con valores no positivos"
        esperado = "tarifa=%s km=%s min=%s" % (_num(t), _num(k), _num(m))
    else:
        esperado = "nada"
    lineas = _lineas(fila)
    if not lineas:
        return None, "sin líneas guardadas"
    app = str(fila.get("app", "")).strip().lower()
    app = app if app in APPS else "otra"
    try:
        ident = int(fila["id"])
    except (KeyError, TypeError, ValueError):
        return None, "sin id"
    cab = ["# errores_lector %d · %s · %s · %s" % (ident, APPS[app], fila.get("version") or "?", fila.get("tipo") or "?"),
           "# Respuesta correcta según veredicto humano (%s)." % fila["estado"]]
    return "%04d-%s.txt" % (ident, app), "\n".join(cab + lineas + ["---", esperado]) + "\n"


def convertir(filas, salida=BANCO, simular=False, reemplazar=False):
    r = {"nuevos": [], "reemplazados": [], "existentes": [], "omitidos": []}
    for f in filas:
        nombre, texto = caso(f)
        if nombre is None:
            r["omitidos"].append((f.get("id"), texto))
            continue
        ruta = os.path.join(salida, nombre)
        existe = os.path.exists(ruta)
        if existe and not reemplazar:
            r["existentes"].append(nombre)
            continue
        (r["reemplazados"] if existe else r["nuevos"]).append(nombre)
        if not simular:
            os.makedirs(salida, exist_ok=True)
            with open(ruta, "w", encoding="utf-8") as h:
                h.write(texto)
    return r


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("entrada", help="JSON exportado de Supabase (ver el encabezado de este archivo)")
    p.add_argument("--salida", default=BANCO)
    p.add_argument("--simular", action="store_true", help="no escribe nada, solo cuenta")
    p.add_argument("--reemplazar", action="store_true")
    a = p.parse_args(argv)
    with open(a.entrada, encoding="utf-8") as h:
        filas = json.load(h)
    r = convertir(filas, a.salida, a.simular, a.reemplazar)
    print("%s%d casos nuevos, %d reemplazados, %d ya existían, %d omitidos" % (
        "(simulación) " if a.simular else "", len(r["nuevos"]), len(r["reemplazados"]), len(r["existentes"]), len(r["omitidos"])))
    for i, motivo in r["omitidos"]:
        print("  omitido %s: %s" % (i, motivo))
    if r["nuevos"] or r["reemplazados"]:
        print("Siguiente: mide el banco, sube banco/minimo.txt si corresponde y corre auditoria/etiquetado/origen_banco.py.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
