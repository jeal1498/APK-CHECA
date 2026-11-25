import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { M, p, lerp } from "./marca";
import { FinalChecaPlay, DURACION_FINAL_CHECA, avanceFinal } from "./FinalPlay";

/**
 * Video completo del guion «001 Checa» (Hooks Virales) con el estilo «La raya» y las decisiones del 25 nov 2026
 * (marketing/videos/CHECA/Editor IA/Identidad/IDENTIDAD.md): la raya amarilla es el personaje y se transforma
 * (carretera → barra de $100 → regla de km → reloj → barra con la gasolina), el pop-up de Checa sale del semáforo,
 * la cámara se acerca en los golpes, subtítulos palabra por palabra al centro, sonidos oficiales y final con Play Store en bucle.
 * Cuentas (fórmulas de la app): $100, 15 km, 30 min → gasolina 15 ÷ 12 × $24 = $30 → netos $70 → $140/h y $4.67/km (metas $200/h y $10/km) → no conviene.
 * Tiempos de palabra estimados (produccion/palabras.json); con la voz de Eduardo se cambian.
 */
export const INICIO_FINAL = 879;
export const DURACION_COMPLETO_CHECA = INICIO_FINAL + DURACION_FINAL_CHECA; // 1074 = 35.8 s

const s = (seg: number) => Math.round(seg * 30);
/** Golpes (fotograma de la palabra clave). */
export const G = {
  viajes: s(2.17), menos: s(3.61), dinero: s(3.99), oferta: s(6.95), cobrar: s(9.82),
  km: s(11.95), tiempo: s(12.98), gasolina: s(13.88), checa: s(15.17), datos: s(16.27), decision: s(19.37),
  aceptarMas: s(21.94), elegir: s(24.21), vale: s(26.22), tambien: s(28.57),
};

type Palabra = [string, number, string?];
const R = M.rojo, A = M.amarillo, V = M.verde;
const SUBS: { de: number; a: number; lineas: Palabra[][] }[] = [
  { de: 0.2, a: 2.5, lineas: [[["¿Y", 0.25], ["SI", 0.47], ["TE", 0.73], ["DIJERA", 0.99]], [["QUE", 1.41], ["ALGUNOS", 1.71], ["VIAJES", 2.17]]] },
  { de: 2.5, a: 4.38, lineas: [[["TE", 2.59], ["DEJAN", 2.85], ["MUCHO", 3.23]], [["MENOS", 3.61, R], ["DINERO", 3.99, R]]] },
  { de: 4.38, a: 5.7, lineas: [[["DEL", 4.41], ["QUE", 4.71], ["PIENSAS?", 5.01]]] },
  { de: 5.72, a: 7.5, lineas: [[["CUANDO", 5.77], ["RECIBES", 6.19]], [["UNA", 6.65], ["OFERTA", 6.95, A]]] },
  { de: 7.5, a: 10.6, lineas: [[["NO", 7.56], ["BASTA", 7.82], ["CON", 8.2], ["MIRAR", 8.5]], [["CUÁNTO", 8.88], ["VAS", 9.3], ["A", 9.6], ["COBRAR", 9.82, A]]] },
  { de: 10.65, a: 12.65, lineas: [[["TAMBIÉN", 10.73], ["CUENTAN", 11.19]], [["LOS", 11.65], ["KILÓMETROS", 11.95, A]]] },
  { de: 12.65, a: 14.75, lineas: [[["EL", 12.72], ["TIEMPO", 12.98, A]], [["Y", 13.4], ["LA", 13.62], ["GASOLINA", 13.88, A]]] },
  { de: 14.8, a: 17.0, lineas: [[["CON", 14.87], ["CHECA", 15.17, V]], [["PUEDES", 15.55], ["VER", 15.97], ["DATOS", 16.27], ["CLAVE", 16.65]]] },
  { de: 17.0, a: 19.0, lineas: [[["DE", 17.03], ["CADA", 17.29], ["OFERTA", 17.63]], [["ANTES", 18.05], ["DE", 18.43], ["TOMAR", 18.69]]] },
  { de: 19.0, a: 20.3, lineas: [[["UNA", 19.07], ["DECISIÓN", 19.37, A]]] },
  { de: 20.3, a: 23.2, lineas: [[["PORQUE", 20.36], ["NO", 20.78], ["SE", 21.04], ["TRATA", 21.3]], [["DE", 21.68], ["ACEPTAR", 21.94], ["MÁS", 22.4, R], ["VIAJES", 22.7, R]]] },
  { de: 23.25, a: 25.3, lineas: [[["SE", 23.31], ["TRATA", 23.57], ["DE", 23.95]], [["ELEGIR", 24.21, V], ["MEJOR", 24.63, V]]] },
  { de: 25.4, a: 26.68, lineas: [[["TU", 25.5], ["TRABAJO", 25.76]], [["VALE", 26.22, V]]] },
  { de: 26.7, a: 29.25, lineas: [[["ASEGÚRATE", 26.75], ["DE", 27.29], ["QUE", 27.55]], [["CADA", 27.85], ["VIAJE", 28.19], ["TAMBIÉN", 28.57, V]]] },
];

const Subtitulos: React.FC<{ f: number }> = ({ f }) => {
  const b = SUBS.find((x) => f >= x.de * 30 && f < x.a * 30);
  if (!b) return null;
  const sale = 1 - p(f, b.a * 30 - 5, 5);
  return (
    <>
      {b.lineas.map((ws, fila) => (
        <div key={fila} style={{ position: "absolute", left: 60, right: 60, top: 864, transform: fila === 0 ? "translateY(calc(-100% - 8px))" : "translateY(8px)", textAlign: "center", whiteSpace: "nowrap", fontFamily: M.titulo, fontWeight: 800, fontSize: 78, lineHeight: 1.1, color: M.hueso, WebkitTextStroke: "16px #000", paintOrder: "stroke fill", opacity: sale }}>
          {ws.map(([t, seg, c], i) => {
            const q = p(f, seg * 30, 4.5);
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

// ── Formas de la raya (10 rayas) ──
type Forma = { x: number; y: number; w: number; h: number; r: number };
const A_FIN = avanceFinal(DURACION_FINAL_CHECA - 1); // dónde quedan las rayas al terminar el final (bucle)
const carretera = (i: number, avance: number): Forma => ({ x: 532, y: 900 + ((i * 74 + avance) % 666), w: 16, h: 44, r: 0 });
const barra = (y: number) => (i: number): Forma => ({ x: 135 + i * 82, y, w: 70, h: 40, r: 0 });
const regla = (i: number): Forma => ({ x: 128 + i * 88, y: 1250 - (i % 5 === 0 ? 40 : 0), w: 14, h: i % 5 === 0 ? 90 : 50, r: 0 });
const reloj = (i: number): Forma => {
  const a = (i / 10) * Math.PI * 2 - Math.PI / 2;
  return { x: 540 + 170 * Math.cos(a) - 20, y: 1220 + 170 * Math.sin(a) - 11, w: 40, h: 22, r: (i / 10) * 360 + 90 };
};
const mezcla = (a: Forma, b: Forma, q: number): Forma => ({ x: lerp(a.x, b.x, q), y: lerp(a.y, b.y, q), w: lerp(a.w, b.w, q), h: lerp(a.h, b.h, q), r: lerp(a.r, b.r, q) });

// Velocidad de la carretera: avanza al inicio, corre rápido en «aceptar más viajes» y vuelve al final.
const velocidad = (f: number) => {
  if (f < G.viajes) return 9;
  const rapido = p(f, G.aceptarMas - 6, 12) * (1 - p(f, G.elegir - 8, 12));
  return 26 * rapido;
};
const AV: number[] = [A_FIN];
for (let i = 1; i <= INICIO_FINAL; i++) AV[i] = AV[i - 1] + velocidad(i);

const Desarrollo: React.FC = () => {
  const f = useCurrentFrame();

  // cámara: acercamiento del 14 % en cada golpe
  const golpes = Object.values(G);
  const zoom = 1 + 0.14 * Math.max(...golpes.map((g) => Math.sin(Math.PI * p(f, g - 2, 14))));

  // ── la raya: de una forma a la siguiente ──
  const tramos: [number, (i: number) => Forma][] = [
    [0, (i) => carretera(i, AV[Math.min(f, G.viajes)])],
    [G.viajes - 4, barra(1180)],
    [G.km - 6, regla],
    [G.tiempo - 6, reloj],
    [G.gasolina - 6, barra(1180)],
    [G.checa - 4, barra(1420)],
    [G.aceptarMas - 8, (i) => carretera(i, AV[f])],
    [G.vale - 14, barra(1330)],
    [INICIO_FINAL - 34, (i) => carretera(i, 0)],
  ];
  let k = 0;
  for (let j = 0; j < tramos.length; j++) if (f >= tramos[j][0]) k = j;
  const q = k === 0 ? 1 : p(f, tramos[k][0], 14);
  const anterior = tramos[Math.max(0, k - 1)][1];
  const actual = tramos[k][1];

  // color de cada raya según la historia
  const rojoGas = (f >= G.menos - 1 && f < G.oferta - 10) || (f >= G.gasolina + 2 && f < G.aceptarMas - 8);
  const verdeQueda = (f >= G.dinero - 1 && f < G.oferta - 10) || (f >= G.gasolina + 10 && f < G.aceptarMas - 8);
  const todoVerde = f >= G.vale - 14 && f < INICIO_FINAL - 34;
  const cae = (f >= G.menos && f < G.oferta - 10 ? p(f, G.menos, 14) : 0) + (f >= G.gasolina + 6 && f < G.checa - 4 ? p(f, G.gasolina + 6, 14) : 0);
  const atenua = f >= G.checa && f < G.aceptarMas - 8 ? 0.45 : 1;

  // la décima raya solo existe como parte de la barra (el final y el inicio usan 9)
  const decima = p(f, G.viajes - 4, 10) * (1 - p(f, G.aceptarMas - 8, 10)) + p(f, G.vale - 14, 10) * (1 - p(f, INICIO_FINAL - 34, 10));

  // oferta de ejemplo (tarjeta hueso)
  const oferta = p(f, G.oferta - 4, 12) * (1 - p(f, G.km - 10, 10));
  const circulo = p(f, G.cobrar - 2, 14) * (1 - p(f, G.km - 10, 10));
  const etiqueta = (de: number, a: number) => p(f, de, 10) * (1 - p(f, a, 8));

  // semáforo de Checa y pop-up que sale de la luz roja
  const sem = p(f, G.checa - 4, 14) * (1 - p(f, G.vale - 16, 12));
  const SX = 540, STOP = 990, ESC = 2.2;
  const LUZ_R = { x: SX, y: STOP + 26 * ESC };
  const abre = p(f, G.datos - 2, 16) * (1 - p(f, 20.1 * 30, 12));
  const dentro = p(f, G.datos + 10, 10) * (1 - p(f, 20.0 * 30, 6));
  const pulso = 1 + 0.04 * Math.sin(Math.PI * p(f, G.decision, 10));
  const rojoLuz = f >= G.datos - 6 && f < 20.5 * 30;
  const verdeLuz = f >= G.elegir;

  return (
    <AbsoluteFill style={{ background: M.asfalto }}>
      {/* cámara */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 864, background: "#1c1c22", display: "flex", alignItems: "center", justifyContent: "center", fontFamily: M.texto, fontSize: 34, color: M.gris, overflow: "hidden" }}>
        <div style={{ transform: `scale(${zoom})` }}>[ Eduardo a cámara ]</div>
      </div>
      <div style={{ position: "absolute", left: 0, top: 862, width: 1080, height: 4, background: M.humo }} />

      {/* oferta de ejemplo */}
      <div style={{ position: "absolute", left: 270, top: 995, width: 540, height: 160, borderRadius: 28, background: M.hueso, opacity: oferta, transform: `translateY(${(1 - oferta) * 30}px)`, padding: "22px 34px", boxSizing: "border-box" }}>
        <div style={{ fontFamily: M.datos, fontWeight: 700, fontSize: 24, letterSpacing: 3, color: "#55555f" }}>OFERTA</div>
        <div style={{ display: "flex", alignItems: "baseline", gap: 24 }}>
          <span style={{ position: "relative", fontFamily: M.titulo, fontWeight: 800, fontSize: 92, lineHeight: 1, color: M.asfalto }}>
            $100
            <svg style={{ position: "absolute", left: -30, top: -18, overflow: "visible" }} width={290} height={130}>
              <ellipse cx={145} cy={62} rx={140} ry={60} fill="none" stroke={M.amarillo} strokeWidth={9} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - circulo} opacity={circulo > 0.01 ? 1 : 0} />
            </svg>
          </span>
          <span style={{ fontFamily: M.texto, fontWeight: 500, fontSize: 32, color: M.humo }}>15 km · 30 min</span>
        </div>
      </div>

      {/* la raya */}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {Array.from({ length: 10 }, (_, i) => {
          const a = anterior(i), b = actual(i);
          const r = mezcla(a, b, q);
          const esGas = i < 3;
          let color: string = M.amarillo;
          if (todoVerde) color = M.verde;
          else if (esGas && rojoGas) color = M.rojo;
          else if (!esGas && verdeQueda) color = M.verde;
          const dy = esGas ? 150 * cae : 0;
          const op = (i === 9 ? decima : 1) * (esGas ? 1 - 0.6 * cae : 1) * atenua;
          return <rect key={i} x={r.x} y={r.y + dy} width={r.w} height={r.h} rx={8} fill={color} opacity={op} transform={`rotate(${r.r} ${r.x + r.w / 2} ${r.y + dy + r.h / 2})`} />;
        })}
      </svg>

      {/* etiquetas de dato (≤ 3 palabras) */}
      {[
        { t: "$100", de: G.viajes + 6, a: G.menos - 2, top: 1100, c: M.hueso },
        { t: "−$30 GASOLINA", de: G.menos + 8, a: G.oferta - 12, top: 1370, c: M.rojo, chico: true },
        { t: "$70", de: G.dinero + 4, a: G.oferta - 12, top: 1240, c: M.verde },
        { t: "15 km", de: G.km + 4, a: G.tiempo - 6, top: 1370, c: M.hueso },
        { t: "30 min", de: G.tiempo + 4, a: G.gasolina - 6, top: 1420, c: M.hueso },
        { t: "−$30 GASOLINA", de: G.gasolina + 10, a: G.checa - 6, top: 1370, c: M.rojo, chico: true },
        { t: "$70", de: G.gasolina + 14, a: G.checa - 6, top: 1240, c: M.verde },
      ].map((e, i) => (
        <div key={i} style={{ position: "absolute", left: 60, right: 60, top: e.top, textAlign: e.chico ? "left" : "center", paddingLeft: e.chico ? 75 : 0, fontFamily: e.chico ? M.datos : M.titulo, fontWeight: 800, fontSize: e.chico ? 32 : 64, color: e.c, opacity: etiqueta(e.de, e.a) }}>
          {e.t}
        </div>
      ))}
      <div style={{ position: "absolute", left: 60, top: 1440, fontFamily: M.datos, fontWeight: 700, fontSize: 26, color: M.gris, opacity: 0.8 * etiqueta(G.gasolina + 20, G.checa - 6) }}>15 km ÷ 12 km/l × $24 = $30</div>

      {/* semáforo de Checa */}
      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        <g opacity={sem * (abre > 0.6 ? 0 : 1)} transform={`translate(${SX - 30 * ESC} ${STOP + (1 - sem) * 30}) scale(${ESC})`}>
          <rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" strokeWidth="1.6" />
          <circle cx="30" cy="26" r="9" fill={rojoLuz ? M.rojo : "#4A2222"} />
          <circle cx="30" cy="50" r="9" fill="#4A3C16" />
          <circle cx="30" cy="74" r="12" fill={verdeLuz ? M.verde : "#1d3a28"} />
          <path d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke={M.sobreVerde} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" opacity={verdeLuz ? 1 : 0} />
        </g>
      </svg>

      {/* pop-up de Checa: sale de la luz roja del semáforo */}
      {abre > 0.001 ? (
        <div style={{ position: "absolute", left: lerp(LUZ_R.x, 540, abre) - lerp(40, 600, abre) / 2, top: lerp(LUZ_R.y, 1190, abre) - lerp(40, 320, abre) / 2, width: lerp(40, 600, abre), height: lerp(40, 320, abre), boxSizing: "border-box", borderRadius: 28, background: abre < 0.4 ? M.rojo : M.tablero, border: `${Math.round(lerp(0, 7, abre))}px solid ${M.rojo}`, boxShadow: `0 0 ${36 * abre}px rgba(239,68,68,0.3)`, overflow: "hidden", transform: `scale(${pulso})` }}>
          <div style={{ position: "absolute", inset: 0, padding: "30px 38px", opacity: dentro }}>
            <div style={{ display: "inline-block", background: M.rojo, color: "#1F0000", fontFamily: M.datos, fontWeight: 700, fontSize: 28, padding: "8px 24px", borderRadius: 999 }}>NO CONVIENE</div>
            <div style={{ display: "flex", alignItems: "baseline", gap: 18, marginTop: 14 }}>
              <span style={{ fontFamily: M.titulo, fontWeight: 800, fontSize: 112, lineHeight: 1, color: M.hueso }}>$70</span>
              <span style={{ fontFamily: M.texto, fontWeight: 500, fontSize: 34, color: M.gris }}>netos</span>
            </div>
            <div style={{ fontFamily: M.datos, fontWeight: 700, fontSize: 34, color: M.hueso, marginTop: 18 }}>$140/h ✕ &nbsp; $4.67/km ✕</div>
          </div>
        </div>
      ) : null}

      <div style={{ position: "absolute", left: 60, top: 1530, fontFamily: M.datos, fontWeight: 700, fontSize: 26, letterSpacing: 3, color: M.gris, opacity: 1 - p(f, INICIO_FINAL - 30, 12) }}>NÚMEROS DE EJEMPLO</div>

      <Subtitulos f={f} />
    </AbsoluteFill>
  );
};

export const CompletoChecaRaya: React.FC = () => (
  <AbsoluteFill style={{ background: M.asfalto }}>
    <Sequence durationInFrames={INICIO_FINAL} layout="none">
      <Desarrollo />
    </Sequence>
    <Sequence from={INICIO_FINAL} layout="none">
      <FinalChecaPlay />
    </Sequence>
  </AbsoluteFill>
);

/** Efectos oficiales (fotograma, archivo de marca/sonido/mp3): un efecto por evento visual. */
export const SONIDOS: [number, string][] = [
  [G.menos, "rechazar"],
  [G.oferta - 4, "popup-entra"],
  [G.cobrar, "considerar"],
  [G.gasolina + 6, "rechazar"],
  [G.datos - 2, "popup-entra"],
  [G.elegir, "aceptar"],
  [INICIO_FINAL + 18, "logo-semaforo"],
  [INICIO_FINAL + 62, "popup-entra"],
];
