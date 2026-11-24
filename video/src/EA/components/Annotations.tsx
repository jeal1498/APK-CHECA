import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso } from "../theme";

const trazo = (p: number): React.CSSProperties => ({ strokeDasharray: 1, strokeDashoffset: 1 - p });

/** Flecha que se dibuja de (x1,y1) a (x2,y2) con una curva suave. */
export const Arrow: React.FC<{
  x1: number; y1: number; x2: number; y2: number;
  curva?: number; inicio: number; dur?: number; color?: string; grosor?: number;
}> = ({ x1, y1, x2, y2, curva = 24, inicio, dur = 18, color = EA.color.negro, grosor = 7 }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, dur);
  const ph = progreso(frame, inicio + dur - 4, 8);
  const mx = (x1 + x2) / 2;
  const my = (y1 + y2) / 2 - curva;
  const ang = Math.atan2(y2 - my, x2 - mx);
  const L = 30;
  const a1 = ang + Math.PI * 0.82;
  const a2 = ang - Math.PI * 0.82;
  const cabeza = `M${x2 + L * Math.cos(a1)} ${y2 + L * Math.sin(a1)} L${x2} ${y2} L${x2 + L * Math.cos(a2)} ${y2 + L * Math.sin(a2)}`;
  return (
    <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
      <path d={`M${x1} ${y1} Q${mx} ${my} ${x2} ${y2}`} pathLength={1} fill="none" stroke={color} strokeWidth={grosor} strokeLinecap="round" style={trazo(p)} />
      <path d={cabeza} pathLength={1} fill="none" stroke={color} strokeWidth={grosor} strokeLinecap="round" strokeLinejoin="round" style={trazo(ph)} />
    </svg>
  );
};

/** Círculo a mano alzada alrededor de algo. */
export const Circle: React.FC<{
  cx: number; cy: number; rx: number; ry: number;
  inicio: number; dur?: number; color?: string; grosor?: number; giro?: number;
}> = ({ cx, cy, rx, ry, inicio, dur = 22, color = EA.color.naranja, grosor = 8, giro = -3 }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, dur);
  const d = `M${cx + rx} ${cy} A${rx} ${ry} 0 1 1 ${cx - rx} ${cy} A${rx} ${ry} 0 1 1 ${cx + rx * 0.9} ${cy - ry * 0.35}`;
  return (
    <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
      <g transform={`rotate(${giro} ${cx} ${cy})`}>
        <path d={d} pathLength={1} fill="none" stroke={color} strokeWidth={grosor} strokeLinecap="round" style={trazo(p)} />
      </g>
    </svg>
  );
};
