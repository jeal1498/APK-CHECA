# E0.3 — Respaldo histórico 10.77: instrucciones para Eduardo

> **DESCARTADO por decisión del propietario (16-nov-2026).** No se va a ejecutar para la 10.77. El procedimiento queda probado y disponible por si se quiere un respaldo más adelante (cambiaría la versión del banco y la lista de ids).

Todo se ejecuta **en tu equipo**, con **tu** llave. Nunca me mandes la llave, ni imágenes, ni la carpeta del respaldo.
Los scripts solo **leen** Supabase (no hay ningún borrado ni escritura en el código).

Las capturas de la 10.77 empiezan a borrarse solas el **20 nov**.

## Sistemas
- **Probado: solo Linux** (Python 3.13, age 1.1.1, gpg 2.4.4). 
- **macOS y Windows: NO probados.** Los comandos equivalentes de abajo están escritos, no ejecutados. Si algo falla ahí, detente y avisa el mensaje exacto; lo más seguro en Windows es usar WSL (Ubuntu) y seguir los pasos de Linux.

## Antes de empezar (5 min)
- Python 3.9+, `age` (https://age-encryption.org) o `gpg`, y ~1 GB libres.
- Una carpeta de trabajo **fuera del repositorio** y **fuera de Dropbox/OneDrive/iCloud** (guardará capturas con direcciones: es privada), de preferencia en un **volumen cifrado** (FileVault, BitLocker o LUKS). Ejemplo: `~/e0` (en Windows `C:\e0`).
- Los scripts de `respaldo-e0/` (esta carpeta). Comprueba que son los auditados: `cd respaldo-e0 && sha256sum -c SCRIPTS.sha256` (macOS: `shasum -a 256 -c SCRIPTS.sha256`; Windows: `Get-FileHash` de cada archivo y compara a mano). `respaldar.py` también lo comprueba y se niega a correr si no coinciden.
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
Debe imprimir `objetos` (≥ 738), `requeridos: 251`, `requeridos_ausentes: 0` y `nombres_no_permitidos: 0`. Si alguno de los dos últimos es mayor que 0, **detente** y avísame con ese número (no la llave).

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

**Inventario:** la advertencia `V08d` lista las capturas 10.77 que se respaldaron y **no** están en la lista de 251. No es un error del respaldo (se guarda todo el almacén): es la reconciliación del inventario. Tú decides si la lista de requeridos se amplía; E0 no se cierra con `V08d` sin esa decisión por escrito.

## Paso 6 de 8 — Dos copias cifradas (10 min)
1. Empaqueta (vuelve a verificar el respaldo y se niega si no verifica; tar determinista, queda **sin cifrar**):
   `python3 evidencia_copias.py empaquetar --dir ~/e0/respaldo-1115 --salida ~/e0/respaldo.tar`
2. Cifra **dos veces por separado**, cada vez con frase/clave distinta (guárdalas en tu gestor de contraseñas, no junto a la copia):
   `age -p -o ~/e0/copiaA.tar.age ~/e0/respaldo.tar` y `age -p -o ~/e0/copiaB.tar.age ~/e0/respaldo.tar`
   (gpg: `gpg -c -o copiaA.tar.gpg respaldo.tar`, igual para B.)
3. Copia **A** a un medio (p. ej. disco externo) y **B** a otro **distinto** (p. ej. otra nube o USB en otro lugar). Conserva `respaldo.tar.empaquetado.json`.
4. Borra el tar en claro (`rm ~/e0/respaldo.tar`; Windows: `Remove-Item`). **Esto no garantiza un borrado seguro:** en discos SSD y en sistemas de archivos con registro o instantáneas, ni `rm` ni `shred` aseguran que los datos desaparezcan. La protección real es que la carpeta de trabajo esté en un volumen cifrado; si no lo está, trátalo como dato que sigue en ese disco. Lo mismo vale para `~/e0/respaldo-1115` y para las carpetas de prueba de restauración cuando ya no las necesites.

## Paso 7 de 8 — Restauración de prueba desde la copia B y comprobación de la A (5 min)
Usa cada archivo **tal como está en su medio** (cópialo de ahí, no del disco de trabajo):
```
python3 evidencia_copias.py restaurar --copia RUTA_A_LA_COPIA_B --etiqueta B --destino ~/e0/prueba-B \
  --empaquetado ~/e0/respaldo.tar.empaquetado.json --ref-objetos ~/e0/ref_objetos.csv --ref-tablas ~/e0/ref_tablas.csv \
  --salida ~/e0/RESTAURACION-B.json
python3 evidencia_copias.py comprobar --copia RUTA_A_LA_COPIA_A --etiqueta A \
  --empaquetado ~/e0/respaldo.tar.empaquetado.json --salida ~/e0/COMPROBACION-A.json
```
Te pedirá la frase de cada copia. Debe decir `Restauración B: OK` y `Comprobación A: OK`.
- `restaurar` descifra B, comprueba que el tar es el empaquetado, lo extrae en una carpeta **nueva** y corre la verificación completa sobre lo restaurado (tamaños, sha256, WebP, tablas, ids). El tar descifrado temporal se escribe junto a `--destino` (no en el temporal del sistema) y se quita al terminar (sin borrado seguro: ver paso 6.4).
- `comprobar` descifra A **sin escribir nada en claro** y comprueba que su contenido es el mismo tar. Sin esto no hay prueba de que la copia A sirva.

Luego registra la evidencia:
```
python3 evidencia_copias.py registrar --copia-a RUTA_COPIA_A --copia-b RUTA_COPIA_B \
  --medio-a "disco externo casa" --medio-b "nube cuenta aparte" \
  --restauracion-b ~/e0/RESTAURACION-B.json --comprobacion-a ~/e0/COMPROBACION-A.json \
  --confirmo-conservar --salida ~/e0/EVIDENCIA-COPIAS.json
```
Debe decir `Evidencia de copias: COMPLETA`. Los textos de `--medio-*` son libres: **sin** datos sensibles. `--confirmo-conservar` es tu declaración de que conservas ambas copias.

## Paso 8 de 8 — Qué me entregas (y qué NO)
Entrégame **solo** estos 4 archivos (no tienen llaves, usuarios, correos ni imágenes):
1. `resumen.json` 2. `manifiesto.csv` 3. `VERIFICACION.json` (los tres están dentro de `~/e0/respaldo-1115/`) 4. `EVIDENCIA-COPIAS.json`.

El cuarto quedó autorizado en la revisión de E0.1 (punto 4 del paquete de revisión).

**Nunca** me mandes: la llave, las frases de cifrado, `tablas/*.jsonl`, `capturas/`, los `.age`/`.gpg`, ni los CSV de referencia (si prefieres, puedes mandarlos: solo traen nombres y tamaños).

Yo corro `python3 verificar_entrega.py --dir <entrega>` y te devuelvo el resultado. **E0 no se cierra** hasta que eso dé APROBADO con la evidencia de copias.

## Si algo sale mal
- `ERROR: …URL` / `rol «anon»`: usaste una llave equivocada; usa la `service_role`.
- `Sin respuesta tras 4 intentos`: red; vuelve a lanzar con una carpeta **nueva** (el script no sobrescribe ni reanuda).
- `INCOMPLETO`: faltan capturas requeridas (¿ya expiraron?). No reintentes en bucle: avísame con el conteo.
- Dudas sobre una llave filtrada: rótala en Supabase y avísame solo que lo hiciste.

## Qué valida y qué no
- Valida: rutas del manifiesto dentro del respaldo, tamaños y sha256 de cada archivo, formato WebP, filas y huella de ids de las tablas, 251 ids requeridos, capturas 10.77 fuera de la lista (V08d), coincidencia con la consulta SQL del mismo momento, restauración desde B, que la copia A contiene el mismo tar, que el manifiesto restaurado sea el original, y que todos los artefactos sean de la misma versión del procedimiento.
- **No** valida: que las capturas sean las «correctas» para el banco; que no hayan cambiado entre el listado y la consulta SQL (si hubo actividad, aparece como advertencia); la independencia física de los medios ni que conserves las copias (son declaraciones tuyas); RLS ni permisos reales de Supabase.

## Pruebas del procedimiento (para el revisor)
`cd respaldo-e0 && python3 -m unittest discover -s pruebas -v` — 86 pruebas contra un servidor **simulado** (no Supabase real). El número lo comprueba `test_d4_readme_declara_el_numero_real_de_pruebas` (D4: antes decía 44 y había 49).

## Integridad de los scripts (D3)
`SCRIPTS.sha256` fija el sha256 de los 7 archivos del procedimiento. Compruébalo antes de nada: `sha256sum -c SCRIPTS.sha256` (en Windows: `certutil -hashfile ARCHIVO SHA256`). `respaldar.py` se niega a correr si algún archivo no coincide o falta; `verificar.py` (V11/V11b), `verificar_entrega.py` (E12/E12b/E12c/C09) y `evidencia_copias.py` exigen que cada artefacto traiga las huellas de los 7 y que sean las fijadas. Si cambias un script, hay que volver a fijar `SCRIPTS.sha256` en un commit revisado.

## Defectos corregidos tras la revisión de E0.1
- **D1:** `registrar` solo da `COMPLETA` si A y B son cifrados distintos (sha256 distinto) y archivos distintos (no el mismo ni un enlace duro); `verificar_entrega` C08 lo exige.
- **D2:** `respaldar.py` da `INCOMPLETO` también si un id requerido no tiene fila en `errores_lector` o su versión no es 10.77 (`motivos_incompleto`).
- **D3:** ver arriba.
- **D4:** conteo real de pruebas comprobado por una prueba.
- **D5:** los commits `e6a05ec` y `5abadea` (ya en `origin/trabajo/e0-respaldo`) llevan una URL de sesión en el mensaje; no se reescribe el historial. Desde ahora `herramientas/revisar_mensajes.py` revisa los mensajes (también como hook `commit-msg`).

## Hallazgos de la auditoría de cierre corregidos (E0.3)
- **H1/H2:** una fila del manifiesto con `..`, ruta absoluta, `\`, o un enlace simbólico hacía que `verificar.py` leyera, y `empaquetar` metiera en el tar, archivos de fuera del respaldo. Ahora `comun.nombre_seguro` / `ruta_en` lo impiden (V02b, E03b) y `empaquetar` solo empaqueta un respaldo que verifica.
- **H3:** la copia A nunca se descifraba; ahora `COMPLETA` exige `comprobar` (o restaurar) A y que dé el mismo tar que B (C10).
- **H4:** el tar descifrado temporal iba al temporal del sistema; ahora junto al destino.
- **H5:** el README daba órdenes de «borrado seguro» que no lo garantizan en SSD ni en sistemas con registro; ya no se promete lo que no se puede garantizar.
- **H6:** las capturas 10.77 respaldadas fuera de la lista se informan (V08d, E06c): el inventario no se da por reconciliado en silencio.
- **H7:** todos los artefactos llevan `procedimiento` (E0.3) y no se mezclan versiones (V02c, E14, C11).
- **H8:** `--solo-listar` avisa de nombres de objeto que detendrían el respaldo; ya no se tolera cualquier archivo que empiece por `VERIFICACION`.
- Un respaldo hecho con E0.1/E0.2 **no** verifica con E0.3 (V02c): habría que repetirlo. No existe ninguno.
