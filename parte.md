# PARTE · 12 nov 2026

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
- AAB 138 (10.58) ya pasó revisión en Prueba cerrada «Alpha»; algunos testers ya lo prueban (11 nov).
- 10.59 en `main`: borde a borde Android 15 (sin colores de barras obsoletos, diálogos con insets). Sin AAB todavía.
- Supabase guarda en cada lectura: lector, teléfono, Android, orientación, zonas, tarjetas, color y tiempos.
- Zonas bloqueadas 10.60–10.66 probadas en su teléfono: origen y destino, con nombre, rojo con motivo en el pop-up; «ya marca» y «mejoró bastante» en velocidad.
- Zonas: buscador del teléfono (Google) primero y Nominatim (OpenStreetMap) de respaldo; origen y destino a la vez; primer lector con dirección; caché en el teléfono.
- Rediseño de la pantalla de Zonas (lista, nombres, contador, Activo/Apagado) aprobado con la muestra.
- AAB 146 (10.66) enviado a revisión en Prueba cerrada «Alpha» junto con Seguridad de los datos (12 nov).
- Privacidad de la web con la sección «Zonas bloqueadas» (12 nov). Texto de Seguridad de los datos y nota del AAB 146 entregados.

## … Falta
- AAB 146 (10.66): esperar que pase la revisión en «Alpha» y avisar a los testers.
- Redes sociales en la app (Ayuda y soporte y Cuenta): esperar de Eduardo qué redes y enlaces.
- En unos días: «Estado de la revisión» para ver qué lector falla más y cuántas ofertas se confirman sin IA.
- Prueba cerrada: día 9 de 14 seguidos con 12 testers (= verificadores). Faltan 5 días para pedir producción.
- Fase 7 (Play Billing) antes del 1 feb 2027: necesita su «adelante» y una cuenta de Google admitida a su nombre.

## ✗ No tocar
- Publicar en Play Console sin su autorización.
- Decir en público que se usa IA para revisar lecturas.
- Logos de Uber/DiDi, ciudad o país en el material de marca.
- Subir el PNG de novedades al repo.
- Secretos y llaves de firma en Git.
