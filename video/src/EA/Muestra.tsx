import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { Headline } from "./components/Headline";
import { Subtitles } from "./components/Subtitles";
import { StatCard } from "./components/StatCard";
import { Arrow, Circle } from "./components/Annotations";
import { LowerThird } from "./components/LowerThird";
import { Logo } from "./components/Logo";

export const DURACION_MUESTRA = 180; // 6 s a 30 fps

export const Muestra: React.FC = () => {
  const frame = useCurrentFrame();
  const meta = progreso(frame, 10, EA.mov.medio);
  return (
    <AbsoluteFill style={{ background: EA.color.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: EA.color.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 3, background: EA.color.negro }} />
      <Logo x={850} y={EA.zona.arriba - 20} inicio={4} />
      <LowerThird nombre="Eduardo Alvarado" rol="Creador de contenido" x={EA.zona.lados} y={680} inicio={8} fin={100} />

      {/* Metadatos editoriales */}
      <div style={{ position: "absolute", left: EA.zona.lados, right: EA.zona.lados, top: 822, display: "flex", justifyContent: "space-between", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 22, letterSpacing: 4, color: EA.color.negro, opacity: meta }}>
        <span>
          <span style={{ color: EA.color.naranja }}>●</span> ANÁLISIS 01
        </span>
        <span>IDEAS QUE CONECTAN</span>
      </div>

      <Headline
        x={EA.zona.lados}
        y={868}
        inicio={12}
        lineas={[
          [{ t: "¿Cuánto " }, { t: "te queda", naranja: true }],
          [{ t: "de este viaje?" }],
        ]}
      />

      <StatCard etiqueta="Tarifa" valor={80} decimales={2} x={60} y={1100} inicio={56} />
      <StatCard etiqueta="Neta" valor={52.4} decimales={2} indicador={{ sube: false, texto: "35 % menos" }} x={600} y={1100} inicio={74} />
      <Arrow x1={490} y1={1230} x2={592} y2={1230} curva={26} inicio={92} />
      <Circle cx={800} cy={1228} rx={190} ry={62} inicio={118} />

      <Subtitles
        y={1388}
        bloques={[
          { de: 6, a: 62, lineas: [[{ t: "Un viaje de $80" }], [{ t: "no siempre", naranja: true }, { t: " paga" }]] },
          { de: 62, a: 118, lineas: [[{ t: "Te quedan " }, { t: "$52", naranja: true }], [{ t: "tras la gasolina" }]] },
          { de: 118, a: 180, lineas: [[{ t: "Haz la " }, { t: "cuenta", naranja: true }], [{ t: "antes de aceptar" }]] },
        ]}
      />
    </AbsoluteFill>
  );
};
