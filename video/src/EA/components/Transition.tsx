import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { EA, ease } from "../theme";

/** Barrido: una cortina negra y otra naranja cruzan la pantalla (cubren en el frame medio). */
export const Transition: React.FC<{ inicio: number; dur?: number }> = ({ inicio, dur = 24 }) => {
  const frame = useCurrentFrame();
  const mover = (retraso: number) =>
    interpolate(frame, [inicio + retraso, inicio + retraso + dur / 2, inicio + retraso + dur], [-1080, 0, 1080], {
      extrapolateLeft: "clamp",
      extrapolateRight: "clamp",
      easing: ease,
    });
  if (frame < inicio || frame > inicio + dur + 6) return null;
  const capa = (x: number, color: string) => <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 1920, background: color, transform: `translateX(${x}px)` }} />;
  return (
    <>
      {capa(mover(0), EA.color.negro)}
      {capa(mover(6), EA.color.naranja)}
    </>
  );
};
