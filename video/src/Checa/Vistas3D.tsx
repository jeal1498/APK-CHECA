import React, { useLayoutEffect } from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { useThree } from "@react-three/fiber";
import { CatmullRomCurve3, Vector3, type PerspectiveCamera } from "three";
import { F, FD, Logo, Mapa, Oferta, Popup, Telefono } from "./ui";
import { Carro3D, Edificios } from "./Calle3D";

// Muestrario de las 8 vistas propuestas (sin voz), ~2.2 s cada una, en la misma ciudad 3D.
const V = 66;
const N = 8;
export const DURACION_VISTAS = V * N + 45;
const PX = "Silkscreen";
const C = { cian: "#22D3EE", magenta: "#EC4899", amarillo: "#FACC15", texto: "#FFFFFF", calle: "#3A3F6E", banqueta: "#9AA3C7" };
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const suave = { ...clamp, easing: Easing.inOut(Easing.cubic) };
const NOMBRES = ["DETRÁS DEL CARRO", "DRON QUE SUBE", "ISOMÉTRICA FIJA", "DENTRO DEL CARRO", "DEL TELÉFONO A LA CALLE", "VISTA LATERAL", "MAPA DE CALOR", "DETALLE EN CÁMARA LENTA"];
const GS: [number, number, number] = [220, 0, 0]; // gasolinera, lejos de la ciudad
const Z_PIN = -42;
const Z_DEST = -120;

const vista = (f: number) => Math.min(N - 1, Math.floor(f / V));
const local = (f: number) => (f % V) / V;

const Camara: React.FC = () => {
  const frame = useCurrentFrame();
  const camera = useThree((s) => s.camera) as PerspectiveCamera;
  const v = vista(frame);
  const t = local(frame);
  let pos: number[] = [0, 10, 10];
  let mira: number[] = [0, 0, 0];
  let fov = 55;
  if (v === 0) {
    const z = -t * 14;
    pos = [2.5, 10, z + 14];
    mira = [0, 0, z - 9];
  } else if (v === 1) {
    const g = interpolate(t, [0, 0.9], [0, 1], suave);
    pos = [interpolate(g, [0, 1], [2.5, 22]), interpolate(g, [0, 1], [10, 105]), interpolate(g, [0, 1], [0, 20])];
    mira = [0, 0, interpolate(g, [0, 1], [-23, -55])];
  } else if (v === 2) {
    const a = Math.PI / 4 + t * 0.35;
    pos = [GS[0] + Math.cos(a) * 95, 80, GS[2] + Math.sin(a) * 95];
    mira = [GS[0], 2, GS[2]];
    fov = 24;
  } else if (v === 3) {
    const z = -t * 22;
    pos = [-0.6, 1.9, z];
    mira = [-0.2, 1.6, z - 20];
    fov = 62;
  } else if (v === 4) {
    const z = -t * 10;
    const g = interpolate(t, [0, 0.8], [0, 1], suave);
    pos = [2.5, interpolate(g, [0, 1], [40, 10]), z + interpolate(g, [0, 1], [4, 14])];
    mira = [0, 0, z - 9];
  } else if (v === 5) {
    const z = -t * 24;
    pos = [16, 3.5, z];
    mira = [0, 2.2, z];
    fov = 48;
  } else if (v === 6) {
    const a = t * 0.6;
    pos = [Math.sin(a) * 30, 130, -40 + Math.cos(a) * 30];
    mira = [0, 0, -60];
  } else {
    const k = interpolate(t, [0, 1], [0, 1], suave);
    pos = [GS[0] + 3 - k * 1.2, 3.2, GS[2] + 7 - k * 3];
    mira = [GS[0] - 3, 2.4, GS[2] + 1];
    fov = 40;
  }
  useLayoutEffect(() => {
    camera.fov = fov;
    camera.position.set(pos[0], pos[1], pos[2]);
    camera.lookAt(mira[0], mira[1], mira[2]);
    camera.updateProjectionMatrix();
  });
  return null;
};

// Manguera de la bomba magenta hasta el tanque del carro
const MANGUERA = new CatmullRomCurve3([new Vector3(-2.2, 2.0, 0.5), new Vector3(-1.4, 0.5, 1.4), new Vector3(-1.6, 0.6, 2.4), new Vector3(-2.0, 1.2, 2.75)]);

export const Gasolinera: React.FC = () => (
  <group position={GS}>
    <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.02, 0]}>
      <planeGeometry args={[120, 120]} />
      <meshStandardMaterial color="#E8B07A" />
    </mesh>
    <mesh position={[0, 0.15, 0]}>
      <boxGeometry args={[26, 0.3, 18]} />
      <meshStandardMaterial color="#B9C0DA" />
    </mesh>
    <mesh position={[0, 6.4, 0]}>
      <boxGeometry args={[20, 1, 12]} />
      <meshStandardMaterial color="#FFFFFF" />
    </mesh>
    <mesh position={[0, 5.8, 6.05]}>
      <boxGeometry args={[20, 0.5, 0.1]} />
      <meshBasicMaterial color={C.magenta} />
    </mesh>
    {[-8, 8].map((x) => (
      <mesh key={x} position={[x, 3, 0]}>
        <boxGeometry args={[0.8, 6, 0.8]} />
        <meshStandardMaterial color="#5B6CF0" />
      </mesh>
    ))}
    {[-3, 3].map((x, i) => (
      <group key={x} position={[x, 0, 0]}>
        <mesh position={[0, 1.6, 0]}>
          <boxGeometry args={[1.6, 3, 1]} />
          <meshStandardMaterial color={i ? C.cian : C.magenta} />
        </mesh>
        <mesh position={[0, 2.3, 0.52]}>
          <boxGeometry args={[1.2, 0.8, 0.05]} />
          <meshBasicMaterial color="#0B1230" />
        </mesh>
        <mesh position={[0, 2.4, 0.56]}>
          <boxGeometry args={[0.9, 0.3, 0.02]} />
          <meshBasicMaterial color={C.amarillo} />
        </mesh>
      </group>
    ))}
    <mesh>
      <tubeGeometry args={[MANGUERA, 32, 0.09, 8, false]} />
      <meshStandardMaterial color="#1A1430" />
    </mesh>
    <group position={[-3, 0, 4]} rotation={[0, Math.PI / 2, 0]}>
      <Carro3D z={0} color="#FFFFFF" />
    </group>
    <mesh position={[14, 6, -6]}>
      <boxGeometry args={[0.5, 12, 0.5]} />
      <meshStandardMaterial color="#3A3F6E" />
    </mesh>
    <mesh position={[14, 11.5, -6]}>
      <boxGeometry args={[4, 3, 0.4]} />
      <meshStandardMaterial color={C.amarillo} />
    </mesh>
    {[[-30, 20], [30, 25], [-28, -26], [26, -30]].map(([x, z], i) => (
      <mesh key={i} position={[x, 1.6, z]}>
        <coneGeometry args={[1.2, 3.2, 6]} />
        <meshStandardMaterial color="#3E9B57" flatShading />
      </mesh>
    ))}
  </group>
);

const Calor: React.FC = () => {
  const frame = useCurrentFrame();
  const puntos = [
    [-22, -20, C.magenta],
    [20, -50, C.amarillo],
    [-20, -80, C.cian],
    [22, -100, C.magenta],
    [0, -35, C.amarillo],
    [-24, -120, C.amarillo],
    [18, -10, C.cian],
  ] as const;
  return (
    <>
      {puntos.map(([x, z, c], i) => {
        const p = 1 + 0.25 * Math.sin(frame / 6 + i);
        return (
          <mesh key={i} position={[x, 26, z]} scale={[p, p, p]}>
            <sphereGeometry args={[5, 20, 14]} />
            <meshBasicMaterial color={c} transparent opacity={0.55} />
          </mesh>
        );
      })}
    </>
  );
};

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const v = vista(frame);
  const t = local(frame);
  const noche = v === 6;
  const zc = v === 0 ? -t * 14 : v === 1 ? -14 : v === 4 ? -t * 10 : v === 5 ? -t * 24 : -t * 22;
  const ruta = v === 1 ? interpolate(t, [0.1, 0.6], [0, 1], clamp) : v === 0 || v === 4 ? 1 : 0;
  const guiones: number[] = [];
  for (let z = zc - 3; z > Z_PIN; z -= 4) guiones.push(z);
  return (
    <>
      <Camara />
      <ambientLight intensity={noche ? 0.25 : 0.9} />
      <directionalLight position={[20, 40, 10]} intensity={noche ? 0.25 : 1.6} />
      <hemisphereLight args={[noche ? "#1B2550" : "#9DB4FF", "#3B2257", noche ? 0.3 : 0.6]} />
      <fog attach="fog" args={[noche ? "#0B1230" : "#7A4D8F", noche ? 90 : 60, noche ? 260 : 190]} />
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.01, -80]}>
        <planeGeometry args={[200, 320]} />
        <meshStandardMaterial color={C.banqueta} />
      </mesh>
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, 0, -80]}>
        <planeGeometry args={[14, 320]} />
        <meshStandardMaterial color={C.calle} />
      </mesh>
      {Array.from({ length: 40 }).map((_, i) => (
        <mesh key={i} rotation={[-Math.PI / 2, 0, 0]} position={[-4.2, 0.02, 20 - i * 6]}>
          <planeGeometry args={[0.3, 3]} />
          <meshBasicMaterial color="#E6E9FF" />
        </mesh>
      ))}
      <Edificios soloLado={v === 5 ? -1 : undefined} />
      {ruta > 0 ? (
        <>
          {guiones.map((z) => (
            <mesh key={z} position={[0, 0.07, z - 1.2]}>
              <boxGeometry args={[1.6, 0.08, 2.4]} />
              <meshBasicMaterial color={C.magenta} />
            </mesh>
          ))}
          <mesh position={[0, 0.06, Z_PIN - ((Z_PIN - Z_DEST) * ruta) / 2]}>
            <boxGeometry args={[1.6, 0.08, (Z_PIN - Z_DEST) * ruta]} />
            <meshBasicMaterial color={C.cian} />
          </mesh>
          <mesh position={[0, 5 + Math.sin(frame / 8) * 0.4, Z_PIN]}>
            <sphereGeometry args={[2, 16, 12]} />
            <meshStandardMaterial color={C.magenta} emissive={C.magenta} emissiveIntensity={0.4} />
          </mesh>
        </>
      ) : null}
      {v !== 3 ? <Carro3D z={zc} color="#FFFFFF" /> : null}
      <Carro3D z={-100 + frame * 0.5} x={-3.6} color="#F2B544" />
      <Carro3D z={-170 + frame * 0.45} x={-3.6} color="#F06BA8" />
      <Gasolinera />
      {noche ? <Calor /> : null}
    </>
  );
};

const Caja: React.FC<{ color: string; children: React.ReactNode; tam?: number }> = ({ color, children, tam = 32 }) => (
  <div style={{ display: "inline-block", fontFamily: PX, fontSize: tam, color, border: `4px solid ${color}`, padding: "8px 18px 6px", background: "rgba(20,18,52,0.9)", boxShadow: `0 0 18px ${color}66`, letterSpacing: 1 }}>{children}</div>
);

const PopupEj: React.FC<{ ancho: number }> = ({ ancho }) => (
  <Popup
    ancho={ancho}
    nivel="rojo"
    app="Uber"
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
);

// Capas HTML encima del 3D según la vista
const Capas: React.FC = () => {
  const frame = useCurrentFrame();
  const v = vista(frame);
  const t = local(frame);
  if (v === 3) {
    // Tablero con el teléfono en el soporte
    return (
      <>
        <div style={{ position: "absolute", left: 0, right: 0, top: 1180, bottom: 0, background: "#1A1430", borderTopLeftRadius: 300, borderTopRightRadius: 300, borderTop: "10px solid #2E2650" }} />
        <div style={{ position: "absolute", left: 140, top: 1300, width: 360, height: 360, borderRadius: "50%", border: "26px solid #2E2650" }} />
        <div style={{ position: "absolute", right: 90, top: 820, transform: `rotate(-4deg)` }}>
          <Telefono ancho={430} alto={760}>
            <Mapa />
            <div style={{ position: "absolute", left: 0, right: 0, bottom: 10, display: "flex", justifyContent: "center" }}>
              <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={390} />
            </div>
            <div style={{ position: "absolute", left: 0, right: 0, top: 30, display: "flex", justifyContent: "center", opacity: interpolate(t, [0.25, 0.4], [0, 1], clamp), transform: `translateY(${interpolate(t, [0.25, 0.4], [-60, 0], suave)}px)` }}>
              <PopupEj ancho={390} />
            </div>
          </Telefono>
        </div>
      </>
    );
  }
  if (v === 4) {
    const g = interpolate(t, [0, 0.65], [0, 1], suave);
    return (
      <div style={{ position: "absolute", left: 0, right: 0, top: 0, bottom: 0, display: "flex", alignItems: "center", justifyContent: "center", opacity: 1 - interpolate(t, [0.45, 0.7], [0, 1], clamp) }}>
        <div style={{ transform: `scale(${interpolate(g, [0, 1], [1, 0.25])})` }}>
          <Telefono ancho={760} alto={1400}>
            <Mapa />
            <div style={{ position: "absolute", left: 0, right: 0, bottom: 20, display: "flex", justifyContent: "center" }}>
              <Oferta app="App de viajes" tarifa="$80.00" tramos={["12 min (6.0 km)", "22 min (8.0 km)"]} ancho={700} />
            </div>
          </Telefono>
        </div>
      </div>
    );
  }
  if (v === 5) {
    // Regla de km abajo
    const desp = (t * 24 * 18) % 180;
    return (
      <div style={{ position: "absolute", left: 0, right: 0, top: 1300, height: 120, borderTop: `4px solid ${C.cian}` }}>
        {Array.from({ length: 9 }).map((_, i) => (
          <div key={i} style={{ position: "absolute", left: i * 180 - desp, top: 0 }}>
            <div style={{ width: 4, height: 40, background: C.cian }} />
            <div style={{ fontFamily: PX, fontSize: 26, color: C.cian, marginTop: 6 }}>{Math.floor((t * 24 * 18) / 180) + i} KM</div>
          </div>
        ))}
      </div>
    );
  }
  if (v === 7) {
    const pesos = interpolate(t, [0.1, 0.9], [0, 100], clamp);
    return (
      <div style={{ position: "absolute", left: 0, right: 0, top: 1150, display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
        <div style={{ fontFamily: PX, fontWeight: 700, fontSize: 130, color: C.amarillo, background: "#0B1230", border: `6px solid ${C.amarillo}`, padding: "10px 40px", textShadow: `0 0 24px ${C.amarillo}88` }}>${pesos.toFixed(2)}</div>
        <div style={{ fontFamily: PX, fontSize: 36, color: C.texto }}>{(pesos / 24).toFixed(2)} LITROS</div>
      </div>
    );
  }
  if (v === 6) {
    return (
      <div style={{ position: "absolute", left: 0, right: 0, top: 1200, display: "flex", justifyContent: "center", gap: 18 }}>
        <Caja color={C.amarillo} tam={28}>19:00 · $11/KM</Caja>
        <Caja color={C.magenta} tam={28}>15:00 · $6/KM</Caja>
      </div>
    );
  }
  return null;
};

export const Vistas3D: React.FC = () => {
  const frame = useCurrentFrame();
  const v = vista(frame);
  const fin = V * N;
  const t = local(frame);
  return (
    <AbsoluteFill style={{ background: v === 6 ? "#0B1230" : "linear-gradient(180deg, #2B2A7A 0%, #8E4B9C 45%, #F08A5D 100%)" }}>
      <ThreeCanvas width={1080} height={1920} camera={{ fov: 55, position: [3, 7, 15], near: 0.5, far: 500 }}>
        <Escena />
      </ThreeCanvas>
      <Capas />
      {frame < fin ? (
        <div style={{ position: "absolute", top: 110, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", gap: 14, opacity: interpolate(t, [0, 0.08], [0, 1], clamp) }}>
          <div style={{ fontFamily: PX, fontSize: 30, color: C.texto, textShadow: "0 3px 10px #000" }}>VISTA {v + 1} DE {N}</div>
          <Caja color={[C.cian, C.magenta, C.amarillo][v % 3]} tam={38}>{NOMBRES[v]}</Caja>
        </div>
      ) : (
        <AbsoluteFill style={{ background: "linear-gradient(180deg, #2B2A7A 0%, #8E4B9C 60%, #F08A5D 100%)", alignItems: "center", justifyContent: "center", display: "flex", flexDirection: "column", gap: 30 }}>
          <Logo tam={120} />
          <div style={{ fontFamily: F, fontWeight: 700, fontSize: 40, color: C.texto }}>8 vistas, una sola ciudad</div>
        </AbsoluteFill>
      )}
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 10, DURACION_VISTAS - 30, DURACION_VISTAS], [0, 0.2, 0.2, 0], clamp)} />
      {Array.from({ length: N }).map((_, i) => (
        <Sequence key={i} from={i * V} durationInFrames={30}>
          <Audio src={staticFile("whoosh.wav")} volume={0.1} />
        </Sequence>
      ))}
      <div style={{ display: "none", fontFamily: FD }} />
    </AbsoluteFill>
  );
};
