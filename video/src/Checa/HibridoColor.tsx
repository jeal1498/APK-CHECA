import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { F, Logo } from "./ui";
import { LINEAS } from "./lineasHibrido";

// Prueba de 6 s del estilo híbrido A COLOR (más colores, como @pixelmente): mapa line-art (1Volt1Amp) + etiquetas neón en letra pixel,
// puntos que se mueven, cámara que se aleja y subtítulos con palabra clave en amarillo (@pixelmente).
export const DURACION_HIBRIDO_COLOR = 180;
const VOZ = 15;
const P = {
  fondo: "#141A3A",
  calle: "#4B5A8C",
  linea: "#2A2F5E",
  cian: "#22D3EE",
  azul: "#2563EB",
  magenta: "#EC4899",
  amarillo: "#FACC15",
  texto: "#FFFFFF",
  tenue: "#C9CFF0",
};
// Techos de colores y parques (paleta de @pixelmente)
const TECHOS = ["#FF8A7A", "#F6C453", "#4FD1C5", "#B79CFF", "#F1F0FA", "#7DD3FC", "#FDA4AF"];
const PARQUE = "#6BCB77";
const PX = "Silkscreen";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ap = (f: number, d: number, dur = 12) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
const en = (palabra: string) => VOZ + Math.round(LINEAS[0].palabras.find((p) => p.w === palabra)!.s * 30);

// Calles (centro) y manzanas
const XS = [180, 420, 660, 900];
const YS = [360, 580, 800, 1100];
const CALLE = 36;

const Mapa: React.FC = () => {
  const frame = useCurrentFrame();
  const bordesX = [-200, ...XS, 1280];
  const bordesY = [120, ...YS, 1300];
  const manzanas: { x: number; y: number; w: number; h: number }[] = [];
  for (let i = 0; i < bordesX.length - 1; i++)
    for (let j = 0; j < bordesY.length - 1; j++) {
      const x = bordesX[i] + CALLE / 2;
      const y = bordesY[j] + CALLE / 2;
      manzanas.push({ x, y, w: bordesX[i + 1] - bordesX[i] - CALLE, h: bordesY[j + 1] - bordesY[j] - CALLE });
    }
  // Otros conductores: puntos que circulan por las calles (vertical: x fijo; horizontal: y fijo)
  const otros: { eje: "v" | "h"; fijo: number; ini: number; v: number }[] = [
    { eje: "v", fijo: 660, ini: 1250, v: -3.2 },
    { eje: "v", fijo: 900, ini: 200, v: 2.6 },
    { eje: "h", fijo: 360, ini: -50, v: 3.0 },
    { eje: "h", fijo: 1100, ini: 1150, v: -2.4 },
    { eje: "v", fijo: 420, ini: 140, v: 2.0 },
  ];
  const mov = (ini: number, v: number) => ((((ini + frame * v) % 1400) + 1400) % 1400) - 100;
  return (
    <svg width={1080} height={1920} style={{ position: "absolute", overflow: "visible" }}>
      <rect x={-300} y={-300} width={1700} height={1700} fill={P.calle} />
      {XS.map((x) => (
        <line key={x} x1={x} y1={-300} x2={x} y2={1400} stroke="#FFFFFF" strokeOpacity={0.35} strokeWidth={3} strokeDasharray="16 18" />
      ))}
      {YS.map((y) => (
        <line key={y} x1={-300} y1={y} x2={1400} y2={y} stroke="#FFFFFF" strokeOpacity={0.35} strokeWidth={3} strokeDasharray="16 18" />
      ))}
      {manzanas.map((m, i) => {
        const parque = i % 7 === 3;
        const c = parque ? PARQUE : TECHOS[(i * 5) % TECHOS.length];
        const casas: React.ReactNode[] = [];
        if (!parque) {
          // Edificios pixel: cuadros con ventanas más oscuras
          for (let k = 0; k < 4; k++) {
            const bx = m.x + 14 + (k % 2) * (m.w / 2 - 7);
            const by = m.y + 14 + Math.floor(k / 2) * (m.h / 2 - 7);
            const bw = m.w / 2 - 21;
            const bh = m.h / 2 - 21;
            casas.push(<rect key={k} x={bx} y={by} width={bw} height={bh} rx={4} fill={c} stroke={P.linea} strokeWidth={3} />);
            for (let w = 0; w < 3; w++)
              casas.push(<rect key={`${k}w${w}`} x={bx + 10 + w * (bw - 20) / 3} y={by + 10} width={(bw - 40) / 3} height={10} fill={P.linea} opacity={0.35} />);
          }
        } else {
          for (let k = 0; k < 6; k++) casas.push(<circle key={k} cx={m.x + 30 + (k % 3) * (m.w - 60) / 2} cy={m.y + 30 + Math.floor(k / 3) * (m.h - 60)} r={18} fill="#3E9B57" stroke={P.linea} strokeWidth={3} />);
        }
        return (
          <g key={i}>
            <rect x={m.x} y={m.y} width={m.w} height={m.h} rx={10} fill={parque ? PARQUE : "#E9E6F5"} stroke={P.linea} strokeWidth={4} />
            {casas}
          </g>
        );
      })}
      {otros.map((o, i) => (
        <rect key={i} x={(o.eje === "v" ? o.fijo : mov(o.ini, o.v)) - (o.eje === "v" ? 12 : 18)} y={(o.eje === "h" ? o.fijo : mov(o.ini, o.v)) - (o.eje === "h" ? 12 : 18)} width={o.eje === "v" ? 24 : 36} height={o.eje === "v" ? 36 : 24} rx={6} fill={["#F6C453", "#FDA4AF", "#4FD1C5", "#FFFFFF", "#B79CFF"][i]} stroke={P.linea} strokeWidth={3} />
      ))}
    </svg>
  );
};

// Ruta que se dibuja; punteada si es la recogida
const Ruta: React.FC<{ d: string; largo: number; desde: number; dur: number; color: string; punteada?: boolean; id: string }> = ({ d, largo, desde, dur, color, punteada, id }) => {
  const frame = useCurrentFrame();
  const p = ap(frame, desde, dur);
  return (
    <svg width={1080} height={1920} style={{ position: "absolute", overflow: "visible" }}>
      <defs>
        <mask id={id}>
          <path d={d} stroke="#fff" strokeWidth={30} fill="none" strokeDasharray={`${largo} ${largo}`} strokeDashoffset={largo * (1 - p)} strokeLinecap="round" strokeLinejoin="round" />
        </mask>
      </defs>
      <path d={d} stroke="#FFFFFF" strokeWidth={28} fill="none" mask={`url(#${id})`} strokeLinejoin="round" strokeLinecap="round" />
      <path d={d} stroke={color} strokeWidth={16} fill="none" mask={`url(#${id})`} strokeDasharray={punteada ? "22 16" : undefined} strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
};

const Caja: React.FC<{ color: string; children: React.ReactNode; tam?: number; tachado?: number }> = ({ color, children, tam = 34, tachado = 0 }) => (
  <div style={{ position: "relative", display: "inline-block", fontFamily: PX, fontSize: tam, color, border: `4px solid ${color}`, padding: "8px 18px 6px", background: "rgba(20,26,58,0.92)", boxShadow: `0 0 18px ${color}55`, letterSpacing: 1 }}>
    {children}
    {tachado > 0 ? <div style={{ position: "absolute", left: -8, top: "50%", height: 6, width: `calc(${tachado * 100}% + 16px)`, background: P.magenta }} /> : null}
  </div>
);

const Pin: React.FC<{ x: number; y: number; desde: number }> = ({ x, y, desde }) => {
  const frame = useCurrentFrame();
  const e = ap(frame, desde, 10);
  return (
    <svg width={1080} height={1920} style={{ position: "absolute", overflow: "visible" }}>
      <g transform={`translate(${x} ${y - 40 * e}) scale(${e})`}>
        <path d="M0 40 C -14 18, -22 10, -22 -4 A 22 22 0 0 1 22 -4 C 22 10, 14 18, 0 40 Z" fill={P.magenta} stroke={P.linea} strokeWidth={5} />
        <circle cx={0} cy={-4} r={8} fill="#FFFFFF" />
      </g>
    </svg>
  );
};

const Carro: React.FC = () => {
  const frame = useCurrentFrame();
  const pulso = (frame % 30) / 30;
  return (
    <svg width={1080} height={1920} style={{ position: "absolute", overflow: "visible" }}>
      <circle cx={180} cy={1100} r={20 + pulso * 40} fill="none" stroke={P.cian} strokeOpacity={1 - pulso} strokeWidth={4} />
      <rect x={160} y={1072} width={40} height={56} rx={10} fill="#FFFFFF" stroke={P.linea} strokeWidth={5} />
      <rect x={168} y={1082} width={24} height={14} rx={3} fill={P.cian} />
    </svg>
  );
};

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const t = (frame - VOZ) / 30;
  const ps = LINEAS[0].palabras;
  if (t < -0.1 || t > LINEAS[0].dur + 0.6) return null;
  const grupo = t < ps[4].s - 0.05 ? ps.slice(0, 4) : ps.slice(4);
  const clave = ["ocho", "catorce."];
  return (
    <div style={{ position: "absolute", top: 1500, left: 60, right: 60, textAlign: "center", fontFamily: F, fontWeight: 800, fontSize: 52, color: P.texto, textShadow: "0 4px 14px rgba(0,0,0,0.9)" }}>
      {grupo.map((p, i) => (
        <span key={i} style={{ color: clave.includes(p.w) ? P.amarillo : P.texto }}>
          {p.w}{" "}
        </span>
      ))}
    </div>
  );
};

export const HibridoColor: React.FC = () => {
  const frame = useCurrentFrame();
  // Cámara: empieza cerca del carro y se aleja
  const z = interpolate(frame, [0, 40], [1.9, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const ox = interpolate(frame, [0, 40], [180, 540], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const oy = interpolate(frame, [0, 40], [1100, 760], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const tPago = en("ocho");
  const tManejas = en("manejas");
  const tCatorce = en("catorce.");
  const km = Math.round(interpolate(frame, [tCatorce, tCatorce + 16], [8, 14], clamp));
  const tachar = ap(frame, tManejas, 8);
  const fin = 140;
  return (
    <AbsoluteFill style={{ background: P.fondo, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `translate(${540 - ox}px, ${760 - oy}px) scale(${z})`, transformOrigin: `${ox}px ${oy}px` }}>
        <Mapa />
        {/* Atenúa el mapa cuando entran las rutas para que resalten */}
        <div style={{ position: "absolute", left: -400, top: -400, width: 1900, height: 1900, background: "#141A3A", opacity: 0.45 * ap(frame, tPago - 6, 14) }} />
        {/* Viaje pagado: del pin al destino (8 km) */}
        <Ruta id="viaje" d="M420 800 L420 580 L900 580 L900 360" largo={920} desde={tPago - 4} dur={22} color={P.cian} />
        {/* Recogida sin pago: del carro al pin (6 km) */}
        <Ruta id="rec" d="M180 1100 L180 800 L420 800" largo={540} desde={tManejas - 4} dur={18} color={P.magenta} punteada />
        <Pin x={420} y={800} desde={tPago - 8} />
        <svg width={1080} height={1920} style={{ position: "absolute", overflow: "visible", opacity: ap(frame, tPago + 10) }}>
          <rect x={880} y={340} width={40} height={40} rx={8} fill={P.cian} stroke={P.linea} strokeWidth={4} />
        </svg>
        <Carro />
        <div style={{ position: "absolute", left: 470, top: 600, opacity: ap(frame, tPago + 12) }}>
          <Caja color={P.cian} tam={30}>8 KM · PAGADOS</Caja>
        </div>
        <div style={{ position: "absolute", left: 210, top: 850, opacity: ap(frame, tManejas + 10) }}>
          <Caja color={P.magenta} tam={30}>6 KM · SIN PAGO</Caja>
        </div>
      </AbsoluteFill>

      {/* Banda de abajo: degradado atardecer como @pixelmente */}
      <div style={{ position: "absolute", top: 1230, left: 0, right: 0, bottom: 0, background: "linear-gradient(180deg, #1B1F4B 0%, #3A1F5C 55%, #7A2E5F 100%)", borderTop: `6px solid ${P.linea}` }} />
      {/* Etiqueta de la oferta arriba */}
      <div style={{ position: "absolute", top: 110, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 18, opacity: ap(frame, 2, 10) * (1 - ap(frame, fin, 10)) }}>
        <Caja color={P.amarillo}>OFERTA · $80</Caja>
      </div>

      {/* Número gigante: 8 KM tachado → 14 KM */}
      <div style={{ position: "absolute", top: 1270, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", opacity: ap(frame, tPago + 6) * (1 - ap(frame, fin, 10)) }}>
        <div style={{ position: "relative", fontFamily: PX, fontWeight: 700, fontSize: 150, lineHeight: 1, color: frame >= tCatorce ? P.magenta : P.amarillo, textShadow: frame >= tCatorce ? `0 0 30px ${P.magenta}aa, 0 6px 0 #14102e` : "0 6px 0 #14102e" }}>
          {km} KM
          {frame < tCatorce && tachar > 0 ? <div style={{ position: "absolute", left: -10, top: "48%", height: 12, width: `calc(${tachar * 100}% + 20px)`, background: P.magenta }} /> : null}
        </div>
        <div style={{ fontFamily: "FiraCode", fontSize: 32, color: P.tenue, marginTop: 10 }}>{frame >= tCatorce ? "los que manejas" : "los que te pagan"}</div>
      </div>

      {/* Cierre */}
      {frame >= fin ? (
        <AbsoluteFill style={{ background: `linear-gradient(180deg, rgba(37,99,235,${0.9 * ap(frame, fin, 10)}) 0%, rgba(122,46,95,${0.95 * ap(frame, fin, 10)}) 100%)`, alignItems: "center", justifyContent: "center", gap: 40, display: "flex", flexDirection: "column" }}>
          <div style={{ opacity: ap(frame, fin + 4, 10) }}>
            <Caja color={P.cian} tam={40}>CHECA CUENTA LOS 14</Caja>
          </div>
          <div style={{ opacity: ap(frame, fin + 10, 10) }}>
            <Logo tam={110} />
          </div>
        </AbsoluteFill>
      ) : null}

      <Subs />
      <Sequence from={VOZ}>
        <Audio src={staticFile("hibrido/l1.mp3")} />
      </Sequence>
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 10, 160, 180], [0, 0.14, 0.14, 0], clamp)} />
      <Sequence from={tPago - 4} durationInFrames={40}>
        <Audio src={staticFile("whoosh.wav")} volume={0.12} />
      </Sequence>
      <Sequence from={tCatorce} durationInFrames={40}>
        <Audio src={staticFile("pop.mp3")} volume={0.18} />
      </Sequence>
      <Sequence from={fin} durationInFrames={40}>
        <Audio src={staticFile("ding.wav")} volume={0.15} />
      </Sequence>
    </AbsoluteFill>
  );
};
