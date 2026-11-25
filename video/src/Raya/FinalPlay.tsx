import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { M, p, lerp } from "./marca";

/**
 * Final de los videos de Checa (estilo «La raya»). Elegido por Eduardo el 25 nov 2026:
 * cierre de marca a pantalla completa (semáforo rojo → ámbar → verde → ✓, «Descarga Checa»), más el nombre como sale en la tienda
 * («Checa: Rentabilidad de viajes», sin «Beta») y el distintivo «YA DISPONIBLE EN Play Store», que SALE DE LA LUZ VERDE.
 * Al final todo regresa: el distintivo vuelve a la luz, el semáforo se va, vuelve la cámara y las rayas de la carretera (bucle).
 * Sin link de la web (el canal directo cerró). Sonidos oficiales: logo-semaforo (f 18) y popup-entra (f 62), mezclados con ffmpeg.
 */
export const DURACION_FINAL_CHECA = 195; // 6.5 s

/** Avance de la carretera dentro del final (lo usa el video completo para que el primer cuadro empate con el último). */
export const avanceFinal = (f: number) => {
  let a = 0;
  for (let i = 1; i <= f; i++) a += 9 * (1 - p(i, 4, 14) + p(i, 172, 16));
  return a;
};

const ESC = 5; // semáforo de 60×100 → 300×500
const SX = 540;
const STOP = 380; // borde de arriba del semáforo
const LUZ_V = { x: SX, y: STOP + 74 * ESC };


export const FinalChecaPlay: React.FC = () => {
  const f = useCurrentFrame();

  // cámara y carretera (se van al empezar y regresan al final, para el bucle)
  const camara = 1 - p(f, 0, 14) + p(f, 172, 16);
  const quieta = p(f, 4, 14) * (1 - p(f, 172, 16));
  const avance = avanceFinal(f);
  const rayas = Array.from({ length: 9 }, (_, i) => 900 + ((i * 74 + avance) % 666));

  // semáforo
  const sem = p(f, 10, 14) * (1 - p(f, 160, 12));
  const l = (a: number, b: number) => (f >= a && f < b ? 1 : 0);
  const rojo = l(18, 27), ambar = l(27, 36), verde = f >= 36 ? 1 : 0, paloma = f >= 42 ? 1 : 0;

  // textos
  const titulo = p(f, 46, 12) * (1 - p(f, 148, 8));
  const nombre = p(f, 54, 12) * (1 - p(f, 148, 8));
  const cuentas = p(f, 100, 12) * (1 - p(f, 148, 8));

  // distintivo de Play Store: sale de la luz verde y regresa a ella
  const abre = p(f, 62, 16) * (1 - p(f, 150, 12));
  const W = 560, H = 170;
  const cx = lerp(LUZ_V.x, 540, abre), cy = lerp(LUZ_V.y, 1330, abre);
  const w = lerp(120, W, abre), h = lerp(120, H, abre);
  const dentro = p(f, 74, 10) * (1 - p(f, 146, 6));

  return (
    <AbsoluteFill style={{ background: M.asfalto }}>
      {/* cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 864, background: "#1c1c22", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: M.texto, fontSize: 34, color: M.gris, opacity: camara }}>[ Eduardo a cámara ]</div>
      <div style={{ position: "absolute", left: 0, top: 862, width: 1080, height: 4, background: M.humo, opacity: camara }} />

      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {rayas.map((y, i) => (
          <rect key={i} x={532} y={y} width={16} height={44} rx={8} fill={M.amarillo} opacity={1 - quieta} />
        ))}
        <g opacity={sem} transform={`translate(${SX - 30 * ESC} ${STOP + (1 - sem) * 40}) scale(${ESC})`}>
          <rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" strokeWidth="1.6" />
          <circle cx="30" cy="26" r="9" fill={rojo ? M.rojo : "#4A2222"} />
          <circle cx="30" cy="50" r="9" fill={ambar ? M.amarillo : "#4A3C16"} />
          <circle cx="30" cy="74" r="12" fill={verde ? M.verde : "#1d3a28"} />
          <path d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke={M.sobreVerde} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" opacity={paloma} />
        </g>
      </svg>

      <div style={{ position: "absolute", left: 0, right: 0, top: 960, textAlign: "center", fontFamily: M.titulo, fontWeight: 800, fontSize: 120, letterSpacing: -2, color: M.hueso, opacity: titulo, transform: `translateY(${(1 - titulo) * 24}px)` }}>Descarga Checa</div>
      <div style={{ position: "absolute", left: 0, right: 0, top: 1110, textAlign: "center", fontFamily: M.texto, fontWeight: 500, fontSize: 46, color: M.gris, opacity: nombre, transform: `translateY(${(1 - nombre) * 18}px)` }}>Checa: Rentabilidad de viajes</div>

      {abre > 0.001 ? (
        <div style={{ position: "absolute", left: cx - w / 2, top: cy - h / 2, width: w, height: h, boxSizing: "border-box", borderRadius: lerp(60, 24, abre), background: abre < 0.4 ? M.verde : "#000", border: `${Math.round(lerp(0, 4, abre))}px solid ${M.gris}`, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", overflow: "hidden" }}>
          <div style={{ opacity: dentro, textAlign: "center" }}>
            <div style={{ fontFamily: M.datos, fontWeight: 700, fontSize: 28, letterSpacing: 6, color: M.hueso }}>YA DISPONIBLE EN</div>
            <div style={{ fontFamily: M.titulo, fontWeight: 800, fontSize: 84, lineHeight: 1, color: M.hueso, marginTop: 4 }}>Play Store</div>
          </div>
        </div>
      ) : null}

      <div style={{ position: "absolute", left: 0, right: 0, top: 1470, textAlign: "center", fontFamily: M.datos, fontWeight: 700, fontSize: 34, color: M.gris, opacity: cuentas }}>@checa.driver · @Eduardoalv9802</div>
    </AbsoluteFill>
  );
};
