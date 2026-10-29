---
name: guion-vertical-checa
description: Escribe guiones de video vertical (TikTok, Reels, Shorts) para Checa, de creador a creador, con gancho de 0-3 s, cuerpo rápido con acotaciones de edición entre corchetes y un CTA de un solo paso. Úsala cuando Eduardo pida un guion nuevo para la cuenta de Checa.
---

# Guion vertical · Checa

## Tono (no negociable)
- Creador a creador. Directo, pragmático, con lenguaje de conductor.
- Prohibido: tono corporativo, publicitario, frases de marketing genéricas ("revoluciona", "la mejor app", "transforma tu experiencia"), saludos ("hola a todos") e intros vacías.
- Frases cortas. Una idea por frase. Números concretos en lugar de adjetivos.

## Paso 1: verificar la funcionalidad antes de escribir
Antes de escribir, confirma en el código lo que el guion va a afirmar. No inventes funciones.
- Qué muestra el pop-up: `app/src/main/java/com/karen/miruta/Popup.kt`.
- Cómo calcula la ganancia neta: `Parser.kt`, función `neta()` = tarifa − gasolina. La gasolina sale de `km / kmPorLitro × precioLitro` (`gasolina()`).
- Valores por defecto de gasolina: `Prefs.kt` (12 km/l, $24/l). Úsalos en ejemplos salvo que Eduardo diga otra cosa.
- Semáforo y filtros: `Parser.nivel()` y `Parser.motivoFiltro()`.
- Vigencia de la oferta: `Licencia.kt`, `BETA_HASTA` (hoy es 31 ene 2027). Si la oferta es "3 meses gratis", eso es una promo comercial y no está en el código; confírmala con Eduardo.

**Lo que hoy el código NO hace:** no resta desgaste de llantas, frenos ni aceite. Solo resta gasolina. No escribas "gasolina y desgaste" en un guion. Si Eduardo quiere ese dato, primero hay que programarlo.

## Paso 2: estructura (todo el guion en el orden exacto)
1. **Gancho (0–3 s):** un golpe de realidad, dato duro o dolor exacto del conductor. Sin saludo, sin presentar la app. Ejemplo de forma: «Ese viaje de 60 pesos te parece buen viaje. Te quedan 38.»
2. **Cuerpo (ritmo rápido):** oraciones cortas. Cada frase va con su acotación entre corchetes, por ejemplo: `[Mostrar tarjeta de oferta $60 / Texto en negrita: 11 km]`. Solo hechos de la funcionalidad real. La app se presenta a mitad del video, antes del segundo 10.
3. **CTA (un solo paso):** una palabra para comentar o un link. Sin pasos intermedios. Ejemplo: «Comenta YO y te mando el link.»

## Paso 3: formato de salida
- Guion con tiempos aproximados por bloque (0:00–0:03, etc.).
- Acotaciones de edición entre corchetes, en cursiva o en negrita si ayudan a leer.
- Al final, un bloque corto «Antes de grabar» con: cifras de ejemplo a cambiar por un viaje real, vigencia de la oferta y cualquier función que el guion mencione y que deba verificarse.
- Duración objetivo: 25–35 s.

## Reglas de contenido
- Nunca prometas una ganancia ni un resultado que el código no calcule.
- No afirmes cifras de terceros ni de Uber/DiDi como hechos generales.
- Si el guion es para la versión A/B, el texto debe ser idéntico en las dos versiones; solo cambia lo visual.
