import React from "react";
import { useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "../theme";

const TAM = {
  s: { w: 440, titulo: 36, texto: 26, pad: 22 },
  m: { w: 600, titulo: 46, texto: 32, pad: 28 },
  l: { w: 820, titulo: 62, texto: 40, pad: 36 },
} as const;

/** Tarjeta informativa: etiqueta naranja, titular y texto. Entra con desplazamiento y escala. */
export const Popup: React.FC<{
  etiqueta?: string;
  titulo: string;
  texto?: string;
  tam?: "s" | "m" | "l";
  x: number;
  y: number;
  inicio: number;
  fin?: number;
  fondo?: string;
}> = ({ etiqueta, titulo, texto, tam = "m", x, y, inicio, fin = 100000, fondo = EA.color.marfil }) => {
  const frame = useCurrentFrame();
  const t = TAM[tam];
  const p = progreso(frame, inicio, EA.mov.medio);
  const s = salida(frame, fin);
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        width: t.w,
        background: fondo,
        border: EA.borde,
        borderRadius: EA.radio,
        boxShadow: EA.sombra,
        padding: t.pad,
        color: EA.color.negro,
        opacity: p * s,
        transform: `translateY(${(1 - p) * 50}px) scale(${0.94 + 0.06 * p})`,
        fontFamily: EA.font.texto,
      }}
    >
      {etiqueta ? (
        <span style={{ display: "inline-block", background: EA.color.naranja, padding: "3px 12px", fontWeight: EA.peso.medio, fontSize: t.texto * 0.7, letterSpacing: 3, textTransform: "uppercase", borderRadius: 3, marginBottom: 10 }}>
          {etiqueta}
        </span>
      ) : null}
      <div style={{ fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: t.titulo, lineHeight: 1.1, letterSpacing: -1 }}>{titulo}</div>
      {texto ? <div style={{ fontWeight: EA.peso.normal, fontSize: t.texto, lineHeight: 1.3, marginTop: 8, opacity: 0.85 }}>{texto}</div> : null}
    </div>
  );
};
