import React, { useLayoutEffect } from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { useThree } from "@react-three/fiber";
import type { PerspectiveCamera } from "three";
import { F, FD, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Carro3D, Edificios } from "./Calle3D";
import { Gasolinera } from "./Vistas3D";
import { armarEscenas, crearEn } from "./plantilla";
import { LINEAS } from "./lineasSinLana";

// Tema 1: «Trabajar mucho y no ver el dinero». Una escena (y una vista) por línea de voz.
const { esc, duracion } = armarEscenas(LINEAS, 6, 10, 80);
export const DURACION_SIN_LANA = duracion;
const en = crearEn(LINEAS, esc);

type Vista = "telefono" | "iso" | "detras" | "bomba" | "lateral" | "dron" | "dentro" | "cierre";
const VISTAS: Vista[] = ["telefono", "iso", "detras", "bomba", "lateral", "dron", "dentro", "cierre"];

const PX = "Silkscreen";
const C = { cian: "#22D3EE", magenta: "#EC4899", amarillo: "#FACC15", texto: "#FFFFFF", tenue: "#C9CFF0", calle: "#3A3F6E", banqueta: "#9AA3C7" };
const CIELO = "linear-gradient(180deg, #2B2A7A 0%, #8E4B9C 45%, #F08A5D 100%)";
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const suave = { ...clamp, easing: Easing.inOut(Easing.cubic) };
const ap = (f: number, d: number, dur = 10) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
const GS: [number, number, number] = [220, 0, 0];
const Z_PIN = -42;
const Z_DEST = -120;

const escena = (f: number) => {
  const i = esc.findIndex((e) => f < e.hasta);
  return i < 0 ? esc.length - 1 : i;
};
const local = (f: number, i: number) => interpolate(f, [esc[i].desde, esc[i].hasta], [0, 1], clamp);

// Posición del carro del conductor en cada escena
const zCarro = (i: number, t: number) => [-t * 4, -t * 10, -t * 16, 0, -t * 24, -16, -t * 22, 0][i];

const Camara: React.FC = () => {
  const frame = useCurrentFrame();
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  const z = zCarro(i, t);
  let pos = [0, 10, 10];
  let mira = [0, 0, 0];
  let fov = 55;
  if (v === "telefono") {
    const g = interpolate(t, [0.3, 0.95], [0, 1], suave);
    pos = [2.5, interpolate(g, [0, 1], [40, 10]), z + interpolate(g, [0, 1], [4, 14])];
    mira = [0, 0, z - 9];
  } else if (v === "iso") {
    const a = Math.PI / 3 + t * 0.3;
    pos = [Math.cos(a) * 95, 150, -14 + Math.sin(a) * 95];
    mira = [0, 0, -14];
    fov = 30;
  } else if (v === "detras") {
    pos = [2.5, 10, z + 14];
    mira = [0, 0, z - 9];
  } else if (v === "bomba") {
    const k = interpolate(t, [0, 1], [0, 1], suave);
    pos = [GS[0] + 3 - k * 1.2, 3.2, GS[2] + 7 - k * 3];
    mira = [GS[0] - 3, 2.4, GS[2] + 1];
    fov = 40;
  } else if (v === "lateral") {
    pos = [16, 3.5, z];
    mira = [0, 2.2, z];
    fov = 48;
  } else if (v === "dron") {
    const g = interpolate(t, [0, 0.85], [0, 1], suave);
    pos = [interpolate(g, [0, 1], [2.5, 22]), interpolate(g, [0, 1], [10, 115]), interpolate(g, [0, 1], [-2, 20])];
    mira = [0, 0, interpolate(g, [0, 1], [-25, -70])];
  } else {
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

export const Guiones: React.FC<{ x?: number; desde: number; hasta: number; p?: number; ancho?: number }> = ({ x = 0, desde, hasta, p = 1, ancho = 1.6 }) => {
  const zs: number[] = [];
  for (let z = desde; z > hasta; z -= 4) zs.push(z);
  return (
    <>
      {zs.slice(0, Math.round(zs.length * p)).map((z) => (
        <mesh key={z} position={[x, 0.07, z - 1.2]}>
          <boxGeometry args={[ancho, 0.08, 2.4]} />
          <meshBasicMaterial color={C.magenta} />
        </mesh>
      ))}
    </>
  );
};

export const Pin: React.FC<{ x?: number; z: number; e?: number; tam?: number }> = ({ x = 0, z, e = 1, tam = 2.2 }) => {
  const frame = useCurrentFrame();
  const s = e * tam;
  return (
    <group position={[x, 4 + Math.sin(frame / 8 + z) * 0.4, z]} scale={[s, s, s]}>
      <mesh position={[0, 0.9, 0]}>
        <sphereGeometry args={[1, 16, 12]} />
        <meshStandardMaterial color={C.magenta} emissive={C.magenta} emissiveIntensity={0.4} />
      </mesh>
      <mesh position={[0, -0.4, 0]} rotation={[Math.PI, 0, 0]}>
        <coneGeometry args={[0.8, 1.8, 16]} />
        <meshStandardMaterial color={C.magenta} emissive={C.magenta} emissiveIntensity={0.4} />
      </mesh>
    </group>
  );
};

export const Viaje: React.FC<{ p: number; x?: number; desde?: number; hasta?: number }> = ({ p, x = 0, desde = Z_PIN, hasta = Z_DEST }) => {
  const largo = (desde - hasta) * p;
  if (largo < 0.1) return null;
  return (
    <mesh position={[x, 0.06, desde - largo / 2]}>
      <boxGeometry args={[1.6, 0.08, largo]} />
      <meshBasicMaterial color={C.cian} />
    </mesh>
  );
};

// Recogidas de otros viajes del día (se ven desde el dron)
const OTRAS: [number, number, number][] = [
  [-3.6, -56, -80],
  [3.6, -84, -104],
  [-3.6, -124, -150],
  [3.6, -150, -176],
  [-3.6, 4, -18],
];

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  const zc = zCarro(i, t);
  return (
    <>
      <Camara />
      <ambientLight intensity={0.9} />
      <directionalLight position={[20, 40, 10]} intensity={1.6} />
      <hemisphereLight args={["#9DB4FF", "#3B2257", 0.6]} />
      <fog attach="fog" args={["#7A4D8F", v === "dron" || v === "iso" ? 110 : 60, v === "dron" || v === "iso" ? 280 : 190]} />
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
          <meshBasicMaterial color="#E6E9FF" />
        </mesh>
      ))}
      <Edificios soloLado={v === "lateral" ? -1 : undefined} />

      {v === "detras" ? (
        <>
          <Viaje p={ap(frame, en(2, "cada") - 4, 24)} />
          <Guiones desde={zc - 3} hasta={Z_PIN} p={ap(frame, en(2, "recoger") - 4, 18)} />
          <Pin z={Z_PIN} e={ap(frame, en(2, "recoger") - 6, 10)} />
        </>
      ) : null}
      {v === "lateral" ? (
        <>
          <Guiones desde={4} hasta={Z_PIN} />
          <Viaje p={1} />
        </>
      ) : null}
      {v === "dron" ? (
        <>
          <Guiones desde={-19} hasta={Z_PIN} />
          <Pin z={Z_PIN} />
          <Viaje p={1} />
          {OTRAS.map(([x, z0, z1], k) => {
            const e = ap(frame, en(5, "ciento") - 4 + k * 5, 10);
            return (
              <React.Fragment key={k}>
                <Guiones x={x} desde={z0} hasta={z1} p={e} ancho={1.2} />
                <Pin x={x} z={z1} e={e} tam={1.6} />
              </React.Fragment>
            );
          })}
        </>
      ) : null}

      {v !== "dentro" && v !== "bomba" ? <Carro3D z={zc} color="#FFFFFF" /> : null}
      <Carro3D z={-100 + frame * 0.5} x={-3.6} color="#F2B544" />
      <Carro3D z={-170 + frame * 0.45} x={-3.6} color="#F06BA8" />
      <Carro3D z={-60 - frame * 0.3} x={3.6} color="#5B6CF0" />
      <Gasolinera />
    </>
  );
};

export const Caja: React.FC<{ color: string; children: React.ReactNode; tam?: number }> = ({ color, children, tam = 32 }) => (
  <div style={{ display: "inline-block", fontFamily: PX, fontSize: tam, color, border: `4px solid ${color}`, padding: "8px 18px 6px", background: "rgba(20,18,52,0.9)", boxShadow: `0 0 18px ${color}66`, letterSpacing: 1, whiteSpace: "nowrap" }}>
    {children}
  </div>
);

// Etiquetas neón apiladas arriba
export const Cajas: React.FC<{ items: { desde: number; color: string; texto: string }[] }> = ({ items }) => {
  const frame = useCurrentFrame();
  return (
    <div style={{ position: "absolute", top: 110, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 16, flexWrap: "wrap", padding: "0 40px" }}>
      {items.map((it, k) => {
        const a = ap(frame, it.desde, 8);
        return (
          <div key={k} style={{ opacity: a, transform: `scale(${1.25 - 0.25 * a})` }}>
            <Caja color={it.color} tam={30}>{it.texto}</Caja>
          </div>
        );
      })}
    </div>
  );
};

// Número gigante en letra pixel (con tachado opcional)
export const Grande: React.FC<{ desde: number; texto: string; color: string; etiqueta?: string; tachar?: number; top?: number }> = ({ desde, texto, color, etiqueta, tachar = 0, top = 260 }) => {
  const frame = useCurrentFrame();
  const a = ap(frame, desde, 8);
  return (
    <div style={{ position: "absolute", top, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", opacity: a, transform: `scale(${1.3 - 0.3 * a})` }}>
      <div style={{ position: "relative", fontFamily: PX, fontWeight: 700, fontSize: 140, lineHeight: 1, color, textShadow: "0 6px 0 #14102e, 0 0 30px rgba(0,0,0,0.5)" }}>
        {texto}
        {tachar > 0 ? <div style={{ position: "absolute", left: -10, top: "46%", height: 14, width: `calc(${tachar * 100}% + 20px)`, background: C.magenta }} /> : null}
      </div>
      {etiqueta ? <div style={{ fontFamily: PX, fontSize: 34, color: C.texto, marginTop: 14, textShadow: "0 3px 10px #000" }}>{etiqueta}</div> : null}
    </div>
  );
};

const PANTALLA_HOY: React.FC<{ tachar: number }> = ({ tachar }) => (
  <div style={{ position: "absolute", inset: 0, background: "#101413", fontFamily: F, color: "#FFFFFF", display: "flex", flexDirection: "column", alignItems: "center", paddingTop: 220 }}>
    <div style={{ fontSize: 40, fontWeight: 600, color: "#9AA3A0" }}>Ganancias de hoy</div>
    <div style={{ position: "relative", fontFamily: FD, fontWeight: 900, fontSize: 150, marginTop: 20 }}>
      $1,600.00
      <div style={{ position: "absolute", left: -10, top: "50%", height: 16, width: `calc(${tachar * 100}% + 20px)`, background: C.magenta, borderRadius: 8, transform: "rotate(-5deg)" }} />
    </div>
    <div style={{ fontSize: 36, fontWeight: 600, color: "#9AA3A0", marginTop: 30 }}>20 viajes · 12 h en línea</div>
    <div style={{ display: "flex", alignItems: "flex-end", gap: 22, height: 340, marginTop: 90 }}>
      {[40, 65, 30, 80, 55, 90, 70].map((h, k) => (
        <div key={k} style={{ width: 56, height: `${h}%`, background: "#3A4442", borderRadius: 10 }} />
      ))}
    </div>
  </div>
);

const Capas: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const t = local(frame, i);
  const v = VISTAS[i];
  if (v === "telefono") {
    const g = interpolate(t, [0.55, 0.95], [0, 1], suave);
    return (
      <>
        <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", opacity: 1 - interpolate(t, [0.75, 0.97], [0, 1], clamp) }}>
          <div style={{ transform: `scale(${interpolate(g, [0, 1], [1, 0.25])})` }}>
            <Telefono ancho={760} alto={1400}>
              <PANTALLA_HOY tachar={ap(frame, en(0, "lana"), 10)} />
            </Telefono>
          </div>
        </div>
        <Cajas
          items={[
            { desde: esc[0].voz, color: C.amarillo, texto: "12 HORAS AL VOLANTE" },
            { desde: en(0, "lana"), color: C.magenta, texto: "¿Y LA LANA?" },
          ]}
        />
      </>
    );
  }
  if (v === "iso") {
    return (
      <>
        <Cajas
          items={[
            { desde: en(1, "veinte"), color: C.cian, texto: "20 VIAJES" },
            { desde: en(1, "ochenta"), color: C.amarillo, texto: "× $80" },
          ]}
        />
        <Grande desde={en(1, "Mil")} texto="$1,600" color={C.amarillo} etiqueta="EN LA APP" />
        <div style={{ position: "absolute", top: 205, left: 0, right: 0, textAlign: "center", fontFamily: PX, fontSize: 24, color: C.tenue, textShadow: "0 2px 8px #000", opacity: ap(frame, esc[1].voz, 8) }}>
          NÚMEROS DE EJEMPLO
        </div>
      </>
    );
  }
  if (v === "detras") {
    return (
      <Cajas
        items={[
          { desde: en(2, "cada"), color: C.cian, texto: "8 KM · PAGADOS" },
          { desde: en(2, "seis"), color: C.magenta, texto: "6 KM · SIN PAGO" },
        ]}
      />
    );
  }
  if (v === "bomba") {
    const pesos = interpolate(frame, [en(3, "gasolina"), en(3, "veintiocho.") + 8], [0, 28], clamp);
    const quedan = frame >= en(3, "veintiocho.") + 10;
    return (
      <>
        <Cajas
          items={[
            { desde: esc[3].voz, color: C.amarillo, texto: "6 + 8 = 14 KM" },
            { desde: en(3, "gasolina"), color: C.magenta, texto: "× $2 POR KM" },
          ]}
        />
        {quedan ? (
          <Grande desde={en(3, "veintiocho.") + 10} texto="$52" color={C.magenta} etiqueta="TE QUEDAN" />
        ) : (
          <Grande desde={en(3, "ochenta,")} texto="$80" color={C.amarillo} etiqueta="LA OFERTA" tachar={ap(frame, en(3, "veintiocho."), 8)} />
        )}
        <div style={{ position: "absolute", left: 0, right: 0, top: 1130, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
          <div style={{ fontFamily: PX, fontWeight: 700, fontSize: 120, color: C.amarillo, background: "#0B1230", border: `6px solid ${C.amarillo}`, padding: "10px 40px", textShadow: `0 0 24px ${C.amarillo}88` }}>
            ${pesos.toFixed(2)}
          </div>
          <div style={{ fontFamily: PX, fontSize: 34, color: C.texto, textShadow: "0 3px 10px #000" }}>{(pesos / 24).toFixed(2)} LITROS</div>
        </div>
      </>
    );
  }
  if (v === "lateral") {
    const km = interpolate(t, [0.05, 0.85], [0, 280], suave);
    const marcas = Array.from({ length: 15 }, (_, k) => k * 20).filter((m) => Math.abs(m - km) < 70);
    return (
      <>
        <Cajas items={[{ desde: esc[4].voz, color: C.cian, texto: "280 KM EN EL DÍA" }]} />
        <Grande desde={en(4, "quinientos")} texto="$560" color={C.magenta} etiqueta="AL TANQUE" />
        <div style={{ position: "absolute", left: 0, right: 0, top: 1360, height: 120, borderTop: `4px solid ${C.cian}` }}>
          {marcas.map((m) => (
            <div key={m} style={{ position: "absolute", left: 540 + (m - km) * 9, top: 0, transform: "translateX(-50%)", display: "flex", flexDirection: "column", alignItems: "center" }}>
              <div style={{ width: 4, height: 40, background: C.cian }} />
              <div style={{ fontFamily: PX, fontSize: 26, color: C.cian, marginTop: 6, whiteSpace: "nowrap" }}>{m} KM</div>
            </div>
          ))}
          <div style={{ position: "absolute", left: 540, top: -70, transform: "translateX(-50%)", fontFamily: PX, fontWeight: 700, fontSize: 40, color: C.amarillo, background: "#0B1230", border: `4px solid ${C.amarillo}`, padding: "4px 14px" }}>
            {Math.round(km)} KM
          </div>
        </div>
      </>
    );
  }
  if (v === "dron") {
    const km = Math.round(interpolate(frame, [en(5, "ciento"), en(5, "kilómetros") + 6], [0, 120], clamp));
    return (
      <>
        <Cajas items={[{ desde: esc[5].voz, color: C.magenta, texto: "RECOGIDAS DEL DÍA" }]} />
        <Grande desde={en(5, "ciento")} texto={`${km} KM`} color={C.magenta} etiqueta={frame >= en(5, "gratis.") ? "MANEJADOS GRATIS" : "SIN PAGO"} />
      </>
    );
  }
  if (v === "dentro") {
    const cae = en(6, "dice");
    return (
      <>
        <div style={{ position: "absolute", left: 0, right: 0, top: 1180, bottom: 0, background: "#1A1430", borderTopLeftRadius: 300, borderTopRightRadius: 300, borderTop: "10px solid #2E2650" }} />
        <div style={{ position: "absolute", left: 120, top: 1260, width: 340, height: 340, borderRadius: "50%", border: "26px solid #2E2650" }} />
        <div style={{ position: "absolute", right: 70, top: 560, transform: "rotate(-4deg)" }}>
          <Telefono ancho={470} alto={830}>
            <Mapa />
            <div style={{ position: "absolute", left: 0, right: 0, bottom: 12, display: "flex", justifyContent: "center" }}>
              <Oferta app="App de viajes" tarifa="$80.00" tramos={["14 min (6.0 km)", "20 min (8.0 km)"]} ancho={430} />
            </div>
            <div style={{ position: "absolute", left: 0, right: 0, top: 40, display: "flex", justifyContent: "center", opacity: ap(frame, cae, 6), transform: `translateY(${interpolate(frame, [cae, cae + 8], [-80, 0], suave)}px)` }}>
              <Popup
                ancho={430}
                nivel="rojo"
                app="App"
                exacto
                neta="52.00"
                viaje="80.00"
                gas="28.00"
                fichas={[
                  { v: "$141/h", l: "meta $150", c: "mal" },
                  { v: "$5.71/km", l: "meta $8", c: "mal" },
                  { v: "34 min", l: "14.0 km total", c: "neutro" },
                ]}
              />
            </div>
          </Telefono>
        </div>
        <Cajas items={[{ desde: en(6, "segundo."), color: C.cian, texto: "EN UN SEGUNDO" }]} />
      </>
    );
  }
  // cierre
  return (
    <AbsoluteFill style={{ background: CIELO, opacity: ap(frame, esc[7].desde, 10), display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 46, paddingBottom: 300 }}>
      <div style={{ opacity: ap(frame, en(7, "checa"), 8) }}>
        <Caja color={C.cian} tam={50}>CHECA LOS NETOS</Caja>
      </div>
      <div style={{ opacity: ap(frame, en(7, "Pruébala"), 10) }}>
        <Logo tam={120} />
      </div>
      <div style={{ fontFamily: F, fontWeight: 700, fontSize: 42, color: C.texto, opacity: ap(frame, en(7, "Pruébala"), 10) }}>7 días gratis · link en el perfil</div>
      <div style={{ fontFamily: F, fontWeight: 800, fontSize: 56, color: C.amarillo, opacity: ap(frame, en(7, "¿Cuánto"), 8) }}>¿Cuánto te quedó hoy?</div>
    </AbsoluteFill>
  );
};

const CLAVE = ["lana", "veinte", "mil", "seiscientos", "seis", "veintiocho", "quinientos", "sesenta", "ciento", "gratis", "limpio", "netos"];
const limpia = (w: string) => w.toLowerCase().replace(/[^a-záéíóúñ]/g, "");

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const l = LINEAS[i];
  const t = (frame - esc[i].voz) / 30;
  if (t < -0.1 || t > l.dur + 0.3) return null;
  const grupos: [number, number][] = [];
  let ini = 0;
  l.palabras.forEach((p, k) => {
    if (/[.!?,:]$/.test(p.w) || k - ini === 3 || k === l.palabras.length - 1) {
      grupos.push([ini, k]);
      ini = k + 1;
    }
  });
  const g = [...grupos].reverse().find(([a]) => t >= l.palabras[a].s - 0.05) ?? grupos[0];
  return (
    <div style={{ position: "absolute", top: 1500, left: 60, right: 60, textAlign: "center", fontFamily: F, fontWeight: 800, fontSize: 56, color: C.texto, textShadow: "0 4px 14px rgba(0,0,0,0.9), 0 2px 0 #000" }}>
      {l.palabras.slice(g[0], g[1] + 1).map((p, k) => (
        <span key={k} style={{ color: CLAVE.includes(limpia(p.w)) ? C.amarillo : C.texto }}>
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

export const SinLana: React.FC = () => {
  const frame = useCurrentFrame();
  const i = escena(frame);
  const cierre = VISTAS[i] === "cierre";
  return (
    <AbsoluteFill style={{ background: CIELO }}>
      <ThreeCanvas width={1080} height={1920} camera={{ fov: 55, position: [3, 7, 15], near: 0.5, far: 500 }}>
        <Escena />
      </ThreeCanvas>
      {!cierre ? <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 480, background: "linear-gradient(180deg, rgba(20,18,52,0.8) 0%, rgba(20,18,52,0.45) 65%, rgba(20,18,52,0) 100%)" }} /> : null}
      <Capas />
      <div style={{ position: "absolute", top: 1420, left: 0, right: 0, height: 500, background: "linear-gradient(180deg, rgba(20,18,52,0) 0%, rgba(20,18,52,0.6) 40%)" }} />
      <Subs />

      {LINEAS.map((l, k) => (
        <Sequence key={l.archivo} from={esc[k].voz}>
          <Audio src={staticFile(`sinlana/${l.archivo}.mp3`)} />
        </Sequence>
      ))}
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 10, duracion - 40, duracion], [0, 0.14, 0.14, 0], clamp)} />
      {esc.slice(1).map((e, k) => (
        <Sfx key={k} en={e.desde} src="whoosh.wav" vol={0.08} />
      ))}
      <Sfx en={en(0, "lana")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(1, "Mil")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(3, "veintiocho.") + 10} src="pop.mp3" vol={0.15} />
      <Sfx en={en(4, "quinientos")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(5, "ciento")} src="pop.mp3" vol={0.15} />
      <Sfx en={en(6, "dice")} src="ding.wav" vol={0.15} />
      <Sfx en={en(7, "Pruébala")} src="ding.wav" vol={0.12} />
    </AbsoluteFill>
  );
};
