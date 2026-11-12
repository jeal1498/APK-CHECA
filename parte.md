# PARTE · 12 nov 2026 (noche)

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
- 10.67–10.69 en `main`: «Al abrir Uber o DiDi» con 3 opciones, Mis metas compacta, y lector más preciso (DiDi toma «Aceptar $X», Uber corrige km con «$/km est.», sin pop-up en mapa de calor ni si cancelan). 10.69 probada con sus capturas: «va bien».
- Revisión de 305 ofertas mal leídas con veredicto en `errores_lector` (`ia_bien`, `ia_mal`, `dudoso`) y 15 casos nuevos en las pruebas.
- Revisor `confirmar-oferta` v26 en producción (commit `f6def4b`): la IA solo lee y las cuentas van en `lector.ts`. Prueba: 101 de 107 (antes 45). Se pierden 2 rescates de Uber (aceptado). `probar-revisor` borrada.
- Reglas remotas (corregir sin APK): diseño hecho, **no construir** hasta medir 1 semana con la 10.69.
- Nota de Play y la imagen del grupo de la 10.69 (zonas bloqueadas destacadas) entregadas.
- AAB 149 (10.69) generado (corrida 15 de «play-aab-firmado», en verde) y enviado a revisión en «Alpha» (12 nov).
- AAB 149 (10.69) aprobado en «Alpha» e imagen de novedades publicada en el grupo (12 nov).
- 10.70 en `main` (sin AAB): reseña nativa de Play (`Resena.kt`, tras 10 viajes aceptados, 1 vez cada 60 días).
- Crashlytics preparado solo para `play`: se activa al poner `app/google-services.json`.
- Tablero Checa (artifact privado, fijado en la barra lateral de claude.ai, https://claude.ai/artifact/DZiwZpPwnT6StCWXCpuDQ3): datos en vivo de Supabase, colores de marca, secciones Estado, Calidad, Crecimiento, Uso, Dispositivos, Viajes y horas, Web. Solo abre con su cuenta de Claude.
- Skill `parte`: ahora `/parte` actualiza los 3 puntos desde la conversación; «lee el parte» solo lee.
- 10.71 en `main`: «Guardar pantallas de ofertas» viene encendido por defecto (los dos canales).
- 10.72 (AAB 152) enviado a revisión en Prueba cerrada «Alpha» (12 nov): las capturas de lecturas dudosas (`diferencia`, `desacuerdo`, `rescate`) se suben solas a Supabase (almacén privado `capturas`, WebP, 7 días) con aviso de una vez «Capturas para mejorar la lectura». Almacén y `confirmar-oferta` ya desplegados.
- Seguridad de los datos en Play Console: «Fotos» (recopiladas, no compartidas, no efímeras, opcional, Funciones de la app y Estadísticas) y Ubicación aproximada capturadas el 12 nov.
- Plan del lunes 16 nov hecho: `docs/plan-lunes-16-nov.md` (meta: coincidencia de 10.72 ≥ 85 %; 10.69 tuvo 72.8 %).

## … Falta
- **16 nov (lunes):** ejecutar `docs/plan-lunes-16-nov.md`. Arranca con «Estado de la revisión»; luego función `ver-capturas` (enlaces temporales), clasificar las capturas, corregir los 3 patrones más frecuentes (10.73) y medir el lunes 23. Si hay menos de 30 capturas, esperar al miércoles.
- Confirmar que Seguridad de los datos (Fotos y Ubicación aproximada) quedó enviada en Play Console junto con el AAB 152, y que 10.72 ya tenga instalaciones (mirar revisiones con versión 10.72).
- 13 nov: «Estado de la revisión de hoy» para revisar el revisor v26: sin `json`/`json_vacio` en `fallas_ia`, mediana de `ms_ia` < 4 s, % coincidió > 49 %, sin `oferta=true` con km 0.
- ~20 nov: comparar 10.69 contra 10.58 y decidir si se construyen las reglas remotas (10–14 h).
- Redes sociales en la app (Ayuda y soporte y Cuenta): esperar de Eduardo qué redes y enlaces.
- Prueba cerrada: día 9 de 14 seguidos con 12 testers (= verificadores). Faltan 5 días para pedir producción.
- Crashlytics: Eduardo crea el proyecto en Firebase con dos apps (`com.jeal1498.checa` y `com.karen.miruta`) y manda `google-services.json`; luego actualizar Seguridad de los datos (informes de fallos).
- Tablero: confirmar si la letra de marca (Bricolage, DM Sans) se ve; página pública solo si la pide (necesita inicio de sesión propio por privacidad).
- Fase 7 (Play Billing) antes del 1 feb 2027: necesita su «adelante» y una cuenta de Google admitida a su nombre.

## ✗ No tocar
- Publicar en Play Console sin su autorización.
- Decir en público que se usa IA para revisar lecturas.
- Logos de Uber/DiDi, ciudad o país en el material de marca.
- Subir el PNG de novedades al repo.
- Secretos y llaves de firma en Git.
