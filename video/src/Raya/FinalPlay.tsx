import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { M, p, lerp } from "./marca";

/**
 * Final de los videos de Checa (estilo «La raya», decidido el 25 nov 2026):
 * semáforo rojo → ámbar → verde → ✓ (1.2 s); el pop-up SALE DEL SEMÁFORO (la luz verde crece y se abre en la tarjeta de Google Play);
 * después todo regresa: la tarjeta vuelve a la luz, el semáforo se va y las rayas vuelven a la carretera (último cuadro = primero).
 * Checa ya está en producción en Google Play (Eduardo, 25 nov).
 * Sonidos oficiales: logo-semaforo (f 18) y popup-entra (f 62), mezclados con ffmpeg.
 */
export const DURACION_FINAL_CHECA = 195; // 6.5 s

const SX = 540; // centro del semáforo
const SY = 1140;
const ESC = 3.0; // semáforo de 60×100 → 180×300
const LUZ_V = { x: SX + (30 - 30) * ESC, y: SY + (74 - 50) * ESC }; // centro de la luz verde

type Palabra = [string, number, string?];
const SUBS: { de: number; a: number; lineas: Palabra[][] }[] = [
  { de: 0.3, a: 2.7, lineas: [[["YA", 0.4], ["ESTÁ", 0.6], ["EN", 0.85]], [["GOOGLE", 1.1, M.verde], ["PLAY", 1.4, M.verde]]] },
  { de: 2.8, a: 5.2, lineas: [[["BÚSCALA", 2.9]], [["COMO", 3.4], ["CHECA", 3.7, M.verde]]] },
];

const Subtitulos: React.FC<{ f: number }> = ({ f }) => {
  const b = SUBS.find((x) => f >= x.de * 30 && f < x.a * 30 + 5);
  if (!b) return null;
  const sale = 1 - p(f, b.a * 30, 5);
  return (
    <>
      {b.lineas.map((ws, fila) => (
        <div key={fila} style={{ position: "absolute", left: 60, right: 60, top: 864, transform: fila === 0 ? "translateY(calc(-100% - 8px))" : "translateY(8px)", textAlign: "center", whiteSpace: "nowrap", fontFamily: M.titulo, fontWeight: 800, fontSize: 80, lineHeight: 1.1, color: M.hueso, WebkitTextStroke: `16px #000`, paintOrder: "stroke fill", opacity: sale }}>
          {ws.map(([t, s, c], i) => {
            const q = p(f, s * 30, 4.5); // 150 ms por palabra (marca)
            return (
              <React.Fragment key={i}>
                <span style={{ display: "inline-block", color: c ?? M.hueso, opacity: q, transform: `translateY(${(1 - q) * 22}px) scale(${0.8 + 0.2 * q})` }}>{t}</span>
                {i < ws.length - 1 ? " " : null}
              </React.Fragment>
            );
          })}
        </div>
      ))}
    </>
  );
};

/** Ícono de la app (icono-app.svg de la marca). */
const Icono: React.FC<{ tam: number }> = ({ tam }) => (
  <svg width={tam} height={tam} viewBox="0 0 512 512">
    <rect width="512" height="512" rx="112" fill={M.asfalto} stroke={M.humo} strokeWidth={8} />
    <g transform="translate(160 56) scale(3.2)">
      <rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" strokeWidth="1.6" />
      <circle cx="30" cy="26" r="9" fill="#4A2222" />
      <circle cx="30" cy="50" r="9" fill="#4A3C16" />
      <circle cx="30" cy="74" r="12" fill={M.verde} />
      <path d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke={M.sobreVerde} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" />
    </g>
  </svg>
);

export const FinalChecaPlay: React.FC = () => {
  const f = useCurrentFrame();

  // carretera: rayas que avanzan, se detienen mientras está el semáforo y vuelven a avanzar (bucle)
  const quieta = p(f, 6, 14) * (1 - p(f, 174, 16));
  const vel = (x: number) => 9 * (1 - p(x, 6, 14) + p(x, 174, 16));
  let avance = 0;
  for (let i = 1; i <= f; i++) avance += vel(i);
  const rayas = Array.from({ length: 9 }, (_, i) => ({ y: 900 + ((i * 74 + avance) % 666) }));

  // semáforo
  const sem = p(f, 8, 12) * (1 - p(f, 166, 12));
  const l = (a: number, b: number) => (f >= a && f < b ? 1 : 0);
  const rojo = l(18, 27), ambar = l(27, 36), verde = f >= 36 ? 1 : 0, paloma = f >= 42 ? 1 : 0;

  // pop-up que sale de la luz verde
  const abre = p(f, 56, 16) * (1 - p(f, 152, 14));
  const W = 700, H = 330;
  const cx = lerp(LUZ_V.x, 540, abre), cy = lerp(LUZ_V.y, 1180, abre);
  const w = lerp(36 * 2, W, abre), h = lerp(36 * 2, H, abre);
  const contenido = p(f, 68, 10) * (1 - p(f, 146, 6));
  const cuentas = p(f, 96, 12) * (1 - p(f, 146, 6));

  return (
    <AbsoluteFill style={{ background: M.asfalto }}>
      {/* cámara */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 864, background: "#1c1c22", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: M.texto, fontSize: 34, color: M.gris }}>[ Eduardo a cámara ]</div>
      <div style={{ position: "absolute", left: 0, top: 862, width: 1080, height: 4, background: M.humo }} />

      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {/* carretera */}
        {rayas.map((r, i) => (
          <rect key={i} x={532} y={r.y} width={16} height={44} rx={8} fill={M.amarillo} opacity={1 - 0.75 * quieta} />
        ))}
        {/* semáforo de la marca */}
        <g opacity={sem * (abre > 0.6 ? 0 : 1)} transform={`translate(${SX - 30 * ESC} ${SY - 50 * ESC + (1 - sem) * 40}) scale(${ESC})`}>
          <rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" strokeWidth="1.6" />
          <circle cx="30" cy="26" r="9" fill={rojo ? M.rojo : "#4A2222"} />
          <circle cx="30" cy="50" r="9" fill={ambar ? M.amarillo : "#4A3C16"} />
          <circle cx="30" cy="74" r="12" fill={verde ? M.verde : "#1d3a28"} />
          <path d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke={M.sobreVerde} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" opacity={paloma * (1 - abre)} />
        </g>
      </svg>

      {/* pop-up de Google Play, sale de la luz verde */}
      {abre > 0.001 ? (
        <div style={{ position: "absolute", left: cx - w / 2, top: cy - h / 2, width: w, height: h, boxSizing: "border-box", borderRadius: lerp(36, 28, abre), background: abre < 0.35 ? M.verde : M.tablero, border: `${Math.round(lerp(0, 6, abre))}px solid ${M.verde}`, boxShadow: `0 0 ${40 * abre}px rgba(34,197,94,0.35)`, overflow: "hidden" }}>
          <div style={{ position: "absolute", inset: 0, padding: "34px 36px", opacity: contenido, display: "flex", flexDirection: "column", gap: 18 }}>
            <div style={{ display: "flex", gap: 26, alignItems: "center" }}>
              <Icono tam={128} />
              <div style={{ fontFamily: M.titulo, fontWeight: 800, fontSize: 44, lineHeight: 1.08, color: M.hueso, whiteSpace: "nowrap" }}>
                Checa: Rentabilidad
                <br />
                de viajes
              </div>
            </div>
            <div style={{ alignSelf: "flex-start", background: M.verde, color: M.sobreVerde, fontFamily: M.datos, fontWeight: 700, fontSize: 30, letterSpacing: 2, padding: "10px 22px", borderRadius: 999 }}>YA EN GOOGLE PLAY</div>
          </div>
        </div>
      ) : null}
      <div style={{ position: "absolute", left: 0, right: 0, top: 1380, textAlign: "center", fontFamily: M.datos, fontWeight: 700, fontSize: 32, color: M.gris, opacity: cuentas }}>@checa.driver · @Eduardoalv9802</div>

      <Subtitulos f={f} />
    </AbsoluteFill>
  );
};
