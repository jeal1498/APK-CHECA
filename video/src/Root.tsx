import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION, VideoChecas } from "./Checa/Video";
import { DURACION_RECOGIDA, Recogida } from "./Checa/Recogida";
import { DURACION_RECOGIDA2, Recogida2 } from "./Checa/Recogida2";
import { DURACION_GASOLINA150, Gasolina150 } from "./Checa/Gasolina150";
import { DURACION_GASOLINA150B, Gasolina150b } from "./Checa/Gasolina150b";
import { DURACION_GASOLINA150C, Gasolina150c } from "./Checa/Gasolina150c";

const fuentes: [string, string, string][] = [
  ["Inter", "Inter-Medium.otf", "500"],
  ["Inter", "Inter-SemiBold.otf", "600"],
  ["Inter", "Inter-Bold.otf", "700"],
  ["Inter", "Inter-ExtraBold.otf", "800"],
  ["InterDisplay", "InterDisplay-Bold.otf", "700"],
  ["InterDisplay", "InterDisplay-Black.otf", "900"],
  ["FiraCode", "FiraCode.ttf", "400"],
];
fuentes.forEach(([family, archivo, weight]) => loadFont({ family, url: staticFile(archivo), weight }));

export const RemotionRoot: React.FC = () => {
  return (
    <>
    <Composition
      id="ChecaGancho"
      component={VideoChecas}
      durationInFrames={DURACION}
      fps={30}
      width={1080}
      height={1920}
    />
    <Composition id="ExplicaRecogida" component={Recogida} durationInFrames={DURACION_RECOGIDA} fps={30} width={1080} height={1920} />
    <Composition id="Recogida2" component={Recogida2} durationInFrames={DURACION_RECOGIDA2} fps={30} width={1080} height={1920} defaultProps={{ musica: "recogida2/musica.mp3" }} />
    <Composition id="Gasolina150" component={Gasolina150} durationInFrames={DURACION_GASOLINA150} fps={30} width={1080} height={1920} defaultProps={{ musica: "recogida2/musica.mp3" }} />
    <Composition id="Gasolina150b" component={Gasolina150b} durationInFrames={DURACION_GASOLINA150B} fps={30} width={1080} height={1920} />
    <Composition id="Gasolina150c" component={Gasolina150c} durationInFrames={DURACION_GASOLINA150C} fps={30} width={1080} height={1920} />
    </>
  );
};
