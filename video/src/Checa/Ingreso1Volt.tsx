import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasIngreso";

// Tema 13 «Ingreso inestable, sin prestaciones ni seguro» (video de valor), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_INGRESO_1VOLT = duracion;
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
const ALCANCIA = "M300 860 Q300 640 540 640 Q780 640 780 860 Q780 1040 640 1070 L640 1140 L560 1140 L560 1080 L520 1080 L520 1140 L440 1140 L440 1070 Q300 1040 300 860 Z M480 680 L600 680";
const ESCUDO = "M540 520 L760 600 L760 780 Q760 960 540 1060 Q320 960 320 780 L320 600 Z";

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
    const alturas = [520, 160, 120, 190];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <line x1={140} y1={1100} x2={940} y2={1100} stroke={K.tenue} strokeWidth={5} />
          {alturas.map((h, k) => {
            const d = k === 0 ? en(0, "bien...") - 4 : en(0, "tres") + (k - 1) * 4;
            const a = ap(frame, d, 10);
            const c = k === 0 ? K.verde : ROJO;
            return <rect key={k} x={180 + k * 190} y={1100 - h * a} width={140} height={h * a} rx={10} fill={`${c}44`} stroke={c} strokeWidth={6} style={{ filter: `drop-shadow(0 0 10px ${c})` }} />;
          })}
        </svg>
        <Frase texto="¿VIVIR AL DÍA?" desde={en(0, "mal.")} color={ROJO} />
      </>
    );
  }
  if (i === 1) {
    const parpadeo = Math.floor((frame - d0) / 8) % 2 === 0 ? 1 : 0.45;
    const nivel = 0.4 + 0.5 * p(d0, 30);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <rect x={310} y={1000 - 380 * nivel} width={40} height={380 * nivel} fill={ROJO} opacity={0.8} />
          <Trazo d="M300 560 Q300 520 330 520 Q360 520 360 560 L360 960 Q400 990 400 1040 Q400 1100 330 1100 Q260 1100 260 1040 Q260 990 300 960 Z" p={p(d0, 12)} color={K.rosa} />
        </svg>
        <div style={{ position: "absolute", top: 680, left: 480, fontFamily: MONO, fontWeight: 700, fontSize: 200, color: ROJO, opacity: p(en(1, "nadie"), 6) * parpadeo, textShadow: `0 0 40px ${ROJO}` }}>$0</div>
        <Frase texto="SI TE ENFERMAS" sub="nadie te paga el día" desde={en(1, "enfermas,")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 420, color: K.verde, opacity: p(en(2, "cuatro"), 6), transform: `scale(${1.3 - 0.3 * p(en(2, "cuatro"), 8)})`, textShadow: `0 0 50px ${K.verde}88` }}>4</div>
        <Frase texto="4 TIPS" sub="para no vivir al día" desde={en(2, "tips")} color={K.verde} />
      </>
    );
  }
  if (i === 3) {
    const t0 = en(3, "guarda");
    const y = interpolate(frame, [t0, t0 + 16], [420, 680], { ...clamp, easing: Easing.in(Easing.quad) });
    return (
      <>
        <Num n={1} c={K.amarillo} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={ALCANCIA} p={p(d0, 14)} color={K.verde} relleno={K.relleno} />
          <circle cx={540} cy={y} r={36} fill={K.relleno} stroke={K.amarillo} strokeWidth={7} opacity={frame >= t0 && frame < t0 + 17 ? 1 : 0} />
        </svg>
        <div style={{ position: "absolute", top: 820, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 110, color: K.amarillo, opacity: p(en(3, "diez"), 6), textShadow: `0 0 30px ${K.amarillo}88` }}>10%</div>
        <Frase texto="GUARDA EL 10%" sub="sin tocarlo" desde={en(3, "diez")} color={K.amarillo} />
      </>
    );
  }
  if (i === 4) {
    const nivel = p(d0, 60);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <clipPath id="alc"><path d={ALCANCIA} /></clipPath>
          <rect x={300} y={1140 - 500 * nivel} width={480} height={500 * nivel} fill={K.amarillo} opacity={0.35} clipPath="url(#alc)" />
          <Trazo d={ALCANCIA} p={1} color={K.verde} />
          <g transform="translate(560 300) scale(0.45)" opacity={p(en(4, "fondo"), 8)}>
            <path d={ESCUDO} fill={`${K.cian}33`} stroke={K.cian} strokeWidth={14} />
          </g>
        </svg>
        <Frase texto="TU FONDO" sub="para la racha mala" desde={en(4, "fondo")} color={K.verde} />
      </>
    );
  }
  if (i === 5) {
    const sobres: [string, string, number][] = [
      ["gasolina", K.naranja, en(5, "gasolina,")],
      ["cuota", K.azul, en(5, "cuota")],
      ["taller", K.rosa, en(5, "mantenimiento.")],
    ];
    return (
      <>
        <Num n={2} c={K.naranja} d={d0} />
        {sobres.map(([t, c, d], k) => (
          <div key={t} style={{ position: "absolute", top: 520, left: 110 + k * 300, width: 260, opacity: ap(frame, d, 8), transform: `translateY(${(1 - ap(frame, d, 8)) * 40}px)` }}>
            <svg width={260} height={190}>
              <rect x={6} y={6} width={248} height={178} rx={14} fill={K.relleno} stroke={c} strokeWidth={7} />
              <path d="M6 20 L130 110 L254 20" fill="none" stroke={c} strokeWidth={7} />
            </svg>
            <div style={{ textAlign: "center", fontFamily: MONO, fontSize: 38, color: c, marginTop: 14 }}>{t}</div>
          </div>
        ))}
        <Frase texto="SEPARA EN SOBRES" desde={en(5, "sobres")} color={K.naranja} />
      </>
    );
  }
  if (i === 6) {
    return (
      <>
        <Num n={3} c={K.verde} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M260 470 L820 470 L820 1100 L260 1100 Z M320 470 L320 1100" p={p(d0, 14)} color={TONO[6].linea} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 560, left: 360, right: 280, fontFamily: MONO }}>
          <div style={{ position: "relative", fontSize: 46, color: K.tenue, opacity: p(en(6, "ingresos"), 6) }}>
            cobrado $1,600
            <div style={{ position: "absolute", left: -6, top: "50%", height: 6, width: `${p(en(6, "cobras."), 6) * 105}%`, background: ROJO }} />
          </div>
          <div style={{ fontSize: 76, fontWeight: 700, color: K.verde, marginTop: 70, opacity: p(en(6, "netas,"), 6), textShadow: `0 0 26px ${K.verde}88` }}>NETOS</div>
          <div style={{ fontSize: 40, color: K.verde, marginTop: 10, opacity: p(en(6, "netas,"), 6) }}>lo que te queda</div>
        </div>
        <Frase texto="MIDE EN NETOS" sub="no en lo que cobras" desde={en(6, "netas,")} color={K.verde} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <Num n={4} c={K.cian} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={ESCUDO} p={p(d0, 16)} color={K.cian} relleno={K.relleno} />
          <Palomita x={540} y={780} c={K.verde} a={p(en(7, "seguro"), 10)} tam={80} />
        </svg>
        <Frase texto="AHORRO O SEGURO" sub="para independientes" desde={en(7, "ahorro")} color={K.cian} />
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
  bien: K.verde, mal: "#F43F5E", enfermas: K.rosa, nadie: "#F43F5E", cuatro: K.verde, tips: K.verde, uno: K.amarillo, diez: K.amarillo, ciento: K.amarillo, fondo: K.verde, racha: "#F43F5E",
  dos: K.naranja, sobres: K.naranja, gasolina: K.naranja, cuota: K.azul, mantenimiento: K.rosa, tres: K.verde, netas: K.verde, cobras: "#F43F5E", ahorro: K.cian, seguro: K.cian, comparte: K.amarillo, síguenos: K.cian,
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

export const Ingreso1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`ingreso/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[3, 5, 6, 7].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(1, "nadie")} src="error.wav" vol={0.08} />
    <Sfx en={en(3, "guarda")} src="ding.wav" vol={0.1} />
    <Sfx en={esc[9].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
