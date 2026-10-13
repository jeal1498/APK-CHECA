import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo, Popup } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasRechazar";

// Tema 7 «Miedo a rechazar» (calificación / tasa de aceptación), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_RECHAZAR_1VOLT = duracion;
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




const ROJO = "#F43F5E";
const TELEFONO = "M360 360 Q360 310 410 310 L670 310 Q720 310 720 360 L720 1110 Q720 1160 670 1160 L410 1160 Q360 1160 360 1110 Z";
const LIBRETA = "M540 520 L540 1080 M540 520 Q420 470 260 500 L260 1060 Q420 1030 540 1080 Q660 1030 820 1060 L820 500 Q660 470 540 520";

const PopRojo = (ancho: number) => (
  <Popup ancho={ancho} nivel="rojo" app="App" exacto neta="44.00" viaje="70.00" gas="26.00"
    fichas={[{ v: "$140/h", l: "meta $150", c: "mal" }, { v: "$5.38/km", l: "meta $8", c: "mal" }, { v: "30 min", l: "13.0 km total", c: "neutro" }]} />
);
const PopVerde = (ancho: number) => (
  <Popup ancho={ancho} nivel="verde" app="App" exacto neta="96.00" viaje="120.00" gas="24.00"
    fichas={[{ v: "$288/h", l: "meta $150", c: "ok" }, { v: "$10.00/km", l: "meta $8", c: "ok" }, { v: "25 min", l: "12.0 km total", c: "neutro" }]} />
);

const Boton: React.FC<{ texto: string; c: string; x: number; y: number; on?: boolean }> = ({ texto, c, x, y, on }) => (
  <div style={{ position: "absolute", left: x, top: y, width: 360, height: 130, borderRadius: 26, border: `5px solid ${c}`, background: on ? `${c}44` : `${c}11`, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 46, color: c, boxShadow: on ? `0 0 30px ${c}88` : undefined }}>{texto}</div>
);

const Lista: React.FC<{ d0: number; rojos: number }> = ({ d0, rojos }) => {
  const frame = useCurrentFrame();
  const malos = [1, 4, 6];
  return (
    <>
      {Array.from({ length: 8 }).map((_, k) => {
        const malo = malos.indexOf(k) >= 0 && frame >= rojos + malos.indexOf(k) * 5;
        const c = malo ? ROJO : K.verde;
        return (
          <div key={k} style={{ position: "absolute", top: 300 + k * 110, left: 160, right: 160, height: 84, borderRadius: 16, border: `4px solid ${c}`, background: `${c}${malo ? "33" : "14"}`, display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 30px", fontFamily: MONO, fontSize: 38, color: c, opacity: ap(frame, d0 + k * 2, 6), boxShadow: malo ? `0 0 20px ${c}66` : undefined }}>
            <span>viaje {k + 1}</span>
            <span style={{ fontWeight: 700 }}>{malo ? "pierdes" : "aceptado"}</span>
          </div>
        );
      })}
    </>
  );
};

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0 || i === 1) {
    const x = 540 + Math.sin((frame - esc[0].desde) / 7) * 220;
    return (
      <>
        <Boton texto="Aceptar" c={K.verde} x={130} y={720} on={x < 450} />
        <Boton texto="Rechazar" c={ROJO} x={590} y={720} on={x > 630} />
        <div style={{ position: "absolute", left: x - 40, top: 900, width: 80, height: 80, borderRadius: 40, border: `5px solid ${K.texto}`, background: `${K.texto}22` }} />
        {i === 1 ? (
          <svg width={1080} height={1920} style={{ position: "absolute" }}>
            <Trazo d="M540 340 L700 620 L380 620 Z" p={p(d0, 12)} color={K.rosa} relleno={K.relleno} />
            <text x={540} y={590} textAnchor="middle" fontFamily={MONO} fontWeight={700} fontSize={110} fill={K.rosa} opacity={p(d0 + 8, 6)}>!</text>
          </svg>
        ) : null}
        <Frase texto={i === 0 ? "MIEDO A RECHAZAR" : "¿TE VAN A CASTIGAR?"} desde={i === 0 ? en(0, "rechazar") : en(1, "castigar.")} color={i === 0 ? K.naranja : K.rosa} />
      </>
    );
  }
  if (i === 2 || i === 3) {
    return (
      <>
        <Lista d0={esc[2].desde} rojos={i === 3 ? en(3, "perder") - 6 : 99999} />
        <Frase texto={i === 2 ? "ACEPTAS DE TODO" : "HASTA LOS QUE PIERDES"} desde={i === 2 ? en(2, "todo,") : en(3, "perder")} color={i === 2 ? K.azul : ROJO} />
      </>
    );
  }
  if (i === 4) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={LIBRETA} p={p(d0, 18)} relleno={K.relleno} />
          {[0, 1, 2, 3].map((k) => (
            <g key={k} opacity={p(d0 + 10 + k * 3, 6)}>
              <line x1={300} y1={600 + k * 100} x2={500} y2={610 + k * 100} stroke={K.tenue} strokeWidth={6} strokeLinecap="round" />
              <line x1={580} y1={610 + k * 100} x2={780} y2={600 + k * 100} stroke={K.tenue} strokeWidth={6} strokeLinecap="round" />
            </g>
          ))}
        </svg>
        <Frase texto="CONOCE LAS REGLAS" sub="de tu plataforma" desde={en(4, "reglas")} color={TONO[4].linea} />
      </>
    );
  }
  if (i === 5) {
    return (
      <>
        <div style={{ position: "absolute", top: 560, left: 120, right: 120, height: 220, borderRadius: 30, border: `6px solid ${ROJO}`, background: `${ROJO}22`, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontWeight: 700, fontSize: 90, color: ROJO, boxShadow: `0 0 40px ${ROJO}88`, opacity: p(d0, 8), transform: `scale(${1.2 - 0.2 * p(d0, 8)})` }}>$70 · 13 km</div>
        <Frase texto="ESE SÍ, RECHÁZALO" sub="el que de verdad no conviene" desde={en(5, "conviene.") - 6} color={ROJO} />
      </>
    );
  }
  if (i === 6 || i === 7) {
    const g = p(d0, 8);
    const rojo = i === 6;
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: g, transform: `scale(${0.9 + 0.1 * g})` }}>
          <div style={{ padding: 18, border: `4px solid ${rojo ? ROJO : K.verde}`, borderRadius: 56, boxShadow: `0 0 40px ${rojo ? ROJO : K.verde}88` }}>{rojo ? PopRojo(860) : PopVerde(860)}</div>
        </div>
        <Etiqueta texto="números de ejemplo" top={1000} a={p(d0 + 6, 10)} />
        <Frase texto={rojo ? "$44 LIMPIOS" : "$96 EN LA BOLSA"} sub={rojo ? "$70 por 13 km" : "$120 por 12 km"} desde={rojo ? en(6, "cuarenta") : en(7, "noventa")} color={rojo ? ROJO : K.verde} />
      </>
    );
  }
  if (i === 8) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 18, alignItems: "flex-start" }}>
          <div style={{ opacity: p(d0, 8) }}>{PopRojo(465)}</div>
          <div style={{ opacity: p(d0 + 6, 8) }}>{PopVerde(465)}</div>
        </div>
        <Frase texto="LA DECISIÓN ES TUYA" desde={en(8, "decisión")} color={K.cian} />
      </>
    );
  }
  if (i === 9 || i === 10) {
    const y = interpolate((frame - esc[9].desde) % 50, [0, 50], [380, 1090]);
    const toque = en(10, "tocas");
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(esc[9].desde, 12)} relleno={K.relleno} />
          {i === 9 ? <line x1={370} y1={y} x2={710} y2={y} stroke={K.cian} strokeWidth={6} style={{ filter: `drop-shadow(0 0 16px ${K.cian})` }} /> : null}
          <path d="M440 620 Q540 540 640 620 Q540 700 440 620 Z" fill="none" stroke={K.cian} strokeWidth={6} opacity={i === 9 ? p(d0, 8) : 0.3} />
          <circle cx={540} cy={620} r={28} fill={K.cian} opacity={i === 9 ? p(d0, 8) : 0.3} />
          <rect x={410} y={940} width={260} height={110} rx={24} fill={i === 10 && frame >= toque ? `${K.verde}55` : "none"} stroke={K.verde} strokeWidth={5} />
          {i === 10 ? <circle cx={540} cy={995} r={interpolate(frame, [toque, toque + 10], [0, 80], clamp)} fill="none" stroke={K.texto} strokeWidth={5} opacity={1 - ap(frame, toque + 4, 10)} /> : null}
        </svg>
        <div style={{ position: "absolute", top: 970, left: 410, width: 260, textAlign: "center", fontFamily: MONO, fontSize: 40, color: K.verde }}>Aceptar</div>
        <Frase texto={i === 9 ? "SOLO LEE LA PANTALLA" : "EL BOTÓN LO TOCAS TÚ"} desde={i === 9 ? en(9, "lee") : toque} color={i === 9 ? K.cian : K.verde} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(11, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días de prueba sin costo</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(11, "link"), 8) }}>link en el perfil ↓</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  miedo: K.naranja, rechazar: "#F43F5E", castigar: K.rosa, todo: K.azul, perder: "#F43F5E", dinero: "#F43F5E", reglas: "#A78BFA", conviene: "#F43F5E",
  setenta: K.amarillo, trece: K.magenta, cuarenta: "#F43F5E", cuatro: "#F43F5E", ciento: K.amarillo, veinte: K.amarillo, doce: K.cian, noventa: K.verde, seis: K.verde,
  decisión: K.cian, tuya: K.cian, checa: K.verde, pantalla: K.cian, botón: K.verde, tú: K.verde, siete: K.verde, días: K.verde, prueba: K.verde,
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

export const Rechazar1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`rechazar/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(1, "castigar.")} src="error.wav" vol={0.08} />
    <Sfx en={en(3, "perder")} src="pop.mp3" vol={0.1} />
    <Sfx en={esc[6].desde} src="error.wav" vol={0.1} />
    <Sfx en={esc[7].desde} src="ding.wav" vol={0.12} />
    <Sfx en={en(10, "tocas")} src="pop.mp3" vol={0.12} />
    <Sfx en={esc[11].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
