// Cuadros sueltos para la hoja de revisión: node cuadros.js <index.html> <carpeta> <lista de cuadros separada por comas>
const {chromium}=require('playwright');
(async()=>{const [,,html,out,lista]=process.argv;
const br=await chromium.launch();const pg=await br.newPage({viewport:{width:1080,height:1920}});
await pg.goto('file://'+require('path').resolve(html));await pg.evaluate(()=>document.fonts.ready);
for(const f of lista.split(',').map(Number)){await pg.evaluate(t=>window.set(t),f/30);await pg.screenshot({path:`${out}/${String(f).padStart(4,'0')}.png`});}
await br.close();})();
