import React from "react";
import { interpolate, useCurrentFrame } from "remotion";

// Gráficos de calle para los videos explicativos (todo vectorial, sin fotos)
export const K = {
  asfalto: "#10161C",
  asfalto2: "#18212A",
  linea: "#F5C518",
  farol: "#FFB547",
  letrero: "#0F6B3A",
  verde: "#22C55E",
  amarillo: "#FACC15",
  rojo: "#F43F5E",
  blanco: "#F4F6F5",
  tenue: "#93A1AC",
};

// Asfalto de noche con líneas de carril que nunca dejan de correr
export const Asfalto: React.FC<{ vel?: number; tono?: string }> = ({ vel = 22, tono = "rgba(255,181,71,0.10)" }) => {
  const frame = useCurrentFrame();
  const off = (frame * vel) % 240;
  const fx = 50 + Math.sin(frame / 50) * 25;
  return (
    <div style={{ position: "absolute", inset: 0, background: K.asfalto, overflow: "hidden" }}>
      <div style={{ position: "absolute", inset: 0, background: `radial-gradient(circle at ${fx}% 18%, ${tono}, transparent 50%)` }} />
      <div style={{ position: "absolute", inset: 0, background: `radial-gradient(circle at ${100 - fx}% 85%, ${tono}, transparent 45%)` }} />
      {[300, 780].map((x) => (
        <div key={x} style={{ position: "absolute", left: x - 7, top: -240 + off, width: 14, height: 2400 }}>
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} style={{ height: 120, marginBottom: 120, background: "rgba(245,197,24,0.16)", borderRadius: 7 }} />
          ))}
        </div>
      ))}
      <div style={{ position: "absolute", left: 40, top: 0, bottom: 0, width: 10, background: "rgba(244,246,245,0.07)" }} />
      <div style={{ position: "absolute", right: 40, top: 0, bottom: 0, width: 10, background: "rgba(244,246,245,0.07)" }} />
    </div>
  );
};

// Letrero de carretera (verde, rojo o amarillo)
export const Letrero: React.FC<{ arriba?: string; grande: string; fondo?: string; texto?: string; ancho?: number; flecha?: boolean }> = ({
  arriba,
  grande,
  fondo = K.letrero,
  texto = K.blanco,
  ancho = 760,
  flecha,
}) => (
  <div style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
    <div
      style={{
        width: ancho,
        background: fondo,
        borderRadius: 26,
        border: `8px solid ${texto}`,
        outline: `6px solid ${fondo}`,
        padding: "22px 30px",
        textAlign: "center",
        color: texto,
        boxShadow: "0 30px 60px rgba(0,0,0,0.55)",
        fontFamily: "Inter",
      }}
    >
      {arriba ? <div style={{ fontWeight: 800, fontSize: 44, letterSpacing: 4 }}>{arriba}</div> : null}
      <div style={{ fontFamily: "InterDisplay", fontWeight: 900, fontSize: 120, lineHeight: 1.02, letterSpacing: -2 }}>
        {flecha ? "↑ " : ""}
        {grande}
      </div>
    </div>
    <div style={{ display: "flex", gap: ancho * 0.5 }}>
      <div style={{ width: 22, height: 70, background: "#6B7682" }} />
      <div style={{ width: 22, height: 70, background: "#6B7682" }} />
    </div>
  </div>
);

// Carro visto de lado; las llantas giran con el avance
export const Carro: React.FC<{ ancho?: number; color?: string; avance: number; humo?: number }> = ({ ancho = 520, color = "#E9EDF0", avance, humo = 0 }) => {
  const giro = avance * 6;
  const rueda = (cx: number) => (
    <g transform={`translate(${cx} 150)`}>
      <circle r={38} fill="#0B0E11" />
      <circle r={22} fill="#AEB7BF" />
      <g transform={`rotate(${giro})`}>
        {[0, 72, 144, 216, 288].map((a) => (
          <rect key={a} x={-3} y={-20} width={6} height={18} fill="#4A535C" transform={`rotate(${a})`} />
        ))}
      </g>
    </g>
  );
  return (
    <svg width={ancho} height={(ancho * 200) / 520} viewBox="0 0 520 200" style={{ overflow: "visible" }}>
      {humo > 0
        ? [0, 1, 2].map((i) => (
            <circle key={i} cx={-20 - i * 40 - humo * 60} cy={140 - i * 10} r={18 + i * 8 + humo * 20} fill="#9AA5AF" opacity={(0.35 - i * 0.08) * (1 - humo)} />
          ))
        : null}
      <ellipse cx={260} cy={190} rx={230} ry={10} fill="#000" opacity={0.45} />
      <path d="M30 150 L30 110 Q34 92 60 88 L150 80 L210 34 Q222 26 240 26 L360 26 Q378 26 392 38 L440 84 L478 92 Q500 98 500 120 L500 150 Z" fill={color} />
      <path d="M222 40 L300 40 L300 84 L170 84 Z" fill="#1E2A35" />
      <path d="M312 40 L362 40 Q372 40 380 48 L414 84 L312 84 Z" fill="#1E2A35" />
      <rect x={30} y={112} width={470} height={8} fill="#C3CBD2" />
      <rect x={470} y={100} width={30} height={14} rx={4} fill={K.amarillo} />
      <rect x={30} y={102} width={18} height={14} rx={3} fill={K.rojo} />
      {rueda(130)}
      {rueda(400)}
    </svg>
  );
};

// Ticket que se imprime línea por línea
export const Ticket: React.FC<{ lineas: { l: string; v: string; c?: string; grande?: boolean; desde: number }[]; ancho?: number }> = ({ lineas, ancho = 820 }) => {
  const frame = useCurrentFrame();
  const vis = lineas.filter((x) => frame >= x.desde);
  return (
    <div style={{ width: ancho, filter: "drop-shadow(0 30px 50px rgba(0,0,0,0.6))" }}>
      <div style={{ background: "#FBFAF5", padding: "30px 44px 20px", fontFamily: "Inter", color: "#16191C" }}>
        <div style={{ textAlign: "center", fontWeight: 800, fontSize: 34, letterSpacing: 6, color: "#6B7280" }}>TU VIAJE REAL</div>
        <div style={{ borderTop: "4px dashed #C9CCD1", margin: "18px 0" }} />
        {vis.map((x, i) => {
          const p = interpolate(frame, [x.desde, x.desde + 8], [0, 1], { extrapolateRight: "clamp" });
          return (
            <div
              key={i}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "baseline",
                padding: x.grande ? "14px 0 6px" : "8px 0",
                borderTop: x.grande ? "4px solid #16191C" : "none",
                opacity: p,
                transform: `translateY(${(1 - p) * -20}px)`,
              }}
            >
              <span style={{ fontWeight: x.grande ? 900 : 600, fontSize: x.grande ? 52 : 42 }}>{x.l}</span>
              <span style={{ fontFamily: "InterDisplay", fontWeight: 900, fontSize: x.grande ? 104 : 64, color: x.c ?? "#16191C" }}>{x.v}</span>
            </div>
          );
        })}
      </div>
      <svg width={ancho} height={26} viewBox={`0 0 ${ancho} 26`} preserveAspectRatio="none" style={{ display: "block" }}>
        <path d={`M0 0 ${Array.from({ length: 28 }).map((_, i) => `L${((i + 0.5) * ancho) / 28} 24 L${((i + 1) * ancho) / 28} 0`).join(" ")} Z`} fill="#FBFAF5" />
      </svg>
    </div>
  );
};

// Bomba de gasolina con display digital
export const Bomba: React.FC<{ pesos: number; litros: number; tam?: number }> = ({ pesos, litros, tam = 360 }) => (
  <svg width={tam} height={tam * 1.5} viewBox="0 0 240 360">
    <rect x={30} y={20} width={150} height={320} rx={18} fill="#D8343F" />
    <rect x={20} y={330} width={170} height={24} rx={8} fill="#5B6670" />
    <rect x={48} y={44} width={114} height={92} rx={10} fill="#0B0F12" />
    <text x={156} y={82} textAnchor="end" fontFamily="Inter" fontWeight={800} fontSize={30} fill="#7CFF8A">
      ${pesos.toFixed(0)}
    </text>
    <text x={156} y={120} textAnchor="end" fontFamily="Inter" fontWeight={700} fontSize={22} fill="#7CFF8A" opacity={0.8}>
      {litros.toFixed(1)} L
    </text>
    <rect x={60} y={160} width={90} height={60} rx={8} fill="#F4F6F5" opacity={0.9} />
    <text x={105} y={200} textAnchor="middle" fontFamily="Inter" fontWeight={900} fontSize={28} fill="#D8343F">
      GAS
    </text>
    <path d="M180 120 Q220 120 220 170 L220 270 Q220 290 205 290" stroke="#2A3138" strokeWidth={12} fill="none" strokeLinecap="round" />
    <rect x={192} y={282} width={30} height={46} rx={6} fill="#2A3138" />
  </svg>
);

// Billetes que salen volando
export const Billetes: React.FC<{ desde: number; n?: number; color?: string }> = ({ desde, n = 8, color = "#3FA06A" }) => {
  const frame = useCurrentFrame();
  const f = frame - desde;
  if (f < 0 || f > 50) return null;
  return (
    <>
      {Array.from({ length: n }).map((_, i) => {
        const ang = (i / n) * Math.PI * 2 + 0.4;
        const d = f * (16 + (i % 3) * 5);
        const x = Math.cos(ang) * d;
        const y = Math.sin(ang) * d - f * 2 + f * f * 0.25;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: 540 + x - 60,
              top: 900 + y - 32,
              width: 120,
              height: 64,
              borderRadius: 8,
              background: color,
              border: "4px solid #1F5E3B",
              color: "#E8FFF0",
              fontFamily: "InterDisplay",
              fontWeight: 900,
              fontSize: 40,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              transform: `rotate(${f * (i % 2 ? 9 : -11)}deg)`,
              opacity: interpolate(f, [30, 50], [1, 0], { extrapolateLeft: "clamp" }),
            }}
          >
            $
          </div>
        );
      })}
    </>
  );
};

// Semáforo con una luz encendida
export const SemaforoLuz: React.FC<{ activo: "rojo" | "amarillo" | "verde"; alto?: number }> = ({ activo, alto = 520 }) => {
  const luces: ("rojo" | "amarillo" | "verde")[] = ["rojo", "amarillo", "verde"];
  const col = { rojo: K.rojo, amarillo: K.amarillo, verde: K.verde };
  return (
    <svg width={alto * 0.45} height={alto} viewBox="0 0 180 400">
      <rect x={10} y={10} width={160} height={380} rx={40} fill="#11161B" stroke="#2C353E" strokeWidth={6} />
      {luces.map((l, i) => (
        <g key={l}>
          {l === activo ? <circle cx={90} cy={80 + i * 120} r={70} fill={col[l]} opacity={0.3} /> : null}
          <circle cx={90} cy={80 + i * 120} r={46} fill={l === activo ? col[l] : "#252D35"} />
        </g>
      ))}
    </svg>
  );
};
