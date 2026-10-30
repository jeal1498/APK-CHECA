// Captura la composición sin fondo (gancho, escenas, subtítulos) para componerla sobre cuadros reales del video.
// El Chromium del entorno no reproduce H.264, así que el video se saca con ffmpeg y las capas se pegan encima con PIL.
// Uso: node vista_previa.js <carpeta_trabajo> <id_del_spec> <t1> <t2> ...   → <trabajo>/ov_<t>.png (con transparencia)
//   y luego: ffmpeg -ss <t> -i public/input-video.mp4 -frames:v 1 vf_<t>.png  y  Image.alpha_composite(vf, ov)
const {chromium}=require(process.env.PLAYWRIGHT_PATH||'/opt/node-tools/node_modules/playwright');
const W=process.argv[2], id=process.argv[3], times=process.argv.slice(4).map(Number);
(async()=>{const b=await chromium.launch();const pg=await b.newPage({viewport:{width:1080,height:1920}});
await pg.goto('file://'+W+'/public/index.html');await pg.waitForTimeout(1200);
await pg.addStyleTag({content:'html,body,#root{background:transparent!important}#video-wrap{display:none!important}'});
for(const t of times){
 await pg.evaluate(({t,id})=>{window.__timelines[id].time(t,false);
  document.querySelectorAll('.clip').forEach(el=>{const s=parseFloat(el.dataset.start),d=parseFloat(el.dataset.duration);if(el.id!=='video-wrap'&&el.tagName!=='VIDEO')el.style.display=(t>=s&&t<s+d)?'':'none'});},{t,id});
 await pg.waitForTimeout(150);await pg.screenshot({path:`${W}/ov_${t}.png`,omitBackground:true});}
await b.close()})();
