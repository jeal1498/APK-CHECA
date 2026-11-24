import { Easing, interpolate } from "remotion";

/** Tema EA: todo lo visual vive aquí (colores, tipos, bordes, sombras, tiempos). */
export const EA = {
  color: {
    negro: "#151515",
    marfil: "#F1EEE6",
    naranja: "#F47732",
    blanco: "#FFFFFF",
    papel: "#DDD7C8",
  },
  font: { titulo: "Montserrat", texto: "Roboto" },
  peso: { extra: 800, semi: 600, medio: 500, normal: 400 },
  borde: "3px solid #151515",
  sombra: "0 8px 0 rgba(21,21,21,0.14)",
  radio: 10,
  /** Zonas seguras (px) de TikTok / Reels / Shorts en 1080x1920. */
  zona: { arriba: 200, abajo: 380, lados: 60 },
  /** Duraciones de movimiento en frames a 30 fps. */
  mov: { corto: 10, medio: 18, largo: 30 },
} as const;

/** Aceleración suave y frenado largo (evita arranques bruscos y tramos congelados). */
export const ease = Easing.bezier(0.4, 0, 0.2, 1);

const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

/** 0 → 1 desde `inicio`, con `dur` frames. */
export const progreso = (frame: number, inicio: number, dur: number = EA.mov.medio) =>
  interpolate(frame, [inicio, inicio + dur], [0, 1], { ...clamp, easing: ease });

/** 1 → 0 terminando en `fin`. */
export const salida = (frame: number, fin: number, dur: number = EA.mov.corto) =>
  interpolate(frame, [fin - dur, fin], [1, 0], clamp);

export type Parte = { t: string; naranja?: boolean };
