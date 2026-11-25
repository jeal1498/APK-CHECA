# Identidad de marca personal: Eduardo Alvarado (cuenta @Eduardoalv9802)

Aprobada por Eduardo el 24 nov 2026. Reemplaza a PS2 Style. No se cambia el posicionamiento ni lo visual sin su autorización.

## 1. Identidad y valor
- Creador de contenido, editor y productor audiovisual. Marca personal.
- Idea central: **«No solo edito videos. Construyo contenido con intención.»**
- Ayuda a profesionales, emprendedores y negocios a convertir sus ideas y grabaciones en contenido para redes, cuando no tienen tiempo, conocimiento o habilidad.
- Público: profesionales independientes, emprendedores, negocios, creadores y quien quiere marca personal.

## 2. Personalidad y tono
- Directa, provocadora, analítica, cercana. Autoridad sin arrogancia; profesionalismo sin rigidez.
- Natural, conversacional, seguro sin presumir. Sin frases motivacionales, sin lenguaje corporativo, sin aparentar ser experto en todo.
- **Guiones: evitar R fuerte (inicial y RR).** Vocabulario fácil de pronunciar sin perder naturalidad.

## 3. Pilares
A. Experiencia real (incluye ser conductor, si aporta) · B. Educación (edición, contenido, redes) · C. Experimentos (con resultados reales) · D. Documental (proceso, errores, aprendizajes).

## 4. Identidad visual (editorial, minimalista, moderna)
- Negro `#151515` · Marfil `#F1EEE6` · Naranja `#F47732`.
- Montserrat (titulares, 800/900) · Roboto (secundario).
- Sello **EA.** como recurso secundario.
- Recursos: contrastes marcados, composiciones limpias, subtítulos destacados, gráficos dinámicos.
- **Estilo aprobado (24 nov 2026): Familia A, editorial minimalista**, en código en `video/src/EA/` (Remotion): fondo marfil, tarjetas blancas con borde negro y sombra sutil, naranja solo como énfasis (palabra clave, etiquetas, anotaciones), titulares Montserrat 800. Biblioteca completa (10 componentes con variantes) y showcase `EA-Showcase`; lista en `../LEEME.md`. El boceto de «pegatinas» queda descartado.
- **Estilo vigente (25 nov 2026): «AB», mezcla de Línea + Tablero** (elegida por Eduardo entre A, B, C, AB y BA; muestras en `propuestas-estilo/`, código `video/src/EA/Mezclas.tsx`, composición `EA-Mezcla-AB`). Sobre la Familia A de arriba:
  - Fondo marfil liso (sin cuadrícula), cámara arriba (0–800 px) con línea negra de 4 px.
  - **Titular grande** Montserrat 800 (~104 px) con la cifra en naranja que cuenta desde 0.
  - **Objeto de una sola línea** negra de 6 px que se dibuja (trazo), con **una sola mancha naranja** que actúa (gota, punto, marca).
  - **Barra que reparte** un monto (borde 5 px, tramo naranja = lo que se va), con rótulos en mayúsculas Roboto 500 y la cuenta en pequeño.
  - El objeto y el dato se unen con una acción física (la gota cae y llena la barra).
  - **Subtítulos (aprobados 25 nov):** palabra por palabra, **sin cuadro**, al centro sobre la línea de la cámara (y = 800): letra negra Montserrat 800 (~70 px) con contorno marfil del 20 % del tamaño; cada palabra brinca cuando suena y la clave se pinta de naranja. Componente `video/src/EA/components/SubtitulosPalabra.tsx` (tiempos por palabra de Whisper).
  - **Sin identificador de nombre** (Eduardo, 25 nov): no se pone «Eduardo Alvarado · Conductor» ni otro cintillo con su nombre en los videos.
  - **Columna de botones** (me gusta, comentarios, compartir): ocupa la orilla derecha (x > 900 px, de y 900 a 1600). Ahí no va el sello EA. ni nada importante.
  - **Movimiento continuo:** siempre hay algo moviéndose en los gráficos (muestra `EA-Movimiento-6s`).
  - **Final de video (aprobado 25 nov):** en la pregunta final el movimiento frena, el elemento principal (la barra) se vuelve **globo de comentario** con un «?» naranja que late y, dentro, los datos del video en pequeño; en la palabra de acción cae la **✓ naranja** del inicio; sale el **sello EA. a la izquierda** (nunca a la derecha). Después todo **regresa al primer cuadro** del video para que se repita sin corte (bucle). Referencia en código: `video/src/EA/Final.tsx` (`EA-Final`, 5 s).
  - **Primer cuadro = último cuadro:** el video arranca ya armado (sin dibujarse desde vacío); sirve de portada y cierra el bucle.
  - La variante BA (negro con cuadrícula) queda solo para algún video especial de dinero, con autorización.
- Evitar: saturación, plantillas genéricas, estética corporativa.

## 5. Formato
- TikTok, Reels, Shorts. Eduardo a cámara (el rostro manda), con gráficos y textos que refuerzan. Duración según el contenido. Un video diario.
- Hecho con HyperFrames y Claude (carpeta `Editor IA`).

## 6. Guion
1. Gancho (curiosidad) · 2. Contexto breve · 3. Desarrollo con ejemplos · 4. Conclusión clara · 5. Interacción si tiene sentido.
- Una sola idea por video. Sin introducciones largas, sin ganchos engañosos, sin convertir cada video en venta.

## 7. Relación con CHECA
Parte de su historia, **no** el centro de la marca. Se muestra solo si aporta a la explicación, sirve de ejemplo, demuestra que identifica problemas y crea soluciones, o documenta aprendizajes. La cuenta no es un canal publicitario de CHECA.

## 8. Reglas para Claude
1. Coherencia con todo esto. 2. No inventar experiencias, resultados ni conocimientos. 3. Ideas originales sobre contenido genérico. 4. Provocar sin polémica gratuita. 5. No abusar de CHECA. 6. Comunicación natural y directa. 7. Lo visual se adapta al mensaje. 8. Respetar la pronunciación (sin R fuerte). 9. Claridad, retención y valor. 10. No cambiar posicionamiento ni identidad sin autorización.
