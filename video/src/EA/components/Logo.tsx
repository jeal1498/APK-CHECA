import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso } from "../theme";

/** Firma EA. discreta (compacta). */
export const Logo: React.FC<{ x: number; y: number; inicio: number; tam?: number; color?: string }> = ({ x, y, inicio, tam = 76, color = EA.color.negro }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, EA.mov.largo);
  return (
    <div style={{ position: "absolute", left: x, top: y, opacity: 0.92 * p, fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: tam, letterSpacing: -3, color, lineHeight: 1 }}>
      EA<span style={{ color: EA.color.naranja }}>.</span>
    </div>
  );
};
