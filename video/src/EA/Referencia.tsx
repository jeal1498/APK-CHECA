import React from "react";
import { AbsoluteFill, Sequence, useCurrentFrame } from "remotion";
import { EA, progreso, salida } from "./theme";
import type { Parte } from "./theme";
import { Headline } from "./components/Headline";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";
import { StatCard } from "./components/StatCard";
import { Popup } from "./components/Popup";
import { Chart } from "./components/Chart";
import { Comparison } from "./components/Comparison";
import { LowerThird } from "./components/LowerThird";
import { Box, Circle, Sello, Underline } from "./components/Annotations";
import { Logo } from "./components/Logo";
import { Transition } from "./components/Transition";

/**
 * Referencia 01 (PERSONAL): «¿Cuánto te queda de este viaje?»
 * Estructura del guion: gancho → contexto → desarrollo → conclusión → interacción.
 * Cuenta: 14 km totales ÷ 12 km/l × $24/l = $28 de gasolina; 80 − 28 = 52; 28 ÷ 80 = 35 %.
 */
export const s = (seg: number) => Math.round(seg * 30);
export const DURACION_REFERENCIA = s(26);

export const B = (de: number, a: number, ...lineas: Parte[][]): Bloque => ({ de: s(de), a: s(a), lineas });
export const t = (x: string): Parte => ({ t: x });
export const n = (x: string): Parte => ({ t: x, naranja: true });

const BLOQUES: Bloque[] = [
  B(0, 2.6, [t("Un viaje de $80")], [n("no siempre"), t(" paga")]),
  B(2.6, 4.2, [t("Parece "), n("buen dinero")]),
  B(4.2, 6, [t("Antes de aceptar,")], [t("falta una "), n("cuenta")]),
  B(6, 8, [t("Son "), n("14 kilómetros")]),
  B(8, 11, [t("Gasolina a $24:")], [t("gastas "), n("$28")]),
  B(11, 14.5, [t("Eso es el "), n("35 %")], [t("de la tarifa")]),
  B(14.5, 16.2, [t("Te quedan "), n("$52")]),
  B(16.2, 18, [t("No "), n("$80")]),
  B(18, 20.3, [t("Haz la "), n("cuenta")], [t("antes de aceptar")]),
  B(20.3, 22.5, [t("Un número")], [t("cambia la "), n("decisión")]),
  B(22.5, 24.2, [t("¿Tú la "), n("haces"), t("?")]),
  B(24.2, 26, [t("Cuéntame en")], [t("los "), n("comentarios")]),
];

const Fundido: React.FC<{ dur: number; children: React.ReactNode }> = ({ dur, children }) => {
  const f = useCurrentFrame();
  return <AbsoluteFill style={{ opacity: salida(f, dur, 8) }}>{children}</AbsoluteFill>;
};
const Acto: React.FC<{ de: number; a: number; children: React.ReactNode }> = ({ de, a, children }) => (
  <Sequence from={s(de)} durationInFrames={s(a) - s(de)}>
    <Fundido dur={s(a) - s(de)}>{children}</Fundido>
  </Sequence>
);

const Grande: React.FC = () => {
  const f = useCurrentFrame();
  const p = progreso(f, 0, EA.mov.medio);
  return (
    <div style={{ position: "absolute", left: 0, right: 0, top: 880, textAlign: "center", opacity: p, transform: `translateY(${(1 - p) * 40}px)`, fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 220, letterSpacing: -6, color: EA.color.negro, lineHeight: 1.1 }}>
      $52
    </div>
  );
};

export const Referencia: React.FC = () => {
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
      <LowerThird nombre="Eduardo Alvarado" rol="Creador de contenido" x={EA.zona.lados} y={680} inicio={s(0.3)} fin={s(5.5)} />
      <div style={{ position: "absolute", left: EA.zona.lados, right: EA.zona.lados, top: 822, display: "flex", justifyContent: "space-between", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 22, letterSpacing: 4, color: EA.color.negro, opacity: meta }}>
        <span>
          <span style={{ color: EA.color.naranja }}>●</span> ANÁLISIS 01
        </span>
        <span>IDEAS QUE CONECTAN</span>
      </div>

      {/* Acto A · gancho y contexto */}
      <Acto de={0} a={6}>
        <Headline x={EA.zona.lados} y={868} inicio={s(0.3)} lineas={[[t("¿Cuánto "), n("te queda")], [t("de este viaje?")]]} />
      </Acto>

      {/* Acto B · datos del viaje */}
      <Acto de={6} a={11}>
        <Popup tam="s" etiqueta="Datos" titulo="14 km" texto="12 km por litro, a $24" x={60} y={900} inicio={0} />
        <StatCard etiqueta="Gasolina" valor={28} decimales={2} x={560} y={900} w={460} inicio={s(2)} />
      </Acto>

      {/* Acto C · qué parte de la tarifa se va */}
      <Acto de={11} a={14.5}>
        <Chart tipo="progreso" x={60} y={980} w={920} h={120} inicio={0} sufijo=" %" datos={[{ etiqueta: "Gasolina", valor: 35, destacado: true }]} />
        <Circle cx={968} cy={1040} rx={92} ry={52} inicio={s(1.4)} />
      </Acto>

      {/* Acto D · la comparación */}
      <Acto de={14.5} a={18}>
        <Comparison y={1010} inicio={0} a={{ etiqueta: "Tarifa", valor: 80, decimales: 2 }} b={{ etiqueta: "Neta", valor: 52, decimales: 2 }} diferencia="−$28" />
      </Acto>

      {/* Acto E · conclusión */}
      <Acto de={18} a={22.5}>
        <Grande />
        <Box x={300} y={900} w={480} h={250} inicio={s(0.6)} />
        <Underline x={300} y={1180} w={480} inicio={s(1.2)} />
        <Sello texto="Tu neta" x={600} y={1235} inicio={s(1.8)} />
      </Acto>

      {/* Acto F · interacción */}
      <Acto de={22.5} a={26}>
        <Popup tam="m" etiqueta="Pregunta" titulo="¿La haces antes de aceptar?" texto="Cuéntame en los comentarios." x={240} y={880} inicio={0} />
        <Logo x={290} y={1200} tam={100} inicio={s(1)} variante="horizontal" />
      </Acto>

      <Transition inicio={s(18) - 12} dur={24} />
      <Subtitles y={1388} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};
