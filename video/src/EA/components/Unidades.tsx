import React from "react";
import { EA } from "../theme";

export type EstadoUnidad = { opacidad?: number; escala?: number; escalaY?: number; color?: string; dx?: number; dy?: number };

/**
 * Rejilla de unidades (1 cuadro = 1 unidad), en orden de columnas.
 * Cada cuadro recibe su estado por función: color, escala, opacidad y desplazamiento.
 */
export const Unidades: React.FC<{
  total?: number;
  filas?: number;
  celda?: number;
  hueco?: number;
  x: number;
  y: number;
  blur?: number;
  estado: (i: number) => EstadoUnidad;
}> = ({ total = 80, filas = 5, celda = 52, hueco = 8, x, y, blur = 0, estado }) => (
  <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, filter: blur > 0.05 ? `blur(${blur}px)` : undefined }}>
    {Array.from({ length: total }, (_, i) => {
      const e = estado(i);
      if ((e.opacidad ?? 1) <= 0.001) return null;
      const c = Math.floor(i / filas);
      const r = i % filas;
      return (
        <div
          key={i}
          style={{
            position: "absolute",
            left: x + c * (celda + hueco) + (e.dx ?? 0),
            top: y + r * (celda + hueco) + (e.dy ?? 0),
            width: celda,
            height: celda,
            background: e.color ?? EA.color.negro,
            borderRadius: 6,
            opacity: e.opacidad ?? 1,
            transform: `scale(${e.escala ?? 1}, ${e.escalaY ?? e.escala ?? 1})`,
          }}
        />
      );
    })}
  </div>
);
