# Prompt: corregir el lector después de leer las capturas

Se pega en una sesión nueva **después** de que Haiku 5.5 leyó todas las capturas de la versión (paso «Banco con capturas»). Cambia `VERSION` y la ruta del JSON.

---

Lee CLAUDE.md y `docs/banco-lector.md`, y haz `git pull origin main`. Respuestas cortas, en español.

Ya están leídas todas las capturas de la versión VERSION (`pruebas/lector/vNN/resultados_haiku_vNN.json`: por captura `id` o `nombre`, `estado` (vigente, vencida, aceptada, sin_oferta), `tarifa`, `km`, `minutos`, `confianza`, `nota`). Hay dos grupos de capturas:
- **Dudosas** (`<id>.webp`): enlazan con `errores_lector` (mismo id; columnas `lineas`, `local`, `ia`, `tipo`, `version`). Traen las líneas que leyó el teléfono.
- **Normales** (`ok-<color>-<tarifa>-<km>-<min>-<hora>-n<id>.webp`, color verde, amarillo o rojo): el nombre dice lo que mostró el pop-up. Desde la 10.80, `n<id>` enlaza con la tabla `capturas_ok` (mismas columnas que `errores_lector`: `lineas`, `local`, `ia`, …). Las de la 10.78–10.79 no traen `n<id>` ni líneas.

Deben estar leídas **todas** las capturas de la 10.77 a la 10.80 (dudosas y normales); si falta alguna, léela antes de seguir. No uses capturas con confianza «baja» ni las marcadas «verificar». Si la pantalla de la imagen no coincide con las líneas guardadas (la oferta cambió), la captura queda fuera.

## Paso 1: medir (sin tocar código)
1. Normales: compara lo que dice el nombre con lo que leyó Haiku. Cada una queda como acierto, error (tarifa, km o minutos) o **falso positivo** (no era oferta, estaba vencida o tomada, o era otra tarjeta); los falsos positivos se corrigen primero. Saca el % de aciertos en tarifa, km y minutos, por color y en total, y cuántas eran ofertas vencidas, aceptadas o sin oferta (pop-up que no debía salir). Tolerancias de `Banco.kt`.
2. Dudosas: lo mismo, y quién acertó (lector, revisión de IA, los dos, ninguno, «no era oferta»).
3. Anota los dos números en el marcador de `docs/banco-lector.md` como «Calle VERSION» (no se mezclan con el banco).

## Paso 2: agrupar los errores
Clasifica cada error en una categoría (tarifa de otra tarjeta o de un botón, km altos, minutos altos, km bajos, tarjeta mezclada, oferta caída leída, oferta no leída, formato nuevo de Uber o DiDi, otra). Tabla con conteo y 2 ids de ejemplo por categoría, de la más frecuente a la menos. Para cada categoría mira 2 o 3 imágenes y escribe en una línea la regla correcta (qué debió leer el pop-up y por qué).

## Paso 3: corregir, de la categoría más frecuente a la menos
Por cada categoría:
1. Mide antes: `docs/banco/calificar.sh` y `java -jar ~/.cache/checa-banco/banco.jar app/src/test/resources/ofertas /tmp/o.txt`.
2. Agrega los casos: dudosas → `app/src/test/resources/banco/<id>-<app>.txt` (líneas guardadas, `---`, `tarifa= km= min=` o `nada`), solo si la respuesta se saca de esas líneas, sin duplicados. Normales con `n<id>` → igual que las dudosas, con las líneas de `capturas_ok`. Normales sin `n<id>` → no van al banco; si el error se entiende, escribe un caso en `ofertas/` con las líneas que se ven en la imagen (cortas, sin nombres ni direcciones completas) y di en el comentario que salió de la captura.
3. Arregla el código: `Parser.kt`, `Tarjetas.kt`, `LectorOcr.kt` o el revisor (`supabase/functions/confirmar-oferta/lector.ts` y su prompt; se despliega solo al subir a `main`, pruébalo con Node antes).
4. Mide después: el banco no puede bajar en ningún caso que antes acertaba (compara las listas de fallas) y `ofertas/` queda en 100 % salvo la falla conocida con Java 21. Sube `banco/minimo.txt` y anota el marcador.
5. Commit pequeño directo a `main` y revisa con GitHub MCP que «Compilar flavor play» pase.

Si una categoría no se puede arreglar sin romper otra, o tres intentos fallan, detente, anota la suposición dudosa y sigue con la siguiente.

## Paso 4: cerrar la versión
1. Sube `versionCode`/`versionName`, bloque en `Novedades.kt` (lenguaje del conductor, sin decir que se usa IA) y entrada en `docs/historial-versiones.md`.
2. Actualiza «Qué falla hoy» en `docs/banco-lector.md` con lo que quedó.
3. Decide lo del revisor con imagen: si después de los arreglos la precisión en normales sigue bajo 95 % (tarifa, km y minutos juntos), propón mandar la imagen a Haiku solo en los casos dudosos (no lo implementes sin el «adelante» de Eduardo).

## Reglas
- Lo que leyó Haiku es la respuesta de referencia, salvo que la imagen muestre otra cosa.
- No hagas commit en `pruebas/**`. Las imágenes no van al repo: bájalas con la función temporal (`docs/banco-lector.md`, clave nueva), y al terminar déjala en stub 410 y borra las imágenes y los JSON con notas del directorio temporal.
- No toques la rama `respaldo-main-local`, no publiques en Play Console, ningún secreto en el repo, nada de lenguaje clínico.

## Al final (máximo 10 líneas)
Precisión de la calle (normales y dudosas), categorías arregladas con su conteo, banco antes → después, qué quedó pendiente, si conviene el revisor con imagen y el link de la corrida para probar.
