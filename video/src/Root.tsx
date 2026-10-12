import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION_GASOLINA150, Gasolina150 } from "./Checa/Gasolina150";

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
    <Composition id="Gasolina150" component={Gasolina150} durationInFrames={DURACION_GASOLINA150} fps={30} width={1080} height={1920} />
    </>
  );
};
