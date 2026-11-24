import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { EA, ease, progreso } from "../theme";

export type Dato = { etiqueta: string; valor: number; destacado?: boolean };

const fmt = (n: number, dec: number) => n.toLocaleString("en-US", { minimumFractionDigits: dec, maximumFractionDigits: dec });

/** Gráfica limpia: barras horizontales, verticales o progreso en %. */
export const Chart: React.FC<{
  tipo: "horizontal" | "vertical" | "progreso";
  datos: Dato[];
  x: number;
  y: number;
  w: number;
  h: number;
  inicio: number;
  prefijo?: string;
  sufijo?: string;
  decimales?: number;
  max?: number;
}> = ({ tipo, datos, x, y, w, h, inicio, prefijo = "", sufijo = "", decimales = 0, max }) => {
  const frame = useCurrentFrame();
  const tope = max ?? (tipo === "progreso" ? 100 : Math.max(...datos.map((d) => d.valor)));
  const color = (d: Dato) => (d.destacado ? EA.color.naranja : EA.color.negro);
  const caja: React.CSSProperties = { position: "absolute", left: x, top: y, width: w, height: h, fontFamily: EA.font.texto, color: EA.color.negro };

  if (tipo === "vertical") {
    const colW = w / datos.length;
    return (
      <div style={{ ...caja, display: "flex", alignItems: "flex-end" }}>
        <div style={{ position: "absolute", left: 0, right: 0, bottom: 50, height: 3, background: EA.color.negro }} />
        {datos.map((d, i) => {
          const p = progreso(frame, inicio + i * 6, EA.mov.largo);
          const alto = ((h - 100) * d.valor) / tope;
          return (
            <div key={i} style={{ width: colW, height: "100%", position: "relative" }}>
              <div style={{ position: "absolute", left: colW * 0.18, width: colW * 0.64, bottom: 53, height: alto * p, background: color(d), borderRadius: "4px 4px 0 0" }} />
              <div style={{ position: "absolute", left: 0, width: colW, bottom: 53 + alto * p + 8, textAlign: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 34, opacity: p }}>
                {prefijo}
                {fmt(d.valor * p, decimales)}
                {sufijo}
              </div>
              <div style={{ position: "absolute", left: 0, width: colW, bottom: 8, textAlign: "center", fontWeight: EA.peso.medio, fontSize: 26, letterSpacing: 2, textTransform: "uppercase" }}>{d.etiqueta}</div>
            </div>
          );
        })}
      </div>
    );
  }

  const fila = h / datos.length;
  return (
    <div style={caja}>
      {datos.map((d, i) => {
        const p = tipo === "progreso" ? interpolate(frame, [inicio + i * 6, inicio + i * 6 + 36], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: ease }) : progreso(frame, inicio + i * 6, EA.mov.largo);
        const barraW = w * 0.62;
        return (
          <div key={i} style={{ position: "absolute", left: 0, top: i * fila, width: w, height: fila, display: "flex", alignItems: "center" }}>
            <div style={{ width: w * 0.3, fontWeight: EA.peso.medio, fontSize: 28, letterSpacing: 2, textTransform: "uppercase" }}>{d.etiqueta}</div>
            <div style={{ position: "relative", width: barraW, height: Math.min(fila * 0.52, 54), background: tipo === "progreso" ? "rgba(21,21,21,0.12)" : "transparent", borderRadius: 4 }}>
              <div style={{ width: `${((d.valor * p) / tope) * 100}%`, height: "100%", background: color(d), borderRadius: 4 }} />
            </div>
            <div style={{ width: w * 0.08 + 40, paddingLeft: 14, fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 32, whiteSpace: "nowrap" }}>
              {prefijo}
              {fmt(d.valor * p, decimales)}
              {sufijo}
            </div>
          </div>
        );
      })}
    </div>
  );
};
