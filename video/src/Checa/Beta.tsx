import React from "react";
import { AbsoluteFill, Audio, Easing, OffthreadVideo, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Logo } from "./ui";
import { CLIPS } from "./lineasBeta";

// Video de Eduardo para la campaña de beta testers: 7 clips unidos, sin tiempos muertos,
// con subtítulos dinámicos, etiquetas de la oferta, zoom suave y un acercamiento al pop-up en el demo.
const FPS = 30;
const CIERRE = 45;
const INI = CLIPS.reduce<number[]>((acc, c, k) => [...acc, k === 0 ? 0 : acc[k - 1] + Math.round(CLIPS[k - 1].dur * FPS)], []);
export const DURACION_BETA = INI[INI.length - 1] + Math.round(CLIPS[CLIPS.length - 1].dur * FPS) + CIERRE;

const K = { verde: "#4ADE80", amarillo: "#FACC15", rojo: "#F43F5E", cian: "#22D3EE", texto: "#FFFFFF" };
const MONO = "FiraCode";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ap = (f: number, d: number, dur = 8) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });

const CLAVE: Record<string, string> = {
  ciegas: K.rojo, perdiste: K.rojo, dinero: K.rojo, app: K.verde, checa: K.verde, conviene: K.verde, convenía: K.verde, uber: K.verde,
  exacto: K.amarillo, kilómetro: K.amarillo, "50": K.amarillo, conductores: K.amarillo, costo: K.verde, tres: K.amarillo, meses: K.amarillo,
  amigo: K.cian, mes: K.verde, gratis: K.verde, yo: K.amarillo, privado: K.cian, accesos: K.verde,
};
const limpia = (w: string) => w.toLowerCase().replace(/[^a-záéíóúñ0-9]/g, "");

const ETIQUETAS: { texto: string; color: string }[] = [
  { texto: "¿ACEPTAS A CIEGAS?", color: K.rojo },
  { texto: "HICE UNA APP", color: K.verde },
  { texto: "ASÍ FUNCIONA", color: K.verde },
  { texto: "PAGO EXACTO POR KM", color: K.amarillo },
  { texto: "50 LUGARES · 3 MESES GRATIS", color: K.verde },
  { texto: "+1 MES GRATIS A CADA UNO", color: K.cian },
  { texto: "COMENTA «YO»", color: K.amarillo },
];

const Etiqueta: React.FC<{ texto: string; color: string }> = ({ texto, color }) => {
  const frame = useCurrentFrame();
  const a = ap(frame, 2, 7);
  return (
    <div style={{ position: "absolute", top: 150, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: a, transform: `scale(${1.25 - 0.25 * a})` }}>
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 50, letterSpacing: 3, color, background: "rgba(5,6,15,0.82)", border: `5px solid ${color}`, borderRadius: 18, padding: "14px 30px", boxShadow: `0 0 26px ${color}88`, textAlign: "center", maxWidth: 960 }}>{texto}</div>
    </div>
  );
};

// Subtítulos dinámicos: 1–3 palabras, rebote al entrar, palabras clave a color
const Subs: React.FC<{ k: number }> = ({ k }) => {
  const frame = useCurrentFrame();
  const t = frame / FPS;
  const ps = CLIPS[k].palabras;
  const grupos: [number, number][] = [];
  let ini = 0;
  ps.forEach((p, j) => {
    if (/[.!?,:]$/.test(p.w) || j - ini === 2 || j === ps.length - 1) {
      grupos.push([ini, j]);
      ini = j + 1;
    }
  });
  const g = [...grupos].reverse().find(([a]) => t >= ps[a].s - 0.05) ?? grupos[0];
  const s = 1.15 - 0.15 * ap(frame, Math.round(ps[g[0]].s * FPS) - 1, 5);
  return (
    <div style={{ position: "absolute", top: 1330, left: 40, right: 40, textAlign: "center", fontFamily: "Inter", fontWeight: 800, fontSize: 84, lineHeight: 1.08, transform: `scale(${s})`, WebkitTextStroke: "4px #000", paintOrder: "stroke fill", textShadow: "0 6px 0 #000, 0 10px 30px rgba(0,0,0,0.9)" }}>
      {ps.slice(g[0], g[1] + 1).map((p, j) => (
        <span key={j} style={{ color: CLAVE[limpia(p.w)] ?? K.texto }}>
          {p.w.toUpperCase()}{" "}
        </span>
      ))}
    </div>
  );
};

const Clip: React.FC<{ k: number }> = ({ k }) => {
  const frame = useCurrentFrame();
  const n = Math.round(CLIPS[k].dur * FPS);
  let s = interpolate(frame, [0, n], [1, 1.06]);
  let origen = "50% 40%";
  if (k === 2) {
    // demo: acercamiento al pop-up de Checa cuando lo menciona
    const c = CLIPS[2].palabras.find((p) => p.w === "Checa")?.s ?? 3;
    s = interpolate(frame, [c * FPS - 6, c * FPS + 14], [1, 1.45], { ...clamp, easing: Easing.inOut(Easing.cubic) });
    origen = "62% 26%";
  }
  const destello = interpolate(frame, [0, 2, 7], [0.35, 0.18, 0], clamp);
  return (
    <AbsoluteFill style={{ background: "#000" }}>
      <AbsoluteFill style={{ transform: `scale(${s})`, transformOrigin: origen }}>
        <OffthreadVideo src={staticFile(`beta/${CLIPS[k].archivo}.mp4`)} />
      </AbsoluteFill>
      <AbsoluteFill style={{ background: "linear-gradient(180deg, rgba(0,0,0,0.45) 0%, rgba(0,0,0,0) 22%, rgba(0,0,0,0) 62%, rgba(0,0,0,0.55) 100%)" }} />
      <Etiqueta {...ETIQUETAS[k]} />
      <Subs k={k} />
      <AbsoluteFill style={{ background: "#FFFFFF", opacity: k === 0 ? 0 : destello }} />
    </AbsoluteFill>
  );
};

const Cierre: React.FC = () => {
  const frame = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: "radial-gradient(ellipse at 50% 40%, #0B1026 0%, #05060F 75%)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 40, opacity: ap(frame, 0, 6) }}>
      <Logo tam={120} />
      <div style={{ fontFamily: MONO, fontWeight: 700, fontSize: 64, color: K.amarillo, textShadow: `0 0 30px ${K.amarillo}88` }}>Comenta «YO»</div>
      <div style={{ fontFamily: MONO, fontSize: 38, color: K.verde }}>50 lugares · 3 meses gratis</div>
    </AbsoluteFill>
  );
};

export const Beta: React.FC = () => (
  <AbsoluteFill style={{ background: "#000" }}>
    {CLIPS.map((c, k) => (
      <Sequence key={c.archivo} from={INI[k]} durationInFrames={Math.round(c.dur * FPS)}>
        <Clip k={k} />
        <Sequence from={0} durationInFrames={20}>
          <Audio src={staticFile("whoosh.wav")} volume={k === 0 ? 0 : 0.05} />
        </Sequence>
      </Sequence>
    ))}
    <Sequence from={DURACION_BETA - CIERRE}>
      <Cierre />
      <Audio src={staticFile("ding.wav")} volume={0.12} />
    </Sequence>
    <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 10, DURACION_BETA - 20, DURACION_BETA], [0, 0.05, 0.05, 0], clamp)} />
  </AbsoluteFill>
);
