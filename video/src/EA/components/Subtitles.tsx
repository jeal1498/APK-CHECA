import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";
import type { Parte } from "../theme";
import { Linea } from "./Resaltado";

export type Bloque = { de: number; a: number; lineas: Parte[][] };

/** Subtítulos por bloques (máx. 2 líneas), palabra clave en naranja. */
export const Subtitles: React.FC<{ bloques: Bloque[]; y: number; tam?: number; color?: string }> = ({
  bloques,
  y,
  tam = 66,
  color = EA.color.negro,
}) => {
  const frame = useCurrentFrame();
  const b = bloques.find((x) => frame >= x.de && frame < x.a);
  if (!b) return null;
  const entra = progreso(frame, b.de, EA.mov.corto);
  const sale = salida(frame, b.a, 6);
  return (
    <div
      style={{
        position: "absolute",
        left: EA.zona.lados,
        right: EA.zona.lados,
        top: y,
        textAlign: "center",
        color,
        fontFamily: EA.font.titulo,
        fontWeight: EA.peso.extra,
        fontSize: tam,
        lineHeight: 1.22,
        letterSpacing: -1,
        opacity: entra * sale,
        transform: `translateY(${(1 - entra) * 26}px)`,
      }}
    >
      {b.lineas.slice(0, 2).map((partes, i) => (
        <div key={i}>
          <Linea partes={partes} frame={frame} inicioResalte={b.de + 4} durResalte={EA.mov.corto} />
        </div>
      ))}
    </div>
  );
};
