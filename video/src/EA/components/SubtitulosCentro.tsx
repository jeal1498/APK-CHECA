import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";
import { Linea } from "./Resaltado";
import type { Bloque } from "./Subtitles";

/**
 * Subtítulos al centro de la pantalla, sobre la línea que separa la cámara de los gráficos (`y` = centro del bloque).
 * Cada línea va en una franja marfil con borde negro, así se lee sobre el video y sobre los gráficos. Máx. 2 líneas.
 */
export const SubtitulosCentro: React.FC<{ bloques: Bloque[]; y: number; tam?: number }> = ({ bloques, y, tam = 60 }) => {
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
        transform: `translateY(calc(-50% + ${(1 - entra) * 18}px))`,
        opacity: entra * sale,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 8,
        fontFamily: EA.font.titulo,
        fontWeight: EA.peso.extra,
        fontSize: tam,
        lineHeight: 1.15,
        letterSpacing: -1,
        color: EA.color.negro,
      }}
    >
      {b.lineas.slice(0, 2).map((partes, i) => (
        <div key={i} style={{ background: EA.color.marfil, border: `4px solid ${EA.color.negro}`, borderRadius: 10, padding: "6px 22px 8px" }}>
          <Linea partes={partes} frame={frame} inicioResalte={b.de + 4} durResalte={EA.mov.corto} />
        </div>
      ))}
    </div>
  );
};
