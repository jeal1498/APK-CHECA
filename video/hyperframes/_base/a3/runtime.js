// Runtime del estilo A3 con loop. Lee D (datos ya compilados a segundos absolutos por a3.py) y anima con window.set(t).
const $=id=>document.getElementById(id);
const cl=(x,a=0,b=1)=>Math.min(b,Math.max(a,x));
const p=(t,a,b)=>cl((t-a)/(b-a));
const eo=x=>1-Math.pow(1-x,3), eio=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const back=x=>{const c=1.7;return 1+(c+1)*Math.pow(x-1,3)+c*Math.pow(x-1,2)};
const lerp=(a,b,x)=>a+(b-a)*x;
const COL={verde:'#22C55E',rojo:'#EF4444',amarillo:'#FACC15',hueso:'#F4F4EF'};
const L=D.lines, N=L.length;
const B=[[120,300,180,'#22C55E',.10],[900,260,240,'#F4F4EF',.06],[820,1500,300,'#22C55E',.08],[160,1600,220,'#FACC15',.05],[540,980,420,'#F4F4EF',.035],[980,900,140,'#EF4444',.05],[300,800,120,'#F4F4EF',.07]];
B.forEach(()=>{const d=document.createElement('div');d.className='bk';$('bks').appendChild(d)});
$('hook').textContent=D.hook;
const TXT={gris:'CHECA',verde:'ACEPTAR',amarillo:'CONSIDERAR',rojo:'RECHAZAR'};
function pop(o){
  const nv=o.nivel;$('cp').className='cp cp-'+(nv=='amarillo'?'amarillo':nv);$('cb').className='cp-banda'+(nv=='gris'?' gris':'');
  $('ca').textContent=o.txt||TXT[nv];
  ['f1','f2','f3'].forEach((id,i)=>$(id).className='cp-ficha'+(o.ok?(i<2?' cp-ok':''):(i<2?' cp-mal':'')));
  $('fh').textContent=o.h||'';$('fk').textContent=o.k||'';$('fm').textContent=o.m||'';$('fkm').textContent=o.km||'';}
function linea(t){let s=0;for(let i=0;i<N;i++)if(t>=L[i].start)s=i;return s}
function golpe(g,t){const k=back(p(t,g.a,g.a+.16)),e=$('gol');e.textContent=g.txt;e.style.fontSize=(g.txt.length>14?'96px':(g.txt.length>9?'108px':'130px'));e.style.color=COL[g.color]||g.color||'#F4F4EF';e.style.opacity=cl(k*2)*(1-p(t,g.b-.12,g.b));e.style.transform=`scale(${lerp(2,1,eo(p(t,g.a,g.a+.14)))}) rotate(${lerp(-5,-1.5,p(t,g.a,g.b))}deg)`}
function set(t){
  const s=linea(t),Ls=L[s],ts=t-Ls.start,vt=Ls.vista;
  [...$('bks').children].forEach((d,i)=>{const b=B[i];const x=b[0]+Math.sin(t*.9+i)*30,y=b[1]+Math.cos(t*.7+i*2)*24-t*8;d.style.cssText=`left:${x-b[2]/2}px;top:${y-b[2]/2}px;width:${b[2]}px;height:${b[2]}px;background:${b[3]};opacity:${b[4]}`});
  // ===== cámara: órbita lenta, zoom por línea y un corte distinto cada vez (lateral, zoom, giro)
  const punch=Math.max(0,1-ts/.35),orb=Math.sin(t*.55)*10;
  const zs=lerp(1,Ls.zoom,eio(p(ts,0,1.4))),pk3=Math.pow(punch,2),sg=(s%2?1:-1),mode=s%3;
  const shk=D.sacude.reduce((m,x)=>Math.max(m,Math.max(0,1-Math.abs(t-x)/.3)*12),0);
  $('world').style.transform=`translate(${(mode==0?sg*340*Math.pow(punch,3):0)+Math.sin(t*95)*shk}px,${Math.cos(t*85)*shk}px) scale(${zs*(1+.03*Math.sin(t*.8)+(mode==1?.38*pk3:.05*eo(punch)))}) rotateZ(${(mode==2?sg*7*pk3:0)+sg*1.6*Math.sin(Math.min(ts,2.5)*1.3)}deg) rotateY(${orb}deg) rotateX(${Math.cos(t*.4)*3}deg)`;
  // ===== teléfono (vistas «telefono» y «metas»)
  const tin=eo(p(t,0,.25)),verTel=(vt=='telefono'||vt=='metas'),tl=Ls.tel;
  const cae=tl&&tl.cae!=null?tl.cae:-99;
  $('tel').style.transform=`translateZ(${lerp(-380,-60,tin)}px) rotateY(${lerp(-32,-10,eio(p(t,0,1.2)))+Math.sin(t*.9)*5}deg) rotateX(${lerp(24,6,eio(p(t,0,1.5)))}deg) translateY(${Math.sin(t*2)*10-(vt=='metas'?40:0)-(vt=='telefono'?165:0)}px) scale(${vt=='metas'?1.12:(vt=='telefono'?.84:1)}) translateX(${Math.sin(t*70)*14*Math.max(0,1-Math.abs(t-cae)/.25)}px)`;
  $('tel').style.opacity=(.75+.25*tin)*(verTel?1:0);
  const rt=tl&&tl.ruta;$('rp').style.strokeDashoffset=rt?900*(1-eio(p(t,rt[0],rt[1]))):0;
  if(tl&&tl.oferta){$('om').textContent=tl.oferta.precio;$('o1').textContent=tl.oferta.l1;$('o2').textContent=tl.oferta.l2}
  const zz=tl&&tl.zoom?1+.22*Math.sin(Math.PI*p(t,tl.zoom[0],tl.zoom[1])):1;
  const caeA=tl&&tl.cae!=null;
  $('oferta').style.transform=`translateY(${caeA?lerp(-330,0,back(p(t,cae-.2,cae+.2))):(s==0?lerp(110,0,back(p(t,0,.3))):0)}px) scale(${zz})`;
  $('oferta').style.opacity=(caeA&&t<cae-.25)?0:1;$('oferta').style.display=(vt=='metas')?'none':'block';
  $('metas').style.display=(vt=='metas')?'block':'none';
  // ===== pop-up: cada dato entra en su palabra
  const E=D.popEntra,pin=p(t,E,E+.4),pout=p(t,D.popSale,D.popSale+.4);
  const verPop=(vt=='pop'||vt=='popup');
  let st=null;
  if(Ls.popup){for(const e of Ls.popup.estados)if(t>=e.t)st=e;if(!st)st=Ls.popup.estados[0]}
  else{for(let i=s;i>=0&&!st;i--)if(L[i].popup)st=L[i].popup.estados[L[i].popup.estados.length-1]}
  if(!st)st={nivel:'gris',neta:'···',pv:'',gas:null,ok:0,nl:'netos'};  // antes del primer pop-up
  pop(st);
  let neta=st.neta;
  if(neta&&neta.cuenta){const c=neta.cuenta;neta='$'+Math.round(lerp(c.de,c.a,eo(p(t,c.t0,c.t1))))}
  $('neta').textContent=neta;$('nl').textContent=' '+(st.nl||'netos');$('pv').textContent=st.pv;
  const gasEn=Ls.popup&&Ls.popup.gas_en!=null?Ls.popup.gas_en:-99;
  const gv=st.gas!=null&&t>=gasEn;$('gas').textContent=st.gas!=null?st.gas:D.gasRef;  // oculta, conserva su lugar para que el texto no salte
  $('gas').style.visibility=gv?'visible':'hidden';$('gas').style.color=gv?'#EF4444':'';
  const bump=st.bump!=null?st.bump:-9;$('neta').style.display='inline-block';$('neta').style.transform=`scale(${1+(t>=bump?.28*Math.max(0,1-(t-bump)/.28):0)})`;
  const pb=back(pin);
  $('popw').style.opacity=cl(pin*3)*(1-pout)*(verPop?1:0);
  $('popw').style.transform=`translateZ(${lerp(40,170,eio(p(t,E-.19,E+.81)))}px) translateY(${lerp(520,0,pb)}px) rotateX(${lerp(35,0,eo(pin))}deg) scale(${lerp(.7,1,pb)})`;
  const fi=Ls.popup?Ls.popup.fichas:null;
  [['f1',0],['f2',.14],['f3',.28]].forEach(([id,d])=>{const k=fi==null?0:(fi===1?1:back(p(t,fi+d,fi+d+.25)));$(id).style.transform=`scale(${lerp(.6,1,k)})`;$(id).style.opacity=cl(k*2)});
  // ===== tarjeta de oferta («esa oferta» de la voz) junto al pop-up
  let cop=0,c0=D.chipRuns.length?D.chipRuns[0][0]:0;
  D.chipRuns.forEach(r=>{cop=Math.max(cop,p(t,r[0]+.07,r[0]+.37)*(1-p(t,r[1],r[1]+.27)));if(t>=r[0])c0=r[0]});
  const ck=Ls.chip&&Ls.chip.kfs?Ls.chip.kfs:null;let kf=null;
  if(ck){for(const k of ck)if(t>=k.t)kf=k;if(!kf)kf=ck[0]}else{for(let i=s;i>=0&&!kf;i--)if(L[i].chip)kf=L[i].chip.kfs[L[i].chip.kfs.length-1]}
  if(kf){$('cprice').textContent=kf.precio;$('csub').textContent=kf.sub}
  const gl=Math.max(0,...D.chipBrillo.map(x=>Math.sin(Math.PI*p(t,x-.1,x+1.0)))),es=Math.max(0,...D.chipPulso.map(x=>Math.sin(Math.PI*p(t,x-.1,x+.6))));
  $('chip').style.opacity=cop;
  $('chip').style.transform=`translateZ(170px) translateY(${lerp(-50,0,eo(p(t,c0,c0+.3)))}px) scale(${1+.07*es+.05*gl})`;
  $('chip').style.boxShadow=`0 20px 60px rgba(0,0,0,.5),0 0 ${60*gl}px ${12*gl}px rgba(34,197,94,${.55*gl})`;
  const sc=D.scan.find(x=>t>=x[0]&&t<x[1]+.05);$('scan').style.left=(sc?lerp(-120,780,p(t,sc[0],sc[1])):-120)+'px';$('scan').style.opacity=sc?1:0;
  const cu=D.cursor.find(x=>t>=x.a&&t<x.fin);
  if(cu){const q=p(t,cu.a,cu.a+1.0),bn=back(p(t,cu.ban,cu.ban+.25));$('cur').style.opacity=t<cu.ac_fin?cl(q*3):0;$('cur').style.transform=`translate(${lerp(300,615,eo(q))}px,${lerp(260,66,eo(q))}px)`;
    $('ban').style.opacity=cl(bn*2)*(1-p(t,cu.ban_fin,cu.ban_fin+.4));$('ban').style.transform=`scale(${lerp(1.8,1,bn)})`}else{$('cur').style.opacity=0;$('ban').style.opacity=0}
  let mo=0,mk=.6;D.ml.forEach(x=>{const o=p(t,x-.05,x+.1)*(1-p(t,x+.78,x+1.03));if(o>mo){mo=o;mk=back(p(t,x-.05,x+.25))}});
  $('ml').style.opacity=mo;$('ml').style.transform=`scale(${lerp(.6,1,mk)})`;
  const stp=D.stamp.find(x=>t>=x.t&&t<x.fin);
  if(stp){const sk=back(p(t,stp.t,stp.t+.2));$('stamp').textContent=stp.txt;$('stamp').style.opacity=cl(sk*2);$('stamp').style.transform=`rotate(-6deg) scale(${lerp(2.2,1,sk)})`}else $('stamp').style.opacity=0;
  const rg=D.ring.find(x=>t>=x.a-.1&&t<x.b+.3);
  if(rg){const rk=p(t,rg.a,rg.b);$('ring').style.opacity=1;$('rc').style.strokeDashoffset=264*rk;$('rt').textContent=Math.max(0,Math.ceil(rg.seg*(1-rk)))+' s'}else $('ring').style.opacity=0;
  // ===== golpes de texto
  const g=D.golpes.filter(x=>t>=x.a&&t<x.b).pop();if(g)golpe(g,t);else $('gol').style.opacity=0;
  // ===== gancho (cuadro 0 completo)
  const ho=1-p(t,1.5,1.72);$('hook').style.opacity=ho;$('hook').style.transform=`scale(${1+.08*Math.max(0,1-t/.22)}) rotate(-3deg)`;
  // ===== metas
  if(Ls.metas){const m=Ls.metas,ak=eio(p(t,m.km,m.km+.5)),ah=eio(p(t,m.hora,m.hora+.5));
    $('mk').textContent='$'+(m.km_v[0]+Math.round((m.km_v[1]-m.km_v[0])*ak))+'/km';$('mkb').style.width=(30+25*ak)+'%';$('mh').textContent='$'+(m.hora_v[0]+Math.round((m.hora_v[1]-m.hora_v[0])*ah))+'/h';$('mhb').style.width=(40+25*ah)+'%';
    const gk=Math.max(0,Math.sin(Math.PI*p(t,m.km-.05,m.km+.8))),gh=Math.max(0,Math.sin(Math.PI*p(t,m.hora-.05,m.hora+.8)));
    $('rk').style.transform=`scale(${1+.07*gk})`;$('rk').style.boxShadow=`0 0 ${30*gk}px rgba(34,197,94,${.7*gk})`;
    $('rh').style.transform=`scale(${1+.07*gh})`;$('rh').style.boxShadow=`0 0 ${30*gh}px rgba(34,197,94,${.7*gh})`}
  // ===== ojo tachado y altavoz
  const oj=D.ojo.reduce((m,x)=>Math.max(m,p(t,x.a-.1,x.a+.1)*(1-p(t,x.b,x.b+.25))),0),av=D.altavoz.reduce((m,x)=>Math.max(m,p(t,x.a-.15,x.a+.05)*(1-p(t,x.b,x.b+.25))),0);
  $('eye').style.opacity=oj;$('spk').style.opacity=av;$('vozic').style.opacity=1;
  ['w1','w2','w3'].forEach((id,i)=>$(id).style.opacity=.3+.7*Math.max(0,Math.sin(t*12-i*1.2)));
  $('vozic').style.transform=`translateZ(0) scale(${1+.05*Math.sin(t*12)})`;
  // ===== subtítulos
  const c=D.caps.find(x=>t>=x[0]&&t<x[1]);
  if(c&&t<D.capFin){$('cap').innerHTML=c[2];const k=back(p(t,c[0],c[0]+.16));$('cap').style.opacity=cl(k*2)*(1-p(t,c[1]-.06,c[1]));$('cap').style.transform=`scale(${lerp(.9,1,k)})`}else $('cap').style.opacity=0;
  // ===== cierre (CTA con logo)
  const C=D.cta;
  $('e4').style.opacity=p(t,C.t+.1,C.t+.5);$('vel').style.opacity=.9;
  $('ejemplo').style.opacity=1-p(t,C.t+.1,C.t+.5);  // «Números de ejemplo» visible mientras haya cifras (todo menos el cierre)
  const lg=eo(p(t,C.t+.1,C.t+.65));$('logo').style.transform=`scale(${lerp(1.4,1,lg)})`;$('logo').style.filter=`blur(${lerp(18,0,lg)}px)`;$('logo').style.opacity=lg;
  $('lc').style.strokeDashoffset=22*(1-eio(p(t,C.t+.55,C.t+.85)));
  const pk=back(p(t,C.pill,C.pill+.3)),gq=C.gp!=null?back(p(t,C.gp-.05,C.gp+.25)):0;
  $('gp').style.opacity=cl(gq*2);$('gp').style.transform=`scale(${lerp(.7,1,gq)})`;
  $('pill').style.opacity=cl(pk*2);$('pill').style.transform=`scale(${lerp(.7,1,pk)*(t>C.t+1.55?1+.05*Math.sin((t-C.t-1.55)*7.5):1)})`;
  $('lv').style.opacity=t>C.t+1.15?.65+.35*Math.sin(t*7):1;
  $('sub10').style.opacity=p(t,C.handles,C.handles+.3);
  // ===== lavado de color, progreso y destello en cada corte
  let wo=0,wc='#22C55E';D.lavado.forEach(x=>{const o=Math.max(0,1-(t-x.t)/.4)*(t>=x.t);if(o>wo){wo=o;wc=COL[x.color]||x.color}});
  $('wash').style.background=wc;$('wash').style.opacity=.3*wo;
  $('prog').style.width=(t/D.total*100)+'%';
  $('flash').style.opacity=Math.max(0,...L.slice(1).map(x=>.14*Math.max(0,1-Math.abs(t-x.start)/.1)));
}
const __base=set;
function setLoop(t){
  if(t<D.total-.6){__base(t);return}
  __base(0);                          // el último cuadro es idéntico al cuadro 0
  const f=1-eio(p(t,D.total-.6,D.total-.05)); // el cierre se disuelve hacia el inicio
  $('e4').style.opacity=f;$('vel').style.opacity=.9;
  $('logo').style.transform='scale(1)';$('logo').style.filter='none';$('logo').style.opacity=1;$('lc').style.strokeDashoffset=0;
  $('pill').style.opacity=1;$('pill').style.transform='scale(1)';$('gp').style.opacity=D.cta.gp!=null?1:0;$('gp').style.transform='scale(1)';$('sub10').style.opacity=1;$('lv').style.opacity=1;
  $('prog').style.width=(100*f)+'%';
}
window.set=setLoop;setLoop(0);
