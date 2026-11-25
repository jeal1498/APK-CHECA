import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";

/**
 * Mezclas de 6 s de las propuestas A (Línea) y B (Tablero).
 * AB: mundo A (marfil, dibujo de una línea) + datos de B (cifra grande que cuenta, barra que reparte).
 * BA: mundo B (negro con cuadrícula, barra) + dibujo de una línea y rótulos de A.
 * En las dos, la gota de gasolina cae de la bomba y llena el 30 % de la barra.
 * Cuenta: 15 km ÷ 12 km/l × $24 = $30 de $100 → te quedan $70.
 */
export const DURACION_MEZCLA = s(6);

const C = EA.color;
const lerp = (a: number, b: number, p: number) => a + (b - a) * p;
const sube = (p: number, d = 16): React.CSSProperties => ({ opacity: p, transform: `translateY(${(1 - p) * d}px)` });
const rotulo: React.CSSProperties = { position: "absolute", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, letterSpacing: 6, textTransform: "uppercase" };

const SUB: Bloque[] = [B(0, 3, [t("De cada cien,")], [n("treinta"), t(" son gasolina")]), B(3, 6, [t("Te quedan "), n("setenta")])];

type Tema = { fondo: string; tinta: string; camara: string; camaraTexto: string; linea: string; grid: boolean; tituloGrande: boolean };

const Mezcla: React.FC<{ tema: Tema }> = ({ tema }) => {
  const f = useCurrentFrame();
  const { tinta } = tema;
  const W = 960;
  const BX = 60;
  const BY = tema.tituloGrande ? 1200 : 1180;
  const BH = 90;
  const cien = Math.round(interpolate(f, [16, 38], [0, 100], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const dibuja = progreso(f, 14, 30);
  const manguera = progreso(f, 38, 16);
  const pista = progreso(f, 48, 18);
  const cae = progreso(f, 70, 18); // la gota cae a la barra
  const llena = progreso(f, 86, 18); // y se vuelve el 30 %
  const trazo = { fill: "none", stroke: tinta, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round", pathLength: 1, strokeDasharray: 1 } as const;

  // Bomba (coordenadas de pantalla). La gota sale de la curva de la manguera (452, PY + 162).
  const PY = tema.tituloGrande ? 980 : 900;
  const gx = lerp(452, BX + W * 0.15, cae);
  const gy = lerp(PY + 200, BY + BH / 2 - 30, cae);

  return (
    <AbsoluteFill
      style={{
        background: tema.fondo,
        backgroundImage: tema.grid ? "linear-gradient(rgba(255,255,255,0.05) 2px, transparent 2px), linear-gradient(90deg, rgba(255,255,255,0.05) 2px, transparent 2px)" : undefined,
        backgroundSize: "60px 60px",
      }}
    >
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: tema.camara, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 32, color: tema.camaraTexto }}>
        [ Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: tema.linea }} />

      {tema.tituloGrande ? (
        <div style={{ position: "absolute", left: 60, top: 840, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 104, lineHeight: 1, letterSpacing: -4, color: tinta, ...sube(progreso(f, 4, 14), 20) }}>
          DE CADA <span style={{ color: C.naranja }}>${cien}</span>
        </div>
      ) : (
        <div style={{ ...rotulo, fontSize: 30, left: 0, right: 0, top: 840, textAlign: "center", color: tinta, ...sube(progreso(f, 4, 14)) }}>
          De cada <span style={{ color: C.naranja }}>${cien}</span> del viaje
        </div>
      )}

      {/* bomba de una línea */}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        <g transform={`translate(${200 - 330} ${PY - 80})`}>
          <path d="M330 250 V110 q0-30 30-30 h120 q30 0 30 30 V250" {...trazo} strokeDashoffset={1 - dibuja} />
          <path d="M300 250 H540" {...trazo} strokeDashoffset={1 - dibuja} />
          <rect x={360} y={115} width={110} height={65} rx={8} {...trazo} strokeDashoffset={1 - progreso(f, 30, 14)} />
          <path d="M510 140 h30 q20 0 20 20 v60 q0 22 22 22 q22 0 22-22 V140 l-32-32" {...trazo} strokeDashoffset={1 - manguera} />
        </g>
        {/* gota: cae y se funde con la barra */}
        <path
          d="M0 -40 q34 50 34 78 a34 34 0 0 1-68 0 q0-28 34-78z"
          fill={C.naranja}
          transform={`translate(${gx} ${gy}) scale(${1 - 0.6 * llena})`}
          opacity={progreso(f, 60, 10) * (1 - progreso(f, 90, 8))}
        />
      </svg>
      <div style={{ position: "absolute", left: 640, top: PY + 40, fontFamily: EA.font.texto, fontSize: 30, lineHeight: 1.4, color: tinta, opacity: 0.7 * progreso(f, 112, 12) }}>
        15 km ÷ 12 km/l
        <br />× $24 = $30
      </div>

      {/* barra que reparte */}
      <div style={{ position: "absolute", left: BX, top: BY, width: W, height: BH, boxSizing: "border-box", border: `5px solid ${tinta}`, borderRadius: 8, transform: `scaleX(${pista})`, transformOrigin: "left center", opacity: pista }} />
      <div style={{ position: "absolute", left: BX, top: BY, width: W * 0.3, height: BH, background: C.naranja, borderRadius: 8, transform: `scaleY(${llena})`, transformOrigin: "bottom center", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 44, color: C.negro }}>
        <span style={{ opacity: progreso(f, 98, 10) }}>$30</span>
      </div>
      <div style={{ position: "absolute", left: BX + W * 0.3, width: W * 0.7, top: BY, height: BH, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 44, color: tinta, opacity: progreso(f, 108, 12) }}>$70</div>
      <div style={{ ...rotulo, fontSize: 26, left: BX, top: BY + BH + 14, color: C.naranja, opacity: progreso(f, 100, 12) }}>Gasolina</div>
      <div style={{ ...rotulo, fontSize: 26, left: BX + W * 0.3 + 20, top: BY + BH + 14, color: tinta, opacity: 0.75 * progreso(f, 110, 12) }}>Te queda</div>

      <Subtitles y={1390} bloques={SUB} color={tinta} />
    </AbsoluteFill>
  );
};

export const MezclaAB: React.FC = () => (
  <Mezcla tema={{ fondo: C.marfil, tinta: C.negro, camara: C.papel, camaraTexto: "rgba(21,21,21,0.45)", linea: C.negro, grid: false, tituloGrande: true }} />
);

export const MezclaBA: React.FC = () => (
  <Mezcla tema={{ fondo: C.negro, tinta: C.marfil, camara: "#2A2A2A", camaraTexto: "rgba(241,238,230,0.45)", linea: C.naranja, grid: true, tituloGrande: false }} />
);
