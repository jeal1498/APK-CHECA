import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo, Popup } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasComision";

// Tema 6 «Tarifas bajas y comisión alta» (elegir mejores viajes), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_COMISION_1VOLT = duracion;
const en = crearEn(LINEAS, esc);

const K = { fondo: "#05060F", linea: "#A78BFA", relleno: "#120F2A", cian: "#22D3EE", magenta: "#D946EF", amarillo: "#FBBF24", verde: "#4ADE80", naranja: "#FB923C", azul: "#60A5FA", rosa: "#FB7185", texto: "#E9E7FF", tenue: "#8B84C9" };
// Cada escena tiene su color de trazo y un tinte de fondo distinto
const TONO = [
  { linea: "#FB923C", fondo: "#2A1206" },
  { linea: "#FB7185", fondo: "#2A0A14" },
  { linea: "#60A5FA", fondo: "#07193A" },
  { linea: "#A78BFA", fondo: "#170D36" },
  { linea: "#4ADE80", fondo: "#06281A" },
  { linea: "#FBBF24", fondo: "#2A1E04" },
  { linea: "#F43F5E", fondo: "#2E0610" },
  { linea: "#D946EF", fondo: "#260A30" },
  { linea: "#22D3EE", fondo: "#04242E" },
];
const MONO = "FiraCode";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ap = (f: number, d: number, dur = 12) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });

const escena = (f: number) => {
  const i = esc.findIndex((e) => f < e.hasta);
  return i < 0 ? esc.length - 1 : i;
};

// Trazo que se dibuja solo
const Trazo: React.FC<{ d: string; p: number; color?: string; w?: number; relleno?: string }> = ({ d, p, color, w = 5, relleno = "none" }) => {
  const frame = useCurrentFrame();
  const c = color ?? TONO[escena(frame) % TONO.length].linea;
  return (
  <path d={d} pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - p} stroke={c} strokeWidth={w} fill={p >= 1 ? (relleno === K.relleno ? `${c}22` : relleno) : "none"} strokeLinecap="round" strokeLinejoin="round" style={{ filter: `drop-shadow(0 0 8px ${c}88)` }} />
  );
};

// Línea de cota horizontal (punteada o sólida) que crece de izquierda a derecha
const Cota: React.FC<{ x1: number; x2: number; y: number; p: number; color: string; punteada?: boolean }> = ({ x1, x2, y, p, color, punteada }) => (
  <>
    <line x1={x1} y1={y} x2={x1 + (x2 - x1) * p} y2={y} stroke={color} strokeWidth={8} strokeDasharray={punteada ? "22 16" : undefined} strokeLinecap="round" style={{ filter: `drop-shadow(0 0 10px ${color})` }} />
    {p > 0 ? <line x1={x1} y1={y - 22} x2={x1} y2={y + 22} stroke={color} strokeWidth={5} /> : null}
    {p >= 1 ? <line x1={x2} y1={y - 22} x2={x2} y2={y + 22} stroke={color} strokeWidth={5} /> : null}
  </>
);

const Numero: React.FC<{ texto: string; color: string; tam?: number; top: number; a: number; tachar?: number }> = ({ texto, color, tam = 190, top, a, tachar = 0 }) => (
  <div style={{ position: "absolute", top, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: a, transform: `translateY(${(1 - a) * 30}px)` }}>
    <div style={{ position: "relative", fontFamily: MONO, fontWeight: 700, fontSize: tam, lineHeight: 1, color, textShadow: `0 0 40px ${color}55` }}>
      {texto}
      {tachar > 0 ? <div style={{ position: "absolute", left: -12, top: "50%", height: 12, width: `calc(${tachar * 100}% + 24px)`, background: K.magenta, boxShadow: `0 0 16px ${K.magenta}` }} /> : null}
    </div>
  </div>
);

const Etiqueta: React.FC<{ texto: string; top: number; a: number; color?: string }> = ({ texto, top, a, color = K.tenue }) => (
  <div style={{ position: "absolute", top, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontSize: 38, color, opacity: a }}>{texto}</div>
);

// Frase clave en MAYÚSCULAS espaciadas + línea chica que la explica
const Frase: React.FC<{ texto: string; sub?: string; desde: number; color?: string }> = ({ texto, sub, desde, color = K.texto }) => {
  const frame = useCurrentFrame();
  const a = ap(frame, desde, 10);
  return (
    <div style={{ position: "absolute", top: 1270, left: 60, right: 60, textAlign: "center", opacity: a }}>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 56, letterSpacing: 10, color }}>{texto}</div>
      {sub ? <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, marginTop: 18 }}>{sub}</div> : null}
    </div>
  );
};




const BILLETE = "M220 640 L860 640 L860 1000 L220 1000 Z M540 820 m-90 0 a90 90 0 1 0 180 0 a90 90 0 1 0 -180 0";
const MITAD_IZQ = "M220 640 L560 640 L520 700 L570 760 L510 830 L560 900 L520 1000 L220 1000 Z";
const MITAD_DER = "M560 640 L860 640 L860 1000 L520 1000 L560 900 L510 830 L570 760 L520 700 Z";
const CANDADO = "M400 760 L680 760 Q700 760 700 780 L700 1040 Q700 1060 680 1060 L400 1060 Q380 1060 380 1040 L380 780 Q380 760 400 760 Z M440 760 L440 680 Q440 580 540 580 Q640 580 640 680 L640 760";

const PopA = (ancho: number) => (
  <Popup ancho={ancho} nivel="amarillo" app="App" exacto neta="72.00" viaje="100.00" gas="28.00"
    fichas={[{ v: "$188/h", l: "meta $150", c: "ok" }, { v: "$7.14/km", l: "meta $8", c: "mal" }, { v: "32 min", l: "14.0 km total", c: "neutro" }]} />
);
const PopB = (ancho: number) => (
  <Popup ancho={ancho} nivel="verde" app="App" exacto oro neta="84.00" viaje="100.00" gas="16.00"
    fichas={[{ v: "$300/h", l: "meta $150", c: "ok" }, { v: "$12.50/km", l: "meta $8", c: "ok" }, { v: "20 min", l: "8.0 km total", c: "neutro" }]} />
);

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    const s = p(en(0, "comisionaza"), 20);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          {s === 0 ? <Trazo d={BILLETE} p={p(d0, 14)} color={K.verde} relleno={K.relleno} /> : (
            <>
              <path d={MITAD_IZQ} fill={`${K.verde}22`} stroke={K.verde} strokeWidth={5} />
              <g transform={`translate(${s * 260} ${-s * 380}) rotate(${s * 25} 700 820)`} opacity={1 - s * 0.7}>
                <path d={MITAD_DER} fill={`${K.rosa}22`} stroke={K.rosa} strokeWidth={5} />
              </g>
            </>
          )}
        </svg>
        <div style={{ position: "absolute", top: 780, left: 220, width: 300, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 90, color: K.verde, opacity: p(d0 + 8, 8) }}>$</div>
        <Frase texto="COMISIÓN" sub="y las tarifas no suben" desde={en(0, "comisionaza")} color={K.rosa} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={CANDADO} p={p(d0, 16)} color={K.rosa} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 880, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 52, color: K.rosa, opacity: p(d0 + 6, 8) }}>comisión</div>
        <Frase texto="NO LA CAMBIAS" desde={en(1, "cambiar...")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    const sel = en(2, "decides");
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontSize: 40, color: K.tenue, opacity: p(d0, 8) }}>tus viajes</div>
        {["$100 · 14 km", "$100 · 8 km", "$80 · 6 km"].map((t, k) => {
          const on = k === 1 && frame >= sel;
          const c = on ? K.verde : K.azul;
          return (
            <div key={t} style={{ position: "absolute", top: 480 + k * 150, left: 200, right: 200, height: 110, borderRadius: 20, border: `4px solid ${c}`, background: on ? `${K.verde}22` : "transparent", display: "flex", alignItems: "center", gap: 30, padding: "0 34px", fontFamily: MONO, fontSize: 44, color: c, opacity: p(d0 + k * 4, 8), boxShadow: on ? `0 0 26px ${K.verde}66` : undefined }}>
              <div style={{ width: 44, height: 44, borderRadius: 10, border: `4px solid ${c}`, background: on ? c : "transparent" }} />
              {t}
            </div>
          );
        })}
        <Frase texto="TÚ DECIDES" sub="qué viajes aceptas" desde={en(2, "decides")} color={K.verde} />
      </>
    );
  }
  if (i === 3) {
    const tel = (x: number, d: number) => (
      <div style={{ position: "absolute", top: 380, left: x, width: 340, height: 620, borderRadius: 50, border: `5px solid ${TONO[3].linea}`, background: `${TONO[3].linea}14`, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", fontFamily: MONO, opacity: ap(frame, d, 8) }}>
        <div style={{ fontSize: 26, color: K.tenue }}>nueva oferta</div>
        <div style={{ fontSize: 100, fontWeight: 700, color: K.amarillo }}>$100</div>
      </div>
    );
    return (
      <>
        {tel(150, d0)}
        {tel(590, d0 + 6)}
        <Etiqueta texto="números de ejemplo" top={1060} a={p(en(3, "cien"), 10)} />
        <Frase texto="DOS OFERTAS" sub="misma tarifa" desde={en(3, "dos")} color={K.amarillo} />
      </>
    );
  }
  if (i === 4 || i === 5) {
    const x2a = 140 + (14 / 14) * 800;
    const x2b = 140 + (8 / 14) * 800;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Cota x1={140} x2={x2a} y={620} p={i === 4 ? p(en(4, "catorce"), 18) : 1} color={K.magenta} />
          {i === 5 ? <Cota x1={140} x2={x2b} y={900} p={p(en(5, "ocho"), 14)} color={K.cian} /> : null}
        </svg>
        <div style={{ position: "absolute", top: 480, left: 140, fontFamily: MONO, fontWeight: 700, fontSize: 70, color: K.magenta, opacity: p(en(4, "catorce") - (i === 5 ? 99 : 0), 8) }}>A · 14 km</div>
        {i === 5 ? <div style={{ position: "absolute", top: 760, left: 140, fontFamily: MONO, fontWeight: 700, fontSize: 70, color: K.cian, opacity: p(en(5, "ocho"), 8) }}>B · 8 km</div> : null}
        <Frase texto={i === 4 ? "KM TOTALES" : "SOLO 8 KM"} sub="con la recogida" desde={i === 4 ? en(4, "catorce") : en(5, "ocho")} color={i === 4 ? K.magenta : K.cian} />
      </>
    );
  }
  if (i === 6) {
    const col = (t: string, v: string, nota: string, c: string, d: number, x: number) => (
      <div style={{ position: "absolute", top: 420, left: x, width: 420, textAlign: "center", fontFamily: MONO, opacity: ap(frame, d, 8) }}>
        <div style={{ fontSize: 50, color: K.tenue }}>{t}</div>
        <div style={{ marginTop: 24, fontSize: 130, fontWeight: 700, color: c, border: `5px solid ${c}`, borderRadius: 28, padding: "26px 0", boxShadow: `0 0 30px ${c}55` }}>{v}</div>
        <div style={{ marginTop: 20, fontSize: 32, color: K.tenue }}>{nota}</div>
      </div>
    );
    return (
      <>
        {col("A · 14 km", "$72", "$100 − $28 gas", K.amarillo, en(6, "setenta"), 90)}
        {col("B · 8 km", "$84", "$100 − $16 gas", K.verde, en(6, "ochenta"), 570)}
        <Frase texto="LO QUE TE QUEDA" desde={en(6, "quedan")} color={K.verde} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <Numero texto="+$12" color={K.verde} tam={260} top={560} a={p(d0, 8)} />
        <Etiqueta texto="por viaje · misma tarifa" top={860} a={p(en(7, "diferencia"), 10)} color={K.verde} />
        <Frase texto="DE DIFERENCIA" desde={en(7, "diferencia")} color={K.verde} />
      </>
    );
  }
  if (i === 8) {
    const n = Math.min(20, Math.max(0, Math.floor((frame - d0 - 2) / 3)));
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          {Array.from({ length: 20 }).map((_, k) => {
            const on = k < n;
            return <circle key={k} cx={240 + (k % 5) * 150} cy={520 + Math.floor(k / 5) * 150} r={50} fill={on ? K.verde : K.relleno} stroke={on ? K.verde : TONO[8].linea} strokeWidth={5} style={{ filter: on ? `drop-shadow(0 0 10px ${K.verde})` : undefined }} />;
          })}
        </svg>
        <Numero texto={`$${n * 12}`} color={K.verde} tam={150} top={240} a={p(d0, 6)} />
        <Frase texto="MÁS EN TU BOLSA" sub="20 viajes × $12" desde={en(8, "doscientos")} color={K.verde} />
      </>
    );
  }
  if (i === 9) {
    return (
      <>
        <div style={{ position: "absolute", top: 520, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 18, alignItems: "flex-start" }}>
          <div style={{ opacity: p(d0, 8) }}>{PopA(465)}</div>
          <div style={{ opacity: p(d0 + 6, 8), transform: `scale(${1 + 0.05 * p(en(9, "conviene"), 6)})` }}>{PopB(465)}</div>
        </div>
        <Frase texto="⭐ LA QUE CONVIENE" sub="antes de aceptar" desde={en(9, "conviene")} color="#E8B84A" />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(10, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días de prueba sin costo</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(10, "link"), 8) }}>link en el perfil ↓</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  comisionaza: K.rosa, comisión: K.rosa, decides: K.verde, cien: K.amarillo, catorce: K.magenta, ocho: K.cian, setenta: K.amarillo, ochenta: K.verde, cuatro: K.verde,
  doce: K.verde, doscientos: K.verde, cuarenta: K.verde, bolsa: K.verde, checa: K.verde, conviene: "#E8B84A", siete: K.verde, días: K.verde, prueba: K.verde,
};
const limpia = (w: string) => w.toLowerCase().replace(/[^a-záéíóúñ]/g, "");

// Subtítulos para redes: 1–3 palabras, Inter negrita con contorno, palabras clave a color
const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const k = escena(frame);
  const l = LINEAS[k];
  const t = (frame - esc[k].voz) / 30;
  if (t < -0.1 || t > l.dur + 0.3) return null;
  const grupos: [number, number][] = [];
  let ini = 0;
  l.palabras.forEach((p, j) => {
    if (/[.!?,:]$/.test(p.w) || j - ini === 2 || j === l.palabras.length - 1) {
      grupos.push([ini, j]);
      ini = j + 1;
    }
  });
  const g = [...grupos].reverse().find(([a]) => t >= l.palabras[a].s - 0.05) ?? grupos[0];
  const inicio = esc[k].voz + Math.round(l.palabras[g[0]].s * 30);
  const s = 1.15 - 0.15 * ap(frame, inicio - 1, 5);
  return (
    <div style={{ position: "absolute", top: 1500, left: 40, right: 40, textAlign: "center", fontFamily: "Inter", fontWeight: 800, fontSize: 70, lineHeight: 1.1, transform: `scale(${s})`, WebkitTextStroke: "3px #000", paintOrder: "stroke fill", textShadow: "0 5px 0 #000, 0 10px 24px rgba(0,0,0,0.9)" }}>
      {l.palabras.slice(g[0], g[1] + 1).map((p, j) => (
        <span key={j} style={{ color: CLAVE[limpia(p.w)] ?? "#FFFFFF" }}>
          {p.w}{" "}
        </span>
      ))}
    </div>
  );
};

const Fondo: React.FC = () => {
  const frame = useCurrentFrame();
  const k = escena(frame);
  const a = ap(frame, esc[k].desde, 8);
  const prev = TONO[Math.max(0, k - 1) % TONO.length].fondo;
  return (
    <>
      <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 40%, ${prev} 0%, ${K.fondo} 75%)` }} />
      <AbsoluteFill style={{ opacity: a, background: `radial-gradient(ellipse at 50% 40%, ${TONO[k % TONO.length].fondo} 0%, ${K.fondo} 75%)` }} />
    </>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol: number }> = ({ en: e, src, vol }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

export const Comision1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`comision/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(0, "comisionaza")} src="whoosh.wav" vol={0.1} />
    <Sfx en={en(2, "decides")} src="pop.mp3" vol={0.12} />
    <Sfx en={esc[7].desde} src="ding.wav" vol={0.13} />
    <Sfx en={en(9, "conviene")} src="ding.wav" vol={0.12} />
    <Sfx en={esc[10].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
