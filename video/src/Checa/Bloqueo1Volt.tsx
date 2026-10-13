import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasBloqueo";

// Tema 10 «Bloqueo o desactivación sin explicación» (video de valor), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_BLOQUEO_1VOLT = duracion;
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




const ROJO = "#F43F5E";
const TELEFONO = "M360 360 Q360 310 410 310 L670 310 Q720 310 720 360 L720 1110 Q720 1160 670 1160 L410 1160 Q360 1160 360 1110 Z";
const CANDADO = "M440 640 L640 640 Q655 640 655 655 L655 840 Q655 855 640 855 L440 855 Q425 855 425 840 L425 655 Q425 640 440 640 Z M465 640 L465 590 Q465 520 540 520 Q615 520 615 590 L615 640";

const Num: React.FC<{ n: number; c: string; d: number }> = ({ n, c, d }) => {
  const frame = useCurrentFrame();
  return <div style={{ position: "absolute", top: 230, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 150, color: c, opacity: ap(frame, d, 6), textShadow: `0 0 40px ${c}88` }}>{n}</div>;
};
const Palomita: React.FC<{ x: number; y: number; c: string; a: number; tam?: number }> = ({ x, y, c, a, tam = 60 }) => (
  <path d={`M${x - tam} ${y} L${x - tam / 4} ${y + tam * 0.7} L${x + tam} ${y - tam * 0.7}`} fill="none" stroke={c} strokeWidth={14} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray="1 1" strokeDashoffset={1 - a} style={{ filter: `drop-shadow(0 0 10px ${c})` }} />
);

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(d0, 12)} relleno={K.relleno} />
          <Trazo d={CANDADO} p={p(en(0, "cuenta"), 12)} color={ROJO} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 400, left: 360, width: 360, textAlign: "center", fontFamily: MONO, fontSize: 40, color: K.amarillo, opacity: p(en(0, "seis"), 6) }}>6:00 am</div>
        <div style={{ position: "absolute", top: 920, left: 360, width: 360, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 40, color: ROJO, opacity: p(en(0, "desactivada..."), 6) }}>Cuenta<br />desactivada</div>
        <Frase texto="¿CUENTA DESACTIVADA?" desde={en(0, "pesadilla.") - 6} color={ROJO} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 380, color: K.rosa, opacity: p(d0, 8), textShadow: `0 0 50px ${K.rosa}88` }}>?</div>
        <Frase texto="NADIE TE DICE POR QUÉ" desde={en(1, "nadie")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        {[0, 1, 2, 3].map((k) => (
          <div key={k} style={{ position: "absolute", top: 420 + k * 150, left: 220, right: 220, height: 100, borderRadius: 18, border: `4px solid ${TONO[2].linea}`, display: "flex", alignItems: "center", gap: 30, padding: "0 30px", opacity: p(d0 + k * 3, 8) }}>
            <div style={{ width: 50, height: 50, borderRadius: 10, border: `4px solid ${TONO[2].linea}`, background: k === 0 && frame >= en(2, "hoy.") ? K.verde : "transparent" }} />
            <div style={{ fontFamily: MONO, fontSize: 40, color: TONO[2].linea }}>{k + 1}</div>
          </div>
        ))}
        <Frase texto="HAZ ESTO DESDE HOY" desde={en(2, "haz")} color={TONO[2].linea} />
      </>
    );
  }
  if (i === 3) {
    return (
      <>
        <Num n={1} c={K.cian} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M260 700 L460 700 L500 750 L820 750 L820 1080 L260 1080 Z" p={p(d0, 14)} color={K.cian} relleno={K.relleno} />
          {[0, 1, 2].map((k) => {
            const t0 = en(3, "captura") + k * 8;
            const y = interpolate(frame, [t0, t0 + 14], [420, 820 + k * 20], { ...clamp, easing: Easing.in(Easing.quad) });
            return <rect key={k} x={380 + k * 110} y={y} width={100} height={170} rx={14} fill="#05060F" stroke={K.cian} strokeWidth={5} opacity={frame >= t0 ? 1 : 0} />;
          })}
        </svg>
        <Frase texto="GUARDA CAPTURAS" sub="de cada viaje con problema" desde={en(3, "captura")} color={K.cian} />
      </>
    );
  }
  if (i === 4) {
    const doc = (t: string, x: number, d: number) => (
      <div style={{ position: "absolute", top: 520, left: x, width: 340, height: 220, borderRadius: 24, border: `5px solid ${K.azul}`, background: `${K.azul}14`, padding: 24, fontFamily: MONO, opacity: ap(frame, d, 8) }}>
        <div style={{ width: 80, height: 80, borderRadius: 12, border: `4px solid ${K.azul}` }} />
        <div style={{ fontSize: 30, color: K.azul, marginTop: 20 }}>{t}</div>
      </div>
    );
    return (
      <>
        <Num n={2} c={K.azul} d={d0} />
        {doc("licencia", 160, en(4, "licencia"))}
        {doc("tarjeta", 580, en(4, "documentos"))}
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Palomita x={540} y={900} c={K.verde} a={p(en(4, "vigentes"), 10)} />
        </svg>
        <Frase texto="DOCUMENTOS VIGENTES" desde={en(4, "vigentes")} color={K.azul} />
      </>
    );
  }
  if (i === 5) {
    const env = en(5, "pruebas");
    return (
      <>
        <Num n={3} c={K.naranja} d={d0} />
        <div style={{ position: "absolute", top: 440, left: 200, right: 200, height: 600, borderRadius: 30, border: `5px solid ${K.naranja}`, background: `${K.naranja}10`, padding: 40, fontFamily: MONO, opacity: p(d0, 8) }}>
          <div style={{ fontSize: 34, color: K.naranja }}>apelación</div>
          {[0, 1, 2].map((k) => <div key={k} style={{ height: 16, borderRadius: 8, background: `${K.naranja}55`, marginTop: 30, width: `${90 - k * 15}%`, opacity: ap(frame, en(5, "apela") + k * 3, 6) }} />)}
          <div style={{ display: "flex", gap: 20, marginTop: 40 }}>
            {[0, 1].map((k) => <div key={k} style={{ width: 90, height: 130, borderRadius: 12, border: `4px solid ${K.cian}`, opacity: ap(frame, env - 6 + k * 4, 6) }} />)}
          </div>
          <div style={{ marginTop: 40, height: 90, borderRadius: 18, background: frame >= env ? `${K.verde}55` : "transparent", border: `4px solid ${K.verde}`, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 36, color: K.verde }}>Enviar apelación</div>
        </div>
        <Frase texto="APELA POR LO OFICIAL" sub="con calma y con pruebas" desde={en(5, "apela")} color={K.naranja} />
      </>
    );
  }
  if (i === 6) {
    return (
      <>
        <Num n={4} c={ROJO} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M380 620 L700 620 L700 900 L380 900 Z M470 720 m-30 0 a30 30 0 1 0 60 0 a30 30 0 1 0 -60 0 M610 720 m-30 0 a30 30 0 1 0 60 0 a30 30 0 1 0 -60 0 M460 830 L620 830 M540 620 L540 540 M540 520 m-20 0 a20 20 0 1 0 40 0 a20 20 0 1 0 -40 0 M440 900 L440 1000 L640 1000 L640 900" p={p(d0, 16)} color={K.rosa} relleno={K.relleno} />
          <path d="M320 500 L760 1060 M760 500 L320 1060" stroke={ROJO} strokeWidth={16} strokeLinecap="round" opacity={p(en(6, "modifican"), 6)} style={{ filter: `drop-shadow(0 0 12px ${ROJO})` }} />
        </svg>
        <Frase texto="CUIDADO CON ESAS APPS" sub="que aceptan por ti o modifican la plataforma" desde={en(6, "apps")} color={ROJO} />
      </>
    );
  }
  if (i === 7 || i === 8) {
    const y = interpolate((frame - esc[7].desde) % 45, [0, 45], [380, 1090]);
    const toque = en(8, "decides");
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(esc[7].desde, 12)} relleno={K.relleno} />
          {i === 7 ? <line x1={370} y1={y} x2={710} y2={y} stroke={K.cian} strokeWidth={6} style={{ filter: `drop-shadow(0 0 16px ${K.cian})` }} /> : null}
          <path d="M440 520 Q540 440 640 520 Q540 600 440 520 Z" fill="none" stroke={K.cian} strokeWidth={6} opacity={i === 7 ? 1 : 0.3} />
          <circle cx={540} cy={520} r={28} fill={K.cian} opacity={i === 7 ? 1 : 0.3} />
          <rect x={390} y={900} width={140} height={100} rx={20} fill={i === 8 && frame >= toque ? `${K.verde}55` : "none"} stroke={K.verde} strokeWidth={5} />
          <rect x={550} y={900} width={140} height={100} rx={20} fill="none" stroke={ROJO} strokeWidth={5} />
          {i === 8 ? <circle cx={460} cy={950} r={interpolate(frame, [toque, toque + 10], [0, 80], clamp)} fill="none" stroke={K.texto} strokeWidth={5} opacity={1 - ap(frame, toque + 4, 10)} /> : null}
        </svg>
        <div style={{ position: "absolute", top: 680, left: 360, width: 360, textAlign: "center", fontFamily: MONO, opacity: p(esc[7].desde + 4, 8) }}>
          <div style={{ fontSize: 26, color: K.tenue }}>oferta</div>
          <div style={{ fontSize: 80, fontWeight: 700, color: K.amarillo }}>$120</div>
        </div>
        <Frase texto={i === 7 ? "SOLO LEE LA OFERTA" : "LO DECIDES TÚ"} sub={i === 7 ? "la app Checa" : "aceptar o rechazar"} desde={i === 7 ? en(7, "lee") : toque} color={i === 7 ? K.cian : K.verde} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 300, opacity: p(d0, 8) }}>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 52, letterSpacing: 8, color: K.amarillo, opacity: p(d0, 8) }}>COMPÁRTELO</div>
      <Logo tam={110} />
      <div style={{ fontFamily: MONO, fontSize: 40, color: K.cian, border: `4px solid ${K.cian}`, borderRadius: 16, padding: "12px 30px", opacity: p(en(9, "Síguenos"), 8) }}>+ Seguir</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(9, "conductor"), 8) }}>consejos de conductor a conductor</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  seis: K.amarillo, desactivada: "#F43F5E", pesadilla: "#F43F5E", nadie: K.rosa, hoy: K.verde, uno: K.cian, captura: K.cian, reportes: K.cian, dos: K.azul, licencia: K.azul, documentos: K.azul,
  tres: K.naranja, apela: K.naranja, oficiales: K.naranja, pruebas: K.naranja, cuatro: "#F43F5E", cuidado: "#F43F5E", checa: K.verde, lee: K.cian, decides: K.verde, tú: K.verde, comparte: K.amarillo, síguenos: K.cian,
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

export const Bloqueo1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`bloqueo/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[3, 4, 5, 6].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(0, "desactivada...")} src="error.wav" vol={0.08} />
    <Sfx en={en(8, "decides")} src="pop.mp3" vol={0.12} />
    <Sfx en={esc[9].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
