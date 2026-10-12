import React from "react";
import { AbsoluteFill, Audio, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import {
  C,
  F,
  FD,
  Fondo,
  Logo,
  Mapa,
  Oferta,
  Popup,
  Subtitulo,
  Telefono,
  aparecer,
  useSpring,
} from "./ui";

// Guion «Este viaje de $150 en realidad te deja $90» — ~28 s a 30 fps, con voz
export const DURACION = 832;

const Sfx: React.FC<{ en: number; src: string; vol?: number }> = ({ en, src, vol = 1 }) => (
  <Sequence from={en} durationInFrames={30}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

// Voz en off (Piper, es_MX-ald), un archivo por escena
const Voz: React.FC<{ en: number; n: number }> = ({ en, n }) => (
  <Sequence from={en}>
    <Audio src={staticFile(`voz/v${n}.m4a`)} volume={1} />
  </Sequence>
);

const centro: React.CSSProperties = { position: "absolute", left: 0, right: 0, display: "flex", justifyContent: "center" };

// Escenas 1–3: la oferta de $150 que en realidad deja $90
const Gancho: React.FC = () => {
  const frame = useCurrentFrame();
  const entra = useSpring(0, { damping: 14, mass: 0.7 });
  const oferta = useSpring(4, { damping: 14, mass: 0.6 });
  const zoomTarifa = interpolate(frame, [12, 24, 50], [1, 1.18, 1.1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const pop = useSpring(72, { damping: 9, mass: 0.6 });
  const tachado = aparecer(frame, 78, 10);
  const acerca = interpolate(frame, [132, 152], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const gas = aparecer(frame, 144, 8);
  return (
    <AbsoluteFill>
      <Fondo />
      <div
        style={{
          ...centro,
          top: 560,
          transform: `translateY(${(1 - entra) * 600}px) scale(${1 + acerca * 0.55}) translateY(${acerca * 260}px)`,
          transformOrigin: "50% 20%",
        }}
      >
        <Telefono>
          <Mapa />
          <div style={{ ...centro, bottom: 30, transform: `translateY(${(1 - oferta) * 700}px)` }}>
            <Oferta app="App de viajes" tarifa="$150.00" tramos={["6 min (2.4 km)", "39 min (27.6 km)"]} zoom={zoomTarifa} tachado={tachado} />
          </div>
          {frame >= 72 ? (
            <div
              style={{
                ...centro,
                top: 50,
                opacity: Math.min(1, pop * 2),
                transform: `translateY(${(1 - pop) * -300}px) scale(${1.25 - 0.25 * pop})`,
              }}
            >
              <Popup
                ancho={640}
                nivel="amarillo"
                app="Viaje"
                neta={90}
                viaje={150}
                gas={60}
                resaltarGas={gas}
                fichas={[
                  { v: "$200", l: "por hora", c: "ok" },
                  { v: "$5.0", l: "por km", c: "mal" },
                  { v: "45 min", l: "30 km total", c: "neutro" },
                ]}
              />
            </div>
          ) : null}
        </Telefono>
      </div>
      <Sequence durationInFrames={72}>
        <Subtitulo texto="Este viaje de $150…" />
      </Sequence>
      <Sequence from={72} durationInFrames={60}>
        <Subtitulo texto="…en realidad te deja ^$90" />
      </Sequence>
      <Sequence from={132} durationInFrames={90}>
        <Subtitulo texto="La gasolina se lleva !$60 que no ves" />
      </Sequence>
      <Sfx en={0} src="whoosh.wav" />
      <Sfx en={72} src="golpe.wav" />
      <Sfx en={144} src="error.wav" vol={0.5} />
      <Voz en={0} n={1} />
      <Voz en={72} n={2} />
      <Voz en={132} n={3} />
    </AbsoluteFill>
  );
};

// Escena 4: por qué existe Checa
const PorQue: React.FC = () => {
  const frame = useCurrentFrame();
  const sale = aparecer(frame, 76, 10);
  const logo = useSpring(80, { damping: 10, mass: 0.6 });
  const linea = (texto: string, desde: number, color: string) => {
    const a = aparecer(frame, desde, 8);
    return (
      <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 110, color, opacity: a, transform: `translateY(${(1 - a) * 40}px)` }}>
        {texto}
      </div>
    );
  };
  return (
    <AbsoluteFill>
      <Fondo />
      <AbsoluteFill
        style={{
          alignItems: "center",
          justifyContent: "center",
          textAlign: "center",
          opacity: 1 - sale,
          transform: `translateY(${-sale * 120}px)`,
        }}
      >
        {linea("Soy conductor.", 0, C.texto)}
        {linea("Me cansé de eso.", 40, C.texto2)}
      </AbsoluteFill>
      {frame >= 80 ? (
        <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", gap: 60 }}>
          <div style={{ transform: `scale(${logo})` }}>
            <Logo tam={190} brillo={logo} />
          </div>
          <div style={{ fontFamily: FD, fontWeight: 800, fontSize: 76, color: C.verde, opacity: aparecer(frame, 84, 8) }}>
            Por eso hice Checa
          </div>
        </AbsoluteFill>
      ) : null}
      <Sfx en={80} src="ding.wav" vol={0.5} />
      <Voz en={0} n={4} />
    </AbsoluteFill>
  );
};

// Escenas 5 y 6: una oferta que conviene y otra que no
const Ejemplo: React.FC<{ bueno: boolean }> = ({ bueno }) => {
  const frame = useCurrentFrame();
  const entra = useSpring(0, { damping: 14 });
  const oferta = useSpring(2, { damping: 14 });
  const pop = useSpring(18, { damping: 9, mass: 0.6 });
  const temblor = bueno ? 0 : interpolate(frame, [18, 22, 26, 30, 34], [0, -18, 14, -8, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill>
      <Fondo tono={bueno ? "rgba(34,197,94,0.2)" : "rgba(239,68,68,0.18)"} />
      <div style={{ ...centro, top: 560, transform: `translateY(${(1 - entra) * 200}px) translateX(${temblor}px)` }}>
        <Telefono>
          <Mapa />
          <div style={{ ...centro, bottom: 30, transform: `translateY(${(1 - oferta) * 700}px)` }}>
            {bueno ? (
              <Oferta app="App de viajes" tarifa="$151.00" tramos={["5 min (1.7 km)", "37 min (13.2 km)"]} />
            ) : (
              <Oferta app="App de viajes" tarifa="$62.00" tramos={["13 min (4.8 km)", "9 min (5.0 km)"]} />
            )}
          </div>
          {frame >= 18 ? (
            <div style={{ ...centro, top: 50, opacity: Math.min(1, pop * 2), transform: `translateY(${(1 - pop) * -300}px) scale(${1.25 - 0.25 * pop})` }}>
              {bueno ? (
                <Popup
                  ancho={640}
                  nivel="verde"
                  app="Viaje"
                  multi="1.4x"
                  neta={121}
                  viaje={151}
                  gas={30}
                  fichas={[
                    { v: "$216", l: "por hora", c: "ok" },
                    { v: "$10.1", l: "por km", c: "ok" },
                    { v: "42 min", l: "14.9 km total", c: "neutro" },
                  ]}
                />
              ) : (
                <Popup
                  ancho={640}
                  nivel="rojo"
                  app="Viaje"
                  neta={42}
                  viaje={62}
                  gas={20}
                  fichas={[
                    { v: "$169", l: "por hora", c: "ok" },
                    { v: "$6.3", l: "por km", c: "mal" },
                    { v: "4.8 km", l: "recogida lejos", c: "mal" },
                  ]}
                />
              )}
            </div>
          ) : null}
        </Telefono>
      </div>
      {bueno ? (
        <Subtitulo texto="Te dice si ^conviene…" />
      ) : (
        <Subtitulo texto="…o si vas a quemar gasolina !gratis" />
      )}
      <Sfx en={18} src={bueno ? "ding.wav" : "error.wav"} vol={0.5} />
      <Voz en={0} n={bueno ? 5 : 6} />
    </AbsoluteFill>
  );
};

// Escena 7: Uber y DiDi al mismo tiempo en la tablet
const Tablet: React.FC = () => {
  const frame = useCurrentFrame();
  const entra = useSpring(0, { damping: 14 });
  const pop1 = useSpring(14, { damping: 10, mass: 0.6 });
  const pop2 = useSpring(22, { damping: 10, mass: 0.6 });
  const mitad = (app: string, tarifa: string, tramos: [string, string]) => (
    <div style={{ flex: 1, position: "relative", overflow: "hidden" }}>
      <Mapa />
      <div style={{ ...centro, bottom: 16 }}>
        <Oferta app={app} tarifa={tarifa} tramos={tramos} ancho={420} />
      </div>
    </div>
  );
  return (
    <AbsoluteFill>
      <Fondo />
      <div style={{ ...centro, top: 640, transform: `scale(${0.8 + 0.2 * entra})`, opacity: entra }}>
        <div
          style={{
            width: 980,
            height: 680,
            borderRadius: 50,
            border: "14px solid #2A302E",
            background: "#000",
            overflow: "hidden",
            display: "flex",
            gap: 6,
            position: "relative",
            boxShadow: "0 40px 100px rgba(0,0,0,0.7)",
          }}
        >
          {mitad("DiDi", "$151.00", ["5 min (1.7 km)", "37 min (13.2 km)"])}
          {mitad("Uber", "$78.00", ["4 min (1.5 km)", "14 min (8.5 km)"])}
          <div style={{ position: "absolute", top: 20, left: 20, transform: `scale(${pop1})`, transformOrigin: "top center" }}>
            <Popup
              ancho={430}
              oro
              nivel="verde"
              app="DiDi"
              neta={121}
              viaje={151}
              gas={30}
              fichas={[
                { v: "$216", l: "por hora", c: "ok" },
                { v: "$10.1", l: "por km", c: "ok" },
                { v: "42 min", l: "14.9 km", c: "neutro" },
              ]}
            />
          </div>
          <div style={{ position: "absolute", top: 28, right: 24, transform: `scale(${pop2})`, transformOrigin: "top center" }}>
            <Popup
              ancho={420}
              apagada
              nivel="amarillo"
              app="Uber"
              neta={58}
              viaje={78}
              gas={20}
              fichas={[
                { v: "$260", l: "por hora", c: "ok" },
                { v: "$7.8", l: "por km", c: "mal" },
                { v: "18 min", l: "10 km", c: "neutro" },
              ]}
            />
          </div>
        </div>
      </div>
      <Subtitulo texto="^Uber y ^DiDi al mismo tiempo" />
      <div
        style={{
          ...centro,
          top: 1400,
          fontFamily: F,
          fontWeight: 700,
          fontSize: 50,
          color: C.oro,
          opacity: aparecer(frame, 34, 8),
        }}
      >
        La mejor opción, marcada en dorado
      </div>
      <Sfx en={14} src="ding.wav" vol={0.4} />
      <Voz en={0} n={7} />
    </AbsoluteFill>
  );
};

// Escena 8: tú pones tu meta
const Meta: React.FC = () => {
  const frame = useCurrentFrame();
  const entra = useSpring(0, { damping: 14 });
  const toque = aparecer(frame, 20, 6);
  const cambio = frame >= 24;
  const ripple = interpolate(frame, [20, 34], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const fila = (titulo: string, valor: string, tocar: boolean, brillo: number) => (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "34px 0", borderTop: `2px solid ${C.borde}` }}>
      <div style={{ fontSize: 46, fontWeight: 600, color: C.texto }}>{titulo}</div>
      <div style={{ display: "flex", alignItems: "center", gap: 26 }}>
        <div style={btn}>−</div>
        <div style={{ fontFamily: FD, fontWeight: 700, fontSize: 64, color: brillo > 0 ? C.verde : C.texto, minWidth: 150, textAlign: "center", transform: `scale(${1 + 0.15 * brillo})` }}>
          {valor}
        </div>
        <div style={{ ...btn, position: "relative", background: tocar && toque > 0 && !cambio ? C.verde : C.tarjeta2 }}>
          +
          {tocar ? (
            <div
              style={{
                position: "absolute",
                inset: -ripple * 40,
                borderRadius: 999,
                border: `4px solid ${C.verde}`,
                opacity: frame >= 20 ? 1 - ripple : 0,
              }}
            />
          ) : null}
        </div>
      </div>
    </div>
  );
  const brillo = interpolate(frame, [24, 30, 50], [0, 1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  return (
    <AbsoluteFill>
      <Fondo />
      <div style={{ ...centro, top: 640, opacity: entra, transform: `translateY(${(1 - entra) * 120}px)` }}>
        <div style={{ width: 900, background: C.tarjeta, borderRadius: 48, padding: "44px 56px 20px", fontFamily: F, boxShadow: "0 40px 100px rgba(0,0,0,0.6)" }}>
          <div style={{ fontSize: 40, fontWeight: 700, color: C.tenue, letterSpacing: 2, marginBottom: 20 }}>FILTROS INTELIGENTES</div>
          {fila("Por km", cambio ? "$9" : "$8", true, brillo)}
          {fila("Por hora", cambio ? "$180" : "$150", false, brillo)}
        </div>
      </div>
      <Subtitulo texto="Tú pones tu ^meta" />
      <Sfx en={20} src="ding.wav" vol={0.3} />
      <Voz en={0} n={8} />
    </AbsoluteFill>
  );
};

const btn: React.CSSProperties = {
  width: 84,
  height: 84,
  borderRadius: 42,
  background: C.tarjeta2,
  color: C.texto,
  fontSize: 56,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

// Escena 9: llamado a la acción
const Cierre: React.FC = () => {
  const frame = useCurrentFrame();
  const logo = useSpring(0, { damping: 11 });
  const l1 = useSpring(18, { damping: 10 });
  const l2 = useSpring(80, { damping: 10 });
  const l3 = useSpring(140, { damping: 10 });
  const pulso = 1 + 0.05 * Math.sin(Math.max(0, frame - 150) / 4);
  const pal = (s: number): React.CSSProperties => ({ opacity: s, transform: `scale(${0.6 + 0.4 * s})` });
  return (
    <AbsoluteFill>
      <Fondo tono="rgba(34,197,94,0.22)" />
      <AbsoluteFill style={{ alignItems: "center", justifyContent: "center", textAlign: "center", fontFamily: FD }}>
        <div style={{ transform: `scale(${logo})`, marginBottom: 90 }}>
          <Logo tam={150} brillo={logo} />
        </div>
        <div style={{ ...pal(l1), fontWeight: 800, fontSize: 70, color: C.texto }}>30 lugares de</div>
        <div style={{ ...pal(l1), fontWeight: 900, fontSize: 150, color: C.oro, letterSpacing: 2, lineHeight: 1.05 }}>FUNDADOR</div>
        <div style={{ ...pal(l2), fontWeight: 900, fontSize: 96, color: C.verde, marginTop: 20 }}>GRATIS de por vida</div>
        <div
          style={{
            opacity: l3,
            transform: `scale(${(0.6 + 0.4 * l3) * pulso})`,
            marginTop: 80,
            background: C.verde,
            color: "#08130C",
            fontWeight: 900,
            fontSize: 84,
            padding: "30px 70px",
            borderRadius: 999,
            boxShadow: "0 0 80px rgba(34,197,94,0.5)",
          }}
        >
          Comenta “YO”
        </div>
      </AbsoluteFill>
      <Sfx en={18} src="golpe.wav" vol={0.4} />
      <Sfx en={80} src="ding.wav" vol={0.4} />
      <Sfx en={140} src="ding.wav" vol={0.5} />
      <Voz en={0} n={9} />
    </AbsoluteFill>
  );
};

export const VideoChecas: React.FC = () => (
  <AbsoluteFill style={{ background: C.fondo }}>
    <Audio src={staticFile("beat.m4a")} volume={0.18} />
    <Sequence durationInFrames={222}>
      <Gancho />
    </Sequence>
    <Sequence from={222} durationInFrames={140}>
      <PorQue />
    </Sequence>
    <Sequence from={362} durationInFrames={60}>
      <Ejemplo bueno />
    </Sequence>
    <Sequence from={422} durationInFrames={80}>
      <Ejemplo bueno={false} />
    </Sequence>
    <Sequence from={502} durationInFrames={80}>
      <Tablet />
    </Sequence>
    <Sequence from={582} durationInFrames={60}>
      <Meta />
    </Sequence>
    <Sequence from={642} durationInFrames={190}>
      <Cierre />
    </Sequence>
  </AbsoluteFill>
);
