import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";
import { Logo } from "./components/Logo";
import { Linea } from "./components/Resaltado";

/**
 * Referencia 03 (PERSONAL), estilo AB: conductor a conductor, guion de Hooks Virales «000 Personal/001».
 * Diseño bloque por bloque en Referencias/referencia-03-conductor/guion.md.
 * Elemento que persiste: la barra del viaje (negra = tu dinero, 960 px = $100). Gotea, se enfoca, la gasolina pinta el 30 %,
 * se compara con la de $85, suelta su tramo naranja y da paso al globo. El ✓ de «aceptar» abre y cierra.
 * Cuentas (gasolina = km totales ÷ 12 km/l × $24/l):
 *   A: $100, 15 km, 30 min → 15 ÷ 12 × 24 = $30 → te quedan $70 (barra de 960 px, 30 % naranja).
 *   B: $85,   5 km, 15 min →  5 ÷ 12 × 24 = $10 → te quedan $75 (barra de 816 px = 85 %, 96 px naranja).
 * Tiempos estimados (~2.8 palabras/s). Con la voz de Eduardo se cambian solo los fotogramas de P (Whisper).
 */
export const DURACION_REFERENCIA_03 = s(26);

/** Fotograma de cada palabra clave: todos los picos de movimiento cuelgan de aquí. */
const P = {
  perdiendo: 60,
  viaje: 132,
  dejando: 165,
  cuenta: 210,
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
const cifra: React.CSSProperties = { position: "absolute", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, letterSpacing: -1 };
const GOTA = "M0 -40 q34 50 34 78 a34 34 0 0 1-68 0 q0-28 34-78z";
const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round", pathLength: 1, strokeDasharray: 1 } as const;

const X = 60;
const W = 960;
const GAS = 288; // 30 % de 960
const WB = 816; // $85 = 85 % de 960
const GASB = 96; // $10 de $85 = 10/85 de 816
const GOTAS = [P.perdiendo, P.viaje, P.dejando, P.cuenta - 6];

export const Referencia03: React.FC = () => {
  const f = useCurrentFrame();
  const p = (inicio: number, dur = 12) => progreso(f, inicio, dur);

  // ── Barra A: posición (1–3 arriba, 4–8 abajo, 9–10 comparar)
  const dibujaA = p(8, 22);
  const baja = p(P.oferta - 8, 20);
  const subeA = p(P.alto - 2, 22);
  const ay = lerp(lerp(1000, 1180, baja), 990, subeA);
  const ah = lerp(110, 90, subeA);
  const pinta = p(P.gasolina + 20, 16); // la gasolina pinta el 30 %
  const cae = p(P.mas - 14, 16); // el tramo naranja cae (bloque 10)
  const alinea = p(P.mas - 2, 16); // lo negro se alinea a la izquierda
  const fueraBarras = 1 - p(P.dato - 28, 10);
  const op100 = p(10, 12) * (1 - p(P.gasolina + 10, 10));

  // ── Bloques 1–3: gotas, charco, ✓ y ojo
  const fueraInicio = 1 - p(P.oferta - 20, 10);
  const gotas = GOTAS.map((k) => p(k - 2, 14));
  const charco = gotas.reduce((a, g) => a + g, 0); // 0 a 4
  const sello = p(P.viaje - 10, 12);
  const paloma = p(P.viaje - 2, 8);
  const ojo = p(P.dejando - 12, 16);
  const cierra = p(P.cuenta - 4, 10);

  // ── Bloques 6–8: iconos de una línea arriba de la barra
  const IY = 930;
  const fueraIconos = 1 - p(P.alto - 14, 10);
  const gx = lerp(903, X + GAS / 2, p(P.gasolina + 4, 18));
  const gy = lerp(IY + 125, 1180 + 30, p(P.gasolina + 4, 18)) - 70 * Math.sin(Math.PI * p(P.gasolina + 4, 18));
  const gOp = p(P.gasolina, 6) * (1 - p(P.gasolina + 20, 6));

  // ── Barra B ($85)
  const by = 1140;
  const crecerB = p(P.alto + 22, 16);
  const pintaB = p(P.alto + 38, 12);

  // ── Globo y ✓ final
  const globo = p(P.dato - 16, 24);
  const pregunta = p(P.dato + 2, 12);
  const selloFinal = p(P.aceptar - 8, 12);

  const pieza = (x: number, y: number, w: number, h: number, fondo: string, radios: string, extra: React.CSSProperties = {}) => (
    <div style={{ position: "absolute", left: x, top: y, width: w, height: h, background: fondo, borderRadius: radios, ...extra }} />
  );

  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: C.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: C.negro }} />

      {/* 1–3 · ✓ de aceptar, ojo, gotas y charco */}
      <svg style={{ position: "absolute", left: 0, top: 0, opacity: fueraInicio }} width={1080} height={1920}>
        <circle cx={940} cy={890} r={46} {...trazo} strokeDashoffset={1 - sello} />
        <path d="M918 890 l16 16 l30 -32" {...trazo} stroke={C.naranja} strokeWidth={8} strokeDashoffset={1 - paloma} />
        <g transform={`translate(160 890) scale(1 ${lerp(1, 0.08, cierra)})`} opacity={ojo > 0.001 ? 1 : 0}>
          <path d="M-90 0 q90 -70 180 0 q-90 70 -180 0z" {...trazo} strokeDashoffset={1 - ojo} />
          <circle cx={0} cy={0} r={20} fill={C.negro} opacity={p(P.dejando, 8)} />
        </g>
        {charco > 0.01 ? <ellipse cx={540} cy={1262} rx={32 * charco} ry={9 + 3 * charco} fill={C.naranja} /> : null}
        {GOTAS.map((k, i) => {
          const g = gotas[i];
          const op = Math.min(1, g * 6) * (1 - p(k + 6, 6));
          return op > 0.001 ? <path key={k} d={GOTA} fill={C.naranja} opacity={op} transform={`translate(540 ${lerp(1150, 1232, g * g)}) scale(0.62)`} /> : null;
        })}
      </svg>
      <div style={{ ...rotulo, fontSize: 28, right: 1080 - 870, top: 874, opacity: p(P.viaje - 4, 12) * fueraInicio }}>Aceptar</div>

      {/* 4 · rótulo de ejemplo */}
      <div style={{ ...rotulo, fontSize: 26, left: X, top: 846, opacity: 0.7 * p(P.oferta - 2, 12) * fueraIconos }}>Oferta · ejemplo</div>

      {/* 6–8 · iconos de una línea */}
      <svg style={{ position: "absolute", left: 0, top: 0, opacity: fueraIconos }} width={1080} height={1920}>
        <g transform={`translate(150 ${IY})`}>
          <path d="M60 0 L0 120 M140 0 L200 120" {...trazo} strokeDashoffset={1 - p(P.kilometros - 8, 16)} />
          <path d="M100 8 V120" {...trazo} stroke={C.naranja} strokeDasharray="0.12 0.08" strokeDashoffset={-p(P.kilometros + 2, 30) * 0.6} opacity={p(P.kilometros, 6)} />
        </g>
        <g transform={`translate(480 ${IY})`}>
          <circle cx={60} cy={60} r={56} {...trazo} strokeDashoffset={1 - p(P.tiempo - 6, 14)} />
          <g transform={`rotate(${300 * p(P.tiempo + 2, 22)} 60 60)`} opacity={p(P.tiempo, 6)}>
            <path d="M60 60 V20" fill="none" stroke={C.naranja} strokeWidth={7} strokeLinecap="round" />
          </g>
        </g>
        <g transform={`translate(700 ${IY})`}>
          <path d="M40 120 V24 q0-20 20-20 h80 q20 0 20 20 V120 M20 120 H180" {...trazo} strokeDashoffset={1 - p(P.gasolina - 18, 16)} />
          <rect x={62} y={28} width={76} height={44} rx={6} {...trazo} strokeDashoffset={1 - p(P.gasolina - 8, 10)} />
          <path d="M160 50 h18 q12 0 12 12 v50 q0 13 13 13 q13 0 13-13 V46 l-18-18" {...trazo} strokeDashoffset={1 - p(P.gasolina - 6, 10)} />
        </g>
      </svg>
      <div style={{ ...rotulo, fontSize: 26, left: 150, width: 200, textAlign: "center", top: IY + 134, opacity: p(P.kilometros + 4) * fueraIconos }}>15 km</div>
      <div style={{ ...rotulo, fontSize: 26, left: 440, width: 200, textAlign: "center", top: IY + 134, opacity: p(P.tiempo + 4) * fueraIconos }}>30 min</div>
      <div style={{ ...rotulo, fontSize: 26, left: 700, width: 200, textAlign: "center", top: IY + 134, color: C.naranja, opacity: p(P.gasolina) * fueraIconos }}>Gasolina</div>

      {/* Barras */}
      <div style={{ position: "absolute", inset: 0, opacity: fueraBarras }}>
        {/* A: tramo de la gasolina (cae en el bloque 10) */}
        <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, clipPath: `inset(0 ${(1 - dibujaA) * 100}% 0 0)`, transform: `translateY(${70 * cae}px)`, opacity: 1 - cae }}>
          {pieza(X, ay, GAS, ah, C.negro, "8px 0 0 8px")}
          {pieza(X, ay, GAS, ah, C.naranja, "8px 0 0 8px", { transform: `scaleY(${pinta})`, transformOrigin: "top center" })}
          <div style={{ ...cifra, left: X, top: ay, width: GAS, height: ah, fontSize: 44, color: C.negro, opacity: p(P.gasolina + 30, 10) * (1 - p(P.alto - 12, 10)) }}>$30</div>
        </div>
        {/* A: lo que te queda */}
        <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, clipPath: `inset(0 ${(1 - dibujaA) * 100}% 0 0)`, transform: `translateX(${-GAS * alinea}px)` }}>
          {pieza(X + GAS, ay, W - GAS, ah, C.negro, alinea > 0.5 ? "8px" : "0 8px 8px 0")}
          <div style={{ ...cifra, left: X + GAS, top: ay, width: W - GAS, height: ah, fontSize: 44, color: C.marfil, opacity: p(P.gasolina + 40, 12) * (1 - p(P.alto - 12, 10)) }}>$70</div>
          <div style={{ ...cifra, left: X + GAS, top: ay, width: W - GAS, height: ah, fontSize: 44, color: C.marfil, opacity: p(P.mas + 6, 12) }}>$70</div>
        </div>
        {/* «$100» sobre la barra entera */}
        <div style={{ ...cifra, left: X, top: ay, width: W, height: ah, fontSize: 60, color: C.marfil, opacity: op100 }}>$100</div>
        <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
          <ellipse cx={540} cy={ay + ah / 2} rx={150} ry={74} fill="none" stroke={C.naranja} strokeWidth={9} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - p(P.paga - 4, 14)} opacity={p(P.paga - 4, 2) * (1 - p(P.kilometros - 20, 10))} />
          <line x1={X + W - GAS} y1={980} x2={X + W - GAS} y2={1240} stroke={C.naranja} strokeWidth={5} strokeDasharray="14 12" opacity={p(P.mas + 18, 12)} />
        </svg>
        <div style={{ ...rotulo, fontSize: 28, left: X, top: 950, opacity: p(P.alto + 10) }}>$100</div>
        <div style={{ position: "absolute", left: X, top: 1310, fontFamily: EA.font.texto, fontSize: 28, color: C.negro, opacity: 0.65 * p(P.gasolina + 50) * fueraIconos }}>15 km ÷ 12 km/l × $24 = $30</div>

        {/* B ($85) */}
        <div style={{ ...rotulo, fontSize: 28, left: X, top: by - 40, opacity: p(P.alto + 22) }}>$85</div>
        <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, transform: `translateY(${70 * cae}px)`, opacity: 1 - cae, clipPath: `inset(0 ${1080 - X - WB * crecerB}px 0 0)` }}>
          {pieza(X, by, GASB, 90, C.negro, "8px 0 0 8px")}
          {pieza(X, by, GASB, 90, C.naranja, "8px 0 0 8px", { transform: `scaleY(${pintaB})`, transformOrigin: "top center" })}
        </div>
        <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, transform: `translateX(${-GASB * alinea}px)` }}>
          <div style={{ position: "absolute", left: X, top: by, width: WB, height: 90, clipPath: `inset(0 ${WB * (1 - crecerB)}px 0 0)` }}>
            {pieza(GASB, 0, WB - GASB, 90, C.negro, alinea > 0.5 ? "8px" : "0 8px 8px 0")}
          </div>
          <div style={{ ...cifra, left: X + GASB, top: by, width: WB - GASB, height: 90, fontSize: 44, color: C.marfil, opacity: p(P.mas + 6, 12) }}>
            <span style={{ position: "relative" }}>
              $75
              <span style={{ position: "absolute", left: -6, right: -6, bottom: -8, height: 9, background: C.naranja, borderRadius: 4, transform: `scaleX(${p(P.mas + 14, 14)})`, transformOrigin: "left center" }} />
            </span>
          </div>
        </div>
      </div>

      {/* Gota de la bomba a la barra */}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {gOp > 0.001 ? <path d={GOTA} fill={C.naranja} opacity={gOp} transform={`translate(${gx} ${gy}) scale(0.75)`} /> : null}
        {/* 11–12 · globo y ✓ final */}
        <path d="M150 930 h780 q30 0 30 30 v180 q0 30-30 30 H300 l-70 60 v-60 H150 q-30 0-30-30 V960 q0-30 30-30z" {...trazo} strokeDashoffset={1 - globo} opacity={globo > 0.001 ? 1 : 0} />
        <g opacity={selloFinal} transform={`translate(930 930) scale(${0.7 + 0.3 * selloFinal})`}>
          <circle cx={0} cy={0} r={46} fill={C.naranja} stroke={C.negro} strokeWidth={6} />
          <path d="M-22 0 l16 16 l30 -32" fill="none" stroke={C.negro} strokeWidth={8} strokeLinecap="round" strokeLinejoin="round" />
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
