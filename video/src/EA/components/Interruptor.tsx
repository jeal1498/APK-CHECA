import React from "react";
import { interpolateColors, useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";

/** Interruptor físico: la perilla se desliza de ✕ a ✓ y el riel pasa de negro a naranja. */
export const Interruptor: React.FC<{ x: number; y: number; inicio: number; cambia: number; fin?: number; w?: number; h?: number }> = ({
  x, y, inicio, cambia, fin = 100000, w = 240, h = 96,
}) => {
  const frame = useCurrentFrame();
  const entra = progreso(frame, inicio, EA.mov.corto);
  const v = progreso(frame, cambia, 12);
  const pad = 10;
  const perilla = h - 2 * pad;
  const fondo = interpolateColors(v, [0, 1], [EA.color.negro, EA.color.naranja]);
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        width: w,
        height: h,
        borderRadius: h / 2,
        background: fondo,
        border: EA.borde,
        boxSizing: "border-box",
        opacity: entra * salida(frame, fin, 8),
        transform: `translateY(${(1 - entra) * 24}px)`,
        fontFamily: EA.font.titulo,
        fontWeight: EA.peso.extra,
      }}
    >
      <div style={{ position: "absolute", right: 26, top: 0, height: "100%", display: "flex", alignItems: "center", fontSize: 40, color: EA.color.marfil, opacity: 1 - v }}>✕</div>
      <div style={{ position: "absolute", left: 26, top: 0, height: "100%", display: "flex", alignItems: "center", fontSize: 40, color: EA.color.negro, opacity: v }}>✓</div>
      <div
        style={{
          position: "absolute",
          top: pad - 3,
          left: pad - 3 + v * (w - 2 * pad - perilla),
          width: perilla,
          height: perilla,
          borderRadius: "50%",
          background: EA.color.blanco,
          border: EA.borde,
          boxSizing: "border-box",
        }}
      />
    </div>
  );
};
