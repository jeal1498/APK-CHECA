# Página web como funnel · Paso 3: recorrido y estructura

Base: `01-publico.md`, `02-oferta.md` y la web actual (`PWA-Checa-Multimedia`, GitHub Pages).
Meta única: **descargar y empezar los 7 días gratis**. Secundaria: **beta tester**.

## Páginas (planas, máximo 2 niveles)
```
Inicio (/)                      ← página de venta, todo el funnel
├── Instalar (/instalar/)       ← NUEVA: «gracias» + guía paso a paso + ayuda por WhatsApp
├── Beta (/beta/)               ← NUEVA: 50 lugares, 3 meses gratis, +1 mes por referido
├── Privacidad (/privacidad.html)   (se queda: la pide Mercado Pago)
├── Términos y reembolsos (/terminos.html)  (se queda; agregar garantía de lectura)
└── descargar/Checa.apk         (se queda igual: no cambiar la URL)
```

**Por qué `/instalar/`:** al tocar «Descargar», el APK baja y la persona pasa a una página con **solo** los pasos de instalación. Instalar es lo más difícil (paso 1): no debe estar escondido al final de la página de venta. Además sirve para medir cuántos descargan.

## Orden de la página de inicio (de arriba a abajo)
| # | Sección | Qué logra | CTA |
|---|---|---|---|
| 1 | **Encabezado**: dolor + promesa, pop-up de ejemplo | Engancha en 3 s | Descargar gratis · 7 días |
| 2 | **Video 02** («trabajar mucho sin lana»), con portada, se reproduce al tocar | Identificación con el dolor | — |
| 3 | **El problema**: gasolina, recogida, $/km bajo, 15 s para decidir | Agita el dolor | — |
| 4 | **Cómo funciona** en 3 pasos | Se ve fácil | — |
| 5 | **Pruébalo aquí** (demo interactiva) | Prueba antes de instalar | Descargar |
| 6 | **Confianza**: no toca tus viajes, no acepta por ti, datos en tu teléfono, **garantía de lectura** | Quita el miedo principal | — |
| 7 | **Haz tus cuentas** (calculadora) | El precio se paga solo | — |
| 8 | **Precios**: 3 paquetes, 6 meses destacado, «≈ $3.27 al día», garantía | Decide el plan | Descargar gratis · 7 días |
| 9 | **Beta**: 50 lugares (banda discreta) | Urgencia real | Quiero ser beta tester → /beta/ |
| 10 | **Preguntas** (6 a 8) | Últimas dudas | — |
| 11 | **Cierre**: frase + botón | Último empujón | Descargar gratis · 7 días |

Cambios contra la web actual: entra el video, la **confianza sube antes de precios** (hoy está solo en preguntas), sale la guía de instalación larga (va a `/instalar/`), entra la banda de beta y la garantía.

## Navegación
- **Arriba (4 + botón):** Cómo funciona · Pruébalo · Precios · Preguntas · **[Descargar gratis]** (verde, a la derecha).
- **Botón fijo abajo en el teléfono** al pasar el encabezado: «Descargar gratis · 7 días».
- **Pie:** Instalar · Beta · Privacidad · Términos y reembolsos · WhatsApp. Sin ciudad.
- WhatsApp **solo como ayuda** (instalar, dudas), nunca compite con el botón de descarga.

## `/instalar/`
1. «Tu descarga empezó» (+ botón por si no bajó).
2. 5 pasos con capturas: abrir APK → Play Protect → ajustes restringidos → Accesibilidad → ▶.
3. Bono: **«¿Te atoraste? Te ayudo por WhatsApp»**.
4. Recordatorio: 7 días gratis, sin tarjeta.

## `/beta/`
- Qué das y qué pides (3 meses gratis a cambio de opinión; +1 mes por amigo).
- Botón a WhatsApp con texto listo: «Quiero ser beta tester, mi correo de Google es…».
- Al llenarse: cambiar a «Beta cerrada: prueba 7 días gratis».

## Medición (paso 7)
Clic en Descargar · llegada a `/instalar/` · clic en WhatsApp de ayuda · clic en Beta · reproducciones del video.

## Cambio 13 oct 2026: inicio como VSL (pedido de Eduardo)
La página era muy larga. Ahora: **título → video → botón** arriba, luego Cómo funciona (con el pop-up), Confianza, Precios, 5 preguntas y Cierre (≈ la mitad de largo).
- Salieron del inicio: la demo interactiva, la calculadora, la sección «El problema» (la cubre el video) y la banda de beta.
- La beta vive solo en **`/beta/`** (página completa: qué recibes, qué te pido, cómo entrar), enlazada en el pie y para usar en redes con `?utm_campaign=beta`.
- El botón fijo del teléfono aparece siempre que el botón principal no está en pantalla.
- Video del inicio: clip de Eduardo (beta) recortado a 27 s sin la parte de «busco 50 / comenta YO» (`media/vsl.mp4`). Pendiente opcional: VSL propio de 60–90 s.
