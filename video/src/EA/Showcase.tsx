import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { Headline } from "./components/Headline";
import { Subtitles } from "./components/Subtitles";
import { StatCard } from "./components/StatCard";
import { Popup } from "./components/Popup";
import { Chart } from "./components/Chart";
import { Comparison } from "./components/Comparison";
import { LowerThird } from "./components/LowerThird";
import { Arrow, Box, Circle, Sello, Underline } from "./components/Annotations";
import { Logo } from "./components/Logo";
import { Transition } from "./components/Transition";

const SEC = 72; // frames por sección (2.4 s)

const Etiqueta: React.FC<{ n: number; total: number; nombre: string }> = ({ n, total, nombre }) => {
  const frame = useCurrentFrame();
  const p = progreso(frame, 0, EA.mov.corto);
  return (
    <div style={{ position: "absolute", left: EA.zona.lados, top: 130, fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 28, letterSpacing: 4, opacity: p, color: EA.color.negro }}>
      <span style={{ color: EA.color.naranja }}>●</span> {String(n).padStart(2, "0")} / {total} · {nombre}
    </div>
  );
};

const Antes: React.FC = () => (
  <div style={{ position: "absolute", left: 0, right: 0, top: 800, textAlign: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 130, color: EA.color.negro }}>Antes</div>
);
const Despues: React.FC = () => (
  <div style={{ position: "absolute", left: 0, right: 0, top: 800, textAlign: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 130, color: EA.color.negro }}>
    Después<span style={{ color: EA.color.naranja }}>.</span>
  </div>
);

const GrandeNeto: React.FC = () => (
  <div style={{ position: "absolute", left: 0, right: 0, top: 740, textAlign: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 170, letterSpacing: -4, color: EA.color.negro }}>$52.40</div>
);

const secciones: [string, React.FC][] = [
  ["EA_Subtitles", () => <Subtitles y={820} tam={78} bloques={[{ de: 0, a: SEC, lineas: [[{ t: "Haz la " }, { t: "cuenta", naranja: true }], [{ t: "antes de aceptar" }]] }]} />],
  [
    "EA_Popup · s / m / l",
    () => (
      <>
        <Popup tam="l" etiqueta="Gasolina" titulo="Cada km te cuesta $2.00" texto="12 km por litro a $24" x={130} y={330} inicio={0} />
        <Popup tam="m" etiqueta="Desgaste" titulo="Llantas y aceite" texto="$1.50 por km" x={240} y={790} inicio={10} />
        <Popup tam="s" etiqueta="Tip" titulo="Mide tu neta" x={320} y={1170} inicio={20} />
      </>
    ),
  ],
  [
    "EA_Headline",
    () => (
      <Headline
        x={EA.zona.lados}
        y={520}
        inicio={0}
        tam={104}
        lineas={[[{ t: "Ideas que" }], [{ t: "conectan.", naranja: true }], [{ t: "Contenido" }], [{ t: "que " }, { t: "destaca.", naranja: true }]]}
      />
    ),
  ],
  [
    "EA_StatCard · dinero / % / cantidad",
    () => (
      <>
        <StatCard etiqueta="Ingreso" valor={1240} decimales={2} indicador={{ sube: true, texto: "12 % vs ayer" }} x={140} y={340} w={800} inicio={0} />
        <StatCard etiqueta="Aceptación" valor={62} prefijo="" sufijo=" %" indicador={{ sube: false, texto: "3 puntos" }} x={140} y={650} w={800} inicio={8} />
        <StatCard etiqueta="Viajes" valor={14} prefijo="" x={140} y={960} w={800} inicio={16} />
      </>
    ),
  ],
  ["EA_Comparison", () => <Comparison y={640} inicio={0} a={{ etiqueta: "Tarifa", valor: 80, decimales: 2 }} b={{ etiqueta: "Neta", valor: 52.4, decimales: 2 }} diferencia="−$27.60" />],
  [
    "EA_Chart · horizontal / vertical / progreso",
    () => (
      <>
        <Chart tipo="horizontal" x={60} y={270} w={960} h={380} inicio={0} prefijo="$" decimales={1} datos={[{ etiqueta: "Tarifa", valor: 80 }, { etiqueta: "Gasolina", valor: 18.6 }, { etiqueta: "Desgaste", valor: 9 }, { etiqueta: "Neta", valor: 52.4, destacado: true }]} />
        <Chart tipo="vertical" x={60} y={700} w={960} h={400} inicio={10} prefijo="$" datos={[{ etiqueta: "Lun", valor: 900 }, { etiqueta: "Mar", valor: 1100 }, { etiqueta: "Mié", valor: 780 }, { etiqueta: "Jue", valor: 1240, destacado: true }]} />
        <Chart tipo="progreso" x={60} y={1150} w={960} h={120} inicio={20} sufijo=" %" datos={[{ etiqueta: "Meta", valor: 62, destacado: true }]} />
      </>
    ),
  ],
  [
    "EA_LowerThird",
    () => (
      <>
        <LowerThird nombre="Eduardo Alvarado" rol="Creador de contenido" x={60} y={500} inicio={0} fin={100000} />
        <LowerThird nombre="Checa" rol="App para conductores" x={60} y={760} inicio={10} fin={100000} />
        <LowerThird nombre="Costo por km" rol="Concepto" x={60} y={1020} inicio={20} fin={100000} />
      </>
    ),
  ],
  [
    "EA_Annotations",
    () => (
      <>
        <GrandeNeto />
        <Box x={210} y={740} w={660} h={210} inicio={0} />
        <Underline x={210} y={985} w={660} inicio={14} />
        <Arrow x1={140} y1={1240} x2={330} y2={995} curva={-60} inicio={22} />
        <div style={{ position: "absolute", left: 0, right: 0, top: 1318, textAlign: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 54, color: EA.color.negro }}>tu ganancia real</div>
        <Circle cx={540} cy={1352} rx={300} ry={66} inicio={30} />
        <Sello texto="Neta" x={700} y={560} inicio={34} />
      </>
    ),
  ],
  [
    "EA_Logo · compacta / horizontal",
    () => (
      <>
        <Logo x={420} y={640} tam={240} inicio={0} />
        <Logo x={190} y={1040} tam={120} inicio={14} variante="horizontal" />
      </>
    ),
  ],
  [
    "EA_Transition",
    () => (
      <>
        <Sequence durationInFrames={30}>
          <Antes />
        </Sequence>
        <Sequence from={30}>
          <Despues />
        </Sequence>
        <Transition inicio={14} dur={32} />
      </>
    ),
  ],
];

export const DURACION_SHOWCASE = secciones.length * SEC;

export const Showcase: React.FC = () => (
  <AbsoluteFill style={{ background: EA.color.marfil }}>
    {secciones.map(([nombre, Seccion], i) => (
      <Sequence key={nombre} from={i * SEC} durationInFrames={SEC} name={nombre}>
        <Seccion />
        <Etiqueta n={i + 1} total={secciones.length} nombre={nombre} />
      </Sequence>
    ))}
  </AbsoluteFill>
);
