import React from "react";
import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION_MUESTRA, Muestra } from "./Muestra";
import { DURACION_SHOWCASE, Showcase } from "./Showcase";
import { DURACION_REFERENCIA, Referencia } from "./Referencia";
import { DURACION_REFERENCIA_02, Referencia02 } from "./Referencia02";
import { DURACION_REFERENCIA_03, Referencia03 } from "./Referencia03";
import { DURACION_ESTILO, EstiloA, EstiloB, EstiloC } from "./Estilos";

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
    <Composition id="EA-Referencia-02" component={Referencia02} durationInFrames={DURACION_REFERENCIA_02} fps={30} width={1080} height={1920} />
    <Composition id="EA-Referencia-03" component={Referencia03} durationInFrames={DURACION_REFERENCIA_03} fps={30} width={1080} height={1920} />
    <Composition id="EA-Estilo-A" component={EstiloA} durationInFrames={DURACION_ESTILO} fps={30} width={1080} height={1920} />
    <Composition id="EA-Estilo-B" component={EstiloB} durationInFrames={DURACION_ESTILO} fps={30} width={1080} height={1920} />
    <Composition id="EA-Estilo-C" component={EstiloC} durationInFrames={DURACION_ESTILO} fps={30} width={1080} height={1920} />
  </>
);
