# PARTE · 11 nov 2026

## ✓ Decidido
- Imagen de novedades para testers hecha (7 tarjetas, marca v2) y publicada en el grupo.
- Plantilla guardada en `marketing/novedades/plantilla.html`.
- Novedades de la imagen marcadas como publicadas en `marketing/novedades/publicadas.md`.
- Texto del grupo: cercano, para máximo 15 personas, con emojis.
- v10.53 (AAB 133) ya pasó revisión y está disponible para los testers en Prueba cerrada «Alpha».
- 10.53 probada con el APK de «Compilar flavor play».
- Release directo con la 9.6 hecho.
- 10.54–10.58 en `main` (compilan 10.54–10.57): pop-up se quita al irse la oferta; ofertas de DiDi por tarjeta; lector de texto solo con lo visible; aviso de versión nueva de Play.
- Pop-ups: en vertical uno solo (el de la oferta nueva tapa a los anteriores); dos solo en horizontal.
- Cascada de lectores: texto primero, la foto confirma sin IA (`coincidencias`); si no coinciden, la IA revisa las dos y guarda cuál acertó; rescate solo si ambos fallan.
- 10.58 probada con el APK.
- Supabase guarda en cada lectura: lector, teléfono, Android, orientación, zonas, tarjetas, color y tiempos.

## … Falta
- Play Console › Seguridad de los datos: agregar modelo del teléfono, Android y tiempos (ver `docs/google-play/data-safety.md`, 10.57–10.58).
- Subir a Play Console el AAB 138 (10.58) de «Preparar AAB Google Play» (generado 11 nov).
- En unos días: «Estado de la revisión» para ver qué lector falla más y cuántas ofertas se confirman sin IA.
- Prueba cerrada: día 8 de 14 seguidos con 12 testers (= verificadores). Faltan 6 días para pedir producción.
- Fase 7 (Play Billing) antes del 1 feb 2027: necesita su «adelante» y una cuenta de Google admitida a su nombre.

## ✗ No tocar
- Publicar en Play Console sin su autorización.
- Decir en público que se usa IA para revisar lecturas.
- Logos de Uber/DiDi, ciudad o país en el material de marca.
- Subir el PNG de novedades al repo.
- Secretos y llaves de firma en Git.
