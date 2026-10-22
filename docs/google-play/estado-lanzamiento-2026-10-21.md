# Estado del lanzamiento de CHECA en Google Play

**Actualizado:** 21 de octubre de 2026  
**Repositorio Android:** `jeal1498/Mi-Ruta`  
**Paquete Play:** `com.jeal1498.checa`

## Bloqueo actual

Google está verificando la identidad y los documentos del desarrollador. Play Console no permite subir la aplicación hasta que finalice esa revisión. **No intentar subir el AAB mientras siga este bloqueo.** No se ha publicado ni enviado ninguna versión a revisión de Google Play.

## Artefacto preparado

- Workflow: `.github/workflows/play-aab-firmado.yml`
- Ejecución: [GitHub Actions run 39692385330](https://github.com/jeal1498/Mi-Ruta/actions/runs/39692385330)
- Commit de la ejecución: `d7a1aab26faed2ef65be79d4d896811d7e61c763`
- Artefacto descargado: `checa-play-release-3`, aproximadamente 21.4 MB; retención de 14 días desde la ejecución.
- SHA-256 del ZIP del artefacto registrado por GitHub: `5f1d8dc59009639a7d5dfc10004e3d8769df9ef2868b8fb8ab3ddec7e37c0097`. Este digest corresponde al ZIP del artefacto, no al AAB por separado.
- Resultado: ejecución y pasos de compilación, validación del bundle, auditoría de manifiesto/certificado y carga de artefactos terminaron en verde.

## Datos del AAB revisados en el diagnóstico

- Paquete: `com.jeal1498.checa`
- `versionCode`: 73
- `versionName`: 9.3
- `minSdk`: 26; `targetSdk`/`compileSdk`: 36
- Certificado de subida reportado: SHA-256 `CE:0C:B5:C1:2B:5F:96:68:A4:A0:29:61:B4:D8:70:58:3D:7D:A4:FA:66:9E:42:CB:3F:BF:0E:66:15:F9:9B:AC`
- El diagnóstico reportó firma válida, ausencia de `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` y `REQUEST_INSTALL_PACKAGES`, presencia de la divulgación de Accesibilidad y del flujo de eliminación de cuenta, y coincidencia del `mapping.txt` con el mapa incluido en el AAB.
- La huella de subida aún debe contrastarse con la que Play Console reconozca cuando permita cargar el AAB. La firma de subida no es lo mismo que la clave de firma de distribución de Play App Signing.

## Pendientes antes de producción

1. Probar en un teléfono real el diálogo de divulgación de Accesibilidad, el consentimiento/cancelación de MediaProjection y el comportamiento de lectura.
2. Desplegar y probar de extremo a extremo la función Supabase `eliminar-cuenta` con una cuenta de prueba; no usar una cuenta real importante.
3. Confirmar que la política de privacidad y las páginas de eliminación de cuenta están accesibles públicamente y coinciden con el comportamiento actual.
4. Completar en Play Console las declaraciones de Accesibilidad, servicios en primer plano, Seguridad de los datos, eliminación de cuenta, ficha, clasificación y recursos gráficos.
5. Verificar los requisitos de librerías nativas de 16 KB y revisar cualquier advertencia que muestre Play al analizar el AAB.
6. Confirmar que `versionCode` 73 no se haya utilizado ya en una versión cargada. Si ya fue usado, incrementar el código antes de generar el siguiente AAB.
7. Resolver la verificación de identidad del desarrollador. Si la cuenta es personal y Google lo exige para producción, preparar la prueba cerrada de 12 testers durante 14 días.

## Límites y protecciones

- La compilación correcta no significa que Google haya aprobado la aplicación.
- No subir ni enviar a revisión hasta que Google habilite la cuenta y se hayan completado las comprobaciones pendientes.
- No modificar el repositorio web `jeal1498/checa-web` ni el workflow legado `.github/workflows/build.yml`, que genera el APK distribuido por web.
- No cambiar la llave de firma ni publicar la aplicación sin autorización explícita de Eduardo.
- Hacer cambios pequeños directamente en `main` y comprobar CI antes de cambios de riesgo.

## Siguiente paso

Esperar a que Google termine la verificación de identidad. Mientras tanto, continuar con pruebas y revisiones que no requieran Play Console, sin modificar el canal web/directo.
