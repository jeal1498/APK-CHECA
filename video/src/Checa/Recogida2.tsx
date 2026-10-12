import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { F, FD, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Billetes, Bomba, Carro, K, Letrero, SemaforoLuz, Ticket } from "./graficos";
import { LINEAS } from "./lineasRecogida2";
import { Cae, Golpe, GolpeCam, Pill, Titulo, VideoExplicativo, armarEscenas, centro, clamp, crearEn, impulso, useResorte } from "./plantilla";

// «Los 6 kilómetros que nadie te paga» v2 · narrador, gráficos de calle, ritmo alto · ~38 s a 30 fps
export const MUSICA = "recogida2/musica.mp3";
const { esc: ESC, duracion } = armarEscenas(LINEAS);
export const DURACION_RECOGIDA2 = duracion;
const en = crearEn(LINEAS, ESC);

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

const GOLPES: GolpeCam[] = [
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

export const Recogida2: React.FC<{ musica?: string }> = ({ musica = MUSICA }) => (
  <VideoExplicativo
    carpeta="recogida2"
    lineas={LINEAS}
    esc={ESC}
    duracion={DURACION_RECOGIDA2}
    escenas={ESCENAS}
    golpes={GOLPES}
    subColor={SUB_COLOR}
    tonos={TONO}
    musica={musica}
  />
);
