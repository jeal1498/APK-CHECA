import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { EA, ease, progreso } from "../theme";

/** Tarjeta de cifra: etiqueta, contador animado e indicador opcional. */
export const StatCard: React.FC<{
  etiqueta: string;
  valor: number;
  prefijo?: string;
  sufijo?: string;
  decimales?: number;
  indicador?: { sube: boolean; texto: string };
  x: number;
  y: number;
  w?: number;
  h?: number;
  inicio: number;
  tamValor?: number;
}> = ({ etiqueta, valor, prefijo = "$", sufijo = "", decimales = 0, indicador, x, y, w = 420, h = 240, inicio, tamValor = 92 }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, inicio, EA.mov.medio);
  const cuenta = interpolate(frame, [inicio + 6, inicio + 30], [0, valor], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: ease,
  });
  return (
    <div
      style={{
        position: "absolute",
        left: x,
        top: y,
        width: w,
        height: h,
        background: EA.color.blanco,
        border: EA.borde,
        borderRadius: EA.radio,
        boxShadow: EA.sombra,
        padding: "22px 26px",
        opacity: p,
        transform: `translateY(${(1 - p) * 60}px) scale(${0.96 + 0.04 * p})`,
        fontFamily: EA.font.texto,
        color: EA.color.negro,
      }}
    >
      <span style={{ display: "inline-block", background: EA.color.naranja, padding: "4px 12px", fontWeight: EA.peso.medio, fontSize: 24, letterSpacing: 3, textTransform: "uppercase", borderRadius: 3 }}>
        {etiqueta}
      </span>
      <div style={{ fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: tamValor, lineHeight: 1.12, letterSpacing: -2, marginTop: 8 }}>
        {prefijo}
        {cuenta.toFixed(decimales)}
        {sufijo}
      </div>
      {indicador ? (
        <div style={{ fontWeight: EA.peso.medio, fontSize: 30, marginTop: 2 }}>
          <span style={{ color: EA.color.naranja }}>{indicador.sube ? "▲" : "▼"}</span> {indicador.texto}
        </div>
      ) : null}
    </div>
  );
};
