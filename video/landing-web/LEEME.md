# Landing animada de Checa (sin mp4)

- `plantilla.html`: la página (copy de `marketing/web/landing-una-pantalla.md`).
- `src/hyperframes.js`: composición HyperFrames `hero-oferta` (GSAP, timeline en `window.__timelines`), corre en vivo.
- `src/remotion.jsx`: escenas de Remotion (`bruto`, `avisa`, `semaforo`) que corren en vivo con `@remotion/player`.
- Armar: `npm i react@19.2.3 react-dom@19.2.3 remotion@4.0.535 @remotion/player@4.0.535 gsap@3.12.5 esbuild@0.24.0`, luego
  `esbuild src/main.js --bundle --minify --format=iife --jsx=automatic --loader:.js=jsx --define:process.env.NODE_ENV=\"production\" --outfile=out/bundle.js`
  y pegar `out/bundle.js` en el `<script>/*BUNDLE*/</script>` de la plantilla.
- Publicada como artifact: https://claude.ai/artifact/KQFKnZLY8mnnFb5MTprBNe
