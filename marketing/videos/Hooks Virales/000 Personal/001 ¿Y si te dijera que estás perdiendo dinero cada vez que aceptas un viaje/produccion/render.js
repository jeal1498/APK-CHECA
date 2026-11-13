// Uso: node render.js hoja | video (borrador con «TU CÁMARA») | video llave (para armar.sh)   (NODE_PATH=$(npm root -g))
const {chromium}=require('playwright'),fs=require('fs'),path=require('path'),{spawn,execFileSync}=require('child_process');
const AQUI=__dirname,OUT=path.join(AQUI,'renders'),modo=process.argv[2]||'hoja',FPS=30;
(async()=>{fs.mkdirSync(OUT,{recursive:true});
 const D=fs.readFileSync(path.join(AQUI,'palabras.json'),'utf8');fs.writeFileSync(path.join(AQUI,'palabras.js'),'const D='+D+';');
 const b=await chromium.launch(),pg=await b.newPage({viewport:{width:1080,height:1920}});
 pg.on('pageerror',e=>{console.error('ERROR',e.message);process.exit(1)});
 await pg.goto('file://'+path.join(AQUI,'pagina.html')+(modo==='marco'?'?marco':process.argv[3]==='llave'?'?llave':''));await pg.evaluate(()=>document.fonts.ready);
 const total=await pg.evaluate(()=>TOTAL);
 if(modo==='marco'){await pg.screenshot({path:path.join(OUT,'marco.png'),omitBackground:true})}
 else if(modo==='hoja'){const ts=process.argv[3]?process.argv[3].split(',').map(Number):Array.from({length:15},(_,i)=>+(i*total/15+.15).toFixed(2));
  const fr=path.join(OUT,'cuadros');fs.mkdirSync(fr,{recursive:true});
  for(let i=0;i<ts.length;i++){await pg.evaluate(t=>seek(t),ts[i]);await pg.screenshot({path:path.join(fr,String(i).padStart(2,'0')+'.png')})}
  execFileSync('ffmpeg',['-y','-loglevel','error','-i',path.join(fr,'%02d.png'),'-vf','scale=432:-1,tile=5x'+Math.ceil(ts.length/5)+':padding=8:color=white',path.join(OUT,'storyboard.png')]);console.log(ts.join(' '))}
 else{const ff=spawn('ffmpeg',['-y','-loglevel','error','-f','image2pipe','-framerate',FPS,'-i','-','-c:v','libx264','-profile:v','high','-pix_fmt','yuv420p','-crf','18',path.join(OUT,process.argv[3]==='llave'?'graficos-llave.mp4':'graficos.mp4')],{stdio:['pipe','inherit','inherit']});
  for(let f=0;f<Math.round(total*FPS);f++){await pg.evaluate(t=>seek(t),f/FPS);const buf=await pg.screenshot({type:'jpeg',quality:95});if(!ff.stdin.write(buf))await new Promise(r=>ff.stdin.once('drain',r))}
  ff.stdin.end();await new Promise(r=>ff.on('close',r))}
 await b.close();console.log('listo',modo,total)})();
