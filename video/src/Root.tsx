import { Composition, staticFile } from "remotion";
import { loadFont } from "@remotion/fonts";
import { DURACION_GASOLINA150, Gasolina150 } from "./Checa/Gasolina150";
import { DURACION_HIBRIDO, Hibrido } from "./Checa/Hibrido";
import { DURACION_HIBRIDO_COLOR, HibridoColor } from "./Checa/HibridoColor";
import { Calle3D, DURACION_CALLE3D } from "./Checa/Calle3D";
import { DURACION_VISTAS, Vistas3D } from "./Checa/Vistas3D";
import { DURACION_SIN_LANA, SinLana } from "./Checa/SinLana";
import { DURACION_RECOGIDA, Recogida } from "./Checa/Recogida";
import { DURACION_RECOGIDA2, Recogida2 } from "./Checa/Recogida2";
import { DURACION_RECOGIDA_1VOLT, Recogida1Volt } from "./Checa/Recogida1Volt";
import { DURACION_GASOLINA_1VOLT, Gasolina1Volt } from "./Checa/Gasolina1Volt";
import { DURACION_HORAS_1VOLT, Horas1Volt } from "./Checa/Horas1Volt";
import { DURACION_DECIDIR_1VOLT, Decidir1Volt } from "./Checa/Decidir1Volt";
import { DURACION_COMISION_1VOLT, Comision1Volt } from "./Checa/Comision1Volt";
import { DURACION_RECHAZAR_1VOLT, Rechazar1Volt } from "./Checa/Rechazar1Volt";
import { DURACION_DESGASTE_1VOLT, Desgaste1Volt } from "./Checa/Desgaste1Volt";
import { DURACION_SEGURIDAD_1VOLT, Seguridad1Volt } from "./Checa/Seguridad1Volt";
import { DURACION_BLOQUEO_1VOLT, Bloqueo1Volt } from "./Checa/Bloqueo1Volt";

const fuentes: [string, string, string][] = [
  ["Inter", "Inter-Medium.otf", "500"],
  ["Inter", "Inter-SemiBold.otf", "600"],
  ["Inter", "Inter-Bold.otf", "700"],
  ["Inter", "Inter-ExtraBold.otf", "800"],
  ["InterDisplay", "InterDisplay-Bold.otf", "700"],
  ["InterDisplay", "InterDisplay-Black.otf", "900"],
  ["FiraCode", "FiraCode.ttf", "400"],
  ["Silkscreen", "Silkscreen-400.woff2", "400"],
  ["Silkscreen", "Silkscreen-700.woff2", "700"],
];
fuentes.forEach(([family, archivo, weight]) => loadFont({ family, url: staticFile(archivo), weight }));

export const RemotionRoot: React.FC = () => {
  return (
    <>
    <Composition id="Gasolina150" component={Gasolina150} durationInFrames={DURACION_GASOLINA150} fps={30} width={1080} height={1920} />
    <Composition id="Hibrido" component={Hibrido} durationInFrames={DURACION_HIBRIDO} fps={30} width={1080} height={1920} />
    <Composition id="HibridoColor" component={HibridoColor} durationInFrames={DURACION_HIBRIDO_COLOR} fps={30} width={1080} height={1920} />
    <Composition id="Calle3D" component={Calle3D} durationInFrames={DURACION_CALLE3D} fps={30} width={1080} height={1920} />
    <Composition id="Vistas3D" component={Vistas3D} durationInFrames={DURACION_VISTAS} fps={30} width={1080} height={1920} />
    <Composition id="SinLana" component={SinLana} durationInFrames={DURACION_SIN_LANA} fps={30} width={1080} height={1920} />
    <Composition id="Recogida" component={Recogida} durationInFrames={DURACION_RECOGIDA} fps={30} width={1080} height={1920} />
    <Composition id="Recogida2" component={Recogida2} durationInFrames={DURACION_RECOGIDA2} fps={30} width={1080} height={1920} />
    <Composition id="Recogida1Volt" component={Recogida1Volt} durationInFrames={DURACION_RECOGIDA_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Gasolina1Volt" component={Gasolina1Volt} durationInFrames={DURACION_GASOLINA_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Horas1Volt" component={Horas1Volt} durationInFrames={DURACION_HORAS_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Decidir1Volt" component={Decidir1Volt} durationInFrames={DURACION_DECIDIR_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Comision1Volt" component={Comision1Volt} durationInFrames={DURACION_COMISION_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Rechazar1Volt" component={Rechazar1Volt} durationInFrames={DURACION_RECHAZAR_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Desgaste1Volt" component={Desgaste1Volt} durationInFrames={DURACION_DESGASTE_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Seguridad1Volt" component={Seguridad1Volt} durationInFrames={DURACION_SEGURIDAD_1VOLT} fps={30} width={1080} height={1920} />
    <Composition id="Bloqueo1Volt" component={Bloqueo1Volt} durationInFrames={DURACION_BLOQUEO_1VOLT} fps={30} width={1080} height={1920} />
    </>
  );
};
