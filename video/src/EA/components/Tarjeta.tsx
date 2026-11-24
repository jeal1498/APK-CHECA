import React from "react";
import { EA } from "../theme";

/** Tarjeta base animable: centro, tamaño, radio, escala, desenfoque y cola (para convertirse en globo). */
export const Tarjeta: React.FC<{
  cx: number;
  cy: number;
  w: number;
  h: number;
  radio?: number;
  escala?: number;
  opacidad?: number;
  blur?: number;
  cola?: number;
  borde?: "solido" | "punteado";
  fondo?: string;
  sombra?: boolean;
  children?: React.ReactNode;
}> = ({ cx, cy, w, h, radio = EA.radio, escala = 1, opacidad = 1, blur = 0, cola = 0, borde = "solido", fondo = EA.color.blanco, sombra = true, children }) => (
  <div
    style={{
      position: "absolute",
      left: cx - w / 2,
      top: cy - h / 2,
      width: w,
      height: h,
      opacity: opacidad,
      transform: `scale(${escala})`,
      transformOrigin: "center center",
      filter: blur > 0.05 ? `blur(${blur}px)` : undefined,
    }}
  >
    <div
      style={{
        position: "absolute",
        inset: 0,
        boxSizing: "border-box",
        background: fondo,
        border: `3px ${borde === "punteado" ? "dashed" : "solid"} ${EA.color.negro}`,
        borderRadius: radio,
        boxShadow: sombra ? EA.sombra : "none",
        overflow: "hidden",
        fontFamily: EA.font.titulo,
        fontWeight: EA.peso.extra,
        color: EA.color.negro,
      }}
    >
      {children}
    </div>
    {cola > 0 ? (
      <>
        <div style={{ position: "absolute", left: 70, top: h - 3, width: 0, height: 0, borderTop: `${64 * cola}px solid ${EA.color.negro}`, borderRight: `${54 * cola}px solid transparent` }} />
        <div style={{ position: "absolute", left: 73, top: h - 6, width: 0, height: 0, borderTop: `${57 * cola}px solid ${EA.color.blanco}`, borderRight: `${47 * cola}px solid transparent` }} />
      </>
    ) : null}
  </div>
);
