import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo, Popup } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasDecidir";

// Tema 5 «Decidir en segundos sin poder hacer la cuenta» (pop-up y voz), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_DECIDIR_1VOLT = duracion;
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




const TELEFONO = "M360 360 Q360 310 410 310 L670 310 Q720 310 720 360 L720 1110 Q720 1160 670 1160 L410 1160 Q360 1160 360 1110 Z";
const CALCU = "M380 520 Q380 480 420 480 L660 480 Q700 480 700 520 L700 1080 Q700 1120 660 1120 L420 1120 Q380 1120 380 1080 Z M420 530 L660 530 L660 650 L420 650 Z";

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    const resta = interpolate(frame, [d0 + 6, esc[0].hasta], [1, 0.15], clamp);
    const c = resta < 0.4 ? "#F43F5E" : K.amarillo;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(d0, 14)} relleno={K.relleno} />
          <circle cx={540} cy={560} r={110} fill="none" stroke="#2A2A40" strokeWidth={18} />
          <circle cx={540} cy={560} r={110} fill="none" stroke={c} strokeWidth={18} strokeDasharray={`${2 * Math.PI * 110 * resta} 999`} transform="rotate(-90 540 560)" strokeLinecap="round" style={{ filter: `drop-shadow(0 0 10px ${c})` }} />
        </svg>
        <div style={{ position: "absolute", top: 520, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 70, color: c }}>{Math.ceil(resta * 15)}s</div>
        <div style={{ position: "absolute", top: 760, left: 400, width: 280, textAlign: "center", fontFamily: MONO, opacity: p(d0 + 6, 8) }}>
          <div style={{ fontSize: 26, color: K.tenue }}>nueva oferta</div>
          <div style={{ fontSize: 90, fontWeight: 700, color: K.amarillo }}>$120</div>
          <div style={{ marginTop: 30, border: `3px solid ${K.verde}`, borderRadius: 16, padding: "10px 0", fontSize: 32, color: K.verde }}>Aceptar</div>
        </div>
        <Frase texto="UNOS SEGUNDOS" sub="para decidir" desde={en(0, "segundos")} color={K.amarillo} />
      </>
    );
  }
  if (i === 1) {
    const datos: [string, string, number][] = [
      ["$120", K.amarillo, d0],
      ["12 km", K.azul, en(1, "kilómetros,")],
      ["25 min", K.cian, en(1, "tiempo")],
      ["2 km recogida", K.magenta, en(1, "recogida...")],
    ];
    const ang = interpolate(frame, [en(1, "gasolina?"), en(1, "gasolina?") + 12], [-60, 40], clamp);
    return (
      <>
        {datos.map(([t, c, d], k) => (
          <div key={t} style={{ position: "absolute", top: interpolate(frame, [d, d + 10], [200, 330 + k * 105], { ...clamp, easing: Easing.out(Easing.back(1.4)) }), left: k % 2 ? 470 : 150, fontFamily: MONO, fontWeight: 700, fontSize: 56, color: c, border: `4px solid ${c}`, borderRadius: 16, padding: "8px 22px", opacity: ap(frame, d, 6), transform: `rotate(${k % 2 ? 4 : -4}deg)` }}>{t}</div>
        ))}
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M340 1080 A200 200 0 0 1 740 1080" fill="none" stroke={K.naranja} strokeWidth={8} opacity={p(en(1, "gasolina?") - 4, 8)} />
          <line x1={540} y1={1080} x2={540 + Math.sin((ang * Math.PI) / 180) * 170} y2={1080 - Math.cos((ang * Math.PI) / 180) * 170} stroke={K.naranja} strokeWidth={10} strokeLinecap="round" opacity={p(en(1, "gasolina?") - 4, 8)} />
        </svg>
        <div style={{ position: "absolute", top: 760, right: 120, fontFamily: MONO, fontWeight: 700, fontSize: 220, color: K.rosa, opacity: p(en(1, "¿y"), 8), textShadow: `0 0 40px ${K.rosa}88` }}>?</div>
        <Frase texto="¿Y LA GASOLINA?" desde={en(1, "gasolina?")} color={K.naranja} />
      </>
    );
  }
  if (i === 2) {
    const toque = en(2, "aceptando");
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform="translate(-170 0)">
            <Trazo d={CALCU} p={p(d0, 14)} relleno={K.relleno} />
            {Array.from({ length: 9 }).map((_, k) => (
              <rect key={k} x={430 + (k % 3) * 80} y={700 + Math.floor(k / 3) * 120} width={60} height={80} rx={10} fill="none" stroke={TONO[2].linea} strokeWidth={4} opacity={p(d0 + 6 + k, 6)} />
            ))}
          </g>
          <line x1={220} y1={1100} x2={520} y2={500} stroke={K.magenta} strokeWidth={14} strokeLinecap="round" strokeDasharray="700" strokeDashoffset={700 * (1 - p(en(2, "cuentas,"), 8))} style={{ filter: `drop-shadow(0 0 12px ${K.magenta})` }} />
          <rect x={620} y={760} width={300} height={110} rx={22} fill={frame >= toque ? `${K.verde}55` : "none"} stroke={K.verde} strokeWidth={5} opacity={p(en(2, "así"), 8)} />
          <circle cx={770} cy={815} r={interpolate(frame, [toque, toque + 10], [0, 70], clamp)} fill="none" stroke={K.texto} strokeWidth={5} opacity={1 - ap(frame, toque + 4, 10)} />
        </svg>
        <div style={{ position: "absolute", top: 790, left: 620, width: 300, textAlign: "center", fontFamily: MONO, fontSize: 40, color: K.verde, opacity: p(en(2, "así"), 8) }}>Aceptar</div>
        <Frase texto="AL AVENTÓN" sub="sin hacer la cuenta" desde={toque} color={K.rosa} />
      </>
    );
  }
  if (i === 3) {
    const filas = [1, 0, 1, 1, 0, 1, 0, 1];
    return (
      <>
        {filas.map((ok, k) => {
          const d = d0 + k * 4;
          const c = ok ? K.verde : "#F43F5E";
          return (
            <div key={k} style={{ position: "absolute", top: 300 + k * 110, left: 160, right: 160, height: 84, borderRadius: 16, border: `4px solid ${c}`, background: `${c}${ok ? "14" : "33"}`, display: "flex", alignItems: "center", justifyContent: "space-between", padding: "0 30px", fontFamily: MONO, fontSize: 38, color: c, opacity: ap(frame, d, 6), transform: `translateX(${(1 - ap(frame, d, 8)) * -60}px)`, boxShadow: ok ? undefined : `0 0 20px ${c}66` }}>
              <span>viaje {k + 1}</span>
              <span style={{ fontWeight: 700, fontFamily: ok ? "Inter" : MONO }}>{ok ? "✓" : "pierdes"}</span>
            </div>
          );
        })}
        <Frase texto="SE TE CUELAN" sub="los que te hacen perder" desde={en(3, "cuelan")} color="#F43F5E" />
      </>
    );
  }
  if (i === 4) {
    const y = interpolate(frame, [d0 + 4, esc[4].hasta - 6], [430, 1080], clamp);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(d0, 12)} relleno={K.relleno} />
          <line x1={370} y1={y} x2={710} y2={y} stroke={K.verde} strokeWidth={6} style={{ filter: `drop-shadow(0 0 16px ${K.verde})` }} />
          <rect x={370} y={420} width={340} height={Math.max(0, y - 420)} fill={`${K.verde}14`} />
        </svg>
        <div style={{ position: "absolute", top: 480, left: 360, width: 360, textAlign: "center", fontFamily: MONO }}>
          <div style={{ fontSize: 28, color: K.tenue }}>nueva oferta</div>
          <div style={{ fontSize: 110, fontWeight: 700, color: K.amarillo }}>$120</div>
          <div style={{ fontSize: 30, color: K.magenta, marginTop: 40 }}>● 5 min (2.0 km)</div>
          <div style={{ fontSize: 30, color: K.cian, marginTop: 16 }}>■ 20 min (10.0 km)</div>
        </div>
        <Frase texto="LEE LA PANTALLA" sub="en un segundo" desde={en(4, "lee")} color={K.verde} />
      </>
    );
  }
  if (i === 5) {
    const palabras: [string, string, number][] = [
      ["ACEPTAR", K.verde, en(5, "Aceptar,")],
      ["CONSIDERAR", K.amarillo, en(5, "Considerar")],
      ["RECHAZAR", "#F43F5E", en(5, "Rechazar.")],
    ];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M470 420 Q470 400 490 400 L590 400 Q610 400 610 420 L610 620 Q610 640 590 640 L490 640 Q470 640 470 620 Z" p={p(d0, 12)} relleno={K.relleno} />
          {[0, 1, 2].map((k) => {
            const r = 160 + k * 70 + ((frame - d0) % 30) * 2;
            return <path key={k} d={`M${540 - r * 0.7} ${520 - r * 0.7} A${r} ${r} 0 0 1 ${540 + r * 0.7} ${520 - r * 0.7}`} fill="none" stroke={TONO[5].linea} strokeWidth={6} opacity={(0.8 - k * 0.2) * p(d0 + 4, 8)} />;
          })}
        </svg>
        {palabras.map(([t, c, d], k) => (
          <div key={t} style={{ position: "absolute", top: 760 + k * 130, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 84, letterSpacing: 8, color: c, opacity: ap(frame, d, 6), transform: `scale(${1.2 - 0.2 * ap(frame, d, 6)})`, textShadow: `0 0 30px ${c}88` }}>{t}</div>
        ))}
        <Frase texto="EN VOZ ALTA" desde={en(5, "voz")} color={K.cian} />
      </>
    );
  }
  if (i === 6) {
    const g = p(d0, 8);
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: g, transform: `scale(${0.9 + 0.1 * g})` }}>
          <div style={{ padding: 18, border: `4px solid ${K.verde}`, borderRadius: 56, boxShadow: `0 0 40px ${K.verde}88` }}>
            <Popup
              ancho={860}
              nivel="verde"
              app="App"
              exacto
              neta="96.00"
              viaje="120.00"
              gas="24.00"
              fichas={[
                { v: "$288/h", l: "meta $150", c: "ok" },
                { v: "$10.00/km", l: "meta $8", c: "ok" },
                { v: "25 min", l: "12.0 km total", c: "neutro" },
              ]}
            />
          </div>
        </div>
        <Frase texto="Y SIGUES MANEJANDO" sub="solo ves el color" desde={en(6, "color")} color={K.verde} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(7, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días de prueba sin costo</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(7, "link"), 8) }}>link en el perfil ↓</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  segundos: K.amarillo, tarifa: K.amarillo, kilómetros: K.azul, recogida: K.magenta, gasolina: K.naranja, cuentas: K.rosa, aventón: K.rosa, perder: "#F43F5E", dinero: "#F43F5E",
  checa: K.verde, segundo: K.verde, voz: K.cian, aceptar: K.verde, considerar: K.amarillo, rechazar: "#F43F5E", color: K.verde, aviso: K.cian, siete: K.verde, días: K.verde, prueba: K.verde,
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

export const Decidir1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`decidir/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(2, "aceptando")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(5, "Aceptar,")} src="ding.wav" vol={0.1} />
    <Sfx en={esc[6].desde} src="ding.wav" vol={0.13} />
    <Sfx en={esc[7].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
