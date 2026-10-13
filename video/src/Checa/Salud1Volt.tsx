import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasSalud";

// Tema 12 «Cansancio y salud» (video de valor), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_SALUD_1VOLT = duracion;
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
const PERSONA = (x: number, y: number) => `M${x} ${y - 230} m-50 0 a50 50 0 1 0 100 0 a50 50 0 1 0 -100 0 M${x} ${y - 180} L${x} ${y} M${x} ${y} L${x - 70} ${y + 170} M${x} ${y} L${x + 70} ${y + 170}`;

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
          {Array.from({ length: 8 }).map((_, k) => (
            <rect key={k} x={480 - k * 4} y={430 + k * 85} width={120 + k * 8} height={60} rx={20} fill={K.relleno} stroke={TONO[0].linea} strokeWidth={5} opacity={p(d0 + k * 2, 6)} />
          ))}
          {[3, 5, 6].map((k) => (
            <circle key={k} cx={620 + k * 3} cy={460 + k * 85} r={18 + 6 * Math.abs(Math.sin(frame / 6 + k))} fill={ROJO} opacity={p(en(0, "espalda"), 8) * 0.9} style={{ filter: `drop-shadow(0 0 12px ${ROJO})` }} />
          ))}
        </svg>
        <Frase texto="¿12 HORAS SENTADO?" desde={en(0, "espalda")} color={ROJO} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={PERSONA(380, 820)} p={p(d0, 16)} color={K.azul} />
          <g opacity={p(en(1, "herramienta"), 10)} transform="rotate(30 720 800)">
            <rect x={695} y={680} width={50} height={380} rx={25} fill={K.relleno} stroke={K.amarillo} strokeWidth={8} />
            <circle cx={720} cy={630} r={85} fill={K.relleno} stroke={K.amarillo} strokeWidth={8} />
            <rect x={690} y={530} width={60} height={110} fill="#05060F" stroke={K.amarillo} strokeWidth={8} />
          </g>
        </svg>
        <Frase texto="TU HERRAMIENTA" sub="de trabajo es tu cuerpo" desde={en(1, "herramienta")} color={K.amarillo} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <div style={{ position: "absolute", top: 480, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 420, color: K.verde, opacity: p(en(2, "cuatro"), 6), transform: `scale(${1.3 - 0.3 * p(en(2, "cuatro"), 8)})`, textShadow: `0 0 50px ${K.verde}88` }}>4</div>
        <Frase texto="4 CONSEJOS" sub="para aguantar el turno" desde={en(2, "consejos")} color={K.verde} />
      </>
    );
  }
  if (i === 3) {
    const brazos = Math.sin((frame - d0) / 8) * 30;
    return (
      <>
        <Num n={1} c={K.cian} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M300 720 m-130 0 a130 130 0 1 0 260 0 a130 130 0 1 0 -260 0" p={p(d0, 12)} color={K.cian} relleno={K.relleno} />
          <g opacity={p(en(3, "bájate"), 8)} stroke={K.cian} strokeWidth={8} strokeLinecap="round" fill="none">
            <circle cx={740} cy={560} r={45} />
            <path d="M740 605 L740 820 M740 820 L680 1000 M740 820 L800 1000" />
            <path d={`M740 650 L${660 - brazos} ${520 - brazos} M740 650 L${820 + brazos} ${520 - brazos}`} />
          </g>
        </svg>
        <div style={{ position: "absolute", top: 680, left: 170, width: 260, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 70, color: K.cian, opacity: p(en(3, "dos"), 6) }}>2 h</div>
        <Frase texto="ESTÍRATE 5 MIN" sub="cada dos horas" desde={en(3, "estirar")} color={K.cian} />
      </>
    );
  }
  if (i === 4) {
    const nivel = interpolate(frame, [en(4, "botella"), en(4, "seguido.")], [0.1, 0.85], clamp);
    return (
      <>
        <Num n={2} c={K.azul} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <rect x={445} y={1080 - 480 * nivel} width={190} height={480 * nivel} fill={K.azul} opacity={0.4} />
          <Trazo d="M500 500 L580 500 L580 560 Q640 600 640 660 L640 1060 Q640 1090 610 1090 L470 1090 Q440 1090 440 1060 L440 660 Q440 600 500 560 Z" p={p(d0, 14)} color={K.azul} />
          <Palomita x={800} y={800} c={K.verde} a={p(en(4, "seguido."), 10)} tam={50} />
        </svg>
        <Frase texto="AGUA A LA MANO" sub="y toma seguido" desde={en(4, "agua")} color={K.azul} />
      </>
    );
  }
  if (i === 5) {
    return (
      <>
        <Num n={3} c={K.naranja} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M380 1040 L700 1040 Q730 1040 730 1010 L730 980 L420 980 L380 560 Q376 530 406 530 L440 530 Q470 530 474 560 L510 980" p={p(d0, 16)} color={K.naranja} />
          <path d="M520 900 A120 120 0 0 0 470 790" fill="none" stroke={K.amarillo} strokeWidth={6} strokeDasharray="10 8" opacity={p(en(5, "respaldo."), 8)} />
          <Palomita x={800} y={760} c={K.verde} a={p(en(5, "respaldo."), 10)} tam={50} />
        </svg>
        <Frase texto="ESPALDA AL RESPALDO" sub="ajusta el asiento" desde={en(5, "asiento")} color={K.naranja} />
      </>
    );
  }
  if (i === 6) {
    const cierre = Math.min(1, Math.max(0, Math.sin((frame - d0) / 10) * 0.6 + 0.5));
    const alto = Math.floor((frame - d0) / 8) % 2 === 0 ? 1 : 0.4;
    return (
      <>
        <Num n={4} c={ROJO} d={d0} />
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M160 620 Q330 470 500 620 Q330 770 160 620 Z" fill="none" stroke={K.rosa} strokeWidth={7} />
          <circle cx={330} cy={620} r={50} fill={K.rosa} opacity={1 - cierre} />
          <path d={`M160 620 Q330 ${470 + 300 * cierre} 500 620`} fill={K.relleno} stroke={K.rosa} strokeWidth={7} />
          <g opacity={p(en(6, "oríllate"), 6) * alto}>
            <path d="M700 480 L840 480 L940 580 L940 720 L840 820 L700 820 L600 720 L600 580 Z" fill={`${ROJO}33`} stroke={ROJO} strokeWidth={8} style={{ filter: `drop-shadow(0 0 16px ${ROJO})` }} />
            <text x={770} y={680} textAnchor="middle" fontFamily={MONO} fontWeight={700} fontSize={80} fill={ROJO}>ALTO</text>
          </g>
        </svg>
        <Frase texto="ORÍLLATE A DESCANSAR" desde={en(6, "oríllate")} color={ROJO} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M260 760 L540 520 L820 760 M320 710 L320 1080 L760 1080 L760 710 M480 1080 L480 900 L600 900 L600 1080" p={p(d0, 16)} color={K.amarillo} relleno="none" />
          <rect x={360} y={780} width={90} height={80} fill={K.amarillo} opacity={p(en(7, "casa."), 8) * 0.8} style={{ filter: `drop-shadow(0 0 20px ${K.amarillo})` }} />
          <rect x={630} y={780} width={90} height={80} fill={K.amarillo} opacity={p(en(7, "casa."), 8) * 0.8} style={{ filter: `drop-shadow(0 0 20px ${K.amarillo})` }} />
        </svg>
        <Frase texto="REGRESA BIEN A CASA" desde={en(7, "regresar")} color={K.amarillo} />
      </>
    );
  }
  if (i === 8) {
    const n = [[540, 560], [330, 900], [750, 900]];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <path d="M540 560 L330 900 M540 560 L750 900" stroke={K.amarillo} strokeWidth={8} opacity={p(d0 + 4, 8)} />
          {n.map(([x, y], k) => <circle key={k} cx={x} cy={y} r={80} fill={K.relleno} stroke={K.amarillo} strokeWidth={8} opacity={p(d0 + k * 3, 6)} style={{ filter: `drop-shadow(0 0 12px ${K.amarillo})` }} />)}
        </svg>
        <Frase texto="COMPÁRTELO" sub="con un compañero" desde={d0} color={K.amarillo} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={110} />
      <div style={{ fontFamily: MONO, fontSize: 40, color: K.cian, border: `4px solid ${K.cian}`, borderRadius: 16, padding: "12px 30px", opacity: p(en(9, "Síguenos"), 8) }}>+ Seguir</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(9, "conductor"), 8) }}>consejos de conductor a conductor</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  doce: "#F43F5E", espalda: "#F43F5E", aguanta: "#F43F5E", cuerpo: K.amarillo, herramienta: K.amarillo, cuatro: K.verde, consejos: K.verde, uno: K.cian, dos: K.cian, estirar: K.cian, cinco: K.cian,
  agua: K.azul, seguido: K.azul, tres: K.naranja, asiento: K.naranja, respaldo: K.naranja, parpadeas: "#F43F5E", bostezas: "#F43F5E", oríllate: "#F43F5E", casa: K.amarillo, regresar: K.amarillo, comparte: K.amarillo, síguenos: K.cian,
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

export const Salud1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`salud/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    {[3, 4, 5, 6].map((k) => (
      <Sfx key={k} en={esc[k].desde} src="pop.mp3" vol={0.1} />
    ))}
    <Sfx en={en(0, "espalda")} src="error.wav" vol={0.08} />
    <Sfx en={en(7, "casa.")} src="ding.wav" vol={0.1} />
    <Sfx en={esc[9].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
