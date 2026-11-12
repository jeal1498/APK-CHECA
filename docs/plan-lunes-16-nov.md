# Plan del lunes 16 nov 2026: mejorar la lectura con las capturas de la 10.72

**Punto de partida (12 nov):** 10.69 coincidió 72.8 % (10.58: 55.3 %, 10.53: 46.5 %). 117 de sus diferencias sin conductor-Eduardo son «el lector local ve oferta y el revisor dice `oferta:false`» (99 DiDi, 15 Uber en ocr). 6 desacuerdos texto↔foto. Ningún caso tiene `veredicto`. La 10.72 (AAB 152) quedó en Alpha el 12 nov; sube capturas de `diferencia`, `desacuerdo` y `rescate` al almacén privado `capturas` (nombre = id de `errores_lector`, `.webp`, 7 días).

**Meta:** coincidencia de 10.72 ≥ 85 % y menos de 60 diferencias por cada 500 revisiones.

## 0. Antes de empezar (5 min)
1. `git pull origin main`.
2. ¿Hay filas de 10.72 en `revisiones`? ¿Cuántas capturas hay en `storage.objects` (bucket `capturas`)? Si hay menos de 30, esperar al miércoles y solo hacer el paso 1.

## 1. Estado de la revisión (10 min)
- Tabla por conductor (formato de CLAUDE.md, sin `a23854c9`): revisiones, coincidió, diferencias, apps, versión, IA, costo.
- Comparar 10.72 contra 10.69, 10.58 y 10.53 (coincidió, diferencias, pop-up, IA, costo por 1,000).
- Confirmar que las subidas funcionan: capturas subidas ≈ diferencias de 10.72 con captura reciente.

## 2. Poder ver las capturas (20 min)
- Crear la función `ver-capturas` en Supabase: lista casos con su enlace temporal (1 h) y solo la puede llamar Eduardo (clave de servicio, no pública).
- Si no sale en 20 min: Eduardo comparte «Lo guardado» desde su teléfono y de dos conductores.

## 3. Clasificar (30–40 min)
Ver las capturas por tipo y asignar `veredicto` en `errores_lector`:
1. **A. No era oferta** (el lector local inventó una oferta: mapa, pantalla en espera, resumen).
2. **B. Oferta real que el revisor no vio** (revisor falla).
3. **C. Oferta real que el lector local leyó mal** (km, min o tarifa distintos).
4. **D. Las dos lecturas mal** (caso difícil, anotar).
- Contar por grupo y por app. Tomar los 3 patrones más frecuentes.

## 4. Corregir (1–2 h, solo los 3 patrones)
| Grupo | Dónde | Qué |
|---|---|---|
| A | `Parser.kt` (`plausible`, tramos) | rechazar el patrón (p. ej. `10 km / 1 min`, tarifa sin botón Aceptar) |
| B | `supabase/functions/confirmar-oferta/lector.ts` | regla del revisor; probar con los casos de la captura |
| C | `Parser.kt` | regla nueva por patrón |
- Cada caso nuevo: `app/src/test/resources/ofertas/*.txt` + `ParserTest.kt`; deben pasar en `play-compilar.yml`.
- Simular con `python3 -I` antes de subir. Un cambio por patrón, en commits pequeños a `main`.

## 5. Cerrar (10 min)
- Subir versión (10.73 / 153), nota en `docs/historial-versiones.md`.
- Eduardo prueba con el APK de «Compilar flavor play».
- Medir el lunes 23: misma tabla del paso 1 contra la meta.
- Borrar capturas procesadas que no hagan falta antes de los 7 días.

## No tocar
- No publicar en Play Console sin autorización de Eduardo.
- No decir en nada público que se usa IA.
- No bajar el filtro de capturas a más de 7 días de retención.
