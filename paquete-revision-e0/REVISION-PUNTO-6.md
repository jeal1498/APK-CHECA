# Independencia de funciones temporales, políticas y producción (punto 6)
Comprobado con búsqueda en `respaldo-e0/` (commit e6a05ec):
- `grep -rniE "deploy|edge.?function|CREATE POLICY|ALTER |DROP |INSERT |UPDATE |DELETE" respaldo-e0/*.py respaldo-e0/referencia.sql` → ver `logs/punto6-grep.txt`.
- El procedimiento solo usa: `POST /storage/v1/object/list/`, `GET /storage/v1/object/authenticated/`, `GET /rest/v1/errores_lector|capturas_ok` (lista blanca en `respaldar.py`, `PERMITIDO`), y SQL `select` manual del propietario. No despliega funciones, no toca políticas, RLS, retención ni Storage.
- No se desactiva `limpiarCapturas`: el respaldo convive con ella y lo registra (listado T0/T1).
- Limitación: la ausencia de escrituras es del **código**; nada se ejecutó contra Supabase real.
