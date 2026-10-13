import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasMultas";

// Tema 14 «Multas, tránsito y papeles» (video de valor), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_MULTAS_1VOLT = duracion;
const en = crearEn(LINEAS, esc);

const K = { fondo: "#05060F", linea: "#A78BFA", relleno: "#120F2A", cian: "#22D3EE", magenta: "#D946EF", amarillo: "#FBBF24", verde: "#4ADE80", naranja: "#FB923C", azul: "#60A5FA", rosa: "#FB7185", texto: "#E9E7FF", tenue: "#8B84C9" };
// Cada escena tiene su color de trazo y un tinte de fondo distinto
const TONO = [
  { linea: "#FB923C", fondo: "#2A1206" },
  { linea: "#FB7185", fondo: "#2A0A14" },
  { linea: "#60A5FA", fondo: "#07193A" },
  { linea: "#A78BFA", fondo: "#170D36" },
  { linea: "#4ADE80", fondo: "#06281A" },
  { linea: "#FBBF24", fondo: "#2A1E04" },
  { linea: "#F43F5E", fondo: "#2E0610" },
  { linea: "#D946EF", fondo: "#260A30" },
  { linea: "#22D3EE", fondo: "#04242E" },
];
const MONO = "FiraCode";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ap = (f: number, d: number, dur = 12) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });

const escena = (f: number) => {
  const i = esc.findIndex((e) => f < e.hasta);
  return i < 0 ? esc.length - 1 : i;
};

// Trazo que se dibuja solo
const Trazo: React.FC<{ d: string; p: number; color?: string; w?: number; relleno?: string }> = ({ d, p, color, w = 5, relleno = "none" }) => {
  const frame = useCurrentFrame();
  const c = color ?? TONO[escena(frame) % TONO.length].linea;
  return (
  <path d={d} pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - p} stroke={c} strokeWidth={w} fill={p >= 1 ? (relleno === K.relleno ? `${c}22` : relleno) : "none"} strokeLinecap="round" strokeLinejoin="round" style={{ filter: `drop-shadow(0 0 8px ${c}88)` }} />
  );
};

// Frase clave en MAYÚSCULAS espaciadas + línea chica que la explica
const Frase: React.FC<{ texto: string; sub?: string; desde: number; color?: string }> = ({ texto, sub, desde, color = K.texto }) => {
  const frame = useCurrentFrame();
  const a = ap(frame, desde, 10);
  return (
    <div style={{ position: "absolute", top: 1270, left: 60, right: 60, textAlign: "center", opacity: a }}>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 56, letterSpacing: 10, color }}>{texto}</div>
      {sub ? <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, marginTop: 18 }}>{sub}</div> : null}
    </div>
  );
};




const ROJO = "#F43F5E";
const TELEFONO = "M640 560 Q640 520 680 520 L840 520 Q880 520 880 560 L880 1000 Q880 1040 840 1040 L680 1040 Q640 1040 640 1000 Z";

const Num: React.FC<{ n: number; c: string; d: number }> = ({ n, c, d }) => {
  const frame = useCurrentFrame();
  return <div style={{ position: "absolute", top: 230, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 150, color: c, opacity: ap(frame, d, 6), textShadow: `0 0 40px ${c}88` }}>{n}</div>;
};
const Palomita: React.FC<{ x: number; y: number; c: string; a: number; tam?: number }> = ({ x, y, c, a, tam = 60 }) => (
  <path d={`M${x - tam} ${y} L${x - tam / 4} ${y + tam * 0.7} L${x + tam} ${y - tam * 0.7}`} fill="none" stroke={c} strokeWidth={14} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - a} style={{ filter: `drop-shadow(0 0 10px ${c})` }} />
);

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    const on = Math.floor((frame - d0) / 6) % 2 === 0;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M300 860 Q300 640 540 640 Q780 640 780 860 Z" fill="none" stroke={K.tenue} strokeWidth={6} opacity={p(d0, 8)} />
          <path d="M300 860 Q300 640 540 640 L540 860 Z" fill={on ? K.azul : `${K.azul}22`} opacity={p(en(0, "torreta..."), 4)} style={{ filter: on ? `drop-shadow(0 0 40px ${K.azul})` : undefined }} />
          <path d="M540 640 Q780 640 780 860 L540 860 Z" fill={!on ? ROJO : `${ROJO}22`} opacity={p(en(0, "torreta..."), 4)} style={{ filter: !on ? `drop-shadow(0 0 40px ${ROJO})` : undefined }} />
          <rect x={260} y={860} width={560} height={60} rx={14} fill={K.relleno} stroke={K.tenue} strokeWidth={6} opacity={p(d0, 8)} />
        </svg>
        <Frase texto="¿LUCES AZULES Y ROJAS?" desde={en(0, "nervio.")} color={K.azul} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        {[[-14, 200, 560], [10, 470, 500], [-6, 330, 760], [16, 600, 720]].map(([r, x, y], k) => (
          <div key={k} style={{ position: "absolute", left: x, top: y + Math.sin(frame / 7 + k) * 14, width: 230, height: 300, borderRadius: 14, border: `5px solid ${TONO[1].linea}`, background: K.relleno, transform: `rotate(${r + Math.sin(frame / 9 + k) * 4}deg)`, opacity: p(d0 + k * 3, 6), padding: 24 }}>
            {[0, 1, 2, 3].map((j) => <div key={j} style={{ height: 10, borderRadius: 5, background: `${TONO[1].linea}66`, marginTop: 26, width: `${90 - j * 12}%` }} />)}
          </div>
        ))}
        <div style={{ position: "absolute", top: 560, right: 110, fontFamily: MONO, fontWeight: 700, fontSize: 220, color: K.rosa, opacity: p(en(1, "desorden."), 6), textShadow: `0 0 40px ${K.rosa}88` }}>?</div>
        <Frase texto="PAPELES EN DESORDEN" desde={en(1, "papeles")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 420, color: K.verde, opacity: p(en(2, "cuatro"), 6), transform: `scale(${1.3 - 0.3 * p(en(2, "cuatro"), 8)})`, textShadow: `0 0 50px ${K.verde}88` }}>4</div>
        <Frase texto="4 TIPS" sub="para circular tranquilo" desde={en(2, "tips")} color={K.verde} />
      </>
    );
  }
  if (i === 3) {
    const docs: [string, number][] = [["licencia", en(3, "licencia,")], ["tarjeta", en(3, "tarjeta")], ["seguro", en(3, "seguro,")]];
    return (
      <>
        <Num n={1} c={K.cian} d={d0} />
        {docs.map(([t, d], k) => (
          <div key={t} style={{ position: "absolute", top: 480 + k * 170, left: 220, right: 220, height: 140, borderRadius: 20, border: `5px solid ${K.cian}`, background: `${K.cian}12`, display: "flex", alignItems: "center", gap: 30, padding: "0 30px", fontFamily: MONO, fontSize: 44, color: K.cian, opacity: ap(frame, d, 6) }}>
            <div style={{ width: 70, height: 80, borderRadius: 10, border: `4px solid ${K.cian}` }} />
            {t}
            <svg width={80} height={80} style={{ marginLeft: "auto" }}><path d="M14 40 L32 58 L66 20" fill="none" stroke={K.verde} strokeWidth={9} strokeLinecap="round" opacity={ap(frame, en(3, "vigentes."), 6)} /></svg>
          </div>
        ))}
        <Frase texto="SIEMPRE VIGENTES" desde={en(3, "vigentes.")} color={K.cian} />
      </>
    );
  }
  if (i === 4) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M180 640 L360 640 L400 690 L560 690 L560 1000 L180 1000 Z" p={p(d0, 14)} color={K.azul} relleno={K.relleno} />
          <Trazo d={TELEFONO} p={p(en(4, "copia"), 12)} color={K.verde} relleno={K.relleno} />
          {[0, 1, 2].map((k) => <rect key={k} x={680} y={580 + k * 140} width={160} height={110} rx={10} fill="none" stroke={K.verde} strokeWidth={4} opacity={ap(frame, en(4, "digital") + k * 3, 6)} />)}
          <path d="M580 820 L620 820 M605 800 L625 820 L605 840" stroke={K.amarillo} strokeWidth={6} fill="none" opacity={p(en(4, "copia"), 6)} />
        </svg>
        <Frase texto="COPIA DIGITAL" sub="en tu teléfono" desde={en(4, "copia")} color={K.verde} />
      </>
    );
  }
  if (i === 5) {
    const lx = 300 + Math.sin((frame - d0) / 12) * 180;
    return (
      <>
        <Num n={2} c={K.amarillo} d={d0} />
        <div style={{ position: "absolute", top: 620, left: 200, right: 200, height: 220, borderRadius: 24, border: `7px solid ${K.amarillo}`, background: K.relleno, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 120, color: K.amarillo, letterSpacing: 6, opacity: p(en(5, "placa"), 8) }}>ABC-123</div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g opacity={p(en(5, "revisa"), 8)} transform={`translate(${lx} 0)`}>
            <circle cx={240} cy={700} r={110} fill={`${K.texto}10`} stroke={K.texto} strokeWidth={9} />
            <line x1={320} y1={780} x2={420} y2={880} stroke={K.texto} strokeWidth={18} strokeLinecap="round" />
          </g>
        </svg>
        <Frase texto="REVISA TU PLACA" sub="una vez al mes" desde={en(5, "placa")} color={K.amarillo} />
      </>
    );
  }
  if (i === 6) {
    return (
      <>
        <Num n={3} c={K.azul} d={d0} />
        <div style={{ position: "absolute", top: 500, left: 140, width: 360, height: 460, borderRadius: 26, border: `5px solid ${K.azul}`, background: `${K.azul}12`, padding: 30, fontFamily: MONO, opacity: p(en(6, "identificación"), 8) }}>
          <div style={{ width: 120, height: 120, borderRadius: 60, border: `5px solid ${K.azul}`, margin: "0 auto" }} />
          <div style={{ textAlign: "center", fontSize: 32, color: K.azul, marginTop: 30 }}>identificación</div>
          <div style={{ height: 10, borderRadius: 5, background: `${K.azul}66`, marginTop: 30 }} />
          <div style={{ height: 10, borderRadius: 5, background: `${K.azul}66`, marginTop: 20, width: "70%" }} />
        </div>
        <div style={{ position: "absolute", top: 540, left: 560, width: 380, height: 440, borderRadius: 16, border: `5px solid ${K.naranja}`, background: `${K.naranja}10`, padding: 30, fontFamily: MONO, opacity: p(en(6, "boleta"), 8) }}>
          <div style={{ fontSize: 34, color: K.naranja }}>boleta</div>
          {[0, 1, 2, 3].map((j) => <div key={j} style={{ height: 10, borderRadius: 5, background: `${K.naranja}66`, marginTop: 34, width: `${95 - j * 15}%` }} />)}
          <div style={{ fontSize: 28, color: K.naranja, marginTop: 40 }}>por escrito</div>
        </div>
        <Frase texto="IDENTIFICACIÓN Y BOLETA" desde={en(6, "identificación")} color={K.azul} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <Num n={4} c={K.verde} d={d0} />
        <div style={{ position: "absolute", top: 500, left: 130, width: 380, height: 460, borderRadius: 16, border: `5px solid ${K.verde}`, background: `${K.verde}10`, padding: 30, fontFamily: MONO, opacity: p(en(7, "canales"), 8) }}>
          <div style={{ fontSize: 34, color: K.verde }}>pago oficial</div>
          {[0, 1, 2].map((j) => <div key={j} style={{ height: 10, borderRadius: 5, background: `${K.verde}66`, marginTop: 34, width: `${90 - j * 15}%` }} />)}
        </div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Palomita x={320} y={880} c={K.verde} a={p(en(7, "oficiales."), 8)} tam={55} />
          <g opacity={p(en(7, "arreglos"), 8)}>
            {[0, 1, 2].map((k) => <rect key={k} x={600 + k * 18} y={600 + k * 22} width={280} height={150} rx={12} fill={K.relleno} stroke={K.rosa} strokeWidth={5} />)}
            <path d="M620 880 Q700 820 820 860 Q900 890 930 960" fill="none" stroke={K.rosa} strokeWidth={6} />
          </g>
          <path d="M580 560 L960 1000 M960 560 L580 1000" stroke={ROJO} strokeWidth={16} strokeLinecap="round" opacity={p(en(7, "calle:"), 6)} style={{ filter: `drop-shadow(0 0 12px ${ROJO})` }} />
        </svg>
        <Frase texto="SOLO CANALES OFICIALES" sub="cero arreglos en la calle" desde={en(7, "paga")} color={K.verde} />
      </>
    );
  }
  if (i === 8) {
    const n = [[540, 560], [330, 900], [750, 900]];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M540 560 L330 900 M540 560 L750 900" stroke={K.amarillo} strokeWidth={8} opacity={p(d0 + 4, 8)} />
          {n.map(([x, y], k) => <circle key={k} cx={x} cy={y} r={80} fill={K.relleno} stroke={K.amarillo} strokeWidth={8} opacity={p(d0 + k * 3, 6)} style={{ filter: `drop-shadow(0 0 12px ${K.amarillo})` }} />)}
        </svg>
        <Frase texto="COMPÁRTELO" sub="con un compañero" desde={d0} color={K.amarillo} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={110} />
      <div style={{ fontFamily: MONO, fontSize: 40, color: K.cian, border: `4px solid ${K.cian}`, borderRadius: 16, padding: "12px 30px", opacity: p(en(9, "Síguenos"), 8) }}>+ Seguir</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(9, "conductor"), 8) }}>consejos de conductor a conductor</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  torreta: K.azul, nervio: "#F43F5E", papeles: K.rosa, desorden: K.rosa, cuatro: K.verde, tips: K.verde, uno: K.cian, licencia: K.cian, tarjeta: K.cian, seguro: K.cian, vigentes: K.cian,
  copia: K.verde, digital: K.verde, dos: K.amarillo, placa: K.amarillo, infracciones: K.amarillo, tres: K.azul, identificación: K.azul, boleta: K.naranja, cero: "#F43F5E", arreglos: "#F43F5E", oficiales: K.verde, comparte: K.amarillo, síguenos: K.cian,
};
const limpia = (w: string) => w.toLowerCase().replace(/[^a-záéíóúñ]/g, "");

// Subtítulos para redes: 1–3 palabras, Inter negrita con contorno, palabras clave a color
const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const k = escena(frame);
  const l = LINEAS[k];
  const t = (frame - esc[k].voz) / 30;
  if (t < -0.1 || t > l.dur + 0.3) return null;
  const grupos: [number, number][] = [];
  let ini = 0;
  l.palabras.forEach((p, j) => {
    if (/[.!?,:]$/.test(p.w) || j - ini === 2 || j === l.palabras.length - 1) {
      grupos.push([ini, j]);
      ini = j + 1;
    }
  });
  const g = [...grupos].reverse().find(([a]) => t >= l.palabras[a].s - 0.05) ?? grupos[0];
  const inicio = esc[k].voz + Math.round(l.palabras[g[0]].s * 30);
  const s = 1.15 - 0.15 * ap(frame, inicio - 1, 5);
  return (
    <div style={{ position: "absolute", top: 1500, left: 40, right: 40, textAlign: "center", fontFamily: "Inter", fontWeight: 800, fontSize: 70, lineHeight: 1.1, transform: `scale(${s})`, WebkitTextStroke: "3px #000", paintOrder: "stroke fill", textShadow: "0 5px 0 #000, 0 10px 24px rgba(0,0,0,0.9)" }}>
      {l.palabras.slice(g[0], g[1] + 1).map((p, j) => (
        <span key={j} style={{ color: CLAVE[limpia(p.w)] ?? "#FFFFFF" }}>
          {p.w}{" "}
        </span>
      ))}
    </div>
  );
};

const Fondo: React.FC = () => {
  const frame = useCurrentFrame();
  const k = escena(frame);
  const a = ap(frame, esc[k].desde, 8);
  const prev = TONO[Math.max(0, k - 1) % TONO.length].fondo;
  return (
    <>
      <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 40%, ${prev} 0%, ${K.fondo} 75%)` }} />
      <AbsoluteFill style={{ opacity: a, background: `radial-gradient(ellipse at 50% 40%, ${TONO[k % TONO.length].fondo} 0%, ${K.fondo} 75%)` }} />
    </>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol: number }> = ({ en: e, src, vol }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

export const Multas1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`multas/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[3, 5, 6, 7].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(0, "torreta...")} src="error.wav" vol={0.08} />
    <Sfx en={en(7, "oficiales.")} src="ding.wav" vol={0.1} />
    <Sfx en={esc[9].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
