import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { EA, progreso } from "./theme";
import { B, n, s, t } from "./Referencia";
import { Subtitles } from "./components/Subtitles";
import type { Bloque } from "./components/Subtitles";
import { LowerThird } from "./components/LowerThird";
import { Logo } from "./components/Logo";
import { Unidades } from "./components/Unidades";
import type { EstadoUnidad } from "./components/Unidades";
import { Interruptor } from "./components/Interruptor";
import { Tarjeta } from "./components/Tarjeta";

/**
 * Referencia 02 (PERSONAL): «¿Cuánto te queda de este viaje?», una sola pieza que se transforma.
 * Elemento que persiste: 80 cuadros (1 cuadro = $1) → tarifa partida, cuenta, gasolina (28), barra 35 %, tarjeta $52, globo.
 * Cuenta: 14 km ÷ 12 km/l × $24/l = $28; 80 − 28 = 52; 28 ÷ 80 = 35 %.
 * Tiempos estimados de la voz (con la voz real salen de las palabras de Whisper).
 */
export const DURACION_REFERENCIA_02 = s(26);

const BLOQUES: Bloque[] = [
  B(0, 2.6, [t("Un viaje de $80")], [n("no siempre"), t(" paga")]),
  B(2.6, 3.95, [t("Parece "), n("buen dinero")]),
  B(3.95, 6.2, [t("Antes de aceptar,")], [t("falta una "), n("cuenta")]),
  B(6.2, 7.85, [t("Son "), n("14 kilómetros")]),
  B(7.85, 9.7, [t("Con tu auto a 12")], [t("por litro")]),
  B(9.7, 11, [t("y la gasolina a "), n("$24")]),
  B(11, 12, [t("gastas "), n("$28")]),
  B(12, 14.4, [t("Eso es el "), n("35 %")], [t("de la tarifa")]),
  B(14.4, 15.65, [t("Te quedan "), n("$52")]),
  B(15.65, 16.8, [t("No "), n("$80")]),
  B(16.8, 19.1, [t("Haz la "), n("cuenta")], [t("antes de aceptar")]),
  B(19.1, 21.6, [t("Un número")], [t("cambia la "), n("decisión")]),
  B(21.6, 23, [t("¿Tú la "), n("haces"), t("?")]),
  B(23, 26, [t("Cuéntame en")], [t("los "), n("comentarios")]),
];

// Geometría (1080×1920). Rejilla de 80 cuadros: 16 columnas × 5 filas.
const X0 = 64;
const Y0 = 975;
const CEL = 52;
const HUE = 8;
const FIL = 5;
const GW = 16 * CEL + 15 * HUE; // 952
const GH = FIL * CEL + (FIL - 1) * HUE; // 292
const CX = 540;
const CY = Y0 + GH / 2; // 1121
const TY = 1296; // pista del 35 %
const TH = 52;
const BARRA = GW * 0.35;

const cl = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
const pos = (i: number): [number, number] => [X0 + Math.floor(i / FIL) * (CEL + HUE), Y0 + (i % FIL) * (CEL + HUE)];

const estadoCelda = (i: number, f: number): EstadoUnidad => {
  const [x, y] = pos(i);
  const p = progreso(f, 98 + i * 0.99, 6); // escena 2: se cuentan hasta 80
  let opacidad = p;
  let escala = 0.3 + 0.7 * p;
  let escalaY: number | undefined;
  let color: string = EA.color.negro;
  let dy = 0;
  if (i < 28) {
    // escena 3: la gasolina se come 28 cuadros (naranja, de columna en columna)
    const ti = 296 + i * 1.86;
    if (f >= ti) color = EA.color.naranja;
    escala *= 1 + 0.08 * Math.sin(Math.PI * cl((f - ti) / 8, 0, 1));
    // escena 4: se aplastan y bajan a la pista; ahí se vuelven la barra del 35 %
    const vuelo = progreso(f, 364, 16);
    dy = (TY + TH / 2 - CEL / 2 - y) * vuelo;
    escalaY = escala * (1 - 0.85 * vuelo);
    opacidad *= 1 - progreso(f, 376, 8);
  } else {
    // escena 5: los 52 que quedan se funden con la tarjeta de $52 (el bloque se contrae, ver Referencia02)
    opacidad *= 1 - progreso(f, 444, 10);
  }
  return { opacidad, escala, escalaY, color, dy };
};

const Etiq: React.FC<{ x?: number; right?: number; y: number; op: number; plano?: boolean; size?: number; children: React.ReactNode }> = ({ x, right, y, op, plano, size = 56, children }) => (
  <div
    style={{
      position: "absolute",
      left: x,
      right,
      top: y,
      opacity: op,
      transform: `translateY(${(1 - op) * 16}px)`,
      background: plano ? "transparent" : EA.color.naranja,
      color: EA.color.negro,
      padding: plano ? 0 : "2px 18px",
      borderRadius: 4,
      fontFamily: plano ? EA.font.texto : EA.font.titulo,
      fontWeight: plano ? EA.peso.medio : EA.peso.extra,
      letterSpacing: plano ? 3 : -1,
      fontSize: plano ? 34 : size,
      lineHeight: 1.15,
    }}
  >
    {children}
  </div>
);

/** Escena 1–2: la tarifa de $80 como tarjeta, partida por una línea y luego deshecha en la rejilla. */
const TarjetaPartida: React.FC = () => {
  const f = useCurrentFrame();
  if (f > 104) return null;
  const entra = progreso(f, 9, 16);
  const expande = progreso(f, 80, 20);
  const W = 540 + (GW - 540) * expande;
  const H = 300 + (GH - 300) * expande;
  const corte = progreso(f, 34, 10);
  const reune = progreso(f, 78, 8);
  const sep = progreso(f, 44, 12) * (1 - reune);
  const fade = 1 - progreso(f, 88, 12);
  const clip = {
    izq: "polygon(0 0, calc(50% + 90px) 0, calc(50% - 90px) 100%, 0 100%)",
    der: "polygon(calc(50% + 90px) 0, 100% 0, 100% 100%, calc(50% - 90px) 100%)",
  } as const;
  const mitad = (lado: "izq" | "der") => {
    const signo = lado === "izq" ? -1 : 1;
    return (
      <div
        style={{
          position: "absolute",
          inset: 0,
          clipPath: clip[lado],
          transform: `translateX(${signo * 14 * sep}px) rotate(${signo * 1.2 * sep}deg)`,
        }}
      >
        <div style={{ position: "absolute", inset: 0, boxSizing: "border-box", background: EA.color.blanco, border: EA.borde, borderRadius: EA.radio, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 170, letterSpacing: -5, color: EA.color.negro }}>
          <span style={{ opacity: 1 - reune }}>$80</span>
        </div>
        <svg style={{ position: "absolute", left: 0, top: 0, opacity: cl(sep * 6, 0, 1) }} width={W} height={H}>
          <line x1={W / 2 + 90} y1={0} x2={W / 2 - 90} y2={H} stroke={EA.color.negro} strokeWidth={6} />
        </svg>
      </div>
    );
  };
  const unida = sep < 0.001;
  return (
    <div style={{ position: "absolute", left: CX - W / 2, top: CY - H / 2, width: W, height: H, opacity: entra * fade, transform: `translateY(${(1 - entra) * 50}px) scale(${0.94 + 0.06 * entra})` }}>
      {unida ? (
        <div style={{ position: "absolute", inset: 0, boxSizing: "border-box", background: EA.color.blanco, border: EA.borde, borderRadius: EA.radio, boxShadow: EA.sombra, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 170, letterSpacing: -5, color: EA.color.negro }}>
          <span style={{ opacity: 1 - reune }}>$80</span>
        </div>
      ) : (
        <>
          <div style={{ position: "absolute", inset: 0, borderRadius: EA.radio, boxShadow: EA.sombra, opacity: 1 - sep }} />
          {mitad("izq")}
          {mitad("der")}
        </>
      )}
      <svg style={{ position: "absolute", left: 0, top: 0, opacity: 1 - reune }} width={W} height={H}>
        <line x1={W / 2 + 90} y1={-8} x2={W / 2 - 90} y2={H + 8} pathLength={1} stroke={EA.color.naranja} strokeWidth={10} strokeLinecap="round" strokeDasharray={1} strokeDashoffset={1 - corte} />
      </svg>
    </div>
  );
};

export const Referencia02: React.FC = () => {
  const f = useCurrentFrame();
  const salePista = 1 - progreso(f, 432, 10);
  const blur = 4 * progreso(f, 186, 12) * (1 - progreso(f, 262, 12));
  const salidaTira = 1 - progreso(f, 248, 12);
  const contrae = progreso(f, 432, 22);
  const cuentaCuadros = cl(Math.floor((f - 98) / 0.99) + 1, 0, 80);
  const opConteo = progreso(f, 104, 10) * (1 - progreso(f, 178, 10));

  // Tarjeta de $52 → globo de comentario
  const entra = progreso(f, 442, 12);
  const grande = progreso(f, 516, 18);
  const sube = progreso(f, 652, 18);
  const cyTarjeta = CY - 40 * grande - 41 * sube;
  const esc = (0.7 + 0.3 * entra) * (1 + 0.25 * grande);
  const cuenta52 = Math.round(interpolate(f, [442, 456], [0, 52], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }));

  // Fantasma de $80 tachado
  const desvanece = 1 - progreso(f, 506, 12);
  const fantasma = progreso(f, 452, 12) * (0.55 - 0.25 * progreso(f, 490, 10)) * desvanece;

  return (
    <AbsoluteFill style={{ background: EA.color.marfil }}>
      {/* Cámara (aquí va el video de Eduardo) */}
      <div style={{ position: "absolute", left: 0, top: 0, width: 1080, height: 800, background: EA.color.papel, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: EA.font.texto, fontSize: 34, color: "rgba(21,21,21,0.45)" }}>
        [ video de Eduardo a cámara ]
      </div>
      <div style={{ position: "absolute", left: 0, top: 800, width: 1080, height: 3, background: EA.color.negro }} />
      <LowerThird nombre="Eduardo Alvarado" x={EA.zona.lados} y={700} inicio={9} fin={70} />

      {/* 1–2 · la tarifa y su cuenta */}
      <TarjetaPartida />
      <div style={{ position: "absolute", inset: 0, transformOrigin: "690px 1121px", transform: `translateX(${-150 * contrae}px) scale(${1 - 0.333 * contrae}, ${1 - 0.171 * contrae})` }}>
        <Unidades x={X0} y={Y0} blur={blur} estado={(i) => estadoCelda(i, f)} />
      </div>
      <Etiq x={X0} y={874} op={opConteo} plano>
        1 cuadro = $1
      </Etiq>
      <Etiq right={1080 - (X0 + GW)} y={866} op={opConteo}>
        {cuentaCuadros}
      </Etiq>

      {/* 3 · la ruta de 14 km y la gasolina */}
      {Array.from({ length: 14 }, (_, i) => (
        <div key={i} style={{ position: "absolute", left: 68 + i * 68, top: 920, width: 60, height: 22, background: EA.color.negro, borderRadius: 3, transform: `scaleX(${progreso(f, 186 + i * 3.6, 6)})`, transformOrigin: "left center", opacity: salidaTira }} />
      ))}
      <Etiq x={68} y={866} op={progreso(f, 196, 10) * salidaTira}>
        14 km
      </Etiq>
      <Etiq x={X0} y={866} op={progreso(f, 344, 10) * (1 - progreso(f, 362, 10))}>
        $28
      </Etiq>

      {/* 4 · la barra del 35 % de la tarifa */}
      <div style={{ position: "absolute", left: X0, top: TY, width: GW, height: TH, boxSizing: "border-box", border: EA.borde, borderRadius: 6, opacity: progreso(f, 360, 10) * salePista, transform: `scaleX(${progreso(f, 360, 10)})`, transformOrigin: "left center" }} />
      <div style={{ position: "absolute", left: X0, top: TY, width: BARRA, height: TH, background: EA.color.naranja, borderRadius: 6, opacity: progreso(f, 374, 12) * salePista, transform: `scaleY(${0.15 + 0.85 * progreso(f, 374, 12)})` }} />
      <div style={{ position: "absolute", left: X0 + BARRA + 26, top: TY, height: TH, lineHeight: `${TH}px`, fontFamily: EA.font.titulo, fontWeight: EA.peso.extra, fontSize: 50, color: EA.color.negro, opacity: progreso(f, 376, 8) * salePista }}>
        {Math.round(interpolate(f, [376, 386], [0, 35], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }))} %
      </div>

      {/* 5 · fantasma de $80 tachado */}
      {fantasma > 0.001 ? (
        <>
          <Tarjeta cx={CX} cy={CY} w={540} h={300} borde="punteado" fondo="transparent" sombra={false} blur={3} opacidad={fantasma}>
            <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 170, letterSpacing: -5, color: "rgba(21,21,21,0.6)" }}>$80</div>
          </Tarjeta>
          <svg style={{ position: "absolute", left: 0, top: 0, opacity: desvanece }} width={1080} height={1920}>
            <line x1={CX + 215} y1={CY - 145} x2={CX - 215} y2={CY + 145} pathLength={1} stroke={EA.color.naranja} strokeWidth={10} strokeLinecap="round" strokeDasharray={1} strokeDashoffset={1 - progreso(f, 486, 10)} />
          </svg>
        </>
      ) : null}

      {/* 5–7 · tarjeta de $52 que crece y se vuelve globo de comentario */}
      {f >= 438 ? (
        <Tarjeta cx={CX} cy={cyTarjeta} w={435} h={242} escala={esc} opacidad={entra} radio={10 + 46 * sube} cola={progreso(f, 664, 12)}>
          <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 150, letterSpacing: -4, opacity: 1 - progreso(f, 652, 10) }}>${cuenta52}</div>
          <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 62, letterSpacing: -1, opacity: progreso(f, 670, 10) }}>¿La haces?</div>
        </Tarjeta>
      ) : null}

      {/* 6 · el número cambia la decisión */}
      <Etiq x={110} y={1274} op={progreso(f, 570, 10) * (1 - progreso(f, 648, 8))} size={60}>
        ¿Sí o no?
      </Etiq>
      <Interruptor x={650} y={1272} inicio={570} cambia={622} fin={656} />

      {/* 7 · firma */}
      <Logo x={445} y={1275} tam={96} inicio={726} />

      <Subtitles y={1388} bloques={BLOQUES} />
    </AbsoluteFill>
  );
};
