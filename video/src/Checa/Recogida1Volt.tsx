import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { POPUP } from "./Recogida";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasRecogida2";

// «Recogidas lejanas» en el estilo 1Volt1Amp: line-art lavanda sobre azul noche, letra mono,
// una idea por pantalla, cotas punteadas, rejilla de puntos y frase clave en mayúsculas. Misma voz que Recogida2.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_RECOGIDA_1VOLT = duracion;
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
  const c = color ?? TONO[escena(frame)].linea;
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
const PIN = (x: number, y: number) => `M${x} ${y} C${x - 50} ${y - 70} ${x - 50} ${y - 130} ${x} ${y - 130} C${x + 50} ${y - 130} ${x + 50} ${y - 70} ${x} ${y} Z`;

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={CARRO} p={p(d0, 16)} relleno={K.relleno} />
          <Trazo d={RUEDAS} p={p(d0 + 4, 14)} relleno={K.relleno} />
          <Trazo d={PIN(930, 960)} p={p(en(0, "pasaje?") - 8, 12)} color={K.magenta} relleno={K.relleno} />
          <circle cx={930} cy={892} r={16} fill={K.magenta} opacity={p(en(0, "pasaje?"), 6)} />
          <Cota x1={610} x2={900} y={1060} p={p(en(0, "seis"), 20)} color={K.magenta} punteada />
        </svg>
        <Numero texto="6 km" color={K.magenta} top={430} a={p(en(0, "seis"), 10)} />
        <Etiqueta texto="solo para ir por el pasaje" top={650} a={p(en(0, "pasaje?"), 10)} />
        <Frase texto="RECOGIDA" sub="nadie te la paga" desde={en(0, "pasaje?")} color={K.magenta} />
      </>
    );
  }
  if (i === 1) {
    const pesos = interpolate(frame, [esc[1].voz, en(1, "gasolina.") + 6], [0, 12], clamp);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M390 1180 L390 640 Q390 600 430 600 L650 600 Q690 600 690 640 L690 1180 Z" p={p(d0, 16)} relleno={K.relleno} />
          <Trazo d="M430 660 L650 660 L650 820 L430 820 Z" p={p(d0 + 6, 12)} />
          <Trazo d="M690 720 L760 720 Q800 720 800 760 L800 1020 Q800 1060 840 1060 L870 1060" p={p(d0 + 10, 14)} />
          <Trazo d="M350 1180 L730 1180" p={p(d0, 10)} />
        </svg>
        <div style={{ position: "absolute", top: 710, left: 430, width: 220, textAlign: "center", fontFamily: MONO, fontWeight: 700, fontSize: 50, color: K.amarillo, opacity: p(d0 + 10, 6) }}>${pesos.toFixed(2)}</div>
        <Etiqueta texto="6 km × $2 por km" top={1230} a={p(en(1, "regalando"), 10)} />
        <Frase texto="GASOLINA REGALADA" desde={en(1, "regalando")} color={K.naranja} />
      </>
    );
  }
  if (i === 2) {
    const corte = 140 + (6 / 13) * 800;
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M380 260 Q380 220 420 220 L660 220 Q700 220 700 260 L700 860 Q700 900 660 900 L420 900 Q380 900 380 860 Z" p={p(d0, 16)} relleno={K.relleno} />
          <Cota x1={140} x2={corte} y={1120} p={p(en(2, "recogida"), 14)} color={K.magenta} punteada />
          <Cota x1={corte} x2={940} y={1120} p={p(en(2, "viaje"), 14)} color={K.cian} />
        </svg>
        <div style={{ position: "absolute", top: 330, left: 380, width: 320, textAlign: "center", fontFamily: MONO, opacity: p(en(2, "setenta"), 8) }}>
          <div style={{ fontSize: 28, color: K.tenue }}>nueva oferta</div>
          <div style={{ fontSize: 96, fontWeight: 700, color: K.amarillo, marginTop: 10 }}>$70</div>
          <div style={{ fontSize: 30, color: K.magenta, marginTop: 40 }}>● 6.0 km</div>
          <div style={{ fontSize: 30, color: K.cian, marginTop: 16 }}>■ 7.0 km</div>
        </div>
        <div style={{ position: "absolute", top: 1160, left: 140, width: corte - 140, textAlign: "center", fontFamily: MONO, fontSize: 32, color: K.magenta, opacity: p(en(2, "recogida"), 8) }}>6 km</div>
        <div style={{ position: "absolute", top: 1160, left: corte, width: 940 - corte, textAlign: "center", fontFamily: MONO, fontSize: 32, color: K.cian, opacity: p(en(2, "viaje"), 8) }}>7 km</div>
        <Numero texto="13 km" color={K.azul} tam={110} top={960} a={p(en(2, "trece"), 8)} />
        <Frase texto="LO QUE MANEJAS" sub="números de ejemplo" desde={en(2, "trece")} color={K.azul} />
      </>
    );
  }
  if (i === 3) {
    const pr = p(en(3, "mitad") - 4, 14);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          {Array.from({ length: 13 }).map((_, k) => {
            const fila = k < 7 ? 0 : 1;
            const col = fila ? k - 7 : k;
            const x = (fila ? 230 : 170) + col * 120;
            const y = 840 + fila * 140;
            const on = k < 6 ? pr * 6 > k : frame >= d0 + 4 + (k - 6) * 2;
            const c = k < 6 ? K.magenta : K.cian;
            return <circle key={k} cx={x} cy={y} r={40} fill={on ? c : K.relleno} stroke={on ? c : K.linea} strokeWidth={5} opacity={ap(frame, d0 + k, 6)} style={{ filter: on ? `drop-shadow(0 0 12px ${c})` : undefined }} />;
          })}
        </svg>
        <Numero texto="6 de 13" color={K.magenta} tam={150} top={450} a={p(en(3, "mitad"), 8)} />
        <Etiqueta texto="km de a gratis" top={630} a={p(en(3, "mitad"), 10)} />
        <Frase texto="CASI LA MITAD" desde={en(3, "mitad")} color={K.magenta} />
      </>
    );
  }
  if (i === 4) {
    const filas: [string, string, string, number][] = [
      ["recogida", "6 km", K.magenta, en(4, "recogida")],
      ["viaje", "7 km", K.cian, en(4, "recogida") + 6],
      ["total", "13 km", K.azul, en(4, "instante")],
      ["gasolina", "−$26", K.magenta, en(4, "gasolina")],
    ];
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M220 420 L860 420 L860 1180 L800 1150 L740 1180 L680 1150 L620 1180 L560 1150 L500 1180 L440 1150 L380 1180 L320 1150 L260 1180 L220 1150 Z" p={p(d0, 18)} relleno={K.relleno} />
          <line x1={280} y1={820} x2={800} y2={820} stroke={K.tenue} strokeWidth={3} strokeDasharray="10 10" opacity={p(en(4, "instante"), 8)} />
        </svg>
        <div style={{ position: "absolute", top: 470, left: 280, width: 520, fontFamily: MONO, fontSize: 30, color: K.tenue, textAlign: "center", opacity: p(d0, 8) }}>checa · la cuenta</div>
        {filas.map(([l, v, c, d], k) => (
          <div key={l} style={{ position: "absolute", top: 580 + k * 100 + (k >= 2 ? 40 : 0), left: 280, width: 520, display: "flex", justifyContent: "space-between", fontFamily: MONO, fontSize: 48, color: c, opacity: ap(frame, d, 8) }}>
            <span>{l}</span>
            <span style={{ fontWeight: 700 }}>{v}</span>
          </div>
        ))}
        <Frase texto="SUMA Y RESTA" sub="al instante, antes de aceptar" desde={en(4, "Checa")} color={K.cian} />
      </>
    );
  }
  if (i === 5) {
    const q = en(5, "cuarenta");
    return (
      <>
        <Numero texto="$70" color={K.amarillo} tam={170} top={420} a={p(d0, 8)} tachar={p(q - 10, 8)} />
        <Etiqueta texto="la oferta" top={610} a={p(d0, 8)} />
        <Numero texto="$44" color={K.cian} tam={260} top={780} a={p(q, 8)} />
        <Etiqueta texto="te quedan limpios" top={1050} a={p(q, 10)} color={K.cian} />
        <Frase texto="LO QUE TE QUEDA" desde={q} color={K.verde} />
      </>
    );
  }
  if (i === 6) {
    const r = en(6, "rechazar.");
    const g = p(d0, 8);
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: g, transform: `scale(${0.9 + 0.1 * g})` }}>
          <div style={{ padding: 18, border: `4px solid ${K.linea}`, borderRadius: 56, boxShadow: `0 0 ${50 * p(r, 6)}px ${K.magenta}` }}>{POPUP(860)}</div>
        </div>
        <Frase texto="✗ RECHAZAR" sub="13 km por $44 no conviene" desde={r} color={K.magenta} />
      </>
    );
  }
  if (i === 7) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M290 560 Q290 500 350 500 L730 500 Q790 500 790 560 L790 1000 Q790 1060 730 1060 L350 1060 Q290 1060 290 1000 Z" p={p(d0, 12)} relleno={K.relleno} />
          <g transform="translate(327 225) scale(0.6)">
            <Trazo d={CARRO} p={p(d0 + 4, 12)} />
            <Trazo d={RUEDAS} p={p(d0 + 6, 10)} />
          </g>
          <line x1={330} y1={1020} x2={750} y2={540} stroke={K.magenta} strokeWidth={14} strokeLinecap="round" strokeDasharray="700" strokeDashoffset={700 * (1 - p(en(7, "regalar"), 8))} style={{ filter: `drop-shadow(0 0 12px ${K.magenta})` }} />
        </svg>
        <Frase texto="DEJA DE REGALAR" sub="tu trabajo" desde={d0} color={K.magenta} />
      </>
    );
  }
  return (
    <AbsoluteFill style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 50, paddingBottom: 300, opacity: p(d0, 8) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontSize: 46, color: K.cian, opacity: p(en(8, "siete"), 8) }}><span style={{ fontFamily: "Inter" }}>✓</span> 7 días de prueba sin costo</div>
      <div style={{ fontFamily: MONO, fontSize: 34, color: K.tenue, opacity: p(en(8, "link"), 8) }}>link en el perfil</div>
    </AbsoluteFill>
  );
};

const CLAVE: Record<string, string> = {
  seis: K.magenta, gasolina: K.naranja, regalando: K.rosa, setenta: K.amarillo, trece: K.azul, mitad: K.magenta, gratis: K.rosa,
  checa: K.verde, cuarenta: K.cian, cuatro: K.cian, limpios: K.cian, rechazar: "#F43F5E", regalar: K.rosa, siete: K.verde, días: K.verde, prueba: K.verde,
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
  const prev = TONO[Math.max(0, k - 1)].fondo;
  return (
    <>
      <AbsoluteFill style={{ background: `radial-gradient(ellipse at 50% 40%, ${prev} 0%, ${K.fondo} 75%)` }} />
      <AbsoluteFill style={{ opacity: a, background: `radial-gradient(ellipse at 50% 40%, ${TONO[k].fondo} 0%, ${K.fondo} 75%)` }} />
    </>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol: number }> = ({ en: e, src, vol }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

export const Recogida1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`recogida2/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
    <Sfx en={en(0, "seis")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(3, "mitad")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(5, "cuarenta")} src="pop.mp3" vol={0.14} />
    <Sfx en={en(6, "rechazar.")} src="error.wav" vol={0.12} />
    <Sfx en={esc[8].desde} src="ding.wav" vol={0.12} />
  </AbsoluteFill>
);
