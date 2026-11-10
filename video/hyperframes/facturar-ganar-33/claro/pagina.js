// CHECA 33 «Las casetas» (estilo claro, escena cenital): un auto visto desde atrás con una torre de billetes en el techo.
// Cada gasto es una caseta de peaje que se come un pedazo de la torre, a escala. Todo en función de window.set(t); D = tiempos.
const $=h=>{const d=document.createElement('div');d.innerHTML=h.trim();return d.firstChild};
const clamp=(x,a=0,b=1)=>Math.min(b,Math.max(a,x));
const ease=x=>1-Math.pow(1-x,3);
const eio=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const back=x=>{const c=1.7;return 1+(c+1)*Math.pow(x-1,3)+c*Math.pow(x-1,2)};
const lerp=(a,b,x)=>a+(b-a)*x;
const c01=(x,a,b)=>clamp((x-a)/(b-a));
const norm=s=>s.toLowerCase().replace(/[:,.¿?¡!…]/g,'');
function T(i,spec){const m=/^(.*?)(?:#(\d+))?(?:@([+-]?[\d.]+))?$/.exec(spec);const w=norm(m[1]),n=+(m[2]||0),off=+(m[3]||0);
 const sc=D.scenes[i-1];let k=0;for(const [x,a] of sc.words){if(norm(x)===w){if(k===n)return sc.start+a+off;k++;}}
 throw new Error('no encuentro '+spec+' en la línea '+i);}
const LS=i=>D.scenes[i-1].start, LE=i=>D.scenes[i-1].start+D.scenes[i-1].dur;
const TOTAL=D.total, HOOK_BACK=TOTAL-0.5;
const G='#22C55E',Y='#FACC15',R='#EF4444',K='#0C0C10';
const col={verde:[G,'#04210F'],amarillo:[Y,K],rojo:[R,'#fff'],hueso:[K,'#fff']};
const fm=v=>'$'+Math.round(v).toLocaleString('en-US');
const st=document.getElementById('st');
const layer=(id,css)=>{const d=document.createElement('div');d.id=id;d.style.cssText='position:absolute;inset:0;'+(css||'');st.appendChild(d);return d};
const Lw=layer('world'),Lgol=layer('gol'),Lcta=layer('cta'),Lflash=layer('flash','pointer-events:none;opacity:0'),Lcap=layer('cap'),Lej=layer('ej'),Lbar=layer('bar');
const items=[];
const px=n=>n.toFixed(1)+'px';

// ---------- tiempos de las palabras clave ----------
const tHoy=T(1,'hoy'),tNo=T(1,'no'),tGanaste=T(1,'ganaste');
const tTragan=T(2,'tragan'),t400=T(2,'400'),tGas=T(2,'gasolina');
const t1100=T(3,'1100'),tDes=T(3,'desgaste');
const tFact=T(4,'facturar'),tMismo=T(4,'mismo'),tGanar=T(4,'ganar');
const tViaje=T(5,'viaje'),tCiegas=T(5,'ciegas');
const tOferta=T(6,'oferta'),t150=T(6,'150'),t16=T(6,'16');
const tCheca=T(7,'checa'),t32=T(7,'32');
const t118=T(8,'118'),tDec=T(9,'decides');

// ---------- fondo y carretera (vista desde atrás) ----------
st.style.background='linear-gradient(180deg,#FFFFFF 0%,#FBFBF7 55%,#F2EFD9 100%)';
st.appendChild($('<div style="position:absolute;left:-200px;right:-200px;bottom:-420px;height:900px;border-radius:50%;background:radial-gradient(closest-side,rgba(250,204,21,.18),rgba(34,197,94,.08) 60%,transparent)"></div>'));
const RY=u=>250+990*u, RW=u=>220+700*u, SCL=u=>RW(u)/920;
const Z=(n,css)=>{const d=document.createElement('div');d.style.cssText='position:absolute;left:0;top:0;width:1080px;height:1920px;pointer-events:none;z-index:'+n+';'+(css||'');Lw.appendChild(d);return d};
const zRoad=Z(1),zRoute=Z(2),zLane=Z(3),zBooth=Z(4),zBoard=Z(5),zCar=Z(6),zPan=Z(7),zTow=Z(8),zTire=Z(9),zHose=Z(10),zSl=Z(11),zPop=Z(12);
zRoad.innerHTML='<svg width="1080" height="1920" style="position:absolute;left:0;top:0"><polygon points="0,250 430,250 49,1330 0,1330" fill="#E3EDD6"/><polygon points="1080,250 650,250 1031,1330 1080,1330" fill="#E3EDD6"/><polygon points="430,250 650,250 1031,1330 49,1330" fill="#2B2B33"/><line x1="430" y1="250" x2="49" y2="1330" stroke="#F4F4EF" stroke-width="8"/><line x1="650" y1="250" x2="1031" y2="1330" stroke="#F4F4EF" stroke-width="8"/><g id="dashes"></g></svg>';
const dashG=zRoad.querySelector('#dashes');const dashes=[];
for(let k=0;k<12;k++){const p=document.createElementNS('http://www.w3.org/2000/svg','polygon');p.setAttribute('fill','#F4F4EF');dashG.appendChild(p);dashes.push(p);}
// distancia recorrida: el camino se detiene cuando el auto para en una caseta
const STOPS=[[tNo,tGas+.8],[tDes-.6,tDes+.9],[tFact-.3,LS(5)+.3],[LS(7)-.2,LE(9)+1]];
const dist=t=>t-STOPS.reduce((s,[a,b])=>s+clamp(t-a,0,b-a),0);
function fDash(t){const ph=(dist(t)*.35)%1;
 dashes.forEach((p,k)=>{const u=((k/12)+ph)%1,u2=u+.04;if(u2>1||u<.02){p.setAttribute('points','');return;}
  const w0=2+11*u,w1=2+11*u2,y0=RY(u),y1=RY(u2);p.setAttribute('points',(540-w0)+','+y0+' '+(540+w0)+','+y0+' '+(540+w1)+','+y1+' '+(540-w1)+','+y1);});}

// ---------- auto visto desde atrás ----------
const car=$('<div style="position:absolute;left:330px;top:950px;width:420px;height:270px"><svg width="420" height="270" viewBox="0 0 420 270">'
 +'<rect x="40" y="0" width="340" height="120" rx="34" fill="#33333D"/><rect x="66" y="16" width="288" height="78" rx="18" fill="#B9CBD3"/>'
 +'<g id="driver"><circle cx="210" cy="52" r="26" fill="#F1C9A5"/><path d="M186 44 Q210 24 234 44 L234 40 Q210 18 186 40Z" fill="#3A2A20"/><rect x="170" y="74" width="80" height="22" rx="11" fill="#2A2A33"/></g>'
 +'<g id="venda" style="opacity:0"><rect x="178" y="42" width="64" height="17" rx="6" fill="#0C0C10"/><path d="M242 45 l18 -9 l-4 22Z" fill="#0C0C10"/></g>'
 +'<rect x="10" y="96" width="400" height="124" rx="32" fill="#33333D"/><rect x="26" y="118" width="96" height="34" rx="12" fill="#EF4444"/><rect x="298" y="118" width="96" height="34" rx="12" fill="#EF4444"/>'
 +'<rect x="168" y="150" width="84" height="38" rx="7" fill="#F4F4EF"/><rect x="22" y="206" width="76" height="64" rx="15" fill="#0C0C10"/><rect x="322" y="206" width="76" height="64" rx="15" fill="#0C0C10"/></svg></div>');
zCar.appendChild(car);
const venda=car.querySelector('#venda');

// ---------- torre de billetes ----------
const bills=(a,b)=>'repeating-linear-gradient(0deg,'+a+' 0 14px,'+b+' 14px 17px)';
const mkTow=(bg,extra)=>{const d=$('<div style="position:absolute;box-sizing:border-box;border-radius:6px 6px 3px 3px;background:'+bg+';box-shadow:inset 0 0 0 3px rgba(0,0,0,.28);'+(extra||'')+'"></div>');zTow.appendChild(d);return d};
const towN=mkTow(bills('#CFE6CF','#7FB58A')),towG=mkTow(bills('#86EFAC','#16A34A'),'opacity:0'),towGh=mkTow(bills('#DAD7C8','#B4B09A'),'opacity:0'),
 sl1=mkTow(bills('#F87171','#B91C1C'),'opacity:0;z-index:3;box-shadow:0 0 36px rgba(239,68,68,.6),inset 0 0 0 3px rgba(0,0,0,.28)'),
 sl2=mkTow(bills('#F87171','#B91C1C'),'opacity:0;z-index:3;box-shadow:0 0 36px rgba(239,68,68,.6),inset 0 0 0 3px rgba(0,0,0,.28)');
const ZZ=[0,.8,.3,1,.2,.9,.4,1,.1,.7,.3,0];
const zig=(w,h,a)=>{if(a<=.5)return 'none';let p=[[0,h],[w,h]];for(let i=ZZ.length-1;i>=0;i--)p.push([w*i/(ZZ.length-1),a*ZZ[i]]);return 'polygon('+p.map(q=>q[0].toFixed(1)+'px '+q[1].toFixed(1)+'px').join(',')+')';};
function box(el,cx,base,sc,H0,amp){const w=220*sc,h=H0*sc;el.style.left=px(cx-w/2);el.style.top=px(base-h);el.style.width=px(w);el.style.height=px(h);el.style.clipPath=zig(w,h,(amp||0)*sc);}
const lbl=(css,size)=>{const d=$('<div style="position:absolute;text-align:center;font:800 '+size+'px/1 \'Bricolage Grotesque\';letter-spacing:-3px;color:#F4F4EF;text-shadow:0 4px 18px rgba(0,0,0,.65);white-space:nowrap;'+css+'"></div>');zTow.appendChild(d);return d};
const lbTop=lbl('width:420px',104),lbSide=lbl('width:200px;text-align:left',66),lbL=lbl('width:320px;color:#8A8678',64),lbR=lbl('width:320px',64);
const panLab=(s)=>{const d=$('<div style="position:absolute;width:320px;text-align:center;font:700 30px/1 \'JetBrains Mono\';letter-spacing:4px;white-space:nowrap">'+s+'</div>');zPan.appendChild(d);return d};
const panL=$('<div style="position:absolute;left:70px;top:360px;width:320px;height:540px;border-radius:36px;background:#fff;border:3px solid #E8E4D2;box-shadow:0 22px 56px rgba(80,70,20,.16);opacity:0"></div>');
const panR=$('<div style="position:absolute;left:690px;top:360px;width:320px;height:540px;border-radius:36px;background:#fff;border:3px solid #E8E4D2;box-shadow:0 22px 56px rgba(80,70,20,.16);opacity:0"></div>');
zPan.appendChild(panL);zPan.appendChild(panR);
const tbL=panLab('FACTURAS'),tbR=panLab('GANAS');tbL.style.color='#8A8678';tbR.style.color='#16A34A';
const neq=$('<div style="position:absolute;left:480px;top:560px;width:120px;height:120px;border-radius:50%;background:'+K+';color:#fff;font:800 78px/120px \'Bricolage Grotesque\';text-align:center;opacity:0;box-shadow:0 16px 40px rgba(0,0,0,.25)">≠</div>');zPan.appendChild(neq);
// migajas del desgaste
const CR=[[-70,-.4,.9],[-35,.2,1.2],[-5,-.1,.7],[25,.5,1.1],[55,-.3,.8],[80,.3,1.3],[-55,.1,1],[10,-.6,.9],[45,.4,.7]];
const crumbs=CR.map(()=>{const d=$('<div style="position:absolute;width:26px;height:16px;border-radius:3px;background:'+bills('#CFE6CF','#7FB58A')+';opacity:0;box-shadow:inset 0 0 0 2px rgba(0,0,0,.28)"></div>');zTow.appendChild(d);return d});
// llanta que rueda
const tire=$('<div style="position:absolute;left:0;top:0;width:170px;height:170px;opacity:0"><svg width="170" height="170" viewBox="0 0 100 100"><circle cx="50" cy="50" r="48" fill="#1B1B22"/><circle cx="50" cy="50" r="40" fill="none" stroke="#3A3A44" stroke-width="6" stroke-dasharray="7 5"/><circle cx="50" cy="50" r="23" fill="#9C9576"/><circle cx="50" cy="50" r="9" fill="#2A2A33"/><path d="M50 27v10M50 63v10M27 50h10M63 50h10" stroke="#2A2A33" stroke-width="5"/></svg></div>');zTire.appendChild(tire);

// ---------- casetas ----------
function booth(label,icon){
 const w=$('<div style="position:absolute;left:0;top:0;width:360px;height:220px;transform-origin:0 0"></div>');
 w.innerHTML='<div style="position:absolute;left:0;top:0;width:340px;height:70px;border-radius:16px;background:'+K+';color:'+Y+';font:800 44px/70px \'Bricolage Grotesque\';text-align:center;letter-spacing:2px">'+label+'</div>'
  +'<div style="position:absolute;left:20px;top:60px;width:300px;height:160px;border-radius:0 0 16px 16px;background:#fff;border:5px solid '+K+';border-top:0"><div style="position:absolute;left:100px;top:22px;width:100px;height:100px;display:flex;align-items:center;justify-content:center">'+icon+'</div></div>'
  +'<div class="arm" style="position:absolute;left:330px;top:100px;height:26px;width:500px;border-radius:13px;transform-origin:0 50%;background:repeating-linear-gradient(45deg,#EF4444 0 26px,#fff 26px 52px);box-shadow:0 0 0 3px '+K+';opacity:0"></div>';
 zBooth.appendChild(w);return w;}
const pump='<svg width="96" height="96" viewBox="0 0 56 56" fill="none" stroke="'+K+'" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"><rect x="12" y="8" width="24" height="40" rx="4"/><rect x="18" y="14" width="12" height="9"/><path d="M36 22h6v14a3 3 0 0 0 6 0V18l-5-5"/></svg>';
const tireIc='<svg width="96" height="96" viewBox="0 0 56 56" fill="none" stroke="'+K+'" stroke-width="3.5" stroke-linecap="round"><circle cx="28" cy="28" r="20"/><circle cx="28" cy="28" r="7"/><path d="M28 8v13M28 35v13M8 28h13M35 28h13"/></svg>';
const bA=booth('GASOLINA',pump),bB=booth('DESGASTE',tireIc);
const armA=bA.querySelector('.arm');
const BK=1.35;
function place(b,u,op,ang){const s=SCL(u)*BK,xc=540-RW(u)/2-110*s,yb=RY(u);
 b.style.transform='translate('+xc+'px,'+yb+'px) scale('+s+') translate(-180px,-220px)';b.style.opacity=op;
 const arm=b.querySelector('.arm');const L=((540+RW(u)/2+16)-(xc+180*s-5*s))/s;arm.style.width=L+'px';
 if(ang!=null){arm.style.transform='rotate('+ang+'deg)';arm.style.opacity=1;}
 return {xc,yb,s};}
const UST=.535,SA=SCL(UST)*BK,XA=540-RW(UST)/2-110*SA,NOZ=[XA+180*SA,RY(UST)-70*SA];
// manguera (de la caseta a la torre)
const hose=$('<svg width="1080" height="1920" style="position:absolute;left:0;top:0"><path id="hp" d="M'+NOZ[0]+' '+NOZ[1]+' C'+(NOZ[0]+70)+' '+(NOZ[1]+50)+' 340 650 440 620" fill="none" stroke="#2A2A33" stroke-width="30" stroke-linecap="round" pathLength="1" stroke-dasharray="1" stroke-dashoffset="1"/><path d="M'+NOZ[0]+' '+NOZ[1]+' C'+(NOZ[0]+70)+' '+(NOZ[1]+50)+' 340 650 440 620" fill="none" stroke="#EF4444" stroke-width="8" stroke-linecap="round" pathLength="1" stroke-dasharray="1" stroke-dashoffset="1" id="hp2"/></svg>');zHose.appendChild(hose);
const hp=hose.querySelector('#hp'),hp2=hose.querySelector('#hp2');
const bez=(p0,p1,p2,p3,s)=>{const a=1-s;return [a*a*a*p0[0]+3*a*a*s*p1[0]+3*a*s*s*p2[0]+s*s*s*p3[0],a*a*a*p0[1]+3*a*a*s*p1[1]+3*a*s*s*p2[1]+s*s*s*p3[1]];};

// ---------- cartel de la oferta, semáforo y carriles (L6–L9) ----------
const board=$('<div style="position:absolute;left:375px;top:575px;width:330px;height:165px;border-radius:28px;background:#fff;border:3px solid #E8E4D2;box-shadow:0 16px 40px rgba(80,70,20,.2);opacity:0"><div style="position:absolute;left:18px;top:14px;padding:3px 14px;border-radius:7px;background:'+K+';color:#fff;font:700 21px/1.3 \'DM Sans\'">Viaje</div><div id="bpr" style="position:absolute;left:14px;top:46px;padding:0 10px;border-radius:12px;font:800 78px/1.05 \'Bricolage Grotesque\';letter-spacing:-3px;color:'+K+'">$150.00</div><div id="bkm" style="position:absolute;left:18px;top:128px;padding:0 8px;border-radius:8px;font:500 25px/1.1 \'JetBrains Mono\';color:#4A463A">32 min · 16 km</div></div>');zBoard.appendChild(board);
const semaf=$('<div style="position:absolute;left:375px;top:600px;width:330px;height:130px;border-radius:65px;background:'+K+';opacity:0;box-shadow:0 16px 40px rgba(0,0,0,.3)"><i id="sr" style="position:absolute;left:44px;top:25px;width:80px;height:80px;border-radius:50%;background:#5A2222"></i><i id="sy" style="position:absolute;left:125px;top:25px;width:80px;height:80px;border-radius:50%;background:#5A4A10"></i><i id="sg" style="position:absolute;left:206px;top:25px;width:80px;height:80px;border-radius:50%;background:#14501F"></i></div>');zBoard.appendChild(semaf);
const hiP=board.querySelector('#bpr'),hiK=board.querySelector('#bkm');
const lane=(txt,x,c)=>{const d=$('<div style="position:absolute;left:'+(x-150)+'px;top:752px;width:300px;text-align:center;transform:scaleY(.7);opacity:0"><span style="display:inline-block;padding:6px 26px;border-radius:99px;background:'+K+';font:800 42px/1.1 \'Bricolage Grotesque\';letter-spacing:5px;color:'+c+'">'+txt+'</span></div>');zLane.appendChild(d);return d};
const laneA=lane('ACEPTAR',375,'#4ADE80'),laneP=lane('PASAR',705,'#F4F4EF');
// ruta de 16 km sobre la carretera
const route=$('<svg width="1080" height="1920" style="position:absolute;left:0;top:0"><path id="rt" d="M540 930 L540 262" stroke="#22C55E" stroke-width="14" stroke-linecap="round" fill="none" pathLength="1" stroke-dasharray="1" stroke-dashoffset="1"/><circle id="rd" cx="540" cy="262" r="16" fill="#22C55E" opacity="0"/></svg>');zRoute.appendChild(route);
const rt=route.querySelector('#rt'),rd=route.querySelector('#rd');
// pop-up de la app (copiado de Popup.kt)
function popApp(o){const T0='#A3A3AD';
 const met=m=>'<div style="flex:1;text-align:center;font:500 58px/1 \'JetBrains Mono\';color:'+m[1]+';white-space:nowrap">'+(m[2]?m[2]+' ':'')+m[0]+'</div>';
 return '<div id="pp" style="position:absolute;left:100px;top:90px;width:780px;border-radius:58px;background:#16161C;border:10px solid '+o.bc+';box-shadow:0 30px 70px rgba(0,0,0,.28)">'
 +'<div style="text-align:center;padding-top:30px;font:800 62px/1.15 \'Bricolage Grotesque\';letter-spacing:6px;color:'+o.hc+'">'+o.hdr+'</div>'
 +'<svg width="40" height="40" viewBox="0 0 24 24" style="position:absolute;right:26px;top:26px" fill="none" stroke="'+T0+'" stroke-width="3" stroke-linecap="round"><path d="M5 5l14 14M19 5L5 19"/></svg>'
 +'<div style="text-align:center;padding:6px 0;font:800 168px/1.1 \'Bricolage Grotesque\';letter-spacing:-3px;color:#F4F4EF">'+o.amt+'</div>'
 +'<div style="display:flex;align-items:center;padding:18px 20px 0">'+met(o.m1)+'<div style="width:4px;height:56px;background:rgba(163,163,173,.43);margin:0 12px"></div>'+met(o.m2)+'</div>'
 +'<div style="text-align:center;padding:22px 0 34px;font:500 43px/1.2 \'JetBrains Mono\';color:'+T0+'">'+o.tot+'</div></div>';}
const SCP=.62;
const popA=$('<div style="position:absolute;left:'+(540-490*SCP)+'px;top:'+(250-90*SCP)+'px;width:1000px;height:640px;transform-origin:0 0;transform:scale('+SCP+');opacity:0"></div>');zPop.appendChild(popA);
const popB=$('<div style="position:absolute;inset:0;transform-origin:490px 340px">'+popApp({hdr:'CONSIDERAR',hc:Y,bc:Y,amt:'$118',m1:['$281/h',G,'✓'],m2:['$9.4/km',R,'✕'],tot:'32 min (16.0 km) total'})+'</div>');popA.appendChild(popB);

// ---------- escena en función del tiempo ----------
function scene(t){
 fDash(t);
 const bob=Math.sin(dist(t)*38)*2.2*(dist(t)>.01?1:0);
 car.style.transform='translateY('+bob+'px)';
 // conductor y venda (a ciegas)
 const vi=ease(c01(t,tCiegas-.05,tCiegas+.25)),vo=ease(c01(t,tOferta-.05,tOferta+.35));
 venda.style.opacity=vi*(1-vo);venda.style.transform='translateY('+(-26*vo)+'px)';
 // --- caseta GASOLINA (L1–L2)
 const uA=t<tGas+.9?lerp(.13,UST,ease(c01(t,0,tNo))):lerp(UST,1.25,eio(c01(t,tGas+.9,tGas+1.9)));
 const angA=t<tGas+.4?lerp(-85,0,back(c01(t,tNo-.12,tNo+.2))):lerp(0,-85,ease(c01(t,tGas+.4,tGas+.8)));
 place(bA,uA,1-c01(uA,.95,1.08),angA);
 // --- caseta DESGASTE (L3)
 const uB=t<tDes+.9?lerp(.08,UST,ease(c01(t,LS(3)-.5,tDes-.6))):lerp(UST,1.25,eio(c01(t,tDes+.9,tDes+1.9)));
 place(bB,uB,(t<LS(3)-.5?0:ease(c01(t,LS(3)-.5,LS(3)-.2)))*(1-c01(uB,.95,1.08)),null);
 // --- torre de billetes
 const gas=ease(c01(t,tGas-.05,tGas+.4));
 let H0=380-(380-279)*gas,cx=540,base=950,sc=1;
 const pm=eio(c01(t,tFact,tFact+.6));cx=lerp(540,850,pm);base=lerp(950,840,pm);sc=lerp(1,.87,pm);
 const rb=eio(c01(t,LS(5)+.05,LS(5)+.65));cx=lerp(cx,540,rb);base=lerp(base,950,rb);sc=lerp(sc,1,rb);H0=lerp(H0,170,rb);
 const g32=ease(c01(t,t32+.35,t32+.75));H0-=36*g32;
 // mordida de la llanta
 const tx=lerp(300,880,c01(t,tDes-.2,tDes+.6));
 const bite=ease(c01(tx,430,690))*22*(1-rb);
 const gv=Math.max(ease(c01(t,tGanar,tGanar+.3))*(1-ease(c01(t,LS(5)+.15,LS(5)+.55))),ease(c01(t,t118,t118+.3)));
 box(towN,cx,base,sc,H0,bite);box(towG,cx,base,sc,H0,bite);
 towN.style.opacity=1-gv;towG.style.opacity=gv;
 // rebanada roja de gasolina (400 → manguera → caseta)
 const pth=ease(c01(t,tGas,tGas+.5)),sv=(t>=t400-.05?ease(c01(t,t400-.05,t400+.1)):0)*(1-c01(t,tGas+.42,tGas+.5));
 {const w=220*lerp(1,.18,pth),h=101*lerp(1,.18,pth),x=lerp(540,NOZ[0],pth),y=lerp(570+50,NOZ[1],pth);
  sl1.style.left=px(x-w/2);sl1.style.top=px(y-h/2);sl1.style.width=px(w);sl1.style.height=px(h);sl1.style.opacity=sv;}
 // manguera
 const ext=ease(c01(t,tTragan,tTragan+.35))*(1-ease(c01(t,tGas+.6,tGas+.95)));
 hp.setAttribute('stroke-dashoffset',1-ext);hp2.setAttribute('stroke-dashoffset',1-ext);
 // fantasma de FACTURAS (1,500) que sale de la torre y se vuelve el letrero de la izquierda
 {const gh=eio(c01(t,tFact,tFact+.6)),go=1-ease(c01(t,LS(5)+.05,LS(5)+.35));
  box(towGh,lerp(540,230,gh),lerp(950,840,gh),lerp(1,.87,gh),380,0);towGh.style.opacity=(t>=tFact-.02?1:0)*go;}
 // etiquetas del monto
 const val=1500-400*gas;
 const showTop=t<tFact?1:0;
 lbTop.style.opacity=showTop;lbTop.textContent=fm(val);lbTop.style.left=px(cx-210);lbTop.style.top=px(base-H0*sc-122);
 {const pk=Math.max(0,1-Math.abs(t-t1100)/.25);lbTop.style.transform='scale('+(1+.12*pk)+')';lbTop.style.color='#F4F4EF';}
 // panel L4
 {const pa=back(c01(t,tFact-.05,tFact+.4)),pf=1-ease(c01(t,LS(5)+.05,LS(5)+.35)),o=clamp(pa*1.6)*pf,tr='scale('+lerp(.8,1,pa)+')';
  panL.style.opacity=o;panR.style.opacity=o;panL.style.transform=tr;panR.style.transform=tr;
  tbL.style.left=px(70);tbL.style.top=px(862);tbR.style.left=px(690);tbR.style.top=px(862);tbL.style.opacity=o;tbR.style.opacity=o;
  lbL.style.opacity=o;lbR.style.opacity=o;lbL.textContent='$1,500';lbR.textContent='$1,100';lbR.style.color=gv>.5?'#16A34A':K;lbR.style.textShadow='none';lbL.style.textShadow='none';
  lbL.style.left=px(70);lbL.style.top=px(840-330-96);lbR.style.left=px(690);lbR.style.top=px(840-242-96);
  const nk=c01(t,tMismo,tMismo+.28);neq.style.opacity=Math.min(1,nk*2)*pf;neq.style.transform='scale('+(2.4-1.4*Math.min(1.08,nk*1.08))+') rotate(-8deg)';}
 // etiqueta lateral del viaje
 {const on=t>=LS(6)?1:0;lbSide.style.opacity=ease(c01(t,t150-.05,t150+.2))*(1-0);lbSide.textContent=t>=t118?'$118':'$150';lbSide.style.left=px(670);lbSide.style.top=px(base-H0*sc/2-34);lbSide.style.color=t>=t118?'#4ADE80':'#F4F4EF';}
 // llanta + migajas
 {const rx=tx,tc=950-279+10;tire.style.opacity=(t>tDes-.6&&t<tDes+1.2)?1:0;
  tire.style.transform='translate('+(rx-85)+'px,'+(tc-85+Math.abs(Math.sin(rx*.05))*-6)+'px) rotate('+(rx*.7)+'deg)';
  const t0=tDes+.05;crumbs.forEach((c,k)=>{const dt=t-t0,[dx,vx,vy]=CR[k];const y=950-279-4+(-120*vy*dt+900*dt*dt*.55),x=540+dx+vx*160*dt;
   c.style.opacity=(dt>0&&dt<.9)?1-dt/.9:0;c.style.transform='translate('+(x-13)+'px,'+y+'px) rotate('+(dt*400*vx)+'deg)';});}
 // rebanada de −$32 que sube de la torre al pop-up
 {const p2=ease(c01(t,t32,t32+.6)),sv2=(t>=t32-.05?ease(c01(t,t32-.05,t32+.08)):0)*(1-c01(t,t32+.5,t32+.62));
  const w=220*lerp(1,.2,p2),h=36*lerp(1,.4,p2),x=lerp(540,540,p2),y=lerp(950-170+18,540,p2);
  sl2.style.left=px(x-w/2);sl2.style.top=px(y-h/2);sl2.style.width=px(w);sl2.style.height=px(h);sl2.style.opacity=sv2;}
 // --- cartel de la oferta (L6) y semáforo (L9)
 {const bi=back(c01(t,tOferta,tOferta+.4)),fl=ease(c01(t,tDec-.05,tDec+.12)),fb=ease(c01(t,tDec+.1,tDec+.3));
  board.style.opacity=clamp(bi*2)*(1-fl);board.style.transform='translateY('+lerp(-160,0,bi)+'px) scaleX('+(1-fl)+')';
  semaf.style.opacity=fb;semaf.style.transform='scaleX('+fb+')';
  const sy=ease(c01(t,tDec+.15,tDec+.4));document.getElementById('sy').style.background=sy>.5?Y:'#5A4A10';document.getElementById('sy').style.boxShadow=sy>.5?'0 0 40px '+Y:'none';
  hiP.style.background=t>=t150?'rgba(250,204,21,.55)':'transparent';hiK.style.background=t>=t16?'rgba(250,204,21,.45)':'transparent';
  const ln=ease(c01(t,tDec+.25,tDec+.55));laneA.style.opacity=ln;laneP.style.opacity=ln;}
 // ruta de 16 km
 {const rp=ease(c01(t,t16,t16+.9));rt.setAttribute('stroke-dashoffset',1-rp);rd.setAttribute('opacity',rp>.98?1:0);
  const rf=1-ease(c01(t,LS(7)+.2,LS(7)+.6));rt.setAttribute('opacity',rf);rd.setAttribute('opacity',(rp>.98?1:0)*rf);}
 // --- pop-up de la app: entra en «Checa» ya con su valor
 {const pop=c01(t,tCheca,tCheca+.38),pu=Math.max(0,1-Math.abs(t-t118-.05)/.28),pu2=Math.max(0,1-Math.abs(t-t32-.55)/.25),gl=Math.max(0,Math.sin(Math.PI*c01(t,tDec-.05,tDec+.9)));
  popA.style.opacity=clamp(pop*3);
  popB.style.transform='translateY('+((1-ease(pop))*-520)+'px) scale('+((.85+.15*Math.min(1.08,pop*1.08))*(1+.07*pu+.04*pu2))+')';
  document.getElementById('pp').style.boxShadow='0 30px 70px rgba(0,0,0,.28),0 0 '+(70*gl)+'px '+(16*gl)+'px rgba(250,204,21,.85)';}
}

// ---------- GOLPES (pastillas arriba) ----------
function add(parent,html,t0,t1,extra){const el=$('<div style="position:absolute;inset:0">'+html+'</div>');parent.appendChild(el);el.style.opacity=0;const it=Object.assign({el,t0,t1:t1==null?1e9:t1},extra||{});items.push(it);return el;}
const GOL=[[1,'hoy','$1,500 HOY','amarillo',99],[1,'no','NO GANASTE','rojo',1.3],
 [2,'doscientos','200 KM','hueso',.9],[2,'400','−$400','rojo',1.2],
 [3,'1100','$1,100','amarillo',.9],[3,'desgaste','DESGASTE','rojo',1.1],
 [4,'facturar','FACTURAR','hueso',.9],[4,'ganar','GANAR','verde',1],
 [5,'ciegas','A CIEGAS','rojo',1.1],
 [6,'150','$150','amarillo',.8],[6,'16','16 KM','amarillo',1],
 [7,'32','−$32','rojo',.9],[7,'antes','ANTES','amarillo',.9],
 [8,'118','$118 TUYOS','verde',1.4],[9,'decides','TÚ DECIDES','amarillo',1.1]];
GOL.forEach(([i,w,txt,k,d],gi)=>{const hook=gi===0;const t=hook?0:T(i,w)-.04;const [bg,fg]=col[k];const nx=GOL[gi+1];
 const t1=hook?T(nx[0],nx[1])-.06:Math.min(t+d,(nx&&nx[0]===i)?T(i,nx[1])-.06:1e9);
 const el=$('<div style="position:absolute;left:0;right:0;top:118px;text-align:center"><span style="display:inline-block;padding:12px 44px;border-radius:99px;background:'+bg+';color:'+fg+';font:800 '+(txt.length>9?84:110)+'px/1.05 \'Bricolage Grotesque\';letter-spacing:-2px;box-shadow:0 16px 40px rgba(0,0,0,.18);white-space:nowrap">'+txt+'</span></div>');
 Lgol.appendChild(el);el.style.opacity=0;items.push({el,t0:t,t1:t1,pop:1,hook});});
const LAV=[[1,'no',R],[2,'400',R],[4,'ganar',G],[8,'118',G],[9,'decides',Y]].map(([i,w,c])=>({t:T(i,w),c}));
const SAC=[T(1,'no'),T(2,'400'),T(3,'desgaste'),T(5,'ciegas')];

// ---------- SUBTÍTULOS ----------
const chunks=[];
D.scenes.forEach((sc,idx)=>{let cur=[];sc.words.forEach((w,k)=>{cur.push([w[0],sc.start+w[1],sc.start+w[2]]);const fin=/[.:,]$/.test(w[0])||cur.length>=3||k===sc.words.length-1;if(fin){chunks.push(cur);cur=[];}});});
chunks.forEach((ch,k)=>{const el=$('<div style="position:absolute;left:40px;right:40px;top:1250px;text-align:center;font:800 78px/1.05 \'Bricolage Grotesque\';letter-spacing:0;color:#fff;-webkit-text-stroke:13px '+K+';paint-order:stroke fill;white-space:nowrap">'+ch.map(w=>'<span>'+w[0]+'</span>').join(' ')+'</div>');
 Lcap.appendChild(el);el.style.display='none';el._ch=ch;el._a=k===0?0:ch[0][1]-.04;el._b=(chunks[k+1]&&chunks[k+1][0][1]-ch[ch.length-1][2]<.3)?chunks[k+1][0][1]-.04:ch[ch.length-1][2]+.25;});

// ---------- barra y letrero ----------
const barEl=$('<div style="position:absolute;left:0;top:0;height:8px;background:'+G+';width:0"></div>');Lbar.appendChild(barEl);
Lej.appendChild($('<div style="position:absolute;left:0;right:0;top:1385px;text-align:center;font:500 26px/1 \'JetBrains Mono\';letter-spacing:5px;color:#8A8678">NÚMEROS DE EJEMPLO</div>'));

// ---------- CTA ----------
const cta=$('<div style="position:absolute;left:0;right:0;top:500px;text-align:center"></div>');Lcta.appendChild(cta);Lcta.style.opacity=0;
const c0=LS(10);
function CT(h,t0){const el=$(h);cta.appendChild(el);el.style.opacity=0;items.push({el,t0,t1:1e9});return el;}
CT('<div style="display:flex;justify-content:center;align-items:center;gap:30px"><svg width="150" height="200" viewBox="0 0 56 56" fill="none"><rect x="17" y="4" width="22" height="48" rx="11" fill="'+K+'"/><circle cx="28" cy="15" r="5" fill="'+R+'"/><circle cx="28" cy="28" r="5" fill="'+Y+'"/><circle cx="28" cy="41" r="5" fill="'+G+'"/></svg><div style="font:800 190px/1 \'Bricolage Grotesque\';letter-spacing:-6px;color:'+K+'">Checa</div></div>',c0);
CT('<div style="margin:70px auto 0;width:660px;height:130px;border-radius:99px;background:'+K+';color:#fff;font:800 56px/130px \'Bricolage Grotesque\'">Comenta CHECA</div>',T(10,'checa')-.1);
CT('<div style="margin:44px auto 0;display:inline-flex;align-items:center;gap:20px;padding:20px 38px;border-radius:99px;background:#fff;border:3px solid #E2DDC4;font:700 40px \'DM Sans\';color:'+K+'"><svg width="40" height="40" viewBox="0 0 24 24"><path d="M3 2l18 10L3 22z" fill="'+G+'"/></svg>Google Play</div>',T(10,'google')-.1);
CT('<div style="margin-top:30px;font:500 28px \'JetBrains Mono\';color:#8A8678;letter-spacing:2px">Solo Android</div>',T(10,'google')+.1);
CT('<div style="margin-top:50px;font:500 30px \'JetBrains Mono\';color:#6A665A;letter-spacing:1px">@checa.driver · @Eduardoalv9802</div>',T(10,'google')+.3);

// ---------- set(t) ----------
window.set=function(t){
 const bk=clamp((t-HOOK_BACK)/0.5),tl=t>=HOOK_BACK?0:t;   // regreso al primer cuadro (loop)
 scene(tl);
 Lw.style.opacity=t<LS(10)-.15?1:(t>=HOOK_BACK?bk:1-ease(c01(t,LS(10)-.15,LS(10)+.15)));
 Lcta.style.opacity=(t<LS(10)-.1?0:1)*(t>=HOOK_BACK?1-bk:1);
 items.forEach(it=>{let p=ease(clamp((t-it.t0)/(it.pop?.22:.35)));const q=t>it.t1?ease(clamp((t-it.t1)/(it.pop?.07:.2))):0;
  let o=p*(1-q);if(it.hook&&t>=HOOK_BACK)o=bk;
  it.el.style.opacity=o;it.el.style.transform=(it.hook&&t<it.t1)?'none':'translateY('+((1-o)*26)+'px) scale('+(it.pop?(.8+.2*Math.min(1.1,o*1.1)):(.95+.05*o))+')';});
 const ts=t>=HOOK_BACK?0:t;
 Lcap.childNodes.forEach(el=>{const on=ts>=el._a&&ts<el._b;el.style.display=on?'block':'none';
  if(on){[...el.children].forEach((s,k)=>{const w=el._ch[k];s.style.color=(ts>=w[1]&&ts<w[2]+.05)?G:'#fff';});}});
 Lej.style.opacity=t>=HOOK_BACK?bk:(t<LS(10)-.1?1:0);
 barEl.style.width=(1080*clamp(t/TOTAL))+'px';
 let f=0;LAV.forEach(l=>{const u=t-l.t;if(u>=0&&u<.5){f=Math.max(f,.2*(1-u/.5));Lflash.style.background=l.c;}});Lflash.style.opacity=f;
 let sx=0,sy=0;SAC.forEach(s=>{const u=t-s;if(u>=0&&u<.35){const a=14*(1-u/.35);sx+=Math.sin(u*90)*a;sy+=Math.cos(u*70)*a*.6;}});
 st.style.transform='translate('+sx+'px,'+sy+'px)';
};
window.set(0);
