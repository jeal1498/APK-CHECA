import React, { useLayoutEffect } from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { useThree } from "@react-three/fiber";
import type { PerspectiveCamera } from "three";
import { F, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Carro3D, Edificios } from "./Calle3D";
import { Gasolinera } from "./Vistas3D";
import { Caja, Cajas, Grande, Guiones, Pin, Viaje } from "./SinLana";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasRecogida";

// Anuncio de ~23 s: «Recogidas lejanas, kilómetros que nadie paga». Escenario nuevo: la misma ciudad de noche.
// Hook (l1) · agitación (l2) · solución con Checa (l3) · CTA (l4). Varios planos por línea, cortados por palabra.
const { esc, duracion } = armarEscenas(LINEAS, 5, 8, 60);
export const DURACION_RECOGIDA = duracion;
const en = crearEn(LINEAS, esc);

type Vista = "telefono" | "detras" | "dron" | "dentro" | "bomba" | "popup" | "cierre";
const PLANOS: { desde: number; v: Vista }[] = [
  { desde: 0, v: "telefono" },
  { desde: esc[1].desde, v: "detras" },
  { desde: en(1, "y") - 3, v: "dron" },
  { desde: esc[2].desde, v: "dentro" },
  { desde: en(2, "Con") - 3, v: "bomba" },
  { desde: en(2, "Y") - 3, v: "popup" },
  { desde: esc[3].desde, v: "cierre" },
];

const PX = "Silkscreen";
const C = { cian: "#22D3EE", magenta: "#EC4899", amarillo: "#FACC15", verde: "#22C55E", rojo: "#F43F5E", texto: "#FFFFFF", tenue: "#C9CFF0", calle: "#262B55", banqueta: "#4A5080" };
const NOCHE = "linear-gradient(180deg, #070A1F 0%, #141A45 55%, #2E2466 100%)";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const suave = { ...clamp, easing: Easing.inOut(Easing.cubic) };
const ap = (f: number, d: number, dur = 10) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
const GS: [number, number, number] = [220, 0, 0];
const Z_PIN = -42;

const plano = (f: number) => {
  let i = 0;
  PLANOS.forEach((p, k) => {
    if (f >= p.desde) i = k;
  });
  return i;
};
const local = (f: number, i: number) => interpolate(f, [PLANOS[i].desde, PLANOS[i + 1]?.desde ?? duracion], [0, 1], clamp);
const zCarro = (v: Vista, t: number) => (v === "telefono" ? -t * 4 : v === "detras" ? -t * 12 : v === "dentro" ? -t * 16 : -10);

const Camara: React.FC = () => {
  const frame = useCurrentFrame();
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const i = plano(frame);
  const t = local(frame, i);
  const v = PLANOS[i].v;
  const z = zCarro(v, t);
  let pos = [2.5, 10, z + 14];
  let mira = [0, 0, z - 9];
  let fov = 55;
  if (v === "telefono") {
    const g = interpolate(t, [0.45, 1], [0, 1], suave);
    pos = [2.5, interpolate(g, [0, 1], [40, 10]), z + interpolate(g, [0, 1], [4, 14])];
  } else if (v === "dron") {
    const g = interpolate(t, [0, 0.8], [0, 1], suave);
    pos = [interpolate(g, [0, 1], [2.5, 20]), interpolate(g, [0, 1], [12, 110]), interpolate(g, [0, 1], [z + 14, 18])];
    mira = [0, 0, interpolate(g, [0, 1], [z - 9, -62])];
  } else if (v === "bomba") {
    const k = interpolate(t, [0, 1], [0, 1], suave);
    pos = [GS[0] + 3 - k * 1.2, 3.2, GS[2] + 7 - k * 3];
    mira = [GS[0] - 3, 2.4, GS[2] + 1];
    fov = 40;
  } else if (v === "dentro" || v === "popup" || v === "cierre") {
    pos = [-0.6, 1.9, z];
    mira = [-0.2, 1.6, z - 20];
    fov = 62;
  }
  useLayoutEffect(() => {
    camera.fov = fov;
    camera.position.set(pos[0], pos[1], pos[2]);
    camera.lookAt(mira[0], mira[1], mira[2]);
    camera.updateProjectionMatrix();
  });
  return null;
};

// Escenario nuevo: postes con luz cálida a los dos lados de la calle
const Postes: React.FC = () => (
  <>
    {Array.from({ length: 12 }).map((_, k) =>
      [-1, 1].map((lado) => {
        const z = 10 - k * 18 + (lado > 0 ? 9 : 0);
        return (
          <group key={`${k}${lado}`} position={[lado * 7.6, 0, z]}>
            <mesh position={[0, 3.5, 0]}>
              <boxGeometry args={[0.3, 7, 0.3]} />
              <meshStandardMaterial color="#2A2F5A" />
            </mesh>
            <mesh position={[-lado * 0.9, 7, 0]}>
              <boxGeometry args={[2, 0.25, 0.5]} />
              <meshStandardMaterial color="#2A2F5A" />
            </mesh>
            <mesh position={[-lado * 1.6, 6.8, 0]}>
              <boxGeometry args={[0.7, 0.25, 0.7]} />
              <meshBasicMaterial color="#FFE3A0" />
            </mesh>
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[-lado * 1.8, 0.03, 0]}>
              <circleGeometry args={[3.4, 24]} />
              <meshBasicMaterial color="#FFC14D" transparent opacity={0.22} />
            </mesh>
          </group>
        );
      }),
    )}
  </>
);

// Carro del conductor con faros encendidos
const CarroNoche: React.FC<{ z: number }> = ({ z }) => (
  <>
    <Carro3D z={z} color="#FFFFFF" />
    {[-0.8, 0.8].map((x) => (
      <mesh key={x} position={[x, 0.95, z - 2.35]}>
        <boxGeometry args={[0.6, 0.3, 0.1]} />
        <meshBasicMaterial color="#FFF4C2" />
      </mesh>
    ))}
    <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0.04, z - 8]}>
      <planeGeometry args={[4, 11]} />
      <meshBasicMaterial color="#FFF4C2" transparent opacity={0.16} />
    </mesh>
  </>
);

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = plano(frame);
  const t = local(frame, i);
  const v = PLANOS[i].v;
  const zc = zCarro(v, t);
  const verRuta = v === "detras" || v === "dron";
  return (
    <>
      <Camara />
      <ambientLight intensity={0.4} />
      <directionalLight position={[20, 40, 10]} intensity={0.45} color="#8FA2FF" />
      <hemisphereLight args={["#3B4A9A", "#120C2A", 0.5]} />
      <fog attach="fog" args={["#141A45", v === "dron" ? 100 : 50, v === "dron" ? 260 : 170]} />
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.01, -80]}>
        <planeGeometry args={[200, 320]} />
        <meshStandardMaterial color={C.banqueta} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0, -80]}>
        <planeGeometry args={[14, 320]} />
        <meshStandardMaterial color={C.calle} />
      </mesh>
      {Array.from({ length: 40 }).map((_, k) => (
        <mesh key={k} rotation={[-Math.PI / 2, 0, 0]} position={[-4.2, 0.02, 20 - k * 6]}>
          <planeGeometry args={[0.3, 3]} />
          <meshBasicMaterial color="#AEB4E8" />
        </mesh>
      ))}
      <Edificios />
      <Postes />
      {verRuta ? (
        <>
          <Guiones desde={zc - 3} hasta={Z_PIN} p={v === "dron" ? 1 : ap(frame, en(1, "manejas") - 4, 14)} />
          <Viaje p={v === "dron" ? 1 : ap(frame, en(1, "setenta") - 4, 20)} />
          <Pin z={Z_PIN} />
        </>
      ) : null}
      {v !== "dentro" && v !== "popup" && v !== "cierre" && v !== "bomba" ? <CarroNoche z={zc} /> : null}
      <Carro3D z={-100 + frame * 0.5} x={-3.6} color="#F2B544" />
      <Carro3D z={-170 + frame * 0.45} x={-3.6} color="#F06BA8" />
      <Gasolinera />
    </>
  );
};

const POPUP = (ancho: number) => (
  <Popup
    ancho={ancho}
    nivel="rojo"
    app="App"
    exacto
    neta="44.00"
    viaje="70.00"
    gas="26.00"
    fichas={[
      { v: "$140/h", l: "meta $150", c: "mal" },
      { v: "$5.38/km", l: "meta $8", c: "mal" },
      { v: "30 min", l: "13.0 km total", c: "neutro" },
    ]}
  />
);
const OFERTA = (ancho: number) => <Oferta app="App de viajes" tarifa="$70.00" tramos={["14 min (6.0 km)", "16 min (7.0 km)"]} ancho={ancho} />;

const Capas: React.FC = () => {
  const frame = useCurrentFrame();
  const i = plano(frame);
  const t = local(frame, i);
  const v = PLANOS[i].v;
  if (v === "telefono") {
    const g = interpolate(t, [0.6, 0.95], [0, 1], suave);
    return (
      <>
        <div style={{ position: "absolute", left: 0, right: 0, top: 150, display: "flex", justifyContent: "center", opacity: 1 - interpolate(t, [0.8, 0.98], [0, 1], clamp) }}>
          <div style={{ transform: `scale(${interpolate(g, [0, 1], [0.8, 0.2])})`, transformOrigin: "50% 0%" }}>
            <Telefono ancho={760} alto={1400}>
              <Mapa />
              <div style={{ position: "absolute", left: 0, right: 0, bottom: 20, display: "flex", justifyContent: "center" }}>{OFERTA(700)}</div>
            </Telefono>
          </div>
        </div>
        <Cajas
          items={[
            { desde: en(0, "seis"), color: C.magenta, texto: "6 KM PARA RECOGER" },
            { desde: en(0, "regalando."), color: C.amarillo, texto: "$0 POR ESOS KM" },
          ]}
        />
      </>
    );
  }
  if (v === "detras") {
    return (
      <Cajas
        items={[
          { desde: PLANOS[i].desde, color: C.amarillo, texto: "OFERTA · $70" },
          { desde: en(1, "trece"), color: C.texto, texto: "13 KM TOTALES" },
        ]}
      />
    );
  }
  if (v === "dron") {
    const gratis = frame >= en(1, "Casi");
    return (
      <>
        <Cajas
          items={[
            { desde: PLANOS[i].desde, color: C.cian, texto: "7 KM PAGADOS" },
            { desde: en(1, "Casi"), color: C.magenta, texto: "6 KM GRATIS" },
          ]}
        />
        <Grande desde={gratis ? en(1, "Casi") : PLANOS[i].desde + 4} texto={gratis ? "6 DE 13" : "13 KM"} color={gratis ? C.magenta : C.amarillo} etiqueta={gratis ? "KM SIN PAGO" : "LO QUE MANEJAS"} />
      </>
    );
  }
  if (v === "dentro") {
    const cae = en(2, "suma");
    return (
      <>
        <div style={{ position: "absolute", left: 0, right: 0, top: 1180, bottom: 0, background: "#110D22", borderTopLeftRadius: 300, borderTopRightRadius: 300, borderTop: "10px solid #241D42" }} />
        <div style={{ position: "absolute", right: 70, top: 330, transform: "rotate(-4deg)" }}>
          <Telefono ancho={470} alto={830}>
            <Mapa />
            <div style={{ position: "absolute", left: 0, right: 0, bottom: 12, display: "flex", justifyContent: "center" }}>{OFERTA(430)}</div>
            <div style={{ position: "absolute", left: 0, right: 0, top: 40, display: "flex", justifyContent: "center", opacity: ap(frame, cae, 6), transform: `translateY(${interpolate(frame, [cae, cae + 8], [-80, 0], suave)}px)` }}>
              {POPUP(430)}
            </div>
          </Telefono>
        </div>
        <Cajas items={[{ desde: PLANOS[i].desde, color: C.cian, texto: "CHECA SUMA LA RECOGIDA" }]} />
      </>
    );
  }
  if (v === "bomba") {
    const pesos = interpolate(frame, [en(2, "gasolina,"), en(2, "cuarenta")], [0, 26], clamp);
    const real = frame >= en(2, "cuarenta");
    return (
      <>
        <Cajas
          items={[
            { desde: PLANOS[i].desde, color: C.texto, texto: "13 KM × $2" },
            { desde: en(2, "cuarenta"), color: C.amarillo, texto: "GANANCIA REAL" },
          ]}
        />
        {real ? (
          <Grande desde={en(2, "cuarenta")} texto="$44" color={C.amarillo} etiqueta="TE QUEDAN" />
        ) : (
          <Grande desde={PLANOS[i].desde + 2} texto="$70" color={C.amarillo} etiqueta="LA OFERTA" tachar={ap(frame, en(2, "cuarenta") - 8, 8)} />
        )}
        <div style={{ position: "absolute", left: 0, right: 0, top: 860, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
          <div style={{ fontFamily: PX, fontWeight: 700, fontSize: 110, color: C.magenta, background: "#0B1230", border: `6px solid ${C.magenta}`, padding: "10px 40px", textShadow: `0 0 24px ${C.magenta}88` }}>
            −${pesos.toFixed(2)}
          </div>
          <div style={{ fontFamily: PX, fontSize: 32, color: C.texto, textShadow: "0 3px 10px #000" }}>GASOLINA</div>
        </div>
      </>
    );
  }
  if (v === "popup") {
    const g = ap(frame, PLANOS[i].desde, 8);
    const alerta = ap(frame, en(2, "rechazar."), 4) * (1 - ap(frame, en(2, "rechazar.") + 6, 14));
    return (
      <>
        <AbsoluteFill style={{ background: "rgba(7,10,31,0.72)" }} />
        <AbsoluteFill style={{ boxShadow: `inset 0 0 ${160 * alerta}px ${C.rojo}` }} />
        <Cajas items={[{ desde: PLANOS[i].desde, color: C.texto, texto: "13 KM TOTALES · REAL $44" }]} />
        <div style={{ position: "absolute", left: 0, right: 0, top: 330, display: "flex", justifyContent: "center", transform: `scale(${(0.85 + 0.15 * g) * (1 + 0.04 * alerta)})`, opacity: g }}>{POPUP(940)}</div>
      </>
    );
  }
  // cierre (CTA)
  return (
    <AbsoluteFill style={{ background: NOCHE, opacity: ap(frame, PLANOS[i].desde, 8), display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 420 }}>
      <Caja color={C.cian} tam={46}>DEJA DE MANEJAR GRATIS</Caja>
      <div style={{ opacity: ap(frame, en(3, "Pruébala"), 10) }}>
        <Logo tam={120} />
      </div>
      <div style={{ fontFamily: F, fontWeight: 700, fontSize: 44, color: C.texto, opacity: ap(frame, en(3, "Pruébala"), 10) }}>7 días gratis · link en el perfil</div>
    </AbsoluteFill>
  );
};

// Subtítulos gigantes al centro: 1–3 palabras, cifras en amarillo, Checa y la prueba en verde, «rechazar» en rojo
const color = (linea: number, w: string) => {
  const p = w.toLowerCase().replace(/[^a-záéíóúñ]/g, "");
  if (p === "rechazar") return C.rojo;
  if (p === "checa" || (linea === 3 && ["pruébala", "siete", "días", "gratis"].includes(p))) return C.verde;
  if (["seis", "setenta", "trece", "siete", "mitad", "cuarenta", "cuatro", "gratis", "regalando"].includes(p)) return C.amarillo;
  return C.texto;
};

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const i = esc.findIndex((e) => frame < e.hasta);
  const k = i < 0 ? esc.length - 1 : i;
  const l = LINEAS[k];
  const t = (frame - esc[k].voz) / 30;
  if (t < -0.1 || t > l.dur + 0.3) return null;
  const grupos: [number, number][] = [];
  let ini = 0;
  l.palabras.forEach((p, j) => {
    if (/[.!?,:]$/.test(p.w) || j - ini === 2 || j === l.palabras.length - 1) {
      grupos.push([ini, j]);
      ini = j + 1;
    }
  });
  const g = [...grupos].reverse().find(([a]) => t >= l.palabras[a].s - 0.05) ?? grupos[0];
  const inicio = esc[k].voz + Math.round(l.palabras[g[0]].s * 30);
  const s = 1.18 - 0.18 * ap(frame, inicio - 1, 5);
  return (
    <div style={{ position: "absolute", top: 1280, left: 40, right: 40, textAlign: "center", fontFamily: F, fontWeight: 800, fontSize: 84, lineHeight: 1.08, transform: `scale(${s})`, WebkitTextStroke: "3px #000", paintOrder: "stroke fill", textShadow: "0 6px 0 #000, 0 10px 30px rgba(0,0,0,0.9)" }}>
      {l.palabras.slice(g[0], g[1] + 1).map((p, j) => (
        <span key={j} style={{ color: color(k, p.w) }}>
          {p.w}{" "}
        </span>
      ))}
    </div>
  );
};

const Sfx: React.FC<{ en: number; src: string; vol: number }> = ({ en: e, src, vol }) => (
  <Sequence from={e} durationInFrames={45}>
    <Audio src={staticFile(src)} volume={vol} />
  </Sequence>
);

export const Recogida: React.FC = () => {
  const frame = useCurrentFrame();
  const cierre = PLANOS[plano(frame)].v === "cierre";
  return (
    <AbsoluteFill style={{ background: NOCHE }}>
      <ThreeCanvas width={1080} height={1920} camera={{ fov: 55, position: [3, 7, 15], near: 0.5, far: 500 }}>
        <Escena />
      </ThreeCanvas>
      {!cierre ? <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 480, background: "linear-gradient(180deg, rgba(7,10,31,0.8) 0%, rgba(7,10,31,0.4) 65%, rgba(7,10,31,0) 100%)" }} /> : null}
      <Capas />
      <Subs />

      {LINEAS.map((l, k) => (
        <Sequence key={l.archivo} from={esc[k].voz}>
          <Audio src={staticFile(`recogida/${l.archivo}.mp3`)} />
        </Sequence>
      ))}
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
      {PLANOS.slice(1).map((p) => (
        <Sfx key={p.desde} en={p.desde} src="whoosh.wav" vol={0.09} />
      ))}
      <Sfx en={en(0, "seis")} src="golpe.wav" vol={0.2} />
      <Sfx en={en(1, "Casi")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(2, "suma")} src="ding.wav" vol={0.14} />
      <Sfx en={en(2, "cuarenta")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(2, "rechazar.")} src="error.wav" vol={0.16} />
      <Sfx en={en(3, "Pruébala")} src="ding.wav" vol={0.12} />
    </AbsoluteFill>
  );
};
