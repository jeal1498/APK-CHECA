# Fuente de los 251 IDs y comparación pendiente

**Hechos verificados en el repo**
- La lista NO existe como archivo independiente: se derivó de `pruebas/lector/v77/resultados_haiku_v77.json` (campo `lista`: «capturas 10.77 del almacén capturas (251 ids, ver prompt-v77.md)»; `resumen.leidas` = 251, `fallidas` = 0).
- Origen operativo: `pruebas/lector/prompt-v77.md`, paso 1 (commit `9d5a098`, 14-nov): `select o.name, e.id from storage.objects o join public.errores_lector e on e.id::text = split_part(o.name,'.',1) where o.bucket_id='capturas' and e.version='10.77' order by e.id;` — o sea, ids de `errores_lector` con versión 10.77 **que tenían objeto en Storage el 14-nov**. Resultados: commits `3923b53` (251 leídas) y `1a76c67` (segunda pasada de 1211–1380).
- 251 ids únicos, rango 947–1380 (183 huecos = otras versiones). Cinco ids marcados «verificar» (1343, 1344, 1345, 1353, 1360) por tarjeta dudosa.
- Solo cubre **dudosas** (`<id>.webp`). No incluye normales de la 10.77: sin `-n<id>` no se pueden atribuir a versión; con `-n<id>` empiezan en la 10.80.

**Lo que NO está comprobado:** que esos 251 sean *todos* los ids 10.77 con captura (pudo haber subidas posteriores al 14-nov, o capturas ya expiradas), ni que la consulta se ejecutó exactamente así. Es la lista vigente del repo, no una lista independiente.

**Comparación explícita (a ejecutar por el propietario, solo ids)**
```sql
select e.id from storage.objects o
join public.errores_lector e on e.id::text = split_part(o.name,'.',1)
where o.bucket_id='capturas' and e.version='10.77' order by e.id;
```
Exportar CSV (columna `id`) y: `python3 comparar_ids.py ../respaldo-e0/ids_requeridos_10.77.txt ids_vivos.csv`.
Salida: ids solo en la lista (ya no existen/no son 10.77) y solo vivos (no listados). Probado con datos sintéticos.
Además, ya dentro del procedimiento: `verificar.py` exige que cada id requerido tenga captura y fila 10.77 (V08b/V08c) y compara conteo y huella de ids 10.77 con la referencia SQL (R07/R08). Gap conocido: no informa ids 10.77 vivos que no estén en la lista; por eso esta comparación aparte. El respaldo guarda **todo** el bucket, así que esos ids extra quedarían respaldados igualmente.

**Decisión pedida al propietario:** confirmar la lista tras la comparación; si hay ids «solo en la lista» ya expirados, E0 debe documentarlos como pérdida, no ocultarlos.
