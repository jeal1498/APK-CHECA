import React from "react";
import { AbsoluteFill, Easing, interpolate, useCurrentFrame } from "remotion";
import type { Escena } from "./plantilla";

// Cambio de toma cada ~3 s dentro de una escena larga (como un corte de zoom en edición):
// alterna encuadres (acercar, alejar, mover) para que algo cambie en pantalla al menos cada 3 s.
const CADA = 90; // 3 s a 30 fps
const TOMAS = [
  { s: 1, x: 0, y: 0 },
  { s: 1.06, x: 0, y: -30 },
  { s: 0.95, x: 0, y: 20 },
  { s: 1.05, x: -20, y: -20 },
];
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

export const Toma: React.FC<{ esc: Escena[]; children: React.ReactNode }> = ({ esc, children }) => {
  const frame = useCurrentFrame();
  const i = Math.max(0, esc.findIndex((e) => frame < e.hasta));
  const e = esc[i] ?? esc[esc.length - 1];
  const t = frame - e.desde;
  // sin cortes en el último segundo de la escena ni en el cierre
  const max = i === esc.length - 1 ? 0 : Math.max(0, Math.floor((e.hasta - e.desde - 30) / CADA));
  const k = Math.min(max, Math.floor(t / CADA));
  const cur = TOMAS[k % TOMAS.length];
  const prev = TOMAS[Math.max(0, k - 1) % TOMAS.length];
  const a = k === 0 ? 1 : interpolate(t - k * CADA, [0, 7], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
  const s = prev.s + (cur.s - prev.s) * a;
  const x = prev.x + (cur.x - prev.x) * a;
  const y = prev.y + (cur.y - prev.y) * a;
  const destello = k === 0 ? 0 : interpolate(t - k * CADA, [0, 2, 8], [0, 0.12, 0], clamp);
  return (
    <>
      <AbsoluteFill style={{ transform: `translate(${x}px, ${y}px) scale(${s})`, transformOrigin: "50% 45%" }}>{children}</AbsoluteFill>
      <AbsoluteFill style={{ background: "#FFFFFF", opacity: destello, pointerEvents: "none" }} />
    </>
  );
};
