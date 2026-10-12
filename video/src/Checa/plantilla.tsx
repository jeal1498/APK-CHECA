import React from "react";
import { AbsoluteFill, Audio, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { F, FD } from "./ui";
import { Asfalto, K } from "./graficos";

// Plantilla de los videos explicativos de Checa (narrador + gráficos de calle + golpes cada ~1.5 s).
// Cada video define: sus LINEAS (voz), sus escenas (una por línea) y sus GOLPES. Ver .claude/skills/videos-checa.

export const FPS = 30;
export type Palabra = { w: string; s: number; e: number };
export type Linea = { archivo: string; dur: number; palabras: Palabra[]; voc: [number, number, number][] };
export type Escena = { desde: number; hasta: number; voz: number };
export type GolpeCam = { f: number; color: string; fuerte?: boolean; sfx?: string };

export const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
export const centro: React.CSSProperties = { position: "absolute", left: 0, right: 0, display: "flex", justifyContent: "center" };
export const impulso = (frame: number, g: number, caida = 5) => (frame >= g ? Math.exp(-(frame - g) / caida) : 0);

// Escenas seguidas: una por línea de voz; la 1ª sin pausa (es el gancho)
export const armarEscenas = (lineas: Linea[], pausa = 5, cola = 8, colaFinal = 50) => {
  const esc: Escena[] = [];
  let f = 0;
  lineas.forEach((l, i) => {
    const p = i === 0 ? 0 : pausa;
    const fin = f + p + Math.round(l.dur * FPS) + (i === lineas.length - 1 ? colaFinal : cola);
    esc.push({ desde: f, hasta: fin, voz: f + p });
    f = fin;
  });
  return { esc, duracion: f };
};

// Cuadro absoluto en que el narrador dice una palabra (escrita igual que en el guion, con su puntuación)
export const crearEn = (lineas: Linea[], esc: Escena[]) => (i: number, palabra: string, vez = 1) => {
  const ps = lineas[i].palabras.filter((x) => x.w === palabra);
  const p = ps[vez - 1];
  if (!p) throw new Error(`No existe la palabra «${palabra}» (vez ${vez}) en la línea ${i + 1}`);
  return esc[i].voz + Math.round(p.s * FPS);
};

export const useResorte = (desde: number, damping = 10) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return spring({ frame: frame - desde, fps, config: { damping, mass: 0.6 } });
};

// Entra con golpe (de grande a su tamaño)
export const Golpe: React.FC<{ desde: number; children: React.ReactNode; rot?: number; top: number }> = ({ desde, children, rot = 0, top }) => {
  const frame = useCurrentFrame();
  const g = useResorte(desde);
  if (frame < desde) return null;
  return <div style={{ ...centro, top, transform: `scale(${1.7 - 0.7 * g}) rotate(${rot}deg)`, opacity: Math.min(1, g * 1.6) }}>{children}</div>;
};

// Cae desde arriba
export const Cae: React.FC<{ desde: number; children: React.ReactNode; top: number }> = ({ desde, children, top }) => {
  const frame = useCurrentFrame();
  const g = useResorte(desde, 12);
  if (frame < desde) return null;
  return <div style={{ ...centro, top, transform: `translateY(${(1 - g) * -900}px) rotate(${(1 - g) * -8}deg)` }}>{children}</div>;
};

export const Titulo: React.FC<{ children: React.ReactNode; tam?: number; color?: string }> = ({ children, tam = 140, color = K.blanco }) => (
  <div style={{ fontFamily: FD, fontWeight: 900, fontSize: tam, color, lineHeight: 1, letterSpacing: -3, textAlign: "center", textShadow: "0 10px 40px rgba(0,0,0,0.6)" }}>
    {children}
  </div>
);

export const Pill: React.FC<{ children: React.ReactNode; fondo?: string; color?: string; tam?: number }> = ({ children, fondo = K.amarillo, color = "#111", tam = 46 }) => (
  <div style={{ fontFamily: F, fontWeight: 800, fontSize: tam, color, background: fondo, borderRadius: 999, padding: "14px 34px" }}>{children}</div>
);

// Subtítulos tipo karaoke (grupos de hasta 4 palabras), abajo en y=1340
export const Subs: React.FC<{ linea: Linea; t: number; color: string }> = ({ linea, t, color }) => {
  const grupos: { i: number; f: number }[] = [];
  let ini = 0;
  linea.palabras.forEach((p, i) => {
    if (/[.!?…,:]$/.test(p.w) || i - ini === 3 || i === linea.palabras.length - 1) {
      grupos.push({ i: ini, f: i });
      ini = i + 1;
    }
  });
  if (t > linea.dur + 0.25 || t < -0.1) return null;
  const g = [...grupos].reverse().find((x) => t >= linea.palabras[x.i].s - 0.05) ?? grupos[0];
  return (
    <div
      style={{
        position: "absolute",
        top: 1340,
        left: 70,
        right: 70,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "0 26px",
        fontFamily: FD,
        fontWeight: 900,
        fontSize: 70,
        lineHeight: 1.1,
        textAlign: "center",
        textShadow: "0 4px 0 #000, 0 8px 24px rgba(0,0,0,0.9)",
      }}
    >
      {linea.palabras.slice(g.i, g.f + 1).map((p, k) => {
        const on = t >= p.s - 0.05;
        return (
          <span key={k} style={{ color: on ? color : "rgba(244,246,245,0.4)", transform: `scale(${on && t < p.e ? 1.04 : 1})`, display: "inline-block" }}>
            {p.w}
          </span>
        );
      })}
    </div>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol?: number }> = ({ en: e, src, vol = 0.5 }) => (
  <Sequence from={e} durationInFrames={60}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

// El video completo: asfalto, música, voces, efectos, cámara (zoom/sacudida/destello), barrido entre escenas y subtítulos
export const VideoExplicativo: React.FC<{
  carpeta: string;
  lineas: Linea[];
  esc: Escena[];
  duracion: number;
  escenas: React.FC[];
  golpes: GolpeCam[];
  subColor: string[];
  tonos: string[];
  musica: string;
  volMusica?: number;
}> = ({ carpeta, lineas, esc, duracion, escenas, golpes, subColor, tonos, musica, volMusica = 0.11 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const k = Math.max(0, esc.findIndex((e) => frame >= e.desde && frame < e.hasta));
  const t = (frame - esc[k].voz) / FPS;
  const Esc = escenas[k];
  let zoom = 1;
  let sx = 0;
  let sy = 0;
  let flash = 0;
  let flashColor = K.blanco;
  for (const g of golpes) {
    const i = impulso(frame, g.f, 5);
    if (i <= 0.01) continue;
    zoom += (g.fuerte ? 0.1 : 0.05) * i;
    if (g.fuerte) {
      sx += Math.sin(frame * 2.3) * 20 * i;
      sy += Math.cos(frame * 2.9) * 14 * i;
    }
    const fl = i * (g.fuerte ? 0.3 : 0.15);
    if (fl > flash) {
      flash = fl;
      flashColor = g.color;
    }
  }
  const deriva = 1 + 0.05 * ((frame - esc[k].desde) / (esc[k].hasta - esc[k].desde));
  const ent = k === 0 ? 1 : spring({ frame: frame - esc[k].desde, fps, config: { damping: 15, mass: 0.5 } });
  const dir = k % 2 ? 1 : -1;
  return (
    <AbsoluteFill style={{ fontFamily: F, overflow: "hidden" }}>
      <Asfalto tono={tonos[k]} />
      <Audio src={staticFile(musica)} volume={(f) => interpolate(f, [0, 6, duracion - 40, duracion], [volMusica * 1.8, volMusica, volMusica, 0], clamp)} />
      {esc.map((e, i) => (
        <Sequence key={i} from={e.voz}>
          <Audio src={staticFile(`${carpeta}/${lineas[i].archivo}.mp3`)} />
        </Sequence>
      ))}
      {golpes
        .filter((g) => g.sfx)
        .map((g, i) => (
          <Sfx key={i} en={g.f} src={g.sfx!} vol={g.fuerte ? 0.45 : 0.3} />
        ))}
      <AbsoluteFill
        style={{
          transform: `translate(${sx + (1 - ent) * 800 * dir}px, ${sy}px) scale(${zoom * deriva}) rotate(${(1 - ent) * 5 * dir}deg)`,
          opacity: Math.min(1, ent * 1.5),
        }}
      >
        <Esc />
      </AbsoluteFill>
      <AbsoluteFill style={{ background: flashColor, opacity: flash, pointerEvents: "none" }} />
      <Subs linea={lineas[k]} t={t} color={subColor[k]} />
    </AbsoluteFill>
  );
};
