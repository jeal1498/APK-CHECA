import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso } from "../theme";

/** Firma EA. Variante compacta (EA.) u horizontal (EA. | EDUARDO ALVARADO). */
export const Logo: React.FC<{
  x: number;
  y: number;
  inicio: number;
  tam?: number;
  color?: string;
  variante?: "compacta" | "horizontal";
}> = ({ x, y, inicio, tam = 76, color = EA.color.negro, variante = "compacta" }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, EA.mov.largo);
  return (
    <div style={{ position: "absolute", left: x, top: y, opacity: 0.92 * p, display: "flex", alignItems: "center", gap: tam * 0.3, color, lineHeight: 1 }}>
      <div style={{ fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: tam, letterSpacing: -3 }}>
        EA<span style={{ color: EA.color.naranja }}>.</span>
      </div>
      {variante === "horizontal" ? (
        <>
          <div style={{ width: 3, height: tam * 0.8, background: color, opacity: 0.5 }} />
          <div style={{ fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: tam * 0.3, letterSpacing: tam * 0.05, textTransform: "uppercase", lineHeight: 1.3 }}>
            Eduardo
            <br />
            Alvarado
          </div>
        </>
      ) : null}
    </div>
  );
};
