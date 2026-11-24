import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso } from "../theme";
import { StatCard } from "./StatCard";
import { Arrow } from "./Annotations";

type Lado = { etiqueta: string; valor: number; decimales?: number; prefijo?: string; sufijo?: string };

/** Dos tarjetas enfrentadas, flecha que las une e indicador de diferencia (animación secuencial). */
export const Comparison: React.FC<{ a: Lado; b: Lado; diferencia: string; y: number; inicio: number }> = ({ a, b, diferencia, y, inicio }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio + 50, EA.mov.corto);
  return (
    <>
      <StatCard {...a} x={60} y={y} inicio={inicio} />
      <Arrow x1={490} y1={y + 130} x2={592} y2={y + 130} curva={26} inicio={inicio + 14} />
      <StatCard {...b} x={600} y={y} inicio={inicio + 22} indicador={{ sube: false, texto: diferencia }} />
      <div
        style={{
          position: "absolute",
          left: 60,
          top: y + 270,
          width: 960,
          textAlign: "center",
          opacity: p,
          transform: `translateY(${(1 - p) * 20}px)`,
          fontFamily: EA.font.titulo,
          fontWeight: EA.peso.extra,
          fontSize: 44,
          color: EA.color.negro,
        }}
      >
        <span style={{ background: EA.color.naranja, padding: "4px 18px", borderRadius: 4 }}>{diferencia}</span>
      </div>
    </>
  );
};
