import React from "react";
import { AbsoluteFill, Audio, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { C, F, FD, Fondo, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { LINEAS, Linea } from "./lineasRecogida";

// Explicativo «La recogida que nadie te paga» · narrador, sin mascota · ~30 s a 30 fps
const FPS = 30;
const M = { verde: "#22C55E", amarillo: "#FACC15", rojo: "#F43F5E", texto: "#F2F5F3", tenue: "#8C9792" };

type Escena = { desde: number; hasta: number; voz: number };
const ESC: Escena[] = [];
{
  let f = 0;
  LINEAS.forEach((l, i) => {
    const pausa = i === 0 ? 0 : 8;
    const fin = f + pausa + Math.round(l.dur * FPS) + (i === LINEAS.length - 1 ? 45 : 14);
    ESC.push({ desde: f, hasta: fin, voz: f + pausa });
    f = fin;
  });
}
export const DURACION_RECOGIDA = ESC[ESC.length - 1].hasta;

// Cuadro (absoluto) en que empieza una palabra de la línea i
const en = (i: number, palabra: string) => ESC[i].voz + Math.round((LINEAS[i].palabras.find((p) => p.w === palabra)?.s ?? 0) * FPS);

const centro: React.CSSProperties = { position: "absolute", left: 0, right: 0, display: "flex", justifyContent: "center" };
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

const useGolpe = (desde: number) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return spring({ frame: frame - desde, fps, config: { damping: 10, mass: 0.6 } });
};

// Subtítulos tipo karaoke, grupos de hasta 4 palabras
const Subs: React.FC<{ linea: Linea; t: number; color: string; top: number }> = ({ linea, t, color, top }) => {
  const grupos: { i: number; f: number }[] = [];
  let ini = 0;
  linea.palabras.forEach((p, i) => {
    if (/[.!?…,]$/.test(p.w) || i - ini === 3 || i === linea.palabras.length - 1) {
      grupos.push({ i: ini, f: i });
      ini = i + 1;
    }
  });
  if (t > linea.dur + 0.3) return null;
  const g = [...grupos].reverse().find((x) => t >= linea.palabras[x.i].s - 0.05) ?? grupos[0];
  return (
    <div
      style={{
        position: "absolute",
        top,
        left: 70,
        right: 70,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "0 20px",
        fontFamily: FD,
        fontWeight: 900,
        fontSize: 72,
        lineHeight: 1.1,
        textAlign: "center",
        textShadow: "0 6px 24px rgba(0,0,0,0.85)",
      }}
    >
      {linea.palabras.slice(g.i, g.f + 1).map((p, k) => (
        <span key={k} style={{ color: t >= p.s - 0.05 ? color : "rgba(242,245,243,0.35)" }}>
          {p.w}
        </span>
      ))}
    </div>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol?: number }> = ({ en: e, src, vol = 0.5 }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

// Texto grande que entra con golpe
const Grande: React.FC<{ desde: number; children: React.ReactNode; tam?: number; color?: string; rot?: number }> = ({
  desde,
  children,
  tam = 130,
  color = M.texto,
  rot = 0,
}) => {
  const frame = useCurrentFrame();
  const g = useGolpe(desde);
  if (frame < desde) return null;
  return (
    <div
      style={{
        fontFamily: FD,
        fontWeight: 900,
        fontSize: tam,
        color,
        lineHeight: 1,
        letterSpacing: -2,
        textAlign: "center",
        transform: `scale(${1.6 - 0.6 * g}) rotate(${rot}deg)`,
        opacity: Math.min(1, g * 1.5),
      }}
    >
      {children}
    </div>
  );
};

// 1 · Gancho: la app dice 8 km, vas a manejar 14
const Gancho: React.FC = () => {
  const frame = useCurrentFrame();
  const catorce = en(0, "catorce.");
  const tacha = interpolate(frame, [catorce - 4, catorce + 4], [0, 1], clamp);
  return (
    <>
      <div style={{ ...centro, top: 120, flexDirection: "column", alignItems: "center", gap: 10 }}>
        <div style={{ fontFamily: F, fontWeight: 700, fontSize: 52, color: M.tenue }}>La app dice</div>
        <div style={{ position: "relative" }}>
          <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 210, color: tacha > 0 ? M.tenue : M.texto, lineHeight: 1 }}>8 km</div>
          <div
            style={{
              position: "absolute",
              left: -10,
              top: "50%",
              height: 18,
              width: `${tacha * 106}%`,
              background: M.rojo,
              borderRadius: 9,
              transform: "rotate(-8deg)",
            }}
          />
        </div>
      </div>
      <div style={{ ...centro, top: 430 }}>
        <Grande desde={catorce} tam={120} color={M.rojo} rot={-3}>
          Vas a manejar
          <br />
          14 km
        </Grande>
      </div>
      <div style={{ ...centro, top: 760, transform: "scale(0.95)" }}>
        <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={720} />
      </div>
    </>
  );
};

// 2 · Mapa: recogida (sin pago) y viaje (pagado)
const MapaRuta: React.FC = () => {
  const frame = useCurrentFrame();
  const rojo = interpolate(frame, [en(1, "manejas"), en(1, "él.")], [0, 1], clamp);
  const verde = interpolate(frame, [en(1, "Y"), en(1, "Y") + 25], [0, 1], clamp);
  const etiqueta = useGolpe(en(1, "nadie"));
  const lenR = 760;
  const lenV = 980;
  return (
    <>
      <div style={{ ...centro, top: 110 }}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 84, color: M.texto, textAlign: "center", lineHeight: 1.05 }}>
          Lo que de verdad
          <br />
          manejas
        </div>
      </div>
      <svg width={1080} height={900} viewBox="0 0 1080 900" style={{ position: "absolute", top: 360, left: 0 }}>
        {[180, 420, 660, 900].map((x) => (
          <rect key={x} x={x} y={0} width={30} height={900} fill="#1C2321" />
        ))}
        {[150, 400, 650].map((y) => (
          <rect key={y} x={0} y={y} width={1080} height={26} fill="#1C2321" />
        ))}
        {/* Recogida */}
        <path
          d="M180 760 C 180 560, 420 640, 435 413"
          stroke={M.rojo}
          strokeWidth={22}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={`${lenR}`}
          strokeDashoffset={lenR * (1 - rojo)}
        />
        {/* Viaje */}
        <path
          d="M435 413 C 470 250, 760 300, 915 120"
          stroke={M.verde}
          strokeWidth={22}
          fill="none"
          strokeLinecap="round"
          strokeDasharray={`${lenV}`}
          strokeDashoffset={lenV * (1 - verde)}
        />
        <circle cx={180} cy={760} r={34} fill={M.texto} />
        <text x={180} y={772} textAnchor="middle" fontSize={34} fontFamily={F} fontWeight={800} fill="#000">
          Tú
        </text>
        <circle cx={435} cy={413} r={28} fill={M.amarillo} stroke="#000" strokeWidth={6} />
        <circle cx={915} cy={120} r={28} fill={M.verde} stroke="#000" strokeWidth={6} />
      </svg>
      <div style={{ position: "absolute", top: 880, left: 500, opacity: rojo > 0.9 ? 1 : 0 }}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 64, color: M.rojo }}>6 km</div>
        <div
          style={{
            fontFamily: F,
            fontWeight: 800,
            fontSize: 40,
            color: "#000",
            background: M.rojo,
            borderRadius: 12,
            padding: "6px 16px",
            marginTop: 6,
            transform: `scale(${etiqueta})`,
            transformOrigin: "left center",
          }}
        >
          NO TE LO PAGAN
        </div>
      </div>
      <div style={{ position: "absolute", top: 370, left: 520, opacity: verde > 0.6 ? 1 : 0 }}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 64, color: M.verde }}>8 km</div>
        <div style={{ fontFamily: F, fontWeight: 700, fontSize: 38, color: M.tenue }}>el viaje</div>
      </div>
    </>
  );
};

// 3 · La cuenta del $/km
const PorKm: React.FC = () => {
  const frame = useCurrentFrame();
  const real = en(2, "En");
  const tacha = interpolate(frame, [real - 3, real + 6], [0, 1], clamp);
  return (
    <>
      <div style={{ ...centro, top: 150 }}>
        <div style={{ fontFamily: F, fontWeight: 700, fontSize: 50, color: M.tenue }}>Lo que parece</div>
      </div>
      <div style={{ ...centro, top: 230 }}>
        <div style={{ position: "relative" }}>
          <Grande desde={ESC[2].voz} tam={110} color={tacha > 0 ? M.tenue : M.texto}>
            $80 ÷ 8 km = $10/km
          </Grande>
          <div
            style={{
              position: "absolute",
              left: -10,
              top: "50%",
              height: 14,
              width: `${tacha * 104}%`,
              background: M.rojo,
              borderRadius: 7,
              transform: "rotate(-4deg)",
            }}
          />
        </div>
      </div>
      {frame >= real ? (
        <>
          <div style={{ ...centro, top: 470 }}>
            <div style={{ fontFamily: F, fontWeight: 700, fontSize: 50, color: M.tenue }}>Lo real</div>
          </div>
          <div style={{ ...centro, top: 550 }}>
            <Grande desde={real} tam={110}>
              $80 ÷ 14 km =
            </Grande>
          </div>
          <div style={{ ...centro, top: 700 }}>
            <Grande desde={en(2, "son")} tam={260} color={M.rojo} rot={-3}>
              $5.70
            </Grande>
          </div>
          <div style={{ ...centro, top: 960 }}>
            <Grande desde={en(2, "son") + 4} tam={70} color={M.rojo}>
              por km
            </Grande>
          </div>
        </>
      ) : null}
    </>
  );
};

// 4 · Gasolina y lo que te queda
const Gasolina: React.FC = () => {
  const frame = useCurrentFrame();
  const quedan = en(3, "Te");
  const filas: [string, string, string, number][] = [
    ["Tarifa", "$80", M.texto, ESC[3].voz],
    ["Gasolina · 14 km × $2", "−$28", M.rojo, en(3, "veintiocho")],
  ];
  return (
    <>
      <div style={{ ...centro, top: 130 }}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 84, color: M.texto }}>Y falta la gasolina</div>
      </div>
      <div style={{ position: "absolute", top: 330, left: 90, right: 90, display: "flex", flexDirection: "column", gap: 26 }}>
        {filas.map(([l, v, c, d], i) =>
          frame >= d ? (
            <div
              key={i}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "baseline",
                borderBottom: "3px solid #232A27",
                paddingBottom: 18,
                opacity: interpolate(frame, [d, d + 8], [0, 1], clamp),
              }}
            >
              <span style={{ fontFamily: F, fontWeight: 700, fontSize: 50, color: M.tenue }}>{l}</span>
              <span style={{ fontFamily: FD, fontWeight: 900, fontSize: 96, color: c }}>{v}</span>
            </div>
          ) : null,
        )}
      </div>
      <div style={{ ...centro, top: 690 }}>
        <div style={{ fontFamily: F, fontWeight: 600, fontSize: 36, color: M.tenue, opacity: frame >= en(3, "veintiocho") ? 1 : 0 }}>
          $24 el litro ÷ 12 km por litro = $2 por km
        </div>
      </div>
      {frame >= quedan ? (
        <>
          <div style={{ ...centro, top: 820 }}>
            <div style={{ fontFamily: F, fontWeight: 700, fontSize: 56, color: M.tenue }}>Te quedan</div>
          </div>
          <div style={{ ...centro, top: 900 }}>
            <Grande desde={en(3, "cincuenta")} tam={300} color={M.amarillo}>
              $52
            </Grande>
          </div>
        </>
      ) : null}
    </>
  );
};

// 5 · Cierre con el pop-up de Checa
const Cierre: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const tel = spring({ frame: frame - ESC[4].desde, fps, config: { damping: 14, mass: 0.7 } });
  const pop = spring({ frame: frame - (ESC[4].voz + 10), fps, config: { damping: 11, mass: 0.6 } });
  const cta = en(4, "Pruébala");
  return (
    <>
      <div style={{ ...centro, top: 90, transform: `translateY(${(1 - tel) * 1400}px) scale(0.92)` }}>
        <Telefono ancho={700} alto={1180}>
          <Mapa />
          <div style={{ ...centro, bottom: 20 }}>
            <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={620} />
          </div>
          {frame >= ESC[4].voz + 10 ? (
            <div style={{ ...centro, top: 60, opacity: Math.min(1, pop * 2), transform: `translateY(${(1 - pop) * -200}px)` }}>
              <Popup
                ancho={640}
                nivel="rojo"
                app="Viaje"
                neta={52}
                viaje={80}
                gas={28}
                fichas={[
                  { v: "$141", l: "por hora", c: "mal" },
                  { v: "$5.7", l: "por km", c: "mal" },
                  { v: "34 min", l: "14 km total", c: "neutro" },
                ]}
              />
            </div>
          ) : null}
        </Telefono>
      </div>
      {frame >= cta ? (
        <div style={{ ...centro, top: 1330, flexDirection: "column", alignItems: "center", gap: 18 }}>
          <Logo tam={90} />
          <div style={{ fontFamily: F, fontWeight: 800, fontSize: 46, color: "#08130C", background: M.verde, borderRadius: 999, padding: "16px 40px" }}>
            7 días gratis · link en el perfil
          </div>
        </div>
      ) : null}
    </>
  );
};

const ESCENAS = [Gancho, MapaRuta, PorKm, Gasolina, Cierre];
const SUBS_TOP = [1320, 1320, 1320, 1320, 1170];
const TONO = ["rgba(244,63,94,0.16)", "rgba(244,63,94,0.12)", "rgba(244,63,94,0.12)", "rgba(250,204,21,0.12)", "rgba(34,197,94,0.16)"];

export const Recogida: React.FC = () => {
  const frame = useCurrentFrame();
  const k = Math.max(0, ESC.findIndex((e) => frame >= e.desde && frame < e.hasta));
  const t = (frame - ESC[k].voz) / FPS;
  const Esc = ESCENAS[k];
  return (
    <AbsoluteFill style={{ background: C.fondo, fontFamily: F, overflow: "hidden" }}>
      <Fondo tono={TONO[k]} />
      <Audio src={staticFile("beat.m4a")} volume={0.12} />
      {ESC.map((e, i) => (
        <Sequence key={i} from={e.voz}>
          <Audio src={staticFile(`recogida/${LINEAS[i].archivo}.mp3`)} />
        </Sequence>
      ))}
      <Sfx en={en(0, "catorce.")} src="golpe.wav" />
      <Sfx en={ESC[1].desde} src="whoosh.wav" />
      <Sfx en={en(1, "nadie")} src="error.wav" vol={0.4} />
      <Sfx en={ESC[2].desde} src="whoosh.wav" />
      <Sfx en={en(2, "son")} src="golpe.wav" />
      <Sfx en={ESC[3].desde} src="whoosh.wav" />
      <Sfx en={en(3, "cincuenta")} src="golpe.wav" />
      <Sfx en={ESC[4].voz + 10} src="error.wav" vol={0.4} />
      <Sfx en={en(4, "Pruébala")} src="ding.wav" vol={0.4} />
      <Esc />
      {k < 4 ? <Subs linea={LINEAS[k]} t={t} color={k === 3 ? M.amarillo : M.texto} top={SUBS_TOP[k]} /> : null}
    </AbsoluteFill>
  );
};
