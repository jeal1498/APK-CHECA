import React, { useLayoutEffect } from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { useThree } from "@react-three/fiber";
import type { PerspectiveCamera } from "three";
import { F, Logo, Mapa, Telefono } from "./ui";
import { Carro3D, Edificios } from "./Calle3D";
import { Gasolinera } from "./Vistas3D";
import { Caja, Cajas, Grande, Guiones, Pin, Viaje } from "./SinLana";
import { CarroNoche, NOCHE, OFERTA, POPUP, Postes } from "./Recogida";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasRecogida2";

// Anuncio «Recogidas lejanas» v2 (guion corregido de Gemini): un corte de toma por línea de voz, ciudad de noche.
const { esc, duracion } = armarEscenas(LINEAS, 2, 3, 45);
export const DURACION_RECOGIDA2 = duracion;
const en = crearEn(LINEAS, esc);

type Vista = "detras" | "bomba" | "telefono" | "dron" | "lateral" | "dentro" | "popup" | "iso" | "cierre";
const VISTAS: Vista[] = ["detras", "bomba", "telefono", "dron", "lateral", "dentro", "popup", "iso", "cierre"];

const PX = "Silkscreen";
const C = { cian: "#22D3EE", magenta: "#EC4899", amarillo: "#FACC15", verde: "#22C55E", rojo: "#F43F5E", texto: "#FFFFFF", calle: "#262B55", banqueta: "#4A5080" };
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const suave = { ...clamp, easing: Easing.inOut(Easing.cubic) };
const ap = (f: number, d: number, dur = 10) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
const GS: [number, number, number] = [220, 0, 0];
const Z_PIN = -42;

const escena = (f: number) => {
  const i = esc.findIndex((e) => f < e.hasta);
  return i < 0 ? esc.length - 1 : i;
};
const local = (f: number, i: number) => interpolate(f, [esc[i].desde, esc[i].hasta], [0, 1], clamp);
const zCarro = (v: Vista, t: number) =>
  v === "detras" ? -t * 10 : v === "telefono" ? -t * 4 : v === "lateral" ? -t * 20 : v === "dentro" || v === "popup" ? -t * 16 : v === "iso" ? -t * 8 : -10;

const Camara: React.FC = () => {
  const frame = useCurrentFrame();
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  const z = zCarro(v, t);
  let pos = [2.5, 10, z + 14];
  let mira = [0, 0, z - 9];
  let fov = 55;
  if (v === "telefono") {
    const g = interpolate(t, [0.55, 1], [0, 1], suave);
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
  } else if (v === "lateral") {
    pos = [16, 3.5, z];
    mira = [0, 2.2, z];
    fov = 48;
  } else if (v === "iso") {
    const a = Math.PI / 3 + t * 0.3;
    pos = [Math.cos(a) * 95, 150, -14 + Math.sin(a) * 95];
    mira = [0, 0, -14];
    fov = 30;
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

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  const zc = zCarro(v, t);
  return (
    <>
      <Camara />
      <ambientLight intensity={0.4} />
      <directionalLight position={[20, 40, 10]} intensity={0.45} color="#8FA2FF" />
      <hemisphereLight args={["#3B4A9A", "#120C2A", 0.5]} />
      <fog attach="fog" args={["#141A45", v === "dron" || v === "iso" ? 100 : 50, v === "dron" || v === "iso" ? 280 : 170]} />
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
      <Edificios soloLado={v === "lateral" ? -1 : undefined} />
      <Postes />
      {v === "detras" ? (
        <>
          <Guiones desde={zc - 3} hasta={Z_PIN} p={ap(frame, en(0, "seis") - 4, 16)} />
          <Pin z={Z_PIN} e={ap(frame, en(0, "pasaje?") - 6, 10)} />
        </>
      ) : null}
      {v === "dron" || v === "lateral" ? (
        <>
          <Guiones desde={v === "lateral" ? 4 : zc - 3} hasta={Z_PIN} />
          <Viaje p={1} />
          <Pin z={Z_PIN} />
        </>
      ) : null}
      {v === "detras" || v === "telefono" || v === "dron" || v === "lateral" || v === "iso" ? <CarroNoche z={zc} /> : null}
      <Carro3D z={-100 + frame * 0.5} x={-3.6} color="#F2B544" />
      <Carro3D z={-170 + frame * 0.45} x={-3.6} color="#F06BA8" />
      <Carro3D z={-60 - frame * 0.3} x={3.6} color="#5B6CF0" />
      <Gasolinera />
    </>
  );
};

const Capas: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  if (v === "detras") {
    return (
      <Cajas
        items={[
          { desde: en(0, "seis"), color: C.magenta, texto: "6 KM SOLO PARA RECOGER" },
          { desde: en(0, "pasaje?"), color: C.amarillo, texto: "PAGO: $0" },
        ]}
      />
    );
  }
  if (v === "bomba") {
    const pesos = interpolate(frame, [esc[1].voz, en(1, "gasolina.") + 6], [0, 12], clamp);
    return (
      <>
        <Cajas items={[{ desde: en(1, "regalando"), color: C.magenta, texto: "6 KM × $2 = GASOLINA REGALADA" }]} />
        <div style={{ position: "absolute", left: 0, right: 0, top: 640, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
          <div style={{ fontFamily: PX, fontWeight: 700, fontSize: 130, color: C.magenta, background: "#0B1230", border: `6px solid ${C.magenta}`, padding: "10px 40px", textShadow: `0 0 24px ${C.magenta}88` }}>
            ${pesos.toFixed(2)}
          </div>
          <div style={{ fontFamily: PX, fontSize: 32, color: C.texto, textShadow: "0 3px 10px #000" }}>SOLO EN LA RECOGIDA</div>
        </div>
      </>
    );
  }
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
            { desde: en(2, "setenta"), color: C.amarillo, texto: "OFERTA · $70" },
            { desde: en(2, "trece"), color: C.texto, texto: "6 + 7 = 13 KM" },
          ]}
        />
      </>
    );
  }
  if (v === "dron") {
    return (
      <>
        <Cajas
          items={[
            { desde: esc[3].voz, color: C.cian, texto: "7 KM PAGADOS" },
            { desde: en(3, "mitad"), color: C.magenta, texto: "6 KM GRATIS" },
          ]}
        />
        <Grande desde={en(3, "mitad")} texto="6 DE 13" color={C.magenta} etiqueta="KM DE A GRATIS" />
      </>
    );
  }
  if (v === "lateral") {
    const km = interpolate(t, [0.05, 0.8], [0, 13], suave);
    const marcas = Array.from({ length: 14 }, (_, k) => k).filter((m) => Math.abs(m - km) < 5);
    return (
      <>
        <Cajas
          items={[
            { desde: en(4, "recogida"), color: C.cian, texto: "+ RECOGIDA" },
            { desde: en(4, "gasolina"), color: C.magenta, texto: "− GASOLINA $26" },
          ]}
        />
        <div style={{ position: "absolute", left: 0, right: 0, top: 620, height: 120, borderTop: `4px solid ${C.cian}` }}>
          {marcas.map((m) => (
            <div key={m} style={{ position: "absolute", left: 540 + (m - km) * 120, top: 0, transform: "translateX(-50%)", display: "flex", flexDirection: "column", alignItems: "center" }}>
              <div style={{ width: 4, height: 40, background: m <= 6 ? C.magenta : C.cian }} />
              <div style={{ fontFamily: PX, fontSize: 26, color: m <= 6 ? C.magenta : C.cian, marginTop: 6, whiteSpace: "nowrap" }}>{m} KM</div>
            </div>
          ))}
          <div style={{ position: "absolute", left: 540, top: -74, transform: "translateX(-50%)", fontFamily: PX, fontWeight: 700, fontSize: 44, color: C.amarillo, background: "#0B1230", border: `4px solid ${C.amarillo}`, padding: "4px 14px", whiteSpace: "nowrap" }}>
            {km.toFixed(1)} KM
          </div>
        </div>
      </>
    );
  }
  if (v === "dentro") {
    const cae = esc[5].voz;
    return (
      <>
        <div style={{ position: "absolute", left: 0, right: 0, top: 1180, bottom: 0, background: "#110D22", borderTopLeftRadius: 300, borderTopRightRadius: 300, borderTop: "10px solid #241D42" }} />
        <div style={{ position: "absolute", right: 60, top: 470, transform: "rotate(-4deg)" }}>
          <Telefono ancho={440} alto={760}>
            <Mapa />
            <div style={{ position: "absolute", left: 0, right: 0, bottom: 12, display: "flex", justifyContent: "center" }}>{OFERTA(400)}</div>
            <div style={{ position: "absolute", left: 0, right: 0, top: 40, display: "flex", justifyContent: "center", opacity: ap(frame, cae, 6), transform: `translateY(${interpolate(frame, [cae, cae + 8], [-80, 0], suave)}px)` }}>
              {POPUP(400)}
            </div>
          </Telefono>
        </div>
        <Grande desde={en(5, "cuarenta")} texto="$44" color={C.amarillo} etiqueta="LIMPIOS" top={180} />
      </>
    );
  }
  if (v === "popup") {
    const g = ap(frame, esc[6].desde, 6);
    const alerta = ap(frame, en(6, "rechazar."), 4) * (1 - ap(frame, en(6, "rechazar.") + 6, 14));
    return (
      <>
        <AbsoluteFill style={{ background: "rgba(7,10,31,0.72)" }} />
        <AbsoluteFill style={{ boxShadow: `inset 0 0 ${160 * alerta}px ${C.rojo}` }} />
        <Cajas items={[{ desde: esc[6].desde, color: C.texto, texto: "13 KM TOTALES · REAL $44" }]} />
        <div style={{ position: "absolute", left: 0, right: 0, top: 330, display: "flex", justifyContent: "center", transform: `scale(${(0.85 + 0.15 * g) * (1 + 0.05 * alerta)})`, opacity: g }}>{POPUP(940)}</div>
      </>
    );
  }
  if (v === "iso") {
    const a = ap(frame, esc[7].desde, 6);
    return (
      <div style={{ position: "absolute", left: 0, right: 0, top: 300, display: "flex", justifyContent: "center", opacity: a, transform: `scale(${1.2 - 0.2 * a})` }}>
        <Caja color={C.magenta} tam={50}>DEJA DE REGALAR</Caja>
      </div>
    );
  }
  // cierre (CTA)
  return (
    <AbsoluteFill style={{ background: NOCHE, opacity: ap(frame, esc[8].desde, 6), display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 420 }}>
      <Logo tam={120} />
      <Caja color={C.verde} tam={44}>7 DÍAS DE PRUEBA SIN COSTO</Caja>
      <div style={{ fontFamily: F, fontWeight: 700, fontSize: 44, color: C.texto, opacity: ap(frame, en(8, "link"), 8) }}>Link en el perfil</div>
    </AbsoluteFill>
  );
};

// Subtítulos gigantes al centro: cifras en amarillo, Checa y la prueba en verde, «rechazar» en rojo
const color = (linea: number, w: string) => {
  const p = w.toLowerCase().replace(/[^a-záéíóúñ]/g, "");
  if (p === "rechazar") return C.rojo;
  if (p === "checa" || (linea === 8 && ["siete", "días", "prueba", "costo"].includes(p))) return C.verde;
  if (["seis", "setenta", "trece", "mitad", "gratis", "cuarenta", "cuatro", "limpios", "regalando", "regalar", "gasolina"].includes(p)) return C.amarillo;
  return C.texto;
};

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const k = escena(frame);
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

export const Recogida2: React.FC = () => {
  const frame = useCurrentFrame();
  const cierre = VISTAS[escena(frame)] === "cierre";
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
          <Audio src={staticFile(`recogida2/${l.archivo}.mp3`)} />
        </Sequence>
      ))}
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 8, duracion - 30, duracion], [0, 0.14, 0.14, 0], clamp)} />
      {esc.slice(1).map((e) => (
        <Sfx key={e.desde} en={e.desde} src="whoosh.wav" vol={0.09} />
      ))}
      <Sfx en={en(0, "seis")} src="golpe.wav" vol={0.2} />
      <Sfx en={en(3, "mitad")} src="pop.mp3" vol={0.15} />
      <Sfx en={esc[5].voz} src="ding.wav" vol={0.14} />
      <Sfx en={en(5, "cuarenta")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(6, "rechazar.")} src="error.wav" vol={0.16} />
      <Sfx en={esc[8].desde} src="ding.wav" vol={0.12} />
    </AbsoluteFill>
  );
};
