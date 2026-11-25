import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";
import { LowerThird } from "./components/LowerThird";
import { Logo } from "./components/Logo";
import { Linea } from "./components/Resaltado";

/**
 * Referencia 03 (PERSONAL), estilo AB: conductor a conductor, guion de Hooks Virales «000 Personal/001».
 * Elemento que persiste: la barra del viaje ($100). Pierde gotas, se llena con la gasolina, se compara con otra y da paso al globo.
 * Cuentas (gasolina = km totales ÷ 12 km/l × $24/l):
 *   A: $100, 15 km, 30 min → 15 ÷ 12 × 24 = $30 → te quedan $70 (barra de 960 px, 30 % naranja).
 *   B: $85,   5 km, 15 min →  5 ÷ 12 × 24 = $10 → te quedan $75 (barra de 816 px = 85 %, 96 px naranja).
 * Tiempos estimados (~2.8 palabras/s). Con la voz de Eduardo se cambian solo los fotogramas de P (Whisper).
 */
export const DURACION_REFERENCIA_03 = s(26);

/** Fotograma de cada palabra clave: todos los picos de movimiento cuelgan de aquí. */
const P = {
  perdiendo: 60,
  dejando: 165,
  oferta: 265,
  paga: 340,
  kilometros: 395,
  tiempo: 415,
  gasolina: 455,
  alto: 525,
  mas: 610,
  dato: 660,
  aceptar: 715,
};

const BLOQUES: Bloque[] = [
  B(0, 2.6, [t("¿Y si te dijera que estás")], [n("perdiendo"), t(" dinero")]),
  B(2.6, 4.6, [t("cada vez que aceptas")], [t("un "), n("viaje"), t("?")]),
  B(4.6, 6.4, [t("Puede que estés")], [n("dejando"), t(" dinero")]),
  B(6.4, 7.4, [t("sin darte "), n("cuenta")]),
  B(7.4, 9.2, [t("Cuando te llega")], [t("una "), n("oferta")]),
  B(9.2, 11.6, [t("no te fijes solo")], [t("en cuánto "), n("paga")]),
  B(11.6, 13.4, [t("Ten en cuenta")], [t("los "), n("kilómetros")]),
  B(13.4, 14.2, [t("el "), n("tiempo")]),
  B(14.2, 16.6, [t("y la "), n("gasolina")], [t("que vas a gastar")]),
  B(16.6, 18.8, [t("Un pago "), n("alto")], [t("no siempre significa")]),
  B(18.8, 21.2, [t("que te va a quedar")], [n("más dinero")]),
  B(21.2, 23.0, [t("¿Qué "), n("dato"), t(" tomas")], [t("en cuenta")]),
  B(23.0, 26, [t("antes de "), n("aceptar"), t("?")]),
];

const C = EA.color;
const lerp = (a: number, b: number, p: number) => a + (b - a) * p;
const sube = (p: number, d = 16): React.CSSProperties => ({ opacity: p, transform: `translateY(${(1 - p) * d}px)` });
const rotulo: React.CSSProperties = { position: "absolute", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, letterSpacing: 5, textTransform: "uppercase", color: C.negro };
const titulo: React.CSSProperties = { position: "absolute", left: 60, top: 850, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 104, lineHeight: 1, letterSpacing: -4, color: C.negro };
const cifra: React.CSSProperties = { display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 44 };
const GOTA = "M0 -40 q34 50 34 78 a34 34 0 0 1-68 0 q0-28 34-78z";
const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round", pathLength: 1, strokeDasharray: 1 } as const;

const W = 960;
const X = 60;

export const Referencia03: React.FC = () => {
  const f = useCurrentFrame();

  // Titular 1: «UN VIAJE DE $100» (cuenta de 0 a 100)
  const cien = Math.round(interpolate(f, [10, 32], [0, 100], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));
  const t1 = progreso(f, 4, 14) * (1 - progreso(f, P.alto - 12, 10));
  const subrayaPaga = progreso(f, P.paga - 4, 14) * (1 - progreso(f, P.kilometros - 20, 10));
  // Titular 2: «$100 O $85»
  const t2 = progreso(f, P.alto, 12) * (1 - progreso(f, P.dato - 28, 10));

  // Barra A: abajo (1190) y luego sube a comparar (1000)
  const subeA = progreso(f, P.alto - 2, 22);
  const ay = lerp(1190, 1010, subeA);
  const ah = lerp(90, 72, subeA);
  const pistaA = progreso(f, 36, 18);
  const llenaA = progreso(f, P.gasolina + 22, 16);
  const fuera = 1 - progreso(f, P.dato - 28, 10);

  // Iconos de una línea: km, tiempo, gasolina
  const icon = (p: number) => 1 - p;
  const fueraIconos = 1 - progreso(f, P.alto - 14, 10);
  const dCamino = progreso(f, P.kilometros - 6, 18);
  const dReloj = progreso(f, P.tiempo - 4, 16);
  const dBomba = progreso(f, P.gasolina - 18, 18);
  const IY = 985;

  // Gota de gasolina: de la manguera (x 903, y 1110) al tramo naranja de la barra
  const cae = progreso(f, P.gasolina + 4, 20);
  const gx = lerp(903, X + W * 0.15, cae);
  const gy = lerp(1110, 1205, cae) - 70 * Math.sin(Math.PI * cae);
  const gOp = progreso(f, P.gasolina, 6) * (1 - progreso(f, P.gasolina + 22, 6));

  // Gotas que se pierden (gancho y contexto)
  const fuga = (k: number, x: number) => {
    const p = progreso(f, k - 4, 18);
    const op = Math.min(1, p * 5) * (1 - progreso(f, k + 8, 10));
    return op > 0.001 ? <path key={k} d={GOTA} fill={C.naranja} opacity={op} transform={`translate(${x} ${1285 + 50 * p}) scale(0.7)`} /> : null;
  };

  // Barra B ($85)
  const pistaB = progreso(f, P.alto + 22, 16);
  const llenaB = progreso(f, P.alto + 38, 12);
  const BY = 1150;
  const WB = W * 0.85;
  const subrayaMas = progreso(f, P.mas - 2, 14);

  // Globo final
  const globo = progreso(f, P.dato - 16, 24);
  const pregunta = progreso(f, P.dato + 2, 12);

  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: C.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: C.negro }} />
      <LowerThird nombre="Eduardo Alvarado" rol="Conductor" x={EA.zona.lados} y={660} inicio={9} fin={80} />

      {/* Rótulo de ejemplo */}
      <div style={{ ...rotulo, fontSize: 24, left: 60, top: 818, opacity: 0.65 * progreso(f, P.oferta - 4, 12) * (1 - progreso(f, P.dato - 28, 10)) }}>Oferta · ejemplo</div>

      {/* Titulares */}
      {t1 > 0.001 ? (
        <div style={{ ...titulo, ...sube(t1, 20) }}>
          UN VIAJE DE{" "}
          <span style={{ position: "relative", color: C.naranja }}>
            ${cien}
            <span style={{ position: "absolute", left: 0, right: 0, bottom: -12, height: 10, background: C.negro, borderRadius: 4, transform: `scaleX(${subrayaPaga})`, transformOrigin: "left center", opacity: subrayaPaga > 0.001 ? 1 : 0 }} />
          </span>
        </div>
      ) : null}
      {t2 > 0.001 ? (
        <div style={{ ...titulo, ...sube(t2, 20) }}>
          $100 <span style={{ color: C.naranja }}>O</span> $85
        </div>
      ) : null}

      {/* Iconos de una línea */}
      <svg style={{ position: "absolute", left: 0, top: 0, opacity: fueraIconos }} width={1080} height={1920}>
        {/* camino */}
        <g transform={`translate(150 ${IY})`}>
          <path d="M60 0 L0 120 M140 0 L200 120" {...trazo} strokeDashoffset={icon(dCamino)} />
          <path d="M100 10 V30 M100 55 V75 M100 100 V120" {...trazo} stroke={C.naranja} strokeDashoffset={icon(progreso(f, P.kilometros + 6, 10))} />
        </g>
        {/* reloj */}
        <g transform={`translate(480 ${IY})`}>
          <circle cx={60} cy={60} r={56} {...trazo} strokeDashoffset={icon(dReloj)} />
          <path d="M60 60 V22 M60 60 L88 74" {...trazo} stroke={C.naranja} strokeDashoffset={icon(progreso(f, P.tiempo + 6, 10))} />
        </g>
        {/* bomba */}
        <g transform={`translate(700 ${IY})`}>
          <path d="M40 120 V24 q0-20 20-20 h80 q20 0 20 20 V120 M20 120 H180" {...trazo} strokeDashoffset={icon(dBomba)} />
          <rect x={62} y={28} width={76} height={44} rx={6} {...trazo} strokeDashoffset={icon(progreso(f, P.gasolina - 8, 12))} />
          <path d="M160 50 h18 q12 0 12 12 v50 q0 13 13 13 q13 0 13-13 V46 l-18-18" {...trazo} strokeDashoffset={icon(progreso(f, P.gasolina - 6, 12))} />
        </g>
      </svg>
      <div style={{ ...rotulo, fontSize: 26, left: 150, width: 200, textAlign: "center", top: IY + 132, opacity: progreso(f, P.kilometros + 4, 12) * fueraIconos }}>15 km</div>
      <div style={{ ...rotulo, fontSize: 26, left: 440, width: 200, textAlign: "center", top: IY + 132, opacity: progreso(f, P.tiempo + 4, 12) * fueraIconos }}>30 min</div>
      <div style={{ ...rotulo, fontSize: 26, left: 700, width: 200, textAlign: "center", top: IY + 132, color: C.naranja, opacity: progreso(f, P.gasolina, 12) * fueraIconos }}>Gasolina</div>

      {/* Barra A ($100) */}
      <div style={{ opacity: fuera }}>
        <div style={{ ...rotulo, fontSize: 26, left: X, top: ay - 40, opacity: progreso(f, P.alto + 12, 12) }}>$100 · 15 km</div>
        <div style={{ position: "absolute", left: X, top: ay, width: W, height: ah, boxSizing: "border-box", border: `5px solid ${C.negro}`, borderRadius: 8, transform: `scaleX(${pistaA})`, transformOrigin: "left center", opacity: pistaA }} />
        <div style={{ ...cifra, position: "absolute", left: X, top: ay, width: W * 0.3, height: ah, background: C.naranja, borderRadius: 8, transform: `scaleY(${llenaA})`, transformOrigin: "bottom center", color: C.negro }}>
          <span style={{ opacity: progreso(f, P.gasolina + 32, 10) }}>$30</span>
        </div>
        <div style={{ ...cifra, position: "absolute", left: X + W * 0.3, width: W * 0.7, top: ay, height: ah, color: C.negro, opacity: progreso(f, P.gasolina + 42, 12) }}>$70</div>
        <div style={{ ...rotulo, fontSize: 24, left: X, top: ay + ah + 14, color: C.naranja, opacity: progreso(f, P.gasolina + 40, 12) * fueraIconos }}>Gasolina</div>
        <div style={{ ...rotulo, fontSize: 24, left: X + W * 0.3 + 20, top: ay + ah + 14, opacity: 0.75 * progreso(f, P.gasolina + 46, 12) * fueraIconos }}>Te queda</div>
        <div style={{ position: "absolute", right: X, top: ay + ah + 10, fontFamily: EA.font.texto, fontSize: 28, color: C.negro, opacity: 0.65 * progreso(f, P.gasolina + 50, 12) * fueraIconos }}>15 km ÷ 12 km/l × $24 = $30</div>

        {/* Barra B ($85) */}
        <div style={{ ...rotulo, fontSize: 26, left: X, top: BY - 40, opacity: progreso(f, P.alto + 22, 12) }}>$85 · 5 km</div>
        <div style={{ position: "absolute", left: X, top: BY, width: WB, height: 72, boxSizing: "border-box", border: `5px solid ${C.negro}`, borderRadius: 8, transform: `scaleX(${pistaB})`, transformOrigin: "left center", opacity: pistaB }} />
        <div style={{ position: "absolute", left: X, top: BY, width: WB * (10 / 85), height: 72, background: C.naranja, borderRadius: 8, transform: `scaleY(${llenaB})`, transformOrigin: "bottom center" }} />
        <div style={{ ...cifra, position: "absolute", left: X + WB * (10 / 85), width: WB * (75 / 85), top: BY, height: 72, color: C.negro, opacity: progreso(f, P.alto + 48, 12) }}>
          <span style={{ position: "relative" }}>
            $75
            <span style={{ position: "absolute", left: -6, right: -6, bottom: -8, height: 10, background: C.naranja, borderRadius: 4, transform: `scaleX(${subrayaMas})`, transformOrigin: "left center" }} />
          </span>
        </div>
        <div style={{ position: "absolute", left: X, top: BY + 86, fontFamily: EA.font.texto, fontSize: 28, color: C.negro, opacity: 0.65 * progreso(f, P.alto + 54, 12) }}>5 km ÷ 12 km/l × $24 = $10</div>
      </div>

      {/* Gotas */}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {fuga(P.perdiendo, 330)}
        {fuga(P.dejando, 720)}
        {gOp > 0.001 ? <path d={GOTA} fill={C.naranja} opacity={gOp} transform={`translate(${gx} ${gy}) scale(${0.8 - 0.3 * cae})`} /> : null}
        {/* Globo de una línea */}
        <g opacity={globo > 0.001 ? 1 : 0}>
          <path d="M150 930 h780 q30 0 30 30 v180 q0 30-30 30 H300 l-70 60 v-60 H150 q-30 0-30-30 V960 q0-30 30-30z" {...trazo} strokeDashoffset={1 - globo} />
        </g>
      </svg>
      {pregunta > 0.001 ? (
        <div style={{ position: "absolute", left: 120, width: 840, top: 930, height: 240, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 80, letterSpacing: -2, color: C.negro, ...sube(pregunta, 12) }}>
          <Linea partes={[t("¿Qué "), n("dato"), t(" tomas?")]} frame={f} inicioResalte={P.dato + 8} durResalte={14} />
        </div>
      ) : null}

      <Logo x={472} y={1250} tam={96} inicio={P.aceptar - 10} />

      <Subtitles y={1390} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};
