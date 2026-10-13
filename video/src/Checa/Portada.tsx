import React from "react";
import { AbsoluteFill, Img, staticFile } from "remotion";
import { Logo } from "./ui";

// Portada fija (primer cuadro para el perfil de TikTok/Reels), estilo 1Volt a color.
export type PropsPortada = { titulo: string; sub: string; color: string; etiqueta: string; fondo?: string };
export const PORTADA_DEFAULT: PropsPortada = { titulo: "12 HORAS Y LA LANA NO APARECE", sub: "lo que se va sin que lo veas", color: "#F43F5E", etiqueta: "1 / 14" };

export const Portada: React.FC<PropsPortada> = ({ titulo, sub, color, etiqueta, fondo }) => (
  <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 42%, ${color}33 0%, #05060F 72%)` }}>
    {fondo ? (
      <>
        <Img src={staticFile(fondo)} style={{ position: "absolute", width: "100%", height: "100%", objectFit: "cover" }} />
        <AbsoluteFill style={{ background: "linear-gradient(180deg, rgba(5,6,15,0.85) 0%, rgba(5,6,15,0.25) 40%, rgba(5,6,15,0.3) 60%, rgba(5,6,15,0.9) 100%)" }} />
      </>
    ) : (
      <svg width={1080} height={1920} style={{ position: "absolute" }}>
        {Array.from({ length: 12 }).map((_, k) => (
          <line key={k} x1={0} y1={160 * k} x2={1080} y2={160 * k} stroke={color} strokeOpacity={0.06} strokeWidth={2} />
        ))}
        <circle cx={540} cy={960} r={420} fill="none" stroke={color} strokeOpacity={0.25} strokeWidth={6} strokeDasharray="30 22" />
      </svg>
    )}
    {etiqueta ? <div style={{ position: "absolute", top: 170, left: 0, right: 0, display: "flex", justifyContent: "center" }}>
      <div style={{ fontFamily: "FiraCode", fontSize: 40, color, border: `4px solid ${color}`, borderRadius: 14, padding: "8px 24px", background: "rgba(5,6,15,0.8)" }}>{etiqueta}</div>
    </div> : null}
    <div style={{ position: "absolute", top: fondo ? 1150 : 640, left: 70, right: 70, textAlign: "center" }}>
      <div style={{ fontFamily: "Inter", fontWeight: 800, fontSize: 118, lineHeight: 1.02, color: "#FFFFFF", WebkitTextStroke: "4px #000", paintOrder: "stroke fill", textShadow: `0 8px 0 #000, 0 0 50px ${color}88` }}>
        {titulo.split("|").map((t, k) => (
          <div key={k} style={{ color: k % 2 ? color : "#FFFFFF" }}>{t}</div>
        ))}
      </div>
      <div style={{ fontFamily: "FiraCode", fontSize: 44, color, marginTop: 36 }}>{sub}</div>
    </div>
    <div style={{ position: "absolute", bottom: 230, left: 0, right: 0, display: "flex", justifyContent: "center", transform: "scale(0.75)" }}>
      <Logo tam={100} />
    </div>
  </AbsoluteFill>
);
