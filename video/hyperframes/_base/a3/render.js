// Render de cuadros del estilo A3: NODE_PATH=$(npm root -g) node render.js <index.html> <carpeta_salida> <desde> <hasta>
// (cuadro i = segundo i/30). checa.py a3 --render lo corre en 4 procesos en paralelo.
const {chromium}=require('playwright');
(async()=>{const [,,html,out,a,b]=process.argv;
const br=await chromium.launch();const pg=await br.newPage({viewport:{width:1080,height:1920}});
await pg.goto('file://'+require('path').resolve(html));await pg.evaluate(()=>document.fonts.ready);
for(let i=+a;i<+b;i++){await pg.evaluate(t=>window.set(t),i/30);await pg.screenshot({path:`${out}/${String(i).padStart(4,'0')}.png`});}
await br.close();})();
