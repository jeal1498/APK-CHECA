import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";

/**
 * Muestras de 6 s de las 3 propuestas de estilo de PERSONAL (Identidad/propuestas-estilo/).
 * Mismo ejemplo: oferta de $100, 15 km → gasolina 15 ÷ 12 km/l × $24 = $30 → te quedan $70.
 */
export const DURACION_ESTILO = s(6);

const C = EA.color;
const lerp = (a: number, b: number, p: number) => a + (b - a) * p;
const sube = (p: number, d = 16): React.CSSProperties => ({ opacity: p, transform: `translateY(${(1 - p) * d}px)` });

const Camara: React.FC<{ fondo: string; texto: string; linea: string }> = ({ fondo, texto, linea }) => (
  <>
    <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: fondo, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 32, color: texto }}>
      [ Eduardo a cámara ]
    </div>
    <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: linea }} />
  </>
);

const rotulo: React.CSSProperties = { position: "absolute", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 30, letterSpacing: 6, textTransform: "uppercase" };

/* ───────── A · Línea ───────── */
const SUB_A: Bloque[] = [B(0, 3, [t("Antes de aceptar,")], [t("mira la "), n("gasolina")]), B(3, 6, [t("De cada $100")], [t("se van "), n("$30")])];

export const EstiloA: React.FC = () => {
  const f = useCurrentFrame();
  const dibuja = progreso(f, 16, 34);
  const manguera = progreso(f, 44, 18);
  const gota = progreso(f, 92, 16);
  const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round", pathLength: 1, strokeDasharray: 1 } as const;
  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      <Camara fondo={C.papel} texto="rgba(21,21,21,0.45)" linea={C.negro} />
      <div style={{ ...rotulo, left: 0, right: 0, top: 850, textAlign: "center", color: C.negro, ...sube(progreso(f, 6, 14)) }}>De cada $100 del viaje</div>
      <svg style={{ position: "absolute", left: 0, top: 900 }} width={1080} height={380}>
        <path d="M330 340 V110 q0-30 30-30 h170 q30 0 30 30 V340" {...trazo} strokeDashoffset={1 - dibuja} />
        <path d="M300 340 H590" {...trazo} strokeDashoffset={1 - dibuja} />
        <rect x={370} y={120} width={150} height={90} rx={8} {...trazo} strokeDashoffset={1 - progreso(f, 34, 16)} />
        <path d="M560 160 h40 q24 0 24 24 v110 q0 26 26 26 q26 0 26-26 V150 l-40-40" {...trazo} strokeDashoffset={1 - manguera} />
        <g transform={`translate(${lerp(640, 760, gota)} ${lerp(-60, 0, gota)})`} opacity={gota}>
          <path d="M0 150 q40 60 40 95 a40 40 0 0 1-80 0 q0-35 40-95z" fill={C.naranja} transform={`translate(0 0)`} />
        </g>
        <text x={830} y={262} fontFamily={EA.font.titulo} fontWeight={800} fontSize={72} fill={C.negro} opacity={progreso(f, 104, 12)}>
          −$30
        </text>
      </svg>
      <div style={{ ...rotulo, left: 0, right: 0, top: 1290, textAlign: "center", color: C.naranja, ...sube(progreso(f, 118, 12)) }}>Gasolina · 15 km</div>
      <Subtitles y={1390} bloques={SUB_A} />
    </AbsoluteFill>
  );
};

/* ───────── B · Tablero ───────── */
const SUB_B: Bloque[] = [B(0, 3, [t("De cada cien,")], [n("treinta"), t(" son gasolina")]), B(3, 6, [t("Te quedan "), n("setenta")])];

export const EstiloB: React.FC = () => {
  const f = useCurrentFrame();
  const cien = Math.round(interpolate(f, [18, 40], [0, 100], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const pista = progreso(f, 40, 20);
  const gas = progreso(f, 66, 20);
  const W = 960;
  return (
    <AbsoluteFill
      style={{
        background: C.negro,
        backgroundImage: "linear-gradient(rgba(255,255,255,0.05) 2px, transparent 2px), linear-gradient(90deg, rgba(255,255,255,0.05) 2px, transparent 2px)",
        backgroundSize: "60px 60px",
      }}
    >
      <Camara fondo="#2A2A2A" texto="rgba(241,238,230,0.45)" linea={C.naranja} />
      <div style={{ position: "absolute", left: 60, top: 860, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 120, lineHeight: 1, letterSpacing: -4, color: C.marfil, ...sube(progreso(f, 6, 14), 20) }}>
        DE CADA <span style={{ color: C.naranja }}>${cien}</span>
      </div>
      {/* pista */}
      <div style={{ position: "absolute", left: 60, top: 1060, width: W, height: 110, boxSizing: "border-box", border: `5px solid ${C.marfil}`, borderRadius: 8, transform: `scaleX(${pista})`, transformOrigin: "left center", opacity: pista }} />
      {/* gasolina 30 % */}
      <div style={{ position: "absolute", left: 60, top: 1060, width: W * 0.3 * gas, height: 110, background: C.naranja, borderRadius: 8, display: "flex", alignItems: "center", justifyContent: "center", overflow: "hidden", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 48, color: C.negro }}>
        <span style={{ opacity: progreso(f, 80, 10) }}>$30</span>
      </div>
      <div style={{ position: "absolute", left: 60 + W * 0.3, width: W * 0.7, top: 1060, height: 110, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 48, color: C.marfil, opacity: progreso(f, 96, 12) }}>$70</div>
      <div style={{ ...rotulo, fontSize: 28, left: 60, top: 1190, color: C.marfil, opacity: 0.75 * progreso(f, 84, 12) }}>Gasolina</div>
      <div style={{ ...rotulo, fontSize: 28, left: 60 + W * 0.3 + 20, top: 1190, color: C.marfil, opacity: 0.75 * progreso(f, 100, 12) }}>Te queda</div>
      <div style={{ position: "absolute", left: 60, top: 1250, fontFamily: EA.font.texto, fontSize: 30, color: C.marfil, opacity: 0.6 * progreso(f, 116, 12) }}>15 km ÷ 12 km/l × $24 = $30</div>
      <Subtitles y={1390} bloques={SUB_B} color={C.marfil} />
    </AbsoluteFill>
  );
};

/* ───────── C · Personaje ───────── */
const SUB_C: Bloque[] = [B(0, 3, [t("Parecía buen viaje,")]), B(3, 6, [t("hasta la "), n("cuenta")])];

export const EstiloC: React.FC = () => {
  const f = useCurrentFrame();
  const llega = progreso(f, 28, 20);
  const parpadeo = 1 - 0.85 * Math.sin(Math.PI * Math.min(1, Math.max(0, (f - 62) / 8)));
  const sorpresa = progreso(f, 124, 12);
  const tarjeta = progreso(f, 52, 18);
  const sale = progreso(f, 100, 10);
  const queda = progreso(f, 112, 12);
  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      <Camara fondo={C.papel} texto="rgba(21,21,21,0.45)" linea={C.negro} />
      <div style={{ ...rotulo, fontSize: 26, letterSpacing: 3, left: 60, top: 840, color: "rgba(21,21,21,0.65)", ...sube(progreso(f, 4, 12)) }}>// viaje · 11:40 p. m.</div>
      <div style={{ position: "absolute", left: 60, top: 882, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 72, lineHeight: 1.05, letterSpacing: -2, color: C.negro, ...sube(progreso(f, 12, 16)) }}>
        Parecía
        <br />
        buen viaje…
      </div>
      {/* personaje */}
      <svg style={{ position: "absolute", left: 90, top: 1040, opacity: llega, transform: `translateY(${(1 - llega) * 40}px)` }} width={400} height={300}>
        <rect x={40} y={20} width={230} height={190} rx={26} fill={C.negro} />
        <rect x={95} y={80 + 23 * (1 - parpadeo)} width={34} height={46 * parpadeo} rx={8} fill={C.marfil} />
        <rect x={180} y={80 + 23 * (1 - parpadeo)} width={34} height={46 * parpadeo} rx={8} fill={C.marfil} />
        <rect x={lerp(128, 140, sorpresa)} y={lerp(150, 140, sorpresa)} width={lerp(56, 32, sorpresa)} height={lerp(10, 26, sorpresa)} rx={lerp(5, 13, sorpresa)} fill={C.naranja} />
        <rect x={118} y={210} width={24} height={60} fill={C.negro} />
        <rect x={170} y={210} width={24} height={60} fill={C.negro} />
        <circle cx={292} cy={40} r={16} fill={C.naranja} />
      </svg>
      {/* tarjeta */}
      <div style={{ position: "absolute", left: 560, top: 1010, width: 420, height: 270, boxSizing: "border-box", background: C.blanco, border: `5px solid ${C.negro}`, borderRadius: 14, ...sube(tarjeta, 30) }}>
        <div style={{ position: "absolute", left: 30, top: 26, fontFamily: EA.font.texto, fontWeight: 500, fontSize: 24, letterSpacing: 3, opacity: 0.6 * (1 - sale) }}>OFERTA</div>
        <div style={{ position: "absolute", left: 30, top: 26, fontFamily: EA.font.texto, fontWeight: 500, fontSize: 24, letterSpacing: 3, opacity: 0.6 * queda }}>TE QUEDA</div>
        <div style={{ position: "absolute", left: 30, top: 60, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 92, letterSpacing: -3, opacity: 1 - sale }}>$100</div>
        <div style={{ position: "absolute", left: 30, top: 60, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 92, letterSpacing: -3, ...sube(queda, 12) }}>$70</div>
        <div style={{ position: "absolute", left: 30, top: 178, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 40, color: C.naranja, ...sube(progreso(f, 126, 12), 10) }}>gasolina −$30</div>
      </div>
      <Subtitles y={1390} bloques={SUB_C} />
    </AbsoluteFill>
  );
};
