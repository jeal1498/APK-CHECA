import React from "react";
import { AbsoluteFill, Audio, Sequence, interpolate, spring, staticFile, useCurrentFrame, useVideoConfig } from "remotion";
import { F, FD, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Asfalto, Billetes, Bomba, Carro, K, Letrero, SemaforoLuz, Ticket } from "./graficos";
import { LINEAS, Linea } from "./lineasRecogida2";

// «Los 6 kilómetros que nadie te paga» v2 · narrador, gráficos de calle, ritmo alto · ~38 s a 30 fps
const FPS = 30;
export const MUSICA = "recogida2/musica.mp3";

type Escena = { desde: number; hasta: number; voz: number };
const ESC: Escena[] = [];
{
  let f = 0;
  LINEAS.forEach((l, i) => {
    const pausa = i === 0 ? 0 : 5;
    const fin = f + pausa + Math.round(l.dur * FPS) + (i === LINEAS.length - 1 ? 50 : 8);
    ESC.push({ desde: f, hasta: fin, voz: f + pausa });
    f = fin;
  });
}
export const DURACION_RECOGIDA2 = ESC[ESC.length - 1].hasta;

const en = (i: number, palabra: string) => {
  const p = LINEAS[i].palabras.find((x) => x.w === palabra);
  if (!p) throw new Error(`No existe la palabra «${palabra}» en la línea ${i + 1}`);
  return ESC[i].voz + Math.round(p.s * FPS);
};
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const centro: React.CSSProperties = { position: "absolute", left: 0, right: 0, display: "flex", justifyContent: "center" };
const impulso = (frame: number, g: number, caida = 5) => (frame >= g ? Math.exp(-(frame - g) / caida) : 0);

const useResorte = (desde: number, damping = 10) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return spring({ frame: frame - desde, fps, config: { damping, mass: 0.6 } });
};

// Entra con golpe (de grande a su tamaño)
const Golpe: React.FC<{ desde: number; children: React.ReactNode; rot?: number; top: number }> = ({ desde, children, rot = 0, top }) => {
  const frame = useCurrentFrame();
  const g = useResorte(desde);
  if (frame < desde) return null;
  return (
    <div style={{ ...centro, top, transform: `scale(${1.7 - 0.7 * g}) rotate(${rot}deg)`, opacity: Math.min(1, g * 1.6) }}>{children}</div>
  );
};

// Cae desde arriba
const Cae: React.FC<{ desde: number; children: React.ReactNode; top: number }> = ({ desde, children, top }) => {
  const frame = useCurrentFrame();
  const g = useResorte(desde, 12);
  if (frame < desde) return null;
  return <div style={{ ...centro, top, transform: `translateY(${(1 - g) * -900}px) rotate(${(1 - g) * -8}deg)` }}>{children}</div>;
};

const Titulo: React.FC<{ children: React.ReactNode; tam?: number; color?: string }> = ({ children, tam = 140, color = K.blanco }) => (
  <div style={{ fontFamily: FD, fontWeight: 900, fontSize: tam, color, lineHeight: 1, letterSpacing: -3, textAlign: "center", textShadow: "0 10px 40px rgba(0,0,0,0.6)" }}>
    {children}
  </div>
);

const Pill: React.FC<{ children: React.ReactNode; fondo?: string; color?: string; tam?: number }> = ({ children, fondo = K.amarillo, color = "#111", tam = 46 }) => (
  <div style={{ fontFamily: F, fontWeight: 800, fontSize: tam, color, background: fondo, borderRadius: 999, padding: "14px 34px" }}>{children}</div>
);

// Subtítulos tipo karaoke
const Subs: React.FC<{ linea: Linea; t: number; color: string }> = ({ linea, t, color }) => {
  const grupos: { i: number; f: number }[] = [];
  let ini = 0;
  linea.palabras.forEach((p, i) => {
    if (/[.!?…,:]$/.test(p.w) || i - ini === 3 || i === linea.palabras.length - 1) {
      grupos.push({ i: ini, f: i });
      ini = i + 1;
    }
  });
  if (t > linea.dur + 0.25 || t < -0.1) return null;
  const g = [...grupos].reverse().find((x) => t >= linea.palabras[x.i].s - 0.05) ?? grupos[0];
  return (
    <div
      style={{
        position: "absolute",
        top: 1340,
        left: 70,
        right: 70,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "0 26px",
        fontFamily: FD,
        fontWeight: 900,
        fontSize: 70,
        lineHeight: 1.1,
        textAlign: "center",
        textShadow: "0 4px 0 #000, 0 8px 24px rgba(0,0,0,0.9)",
      }}
    >
      {linea.palabras.slice(g.i, g.f + 1).map((p, k) => {
        const on = t >= p.s - 0.05;
        return (
          <span key={k} style={{ color: on ? color : "rgba(244,246,245,0.4)", transform: `scale(${on && t < p.e ? 1.04 : 1})`, display: "inline-block" }}>
            {p.w}
          </span>
        );
      })}
    </div>
  );
};

// 1 · Gancho
const S1: React.FC = () => {
  const frame = useCurrentFrame();
  const vibra = Math.sin(frame * 3.1) * 6 * (impulso(frame, 0, 10) + impulso(frame, en(0, "lana"), 8));
  return (
    <>
      <Golpe desde={0} top={110}>
        <Titulo tam={170}>12 HORAS</Titulo>
      </Golpe>
      <Golpe desde={en(0, "lana")} top={300} rot={-3}>
        <Titulo tam={130} color={K.amarillo}>
          ¿Y LA LANA?
        </Titulo>
      </Golpe>
      <div style={{ ...centro, top: 470, transform: `rotate(${vibra}deg)` }}>
        <Telefono ancho={500} alto={680}>
          <Mapa />
          <div style={{ ...centro, bottom: 14 }}>
            <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={450} />
          </div>
        </Telefono>
      </div>
      <Golpe desde={en(0, "Al")} top={1190}>
        <Pill>Al final: cuánto pierdes al mes 👀</Pill>
      </Golpe>
    </>
  );
};

// 2 · Kilómetros gratis: el carro avanza, el odómetro corre
const S2: React.FC = () => {
  const frame = useCurrentFrame();
  const local = frame - ESC[1].desde;
  const entra = useResorte(ESC[1].desde, 14);
  const km = Math.floor(local * 1.7);
  const dash = (frame * 30) % 160;
  return (
    <>
      <Cae desde={en(1, "Son")} top={120}>
        <Letrero arriba="ATENCIÓN" grande="KM GRATIS" fondo={K.rojo} />
      </Cae>
      {/* carretera */}
      <div style={{ position: "absolute", left: 0, right: 0, top: 820, height: 120, background: K.asfalto2, overflow: "hidden" }}>
        <div style={{ position: "absolute", top: 54, left: -dash, display: "flex", gap: 80 }}>
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} style={{ width: 80, height: 12, background: K.linea, borderRadius: 6 }} />
          ))}
        </div>
      </div>
      <div style={{ position: "absolute", top: 640, left: 280 + (1 - entra) * -900 + Math.sin(frame / 6) * 6 }}>
        <Carro ancho={520} avance={frame * 8} />
      </div>
      <div style={{ ...centro, top: 990 }}>
        <div style={{ display: "flex", gap: 8, alignItems: "center", background: "#05080A", border: "4px solid #2C353E", borderRadius: 18, padding: "10px 22px" }}>
          {String(km).padStart(4, "0")
            .split("")
            .map((d, i) => (
              <div key={i} style={{ fontFamily: FD, fontWeight: 900, fontSize: 80, color: K.blanco, background: "#1A2229", borderRadius: 8, width: 66, textAlign: "center" }}>
                {d}
              </div>
            ))}
          <div style={{ fontFamily: F, fontWeight: 800, fontSize: 44, color: K.tenue, marginLeft: 10 }}>km</div>
        </div>
      </div>
      <Golpe desde={en(1, "nadie")} top={1150} rot={-6}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 90, color: K.rojo, border: `10px solid ${K.rojo}`, borderRadius: 20, padding: "0 30px" }}>
          $0 PAGADO
        </div>
      </Golpe>
    </>
  );
};

// 3 · Los letreros: viaje 8, recogida 6, manejas 14
const S3: React.FC = () => (
  <>
    <Cae desde={ESC[2].desde + 2} top={90}>
      <Letrero arriba="VIAJE · $80" grande="8 km" ancho={700} />
    </Cae>
    <Cae desde={en(2, "seis")} top={460}>
      <Letrero arriba="RECOGIDA · SIN PAGO" grande="6 km" fondo={K.rojo} ancho={700} />
    </Cae>
    <Golpe desde={en(2, "Esos")} top={850} rot={-2}>
      <Letrero arriba="TÚ MANEJAS" grande="14 km" fondo={K.amarillo} texto="#111" ancho={780} />
    </Golpe>
  </>
);

// 4 · Ticket y gasolina
const S4: React.FC = () => {
  const frame = useCurrentFrame();
  const q = en(3, "quedan");
  const queda = Math.round(interpolate(frame, [q, en(3, "dos.") + 4], [80, 52], clamp));
  const gas = interpolate(frame, [en(3, "son"), en(3, "gasolina.")], [0, 28], clamp);
  return (
    <>
      <div style={{ ...centro, top: 90 }}>
        <Ticket
          ancho={820}
          lineas={[
            { l: "Tarifa", v: "$80", desde: ESC[3].desde + 4 },
            { l: "Gasolina 14 km", v: "−$28", c: K.rojo, desde: en(3, "veintiocho") },
            { l: "TE QUEDAN", v: `$${queda}`, c: "#B45309", grande: true, desde: q },
          ]}
        />
      </div>
      <div style={{ position: "absolute", top: 760, left: 110 }}>
        <Bomba pesos={gas} litros={gas / 24} tam={300} />
      </div>
      <div style={{ position: "absolute", top: 860, left: 450, right: 50, fontFamily: F, color: K.blanco }}>
        <div style={{ fontWeight: 700, fontSize: 34, color: K.tenue }}>Litro a $24 · 12 km por litro</div>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 86, marginTop: 8 }}>
          = <span style={{ color: K.amarillo }}>$2</span> por km
        </div>
      </div>
      <Billetes desde={q} />
    </>
  );
};

// 5 · Lo que pierdes al mes
const S5: React.FC = () => {
  const frame = useCurrentFrame();
  const fase2 = en(4, "En");
  const mes = Math.round(interpolate(frame, [en(4, "mes"), en(4, "que")], [60, 1560], clamp));
  if (frame < fase2) {
    const pins = Array.from({ length: 5 }).map((_, i) => ESC[4].voz + 4 + i * 8);
    return (
      <>
        <div style={{ ...centro, top: 110, gap: 26 }}>
          {pins.map((d, i) => {
            const g = interpolate(frame, [d, d + 8], [0, 1], clamp);
            return (
              <svg key={i} width={120} height={150} viewBox="0 0 40 50" style={{ transform: `scale(${g}) translateY(${(1 - g) * -40}px)` }}>
                <path d="M20 48 C 8 30, 2 24, 2 16 A 18 18 0 0 1 38 16 C 38 24, 32 30, 20 48 Z" fill={K.rojo} />
                <circle cx={20} cy={16} r={7} fill="#fff" />
              </svg>
            );
          })}
        </div>
        <div style={{ ...centro, top: 280 }}>
          <div style={{ fontFamily: F, fontWeight: 800, fontSize: 48, color: K.tenue }}>5 recogidas al día</div>
        </div>
        <Cae desde={en(4, "treinta")} top={380}>
          <Letrero arriba="REGALADOS AL DÍA" grande="30 km" fondo={K.rojo} ancho={700} />
        </Cae>
        <Golpe desde={en(4, "Sesenta")} top={780}>
          <div style={{ display: "flex", alignItems: "center", gap: 30 }}>
            <Bomba pesos={60} litros={2.5} tam={170} />
            <Titulo tam={130} color={K.amarillo}>
              $60
              <div style={{ fontSize: 56, color: K.blanco, letterSpacing: 0 }}>de gasolina al día</div>
            </Titulo>
          </div>
        </Golpe>
      </>
    );
  }
  const dias = Math.min(26, Math.max(0, Math.floor((frame - fase2) / 2)));
  return (
    <>
      <Golpe desde={fase2} top={120}>
        <div style={{ fontFamily: F, fontWeight: 800, fontSize: 56, color: K.tenue, letterSpacing: 4 }}>EN UN MES DE TRABAJO</div>
      </Golpe>
      <div style={{ position: "absolute", top: 230, left: 140, right: 140, display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: 12 }}>
        {Array.from({ length: 28 }).map((_, i) => (
          <div
            key={i}
            style={{
              height: 62,
              borderRadius: 10,
              background: i < dias ? K.rojo : "#1E2730",
              color: i < dias ? "#fff" : K.tenue,
              fontFamily: F,
              fontWeight: 800,
              fontSize: 26,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              opacity: (i + 1) % 7 === 0 ? 0.35 : 1,
            }}
          >
            {i + 1}
          </div>
        ))}
      </div>
      {frame >= en(4, "mes") ? (
        <Golpe desde={en(4, "mes")} top={620} rot={-2}>
          <Titulo tam={230} color={K.rojo}>
            −${mes.toLocaleString("en-US")}
          </Titulo>
        </Golpe>
      ) : null}
      <Golpe desde={en(4, "salen")} top={890}>
        <Pill fondo={K.rojo} color="#fff" tam={52}>
          salen de TU bolsa
        </Pill>
      </Golpe>
      <Billetes desde={en(4, "quinientos")} n={10} />
    </>
  );
};

// 6 · Checa: semáforo y pop-up
const S6: React.FC = () => {
  const frame = useCurrentFrame();
  const cambia = en(5, "aceptes:");
  const lado = useResorte(en(5, "cuánto"), 14);
  const pop = useResorte(en(5, "cuánto") + 4, 11);
  return (
    <>
      <div style={{ position: "absolute", top: 160 + lado * 120, left: 540 - 117 - lado * 330, transform: `scale(${1 - lado * 0.35})`, transformOrigin: "top left" }}>
        <SemaforoLuz activo={frame >= cambia ? "rojo" : "amarillo"} alto={520} />
      </div>
      {frame >= en(5, "cuánto") ? (
        <div style={{ position: "absolute", top: 120, left: 360, transform: `translateX(${(1 - lado) * 900}px)` }}>
          <Telefono ancho={620} alto={1080}>
            <Mapa />
            <div style={{ ...centro, bottom: 14 }}>
              <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={560} />
            </div>
            <div style={{ ...centro, top: 50, opacity: Math.min(1, pop * 2), transform: `translateY(${(1 - pop) * -200}px)` }}>
              <Popup
                ancho={570}
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
          </Telefono>
        </div>
      ) : (
        <Golpe desde={ESC[5].desde + 2} top={820}>
          <Titulo tam={110}>
            Antes de
            <br />
            <span style={{ color: K.verde }}>aceptar</span>
          </Titulo>
        </Golpe>
      )}
    </>
  );
};

// 7 · Cierre y pregunta
const S7: React.FC = () => {
  const frame = useCurrentFrame();
  const sale = interpolate(frame, [ESC[6].voz + 10, ESC[6].hasta], [-600, 1300]);
  return (
    <>
      <Golpe desde={ESC[6].desde} top={140}>
        <Logo tam={130} />
      </Golpe>
      <Golpe desde={en(6, "siete")} top={360}>
        <Pill fondo={K.verde} tam={54}>
          7 días gratis · link en el perfil
        </Pill>
      </Golpe>
      <div style={{ position: "absolute", top: 560, left: sale }}>
        <Carro ancho={440} avance={frame * 10} humo={(frame % 20) / 20} />
      </div>
      <Golpe desde={en(6, "¿cuál")} top={850} rot={-2}>
        <Titulo tam={100} color={K.amarillo}>
          ¿Tu recogida
          <br />
          más lejana? 👇
        </Titulo>
      </Golpe>
    </>
  );
};

const ESCENAS = [S1, S2, S3, S4, S5, S6, S7];

const Sfx: React.FC<{ en: number; src: string; vol?: number }> = ({ en: e, src, vol = 0.5 }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

const GOLPES: { f: number; color: string; fuerte?: boolean; sfx?: string }[] = [
  { f: 0, color: K.blanco, fuerte: true, sfx: "golpe.wav" },
  { f: en(0, "lana"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: en(0, "Al"), color: K.amarillo, sfx: "recogida2/pop.mp3" },
  { f: ESC[1].desde, color: K.blanco, sfx: "recogida2/motor.mp3" },
  { f: en(1, "Son"), color: K.rojo, sfx: "whoosh.wav" },
  { f: en(1, "nadie"), color: K.rojo, fuerte: true, sfx: "recogida2/frenado.mp3" },
  { f: ESC[2].desde + 2, color: K.verde, sfx: "whoosh.wav" },
  { f: en(2, "seis"), color: K.rojo, sfx: "whoosh.wav" },
  { f: en(2, "Esos"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: ESC[3].desde, color: K.blanco, sfx: "whoosh.wav" },
  { f: en(3, "veintiocho"), color: K.rojo, sfx: "error.wav" },
  { f: en(3, "quedan"), color: K.amarillo, fuerte: true, sfx: "recogida2/caja.mp3" },
  { f: ESC[4].desde, color: K.rojo, sfx: "whoosh.wav" },
  { f: en(4, "treinta"), color: K.rojo, sfx: "recogida2/pop.mp3" },
  { f: en(4, "Sesenta"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: en(4, "En"), color: K.blanco, sfx: "whoosh.wav" },
  { f: en(4, "mes"), color: K.rojo, fuerte: true, sfx: "golpe.wav" },
  { f: en(4, "salen"), color: K.rojo, sfx: "recogida2/caja.mp3" },
  { f: ESC[5].desde, color: K.amarillo, sfx: "whoosh.wav" },
  { f: en(5, "aceptes:"), color: K.rojo, fuerte: true, sfx: "golpe.wav" },
  { f: en(5, "cuánto"), color: K.blanco, sfx: "whoosh.wav" },
  { f: ESC[6].desde, color: K.verde, fuerte: true, sfx: "ding.wav" },
  { f: ESC[6].voz + 10, color: K.verde, sfx: "recogida2/motor.mp3" },
  { f: en(6, "¿cuál"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
];

const SUB_COLOR = [K.amarillo, K.blanco, K.blanco, K.amarillo, K.blanco, K.verde, K.amarillo];
const TONO = [
  "rgba(250,204,21,0.14)",
  "rgba(244,63,94,0.12)",
  "rgba(255,181,71,0.12)",
  "rgba(250,204,21,0.12)",
  "rgba(244,63,94,0.16)",
  "rgba(244,63,94,0.12)",
  "rgba(34,197,94,0.16)",
];

export const Recogida2: React.FC<{ musica?: string }> = ({ musica = MUSICA }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const k = Math.max(0, ESC.findIndex((e) => frame >= e.desde && frame < e.hasta));
  const t = (frame - ESC[k].voz) / FPS;
  const Esc = ESCENAS[k];
  let zoom = 1;
  let sx = 0;
  let sy = 0;
  let flash = 0;
  let flashColor = K.blanco;
  for (const g of GOLPES) {
    const i = impulso(frame, g.f, 5);
    if (i <= 0.01) continue;
    zoom += (g.fuerte ? 0.1 : 0.05) * i;
    if (g.fuerte) {
      sx += Math.sin(frame * 2.3) * 20 * i;
      sy += Math.cos(frame * 2.9) * 14 * i;
    }
    const fl = i * (g.fuerte ? 0.3 : 0.15);
    if (fl > flash) {
      flash = fl;
      flashColor = g.color;
    }
  }
  const deriva = 1 + 0.05 * ((frame - ESC[k].desde) / (ESC[k].hasta - ESC[k].desde));
  const ent = k === 0 ? 1 : spring({ frame: frame - ESC[k].desde, fps, config: { damping: 15, mass: 0.5 } });
  const dir = k % 2 ? 1 : -1;
  return (
    <AbsoluteFill style={{ fontFamily: F, overflow: "hidden" }}>
      <Asfalto tono={TONO[k]} vel={k === 1 ? 40 : 22} />
      <Audio
        src={staticFile(musica)}
        volume={(f) => interpolate(f, [0, 6, DURACION_RECOGIDA2 - 40, DURACION_RECOGIDA2], [0.2, 0.11, 0.11, 0], clamp)}
      />
      {ESC.map((e, i) => (
        <Sequence key={i} from={e.voz}>
          <Audio src={staticFile(`recogida2/${LINEAS[i].archivo}.mp3`)} />
        </Sequence>
      ))}
      {GOLPES.filter((g) => g.sfx).map((g, i) => (
        <Sfx key={i} en={g.f} src={g.sfx!} vol={g.fuerte ? 0.45 : 0.3} />
      ))}
      <AbsoluteFill
        style={{
          transform: `translate(${sx + (1 - ent) * 800 * dir}px, ${sy}px) scale(${zoom * deriva}) rotate(${(1 - ent) * 5 * dir}deg)`,
          opacity: Math.min(1, ent * 1.5),
        }}
      >
        <Esc />
      </AbsoluteFill>
      <AbsoluteFill style={{ background: flashColor, opacity: flash, pointerEvents: "none" }} />
      <Subs linea={LINEAS[k]} t={t} color={SUB_COLOR[k]} />
    </AbsoluteFill>
  );
};
