// Estilo «claro» (referencia: mapa de sistema con nodos). Todo en función de window.set(t); D = tiempos.
const $=h=>{const d=document.createElement('div');d.innerHTML=h.trim();return d.firstChild};
const clamp=(x,a=0,b=1)=>Math.min(b,Math.max(a,x));
const ease=x=>1-Math.pow(1-x,3);
const norm=s=>s.toLowerCase().replace(/[:,.¿?¡!…]/g,'');
function T(i,spec){const m=/^(.*?)(?:#(\d+))?(?:@([+-]?[\d.]+))?$/.exec(spec);const w=norm(m[1]),n=+(m[2]||0),off=+(m[3]||0);
 const sc=D.scenes[i-1];let k=0;for(const [x,a] of sc.words){if(norm(x)===w){if(k===n)return sc.start+a+off;k++;}}
 throw new Error('no encuentro '+spec+' en la línea '+i);}
const LS=i=>D.scenes[i-1].start, LE=i=>D.scenes[i-1].start+D.scenes[i-1].dur;
const TOTAL=D.total, HOOK_BACK=TOTAL-0.5;
const G='#22C55E',Y='#FACC15',R='#EF4444',K='#0C0C10';
const col={verde:[G,'#04210F'],amarillo:[Y,K],rojo:[R,'#fff'],hueso:[K,'#fff']};
const st=document.getElementById('st');
const layer=(id,css)=>{const d=document.createElement('div');d.id=id;d.style.cssText='position:absolute;inset:0;'+(css||'');st.appendChild(d);return d};
const Lhook=layer('hook'),Lchain=layer('chain'),Lcard=layer('cardw'),Lgol=layer('gol'),Lcta=layer('cta'),Lflash=layer('flash','pointer-events:none;opacity:0'),Lcap=layer('cap'),Lej=layer('ej'),Lbar=layer('bar');
const items=[];
function add(parent,html,t0,t1,tf){const el=$('<div style="position:absolute;inset:0">'+html+'</div>');parent.appendChild(el);el.style.opacity=0;items.push({el,t0,t1:t1==null?1e9:t1,tf:tf||''});return el;}
const px=(o)=>Object.entries(o).map(([k,v])=>k+':'+v+(typeof v==='number'?'px':'')).join(';');

// ---------- iconos ----------
const ic=(n,c=K)=>{const s='fill="none" stroke="'+c+'" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"';
 const P=[
 '<path d="M10 20v-8h8M46 20v-8h-8M10 36v8h8M46 36v8h-8"/><path d="M14 28h28"/>',
 '<path d="M28 7C28 7 12 26 12 36a16 16 0 0 0 32 0C44 26 28 7 28 7z"/>',
 '<rect x="18" y="6" width="20" height="44" rx="10"/><circle cx="28" cy="17" r="3.5"/><circle cx="28" cy="28" r="3.5"/><circle cx="28" cy="39" r="3.5"/>',
 '<rect x="9" y="22" width="14" height="26" rx="3"/><rect x="33" y="8" width="14" height="40" rx="3"/>',
 '<path d="M10 46V30M22 46V18M34 46V26M46 46V10"/>'];
 return '<svg width="56" height="56" viewBox="0 0 56 56" '+s+'>'+P[n]+'</svg>';};
const NOM=['LECTURA','SEMÁFORO','COMPARADOR','MI DÍA'];const IC=[0,2,3,4];

// ---------- fondo ----------
st.style.background='linear-gradient(180deg,#FFFFFF 0%,#FBFBF7 55%,#F2EFD9 100%)';
st.appendChild($('<div style="position:absolute;left:-200px;right:-200px;bottom:-420px;height:900px;border-radius:50%;background:radial-gradient(closest-side,rgba(250,204,21,.18),rgba(34,197,94,.08) 60%,transparent)"></div>'));

// ---------- barra, ejemplo ----------
const barEl=$('<div style="position:absolute;left:0;top:0;height:8px;background:'+G+';width:0"></div>');Lbar.appendChild(barEl);
Lej.appendChild($('<div style="position:absolute;left:0;right:0;top:1385px;text-align:center;font:500 26px/1 \'JetBrains Mono\';letter-spacing:5px;color:#8A8678">NÚMEROS DE EJEMPLO</div>'));
const ejEl=Lej.firstChild;

// ---------- HOOK: mapa del sistema (cuadro 0 completo) ----------
const hookItems=[];
const title=$('<div style="position:absolute;left:60px;right:60px;top:150px;text-align:center;font:800 78px/1.02 \'Bricolage Grotesque\';letter-spacing:-2px;color:'+K+'"><div style="display:flex;justify-content:center;gap:16px;margin-bottom:22px"><i style="width:34px;height:34px;border-radius:50%;background:'+G+'"></i><i style="width:34px;height:34px;border-radius:50%;background:'+Y+'"></i><i style="width:34px;height:34px;border-radius:50%;background:'+R+'"></i></div>Un sistema que decide<br>cada viaje</div>');
Lhook.appendChild(title);
const map=$('<div style="position:absolute;left:0;top:0;width:1080px;height:1920px;transform-origin:270px 570px"></div>');Lhook.appendChild(map);
const POS=[[60,520],[540,520],[540,790],[60,790]];
const SUB=['Lee la oferta','Tus metas','Compara dos ofertas','Tu día'];
// conectores
let svg='<svg width="1080" height="1920" style="position:absolute;left:0;top:0" fill="none" stroke="#CFC9AE" stroke-width="4" stroke-dasharray="3 14" stroke-linecap="round">';
const ctr=p=>[p[0]+240,p[1]+95];
svg+='<path d="M'+(POS[0][0]+480)+' '+(POS[0][1]+95)+'H'+POS[1][0]+'"/>';
svg+='<path d="M'+(POS[1][0]+240)+' '+(POS[1][1]+190)+'V'+POS[2][1]+'"/>';
svg+='<path d="M'+POS[2][0]+' '+(POS[2][1]+95)+'H'+(POS[3][0]+480)+'"/>';
svg+='</svg>';
map.appendChild($(svg));
POS.forEach((p,i)=>{map.appendChild($('<div style="position:absolute;left:'+p[0]+'px;top:'+p[1]+'px;width:480px;height:190px;border-radius:36px;background:#fff;border:3px solid #E8E4D2;box-shadow:0 20px 50px rgba(80,70,20,.10);display:flex;align-items:center;gap:26px;padding:0 34px"><div style="width:100px;height:100px;border-radius:26px;border:3px solid #E2DDC4;background:#FBFAF2;display:flex;align-items:center;justify-content:center;flex:none">'+ic(IC[i])+'</div><div><div style="font:700 24px/1 \'JetBrains Mono\';letter-spacing:3px;color:#8A8678">MÓDULO '+(i+1)+'</div><div style="font:800 40px/1.1 \'Bricolage Grotesque\';color:'+K+';margin-top:8px">'+NOM[i]+'</div><div style="font:400 28px/1.2 \'DM Sans\';color:#6A665A;margin-top:4px">'+SUB[i]+'</div></div></div>'));});
map.appendChild($('<div style="position:absolute;left:300px;top:1070px;width:480px;height:190px;display:flex;align-items:center;justify-content:center;gap:24px"><svg width="90" height="120" viewBox="0 0 56 56" fill="none"><rect x="17" y="4" width="22" height="48" rx="11" fill="'+K+'"/><circle cx="28" cy="15" r="5" fill="'+R+'"/><circle cx="28" cy="28" r="5" fill="'+Y+'"/><circle cx="28" cy="41" r="5" fill="'+G+'"/></svg><div style="font:800 76px/1 \'Bricolage Grotesque\';color:'+K+'">Checa</div></div>'));

// ---------- CADENA superior ----------
const NX=i=>150+i*260;
const chainBox=$('<div style="position:absolute;inset:0"></div>');Lchain.appendChild(chainBox);Lchain.style.opacity=0;
let sv='<svg width="1080" height="400" style="position:absolute;left:0;top:0" stroke-linecap="round" stroke-width="4">';
for(let i=0;i<3;i++)sv+='<path class="cl" d="M'+(NX(i)+60)+' 210H'+(NX(i+1)-60)+'" stroke="#D8D3BD" stroke-dasharray="3 12" fill="none"/>';
chainBox.appendChild($(sv+'</svg>'));
const nodes=[];
for(let i=0;i<4;i++){const n=$('<div style="position:absolute;left:'+(NX(i)-58)+'px;top:152px;width:116px;text-align:center"><div class="nb" style="width:116px;height:116px;border-radius:30px;background:#fff;border:3px solid #E2DDC4;display:flex;align-items:center;justify-content:center;box-shadow:0 14px 30px rgba(80,70,20,.10)">'+ic(IC[i])+'</div><div style="margin-top:14px;font:700 21px/1 \'JetBrains Mono\';letter-spacing:2px;color:#6A665A;white-space:nowrap;margin-left:-20px;margin-right:-20px">'+NOM[i]+'</div></div>');chainBox.appendChild(n);nodes.push(n);}
const ACT=[[2,0],[3,1],[4,1],[5,2],[6,2],[7,1],[8,3],[9,-1]];

// ---------- TARJETA de contenido ----------
const card=$('<div style="position:absolute;left:70px;top:470px;width:940px;height:730px;border-radius:46px;background:#fff;border:2px solid #ECE8D6;box-shadow:0 34px 90px rgba(80,70,20,.12)"></div>');Lcard.appendChild(card);Lcard.style.opacity=0;
const C=(h,t0,t1,still)=>{const el=add(card,h,t0,t1,'');if(still===true)el._still=1;return el;};
const mono=(s,x,y,c='#8A8678',sz=28)=>'<div style="position:absolute;left:'+x+'px;top:'+y+'px;font:700 '+sz+'px/1 \'JetBrains Mono\';letter-spacing:3px;color:'+c+'">'+s+'</div>';
const big=(s,x,y,sz,c,w=800,fam='Bricolage Grotesque')=>'<div style="position:absolute;left:'+x+'px;top:'+y+'px;font:'+w+' '+sz+'px/1 \''+fam+'\';letter-spacing:-'+Math.round(sz/25)+'px;color:'+c+';white-space:nowrap">'+s+'</div>';
function oferta(x,y,w,precio,l1,l2){const s=w/760;return '<div style="position:absolute;left:'+x+'px;top:'+y+'px;width:'+w+'px;height:'+(300*s)+'px;border-radius:'+(36*s)+'px;background:#fff;border:3px solid #E8E4D2;box-shadow:0 18px 44px rgba(80,70,20,.14);overflow:hidden">'
 +'<div style="position:absolute;left:'+(36*s)+'px;top:'+(28*s)+'px;font:800 '+(118*s)+'px/1 \'Bricolage Grotesque\';letter-spacing:-'+(4*s)+'px;color:'+K+'">'+precio+'</div>'
 +'<div style="position:absolute;left:'+(38*s)+'px;top:'+(172*s)+'px;font:500 '+(34*s)+'px/1.35 \'JetBrains Mono\';color:#4A463A">'+l1+'<br>'+l2+'</div>'
 +'<div style="position:absolute;right:'+(30*s)+'px;bottom:'+(28*s)+'px;width:'+(210*s)+'px;height:'+(78*s)+'px;border-radius:99px;background:'+K+';color:#fff;font:700 '+(30*s)+'px/'+(78*s)+'px \'DM Sans\';text-align:center">Aceptar</div></div>';}

function ofertaU(x,y,w,precio,a,v,ad1,ad2){const s=w/760;const px=n=>(n*s).toFixed(1)+'px';
 const star='<svg viewBox="0 0 24 24" width="'+px(34)+'" height="'+px(34)+'" fill="#0C0C10" style="vertical-align:-.12em"><path d="M12 1.5l3.2 7.2 7.8.8-5.9 5.2 1.7 7.7L12 18.4 5.2 22.4l1.7-7.7L1 9.5l7.8-.8z"/></svg>';
 return '<div style="position:absolute;left:'+x+'px;top:'+y+'px;width:'+w+'px;height:'+px(640)+';border-radius:'+px(40)+';background:#fff;border:2px solid #E8E4D2;box-shadow:0 22px 56px rgba(80,70,20,.16);overflow:hidden">'
 +'<div style="position:absolute;left:'+px(34)+';top:'+px(30)+';padding:'+px(8)+' '+px(24)+';border-radius:'+px(10)+';background:#0C0C10;color:#fff;font:700 '+px(32)+'/1.2 \'DM Sans\'">Viaje</div>'
 +'<svg viewBox="0 0 16 16" width="'+px(40)+'" height="'+px(40)+'" style="position:absolute;right:'+px(34)+';top:'+px(30)+'" fill="none" stroke="#0C0C10" stroke-width="2.1" stroke-linecap="square"><path d="M2 2l12 12M14 2L2 14"/></svg>'
 +'<div style="position:absolute;left:'+px(34)+';top:'+px(96)+';font:800 '+px(126)+'/1 \'Bricolage Grotesque\';letter-spacing:'+px(-4)+';color:#0C0C10">'+precio+'</div>'
 +'<div style="position:absolute;left:'+px(38)+';top:'+px(236)+';font:500 '+px(34)+'/1 \'DM Sans\';color:#0C0C10">'+star+'&nbsp; 4.92</div>'
 +'<div style="position:absolute;left:'+px(60)+';top:'+px(330)+';width:'+px(4)+';height:'+px(110)+';background:#0C0C10"></div>'
 +'<div style="position:absolute;left:'+px(50)+';top:'+px(318)+';width:'+px(24)+';height:'+px(24)+';border-radius:50%;background:#0C0C10"></div>'
 +'<div style="position:absolute;left:'+px(50)+';top:'+px(436)+';width:'+px(24)+';height:'+px(24)+';border-radius:'+px(4)+';background:#0C0C10"></div>'
 +'<div style="position:absolute;left:'+px(104)+';top:'+px(300)+'"><div style="font:700 '+px(38)+'/1.1 \'DM Sans\';color:#0C0C10">'+a+'</div><div style="font:400 '+px(30)+'/1.2 \'DM Sans\';color:#6A665A;margin-top:'+px(6)+'">'+ad1+'</div></div>'
 +'<div style="position:absolute;left:'+px(104)+';top:'+px(418)+'"><div style="font:700 '+px(38)+'/1.1 \'DM Sans\';color:#0C0C10">'+v+'</div><div style="font:400 '+px(30)+'/1.2 \'DM Sans\';color:#6A665A;margin-top:'+px(6)+'">'+ad2+'</div></div>'
 +'<div style="position:absolute;left:'+px(30)+';right:'+px(30)+';bottom:'+px(28)+';height:'+px(96)+';border-radius:'+px(14)+';background:#0C0C10;overflow:hidden"><div style="position:absolute;left:0;top:0;bottom:0;width:62%;background:#2B2B33"></div><div style="position:relative;text-align:center;color:#fff;font:700 '+px(36)+'/'+px(96)+' \'DM Sans\'">Viaje disponible</div></div></div>';}
function popApp(o){const T0='#A3A3AD',N={hdr:'···',hc:T0,bc:'#3A3A45',amt:'',m1:['···',T0,''],m2:['···',T0,''],tot:''};o=Object.assign(N,o);
 const met=m=>'<div style="flex:1;text-align:center;font:500 58px/1 \'JetBrains Mono\';color:'+m[1]+';white-space:nowrap">'+(m[2]?m[2]+' ':'')+m[0]+'</div>';
 return '<div style="position:absolute;left:100px;top:90px;width:780px;border-radius:58px;background:#16161C;border:10px solid '+o.bc+';box-shadow:0 30px 70px rgba(0,0,0,.28)">'
 +'<div style="text-align:center;padding-top:30px;font:800 62px/1.15 \'Bricolage Grotesque\';letter-spacing:6px;color:'+o.hc+'">'+o.hdr+'</div>'
 +'<svg width="40" height="40" viewBox="0 0 24 24" style="position:absolute;right:26px;top:26px" fill="none" stroke="'+T0+'" stroke-width="3" stroke-linecap="round"><path d="M5 5l14 14M19 5L5 19"/></svg>'
 +'<div style="text-align:center;padding:6px 0;font:800 168px/1.1 \'Bricolage Grotesque\';letter-spacing:-3px;color:#F4F4EF">'+o.amt+'</div>'
 +'<div style="display:flex;align-items:center;padding:18px 20px 0">'+met(o.m1)+'<div style="width:4px;height:56px;background:rgba(163,163,173,.43);margin:0 12px"></div>'+met(o.m2)+'</div>'
 +'<div style="text-align:center;padding:22px 0 34px;font:500 43px/1.2 \'JetBrains Mono\';color:'+T0+'">'+o.tot+'</div></div>';}
const ficha=(v,m,c,bg)=>'<div style="flex:1;height:150px;border-radius:26px;background:'+bg+';border:3px solid '+c+';display:flex;flex-direction:column;align-items:center;justify-content:center"><b style="font:700 46px \'JetBrains Mono\';color:'+c+'">'+v+'</b><span style="font:400 30px \'DM Sans\';color:#C8C8D0;margin-top:6px">'+m+'</span></div>';

// L2 — módulo uno: lee la oferta
{const i=2,a=LS(i),e=LE(i);
 C(ofertaU(120,10,700,'$60.00','A 5 min (2 km)','Viaje: 20 min (10 km)','Calle Hidalgo 120','Av. Principal 450'),a,e);
 const sc=C('<div style="position:absolute;left:80px;top:60px;width:780px;height:8px;border-radius:4px;background:linear-gradient(90deg,transparent,'+G+',transparent);box-shadow:0 0 30px '+G+'"></div>',T(i,'lee')-.05,e);
 sc._scan=[T(i,'lee')-.05,T(i,'oferta')+.55,40,500];
 C('<div style="position:absolute;left:375px;top:866px;width:150px;height:150px;zoom:.72;border-radius:50%;background:'+G+';display:flex;align-items:center;justify-content:center"><svg width="80" height="80" viewBox="0 0 56 56" fill="none" stroke="#04210F" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"><path d="M12 29l11 11 21-24"/></svg></div>',T(i,'sola')-.05,e);
 C('<div style="position:absolute;left:778px;top:866px;width:150px;height:150px;zoom:.72"><svg width="150" height="150" viewBox="0 0 150 150" fill="none"><circle cx="75" cy="75" r="62" stroke="'+R+'" stroke-width="12"/><path d="M31 119L119 31" stroke="'+R+'" stroke-width="12" stroke-linecap="round"/><circle cx="75" cy="75" r="16" fill="'+K+'" opacity=".85"/></svg></div>',T(i,'toques')-.05,e);}
// L3 — módulo dos: tu meta
{const i=3,a=LS(i),e=LE(i);
 C(ofertaU(260,30,420,'$60.00','A 5 min (2 km)','Viaje: 20 min (10 km)','Calle Hidalgo 120','Av. Principal 450'),T(i,'compara')-.1,e);
 const mc=(x,l,v)=>'<div style="position:absolute;left:'+x+'px;top:420px;width:410px;height:270px;border-radius:34px;background:#FBFAF2;border:3px solid #E2DDC4;text-align:center"><div style="margin-top:34px;font:700 28px/1 \'JetBrains Mono\';letter-spacing:3px;color:#8A8678">'+l+'</div><div style="margin-top:34px;font:800 132px/1 \'Bricolage Grotesque\';letter-spacing:-5px;color:'+K+'">'+v+'</div></div>';
 C(mc(40,'META POR HORA','$200'),T(i,'hora')-.1,e);
 C(mc(490,'META POR KM','$10'),T(i,'kilómetro')-.1,e);}
// L4 — rechazar (pop-up real de la app)
{const i=4,a=LS(i),e=LE(i),r=T(i,'rechazar'),v='25 min (12.0 km) total';
 const h=['$144/h',R,'✕'],k=['$5.0/km',R,'✕'],n=['···','#A3A3AD',''];
 C(popApp({amt:'$60',tot:v}),a,r-.05);
 C(popApp({hdr:'RECHAZAR',hc:R,bc:R,amt:'$60',m1:h,m2:k,tot:v}),r-.05,e,true);
 C(popApp({amt:'$60',m1:h,tot:v}),T(i,'144')-.05,r-.05,true);
 C(popApp({amt:'$60',m1:h,m2:k,tot:v}),T(i,'5')-.05,r-.05,true);}
// L5-6 — comparar dos ofertas (izquierda 240/h, derecha 300/h)
{const e=LE(6),d=T(5,'dos');
 const cl=C(ofertaU(40,40,420,'$100.00','A 5 min (2 km)','Viaje: 20 min (9 km)','Calle Hidalgo 120','Av. Principal 450'),d-.1,e);
 const cr=C(ofertaU(480,40,420,'$70.00','A 4 min (1.5 km)','Viaje: 10 min (5 km)','Calle Juárez 85','Calzada Norte 300'),d+.25,e);
 C('<div style="position:absolute;left:431px;top:150px;width:78px;height:78px;z-index:2;border-radius:50%;background:'+K+';color:#fff;font:800 30px/78px \'Bricolage Grotesque\';text-align:center">VS</div>',T(5,'mismo'),e);
 C('<div style="position:absolute;left:300px;top:496px;width:340px;height:70px;border-radius:99px;background:#F3F0DC;border:3px solid #E2DDC4;text-align:center;font:700 30px/64px \'JetBrains Mono\';letter-spacing:4px;color:'+K+'">COMPARA</div>',T(5,'compara'),e);
 C('<div style="position:absolute;left:70px;top:416px;width:360px;height:76px;border-radius:99px;background:'+Y+';color:'+K+';text-align:center;font:800 40px/76px \'JetBrains Mono\'">$240/h</div>',T(6,'240')-.05,e);
 C('<div style="position:absolute;left:510px;top:416px;width:360px;height:76px;border-radius:99px;background:'+G+';color:#04210F;text-align:center;font:800 40px/76px \'JetBrains Mono\'">$300/h</div>',T(6,'300')-.05,e);
 C('<div style="position:absolute;left:300px;top:610px;width:340px;height:92px;border-radius:99px;background:'+G+';color:#04210F;text-align:center;font:800 46px/92px \'Bricolage Grotesque\';letter-spacing:5px;box-shadow:0 0 60px rgba(34,197,94,.5)">GANA ✓</div>',T(6,'gana'),e);
 cl._gana=[T(6,'gana'),cl.firstChild];cr._gana=[T(6,'gana'),cr.firstChild];}
// L7 — aceptar
{const i=7,a=LS(i),e=LE(i),ac=T(i,'aceptar'),n=T(i,'cumple'),v='14 min (6.5 km) total';
 C(popApp({amt:'$70',tot:v}),a,ac-.05);
 C(popApp({hdr:'ACEPTAR',hc:G,bc:G,amt:'$70',tot:v}),ac-.05,n-.05,true);
 C(popApp({hdr:'ACEPTAR',hc:G,bc:G,amt:'$70',m1:['$300/h',G,'✓'],m2:['$10.8/km',G,'✓'],tot:v}),n-.05,e,true);
 C('<div style="position:absolute;left:250px;top:640px;width:440px;height:64px;border-radius:99px;background:'+G+';color:#04210F;text-align:center;font:800 32px/64px \'JetBrains Mono\';letter-spacing:3px">LAS 2 METAS ✓</div>',T(i,'metas')-.1,e);}
// L8 — mi día
{const i=8,a=LS(i),e=LE(i),d=T(i,'día');
 C(mono('MI DÍA',70,50,'#8A8678',34),d-.1,e);
 const H=[.50,.66,.38,.82,1,.60,.90],base=620,max=420;
 C('<div style="position:absolute;left:60px;right:60px;top:'+(base-max*.62)+'px;border-top:4px dashed #CFC9AE"></div>',d,e);
 C(mono('META $200/H',700,base-max*.62-44,'#8A8678',24),d+.1,e);
 H.forEach((h,k)=>{const c=h>=.62?G:Y;C('<div style="position:absolute;left:'+(80+k*118)+'px;top:'+(base-max*h)+'px;width:82px;height:'+(max*h)+'px;border-radius:20px 20px 8px 8px;background:'+c+';transform-origin:50% 100%"></div>',d+.12*k,e,'');});
 C(mono('CADA HORA',70,650,'#6A665A',32),T(i,'hora'),e);}
// L9 — números, no suposiciones
{const i=9,a=LS(i),e=LE(i);
 C(mono('NÚMEROS',46,70,'#16A34A',36)+big('$300/h',46,170,150,G),T(i,'números')-.3,e);
 C(mono('SUPOSICIONES',500,70,'#8A8678',36)+big('¿$?',560,170,200,'#9A968A'),T(i,'suposiciones')-.3,e);
 C('<div style="position:absolute;left:520px;top:290px;width:360px;height:14px;border-radius:7px;background:'+R+';transform:rotate(-8deg)"></div>',T(i,'suposiciones')+.2,e);}

// ---------- GOLPES (pastillas sobre la tarjeta) ----------
const GOL=[[1,'decide','DECIDE','amarillo',.9],[1,'tres','3 SEGUNDOS','verde',1],[2,'uno','MÓDULO 1','hueso',.9],[2,'sola','SOLA','verde',.9],
 [3,'dos','MÓDULO 2','hueso',.7],[3,'hora','POR HORA','amarillo',.8],[3,'kilómetro','POR KM','amarillo',1],
 [4,'144','$144/H','rojo',.8],[4,'rechazar','RECHAZAR','rojo',1],
 [5,'tres','MÓDULO 3','hueso',.7],[5,'dos','2 OFERTAS','amarillo',.9],[5,'solita','SOLITA','verde',.9],
 [6,'240','$240/H','amarillo',.9],[6,'300','$300/H','verde',.9],[7,'aceptar','ACEPTAR','verde',.8],[7,'metas','LAS 2 METAS','verde',1],
 [8,'día','TU DÍA','amarillo',.9],[8,'hora','CADA HORA','verde',1],[9,'números','NÚMEROS','verde',.9],[9,'no','NO','rojo',.7],[9,'suposiciones','SUPOSICIONES','rojo',1]];
GOL.forEach(([i,w,txt,k,d],gi)=>{const t=T(i,w)-.04;const [bg,fg]=col[k];const nx=GOL[gi+1];const t1=Math.min(t+d,(nx&&nx[0]===i)?T(i,nx[1])-.06:1e9);
 const el=$('<div style="position:absolute;left:0;right:0;top:'+(i===1?392:325)+'px;text-align:center"><span style="display:inline-block;padding:14px 46px;border-radius:99px;background:'+bg+';color:'+fg+';font:800 '+(txt.length>9?84:110)+'px/1.05 \'Bricolage Grotesque\';letter-spacing:-2px;box-shadow:0 16px 40px rgba(0,0,0,.18);white-space:nowrap">'+txt+'</span></div>');
 Lgol.appendChild(el);el.style.opacity=0;items.push({el,t0:t,t1:t1,tf:'',pop:1});});
// lavados
const LAV=[[4,'rechazar',R],[6,'gana',G],[7,'aceptar',G]].map(([i,w,c])=>({t:T(i,w),c}));
const SAC=[T(4,'rechazar'),T(9,'no')];

// ---------- SUBTÍTULOS ----------
const chunks=[];
D.scenes.forEach((sc,idx)=>{let cur=[];sc.words.forEach((w,k)=>{cur.push([w[0],sc.start+w[1],sc.start+w[2]]);const fin=/[.:,]$/.test(w[0])||cur.length>=3||k===sc.words.length-1;if(fin){chunks.push(cur);cur=[];}});});
chunks.forEach((ch,k)=>{const el=$('<div style="position:absolute;left:40px;right:40px;top:1250px;text-align:center;font:800 78px/1.05 \'Bricolage Grotesque\';letter-spacing:0;color:#fff;-webkit-text-stroke:13px '+K+';paint-order:stroke fill;white-space:nowrap">'+ch.map(w=>'<span>'+w[0]+'</span>').join(' ')+'</div>');
 Lcap.appendChild(el);el.style.display='none';el._ch=ch;el._a=k===0?0:ch[0][1]-.04;el._b=(chunks[k+1]&&chunks[k+1][0][1]-ch[ch.length-1][2]<.3)?chunks[k+1][0][1]-.04:ch[ch.length-1][2]+.25;});

// ---------- CTA ----------
const cta=$('<div style="position:absolute;left:0;right:0;top:500px;text-align:center"></div>');Lcta.appendChild(cta);Lcta.style.opacity=0;
const c0=LS(10);
function CT(h,t0){const el=$(h);cta.appendChild(el);el.style.opacity=0;items.push({el,t0,t1:1e9,tf:''});return el;}
CT('<div style="display:flex;justify-content:center;align-items:center;gap:30px"><svg width="150" height="200" viewBox="0 0 56 56" fill="none"><rect x="17" y="4" width="22" height="48" rx="11" fill="'+K+'"/><circle cx="28" cy="15" r="5" fill="'+R+'"/><circle cx="28" cy="28" r="5" fill="'+Y+'"/><circle cx="28" cy="41" r="5" fill="'+G+'"/></svg><div style="font:800 190px/1 \'Bricolage Grotesque\';letter-spacing:-6px;color:'+K+'">Checa</div></div>',c0);
const pill=CT('<div style="margin:70px auto 0;width:660px;height:130px;border-radius:99px;background:'+K+';color:#fff;font:800 56px/130px \'Bricolage Grotesque\'">Comenta CHECA</div>',T(10,'checa')-.1);
CT('<div style="margin:44px auto 0;display:inline-flex;align-items:center;gap:20px;padding:20px 38px;border-radius:99px;background:#fff;border:3px solid #E2DDC4;font:700 40px \'DM Sans\';color:'+K+'"><svg width="40" height="40" viewBox="0 0 24 24"><path d="M3 2l18 10L3 22z" fill="'+G+'"/></svg>Google Play</div>',T(10,'google')-.1);
CT('<div style="margin-top:30px;font:500 28px \'JetBrains Mono\';color:#8A8678;letter-spacing:2px">Solo Android</div>',T(10,'google')+.1);
CT('<div style="margin-top:50px;font:500 30px \'JetBrains Mono\';color:#6A665A;letter-spacing:1px">@checa.driver · @Eduardoalv9802</div>',T(10,'google')+.3);

// ---------- set(t) ----------
const vis=(el,p,tf,popk)=>{if(el._still){el.style.opacity=p;el.style.transform='none';return;}el.style.opacity=p;el.style.transform=(tf||'')+' translateY('+((1-p)*26)+'px) scale('+(popk?(.8+.2*Math.min(1.1,p*1.1)):(.95+.05*p))+')';};
window.set=function(t){
 const back=clamp((t-HOOK_BACK)/0.5);          // regreso al primer cuadro (loop)
 const tl=t>=HOOK_BACK?0:t;                      // el gancho se anima con tiempo local
 // gancho
 const z=ease(clamp((tl-(LS(2)-.5))/.7));
 const hookA=t<LS(2)-.5?1:(1-z);
 const hookShow=t>=HOOK_BACK?back:hookA;
 Lhook.style.opacity=hookShow;
 map.style.transform='translate(0,0) scale('+(1+.025*tl+ (t>=HOOK_BACK?0:z*1.35))+')';
 title.style.opacity=1;
 // cadena y tarjeta
 const inCard=t>=LS(2)-.3&&t<LE(9)+.2;
 const ca=(t<LS(2)-.3?0:ease(clamp((t-(LS(2)-.3))/.4)))*(t>LE(9)-.1?1-clamp((t-(LE(9)-.1))/.3):1)*(t>=HOOK_BACK?1-back:1);
 Lchain.style.opacity=ca;Lcard.style.opacity=ca;
 Lcta.style.opacity=(t<LS(10)-.1?0:1)*(t>=HOOK_BACK?1-back:1);
 // nodos
 nodes.forEach((n,k)=>{let w=0;ACT.forEach(([ln,idx],j)=>{const p=ease(clamp((t-LS(ln))/.3));const prev=j?ACT[j-1][1]:-9;
   if(t>=LS(ln)&&(j===ACT.length-1||t<LS(ACT[j+1][0]))){const on=idx===-1?1:(idx===k?1:0);const was=prev===-1?1:(prev===k?1:0);w=was+(on-was)*p;}});
  const a=.4+.6*w;n.style.opacity=a;const nb=n.firstChild;nb.style.borderColor=w>.5?K:'#E2DDC4';nb.style.transform='scale('+(1+.12*w)+')';nb.style.background=w>.5?'#FBFAF2':'#fff';});
 chainBox.querySelectorAll('.cl').forEach((l,k)=>{const on=ACT.some(([ln,idx])=>t>=LS(ln)&&t<LE(ln)&&(idx>k||idx===-1));l.setAttribute('stroke',on?'#0C0C10':'#D8D3BD');});
 // items
 items.forEach(it=>{const p=ease(clamp((t-it.t0)/(it.pop?.22:.35)));const q=t>it.t1?ease(clamp((t-it.t1)/(it.pop?.07:.2))):0;
  vis(it.el,p*(1-q),it.tf,it.pop);
  const el=it.el;
  if(el._scan){const [a,b,y0,y1]=el._scan;const u=clamp((t-a)/(b-a));el.firstChild.style.top=(y0+(y1-y0)*u)+'px';el.style.opacity=(u>0&&u<1?1:0)*(1-q);}
  if(el._cnt){const [a,b,v0,v1]=el._cnt;const u=ease(clamp((t-a)/(b-a)));el.querySelector('.cnt').textContent='$'+Math.round(v0+(v1-v0)*u);}
 });
 // ganadora: borde verde / otra opaca (L7)
 items.forEach(it=>{const el=it.el;if(el._gana){const g=el._gana[0],box=el._gana[1];const u=clamp((t-g)/.3);
  const isC=el.innerHTML.indexOf('$120.00')>=0;
  if(isC){box.style.borderColor=u>0?G:'#E8E4D2';box.style.borderWidth=(3+5*u)+'px';}
  else{el.style.opacity=(parseFloat(el.style.opacity))*(1-.55*u);}}});
 // bandera L9: crecer barras
 items.forEach(it=>{const el=it.el;const k=el.firstChild;if(k&&k.style&&k.style.transformOrigin==='50% 100%'){const p=ease(clamp((t-it.t0)/.55));k.style.transform='scaleY('+p+')';el.style.transform='none';}});
 // subtítulos
 const ts=t>=HOOK_BACK?0:t;
 Lcap.childNodes.forEach(el=>{const on=ts>=el._a&&ts<el._b;el.style.display=on?'block':'none';
  if(on){[...el.children].forEach((s,k)=>{const w=el._ch[k];s.style.color=(ts>=w[1]&&ts<w[2]+.05)?G:'#fff';});}});
 Lcap.style.opacity=1;
 Lej.style.opacity=t>=HOOK_BACK?back:(t<LS(10)-.1?1:0);
 // barra
 barEl.style.width=(1080*clamp(t/TOTAL))+'px';
 // lavados y sacudida
 let f=0;LAV.forEach(l=>{const u=t-l.t;if(u>=0&&u<.5){f=Math.max(f,.22*(1-u/.5));Lflash.style.background=l.c;}});Lflash.style.opacity=f;
 let sx=0,sy=0;SAC.forEach(s=>{const u=t-s;if(u>=0&&u<.35){const a=14*(1-u/.35);sx+=Math.sin(u*90)*a;sy+=Math.cos(u*70)*a*.6;}});
 st.style.transform='translate('+sx+'px,'+sy+'px)';
};
window.set(0);
