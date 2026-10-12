import React from "react";
import { interpolate, useCurrentFrame } from "remotion";
import { F, FD, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Billetes, Bomba, Carro, K, Letrero, Ticket } from "./graficos";
import { LINEAS } from "./lineasGasolina150";
import { Cae, Golpe, GolpeCam, Pill, Titulo, VideoExplicativo, armarEscenas, centro, clamp, crearEn, impulso, useResorte } from "./plantilla";

// «Tus $150 no son $150» · video 1 del calendario · narrador, gráficos de calle · ~37 s a 30 fps
export const MUSICA = "recogida2/musica.mp3";
const { esc: ESC, duracion } = armarEscenas(LINEAS);
export const DURACION_GASOLINA150 = duracion;
const en = crearEn(LINEAS, ESC);

const TRAMOS: [string, string] = ["8 min (4.0 km)", "24 min (16.0 km)"];

// 1 · Gancho: la oferta de $150
const S1: React.FC = () => {
  const frame = useCurrentFrame();
  const vibra = Math.sin(frame * 3.1) * 6 * (impulso(frame, 0, 10) + impulso(frame, en(0, "menos."), 8));
  return (
    <>
      <Golpe desde={0} top={110}>
        <Titulo tam={190}>$150</Titulo>
      </Golpe>
      <Golpe desde={en(0, "menos.")} top={320} rot={-3}>
        <Titulo tam={110} color={K.amarillo}>
          ¿CUÁNTO LLEGA?
        </Titulo>
      </Golpe>
      <div style={{ ...centro, top: 480, transform: `rotate(${vibra}deg)` }}>
        <Telefono ancho={500} alto={680}>
          <Mapa />
          <div style={{ ...centro, bottom: 14 }}>
            <Oferta app="App de viajes" tarifa="$150.00" tramos={TRAMOS} ancho={450} />
          </div>
        </Telefono>
      </div>
      <Golpe desde={en(0, "Al")} top={1190}>
        <Pill>Al final: cuánto se va al mes 👀</Pill>
      </Golpe>
    </>
  );
};

// 2 · La gasolina no aparece
const S2: React.FC = () => {
  const frame = useCurrentFrame();
  const gira = Math.sin(frame / 4) * 4;
  return (
    <>
      <Cae desde={ESC[1].desde + 2} top={110}>
        <Letrero arriba="LA OFERTA DICE" grande="$150" ancho={700} />
      </Cae>
      <div style={{ ...centro, top: 520, transform: `rotate(${gira}deg)` }}>
        <Bomba pesos={0} litros={0} tam={300} />
      </div>
      <Golpe desde={en(1, "gasolina")} top={560} rot={8}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 260, color: K.amarillo, marginLeft: 420 }}>?</div>
      </Golpe>
      <Golpe desde={en(1, "ningún")} top={1050} rot={-4}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 80, color: K.rojo, border: `10px solid ${K.rojo}`, borderRadius: 20, padding: "0 30px" }}>
          NO APARECE
        </div>
      </Golpe>
    </>
  );
};

// 3 · Recogida + viaje = 20 km
const S3: React.FC = () => {
  const frame = useCurrentFrame();
  const entra = useResorte(ESC[2].desde, 14);
  const dash = (frame * 30) % 160;
  return (
    <>
      <div style={{ position: "absolute", left: 0, right: 0, top: 1060, height: 110, background: K.asfalto2, overflow: "hidden" }}>
        <div style={{ position: "absolute", top: 50, left: -dash, display: "flex", gap: 80 }}>
          {Array.from({ length: 12 }).map((_, i) => (
            <div key={i} style={{ width: 80, height: 12, background: K.linea, borderRadius: 6 }} />
          ))}
        </div>
      </div>
      <div style={{ position: "absolute", top: 890, left: 300 + (1 - entra) * -900 + Math.sin(frame / 6) * 6 }}>
        <Carro ancho={460} avance={frame * 8} />
      </div>
      <Cae desde={en(2, "cuatro")} top={90}>
        <Letrero arriba="RECOGIDA" grande="4 km" fondo={K.rojo} ancho={560} />
      </Cae>
      <Cae desde={en(2, "dieciséis.")} top={360}>
        <Letrero arriba="VIAJE" grande="16 km" ancho={560} />
      </Cae>
      <Golpe desde={en(2, "Veinte")} top={630} rot={-2}>
        <Letrero arriba="TÚ MANEJAS" grande="20 km" fondo={K.amarillo} texto="#111" ancho={720} />
      </Golpe>
    </>
  );
};

// 4 · Cada km cuesta $2
const S4: React.FC = () => {
  const frame = useCurrentFrame();
  const pesos = interpolate(frame, [ESC[3].voz, en(3, "veinticuatro")], [0, 24], clamp);
  return (
    <>
      <div style={{ ...centro, top: 110 }}>
        <Bomba pesos={pesos} litros={pesos / 24} tam={340} />
      </div>
      <Golpe desde={en(3, "veinticuatro")} top={640}>
        <Pill tam={54}>Litro a $24</Pill>
      </Golpe>
      <Golpe desde={en(3, "doce")} top={760}>
        <Pill tam={54}>12 km por litro</Pill>
      </Golpe>
      <Golpe desde={en(3, "dos")} top={910} rot={-2}>
        <Titulo tam={200} color={K.amarillo}>
          $2 <span style={{ fontSize: 100, color: K.blanco }}>por km</span>
        </Titulo>
      </Golpe>
    </>
  );
};

// 5 · El ticket: $150 − $40 = $110
const S5: React.FC = () => {
  const frame = useCurrentFrame();
  const q = en(4, "quedan");
  const queda = Math.round(interpolate(frame, [q, en(4, "diez.") + 4], [150, 110], clamp));
  return (
    <>
      <div style={{ ...centro, top: 110 }}>
        <Ticket
          ancho={820}
          lineas={[
            { l: "Tarifa", v: "$150", desde: ESC[4].desde + 4 },
            { l: "Gasolina 20 km", v: "−$40", c: K.rojo, desde: en(4, "cuarenta") },
            { l: "TE QUEDAN", v: `$${queda}`, c: "#B45309", grande: true, desde: q },
          ]}
        />
      </div>
      <Cae desde={en(4, "cuarenta")} top={850}>
        <Letrero arriba="GASOLINA" grande="−$40" fondo={K.rojo} ancho={560} />
      </Cae>
      <Billetes desde={q} />
    </>
  );
};

// 6 · Lo que se va al mes
const S6: React.FC = () => {
  const frame = useCurrentFrame();
  const fase2 = en(5, "Casi");
  if (frame < fase2) {
    return (
      <>
        <Cae desde={ESC[5].desde + 2} top={130}>
          <Letrero arriba="MANEJAS AL DÍA" grande="150 km" ancho={700} />
        </Cae>
        <Golpe desde={en(5, "trescientos")} top={620}>
          <div style={{ display: "flex", alignItems: "center", gap: 30 }}>
            <Bomba pesos={300} litros={12.5} tam={170} />
            <Titulo tam={150} color={K.amarillo}>
              $300
              <div style={{ fontSize: 56, color: K.blanco, letterSpacing: 0 }}>de gasolina al día</div>
            </Titulo>
          </div>
        </Golpe>
      </>
    );
  }
  const dias = Math.min(26, Math.max(0, Math.floor((frame - fase2) / 1.5)));
  const mes = Math.round(interpolate(frame, [fase2, en(5, "mes") + 6], [300, 7800], clamp) / 100) * 100;
  return (
    <>
      <Golpe desde={fase2} top={110}>
        <div style={{ fontFamily: F, fontWeight: 800, fontSize: 56, color: K.tenue, letterSpacing: 4 }}>EN UN MES · 26 DÍAS</div>
      </Golpe>
      <div style={{ position: "absolute", top: 220, left: 140, right: 140, display: "grid", gridTemplateColumns: "repeat(7, 1fr)", gap: 12 }}>
        {Array.from({ length: 28 }).map((_, i) => {
          const dom = (i + 1) % 7 === 0;
          const n = i - Math.floor((i + 1) / 7);
          const lleno = !dom && n < dias;
          return (
            <div
              key={i}
              style={{
                height: 62,
                borderRadius: 10,
                background: lleno ? K.rojo : "#1E2730",
                color: lleno ? "#fff" : K.tenue,
                fontFamily: F,
                fontWeight: 800,
                fontSize: 26,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                opacity: dom ? 0.35 : 1,
              }}
            >
              {i + 1}
            </div>
          );
        })}
      </div>
      <Golpe desde={en(5, "ocho")} top={610} rot={-2}>
        <Titulo tam={210} color={K.rojo}>
          −${mes.toLocaleString("en-US")}
        </Titulo>
      </Golpe>
      <Golpe desde={en(5, "ninguna")} top={900}>
        <Pill fondo={K.rojo} color="#fff" tam={50}>
          que ninguna oferta te enseña
        </Pill>
      </Golpe>
      <Billetes desde={en(5, "mil")} n={10} />
    </>
  );
};

// 7 · Checa y cierre
const S7: React.FC = () => {
  const frame = useCurrentFrame();
  const cierre = en(6, "Pruébala");
  const pop = useResorte(en(6, "encima") , 11);
  if (frame < cierre) {
    return (
      <>
        <Golpe desde={ESC[6].desde} top={90}>
          <Titulo tam={80}>
            Lo que te queda, <span style={{ color: K.verde }}>en 1 segundo</span>
          </Titulo>
        </Golpe>
        <div style={{ ...centro, top: 240 }}>
          <Telefono ancho={620} alto={1000}>
            <Mapa />
            <div style={{ ...centro, bottom: 14 }}>
              <Oferta app="App de viajes" tarifa="$150.00" tramos={TRAMOS} ancho={560} />
            </div>
            <div style={{ ...centro, top: 50, opacity: Math.min(1, pop * 2), transform: `translateY(${(1 - pop) * -200}px)` }}>
              <Popup
                ancho={570}
                nivel="amarillo"
                app="Viaje"
                neta={110}
                viaje={150}
                gas={40}
                fichas={[
                  { v: "$281", l: "por hora", c: "ok" },
                  { v: "$7.5", l: "por km", c: "mal" },
                  { v: "32 min", l: "20 km total", c: "neutro" },
                ]}
              />
            </div>
          </Telefono>
        </div>
      </>
    );
  }
  const sale = interpolate(frame, [cierre + 10, ESC[6].hasta], [-600, 1300]);
  return (
    <>
      <Golpe desde={cierre} top={140}>
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
      <Golpe desde={en(6, "¿Tú")} top={850} rot={-2}>
        <Titulo tam={96} color={K.amarillo}>
          ¿Ya le restas
          <br />
          la gasolina? 👇
        </Titulo>
      </Golpe>
    </>
  );
};

const ESCENAS = [S1, S2, S3, S4, S5, S6, S7];

const GOLPES: GolpeCam[] = [
  { f: 0, color: K.blanco, fuerte: true, sfx: "golpe.wav" },
  { f: en(0, "menos."), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: en(0, "Al"), color: K.amarillo, sfx: "recogida2/pop.mp3" },
  { f: ESC[1].desde + 2, color: K.verde, sfx: "whoosh.wav" },
  { f: en(1, "gasolina"), color: K.amarillo, sfx: "recogida2/pop.mp3" },
  { f: en(1, "ningún"), color: K.rojo, fuerte: true, sfx: "error.wav" },
  { f: ESC[2].desde, color: K.blanco, sfx: "recogida2/motor.mp3" },
  { f: en(2, "cuatro"), color: K.rojo, sfx: "whoosh.wav" },
  { f: en(2, "dieciséis."), color: K.verde, sfx: "whoosh.wav" },
  { f: en(2, "Veinte"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: ESC[3].desde, color: K.blanco, sfx: "whoosh.wav" },
  { f: en(3, "veinticuatro"), color: K.blanco, sfx: "recogida2/pop.mp3" },
  { f: en(3, "doce"), color: K.blanco, sfx: "recogida2/pop.mp3" },
  { f: en(3, "dos"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: ESC[4].desde, color: K.blanco, sfx: "whoosh.wav" },
  { f: en(4, "cuarenta"), color: K.rojo, fuerte: true, sfx: "error.wav" },
  { f: en(4, "quedan"), color: K.amarillo, sfx: "recogida2/caja.mp3" },
  { f: ESC[5].desde, color: K.blanco, sfx: "whoosh.wav" },
  { f: ESC[5].desde + 2, color: K.verde, sfx: "recogida2/motor.mp3" },
  { f: en(5, "trescientos"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
  { f: en(5, "Casi"), color: K.blanco, sfx: "whoosh.wav" },
  { f: en(5, "ocho"), color: K.rojo, fuerte: true, sfx: "golpe.wav" },
  { f: en(5, "ninguna"), color: K.rojo, sfx: "recogida2/caja.mp3" },
  { f: ESC[6].desde, color: K.amarillo, sfx: "whoosh.wav" },
  { f: en(6, "encima"), color: K.amarillo, fuerte: true, sfx: "recogida2/pop.mp3" },
  { f: en(6, "Pruébala"), color: K.verde, fuerte: true, sfx: "ding.wav" },
  { f: en(6, "siete") + 10, color: K.verde, sfx: "recogida2/motor.mp3" },
  { f: en(6, "¿Tú"), color: K.amarillo, fuerte: true, sfx: "golpe.wav" },
];

const SUB_COLOR = [K.amarillo, K.blanco, K.blanco, K.amarillo, K.amarillo, K.blanco, K.verde];
const TONO = [
  "rgba(250,204,21,0.14)",
  "rgba(244,63,94,0.12)",
  "rgba(255,181,71,0.12)",
  "rgba(250,204,21,0.12)",
  "rgba(244,63,94,0.12)",
  "rgba(244,63,94,0.16)",
  "rgba(34,197,94,0.16)",
];

export const Gasolina150: React.FC<{ musica?: string }> = ({ musica = MUSICA }) => (
  <VideoExplicativo
    carpeta="gasolina150"
    lineas={LINEAS}
    esc={ESC}
    duracion={DURACION_GASOLINA150}
    escenas={ESCENAS}
    golpes={GOLPES}
    subColor={SUB_COLOR}
    tonos={TONO}
    musica={musica}
    volMusica={0.08}
    volSfx={0.6}
  />
);
