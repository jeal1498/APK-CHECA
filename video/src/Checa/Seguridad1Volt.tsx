import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasSeguridad";

// Tema 9 «Inseguridad» (video de valor, Checa solo firma), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_SEGURIDAD_1VOLT = duracion;
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
const ESCUDO = "M540 560 L760 640 L760 820 Q760 1000 540 1100 Q320 1000 320 820 L320 640 Z";
const TELEFONO = "M250 520 Q250 480 290 480 L470 480 Q510 480 510 520 L510 1000 Q510 1040 470 1040 L290 1040 Q250 1040 250 1000 Z";

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
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M640 380 m-120 0 a120 120 0 1 0 240 0 a120 120 0 1 0 -240 0" p={p(d0, 14)} color={K.amarillo} relleno={K.relleno} />
          <circle cx={700} cy={350} r={110} fill="#05060F" />
          {[[160, 700, 140, 420], [330, 640, 120, 480], [480, 720, 160, 400], [670, 660, 130, 460], [830, 700, 120, 420]].map(([x, y, w, h], k) => (
            <Trazo key={k} d={`M${x} ${y + h} L${x} ${y} L${x + w} ${y} L${x + w} ${y + h}`} p={p(d0 + 4 + k * 2, 12)} color={TONO[0].linea} />
          ))}
          <line x1={80} y1={1120} x2={1000} y2={1120} stroke={TONO[0].linea} strokeWidth={5} opacity={p(d0, 10)} />
        </svg>
        <Frase texto="¿MIEDO A LA NOCHE?" desde={en(0, "noche")} color={K.amarillo} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={ESCUDO} p={p(d0, 16)} color={K.verde} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 660, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 260, color: K.verde, opacity: p(en(1, "cinco"), 6), textShadow: `0 0 40px ${K.verde}88` }}>5</div>
        <Frase texto="5 CONSEJOS" sub="guárdalos" desde={en(1, "guarda")} color={K.verde} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <Num n={1} c={K.cian} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(d0, 12)} relleno={K.relleno} />
          <path d="M380 700 C330 640 330 590 380 590 C430 590 430 640 380 700 Z" fill={K.cian} opacity={p(d0 + 8, 6)} />
          <path d="M520 700 Q650 520 790 700" fill="none" stroke={K.cian} strokeWidth={6} strokeDasharray="16 14" strokeDashoffset={-(frame - d0) * 2} opacity={p(en(2, "comparte"), 8)} />
          <circle cx={830} cy={760} r={90} fill={`${K.cian}22`} stroke={K.cian} strokeWidth={5} opacity={p(en(2, "alguien"), 8)} />
          <circle cx={830} cy={735} r={28} fill="none" stroke={K.cian} strokeWidth={5} opacity={p(en(2, "alguien"), 8)} />
          <path d="M780 800 Q830 760 880 800" fill="none" stroke={K.cian} strokeWidth={5} opacity={p(en(2, "alguien"), 8)} />
          <Palomita x={830} y={920} c={K.verde} a={p(en(2, "confianza,"), 10)} tam={45} />
        </svg>
        <Frase texto="COMPARTE TU UBICACIÓN" sub="con alguien de confianza" desde={en(2, "comparte")} color={K.cian} />
      </>
    );
  }
  if (i === 3) {
    const ok = p(en(3, "coincidan"), 10);
    return (
      <>
        <Num n={2} c={K.azul} d={d0} />
        <div style={{ position: "absolute", top: 480, left: 170, right: 170, height: 360, borderRadius: 34, border: `5px solid ${ok > 0.5 ? K.verde : K.azul}`, background: `${K.azul}14`, display: "flex", alignItems: "center", gap: 40, padding: "0 50px", opacity: p(d0, 8) }}>
          <div style={{ width: 170, height: 170, borderRadius: 85, border: `5px solid ${K.azul}`, display: "flex", alignItems: "center", justifyContent: "center", opacity: p(en(3, "foto"), 6) }}>
            <svg width={120} height={120}><circle cx={60} cy={42} r={26} fill="none" stroke={K.azul} strokeWidth={5} /><path d="M14 112 Q60 60 106 112" fill="none" stroke={K.azul} strokeWidth={5} /></svg>
          </div>
          <div style={{ fontFamily: MONO, opacity: p(en(3, "nombre"), 6) }}>
            <div style={{ fontSize: 30, color: K.tenue }}>usuario</div>
            <div style={{ fontSize: 52, fontWeight: 700, color: K.texto, marginTop: 8 }}>Ana P.</div>
          </div>
        </div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Palomita x={540} y={1000} c={K.verde} a={ok} />
        </svg>
        <Frase texto="FOTO Y NOMBRE" sub="antes de quitar el seguro" desde={en(3, "foto")} color={K.azul} />
      </>
    );
  }
  if (i === 4) {
    const toque = en(4, "cancela");
    return (
      <>
        <Num n={3} c={ROJO} d={d0} />
        <div style={{ position: "absolute", top: 560, left: 190, right: 190, height: 150, borderRadius: 30, border: `6px solid ${ROJO}`, background: frame >= toque ? `${ROJO}44` : `${ROJO}11`, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 56, color: ROJO, opacity: p(d0 + 4, 8), boxShadow: frame >= toque ? `0 0 34px ${ROJO}` : undefined }}>Cancelar viaje</div>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M300 920 L700 920 M620 840 L720 920 L620 1000" fill="none" stroke={K.amarillo} strokeWidth={14} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - p(en(4, "vete."), 8)} transform={`translate(${p(en(4, "vete."), 20) * 80} 0)`} />
        </svg>
        <Frase texto="CANCELA Y VETE" sub="tu vida vale más que una tarifa" desde={toque} color={ROJO} />
      </>
    );
  }
  if (i === 5) {
    return (
      <>
        <Num n={4} c={K.naranja} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={ESCUDO} p={p(d0, 16)} color={K.naranja} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 750, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 130, color: K.naranja, opacity: p(en(5, "emergencia"), 6) * (0.75 + 0.25 * Math.sin(frame / 4)), textShadow: `0 0 30px ${K.naranja}` }}>SOS</div>
        <Frase texto="BOTÓN DE EMERGENCIA" sub="ubícalo antes de necesitarlo" desde={en(5, "botón")} color={K.naranja} />
      </>
    );
  }
  if (i === 6) {
    return (
      <>
        <Num n={5} c={K.verde} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M150 640 L450 640 L450 960 L150 960 Z" p={p(en(6, "soporte,") - 6, 12)} color={K.verde} relleno={K.relleno} />
          <path d="M120 1000 L480 1000 M300 960 L300 1060" stroke={K.verde} strokeWidth={8} opacity={p(en(6, "soporte,"), 8)} />
          <Palomita x={300} y={1150} c={K.verde} a={p(en(6, "soporte,"), 10)} tam={45} />
          <g opacity={p(en(6, "jamás"), 8)}>
            <path d="M660 640 L880 640 L880 960 L660 960 Z" fill="none" stroke={K.rosa} strokeWidth={6} transform="rotate(-12 770 800)" />
            <path d="M620 1000 Q700 900 800 960 Q900 1000 920 1060" fill="none" stroke={K.rosa} strokeWidth={6} />
          </g>
          <path d="M640 620 L920 1080 M920 620 L640 1080" stroke={ROJO} strokeWidth={14} strokeLinecap="round" opacity={p(en(6, "mano."), 6)} style={{ filter: `drop-shadow(0 0 10px ${ROJO})` }} />
        </svg>
        <Frase texto="SIEMPRE EN EL SOPORTE" sub="nunca en la mano" desde={en(6, "soporte,")} color={K.verde} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 56, letterSpacing: 8, color: K.verde, opacity: p(d0, 8) }}>CUÍDATE MUCHO</div>
      <Logo tam={110} />
      <div style={{ fontFamily: MONO, fontSize: 40, color: K.cian, border: `4px solid ${K.cian}`, borderRadius: 16, padding: "12px 30px", opacity: p(en(7, "Síguenos"), 8) }}>+ Seguir</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(7, "conductor"), 8) }}>consejos de conductor a conductor</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  noche: K.amarillo, juego: K.amarillo, cinco: K.verde, consejos: K.verde, uno: K.cian, ubicación: K.cian, confianza: K.cian, dos: K.azul, foto: K.azul, nombre: K.azul,
  tres: "#F43F5E", cancela: "#F43F5E", vete: "#F43F5E", vida: "#F43F5E", cuatro: K.naranja, emergencia: K.naranja, soporte: K.verde, mano: "#F43F5E", cuídate: K.verde, síguenos: K.cian,
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

export const Seguridad1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`seguridad/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[2, 3, 4, 5, 6].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(4, "cancela")} src="error.wav" vol={0.08} />
    <Sfx en={esc[7].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
