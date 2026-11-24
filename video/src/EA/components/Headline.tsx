import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso } from "../theme";
import type { Parte } from "../theme";
import { Linea } from "./Resaltado";

/** Titular editorial: cada línea sube desde una máscara; lo naranja se rellena después. */
export const Headline: React.FC<{
  lineas: Parte[][];
  x: number;
  y: number;
  inicio: number;
  tam?: number;
  separacion?: number;
}> = ({ lineas, x, y, inicio, tam = 92, separacion = 8 }) => {
  const frame = useCurrentFrame();
  return (
    <div style={{ position: "absolute", left: x, top: y, color: EA.color.negro, fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: tam, lineHeight: 1.08, letterSpacing: -2 }}>
      {lineas.map((partes, i) => {
        const ini = inicio + i * separacion;
        const p = progreso(frame, ini);
        return (
          <div key={i} style={{ overflow: "hidden", paddingBottom: 6 }}>
            <div style={{ transform: `translateY(${(1 - p) * 110}%)` }}>
              <Linea partes={partes} frame={frame} inicioResalte={ini + 14} />
            </div>
          </div>
        );
      })}
    </div>
  );
};
