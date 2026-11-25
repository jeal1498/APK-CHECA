import React from "react";
import { AbsoluteFill, interpolateColors, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { s } from "./Referencia";
import { SubtitulosPalabra } from "./components/SubtitulosPalabra";
import type { BloquePalabras } from "./components/SubtitulosPalabra";
import { Logo } from "./components/Logo";

/**
 * Final de 5 s (estilo AB, guion 001): «¿Qué dato tomas en cuenta antes de aceptar?».
 * 1) el auto frena; 2) la barra de $100 se vuelve globo de comentario con «?»; 3) dentro regresan camino, reloj y bomba;
 * 4) la ✓ cae sobre el globo; 5) sello EA.; 6) todo regresa a la barra de $100 con el auto andando:
 * el último cuadro es igual al primero de EA-Movimiento-6s, así el video se repite sin que se note el corte.
 */
export const DURACION_FINAL = s(5);

const P = { dato: 10, cuenta: 35, aceptar: 62 };

const BLOQUES: BloquePalabras[] = [
  { de: 0, a: 1.55, lineas: [[["¿Qué", 0.05], ["dato", 0.33, true], ["tomas", 0.62]], [["en", 0.95], ["cuenta", 1.12]]] },
  { de: 1.55, a: 3.1, lineas: [[["antes", 1.6], ["de", 1.85]], [["aceptar?", 2.05, true]]] },
];

const C = EA.color;
const lerp = (a: number, b: number, x: number) => a + (b - a) * x;
const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round" } as const;
const SUELO = 1440;
const AUTO_X = 290;
const ESC = 1.25;
const BARRA = { x: 60, y: 960, w: 960, h: 100 };
const GLOBO = { x: 140, y: 905, w: 800, h: 230 };
const REGRESA = 108; // empieza la vuelta al cuadro inicial

// Velocidad: viene rápido (16), frena, se detiene y al final vuelve a andar a 7 (la del inicio).
const velocidad = (f: number) => 16 * (1 - progreso(f, 0, 22)) + 7 * progreso(f, REGRESA + 8, 26);
const DIST: number[] = [0];
for (let i = 1; i <= 200; i++) DIST[i] = DIST[i - 1] + velocidad(i);
const dist = (f: number) => DIST[Math.max(0, Math.min(200, Math.floor(f)))];

/** `distInicial` y `desfase` vienen de la escena anterior para que el camino, las llantas y el bote sigan sin salto. */
export const Final: React.FC<{ distInicial?: number; desfase?: number }> = ({ distInicial = 0, desfase = 0 }) => {
  const f = useCurrentFrame();
  const p = (inicio: number, dur = 12) => progreso(f, inicio, dur);

  const vuelve = p(REGRESA, 26);
  const globo = p(P.dato - 2, 22) * (1 - vuelve); // 0 = barra, 1 = globo
  const r = {
    x: lerp(BARRA.x, GLOBO.x, globo),
    y: lerp(BARRA.y, GLOBO.y, globo),
    w: lerp(BARRA.w, GLOBO.w, globo),
    h: lerp(BARRA.h, GLOBO.h, globo),
  };
  const tono = Math.min(1, globo * 4); // el color cambia rápido (sin gris largo); la forma cambia despacio
  const fondo = interpolateColors(tono, [0, 1], [C.negro, C.marfil]);
  const naranja = 0.3 * (1 - p(P.dato - 6, 14)); // el tramo de la gasolina se va
  const op100 = (1 - p(P.dato - 8, 8)) + p(REGRESA + 14, 10); // «$100» sale y regresa
  const cola = p(P.dato + 16, 8) * (1 - p(REGRESA - 4, 8));
  const contenido = 1 - p(REGRESA - 8, 10);
  const pulso = 1 + 0.07 * Math.sin(f / 4.5);
  const cae = p(P.aceptar - 6, 14);
  const bote = 2.5 * Math.sin((f + desfase) / 3.2) * (1 - p(0, 22) + p(REGRESA + 8, 20)) + 0;
  const giro = (distInicial + dist(f)) * 1.8;
  const zoom = 1 + 0.14 * (Math.sin(Math.PI * p(P.dato - 2, 14)) + Math.sin(Math.PI * p(P.aceptar - 2, 14)));
  const lineasVel = 1 - p(0, 14);

  const icono = (i: number) => p(P.cuenta - 12 + i * 7, 14);
  const dibujo = (x: number) => ({ pathLength: 1, strokeDasharray: 1, strokeDashoffset: 1 - x });

  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: C.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        <div style={{ transform: `scale(${zoom})` }}>[ video de Eduardo a cámara ]</div>
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: C.negro }} />

      {/* barra ↔ globo (un solo elemento que cambia de forma) */}
      <div style={{ position: "absolute", left: r.x, top: r.y, width: r.w, height: r.h, boxSizing: "border-box", background: fondo, border: `${Math.round(6 * tono)}px solid ${C.negro}`, borderRadius: lerp(10, 44, globo) }}>
        {naranja > 0.001 ? <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: `${naranja * 100}%`, background: C.naranja, borderRadius: "10px 0 0 10px" }} /> : null}
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 62, letterSpacing: -2, color: C.marfil, opacity: Math.min(1, op100) }}>$100</div>
      </div>

      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {/* cola del globo */}
        {cola > 0.001 ? <path d={`M300 ${GLOBO.y + GLOBO.h - 3} l${-70 * cola} ${62 * cola} l${30 * cola} ${-62 * cola}`} fill={C.marfil} stroke={C.negro} strokeWidth={6} strokeLinejoin="round" /> : null}

        {/* dentro del globo: camino, reloj, bomba y «?» */}
        <g opacity={contenido}>
          <g transform={`translate(215 ${GLOBO.y + 55}) scale(0.62)`}>
            <path d="M60 0 L0 190 M140 0 L200 190" {...trazo} strokeWidth={9} {...dibujo(icono(0))} />
            <path d="M100 10 V180" fill="none" stroke={C.naranja} strokeWidth={9} strokeDasharray="22 16" opacity={icono(0)} />
          </g>
          <g transform={`translate(400 ${GLOBO.y + 55}) scale(0.62)`}>
            <circle cx={95} cy={95} r={88} {...trazo} strokeWidth={9} {...dibujo(icono(1))} />
            <path d="M95 95 V35 M95 95 L135 118" fill="none" stroke={C.naranja} strokeWidth={10} strokeLinecap="round" opacity={icono(1)} />
          </g>
          <g transform={`translate(585 ${GLOBO.y + 55}) scale(0.62)`}>
            <path d="M40 190 V30 q0-24 24-24 h96 q24 0 24 24 V190 M16 190 H208" {...trazo} strokeWidth={9} {...dibujo(icono(2))} />
            <rect x={66} y={34} width={92} height={56} rx={8} {...trazo} strokeWidth={9} {...dibujo(icono(2))} />
          </g>
          <text x={850} y={GLOBO.y + 160} textAnchor="middle" fontFamily={EA.font.titulo} fontWeight={800} fontSize={150} fill={C.naranja} stroke={C.negro} strokeWidth={4} opacity={p(P.dato + 6, 10)} transform={`translate(850 ${GLOBO.y + 110}) scale(${pulso}) translate(-850 ${-(GLOBO.y + 110)})`}>
            ?
          </text>
          {/* ✓ que cae sobre el globo */}
          <g transform={`translate(${GLOBO.x + GLOBO.w - 70} ${lerp(GLOBO.y - 140, GLOBO.y, cae)}) scale(${1 + 0.18 * Math.sin(Math.PI * p(P.aceptar + 6, 8))})`} opacity={Math.min(1, cae * 4)}>
            <circle r={46} fill={C.naranja} stroke={C.negro} strokeWidth={6} />
            <path d="M-22 0 l16 16 l30 -32" fill="none" stroke={C.negro} strokeWidth={8} strokeLinecap="round" strokeLinejoin="round" />
          </g>
        </g>

        {/* camino */}
        <line x1={0} y1={SUELO} x2={1080} y2={SUELO} stroke={C.negro} strokeWidth={6} strokeLinecap="round" />
        <line x1={0} y1={SUELO + 44} x2={1080} y2={SUELO + 44} stroke={C.negro} strokeWidth={6} strokeDasharray="60 50" strokeDashoffset={(distInicial + dist(f)) % 110} />

        {/* auto que frena y, al final, vuelve a andar */}
        <g transform={`translate(${AUTO_X} ${SUELO + bote * 0.4}) scale(${ESC}) rotate(${2.5 * Math.sin(Math.PI * p(0, 18))} 200 0)`}>
          <g transform={`translate(0 ${bote})`}>
            <path d="M10 -70 L60 -70 L115 -130 L270 -130 L330 -70 L390 -70 Q400 -70 400 -60 L400 -44 L0 -44 L0 -60 Q0 -70 10 -70 Z" {...trazo} />
            <path d="M128 -118 L188 -118 L188 -78 L95 -78 Z M204 -118 L262 -118 L304 -78 L204 -78 Z" {...trazo} strokeWidth={5} />
          </g>
          {[90, 310].map((cx) => (
            <g key={cx} transform={`translate(${cx} -34) rotate(${giro})`}>
              <circle r={34} fill={C.marfil} stroke={C.negro} strokeWidth={6} />
              <line x1={-20} y1={0} x2={20} y2={0} stroke={C.naranja} strokeWidth={6} strokeLinecap="round" />
            </g>
          ))}
          {[0, 1, 2].map((i) => {
            const fase = (((f + desfase) * 9 + i * 40) % 120) / 120;
            return <line key={i} x1={-30 - 120 * fase} y1={-110 + i * 26} x2={-30 - 120 * fase - 60} y2={-110 + i * 26} stroke={C.negro} strokeWidth={5} strokeLinecap="round" opacity={lineasVel * (1 - fase)} />;
          })}
        </g>
      </svg>

      {/* sello EA. (a la izquierda del auto: la derecha la tapan los botones de TikTok, Reels y Shorts) */}
      <div style={{ opacity: 1 - p(REGRESA - 6, 10) }}>
        <Logo x={70} y={1250} tam={96} inicio={P.aceptar + 16} />
      </div>

      <SubtitulosPalabra y={800} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};
