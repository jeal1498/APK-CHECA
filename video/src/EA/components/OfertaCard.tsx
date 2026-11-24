import React from "react";
import { EA } from "../theme";

/**
 * Tarjeta de oferta (ejemplo) que se transforma: tamaño real por ancho/alto, `escala` solo para encogerla al comparar.
 * Borde de 5 px para que al 72 % siga midiendo más de 3 px. Sin sombra ni desenfoque. `cola` la vuelve globo de comentario.
 */
export const OfertaCard: React.FC<{
  cx: number;
  cy: number;
  w: number;
  h: number;
  escala?: number;
  opacidad?: number;
  radio?: number;
  cola?: number;
  children?: React.ReactNode;
}> = ({ cx, cy, w, h, escala = 1, opacidad = 1, radio = 12, cola = 0, children }) => (
  <div style={{ position: "absolute", left: cx - w / 2, top: cy - h / 2, width: w, height: h, opacity: opacidad, transform: `scale(${escala})`, transformOrigin: "center center" }}>
    {cola > 0 ? (
      <svg style={{ position: "absolute", left: 70, top: h - 5 }} width={70} height={74}>
        <polygon points={`0,0 ${60 * cola},0 0,${68 * cola}`} fill={EA.color.blanco} stroke={EA.color.negro} strokeWidth={5} strokeLinejoin="round" />
      </svg>
    ) : null}
    <div
      style={{
        position: "absolute",
        inset: 0,
        boxSizing: "border-box",
        background: EA.color.blanco,
        border: `5px solid ${EA.color.negro}`,
        borderRadius: radio,
        overflow: "hidden",
        fontFamily: EA.font.titulo,
        fontWeight: EA.peso.extra,
        color: EA.color.negro,
      }}
    >
      {children}
    </div>
    {cola > 0 ? (
      <div style={{ position: "absolute", left: 75, top: h - 5, width: 60 * cola - 12, height: 6, background: EA.color.blanco }} />
    ) : null}
  </div>
);

/** Fila de dato de la oferta: marcador naranja, cifra en grande y, si hay, la cuenta en pequeño. */
export const Fila: React.FC<{ y: number; op: number; texto: string; cuenta?: string }> = ({ y, op, texto, cuenta }) =>
  op > 0.001 ? (
    <div style={{ position: "absolute", left: 40, right: 30, top: y, opacity: op, transform: `translateX(${(1 - op) * -24}px)`, display: "flex", alignItems: "flex-start", gap: 22 }}>
      <div style={{ width: 10, height: 56, background: EA.color.naranja, borderRadius: 3, flexShrink: 0 }} />
      <div>
        <div style={{ fontSize: 54, letterSpacing: -1, lineHeight: "56px" }}>{texto}</div>
        {cuenta ? <div style={{ fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 30, marginTop: 6, color: "rgba(21,21,21,0.7)" }}>{cuenta}</div> : null}
      </div>
    </div>
  ) : null;
