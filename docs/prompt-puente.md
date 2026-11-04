# Prompt: puente con Clientela (ejecutar en este repo, después del esquema compartido)

```
Lee CLAUDE.md, docs/web-y-ventas.md ("Pendientes de funciones nuevas") y el PLAN.md etapa 10
de jeal1498/PWA-Clientela (solo lectura). Confirma que el esquema compartido (perfil_conductor,
viajes_privados, contactos_sos, viajes_checa) ya está desplegado; si no, detente y avísalo.

Hazlo en este orden, versión nueva por etapa (sube versionCode/versionName), respuestas cortas,
sin compilar localmente (revisa el Kotlin con cuidado):

1. Moneda: leer/guardar moneda en perfil_conductor (Cuenta.kt); Parser acepta símbolos y
   formatos de otras monedas sin romper la regla "tarifa = primer texto que sea solo un monto";
   Popup y textos de precios usan la moneda elegida. Prueba las regex con python3 -I.
2. Botón "Clientela" en Inicio o Ajustes: abre https://jeal1498.github.io/PWA-Clientela/
   (o el APK cuando exista).
3. "Mi día" unificado: sumar viajes de viajes_checa y viajes_privados.
4. SOS: contactos de contactos_sos, botón de alarma, ubicación por WhatsApp y llamada.
   Revisa permisos (ubicación) y su efecto en la ficha de Play; pide el OK de Eduardo
   antes de pedir permisos nuevos.
5. Actualizar Privacidad (PWA-Checa-Multimedia) y "Seguridad de los datos" de Play por los
   datos de clientes y contactos.
6. Licencias combinadas Checa + Clientela junto con la Fase 7 (Play Billing), solo con su
   "adelante": debe estar lista antes del 1 feb 2027.

Cambios directo a main en commits pequeños; ningún secreto en el repo; no publicar en Play
Console sin autorización.
```
