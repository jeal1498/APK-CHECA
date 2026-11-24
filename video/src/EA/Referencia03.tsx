import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";
import { LowerThird } from "./components/LowerThird";
import { Logo } from "./components/Logo";
import { Linea } from "./components/Resaltado";
import { OfertaCard, Fila } from "./components/OfertaCard";

/**
 * Referencia 03 (PERSONAL): conductor a conductor. Guion de Hooks Virales «000 Personal/001».
 * Elemento que persiste: una tarjeta de oferta (ejemplo) que crece, se compara con otra y termina en globo.
 * Cuentas (gasolina = km totales ÷ 12 km/l × $24/l):
 *   A: $100, 15 km, 30 min → 15 ÷ 12 × 24 = $30 → te quedan $70.
 *   B: $85,   5 km, 15 min →  5 ÷ 12 × 24 = $10 → te quedan $75.
 * Tiempos estimados (~2.8 palabras/s). Con la voz de Eduardo se cambian solo los fotogramas de P (Whisper).
 */
export const DURACION_REFERENCIA_03 = s(26);

/** Fotograma de cada palabra clave: todos los picos de movimiento cuelgan de aquí. */
const P = {
  perdiendo: 60,
  dejando: 165,
  oferta: 265,
  paga: 340,
  kilometros: 395,
  tiempo: 415,
  gasolina: 455,
  alto: 525,
  mas: 610,
  dato: 660,
  aceptar: 715,
};

const BLOQUES: Bloque[] = [
  B(0, 2.6, [t("¿Y si te dijera que estás")], [n("perdiendo"), t(" dinero")]),
  B(2.6, 4.6, [t("cada vez que aceptas")], [t("un "), n("viaje"), t("?")]),
  B(4.6, 6.4, [t("Puede que estés")], [n("dejando"), t(" dinero")]),
  B(6.4, 7.4, [t("sin darte "), n("cuenta")]),
  B(7.4, 9.2, [t("Cuando te llega")], [t("una "), n("oferta")]),
  B(9.2, 11.6, [t("no te fijes solo")], [t("en cuánto "), n("paga")]),
  B(11.6, 13.4, [t("Ten en cuenta")], [t("los "), n("kilómetros")]),
  B(13.4, 14.2, [t("el "), n("tiempo")]),
  B(14.2, 16.6, [t("y la "), n("gasolina")], [t("que vas a gastar")]),
  B(16.6, 18.8, [t("Un pago "), n("alto")], [t("no siempre significa")]),
  B(18.8, 21.2, [t("que te va a quedar")], [n("más dinero")]),
  B(21.2, 23.0, [t("¿Qué "), n("dato"), t(" tomas")], [t("en cuenta")]),
  B(23.0, 26, [t("antes de "), n("aceptar"), t("?")]),
];

const lerp = (a: number, b: number, p: number) => a + (b - a) * p;
const CY = 1060;

export const Referencia03: React.FC = () => {
  const f = useCurrentFrame();

  // Tarjeta A (la que persiste)
  const entra = progreso(f, 8, 20);
  const oferta = progreso(f, P.kilometros - 14, 20);
  const etiqueta = progreso(f, P.oferta - 4, 12);
  const compara = progreso(f, P.alto - 12, 22);
  const globo = progreso(f, P.dato - 22, 24);
  const escA = lerp(lerp(lerp(0.94, 1, entra), 0.72, compara), 1, globo);
  const cxA = lerp(lerp(540, 290, compara), 540, globo);
  const wA = lerp(lerp(560, 620, oferta), 720, globo);
  const hA = lerp(lerp(240, 480, oferta), 240, globo);

  // Contenido (nunca dos textos encimados)
  const interroga = progreso(f, P.perdiendo - 2, 12) * (1 - progreso(f, P.dejando - 4, 10));
  const circulo = progreso(f, P.paga - 4, 14) * (1 - progreso(f, P.kilometros - 30, 10));
  const sale = progreso(f, P.mas - 26, 10);
  const queda = progreso(f, P.mas - 14, 12);
  const opContenido = 1 - progreso(f, P.dato - 34, 10);
  const pregunta = progreso(f, P.dato - 2, 12);

  // Tarjeta B (solo para comparar)
  const entraB = progreso(f, P.alto + 8, 16) * (1 - progreso(f, P.dato - 34, 10));
  const subraya = progreso(f, P.mas, 14);

  return (
    <AbsoluteFill style={{ background: EA.color.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: EA.color.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 3, background: EA.color.negro }} />
      <LowerThird nombre="Eduardo Alvarado" rol="Conductor" x={EA.zona.lados} y={660} inicio={9} fin={80} />

      {/* Tarjeta B: $85, 5 km */}
      {entraB > 0.001 ? (
        <OfertaCard cx={790} cy={CY + (1 - entraB) * 40} w={620} h={480} escala={0.72} opacidad={entraB}>
          <Monto viejo="$85" nuevo="$75" sale={sale} queda={queda} centrado={0} subraya={subraya} etiqueta={1} />
          <Fila y={190} op={1} texto="5 km" />
          <Fila y={270} op={1} texto="15 min" />
          <Fila y={350} op={1} texto="Gasolina −$10" cuenta="5 ÷ 12 km/l × $24" />
        </OfertaCard>
      ) : null}

      {/* Tarjeta A: $100, 15 km → globo */}
      <OfertaCard cx={cxA} cy={CY + (1 - entra) * 50} w={wA} h={hA} escala={escA} opacidad={entra} radio={lerp(12, 60, globo)} cola={progreso(f, P.dato + 2, 12)}>
        <div style={{ position: "absolute", inset: 0, opacity: opContenido }}>
          <Monto viejo="$100" nuevo="$70" sale={sale} queda={queda} centrado={1 - oferta} interroga={interroga} circulo={circulo} etiqueta={etiqueta} />
          <Fila y={190} op={progreso(f, P.kilometros + 4, 12)} texto="15 km" />
          <Fila y={270} op={progreso(f, P.tiempo - 2, 12)} texto="30 min" />
          <Fila y={350} op={progreso(f, P.gasolina - 4, 12)} texto="Gasolina −$30" cuenta="15 ÷ 12 km/l × $24" />
        </div>
        {pregunta > 0 ? (
          <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 66, letterSpacing: -2, opacity: pregunta }}>
            <Linea partes={[t("¿Qué "), n("dato"), t(" tomas?")]} frame={f} inicioResalte={P.dato + 4} durResalte={14} />
          </div>
        ) : null}
      </OfertaCard>

      <Logo x={472} y={1236} tam={96} inicio={P.aceptar - 10} />

      <Subtitles y={1370} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};

/** Monto grande de la tarjeta: tarifa → «Te queda». `centrado` 1 = al centro de la tarjeta chica. */
const Monto: React.FC<{ viejo: string; nuevo: string; sale: number; queda: number; centrado: number; interroga?: number; circulo?: number; subraya?: number; etiqueta?: number }> = ({
  viejo,
  nuevo,
  sale,
  queda,
  centrado,
  interroga = 0,
  circulo = 0,
  subraya = 0,
  etiqueta = 0,
}) => {
  const top = lerp(30, 45, centrado);
  const tam = lerp(124, 150, centrado);
  const base: React.CSSProperties = { position: "absolute", left: 0, right: 0, top, height: 170, display: "flex", alignItems: "center", justifyContent: "center", fontSize: tam, letterSpacing: -4, lineHeight: 1 };
  const rotulo: React.CSSProperties = { position: "absolute", left: 0, right: 0, top: 16, textAlign: "center", fontFamily: EA.font.texto, fontWeight: EA.peso.medio, fontSize: 30, letterSpacing: 4, textTransform: "uppercase" };
  return (
    <>
      {etiqueta * (1 - sale) > 0.001 ? <div style={{ ...rotulo, opacity: etiqueta * (1 - sale), color: "rgba(21,21,21,0.7)" }}>Oferta · ejemplo</div> : null}
      <div style={{ ...base, opacity: 1 - sale }}>
        <span style={{ position: "relative" }}>
          {viejo}
          {interroga > 0 ? <span style={{ position: "absolute", left: "100%", marginLeft: 10, color: EA.color.naranja, opacity: interroga, transform: `scale(${0.6 + 0.4 * interroga})`, display: "inline-block" }}>?</span> : null}
        </span>
        {circulo > 0 ? (
          <svg style={{ position: "absolute", left: "50%", top: "50%", marginLeft: -190, marginTop: -78 }} width={380} height={156}>
            <ellipse cx={190} cy={78} rx={180} ry={68} fill="none" stroke={EA.color.naranja} strokeWidth={9} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - circulo} opacity={Math.min(1, circulo * 8)} />
          </svg>
        ) : null}
      </div>
      {queda > 0 ? (
        <>
          <div style={{ ...rotulo, opacity: queda }}>Te queda</div>
          <div style={{ ...base, top: 58, height: 124, opacity: queda, transform: `translateY(${(1 - queda) * 14}px)` }}>
            <span style={{ position: "relative" }}>
              {nuevo}
              <span style={{ position: "absolute", left: -8, right: -8, bottom: -10, height: 14, background: EA.color.naranja, borderRadius: 4, transform: `scaleX(${subraya})`, transformOrigin: "left center" }} />
            </span>
          </div>
        </>
      ) : null}
    </>
  );
};
