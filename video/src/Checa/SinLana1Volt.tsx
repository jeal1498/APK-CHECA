import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo, Popup } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasSinLana1Volt";

// Tema 1 «Trabajar mucho y no ver el dinero», versión 1Volt a color (guion aprobado de SinLana, partido en cortes de ~3 s).
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_SINLANA_1VOLT = duracion;
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

// Carro de perfil en line-art
const CARRO = "M120 980 L120 920 Q130 900 160 896 L220 890 L270 840 Q285 826 310 826 L420 826 Q445 826 460 842 L505 890 L560 900 Q590 906 590 930 L590 980 Z";
const RUEDAS = "M190 985 a38 38 0 1 0 76 0 a38 38 0 1 0 -76 0 M440 985 a38 38 0 1 0 76 0 a38 38 0 1 0 -76 0";


const ROJO = "#F43F5E";
const VOLANTE = "M540 820 m-260 0 a260 260 0 1 0 520 0 a260 260 0 1 0 -520 0 M540 820 m-70 0 a70 70 0 1 0 140 0 a70 70 0 1 0 -140 0 M290 820 L470 820 M610 820 L790 820 M540 890 L540 1080";
const CARTERA = "M240 660 L840 660 Q880 660 880 700 L880 1040 Q880 1080 840 1080 L240 1080 Q200 1080 200 1040 L200 700 Q200 660 240 660 Z M640 800 L880 800 L880 940 L640 940 Q600 940 600 900 L600 840 Q600 800 640 800 Z";
const BIDON = "M380 560 L640 560 L700 620 L700 1180 Q700 1210 670 1210 L410 1210 Q380 1210 380 1180 Z M440 560 L440 500 L540 500 L540 560";

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={VOLANTE} p={p(d0, 16)} relleno={K.relleno} />
        </svg>
        <Numero texto="12 h" color={K.amarillo} tam={160} top={280} a={p(en(0, "Doce"), 6)} />
        <Frase texto="AL VOLANTE" desde={en(0, "volante.")} color={K.naranja} />
      </>
    );
  }
  if (i === 1) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={CARTERA} p={p(d0, 14)} color={K.rosa} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 760, left: 260, fontFamily: MONO, fontWeight: 700, fontSize: 130, color: K.rosa, opacity: p(en(1, "lana"), 6), textShadow: `0 0 30px ${K.rosa}` }}>$0</div>
        <Frase texto="¿Y LA LANA?" desde={en(1, "lana")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    const n = Math.min(20, Math.max(0, Math.floor((frame - en(2, "veinte")) / 2)));
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          {Array.from({ length: 20 }).map((_, k) => {
            const on = k < n;
            return <circle key={k} cx={240 + (k % 5) * 150} cy={560 + Math.floor(k / 5) * 140} r={48} fill={on ? `${K.amarillo}44` : K.relleno} stroke={on ? K.amarillo : TONO[2].linea} strokeWidth={5} />;
          })}
        </svg>
        <Numero texto="× $80" color={K.amarillo} tam={110} top={300} a={p(en(2, "ochenta"), 6)} />
        <Frase texto="20 VIAJES" sub="números de ejemplo" desde={en(2, "veinte")} color={K.azul} />
      </>
    );
  }
  if (i === 3) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M330 380 Q330 330 380 330 L700 330 Q750 330 750 380 L750 1100 Q750 1150 700 1150 L380 1150 Q330 1150 330 1100 Z" p={p(d0, 12)} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 560, left: 330, width: 420, textAlign: "center", fontFamily: MONO }}>
          <div style={{ fontSize: 30, color: K.tenue, opacity: p(d0 + 4, 6) }}>ganancias de hoy</div>
          <div style={{ fontSize: 100, fontWeight: 700, color: K.amarillo, marginTop: 20, opacity: p(en(3, "Mil"), 6), textShadow: `0 0 30px ${K.amarillo}66` }}>$1,600</div>
        </div>
        <Frase texto="EN LA APP" desde={en(3, "app.")} color={K.amarillo} />
      </>
    );
  }
  if (i === 4) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform="translate(-60 -100)">
            <Trazo d={CARRO} p={p(d0, 12)} relleno={K.relleno} />
            <Trazo d={RUEDAS} p={p(d0 + 4, 10)} relleno={K.relleno} />
          </g>
          <path d="M930 900 C880 830 880 760 930 760 C980 760 980 830 930 900 Z" fill={`${K.magenta}33`} stroke={K.magenta} strokeWidth={6} opacity={p(en(4, "recoger"), 6)} />
          <Cota x1={560} x2={910} y={1000} p={p(en(4, "seis"), 14)} color={K.magenta} punteada />
        </svg>
        <Numero texto="6 km" color={K.magenta} tam={150} top={330} a={p(en(4, "seis"), 6)} />
        <Frase texto="SIN PAGO" sub="solo para recoger" desde={en(4, "nadie")} color={K.magenta} />
      </>
    );
  }
  if (i === 5) {
    const corte = 140 + (6 / 14) * 800;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Cota x1={140} x2={corte} y={820} p={p(d0, 12)} color={K.magenta} punteada />
          <Cota x1={corte} x2={940} y={820} p={p(en(5, "ocho"), 14)} color={K.cian} />
        </svg>
        <div style={{ position: "absolute", top: 860, left: 140, width: corte - 140, textAlign: "center", fontFamily: MONO, fontSize: 34, color: K.magenta }}>6 km</div>
        <div style={{ position: "absolute", top: 860, left: corte, width: 940 - corte, textAlign: "center", fontFamily: MONO, fontSize: 34, color: K.cian, opacity: p(en(5, "ocho"), 6) }}>8 km</div>
        <Numero texto="14 km" color={K.azul} tam={160} top={460} a={p(en(5, "viaje."), 6)} />
        <Frase texto="LO QUE MANEJAS" desde={en(5, "viaje.")} color={K.azul} />
      </>
    );
  }
  if (i === 6) {
    const q = en(6, "veintiocho.");
    return (
      <>
        <Numero texto="$80" color={K.amarillo} tam={150} top={330} a={p(d0, 6)} tachar={p(q, 8)} />
        <Numero texto="−$28" color={K.naranja} tam={110} top={540} a={p(en(6, "gasolina"), 6)} />
        <Numero texto="$52" color={K.verde} tam={230} top={720} a={p(q + 6, 8)} />
        <Frase texto="TE QUEDAN $52" sub="14 km × $2 de gasolina" desde={q + 6} color={K.verde} />
      </>
    );
  }
  if (i === 7) {
    const nivel = interpolate(frame, [d0, esc[7].hasta], [0.9, 0.15], clamp);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <rect x={400} y={1190 - 560 * nivel} width={280} height={560 * nivel} fill={K.naranja} opacity={0.35} />
          <Trazo d={BIDON} p={p(d0, 14)} color={K.naranja} />
        </svg>
        <Numero texto="$560" color={K.rosa} tam={170} top={250} a={p(en(7, "quinientos"), 6)} />
        <Frase texto="AL TANQUE" sub="en todo el día" desde={en(7, "tanque.")} color={K.rosa} />
      </>
    );
  }
  if (i === 8) {
    const n = Math.min(20, Math.max(0, Math.floor((frame - d0) / 2)));
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          {Array.from({ length: 20 }).map((_, k) => (
            <line key={k} x1={180 + (k % 4) * 190} y1={560 + Math.floor(k / 4) * 120} x2={320 + (k % 4) * 190} y2={560 + Math.floor(k / 4) * 120} stroke={k < n ? K.magenta : `${K.magenta}33`} strokeWidth={14} strokeDasharray="18 12" strokeLinecap="round" />
          ))}
        </svg>
        <Numero texto="120 km" color={K.magenta} tam={150} top={260} a={p(en(8, "ciento"), 6)} />
        <Frase texto="MANEJADOS GRATIS" desde={en(8, "gratis.")} color={K.magenta} />
      </>
    );
  }
  if (i === 9) {
    const g = p(d0, 8);
    return (
      <>
        <div style={{ position: "absolute", top: 400, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: g, transform: `scale(${0.9 + 0.1 * g})` }}>
          <div style={{ padding: 18, border: `4px solid ${ROJO}`, borderRadius: 56, boxShadow: `0 0 40px ${ROJO}88` }}>
            <Popup ancho={860} nivel="rojo" app="App" exacto neta="52.00" viaje="80.00" gas="28.00"
              fichas={[{ v: "$141/h", l: "meta $150", c: "mal" }, { v: "$5.71/km", l: "meta $8", c: "mal" }, { v: "34 min", l: "14.0 km total", c: "neutro" }]} />
          </div>
        </div>
        <Frase texto="LO LIMPIO" sub="antes de aceptar" desde={en(9, "limpio")} color={K.verde} />
      </>
    );
  }
  if (i === 10) {
    return (
      <>
        <Numero texto="tarifa" color={K.tenue} tam={90} top={420} a={p(d0, 6)} tachar={p(en(10, "netos."), 8)} />
        <Numero texto="NETOS" color={K.verde} tam={220} top={600} a={p(en(10, "netos."), 6)} />
        <Frase texto="CHECA LOS NETOS" sub="en tu próxima oferta" desde={en(10, "checa")} color={K.verde} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(11, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días gratis</div>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 52, color: K.amarillo, opacity: p(en(11, "¿Cuánto"), 8) }}>¿Cuánto te quedó hoy?</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(11, "¿Cuánto"), 8) }}>link en el perfil ↓</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  doce: K.amarillo, volante: K.naranja, lana: K.rosa, veinte: K.azul, ochenta: K.amarillo, mil: K.amarillo, seiscientos: K.amarillo, seis: K.magenta, nadie: K.magenta, ocho: K.cian,
  gasolina: K.naranja, veintiocho: K.naranja, quinientos: K.rosa, sesenta: K.rosa, tanque: K.rosa, ciento: K.magenta, gratis: K.magenta, checa: K.verde, limpio: K.verde, netos: K.verde, siete: K.verde, días: K.verde,
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

export const SinLana1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`sinlana1v/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(1, "lana")} src="error.wav" vol={0.08} />
    <Sfx en={en(3, "Mil")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(6, "veintiocho.") + 6} src="pop.mp3" vol={0.12} />
    <Sfx en={en(8, "ciento")} src="golpe.wav" vol={0.12} />
    <Sfx en={esc[9].desde} src="error.wav" vol={0.08} />
    <Sfx en={esc[11].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
