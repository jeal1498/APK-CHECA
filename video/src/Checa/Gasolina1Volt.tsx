import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo, Popup } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { Toma } from "./Toma";
import { LINEAS } from "./lineasGasolina";

// Tema 2 «Gasolina cara que se come la ganancia», estilo 1Volt a color (plantilla: Recogida1Volt.tsx). Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_GASOLINA_1VOLT = duracion;
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

// Carro de perfil en line-art
const CARRO = "M120 980 L120 920 Q130 900 160 896 L220 890 L270 840 Q285 826 310 826 L420 826 Q445 826 460 842 L505 890 L560 900 Q590 906 590 930 L590 980 Z";
const RUEDAS = "M190 985 a38 38 0 1 0 76 0 a38 38 0 1 0 -76 0 M440 985 a38 38 0 1 0 76 0 a38 38 0 1 0 -76 0";


// Bidón de gasolina en line-art (con nivel que baja)
const BIDON = "M380 560 L640 560 L700 620 L700 1180 Q700 1210 670 1210 L410 1210 Q380 1210 380 1180 Z M440 560 L440 500 L540 500 L540 560";
const BILLETE = "M220 640 L860 640 L860 1000 L220 1000 Z M540 820 m-90 0 a90 90 0 1 0 180 0 a90 90 0 1 0 -180 0";

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0 || i === 1) {
    const nivel = i === 0 ? 1 : interpolate(frame, [esc[1].voz, esc[1].voz + 50], [1, 0.12], { ...clamp, easing: Easing.inOut(Easing.cubic) });
    const alto = 560 * nivel;
    const c = nivel < 0.3 ? K.rosa : K.naranja;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <rect x={400} y={1190 - alto} width={280} height={alto} fill={c} opacity={0.35 * p(esc[0].voz, 10)} />
          <Trazo d={BIDON} p={p(esc[0].desde, 16)} color={K.naranja} />
        </svg>
        <Numero texto="$800" color={K.amarillo} tam={170} top={260} a={p(en(0, "ochocientos"), 8)} />
        {i === 1 ? <Frase texto="EN LA RESERVA" sub="a media semana" desde={en(1, "reserva.")} color={K.rosa} /> : <Frase texto="TANQUE LLENO" desde={en(0, "gas")} color={K.naranja} />}
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M390 1180 L390 700 Q390 660 430 660 L650 660 Q690 660 690 700 L690 1180 Z" p={p(d0, 16)} relleno={K.relleno} />
          <Trazo d="M430 720 L650 720 L650 860 L430 860 Z" p={p(d0 + 6, 12)} />
          <Trazo d="M690 780 L760 780 Q800 780 800 820 L800 1060 Q800 1100 840 1100 L870 1100" p={p(d0 + 10, 14)} />
        </svg>
        <div style={{ position: "absolute", top: 760, left: 430, width: 220, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 50, color: K.amarillo, opacity: p(en(2, "dos"), 6) }}>$2/km</div>
        <Numero texto="$2 / km" color={K.amarillo} tam={150} top={330} a={p(en(2, "dos"), 8)} />
        <Etiqueta texto="12 km/l · $24 el litro" top={520} a={p(en(2, "dos") + 6, 10)} />
        <Frase texto="HAZ CUENTAS" sub="números de ejemplo" desde={d0} color={K.azul} />
      </>
    );
  }
  if (i === 3) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M360 300 Q360 250 410 250 L670 250 Q720 250 720 300 L720 1050 Q720 1100 670 1100 L410 1100 Q360 1100 360 1050 Z" p={p(d0, 16)} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 480, left: 360, width: 360, textAlign: "center", fontFamily: MONO, opacity: p(en(3, "noventa"), 8) }}>
          <div style={{ fontSize: 30, color: K.tenue }}>nueva oferta</div>
          <div style={{ fontSize: 120, fontWeight: 700, color: K.amarillo, marginTop: 10 }}>$90</div>
          <div style={{ marginTop: 50, border: `3px solid ${K.verde}`, borderRadius: 18, padding: "14px 0", fontSize: 36, color: K.verde }}>Aceptar</div>
        </div>
        <Frase texto="¿SUENA BIEN?" desde={en(3, "Suena")} color={K.amarillo} />
      </>
    );
  }
  if (i === 4) {
    const corte = 140 + (5 / 15) * 800;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Cota x1={140} x2={corte} y={860} p={p(en(4, "recoger"), 14)} color={K.magenta} punteada />
          <Cota x1={corte} x2={940} y={860} p={p(en(4, "viaje"), 16)} color={K.cian} />
        </svg>
        <div style={{ position: "absolute", top: 900, left: 140, width: corte - 140, textAlign: "center", fontFamily: MONO, fontSize: 34, color: K.magenta, opacity: p(en(4, "recoger"), 8) }}>5 km</div>
        <div style={{ position: "absolute", top: 900, left: corte, width: 940 - corte, textAlign: "center", fontFamily: MONO, fontSize: 34, color: K.cian, opacity: p(en(4, "viaje"), 8) }}>10 km</div>
        <Numero texto="15 km" color={K.azul} tam={170} top={500} a={p(en(4, "quince"), 8)} />
        <Frase texto="LO QUE MANEJAS" sub="recogida + viaje" desde={en(4, "quince")} color={K.azul} />
      </>
    );
  }
  if (i === 5) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform="translate(270 300) scale(0.5)">
            <Trazo d={BIDON} p={p(d0, 14)} color={K.rosa} />
          </g>
        </svg>
        <Numero texto="−$30" color={K.rosa} tam={200} top={760} a={p(d0, 8)} />
        <Etiqueta texto="15 km × $2" top={990} a={p(d0 + 8, 10)} />
        <Frase texto="DIRECTO AL TANQUE" desde={en(5, "tanque!")} color={K.rosa} />
      </>
    );
  }
  if (i === 6) {
    const t = en(6, "tercio");
    return (
      <>
        <Numero texto="$90" color={K.amarillo} tam={150} top={300} a={p(d0, 8)} tachar={p(en(6, "sesenta.") - 6, 8)} />
        <Numero texto="$60" color={K.verde} tam={220} top={480} a={p(en(6, "sesenta."), 8)} />
        <div style={{ position: "absolute", top: 860, left: 160, right: 160, display: "flex", gap: 16 }}>
          {[0, 1, 2].map((k) => (
            <div key={k} style={{ flex: 1, height: 90, borderRadius: 14, border: `4px solid ${k === 0 ? K.naranja : K.verde}`, background: k === 0 ? `${K.naranja}${p(t, 10) > 0.5 ? "cc" : "22"}` : `${K.verde}22`, boxShadow: k === 0 && p(t, 10) > 0.5 ? `0 0 24px ${K.naranja}` : undefined, opacity: p(d0 + k * 3, 8) }} />
          ))}
        </div>
        <Etiqueta texto="1 de 3 partes: gasolina" top={980} a={p(t, 10)} color={K.naranja} />
        <Frase texto="UN TERCIO SE FUE" desde={t} color={K.naranja} />
      </>
    );
  }
  if (i === 7) {
    const filas: [string, string, string, number][] = [
      ["oferta", "$90", K.amarillo, d0 + 4],
      ["km totales", "15", K.azul, en(7, "resta") - 6],
      ["gasolina", "−$30", K.rosa, en(7, "resta")],
      ["te quedan", "$60", K.verde, en(7, "aceptes,") - 4],
    ];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M220 420 L860 420 L860 1150 L800 1120 L740 1150 L680 1120 L620 1150 L560 1120 L500 1150 L440 1120 L380 1150 L320 1120 L260 1150 L220 1120 Z" p={p(d0, 16)} relleno={K.relleno} />
          <line x1={280} y1={850} x2={800} y2={850} stroke={K.tenue} strokeWidth={3} strokeDasharray="10 10" opacity={p(en(7, "aceptes,") - 4, 8)} />
        </svg>
        <div style={{ position: "absolute", top: 470, left: 280, width: 520, fontFamily: MONO, fontSize: 30, color: K.tenue, textAlign: "center", opacity: p(d0, 8) }}>checa · la resta</div>
        {filas.map(([l, v, c, d], k) => (
          <div key={l} style={{ position: "absolute", top: 570 + k * 90 + (k === 3 ? 60 : 0), left: 280, width: 520, display: "flex", justifyContent: "space-between", fontFamily: MONO, fontSize: 46, color: c, opacity: ap(frame, d, 8) }}>
            <span>{l}</span>
            <span style={{ fontWeight: 700 }}>{v}</span>
          </div>
        ))}
        <Frase texto="ANTES DE ACEPTAR" desde={en(7, "antes")} color={K.verde} />
      </>
    );
  }
  if (i === 8) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform="translate(185 -120)">
            <Trazo d={CARRO} p={p(d0, 14)} relleno={K.relleno} />
            <Trazo d={RUEDAS} p={p(d0 + 4, 12)} relleno={K.relleno} />
          </g>
        </svg>
        <div style={{ position: "absolute", top: 1000, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 30 }}>
          {[["12 km/l", K.cian], ["$24 / l", K.amarillo]].map(([t, c], k) => (
            <div key={t} style={{ fontFamily: MONO, fontWeight: 700, fontSize: 50, color: c, border: `4px solid ${c}`, borderRadius: 18, padding: "12px 28px", boxShadow: `0 0 18px ${c}55`, opacity: p(en(8, "rendimiento") + k * 5, 8) }}>{t}</div>
          ))}
        </div>
        <Frase texto="RENDIMIENTO REAL" sub="lo ajustas a tu carro" desde={en(8, "rendimiento")} color={K.cian} />
      </>
    );
  }
  if (i === 9) {
    const g = p(d0, 8);
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: g, transform: `scale(${0.9 + 0.1 * g})` }}>
          <div style={{ padding: 18, border: `4px solid ${K.amarillo}`, borderRadius: 56, boxShadow: `0 0 ${40 * p(en(9, "conviene"), 6)}px ${K.amarillo}` }}>
            <Popup
              ancho={860}
              nivel="amarillo"
              app="App"
              exacto
              neta="60.00"
              viaje="90.00"
              gas="30.00"
              fichas={[
                { v: "$193/h", l: "meta $150", c: "ok" },
                { v: "$6.00/km", l: "meta $8", c: "mal" },
                { v: "28 min", l: "15.0 km total", c: "neutro" },
              ]}
            />
          </div>
        </div>
        <Frase texto="¿CONVIENE O NO?" sub="cumple 1 de 2 metas: considerar" desde={en(9, "conviene")} color={K.amarillo} />
      </>
    );
  }
  if (i === 10) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={BILLETE} p={p(d0, 16)} color={K.verde} relleno={K.relleno} />
        </svg>
        <div style={{ position: "absolute", top: 770, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 90, color: K.verde, opacity: p(d0 + 10, 8) }}>$</div>
        <Frase texto="CUIDA TU GANANCIA" desde={en(10, "ganancia.")} color={K.verde} />
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
  ochocientos: K.amarillo, reserva: K.rosa, dos: K.amarillo, gasolina: K.naranja, noventa: K.amarillo, quince: K.azul, treinta: K.rosa, tanque: K.rosa,
  sesenta: K.verde, tercio: K.naranja, checa: K.verde, resta: K.cian, rendimiento: K.cian, conviene: K.amarillo, ganancia: K.verde, siete: K.verde, días: K.verde, prueba: K.verde,
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

export const Gasolina1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Toma esc={esc}>
      <Escena />
    </Toma>
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`gasolina/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
    <Sfx en={en(0, "ochocientos")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(2, "dos")} src="pop.mp3" vol={0.12} />
    <Sfx en={esc[5].voz} src="golpe.wav" vol={0.16} />
    <Sfx en={en(6, "sesenta.")} src="pop.mp3" vol={0.14} />
    <Sfx en={esc[9].desde} src="ding.wav" vol={0.13} />
    <Sfx en={esc[11].desde} src="ding.wav" vol={0.12} />
  </AbsoluteFill>
);
