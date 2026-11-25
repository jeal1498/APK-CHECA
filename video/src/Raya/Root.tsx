import React from "react";
import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION_FINAL_CHECA, FinalChecaPlay } from "./FinalPlay";

/** Videos de @checa.driver, estilo «La raya» (marketing/videos/CHECA/Editor IA/Identidad/IDENTIDAD.md). */
const fuentes: [string, string, string][] = [
  ["Bricolage Grotesque", "raya/bricolage-grotesque-latin-800-normal.woff2", "800"],
  ["DM Sans", "raya/dm-sans-latin-500-normal.woff2", "500"],
  ["JetBrains Mono", "raya/jetbrains-mono-latin-700-normal.woff2", "700"],
];
fuentes.forEach(([family, archivo, weight]) => loadFont({ family, url: staticFile(archivo), weight }));

export const RayaRoot: React.FC = () => (
  <>
    <Composition id="Raya-Final-Play" component={FinalChecaPlay} durationInFrames={DURACION_FINAL_CHECA} fps={30} width={1080} height={1920} />
  </>
);
