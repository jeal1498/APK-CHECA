# E0.1 — Respaldo histórico 10.77: instrucciones para Eduardo

Todo se ejecuta **en tu equipo**, con **tu** llave. Nunca me mandes la llave, ni imágenes, ni la carpeta del respaldo.
Los scripts solo **leen** Supabase (no hay ningún borrado ni escritura en el código).

**Urgente:** las capturas de la 10.77 empiezan a borrarse solas el **20 nov**. Haz los pasos 1–6 antes.

## Antes de empezar (5 min)
- Python 3.9+, `age` (https://age-encryption.org) o `gpg`, y ~1 GB libres.
- Una carpeta de trabajo **fuera del repositorio** y **fuera de Dropbox/OneDrive/iCloud** (guardará capturas con direcciones: es privada). Ejemplo: `~/e0` (en Windows `C:\e0`).
- Los scripts de `respaldo-e0/` (esta carpeta). Anota sus hashes: `sha256sum respaldo-e0/*.py respaldo-e0/referencia.sql` (en Windows: `Get-FileHash`). El resumen los registra; debo ver que son los auditados.
- La llave `service_role` del panel de Supabase (Project Settings → API). Solo en tu terminal.

## Paso 1 de 8 — Poner la llave (1 min, solo en esa terminal)
- Mac/Linux: `read -rs SUPABASE_SERVICE_ROLE_KEY; export SUPABASE_SERVICE_ROLE_KEY` (pega, Enter; no se ve).
- Windows PowerShell: `$env:SUPABASE_SERVICE_ROLE_KEY = Read-Host "llave"` .
- No la pongas en un archivo, ni en el comando, ni en el chat.

## Paso 2 de 8 — Prueba sin descargar nada (1 min)
```
cd respaldo-e0
python3 respaldar.py --solo-listar
```
Debe imprimir `objetos` (≥ 738), `requeridos: 251` y `requeridos_ausentes: 0`. Si `requeridos_ausentes` > 0, **detente** y avísame con ese número (no la llave).

## Paso 3 de 8 — Respaldo real (5–15 min)
```
python3 respaldar.py --salida ~/e0/respaldo-1115
```
Termina con `Resultado: COMPLETO` (o `COMPLETO_CON_ADVERTENCIAS`). Si dice `INCOMPLETO`, **no sigas**: avísame. Puedes quitar la llave de la terminal: `unset SUPABASE_SERVICE_ROLE_KEY` (Windows: `Remove-Item Env:SUPABASE_SERVICE_ROLE_KEY`).

## Paso 4 de 8 — Consulta de referencia (3 min, justo después del paso 3)
1. Supabase → SQL Editor, con tu sesión. Pega **solo la consulta A** de `referencia.sql` → Run → exporta CSV → guárdalo como `~/e0/ref_objetos.csv`.
2. Igual con la **consulta B** → `~/e0/ref_tablas.csv` (una fila).
3. El editor limita a 1,000 filas; si el CSV de A tiene ≥ 1,000 filas, avísame antes de seguir.

## Paso 5 de 8 — Verificar (1 min)
```
python3 verificar.py --dir ~/e0/respaldo-1115 --ref-objetos ~/e0/ref_objetos.csv --ref-tablas ~/e0/ref_tablas.csv
```
Debe decir `APROBADO`. Si dice `FALLO`, no cifres nada todavía: avísame qué comprobación falló (ids `V…`/`R…`). `APROBADO_CON_ADVERTENCIAS` se acepta solo si me explicas cada advertencia.

## Paso 6 de 8 — Dos copias cifradas (10 min)
1. Empaqueta (tar determinista, queda **sin cifrar**: bórralo en el paso siguiente):
   `python3 evidencia_copias.py empaquetar --dir ~/e0/respaldo-1115 --salida ~/e0/respaldo.tar`
2. Cifra **dos veces por separado**, cada vez con frase/clave distinta (guárdalas en tu gestor de contraseñas, no junto a la copia):
   `age -p -o ~/e0/copiaA.tar.age ~/e0/respaldo.tar` y `age -p -o ~/e0/copiaB.tar.age ~/e0/respaldo.tar`
   (gpg: `gpg -c -o copiaA.tar.gpg respaldo.tar`, igual para B.)
3. Copia **A** a un medio (p. ej. disco externo) y **B** a otro **distinto** (p. ej. otra nube o USB en otro lugar). Conserva `respaldo.tar.empaquetado.json`.
4. Borra el tar en claro: `shred -u ~/e0/respaldo.tar` (Mac: `rm -P`; Windows: `cipher /w` o bórralo y vacía la papelera).

## Paso 7 de 8 — Restauración de prueba desde la copia B (5 min)
Usa el archivo **tal como está en el medio B** (cópialo de ahí, no del disco de trabajo):
```
python3 evidencia_copias.py restaurar --copia RUTA_A_LA_COPIA_B --etiqueta B --destino ~/e0/prueba-B \
  --empaquetado ~/e0/respaldo.tar.empaquetado.json --ref-objetos ~/e0/ref_objetos.csv --ref-tablas ~/e0/ref_tablas.csv \
  --salida ~/e0/RESTAURACION-B.json
```
Te pedirá la frase de B. Debe decir `Restauración B: OK`. No toca el original (restaura en una carpeta nueva). Recomendado: repetir con la copia A (`--etiqueta A`).
```
python3 evidencia_copias.py registrar --copia-a RUTA_COPIA_A --copia-b RUTA_COPIA_B \
  --medio-a "disco externo casa" --medio-b "nube cuenta aparte" \
  --restauracion-b ~/e0/RESTAURACION-B.json --confirmo-conservar --salida ~/e0/EVIDENCIA-COPIAS.json
```
Los textos de `--medio-*` son libres: **sin** datos sensibles. `--confirmo-conservar` es tu declaración de que conservas ambas copias.

## Paso 8 de 8 — Qué me entregas (y qué NO)
Entrégame **solo** estos 4 archivos (no tienen llaves, usuarios, correos ni imágenes):
1. `resumen.json` 2. `manifiesto.csv` 3. `VERIFICACION.json` (los tres están dentro de `~/e0/respaldo-1115/`) 4. `EVIDENCIA-COPIAS.json`.

El cuarto no está en la lista original de tres: lo necesito para comprobar las dos copias y la restauración (criterios de E0). Dime si lo autorizas.

**Nunca** me mandes: la llave, las frases de cifrado, `tablas/*.jsonl`, `capturas/`, los `.age`/`.gpg`, ni los CSV de referencia (si prefieres, puedes mandarlos: solo traen nombres y tamaños).

Yo corro `python3 verificar_entrega.py --dir <entrega>` y te devuelvo el resultado. **E0 no se cierra** hasta que eso dé APROBADO con la evidencia de copias.

## Si algo sale mal
- `ERROR: …URL` / `rol «anon»`: usaste una llave equivocada; usa la `service_role`.
- `Sin respuesta tras 4 intentos`: red; vuelve a lanzar con una carpeta **nueva** (el script no sobrescribe ni reanuda).
- `INCOMPLETO`: faltan capturas requeridas (¿ya expiraron?). No reintentes en bucle: avísame con el conteo.
- Dudas sobre una llave filtrada: rótala en Supabase y avísame solo que lo hiciste.

## Qué valida y qué no
- Valida: tamaños y sha256 de cada archivo, formato WebP, filas y huella de ids de las tablas, 251 ids requeridos, coincidencia con la consulta SQL del mismo momento, restauración desde B, que el manifiesto restaurado sea el original.
- **No** valida: que las capturas sean las «correctas» para el banco; que no hayan cambiado entre el listado y la consulta SQL (si hubo actividad, aparece como advertencia); la independencia física de los medios ni que conserves las copias (son declaraciones tuyas); RLS ni permisos reales de Supabase.

## Pruebas del procedimiento (para el revisor)
`cd respaldo-e0 && python3 -m unittest discover -s pruebas -v` — 44 pruebas contra un servidor **simulado** (no Supabase real).
