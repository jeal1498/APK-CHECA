import React from "react";
import { EA, progreso } from "../theme";
import type { Parte } from "../theme";

/** Una línea de texto con partes resaltadas en naranja (bloque que se expande). */
export const Linea: React.FC<{
  partes: Parte[];
  frame: number;
  inicioResalte: number;
  durResalte?: number;
}> = ({ partes, frame, inicioResalte, durResalte = EA.mov.medio }) => {
  const p = progreso(frame, inicioResalte, durResalte);
  return (
    <>
      {partes.map((parte, i) => (
        <span key={i} style={{ position: "relative", display: "inline-block", whiteSpace: "pre", padding: parte.naranja ? "0 14px" : 0 }}>
          {parte.naranja ? (
            <span
              style={{
                position: "absolute",
                inset: "6% 0 4% 0",
                background: EA.color.naranja,
                transform: `scaleX(${p})`,
                transformOrigin: "left center",
                borderRadius: 4,
              }}
            />
          ) : null}
          <span style={{ position: "relative" }}>{parte.t}</span>
        </span>
      ))}
    </>
  );
};
