# Guion · 29 - Checa cambia de app (CHECA) — 30 oct 2026, skill `guion-checa`

**Estado:** propuesto. Aviso para quien usa el APK de la web: el canal directo se cierra el 15 nov 2026 (`Migracion.kt`, `LIMITE_MS`). Enfoque informativo y tranquilo, CTA vigente.

Concepto / Problema detectado en el repo:
La 9.5 (release v80) avisa con cuenta regresiva y bloquea el APK el 15 nov. La web ya manda a `/registro/`. `Cuenta.kt`: la licencia (tabla `licencias`) y los ajustes (tabla `ajustes`) viven en Supabase ligados al correo; el historial, «Mi día» y las metas son locales y no se pasan. **Por eso la voz promete solo licencia y ajustes con el mismo Google, y no menciona el historial.** Pendiente de Eduardo: confirmar con su cuenta en la Prueba cerrada que el login en Play baja licencia y ajustes.

## Guion de voz (limpio, ~22 s)
El 15 de noviembre, tu Checa deja de abrir. Si la instalaste con el archivo de la web, ese archivo se cierra ese día. La app Checa se pasa a Google Play. Entras con el mismo Google, y tu licencia y tus ajustes se quedan. Regístrate con tu Gmail, el de tu Play Store. Comenta CHECA y te paso el acceso a la prueba en Google Play.

## Guión Desglosado
- `0:00 - 0:03` | Audio: «El 15 de noviembre, tu Checa deja de abrir.» | Visual: [Flash en el cuadro 0; «15 NOV» se estrella con zoom en rojo; cámara sacude; semáforo en rojo]
- `0:03 - 0:07` | Audio: «Si la instalaste con el archivo de la web, ese archivo se cierra ese día.» | Visual: [Ícono de archivo «Checa.apk» entra con spring y se apaga con un candado]
- `0:07 - 0:10` | Audio: «La app Checa se pasa a Google Play.» | Visual: [El logo del semáforo con ✓ se desliza hacia una tarjeta «Google Play»; raya punteada]
- `0:10 - 0:15` | Audio: «Entras con el mismo Google, y tu licencia y tus ajustes se quedan.» | Visual: [Dos fichas «Licencia» y «Ajustes» con ✓ verde, en escalera]
- `0:15 - 0:18` | Audio: «Regístrate con tu Gmail, el de tu Play Store.» | Visual: [Campo de correo que se llena letra por letra; botón «Registrarme»]
- `0:18 - 0:22` | Audio: «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Visual: [Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA»]

CTA final:
«Comenta CHECA y te paso el acceso a la prueba en Google Play.»

## Notas
Sin «gratis», precios ni ciudad; sin nombres de Uber/DiDi; montos sin moneda en la voz; sin `[pause]` en el gancho. Todo por encima de y≈1150.

## Tabla de animaciones por toma (sacada del desglose)
| Toma | Frase | Animación |
|---|---|---|
| 0:00–0:03 | «El 15 de noviembre, tu Checa deja de abrir.» | Flash en el cuadro 0; «15 NOV» se estrella con zoom en rojo; cámara sacude; semáforo en rojo |
| 0:03–0:07 | «Si la instalaste con el archivo de la web, ese archivo se cierra ese día.» | Ícono de archivo «Checa.apk» entra con spring y se apaga con un candado |
| 0:07–0:10 | «La app Checa se pasa a Google Play.» | El logo del semáforo con ✓ se desliza hacia una tarjeta «Google Play»; raya punteada |
| 0:10–0:15 | «Entras con el mismo Google, y tu licencia y tus ajustes se quedan.» | Dos fichas «Licencia» y «Ajustes» con ✓ verde, en escalera |
| 0:15–0:18 | «Regístrate con tu Gmail, el de tu Play Store.» | Campo de correo que se llena letra por letra; botón «Registrarme» |
| 0:18–0:22 | «Comenta CHECA y te paso el acceso a la prueba en Google Play.» | Pantalla final estándar sin link: logo rojo → ámbar → verde → ✓, pastilla «Comenta CHECA» |
