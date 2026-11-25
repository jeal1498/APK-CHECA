import { Easing, interpolate } from "remotion";

/** Valores de la marca Checa v2 (fuente: PWA-Checa-Multimedia/marca/tokens.json). Solo se usan, no se redefinen. */
export const M = {
  asfalto: "#0C0C10",
  tablero: "#16161C",
  humo: "#3A3A45",
  hueso: "#F4F4EF",
  gris: "#A3A3AD",
  verde: "#22C55E",
  amarillo: "#FACC15",
  rojo: "#EF4444",
  sobreVerde: "#04210F",
  titulo: "Bricolage Grotesque, Arial Black, sans-serif",
  texto: "DM Sans, Arial, sans-serif",
  datos: "JetBrains Mono, Courier New, monospace",
} as const;

/** Curva de la marca: cubic-bezier(.2,.8,.2,1), sin rebote. */
export const curva = Easing.bezier(0.2, 0.8, 0.2, 1);
export const p = (f: number, inicio: number, dur: number) =>
  interpolate(f, [inicio, inicio + dur], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: curva });
export const lerp = (a: number, b: number, x: number) => a + (b - a) * x;
