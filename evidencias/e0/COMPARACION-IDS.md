# E0 — Comparación de IDs requeridos con lo que existe (hecho verificado, solo lectura)

- **Cómo:** una sola consulta `select` (sin escrituras) vía el conector MCP de Supabase, proyecto `gqsadclvftkzwrihfipz`, `consultado_utc` = **2026-11-15T21:16:04Z**. Solo se leyeron conteos, huellas e ids numéricos; ningún dato personal.
- **Resultado:**
  | Dato | Valor |
  |---|---|
  | Objetos en `capturas` | 773 |
  | Filas `errores_lector` / de 10.77 | 1648 / 381 |
  | Filas `capturas_ok` | 403 |
  | Ids 10.77 con captura viva | **272** (`ids_vivos_10_77_20261115T211604Z.txt`) |
  | Ids requeridos (`ids_requeridos_10.77.txt`) | 251 |
  | Requeridos **ausentes** | **0** |
  | Vivos 10.77 **no listados** | 21: 1391–1393, 1441, 1443–1452, 1473, 1603, 1604, 1638, 1639, 1644, 1648 |
  | Objeto más viejo | 2026-11-13 08:39 UTC → empieza a expirar ~**2026-11-20** |
  | `huella_listado` / `huella_ids_errores_lector_10_77` | `b47e53e9…ab9b` / `b9c25582…0cc5` (válidas solo para ese instante) |
- **Lectura:** la lista de 251 está completa en el almacén hoy. Los 21 extra son subidas 10.77 posteriores al 14-nov; `respaldar.py` respalda **todo** el bucket, así que quedan incluidos aunque no sean «requeridos».
- **Decisión del propietario pendiente:** ampliar la lista a 272 (cambia `ids_requeridos_10.77.txt` y `SCRIPTS.sha256`) o mantener 251. No se cambió por iniciativa propia.
- **Esto NO es el respaldo:** no se descargó ninguna imagen ni fila; la huella sirve de referencia orientativa, no la del momento del respaldo.
