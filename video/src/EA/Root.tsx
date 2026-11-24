import React from "react";
import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION_MUESTRA, Muestra } from "./Muestra";
import { DURACION_SHOWCASE, Showcase } from "./Showcase";
import { DURACION_REFERENCIA, Referencia } from "./Referencia";

const fuentes: [string, string, string][] = [
  ["Montserrat", "ea/montserrat-600.woff2", "600"],
  ["Montserrat", "ea/montserrat-800.woff2", "800"],
  ["Roboto", "ea/roboto-400.woff2", "400"],
  ["Roboto", "ea/roboto-500.woff2", "500"],
];
fuentes.forEach(([family, archivo, weight]) => loadFont({ family, url: staticFile(archivo), weight }));

export const EARoot: React.FC = () => (
  <>
    <Composition id="EA-Muestra" component={Muestra} durationInFrames={DURACION_MUESTRA} fps={30} width={1080} height={1920} />
    <Composition id="EA-Showcase" component={Showcase} durationInFrames={DURACION_SHOWCASE} fps={30} width={1080} height={1920} />
    <Composition id="EA-Referencia-01" component={Referencia} durationInFrames={DURACION_REFERENCIA} fps={30} width={1080} height={1920} />
  </>
);
