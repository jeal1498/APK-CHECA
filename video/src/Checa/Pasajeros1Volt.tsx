import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasPasajeros";

// Tema 11 «Pasajeros problemáticos, cancelaciones y esperas» (video de valor), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_PASAJEROS_1VOLT = duracion;
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
const PIN = (x: number, y: number, r = 1) => `M${x} ${y} C${x - 90 * r} ${y - 120 * r} ${x - 90 * r} ${y - 240 * r} ${x} ${y - 240 * r} C${x + 90 * r} ${y - 240 * r} ${x + 90 * r} ${y - 120 * r} ${x} ${y} Z`;
const RELOJ_ARENA = "M400 520 L680 520 L560 780 L680 1040 L400 1040 L520 780 Z";

const Num: React.FC<{ n: number; c: string; d: number }> = ({ n, c, d }) => {
  const frame = useCurrentFrame();
  return <div style={{ position: "absolute", top: 230, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 150, color: c, opacity: ap(frame, d, 6), textShadow: `0 0 40px ${c}88` }}>{n}</div>;
};
const Palomita: React.FC<{ x: number; y: number; c: string; a: number; tam?: number }> = ({ x, y, c, a, tam = 60 }) => (
  <path d={`M${x - tam} ${y} L${x - tam / 4} ${y + tam * 0.7} L${x + tam} ${y - tam * 0.7}`} fill="none" stroke={c} strokeWidth={14} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - a} style={{ filter: `drop-shadow(0 0 10px ${c})` }} />
);
const Tache: React.FC<{ x: number; y: number; r: number; a: number }> = ({ x, y, r, a }) => (
  <path d={`M${x - r} ${y - r} L${x + r} ${y + r} M${x + r} ${y - r} L${x - r} ${y + r}`} stroke={ROJO} strokeWidth={16} strokeLinecap="round" opacity={a} style={{ filter: `drop-shadow(0 0 12px ${ROJO})` }} />
);

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={PIN(540, 1000)} p={p(d0, 14)} color={K.naranja} relleno={K.relleno} />
          <circle cx={540} cy={850} r={40} fill={K.naranja} opacity={p(d0 + 6, 6)} />
          <Tache x={540} y={850} r={170} a={p(en(0, "cancela."), 6)} />
        </svg>
        <Frase texto="¿CANCELACIÓN EN LA CARA?" desde={en(0, "cancela.")} color={ROJO} />
      </>
    );
  }
  if (i === 1) {
    const ang = ((frame - d0) / 30) * 120;
    const gas = interpolate(frame, [d0, esc[1].hasta], [40, -50], clamp);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M380 700 m-200 0 a200 200 0 1 0 400 0 a200 200 0 1 0 -400 0" p={p(d0, 12)} relleno={K.relleno} />
          <line x1={380} y1={700} x2={380 + Math.sin((ang * Math.PI) / 180) * 160} y2={700 - Math.cos((ang * Math.PI) / 180) * 160} stroke={K.amarillo} strokeWidth={10} strokeLinecap="round" />
          <path d="M640 900 A160 160 0 0 1 960 900" fill="none" stroke={K.naranja} strokeWidth={8} opacity={p(d0 + 6, 8)} />
          <line x1={800} y1={900} x2={800 + Math.sin((gas * Math.PI) / 180) * 140} y2={900 - Math.cos((gas * Math.PI) / 180) * 140} stroke={K.naranja} strokeWidth={10} strokeLinecap="round" opacity={p(d0 + 6, 8)} />
        </svg>
        <div style={{ position: "absolute", top: 940, left: 180, width: 400, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 60, color: K.amarillo, opacity: p(en(1, "diez"), 6) }}>10 min</div>
        <Frase texto="TE DEJA PLANTADO" desde={en(1, "plantado")} color={K.amarillo} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 420, color: K.verde, opacity: p(en(2, "cuatro"), 6), transform: `scale(${1.3 - 0.3 * p(en(2, "cuatro"), 8)})`, textShadow: `0 0 50px ${K.verde}88` }}>4</div>
        <Frase texto="4 CONSEJOS" sub="de volada" desde={en(2, "consejos")} color={K.verde} />
      </>
    );
  }
  if (i === 3) {
    const c = (frame - d0) % 60 / 60;
    return (
      <>
        <Num n={1} c={K.azul} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={RELOJ_ARENA} p={p(d0, 14)} color={K.azul} relleno={K.relleno} />
          <path d={`M${440 + 80 * c} 560 L${640 - 80 * c} 560 L540 760 Z`} fill={K.amarillo} opacity={0.6} />
          <path d={`M540 1020 L${540 - 110 * c} 1020 L540 ${1020 - 150 * c} L${540 + 110 * c} 1020 Z`} fill={K.amarillo} opacity={0.6} />
        </svg>
        <Frase texto="REGLAS DE ESPERA" sub="las de tu app" desde={en(3, "reglas")} color={K.azul} />
      </>
    );
  }
  if (i === 4) {
    return (
      <>
        <div style={{ position: "absolute", top: 600, left: 190, right: 190, height: 150, borderRadius: 30, border: `6px solid ${K.verde}`, background: `${K.verde}22`, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 56, color: K.verde, opacity: p(d0, 8), boxShadow: `0 0 30px ${K.verde}66` }}>Cancelar viaje</div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Palomita x={540} y={900} c={K.verde} a={p(en(4, "culpa"), 10)} />
        </svg>
        <Frase texto="CANCELA SIN CULPA" sub="y checa si te toca cobro" desde={en(4, "cancela")} color={K.verde} />
      </>
    );
  }
  if (i === 5) {
    const t0 = en(5, "captura");
    const y = interpolate(frame, [t0, t0 + 14], [380, 700], { ...clamp, easing: Easing.in(Easing.quad) });
    return (
      <>
        <Num n={2} c={K.cian} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <rect x={470} y={y} width={140} height={240} rx={18} fill="#05060F" stroke={K.cian} strokeWidth={5} opacity={frame >= t0 ? 1 : 0} />
        </svg>
        <div style={{ position: "absolute", top: 900, left: 240, right: 240, height: 130, borderRadius: 26, border: `5px solid ${K.cian}`, background: frame >= en(5, "reporta.") ? `${K.cian}33` : "transparent", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 50, color: K.cian, opacity: p(d0 + 4, 8) }}>Reportar</div>
        <Frase texto="CAPTURA Y REPORTA" desde={en(5, "captura")} color={K.cian} />
      </>
    );
  }
  if (i === 6) {
    const ok = p(en(6, "destino"), 10);
    return (
      <>
        <Num n={3} c={K.naranja} d={d0} />
        <div style={{ position: "absolute", top: 480, left: 170, right: 170, height: 360, borderRadius: 34, border: `5px solid ${ok > 0.5 ? K.verde : K.naranja}`, background: `${K.naranja}10`, padding: "40px 50px", fontFamily: MONO, opacity: p(d0, 8) }}>
          <div style={{ fontSize: 30, color: K.tenue }}>usuario</div>
          <div style={{ fontSize: 56, fontWeight: 700, color: K.texto, marginTop: 6, opacity: p(en(6, "nombre"), 6) }}>Ana P.</div>
          <div style={{ fontSize: 30, color: K.tenue, marginTop: 30 }}>destino</div>
          <div style={{ fontSize: 44, color: K.naranja, marginTop: 6, opacity: p(en(6, "destino"), 6) }}>Av. Central 120</div>
        </div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Palomita x={540} y={1000} c={K.verde} a={ok} />
        </svg>
        <Frase texto="NOMBRE Y DESTINO" sub="antes de quitar seguros" desde={en(6, "nombre")} color={K.naranja} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <Num n={4} c={K.rosa} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M280 520 L800 520 Q840 520 840 560 L840 880 Q840 920 800 920 L460 920 L360 1020 L380 920 L280 920 Q240 920 240 880 L240 560 Q240 520 280 520 Z" p={p(d0, 14)} color={K.rosa} relleno={K.relleno} />
          <text x={540} y={780} textAnchor="middle" fontFamily={MONO} fontWeight={700} fontSize={130} fill={K.rosa} opacity={p(d0 + 8, 6)}>#!@</text>
          <Tache x={540} y={720} r={260} a={p(en(7, "alegues."), 6)} />
        </svg>
        <Frase texto="NI LE ALEGUES" desde={en(7, "alegues.")} color={K.rosa} />
      </>
    );
  }
  if (i === 8) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={PIN(540, 1040)} p={p(d0, 14)} color={K.verde} relleno={K.relleno} />
          <Palomita x={540} y={880} c={K.verde} a={p(en(8, "seguro"), 10)} tam={50} />
        </svg>
        <Frase texto="LUGAR SEGURO" sub="y cancela el viaje" desde={en(8, "lugar")} color={K.verde} />
      </>
    );
  }
  if (i === 9) {
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
      <div style={{ fontFamily: MONO, fontSize: 40, color: K.cian, border: `4px solid ${K.cian}`, borderRadius: 16, padding: "12px 30px", opacity: p(en(10, "Síguenos"), 8) }}>+ Seguir</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(10, "conductor"), 8) }}>consejos de conductor a conductor</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  cancela: "#F43F5E", plantado: K.amarillo, diez: K.amarillo, cuatro: K.verde, consejos: K.verde, uno: K.azul, reglas: K.azul, espera: K.azul, culpa: K.verde, cobro: K.verde,
  dos: K.cian, captura: K.cian, reporta: K.cian, tres: K.naranja, nombre: K.naranja, destino: K.naranja, pesado: K.rosa, alegues: K.rosa, seguro: K.verde, comparte: K.amarillo, síguenos: K.cian,
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

export const Pasajeros1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`pasajeros/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[3, 5, 6, 7].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(0, "cancela.")} src="error.wav" vol={0.08} />
    <Sfx en={en(4, "culpa")} src="ding.wav" vol={0.1} />
    <Sfx en={esc[10].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
