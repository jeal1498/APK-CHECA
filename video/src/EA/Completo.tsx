import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { SubtitulosPalabra } from "./components/SubtitulosPalabra";
import type { BloquePalabras } from "./components/SubtitulosPalabra";
import { Final } from "./Final";

/**
 * Video completo del guion 001 (PERSONAL) con la identidad cerrada el 25 nov 2026 (ver Identidad/IDENTIDAD.md §4):
 * estilo AB, subtítulos palabra por palabra al centro, movimiento continuo, pop-up que sale del objeto,
 * acercamiento de cámara en los golpes, transformaciones (un ícono que cambia de forma) y final en bucle (EA-Final).
 * Cuentas (gasolina = km ÷ 12 km/l × $24/l): $100, 15 km → $30, quedan $70. $85, 5 km → $10, quedan $75.
 * Tiempos estimados (~2.8 palabras/s). Con la voz de Eduardo se cambian los segundos de las palabras y P.
 */
export const INICIO_FINAL = 640;
export const DURACION_COMPLETO = INICIO_FINAL + 150; // 790 fotogramas = 26.3 s

/** Fotograma de cada golpe (palabra clave). */
export const P = {
  perdiendo: 57,
  aceptas: 102,
  viaje: 129,
  dejando: 162,
  cuenta: 206,
  oferta: 262,
  paga: 337,
  kilometros: 390,
  tiempo: 412,
  gasolina: 445,
  alto: 519,
  mas: 585,
};

const BLOQUES: BloquePalabras[] = [
  { de: 0, a: 2.6, lineas: [[["¿Y", 0.1], ["si", 0.32], ["te", 0.5], ["dijera", 0.68], ["que", 1.1], ["estás", 1.3]], [["perdiendo", 1.9, true], ["dinero", 2.25]]] },
  { de: 2.6, a: 4.6, lineas: [[["cada", 2.62], ["vez", 2.85], ["que", 3.05], ["aceptas", 3.4, true]], [["un", 3.95], ["viaje?", 4.3, true]]] },
  { de: 4.6, a: 6.3, lineas: [[["Puede", 4.62], ["que", 4.88], ["estés", 5.02]], [["dejando", 5.4, true], ["dinero", 5.8]]] },
  { de: 6.3, a: 7.4, lineas: [[["sin", 6.35], ["darte", 6.55], ["cuenta", 6.87, true]]] },
  { de: 7.4, a: 9.2, lineas: [[["Cuando", 7.45], ["te", 7.75], ["llega", 7.9]], [["una", 8.4], ["oferta", 8.73, true]]] },
  { de: 9.2, a: 11.6, lineas: [[["no", 9.25], ["te", 9.45], ["fijes", 9.6], ["solo", 10.0]], [["en", 10.5], ["cuánto", 10.7], ["paga", 11.23, true]]] },
  { de: 11.6, a: 13.4, lineas: [[["Ten", 11.65], ["en", 11.85], ["cuenta", 12.0]], [["los", 12.6], ["kilómetros", 13.0, true]]] },
  { de: 13.4, a: 14.2, lineas: [[["el", 13.45], ["tiempo", 13.73, true]]] },
  { de: 14.2, a: 16.6, lineas: [[["y", 14.25], ["la", 14.45], ["gasolina", 14.83, true]], [["que", 15.5], ["vas", 15.7], ["a", 15.85], ["gastar", 16.0]]] },
  { de: 16.6, a: 18.6, lineas: [[["Un", 16.65], ["pago", 16.85], ["alto", 17.3, true]], [["no", 17.7], ["siempre", 17.9], ["significa", 18.2]]] },
  { de: 18.6, a: 21.3, lineas: [[["que", 18.65], ["te", 18.8], ["va", 18.92], ["a", 19.04], ["quedar", 19.15]], [["más", 19.5, true], ["dinero", 19.85, true]]] },
];

const C = EA.color;
const lerp = (a: number, b: number, x: number) => a + (b - a) * x;
const cl = (x: number) => Math.min(1, Math.max(0, x));
const trazo = { fill: "none", stroke: C.negro, strokeWidth: 6, strokeLinecap: "round", strokeLinejoin: "round" } as const;
const dib = (x: number) => ({ pathLength: 1, strokeDasharray: 1, strokeDashoffset: 1 - x });

// Camino y auto
const velocidad = (f: number) => 7 + 9 * progreso(f, P.viaje - 6, 20);
const DIST: number[] = [0];
for (let i = 1; i <= INICIO_FINAL + 10; i++) DIST[i] = DIST[i - 1] + velocidad(i);
const dist = (f: number) => DIST[Math.max(0, Math.min(INICIO_FINAL + 10, Math.floor(f)))];
const SUELO = 1440;
const AUTO_X = 290;
const ESC = 1.25;
const COLA = AUTO_X + 6 * ESC;
const GOTAS = [P.perdiendo - 4, 76, 92, 108, 122, 134, 144, 153, 161, 168];

// Barra del viaje y barra de comparación
const BX = 60;
const BY = 960;
const BW = 960;
const BH = 100;
const GAS = 288; // 30 % de 960
const B2Y = 1110;
const B2W = 816; // $85 = 85 %
const B2GAS = 96; // $10 de $85

// Ícono que se transforma (encima del auto)
const SX = 540;
const SY = 1172;

const Desarrollo: React.FC = () => {
  const f = useCurrentFrame();
  const p = (inicio: number, dur = 12) => progreso(f, inicio, dur);

  // Cámara: acercamiento del 14 % en cada golpe
  const golpes = [P.perdiendo, P.aceptas, P.viaje, P.dejando, P.cuenta, P.oferta, P.paga, P.kilometros, P.tiempo, P.gasolina, P.alto, P.mas];
  const zoom = 1 + 0.14 * Math.max(...golpes.map((g) => Math.sin(Math.PI * p(g - 2, 14))));

  // Auto
  const bote = 2.5 * Math.sin(f / 3.2);
  const giro = dist(f) * 1.8;
  const inclina = -2.5 * Math.sin(Math.PI * p(P.viaje - 6, 16));
  const vel = p(P.viaje - 6, 16);

  // Barra: las gotas la pintan de naranja (3 % por gota, tope 30 % = la gasolina del ejemplo)
  const caidas = GOTAS.map((k) => p(k + 8, 8)).reduce((a, x) => a + x, 0);
  const perdido = Math.min(0.3, 0.03 * caidas);
  const sueltaTramo = p(P.mas - 12, 16) * (1 - p(INICIO_FINAL - 16, 14)); // el tramo naranja cae… y regresa antes del final
  const alinea = p(P.mas, 16) * (1 - p(INICIO_FINAL - 16, 14));
  const op100 = 1 - p(P.mas - 14, 8) + p(INICIO_FINAL - 8, 8);
  const quedan = p(P.mas + 8, 10) * (1 - p(INICIO_FINAL - 18, 8));
  const circulo = p(P.paga - 4, 14) * (1 - p(P.kilometros - 22, 10));

  // Barra de $85
  const b2 = p(P.alto + 6, 16) * (1 - p(INICIO_FINAL - 18, 10));
  const b2gas = p(P.alto + 20, 10);

  // Ícono que se transforma: ✓ → ojo → teléfono → camino → reloj → bomba
  const fases = [
    { de: P.aceptas - 4, a: P.dejando - 4 }, // ✓
    { de: P.dejando - 4, a: P.oferta - 6 }, // ojo
    { de: P.oferta - 6, a: P.kilometros - 4 }, // teléfono
    { de: P.kilometros - 4, a: P.tiempo - 4 }, // camino
    { de: P.tiempo - 4, a: P.gasolina - 4 }, // reloj
    { de: P.gasolina - 4, a: P.alto - 6 }, // bomba
  ];
  const vis = (i: number) => {
    const fa = fases[i];
    const entra = p(fa.de, 10);
    const sale = 1 - p(fa.a, 8);
    return Math.min(entra, sale);
  };
  const cierraOjo = p(P.cuenta - 3, 8);
  const ping = cl((f - P.oferta) / 18);

  // Pop-up «sale del objeto»: nace del tramo naranja de la barra
  const pop = p(P.gasolina + 2, 14) * (1 - p(P.alto - 16, 12));
  const popIn = p(P.gasolina + 12, 10) * (1 - p(P.alto - 20, 6));
  const card = {
    x: lerp(BX, 70, pop),
    y: lerp(BY, 1090, pop),
    w: lerp(GAS, 370, pop),
    h: lerp(BH, 170, pop),
  };

  // Chips de dato junto al ícono
  const chip = (de: number, a: number) => p(de + 4, 10) * (1 - p(a, 8));

  return (
    <AbsoluteFill style={{ background: C.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: C.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)", overflow: "hidden" }}>
        <div style={{ transform: `scale(${zoom})` }}>[ video de Eduardo a cámara ]</div>
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 4, background: C.negro }} />

      {/* Barra del viaje ($100) en dos piezas: lo que se va (naranja) y lo que te queda */}
      <div style={{ position: "absolute", left: BX, top: BY + 80 * sueltaTramo, width: GAS, height: BH, opacity: 1 - sueltaTramo }}>
        <div style={{ position: "absolute", inset: 0, background: C.negro, borderRadius: "10px 0 0 10px" }} />
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: Math.min(GAS, BW * perdido), background: C.naranja, borderRadius: "10px 0 0 10px" }} />
      </div>
      <div style={{ position: "absolute", left: BX + GAS - GAS * alinea, top: BY, width: BW - GAS, height: BH, background: C.negro, borderRadius: alinea > 0.5 ? 10 : "0 10px 10px 0" }}>
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 52, color: C.marfil, opacity: quedan }}>$70</div>
      </div>
      <div style={{ position: "absolute", left: BX, top: BY, width: BW, height: BH, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 62, letterSpacing: -2, color: C.marfil, opacity: Math.min(1, op100) }}>$100</div>

      {/* Barra de $85 */}
      {b2 > 0.001 ? (
        <>
          <div style={{ position: "absolute", left: BX, top: B2Y + 80 * sueltaTramo, width: B2GAS, height: 80, opacity: b2 * (1 - sueltaTramo), clipPath: `inset(0 ${(1 - b2) * 100}% 0 0)` }}>
            <div style={{ position: "absolute", inset: 0, background: C.negro, borderRadius: "10px 0 0 10px" }} />
            <div style={{ position: "absolute", inset: 0, background: C.naranja, borderRadius: "10px 0 0 10px", transform: `scaleY(${b2gas})`, transformOrigin: "top center" }} />
          </div>
          <div style={{ position: "absolute", left: BX + B2GAS - B2GAS * alinea, top: B2Y, width: (B2W - B2GAS) * b2, height: 80, background: C.negro, borderRadius: alinea > 0.5 ? 10 : "0 10px 10px 0", opacity: b2 }}>
            <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 52, color: C.marfil, opacity: quedan }}>
              <span style={{ position: "relative" }}>
                $75
                <span style={{ position: "absolute", left: -6, right: -6, bottom: -6, height: 9, background: C.naranja, borderRadius: 4, transform: `scaleX(${p(P.mas + 16, 12) * quedan})`, transformOrigin: "left center" }} />
              </span>
            </div>
          </div>
          <div style={{ position: "absolute", left: BX, top: B2Y - 40, fontFamily: EA.font.texto, fontWeight: 500, fontSize: 28, letterSpacing: 4, color: C.negro, opacity: b2 * (1 - p(P.mas - 8, 8)) }}>$85</div>
        </>
      ) : null}

      <svg style={{ position: "absolute", left: 0, top: 0 }} width={1080} height={1920}>
        {/* círculo «en cuánto paga» */}
        <ellipse cx={540} cy={BY + BH / 2} rx={150} ry={72} fill="none" stroke={C.naranja} strokeWidth={9} {...dib(circulo)} opacity={circulo > 0.001 ? 1 : 0} />
        {/* línea de comparación $70 | $75 */}
        <line x1={BX + BW - GAS} y1={BY - 14} x2={BX + BW - GAS} y2={B2Y + 94} stroke={C.naranja} strokeWidth={5} strokeDasharray="14 12" opacity={quedan * p(P.mas + 20, 10)} />

        {/* camino que avanza */}
        <line x1={0} y1={SUELO} x2={1080} y2={SUELO} stroke={C.negro} strokeWidth={6} strokeLinecap="round" />
        <line x1={0} y1={SUELO + 44} x2={1080} y2={SUELO + 44} stroke={C.negro} strokeWidth={6} strokeDasharray="60 50" strokeDashoffset={dist(f) % 110} />

        {/* gotas que se quedan en el camino */}
        {GOTAS.map((k) => {
          if (f < k) return null;
          const cae = p(k, 8);
          const enSuelo = f >= k + 8;
          const x = enSuelo ? COLA - (dist(f) - dist(k + 8)) : COLA;
          if (x < -40) return null;
          return enSuelo ? (
            <ellipse key={k} cx={x} cy={SUELO - 4} rx={lerp(10, 22, p(k + 8, 6))} ry={7} fill={C.naranja} />
          ) : (
            <path key={k} d="M0 -40 q34 50 34 78 a34 34 0 0 1-68 0 q0-28 34-78z" fill={C.naranja} transform={`translate(${x} ${lerp(SUELO - 80, SUELO - 8, cae * cae)}) scale(0.4)`} />
          );
        })}

        {/* auto */}
        <g transform={`translate(${AUTO_X} ${SUELO + bote * 0.4}) scale(${ESC}) rotate(${inclina} 200 0)`}>
          <g transform={`translate(0 ${bote})`}>
            <path d="M10 -70 L60 -70 L115 -130 L270 -130 L330 -70 L390 -70 Q400 -70 400 -60 L400 -44 L0 -44 L0 -60 Q0 -70 10 -70 Z" {...trazo} />
            <path d="M128 -118 L188 -118 L188 -78 L95 -78 Z M204 -118 L262 -118 L304 -78 L204 -78 Z" {...trazo} strokeWidth={5} />
          </g>
          {[90, 310].map((cx) => (
            <g key={cx} transform={`translate(${cx} -34) rotate(${giro})`}>
              <circle r={34} fill={C.marfil} stroke={C.negro} strokeWidth={6} />
              <line x1={-20} y1={0} x2={20} y2={0} stroke={C.naranja} strokeWidth={6} strokeLinecap="round" />
            </g>
          ))}
          {[0, 1, 2].map((i) => {
            const fase = ((f * 9 + i * 40) % 120) / 120;
            return <line key={i} x1={-30 - 120 * fase} y1={-110 + i * 26} x2={-30 - 120 * fase - 60} y2={-110 + i * 26} stroke={C.negro} strokeWidth={5} strokeLinecap="round" opacity={vel * (1 - fase)} />;
          })}
        </g>

        {/* ícono que se transforma, sobre el auto */}
        <g transform={`translate(${SX} ${SY + bote})`}>
          {/* ✓ */}
          <g opacity={vis(0)} transform={`scale(${0.6 + 0.4 * vis(0)})`}>
            <line x1={0} y1={44} x2={0} y2={96} stroke={C.negro} strokeWidth={5} strokeDasharray="8 8" />
            <circle r={44} fill={C.naranja} stroke={C.negro} strokeWidth={6} />
            <path d="M-20 0 l14 14 l28 -30" fill="none" stroke={C.negro} strokeWidth={8} strokeLinecap="round" strokeLinejoin="round" {...dib(p(P.aceptas + 6, 10))} />
          </g>
          {/* ojo que se cierra (sin darte cuenta) */}
          <g opacity={vis(1)} transform={`scale(${0.6 + 0.4 * vis(1)}) scale(1 ${lerp(1, 0.08, cierraOjo)})`}>
            <path d="M-70 0 q70 -56 140 0 q-70 56 -140 0z" {...trazo} />
            <circle r={17} fill={C.naranja} />
          </g>
          {/* teléfono con la oferta */}
          <g opacity={vis(2)} transform={`scale(${0.6 + 0.4 * vis(2)})`}>
            <circle r={40 + 70 * ping} fill="none" stroke={C.naranja} strokeWidth={5} opacity={(1 - ping) * (f >= P.oferta ? 1 : 0)} />
            <rect x={-40} y={-70} width={80} height={140} rx={14} {...trazo} fill={C.marfil} />
            <text x={0} y={10} textAnchor="middle" fontFamily={EA.font.titulo} fontWeight={800} fontSize={30} fill={C.negro}>
              $100
            </text>
            <line x1={-12} y1={52} x2={12} y2={52} stroke={C.naranja} strokeWidth={6} strokeLinecap="round" />
          </g>
          {/* camino */}
          <g opacity={vis(3)} transform={`scale(${0.6 + 0.4 * vis(3)})`}>
            <path d="M-25 -70 L-60 70 M25 -70 L60 70" {...trazo} />
            <path d="M0 -60 V70" fill="none" stroke={C.naranja} strokeWidth={6} strokeDasharray="18 14" strokeDashoffset={-f * 3} />
          </g>
          {/* reloj */}
          <g opacity={vis(4)} transform={`scale(${0.6 + 0.4 * vis(4)})`}>
            <circle r={62} {...trazo} fill={C.marfil} />
            <g transform={`rotate(${300 * p(P.tiempo, 24)})`}>
              <line x1={0} y1={0} x2={0} y2={-42} stroke={C.naranja} strokeWidth={7} strokeLinecap="round" />
            </g>
            <circle r={6} fill={C.negro} />
          </g>
          {/* bomba */}
          <g opacity={vis(5)} transform={`scale(${0.6 + 0.4 * vis(5)})`}>
            <path d="M-50 70 V-50 q0-16 16-16 h60 q16 0 16 16 V70 M-66 70 H58 M42 -30 h14 q10 0 10 10 v46 q0 12 12 12" {...trazo} />
            <rect x={-30} y={-48} width={52} height={32} rx={5} fill={C.naranja} stroke={C.negro} strokeWidth={5} />
          </g>
        </g>
      </svg>

      {/* chips de dato junto al ícono (≤ 2 palabras) */}
      {[
        { t: "15 km", de: P.kilometros, a: P.tiempo - 6 },
        { t: "30 min", de: P.tiempo, a: P.gasolina - 6 },
      ].map((c) => (
        <div key={c.t} style={{ position: "absolute", left: SX + 90, top: SY - 30 + bote, fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 48, color: C.negro, opacity: chip(c.de, c.a), transform: `translateX(${(1 - chip(c.de, c.a)) * -20}px)` }}>
          {c.t}
        </div>
      ))}

      {/* Pop-up «sale del objeto»: nace del tramo naranja (gasolina) y regresa */}
      {pop > 0.001 ? (
        <div style={{ position: "absolute", left: card.x, top: card.y, width: card.w, height: card.h, boxSizing: "border-box", background: popIn > 0.5 || pop > 0.6 ? C.blanco : C.naranja, border: `${Math.round(6 * pop)}px solid ${C.negro}`, borderRadius: lerp(10, 16, pop), padding: "18px 26px", overflow: "hidden" }}>
          <div style={{ opacity: popIn }}>
            <div style={{ fontFamily: EA.font.texto, fontWeight: 500, fontSize: 26, letterSpacing: 4, color: C.negro, opacity: 0.7 }}>GASOLINA</div>
            <div style={{ fontFamily: EA.font.titulo, fontWeight: 800, fontSize: 72, lineHeight: 1, letterSpacing: -2, color: C.negro }}>
              <span style={{ color: C.naranja }}>−</span>$30
            </div>
            <div style={{ fontFamily: EA.font.texto, fontSize: 24, color: C.negro, opacity: 0.7, marginTop: 6 }}>15 km ÷ 12 km/l × $24</div>
          </div>
        </div>
      ) : null}

      <SubtitulosPalabra y={800} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};

export const Completo: React.FC = () => (
  <AbsoluteFill style={{ background: C.marfil }}>
    <Sequence durationInFrames={INICIO_FINAL} layout="none">
      <Desarrollo />
    </Sequence>
    <Sequence from={INICIO_FINAL} layout="none">
      <Final distInicial={DIST[INICIO_FINAL]} desfase={INICIO_FINAL} />
    </Sequence>
  </AbsoluteFill>
);

