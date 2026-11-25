import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";

/** Palabra: [texto, segundo en que suena, ¿es clave?]. Los segundos salen de Whisper (palabras.json). */
export type Palabra = [string, number, boolean?];
export type BloquePalabras = { de: number; a: number; lineas: Palabra[][] };

/**
 * Subtítulos palabra por palabra, sin cuadro (aprobados por Eduardo el 25 nov 2026).
 * Van al centro, sobre la línea de la cámara (`y`): la 1.ª línea arriba de la línea, la 2.ª abajo.
 * Letra negra con contorno marfil grueso (se lee sobre el video y sobre los gráficos).
 * Cada palabra brinca cuando suena (6 fotogramas); la clave se pinta de naranja.
 */
export const SubtitulosPalabra: React.FC<{ bloques: BloquePalabras[]; y: number; tam?: number }> = ({ bloques, y, tam = 70 }) => {
  const f = useCurrentFrame();
  const b = bloques.find((x) => f >= x.de * 30 && f < x.a * 30);
  if (!b) return null;
  const sale = salida(f, b.a * 30, 6);
  return (
    <>
      {b.lineas.slice(0, 2).map((ws, fila) => (
        <div
          key={fila}
          style={{
            position: "absolute",
            left: EA.zona.lados,
            right: EA.zona.lados,
            top: y,
            transform: fila === 0 ? "translateY(calc(-100% - 6px))" : "translateY(6px)",
            textAlign: "center",
            whiteSpace: "nowrap",
            fontFamily: EA.font.titulo,
            fontWeight: EA.peso.extra,
            fontSize: tam,
            lineHeight: 1.12,
            color: EA.color.negro,
            WebkitTextStroke: `${Math.round(tam * 0.2)}px ${EA.color.marfil}`,
            paintOrder: "stroke fill",
            opacity: sale,
          }}
        >
          {ws.map(([texto, seg, clave], i) => {
            const p = progreso(f, seg * 30, 6);
            const hl = progreso(f, seg * 30 + 1.5, 7);
            return (
              <React.Fragment key={i}>
                <span
                  style={{
                    position: "relative",
                    display: "inline-block",
                    padding: clave ? `0 ${Math.round(tam * 0.18)}px` : 0,
                    opacity: p,
                    transform: `translateY(${(1 - p) * 0.35}em) scale(${0.75 + 0.25 * p})`,
                  }}
                >
                  {clave ? (
                    <span style={{ position: "absolute", inset: "8% -8px 4% -8px", background: EA.color.naranja, borderRadius: 6, transform: `scaleX(${hl})`, transformOrigin: "left center" }} />
                  ) : null}
                  <span style={{ position: "relative" }}>{texto}</span>
                </span>
                {i < ws.length - 1 ? " " : null}
              </React.Fragment>
            );
          })}
        </div>
      ))}
    </>
  );
};
