import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";

/** Identificador compacto: franja naranja + nombre + rol. */
export const LowerThird: React.FC<{ nombre: string; rol?: string; x: number; y: number; inicio: number; fin: number }> = ({ nombre, rol, x, y, inicio, fin }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, EA.mov.medio);
  const s = salida(frame, fin);
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        display: "flex",
        opacity: s,
        clipPath: `inset(0 ${(1 - p) * 100}% 0 0)`,
        fontFamily: EA.font.texto,
      }}
    >
      <div style={{ width: 12, background: EA.color.naranja }} />
      <div style={{ background: EA.color.negro, color: EA.color.marfil, padding: "14px 28px 16px 22px" }}>
        <div style={{ fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 42, letterSpacing: -1, textTransform: "uppercase" }}>{nombre}</div>
        {rol ? <div style={{ fontWeight: EA.peso.medio, fontSize: 26, color: EA.color.naranja, letterSpacing: 2, textTransform: "uppercase" }}>{rol}</div> : null}
      </div>
    </div>
  );
};
