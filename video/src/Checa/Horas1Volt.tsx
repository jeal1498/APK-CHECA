import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasHoras";

// Tema 4 «Horas muertas, no saber a qué hora salir» (Mejores horas), estilo 1Volt a color. Guion de Gemini.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_HORAS_1VOLT = duracion;
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




// Ejemplo de semáforo por hora (meta $8/km): gris < 3 ofertas, rojo < 85 %, amarillo ≥ 85 %, verde ≥ meta
const COLOR_HORA = (h: number) => (h < 6 ? "#4B5168" : h < 9 ? K.amarillo : h < 12 ? K.verde : h < 17 ? "#F43F5E" : h < 22 ? K.verde : K.amarillo);
const RELOJ = "M540 820 m-260 0 a260 260 0 1 0 520 0 a260 260 0 1 0 -520 0";
const OJO = "M260 820 Q540 560 820 820 Q540 1080 260 820 Z M540 820 m-90 0 a90 90 0 1 0 180 0 a90 90 0 1 0 -180 0";

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const d0 = esc[i].desde;
  const p = (desde: number, dur = 18) => ap(frame, desde, dur);

  if (i === 0) {
    const giro = interpolate(frame, [d0, en(0, "tres") + 6], [0, 90], { ...clamp, easing: Easing.out(Easing.cubic) });
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={RELOJ} p={p(d0, 16)} relleno={K.relleno} />
          {Array.from({ length: 12 }).map((_, k) => {
            const a = (k / 12) * Math.PI * 2;
            return <line key={k} x1={540 + Math.sin(a) * 220} y1={820 - Math.cos(a) * 220} x2={540 + Math.sin(a) * 245} y2={820 - Math.cos(a) * 245} stroke={TONO[0].linea} strokeWidth={6} opacity={p(d0 + 4, 10)} />;
          })}
          <line x1={540} y1={820} x2={540 + Math.sin((giro * Math.PI) / 180) * 140} y2={820 - Math.cos((giro * Math.PI) / 180) * 140} stroke={K.amarillo} strokeWidth={12} strokeLinecap="round" />
          <line x1={540} y1={820} x2={540} y2={620} stroke={K.texto} strokeWidth={7} strokeLinecap="round" opacity={p(d0 + 4, 8)} />
        </svg>
        <Numero texto="3:00 pm" color={K.amarillo} tam={130} top={300} a={p(en(0, "tres"), 8)} />
        <Frase texto="HORA DE SALIR" desde={en(0, "tarde")} color={K.naranja} />
      </>
    );
  }
  if (i === 1) {
    const min = Math.round(interpolate(frame, [d0, esc[1].hasta], [0, 120], clamp));
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M360 500 Q360 450 410 450 L670 450 Q720 450 720 500 L720 1150 Q720 1200 670 1200 L410 1200 Q360 1200 360 1150 Z" p={p(d0, 14)} relleno={K.relleno} />
          {[0, 1, 2].map((k) => (
            <circle key={k} cx={500 + k * 40} cy={830} r={12} fill={K.tenue} opacity={0.3 + 0.7 * Math.abs(Math.sin(frame / 8 + k))} />
          ))}
        </svg>
        <div style={{ position: "absolute", top: 880, left: 360, width: 360, textAlign: "center", fontFamily: MONO, fontSize: 30, color: K.tenue }}>sin ofertas</div>
        <Numero texto={`${Math.floor(min / 60)}:${String(min % 60).padStart(2, "0")} h`} color={K.rosa} tam={130} top={230} a={p(d0, 6)} />
        <Frase texto="NADA QUE VALGA" sub="la pena" desde={en(1, "nada")} color={K.rosa} />
      </>
    );
  }
  if (i === 2) {
    const t = ((frame - d0) / 70) * Math.PI * 2;
    const nivel = interpolate(frame, [d0, esc[2].hasta], [0.8, 0.35], clamp);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M300 560 L780 560 L780 1040 L300 1040 Z" p={p(d0, 14)} relleno={K.relleno} />
          <rect x={250} y={510} width={580} height={580} rx={40} fill="none" stroke={`${K.azul}55`} strokeWidth={30} />
          <circle cx={540 + Math.cos(t) * 290} cy={800 + Math.sin(t) * 290} r={22} fill={K.amarillo} style={{ filter: `drop-shadow(0 0 12px ${K.amarillo})` }} />
          <rect x={880} y={1040 - 300 * nivel} width={60} height={300 * nivel} fill={K.naranja} opacity={0.7} />
          <rect x={880} y={740} width={60} height={300} fill="none" stroke={K.naranja} strokeWidth={4} />
        </svg>
        <Frase texto="DANDO VUELTAS" sub="gastando gasolina" desde={en(2, "gasolina")} color={K.naranja} />
      </>
    );
  }
  if (i === 3) {
    const caida = interpolate((frame - d0) % 60, [0, 60], [0, 1]);
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d="M380 520 L700 520 L560 800 L700 1080 L380 1080 L520 800 Z" p={p(d0, 16)} relleno={K.relleno} />
          <path d={`M${420 + 100 * caida} 560 L${660 - 100 * caida} 560 L540 ${780 - 0 * caida} Z`} fill={K.amarillo} opacity={0.6} />
          <path d={`M540 1060 L${540 - 120 * caida} 1060 L540 ${1060 - 160 * caida} L${540 + 120 * caida} 1060 Z`} fill={K.amarillo} opacity={0.6} />
          <line x1={540} y1={800} x2={540} y2={1060} stroke={K.amarillo} strokeWidth={4} strokeDasharray="8 10" />
        </svg>
        <Frase texto="ESPERANDO" sub="el viaje bueno" desde={d0 + 4} color={K.amarillo} />
      </>
    );
  }
  if (i === 4) {
    return (
      <>
        <Numero texto="15:00" color={K.texto} tam={110} top={420} a={p(d0, 8)} />
        <Numero texto="$6/km" color="#F43F5E" tam={210} top={600} a={p(en(4, "seis"), 8)} />
        <Etiqueta texto="números de ejemplo" top={880} a={p(en(4, "seis"), 10)} />
        <Frase texto="A ESA HORA" desde={d0 + 4} color="#F43F5E" />
      </>
    );
  }
  if (i === 5) {
    const col = (h: string, v: string, c: string, d: number, x: number) => (
      <div style={{ position: "absolute", top: 420, left: x, width: 420, textAlign: "center", fontFamily: MONO, opacity: ap(frame, d, 8) }}>
        <div style={{ fontSize: 70, color: K.texto }}>{h}</div>
        <div style={{ marginTop: 30, fontSize: 120, fontWeight: 700, color: c, border: `5px solid ${c}`, borderRadius: 28, padding: "30px 0", boxShadow: `0 0 30px ${c}66` }}>{v}</div>
      </div>
    );
    return (
      <>
        {col("15:00", "$6", "#F43F5E", d0, 90)}
        {col("19:00", "$11", K.verde, en(5, "once"), 570)}
        <Etiqueta texto="por km · números de ejemplo" top={880} a={p(en(5, "once"), 10)} />
        <Frase texto="SUBEN A $11" desde={en(5, "once")} color={K.verde} />
      </>
    );
  }
  if (i === 6 || i === 7) {
    return (
      <>
        <div style={{ position: "absolute", top: 760, left: 60, right: 60, display: "flex", gap: 6 }}>
          {Array.from({ length: 24 }).map((_, h) => {
            const on = i === 7 || frame >= d0 + h * 1.5;
            return <div key={h} style={{ flex: 1, height: 160, borderRadius: 8, background: on ? COLOR_HORA(h) : "#1A1A2E", boxShadow: on && i === 6 ? `0 0 12px ${COLOR_HORA(h)}88` : undefined }} />;
          })}
        </div>
        {[0, 6, 12, 18, 24].map((h) => (
          <div key={h} style={{ position: "absolute", top: 940, left: 60 + (h / 24) * 960 - 30, width: 60, textAlign: "center", fontFamily: MONO, fontSize: 28, color: K.tenue }}>{h}h</div>
        ))}
        {i === 7
          ? Array.from({ length: 14 }).map((_, k) => {
              const h = (k * 7 + 3) % 24;
              const t0 = d0 + k * 4;
              const y = interpolate(frame, [t0, t0 + 14], [300, 820], { ...clamp, easing: Easing.in(Easing.quad) });
              return <div key={k} style={{ position: "absolute", left: 60 + ((h + 0.5) / 24) * 960 - 14, top: y, width: 28, height: 28, borderRadius: 14, background: K.texto, opacity: frame < t0 ? 0 : frame > t0 + 16 ? 0 : 1, boxShadow: `0 0 14px ${K.texto}` }} />;
            })
          : null}
        {i === 6 ? <Frase texto="LA HORA DEFINE" sub="cuánto te llevas a la bolsa" desde={en(6, "define")} color={K.azul} /> : <Frase texto="CADA OFERTA CUENTA" sub="checa la guarda sola" desde={d0 + 4} color={K.verde} />}
      </>
    );
  }
  if (i === 8) {
    return (
      <>
        <div style={{ position: "absolute", top: 380, left: 150, right: 150, display: "grid", gridTemplateColumns: "repeat(6, 1fr)", gap: 14 }}>
          {Array.from({ length: 24 }).map((_, h) => (
            <div key={h} style={{ height: 110, borderRadius: 16, background: `${COLOR_HORA(h)}33`, border: `4px solid ${COLOR_HORA(h)}`, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: MONO, fontSize: 34, fontWeight: 700, color: COLOR_HORA(h), opacity: ap(frame, d0 + h, 6), transform: `scale(${0.8 + 0.2 * ap(frame, d0 + h, 6)})` }}>
              {h}
            </div>
          ))}
        </div>
        <div style={{ position: "absolute", top: 940, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 24, fontFamily: MONO, fontSize: 30 }}>
          {[["verde", K.verde, en(8, "verde,")], ["amarillo", K.amarillo, en(8, "amarillo")], ["rojo", "#F43F5E", en(8, "rojo.")]].map(([t, c, d]) => (
            <span key={t as string} style={{ color: c as string, opacity: ap(frame, d as number, 6) }}>● {t}</span>
          ))}
        </div>
        <Frase texto="TUS MEJORES HORAS" sub="con tus propias ofertas" desde={en(8, "semáforo")} color={K.verde} />
      </>
    );
  }
  if (i === 9) {
    const ficha = (titulo: string, h: string, v: string, c: string, nota: string, d: number, top: number) => (
      <div style={{ position: "absolute", top, left: 140, right: 140, border: `5px solid ${c}`, borderRadius: 30, padding: "28px 40px", fontFamily: MONO, background: `${c}14`, boxShadow: `0 0 26px ${c}44`, opacity: ap(frame, d, 8), transform: `translateY(${(1 - ap(frame, d, 8)) * 30}px)` }}>
        <div style={{ fontSize: 30, color: K.tenue }}>{titulo}</div>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline", marginTop: 10 }}>
          <span style={{ fontSize: 64, fontWeight: 700, color: K.texto }}>{h}</span>
          <span style={{ fontSize: 64, fontWeight: 700, color: c }}>{v}</span>
        </div>
        <div style={{ fontSize: 32, color: c, marginTop: 8 }}>{nota}</div>
      </div>
    );
    return (
      <>
        {ficha("Ahora", "15:00", "$6/km", "#F43F5E", "descansa", en(9, "descansar.") - 12, 420)}
        {ficha("Lo mejor que viene", "19:00", "$11/km", K.verde, "› sal a manejar", en(9, "salir"), 760)}
        <Frase texto="SALIR O DESCANSAR" desde={en(9, "conviene")} color={K.cian} />
      </>
    );
  }
  if (i === 10) {
    return (
      <>
        <svg width={1080} height={1920} style={{ position: "absolute" }}>
          <Trazo d={OJO} p={p(d0, 14)} color={K.magenta} relleno={K.relleno} />
          <line x1={300} y1={1060} x2={780} y2={580} stroke={K.magenta} strokeWidth={14} strokeLinecap="round" strokeDasharray="700" strokeDashoffset={700 * (1 - p(en(10, "ciegas."), 8))} style={{ filter: `drop-shadow(0 0 12px ${K.magenta})` }} />
        </svg>
        <Frase texto="NO MANEJES A CIEGAS" desde={d0} color={K.magenta} />
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
  tres: K.amarillo, dos: K.rosa, nada: K.rosa, gasolina: K.naranja, bueno: K.amarillo, seis: "#F43F5E", siete: K.verde, once: K.verde, define: K.azul, bolsa: K.verde,
  checa: K.verde, semáforo: K.cian, verde: K.verde, amarillo: K.amarillo, rojo: "#F43F5E", salir: K.verde, descansar: K.cian, ciegas: K.magenta, días: K.verde, prueba: K.verde,
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

export const Horas1Volt: React.FC = () => (
  <AbsoluteFill style={{ background: K.fondo }}>
    <Fondo />
    <Escena />
    <Subs />
    {LINEAS.map((l, k) => (
      <Sequence key={l.archivo} from={esc[k].voz}>
        <Audio src={staticFile(`horas/${l.archivo}.mp3`)} />
      </Sequence>
    ))}
    <Sfx en={en(0, "tres")} src="pop.mp3" vol={0.12} />
    <Sfx en={en(4, "seis")} src="error.wav" vol={0.1} />
    <Sfx en={en(5, "once")} src="ding.wav" vol={0.12} />
    <Sfx en={esc[8].desde} src="pop.mp3" vol={0.12} />
    <Sfx en={esc[11].desde} src="ding.wav" vol={0.12} />
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
  </AbsoluteFill>
);
