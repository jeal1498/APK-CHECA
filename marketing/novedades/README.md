# Novedades para testers (imagen + texto)

Plantilla: `plantilla.html` (1080×1350, marca v2). Para cada entrega:
1. Copia la plantilla, cambia título, tarjetas (icono, nombre, descripción, versión) y pon solo lo **no publicado** (ver abajo).
2. Render: `NODE_PATH=$(npm root -g) node -e "const {chromium}=require('playwright');(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:1080,height:1350},deviceScaleFactor:2});await p.goto('file://'+process.cwd()+'/plantilla.html');await p.screenshot({path:'novedades.png'});await b.close()})()"`
3. Mandar a Eduardo imagen + texto del grupo; al publicarse, anotar las novedades en `publicadas.md`.
4. Reglas: sin logos ni ciudad; máximo 7 tarjetas; el PNG no se commitea.

## Publicadas en el grupo de testers
Ver `publicadas.md` (no repetir).
