"""Comparación explícita: lista de ids requeridos (del repo) vs ids 10.77 vivos en Supabase.
Entrada viva = CSV de UNA columna `id` exportado por el propietario con la consulta de IDS-FUENTE.md
(solo ids, sin datos personales). Salida: diferencias en ambos sentidos. No usa red."""
import csv, json, sys
req = [int(l) for l in open(sys.argv[1]) if l.strip()]
vivos = [int(r["id"]) for r in csv.DictReader(open(sys.argv[2]))]
a, b = set(req), set(vivos)
print(json.dumps({"requeridos": len(a), "vivos_10_77_con_captura": len(b), "en_lista_y_vivos": len(a & b),
                  "solo_en_lista(ya no existen o no son 10.77)": sorted(a - b), "solo_vivos(no estaban en la lista)": sorted(b - a)}, indent=1, ensure_ascii=False))
