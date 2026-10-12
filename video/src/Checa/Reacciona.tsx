import React from "react";
import { AbsoluteFill, Audio, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { C, F, FD, Fondo, Logo, Oferta, Popup, Ficha, Nivel } from "./ui";
import { LINEAS, Linea } from "./lineas1";

// «El Checa reacciona a ofertas» · episodio 1 · ~31 s a 30 fps
const FPS = 30;
const MARCA = { verde: "#22C55E", amarillo: "#FACC15", rojo: "#F43F5E", apagado: "#3A4541" };
const INK = "#0B0F0D";

type Animo = "feliz" | "duda" | "enojo" | "neutro";
type Escena = { desde: number; hasta: number; voz: number; linea: number };

// Escenas seguidas, medidas con la duración de cada voz. La 1ª arranca sin pausa: es el gancho.
const ESC: Escena[] = [];
{
  let f = 0;
  LINEAS.forEach((l, i) => {
    const pausa = i === 0 ? 0 : i === LINEAS.length - 1 ? 12 : 10;
    const fin = f + pausa + Math.round(l.dur * FPS) + (i === LINEAS.length - 1 ? 42 : 17);
    ESC.push({ desde: f, hasta: fin, voz: f + pausa, linea: i });
    f = fin;
  });
}
export const DURACION_REACCIONA = ESC[ESC.length - 1].hasta;

const OFERTAS: {
  tarifa: string;
  tramos: [string, string];
  nivel: Nivel;
  animo: Animo;
  neta: number;
  viaje: number;
  gas: number;
  fichas: Ficha[];
  revela: string;
  sfx: string;
}[] = [
  {
    tarifa: "$80.00",
    tramos: ["12 min (6.0 km)", "22 min (8.0 km)"],
    nivel: "rojo",
    animo: "enojo",
    neta: 52,
    viaje: 80,
    gas: 28,
    fichas: [
      { v: "$141", l: "por hora", c: "mal" },
      { v: "$5.7", l: "por km", c: "mal" },
      { v: "34 min", l: "14 km total", c: "neutro" },
    ],
    revela: "¡Ni",
    sfx: "error.wav",
  },
  {
    tarifa: "$150.00",
    tramos: ["5 min (2.0 km)", "25 min (14.0 km)"],
    nivel: "verde",
    animo: "feliz",
    neta: 118,
    viaje: 150,
    gas: 32,
    fichas: [
      { v: "$300", l: "por hora", c: "ok" },
      { v: "$9.4", l: "por km", c: "ok" },
      { v: "30 min", l: "16 km total", c: "neutro" },
    ],
    revela: "¡Te",
    sfx: "ding.wav",
  },
  {
    tarifa: "$95.00",
    tramos: ["6 min (3.0 km)", "22 min (10.0 km)"],
    nivel: "amarillo",
    animo: "duda",
    neta: 69,
    viaje: 95,
    gas: 26,
    fichas: [
      { v: "$204", l: "por hora", c: "ok" },
      { v: "$7.3", l: "por km", c: "mal" },
      { v: "28 min", l: "13 km total", c: "neutro" },
    ],
    revela: "Mmm.",
    sfx: "golpe.wav",
  },
];

// Apertura de la boca (0 a 1) en el segundo t de una línea
const boca = (l: Linea, t: number) => {
  let v = 0;
  for (const [s, e, a] of l.voc) {
    if (t >= s - 0.03 && t <= e + 0.03) v = Math.max(v, a);
  }
  return v;
};

// Segundo (dentro de la línea) en que empieza una palabra
const tiempoDe = (l: Linea, palabra: string) => l.palabras.find((p) => p.w === palabra)?.s ?? 0;

// La bolita de luz con gorra (opción B)
const Bolita: React.FC<{ color: string; animo: Animo; abre: number; tam: number; parpadeo: number; brillo?: number }> = ({
  color,
  animo,
  abre,
  tam,
  parpadeo,
  brillo = 1,
}) => {
  const cx = 100;
  const cy = 150;
  const s = 2.1;
  const ex = 10 * s;
  const ey = cy - 5 * s;
  const w = 2.4 * s;
  const ojoRy = 7.5 * s * (1 - parpadeo * 0.9);
  const px = animo === "duda" ? 2 * s : 0;
  const hablando = abre > 0.05;
  let cejas: React.ReactNode = null;
  if (animo === "enojo") {
    cejas = (
      <>
        <path d={`M${cx - ex - 7 * s} ${ey - 11 * s} L${cx - ex + 6 * s} ${ey - 6 * s}`} stroke={INK} strokeWidth={w * 1.3} strokeLinecap="round" />
        <path d={`M${cx + ex + 7 * s} ${ey - 11 * s} L${cx + ex - 6 * s} ${ey - 6 * s}`} stroke={INK} strokeWidth={w * 1.3} strokeLinecap="round" />
      </>
    );
  } else if (animo === "duda") {
    cejas = (
      <>
        <path d={`M${cx - ex - 6 * s} ${ey - 10 * s} L${cx - ex + 6 * s} ${ey - 10 * s}`} stroke={INK} strokeWidth={w} strokeLinecap="round" />
        <path d={`M${cx + ex - 6 * s} ${ey - 12 * s} Q${cx + ex} ${ey - 16 * s} ${cx + ex + 6 * s} ${ey - 12 * s}`} fill="none" stroke={INK} strokeWidth={w} strokeLinecap="round" />
      </>
    );
  }
  let bocaSvg: React.ReactNode;
  if (hablando) {
    const ry = 2.5 * s + abre * 7 * s;
    bocaSvg = (
      <g>
        <ellipse cx={cx} cy={cy + 13 * s} rx={8 * s} ry={ry} fill={INK} />
        <ellipse cx={cx} cy={cy + 13 * s + ry * 0.55} rx={4.5 * s} ry={ry * 0.35} fill="#F43F5E" opacity={0.8} />
      </g>
    );
  } else if (animo === "enojo") {
    bocaSvg = <path d={`M${cx - 8 * s} ${cy + 15 * s} Q${cx} ${cy + 7 * s} ${cx + 8 * s} ${cy + 15 * s}`} fill="none" stroke={INK} strokeWidth={w} strokeLinecap="round" />;
  } else if (animo === "duda") {
    bocaSvg = (
      <path
        d={`M${cx - 8 * s} ${cy + 12 * s} Q${cx - 4 * s} ${cy + 8 * s} ${cx} ${cy + 12 * s} T${cx + 8 * s} ${cy + 12 * s}`}
        fill="none"
        stroke={INK}
        strokeWidth={w}
        strokeLinecap="round"
      />
    );
  } else {
    bocaSvg = <path d={`M${cx - 10 * s} ${cy + 7 * s} Q${cx} ${cy + 21 * s} ${cx + 10 * s} ${cy + 7 * s} Z`} fill={INK} />;
  }
  const g = 112;
  const top = 82;
  return (
    <svg width={tam} height={(tam * 288) / 200} viewBox="0 -26 200 288" style={{ overflow: "visible" }}>
      <defs>
        <radialGradient id="halo">
          <stop offset="0" stopColor={color} stopOpacity={0.55 * brillo} />
          <stop offset="1" stopColor={color} stopOpacity={0} />
        </radialGradient>
      </defs>
      <ellipse cx="100" cy="236" rx="46" ry="9" fill="#000" opacity={0.6} />
      <circle cx="100" cy="146" r="100" fill="url(#halo)" />
      <circle cx="100" cy="146" r="72" fill={color} />
      <ellipse cx="74" cy="114" rx="16" ry="10" fill="#fff" opacity={0.22} transform="rotate(-30 74 114)" />
      <ellipse cx={cx - ex} cy={ey} rx={6.5 * s} ry={ojoRy} fill="#fff" />
      <ellipse cx={cx + ex} cy={ey} rx={6.5 * s} ry={ojoRy} fill="#fff" />
      {parpadeo < 0.5 ? (
        <>
          <circle cx={cx - ex + px} cy={ey + s} r={3.4 * s} fill={INK} />
          <circle cx={cx + ex + px} cy={ey + s} r={3.4 * s} fill={INK} />
        </>
      ) : null}
      {cejas}
      {bocaSvg}
      <path d={`M${cx - g / 2} ${top} Q${cx - g / 2} ${top - g * 0.42} ${cx} ${top - g * 0.46} Q${cx + g / 2} ${top - g * 0.42} ${cx + g / 2} ${top} Z`} fill="#0E1210" stroke="#2C3531" strokeWidth={2} />
      <rect x={cx - g / 2} y={top - g * 0.12} width={g} height={g * 0.1} fill={MARCA.verde} />
      <path
        d={`M${cx - g * 0.1} ${top} Q${cx + g * 0.35} ${top - 2} ${cx + g * 0.72} ${top + g * 0.08} Q${cx + g * 0.3} ${top + g * 0.14} ${cx - g * 0.1} ${top + g * 0.06} Z`}
        fill="#0A0D0C"
        stroke="#2C3531"
        strokeWidth={2}
      />
    </svg>
  );
};

// Parpadeo cada ~3.5 s
const parpadeoEn = (frame: number) => {
  const p = frame % 105;
  return p < 3 ? 1 - Math.abs(p - 1.5) / 1.5 : 0;
};

// Subtítulos tipo karaoke: grupos de hasta 4 palabras
const Subs: React.FC<{ linea: Linea; t: number; color: string; top: number }> = ({ linea, t, color, top }) => {
  const grupos: { i: number; f: number }[] = [];
  let ini = 0;
  linea.palabras.forEach((p, i) => {
    const corta = /[.!?…,]$/.test(p.w) || i - ini === 3 || i === linea.palabras.length - 1;
    if (corta) {
      grupos.push({ i: ini, f: i });
      ini = i + 1;
    }
  });
  const g = [...grupos].reverse().find((x) => t >= linea.palabras[x.i].s - 0.05) ?? grupos[0];
  if (t > linea.dur + 0.4) return null;
  return (
    <div
      style={{
        position: "absolute",
        top,
        left: 70,
        right: 70,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "0 22px",
        fontFamily: FD,
        fontWeight: 900,
        fontSize: 84,
        lineHeight: 1.1,
        textAlign: "center",
        textShadow: "0 6px 24px rgba(0,0,0,0.8)",
      }}
    >
      {linea.palabras.slice(g.i, g.f + 1).map((p, k) => {
        const activa = t >= p.s - 0.05;
        return (
          <span key={k} style={{ color: activa ? color : "rgba(242,245,243,0.35)", transform: `scale(${activa && t < p.e ? 1.06 : 1})`, display: "inline-block" }}>
            {p.w}
          </span>
        );
      })}
    </div>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol?: number }> = ({ en, src, vol = 1 }) => (
  <Sequence from={en} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

const centro: React.CSSProperties = { position: "absolute", left: 0, right: 0, display: "flex", justifyContent: "center" };

export const Reacciona: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const k = Math.max(0, ESC.findIndex((e) => frame >= e.desde && frame < e.hasta));
  const esc = ESC[k];
  const linea = LINEAS[esc.linea];
  const t = (frame - esc.voz) / FPS;
  const abre = t >= 0 && t <= linea.dur ? boca(linea, t) : 0;
  const local = frame - esc.desde;

  // Estado de la oferta (escenas 1 a 3)
  const of = k <= 2 ? OFERTAS[k] : null;
  const cierre = k === 3;
  const revelaF = of ? esc.voz + Math.round(tiempoDe(linea, of.revela) * FPS) : 0;
  const revelada = of ? frame >= revelaF : false;
  const pop = of ? spring({ frame: frame - revelaF, fps, config: { damping: 11, mass: 0.6 } }) : 0;
  const entra = k === 0 ? 1 : spring({ frame: local, fps, config: { damping: 14, mass: 0.7 } });

  let color: string = MARCA.verde;
  let animo: Animo = "feliz";
  if (of) {
    // En el gancho el Checa ya está rojo y enojado desde el primer cuadro
    const yaReacciona = revelada || k === 0;
    color = yaReacciona ? MARCA[of.nivel] : MARCA.apagado;
    animo = yaReacciona ? of.animo : "neutro";
  }
  const subColor = of && (revelada || k === 0) ? MARCA[of.nivel] : "#F2F5F3";
  // Sacudida de enojo al inicio
  const sacude = k === 0 ? Math.sin(frame * 1.9) * interpolate(frame, [0, 28], [14, 0], { extrapolateRight: "clamp" }) : 0;

  // Tamaño y posición de la mascota
  const grande = cierre;
  // Durante el texto del gancho la mascota va un poco más chica y abajo
  const enGancho = k === 0 ? interpolate(frame, [95, 115], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }) : 0;
  const tam = grande ? 560 : 330 - 50 * enGancho;
  const salto = of && revelada ? (1 - pop) * -40 : 0;
  const zoomPop = of && revelada ? 1 + 0.12 * Math.sin(Math.min(1, (frame - revelaF) / 8) * Math.PI) : 1;
  const flota = Math.sin(frame / 14) * 6;
  const mascotaTop = grande ? 380 : 90;

  const tituloSale = interpolate(local, [0, 10], [0, 1], { extrapolateRight: "clamp" });

  return (
    <AbsoluteFill style={{ background: C.fondo, fontFamily: F, overflow: "hidden" }}>
      <Fondo tono={`${color}33`} />

      {/* Música y voces */}
      <Audio src={staticFile("beat.m4a")} volume={0.12} />
      {ESC.map((e, i) => (
        <Sequence key={i} from={e.voz}>
          <Audio src={staticFile(`checa1/${LINEAS[i].archivo}.mp3`)} volume={1} />
        </Sequence>
      ))}
      {OFERTAS.map((o, i) => (
        <React.Fragment key={i}>
          {i > 0 ? <Sfx en={ESC[i].desde} src="whoosh.wav" vol={0.5} /> : null}
          <Sfx en={ESC[i].voz + Math.round(tiempoDe(LINEAS[i], o.revela) * FPS)} src={o.sfx} vol={0.45} />
        </React.Fragment>
      ))}

      {/* Gancho en texto: arriba desde el cuadro 0 */}
      {k === 0 && frame < 110 ? (
        <div style={{ ...centro, top: 40 }}>
          <div
            style={{
              fontFamily: FD,
              fontWeight: 900,
              fontSize: 62,
              lineHeight: 1.05,
              textAlign: "center",
              color: "#F2F5F3",
              background: "rgba(0,0,0,0.55)",
              borderRadius: 24,
              padding: "14px 30px",
              opacity: interpolate(frame, [96, 110], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
              transform: `rotate(-2deg) scale(${1 + 0.04 * Math.sin(frame / 4)})`,
            }}
          >
            Solo <span style={{ color: MARCA.verde }}>1</span> de estas 3 ofertas
            <br />
            vale la pena
          </div>
        </div>
      ) : null}

      {/* Etiqueta de serie */}
      {of && !(k === 0 && frame < 110) ? (
        <div style={{ ...centro, top: 60, opacity: 1 }}>
          <div
            style={{
              fontFamily: F,
              fontWeight: 800,
              fontSize: 34,
              letterSpacing: 3,
              color: "#F2F5F3",
              background: "rgba(255,255,255,0.08)",
              border: "2px solid rgba(255,255,255,0.15)",
              borderRadius: 999,
              padding: "12px 28px",
            }}
          >
            OFERTA {k + 1} DE 3
          </div>
        </div>
      ) : null}

      {/* Mascota */}
      <div style={{ ...centro, top: mascotaTop + salto + flota + (of ? 50 : 0) + 90 * enGancho, transform: `translateX(${sacude}px)` }}>
        <div style={{ transform: `scale(${zoomPop})`, transformOrigin: "center bottom" }}>
          <Bolita color={color} animo={animo} abre={abre} tam={tam} parpadeo={parpadeoEn(frame)} />
        </div>
      </div>

      {/* Ofertas y veredicto */}
      {of ? (
        <>
          <Subs linea={linea} t={t} color={subColor} top={640} />
          <div style={{ ...centro, top: 900, transform: `translateY(${(1 - entra) * 900}px) scale(${revelada ? 0.94 : 1})` }}>
            <Oferta app="App de viajes" tarifa={of.tarifa} tramos={of.tramos} ancho={760} />
          </div>
          {revelada ? (
            <div
              style={{
                ...centro,
                top: 860,
                opacity: Math.min(1, pop * 2),
                transform: `translateY(${(1 - pop) * -200}px) scale(${1.15 - 0.15 * pop})`,
              }}
            >
              <Popup ancho={860} nivel={of.nivel} app="Viaje" neta={of.neta} viaje={of.viaje} gas={of.gas} fichas={of.fichas} />
            </div>
          ) : null}
        </>
      ) : null}

      {/* Cierre */}
      {cierre ? (
        <>
          <div style={{ ...centro, top: 150, opacity: tituloSale }}>
            <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 96, color: "#F2F5F3", textAlign: "center", lineHeight: 1.05 }}>
              ¿Tú cuál
              <br />
              aceptabas?
            </div>
          </div>
          <Subs linea={linea} t={t} color={MARCA.verde} top={1240} />
          {frame >= esc.voz + Math.round(tiempoDe(linea, "Pruébame") * FPS) ? (
            <div style={{ ...centro, top: 1470, flexDirection: "column", alignItems: "center", gap: 18 }}>
              <Logo tam={90} />
              <div
                style={{
                  fontFamily: F,
                  fontWeight: 800,
                  fontSize: 44,
                  color: INK,
                  background: MARCA.verde,
                  borderRadius: 999,
                  padding: "16px 40px",
                }}
              >
                7 días gratis · link en el perfil
              </div>
            </div>
          ) : null}
        </>
      ) : null}
    </AbsoluteFill>
  );
};
