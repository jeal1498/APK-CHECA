import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION, VideoChecas } from "./Checa/Video";
import { DURACION_REACCIONA, Reacciona } from "./Checa/Reacciona";
import { DURACION_RECOGIDA, Recogida } from "./Checa/Recogida";

const fuentes: [string, string, string][] = [
  ["Inter", "Inter-Medium.otf", "500"],
  ["Inter", "Inter-SemiBold.otf", "600"],
  ["Inter", "Inter-Bold.otf", "700"],
  ["Inter", "Inter-ExtraBold.otf", "800"],
  ["InterDisplay", "InterDisplay-Bold.otf", "700"],
  ["InterDisplay", "InterDisplay-Black.otf", "900"],
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
    <Composition id="ChecaReacciona1" component={Reacciona} durationInFrames={DURACION_REACCIONA} fps={30} width={1080} height={1920} />
    <Composition id="ExplicaRecogida" component={Recogida} durationInFrames={DURACION_RECOGIDA} fps={30} width={1080} height={1920} />
    </>
  );
};
