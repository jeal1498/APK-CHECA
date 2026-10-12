import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION, VideoChecas } from "./Checa/Video";

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
    <Composition
      id="ChecaGancho"
      component={VideoChecas}
      durationInFrames={DURACION}
      fps={30}
      width={1080}
      height={1920}
    />
  );
};
