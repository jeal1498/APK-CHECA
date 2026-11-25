import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import type { Bloque } from "./components/Subtitles";
import { SubtitulosCentro } from "./components/SubtitulosCentro";
import { LowerThird } from "./components/LowerThird";

/**
 * Muestra de 6 s (estilo AB) con movimiento continuo: los primeros 6 s del guion 001.
 * Siempre hay algo moviéndose: el camino avanza, las llantas giran y el auto va dejando dinero (gotas naranjas) en el camino.
 * La barra de $100 se pinta de naranja con cada gota (3 % por gota, tope 30 %: la gasolina del ejemplo 15 km ÷ 12 × $24 = $30).
 * Subtítulos al centro, sobre la línea de la cámara (y = 800).
 */
export const DURACION_MOVIMIENTO = s(6);

const P = { perdiendo: 60, aceptas: 105, viaje: 132, dejando: 165 };

const BLOQUES: Bloque[] = [
  B(0, 2.6, [t("¿Y si te dijera que estás")], [n("perdiendo"), t(" dinero")]),
  B(2.6, 4.6, [t("cada vez que aceptas")], [t("un "), n("viaje"), t("?")]),
  B(4.6, 6.3, [t("Puede que estés")], [n("dejando"), t(" dinero")]),
];

const C = EA.color;
const lerp = (a: number, b: number, p: number) => a + (b - a) * p;
const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round" } as const;

// Velocidad del camino (px por fotograma) y distancia recorrida hasta cada fotograma.
const velocidad = (f: number) => 7 + 9 * progreso(f, P.viaje - 6, 20);
const DIST: number[] = [0];
for (let i = 1; i <= 400; i++) DIST[i] = DIST[i - 1] + velocidad(i);
const dist = (f: number) => DIST[Math.max(0, Math.min(400, Math.floor(f)))];

// Gotas: cada vez más seguido (dejando dinero).
const GOTAS = [P.perdiendo - 4, 76, 92, 108, 122, 134, 144, 153, 161, 168];
const SUELO = 1440;
const AUTO_X = 290;
const ESC = 1.25;
const COLA = AUTO_X + 6 * ESC;

export const Movimiento: React.FC = () => {
  const f = useCurrentFrame();
  const p = (inicio: number, dur = 12) => progreso(f, inicio, dur);

  const entra = p(0, 20);
  const dibujaAuto = p(4, 24);
  const bote = 2.5 * Math.sin(f / 3.2);
  const giro = dist(f) * 1.8; // llantas
  const inclina = -2.5 * Math.sin(Math.PI * p(P.viaje - 6, 16));

  const caidas = GOTAS.map((k) => p(k + 8, 8)).reduce((a, x) => a + x, 0);
  const perdido = Math.min(0.3, 0.03 * caidas);

  const sello = p(P.aceptas - 4, 14);
  const paloma = p(P.aceptas + 6, 10);

  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      {/* Cámara */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: C.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: C.negro }} />
      <LowerThird nombre="Eduardo Alvarado" rol="Conductor" x={EA.zona.lados} y={220} inicio={6} fin={84} />

      {/* Barra del viaje: se pinta de naranja con cada gota */}
      <div style={{ position: "absolute", left: 60, top: 960, width: 960, height: 100, clipPath: `inset(0 ${(1 - entra) * 100}% 0 0)` }}>
        <div style={{ position: "absolute", inset: 0, background: C.negro, borderRadius: 10 }} />
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 960 * perdido, background: C.naranja, borderRadius: "10px 0 0 10px" }} />
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 62, letterSpacing: -2, color: C.marfil }}>$100</div>
      </div>

      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {/* camino que avanza */}
        <line x1={0} y1={SUELO} x2={1080 * entra} y2={SUELO} stroke={C.negro} strokeWidth={6} strokeLinecap="round" />
        <line x1={0} y1={SUELO + 44} x2={1080} y2={SUELO + 44} stroke={C.negro} strokeWidth={6} strokeDasharray="60 50" strokeDashoffset={dist(f) % 110} opacity={entra} />

        {/* gotas: caen de la cola y se quedan en el camino */}
        {GOTAS.map((k) => {
          const cae = p(k, 8);
          if (f < k) return null;
          const enSuelo = f >= k + 8;
          const x = enSuelo ? COLA - (dist(f) - dist(k + 8)) : COLA;
          if (x < -40) return null;
          const y = lerp(SUELO - 80, SUELO - 8, cae * cae);
          return enSuelo ? (
            <ellipse key={k} cx={x} cy={SUELO - 4} rx={lerp(10, 22, p(k + 8, 6))} ry={7} fill={C.naranja} />
          ) : (
            <path key={k} d="M0 -40 q34 50 34 78 a34 34 0 0 1-68 0 q0-28 34-78z" fill={C.naranja} transform={`translate(${x} ${y}) scale(0.4)`} />
          );
        })}

        {/* auto de una línea */}
        <g transform={`translate(${AUTO_X} ${SUELO + bote * 0.4}) scale(${ESC}) rotate(${inclina} 200 0)`}>
          <g transform={`translate(0 ${bote})`}>
            <path d="M10 -70 L60 -70 L115 -130 L270 -130 L330 -70 L390 -70 Q400 -70 400 -60 L400 -44 L0 -44 L0 -60 Q0 -70 10 -70 Z" {...trazo} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - dibujaAuto} />
            <path d="M128 -118 L188 -118 L188 -78 L95 -78 Z M204 -118 L262 -118 L304 -78 L204 -78 Z" {...trazo} strokeWidth={5} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - p(16, 16)} />
          </g>
          {[90, 310].map((cx) => (
            <g key={cx} transform={`translate(${cx} -34) rotate(${giro})`} opacity={dibujaAuto}>
              <circle r={34} fill={C.marfil} stroke={C.negro} strokeWidth={6} />
              <line x1={-20} y1={0} x2={20} y2={0} stroke={C.naranja} strokeWidth={6} strokeLinecap="round" />
            </g>
          ))}
          {/* líneas de velocidad */}
          {[0, 1, 2].map((i) => {
            const v = p(P.viaje - 6, 16);
            const fase = ((f * 9 + i * 40) % 120) / 120;
            return <line key={i} x1={-30 - 120 * fase} y1={-110 + i * 26} x2={-30 - 120 * fase - 60} y2={-110 + i * 26} stroke={C.negro} strokeWidth={5} strokeLinecap="round" opacity={v * (1 - fase)} />;
          })}
        </g>

        {/* ✓ de aceptar sobre el auto */}
        <g transform={`translate(${AUTO_X + 200 * ESC} ${1170 + bote}) scale(${0.6 + 0.4 * sello})`} opacity={sello}>
          <line x1={0} y1={40} x2={0} y2={92} stroke={C.negro} strokeWidth={5} strokeDasharray="8 8" />
          <circle r={44} fill={C.naranja} stroke={C.negro} strokeWidth={6} />
          <path d="M-20 0 l14 14 l28 -30" fill="none" stroke={C.negro} strokeWidth={8} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray={1} strokeDashoffset={1 - paloma} />
        </g>
      </svg>
      <div style={{ position: "absolute", left: AUTO_X + 200 * ESC + 70, top: 1152 + bote, fontFamily: EA.font.texto, fontWeight: 500, fontSize: 30, letterSpacing: 5, color: C.negro, opacity: p(P.aceptas + 4, 12) }}>ACEPTAR</div>

      <SubtitulosCentro y={800} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};
