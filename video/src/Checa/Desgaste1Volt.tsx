import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasDesgaste";

// Tema 8 «Desgaste del carro / renta semanal», estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_DESGASTE_1VOLT = duracion;
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


const ROJO = "#F43F5E";
const RELOJ = "M540 820 m-220 0 a220 220 0 1 0 440 0 a220 220 0 1 0 -440 0";
const LLAVE = "M330 820 m-110 0 a110 110 0 1 0 220 0 a110 110 0 1 0 -220 0 M440 820 L820 820 L820 900 M740 820 L740 880";
const TELEFONO = "M360 360 Q360 310 410 310 L670 310 Q720 310 720 360 L720 1110 Q720 1160 670 1160 L410 1160 Q360 1160 360 1110 Z";
const ALCANCIA = "M300 820 Q300 600 540 600 Q780 600 780 820 Q780 1000 640 1030 L640 1100 L560 1100 L560 1040 L520 1040 L520 1100 L440 1100 L440 1030 Q300 1000 300 820 Z M480 640 L600 640";

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={RELOJ} p={p(d0, 14)} relleno={K.relleno} />
          <line x1={540} y1={820} x2={540 + Math.sin((150 * Math.PI) / 180) * 120} y2={820 - Math.cos((150 * Math.PI) / 180) * 120} stroke={K.amarillo} strokeWidth={12} strokeLinecap="round" opacity={p(d0 + 6, 8)} />
          <line x1={540} y1={820} x2={540} y2={640} stroke={K.texto} strokeWidth={7} strokeLinecap="round" opacity={p(d0 + 6, 8)} />
        </svg>
        <div style={{ position: "absolute", top: 1060, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontSize: 44, color: K.amarillo, opacity: p(d0 + 6, 8) }}>5:00 am</div>
        <Numero texto="−$500" color={ROJO} tam={170} top={300} a={p(en(0, "quinientos"), 8)} />
        <Frase texto="¿DESPERTAR DEBIENDO?" desde={en(0, "chinga.") - 4} color={ROJO} />
      </>
    );
  }
  if (i === 1) {
    const parpadeo = Math.floor((frame - d0) / 8) % 2 === 0 ? 1 : 0.45;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={LLAVE} p={p(d0, 16)} color={K.naranja} />
        </svg>
        <div style={{ position: "absolute", top: 340, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 150, color: ROJO, opacity: p(d0, 6) * parpadeo, textShadow: `0 0 40px ${ROJO}` }}>−$500</div>
        <Etiqueta texto="renta: $3,000 ÷ 6 días · números de ejemplo" top={540} a={p(d0 + 6, 10)} />
        <Frase texto="NÚMEROS ROJOS" sub="antes de ganar un peso" desde={en(1, "rojos")} color={ROJO} />
      </>
    );
  }
  if (i === 2) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform="translate(185 -120)">
            <Trazo d={CARRO} p={p(d0, 14)} relleno={K.relleno} />
            <Trazo d={RUEDAS} p={p(d0 + 4, 12)} relleno={K.relleno} />
          </g>
        </svg>
        <Frase texto="SI EL AUTO ES TUYO" sub="no te engañes" desde={d0 + 4} color={TONO[2].linea} />
      </>
    );
  }
  if (i === 3) {
    const piezas: [string, string, number][] = [
      ["llantas", K.naranja, en(3, "llantas,")],
      ["frenos", K.rosa, en(3, "frenos")],
      ["aceite", K.amarillo, en(3, "aceite.")],
    ];
    return (
      <>
        {piezas.map(([t, c, d], k) => {
          const nivel = interpolate(frame, [d, d + 25], [1, 0.25], clamp);
          return (
            <div key={t} style={{ position: "absolute", top: 380, left: 130 + k * 290, width: 240, textAlign: "center", fontFamily: MONO, opacity: ap(frame, d0 + k * 4, 8) }}>
              <svg width={240} height={240}>
                {k === 0 ? <><circle cx={120} cy={120} r={100} fill="none" stroke={c} strokeWidth={8} /><circle cx={120} cy={120} r={45} fill="none" stroke={c} strokeWidth={6} /></> : null}
                {k === 1 ? <><circle cx={120} cy={120} r={95} fill="none" stroke={c} strokeWidth={8} /><path d="M60 70 Q120 30 180 70 L170 95 Q120 65 70 95 Z" fill={`${c}55`} stroke={c} strokeWidth={5} /></> : null}
                {k === 2 ? <path d="M120 20 Q190 120 190 160 Q190 220 120 220 Q50 220 50 160 Q50 120 120 20 Z" fill="none" stroke={c} strokeWidth={8} /> : null}
              </svg>
              <div style={{ marginTop: 20, height: 40, borderRadius: 10, border: `4px solid ${c}` }}>
                <div style={{ width: `${nivel * 100}%`, height: "100%", background: nivel < 0.4 ? ROJO : c }} />
              </div>
              <div style={{ marginTop: 16, fontSize: 38, color: c }}>{t}</div>
            </div>
          );
        })}
        <Frase texto="LE SACA LA VIDA" sub="cada kilómetro de más" desde={en(3, "vida")} color={K.naranja} />
      </>
    );
  }
  if (i === 4) {
    const rompe = p(en(4, "doble:"), 10);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <g transform={`translate(${-rompe * 30} ${rompe * 20}) rotate(${-rompe * 6} 340 700)`}>
            <Cota x1={140} x2={530} y={700} p={p(d0, 12)} color={K.magenta} />
          </g>
          <g transform={`translate(${rompe * 30} ${rompe * 20}) rotate(${rompe * 6} 740 700)`}>
            <Cota x1={550} x2={940} y={700} p={p(d0 + 6, 12)} color={K.magenta} />
          </g>
        </svg>
        <div style={{ position: "absolute", top: 820, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 30 }}>
          {[["GASOLINA", K.naranja, en(4, "gasolina")], ["DESGASTE", ROJO, en(4, "desgaste.")]].map(([t, c, d]) => (
            <div key={t as string} style={{ fontFamily: MONO, fontWeight: 700, fontSize: 52, color: c as string, border: `4px solid ${c}`, borderRadius: 14, padding: "10px 28px", opacity: ap(frame, d as number, 6), boxShadow: `0 0 20px ${c}66` }}>{t}</div>
          ))}
        </div>
        <Frase texto="PIERDES DOBLE" desde={en(4, "doble:")} color={ROJO} />
      </>
    );
  }
  if (i === 5 || i === 6) {
    const y = interpolate((frame - esc[5].desde) % 45, [0, 45], [380, 1090]);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={TELEFONO} p={p(esc[5].desde, 12)} relleno={K.relleno} />
          {i === 5 ? <line x1={370} y1={y} x2={710} y2={y} stroke={K.verde} strokeWidth={6} style={{ filter: `drop-shadow(0 0 16px ${K.verde})` }} /> : null}
        </svg>
        <div style={{ position: "absolute", top: 450, left: 360, width: 360, textAlign: "center", fontFamily: MONO }}>
          <div style={{ fontSize: 28, color: K.tenue }}>nueva oferta</div>
          <div style={{ fontSize: 110, fontWeight: 700, color: K.amarillo }}>$130</div>
          <div style={{ fontSize: 30, color: K.magenta, marginTop: 30 }}>● 3.0 km recogida</div>
          <div style={{ fontSize: 30, color: K.cian, marginTop: 14 }}>■ 10.0 km viaje</div>
        </div>
        {i === 6 ? (
          <div style={{ position: "absolute", top: 830, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 24 }}>
            {[["13.0 km total", K.azul, en(6, "kilómetros")], ["$10.00/km", K.verde, en(6, "pago")]].map(([t, c, d]) => (
              <div key={t as string} style={{ fontFamily: MONO, fontWeight: 700, fontSize: 50, color: c as string, background: "#05060F", border: `5px solid ${c}`, borderRadius: 18, padding: "14px 26px", opacity: ap(frame, d as number, 6), transform: `scale(${1.2 - 0.2 * ap(frame, d as number, 6)})`, boxShadow: `0 0 26px ${c}88` }}>{t}</div>
            ))}
          </div>
        ) : null}
        <Frase texto={i === 5 ? "ESCANEA LA OFERTA" : "KM REALES Y $/KM"} sub={i === 5 ? "al instante" : "con la recogida"} desde={i === 5 ? en(5, "escanea") : en(6, "kilómetros")} color={i === 5 ? K.verde : K.azul} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M390 1150 L390 700 Q390 660 430 660 L650 660 Q690 660 690 700 L690 1150 Z" p={p(d0, 14)} relleno={K.relleno} />
          <Trazo d="M430 720 L650 720 L650 860 L430 860 Z" p={p(d0 + 6, 10)} />
        </svg>
        <div style={{ position: "absolute", top: 760, left: 430, width: 220, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 50, color: K.naranja, opacity: p(d0 + 8, 6) }}>−$26</div>
        <Etiqueta texto="13 km × $2 · ya descontada" top={1200} a={p(en(7, "automático."), 8)} color={K.naranja} />
        <Frase texto="EN AUTOMÁTICO" sub="la gasolina" desde={en(7, "calcula")} color={K.naranja} />
      </>
    );
  }
  if (i === 8) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={ALCANCIA} p={p(d0, 16)} color={K.verde} relleno={K.relleno} />
          <circle cx={540} cy={520 + ((frame - d0) % 30) * 4} r={30} fill="none" stroke={K.amarillo} strokeWidth={6} opacity={frame - d0 > 10 ? 1 : 0} />
        </svg>
        <div style={{ position: "absolute", top: 800, left: 0, right: 0, textAlign: "center", fontFamily: MONO, fontSize: 40, color: K.verde, opacity: p(d0 + 8, 8) }}>mantenimiento <span style={{ fontFamily: "Inter" }}>✓</span></div>
        <Frase texto="APÁRTALO TÚ" sub="cada semana" desde={en(8, "apártalo")} color={K.verde} />
      </>
    );
  }
  if (i === 9) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M290 560 Q290 500 350 500 L730 500 Q790 500 790 560 L790 1000 Q790 1060 730 1060 L350 1060 Q290 1060 290 1000 Z" p={p(d0, 12)} relleno={K.relleno} />
          <g transform="translate(327 225) scale(0.6)">
            <Trazo d={CARRO} p={p(d0 + 4, 12)} />
            <Trazo d={RUEDAS} p={p(d0 + 6, 10)} />
          </g>
          <line x1={330} y1={1020} x2={750} y2={540} stroke={K.magenta} strokeWidth={14} strokeLinecap="round" strokeDasharray="700" strokeDashoffset={700 * (1 - p(en(9, "regalar"), 8))} style={{ filter: `drop-shadow(0 0 12px ${K.magenta})` }} />
        </svg>
        <Frase texto="DEJA DE REGALAR" sub="el desgaste de tu carro" desde={d0} color={K.magenta} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(10, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días de prueba sin costo</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(10, "enlace"), 8) }}>enlace en el perfil ↓</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  quinientos: "#F43F5E", chinga: "#F43F5E", rojos: "#F43F5E", tuyo: K.azul, llantas: K.naranja, frenos: K.rosa, aceite: K.amarillo, doble: "#F43F5E", gasolina: K.naranja, desgaste: "#F43F5E",
  checa: K.verde, escanea: K.verde, kilómetros: K.azul, reales: K.azul, pago: K.verde, automático: K.naranja, mantenimiento: K.verde, apártalo: K.verde, regalar: K.magenta, siete: K.verde, días: K.verde, prueba: K.verde,
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

export const Desgaste1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`desgaste/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(0, "quinientos")} src="error.wav" vol={0.08} />
    <Sfx en={en(4, "doble:")} src="golpe.wav" vol={0.14} />
    <Sfx en={en(6, "kilómetros")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(8, "apártalo")} src="ding.wav" vol={0.12} />
    <Sfx en={esc[10].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
