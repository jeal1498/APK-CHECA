import React, { useLayoutEffect, useMemo } from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { ThreeCanvas } from "@remotion/three";
import { useThree } from "@react-three/fiber";
import { F, Logo } from "./ui";
import { LINEAS } from "./lineasHibrido";

// Prueba de 6 s en 3D: cámara en tercera persona detrás del carro, calle con edificios de colores
// (como las escenas de @pixelmente) + etiquetas neón en letra pixel y número gigante.
export const DURACION_CALLE3D = 180;
const VOZ = 15;
const PX = "Silkscreen";
const C = {
  cian: "#22D3EE",
  magenta: "#EC4899",
  amarillo: "#FACC15",
  texto: "#FFFFFF",
  tenue: "#C9CFF0",
  calle: "#3A3F6E",
  banqueta: "#9AA3C7",
  edificios: ["#F2B544", "#5B6CF0", "#F06BA8", "#3BC9DB", "#9B7BF7", "#FF8A5B", "#7ED957"],
};
const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;
const ap = (f: number, d: number, dur = 12) => interpolate(f, [d, d + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
const en = (palabra: string) => VOZ + Math.round(LINEAS[0].palabras.find((p) => p.w === palabra)!.s * 30);

const T_PAGO = en("ocho");
const T_MANEJAS = en("manejas");
const T_CATORCE = en("catorce.");
const FIN = 140;
const Z_PIN = -42; // recogida: 6 km
const Z_DEST = -120; // viaje: 8 km más

const zCarro = (f: number) => -interpolate(f, [0, 150], [0, 34], clamp);

// Cámara: detrás del carro y luego sube para enseñar toda la ruta
const Camara: React.FC = () => {
  const frame = useCurrentFrame();
  const camera = useThree((s) => s.camera);
  const z = zCarro(frame);
  const g = interpolate(frame, [T_CATORCE - 4, T_CATORCE + 30], [0, 1], { ...clamp, easing: Easing.inOut(Easing.cubic) });
  const pos = [interpolate(g, [0, 1], [2.5, 22]), interpolate(g, [0, 1], [10, 105]), interpolate(g, [0, 1], [z + 14, 20])];
  const mira = [0, 0, interpolate(g, [0, 1], [z - 9, -55])];
  useLayoutEffect(() => {
    camera.position.set(pos[0], pos[1], pos[2]);
    camera.lookAt(mira[0], mira[1], mira[2]);
    camera.updateProjectionMatrix();
  });
  return null;
};

export const Edificios: React.FC<{ soloLado?: number }> = ({ soloLado }) => {
  const lista = useMemo(() => {
    const out: { x: number; z: number; w: number; d: number; h: number; c: string }[] = [];
    let k = 0;
    for (let z = 20; z > -200; z -= 14) {
      if (Math.abs((z + 200) % 42) < 1) continue; // calle transversal
      for (const lado of [-1, 1]) {
        const h = 6 + ((k * 37) % 17);
        out.push({ x: lado * (13 + (k % 3)), z, w: 10 + (k % 2) * 3, d: 11, h, c: C.edificios[(k * 3) % C.edificios.length] });
        k++;
      }
    }
    return out;
  }, []);
  return (
    <>
      {lista.filter((b) => !soloLado || Math.sign(b.x) === soloLado).map((b, i) => (
        <group key={i} position={[b.x, b.h / 2, b.z]}>
          <mesh>
            <boxGeometry args={[b.w, b.h, b.d]} />
            <meshStandardMaterial color={b.c} flatShading />
          </mesh>
          {/* ventanas hacia la calle */}
          {Array.from({ length: Math.floor(b.h / 3) }).map((_, r) =>
            [-1, 1].map((cz) => (
              <mesh key={`${r}${cz}`} position={[-Math.sign(b.x) * (b.w / 2 + 0.05), -b.h / 2 + 2 + r * 3, cz * 2.6]}>
                <boxGeometry args={[0.1, 1.2, 1.8]} />
                <meshBasicMaterial color="#FFE9A8" />
              </mesh>
            )),
          )}
        </group>
      ))}
    </>
  );
};

export const Carro3D: React.FC<{ z: number; color: string; x?: number }> = ({ z, color, x = 0 }) => (
  <group position={[x, 0, z]}>
    <mesh position={[0, 0.9, 0]}>
      <boxGeometry args={[2.6, 1.1, 4.6]} />
      <meshStandardMaterial color={color} flatShading />
    </mesh>
    <mesh position={[0, 1.8, 0.3]}>
      <boxGeometry args={[2.2, 0.9, 2.4]} />
      <meshStandardMaterial color="#1E2350" flatShading />
    </mesh>
    {[-1.1, 1.1].map((wx) =>
      [-1.5, 1.5].map((wz) => (
        <mesh key={`${wx}${wz}`} position={[wx * 1.05, 0.45, wz]}>
          <boxGeometry args={[0.5, 0.9, 0.9]} />
          <meshStandardMaterial color="#111" />
        </mesh>
      )),
    )}
    <mesh position={[0, 0.95, 2.35]}>
      <boxGeometry args={[2.0, 0.3, 0.1]} />
      <meshBasicMaterial color="#FF5A6E" />
    </mesh>
  </group>
);

const Ruta3D: React.FC = () => {
  const frame = useCurrentFrame();
  // Viaje pagado (cian, sólido) del pin al destino
  const pv = ap(frame, T_PAGO - 4, 24);
  const largoV = (Z_PIN - Z_DEST) * pv;
  // Recogida sin pago (magenta, punteada) del carro al pin
  const pr = ap(frame, T_MANEJAS - 4, 20);
  const guiones: number[] = [];
  for (let z = Math.floor(zCarro(T_MANEJAS)) - 3; z > Z_PIN; z -= 4) guiones.push(z);
  const visibles = Math.round(guiones.length * pr);
  return (
    <>
      {largoV > 0.1 ? (
        <mesh position={[0, 0.06, Z_PIN - largoV / 2]}>
          <boxGeometry args={[1.6, 0.08, largoV]} />
          <meshBasicMaterial color={C.cian} />
        </mesh>
      ) : null}
      {guiones.slice(0, visibles).map((z) => (
        <mesh key={z} position={[0, 0.07, z - 1.2]}>
          <boxGeometry args={[1.6, 0.08, 2.4]} />
          <meshBasicMaterial color={C.magenta} />
        </mesh>
      ))}
    </>
  );
};

const Pin3D: React.FC = () => {
  const frame = useCurrentFrame();
  const e = ap(frame, T_PAGO - 8, 12);
  const sube = Math.sin(frame / 8) * 0.4;
  return (
    <group position={[0, 4 + sube, Z_PIN]} scale={[e * 2.2, e * 2.2, e * 2.2]}>
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

const Escena: React.FC = () => {
  const frame = useCurrentFrame();
  const z = zCarro(frame);
  return (
    <>
      <Camara />
      <ambientLight intensity={0.9} />
      <directionalLight position={[20, 40, 10]} intensity={1.6} />
      <hemisphereLight args={["#9DB4FF", "#3B2257", 0.6]} />
      <fog attach="fog" args={["#7A4D8F", 60, 190]} />
      {/* piso: banquetas y calle principal */}
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
      <Edificios />
      <Ruta3D />
      <Pin3D />
      <mesh position={[0, 1.5, Z_DEST]} visible={ap(frame, T_PAGO + 14) > 0.5}>
        <boxGeometry args={[3, 3, 3]} />
        <meshStandardMaterial color={C.cian} emissive={C.cian} emissiveIntensity={0.5} />
      </mesh>
      <Carro3D z={z} color="#FFFFFF" />
      {/* otros conductores */}
      <Carro3D z={-100 + frame * 0.5} x={-3.6} color="#F2B544" />
      <Carro3D z={-170 + frame * 0.45} x={-3.6} color="#F06BA8" />
    </>
  );
};

const Caja: React.FC<{ color: string; children: React.ReactNode; tam?: number }> = ({ color, children, tam = 34 }) => (
  <div style={{ display: "inline-block", fontFamily: PX, fontSize: tam, color, border: `4px solid ${color}`, padding: "8px 18px 6px", background: "rgba(20,18,52,0.9)", boxShadow: `0 0 18px ${color}66`, letterSpacing: 1 }}>{children}</div>
);

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const t = (frame - VOZ) / 30;
  const ps = LINEAS[0].palabras;
  if (t < -0.1 || t > LINEAS[0].dur + 0.6 || frame >= FIN) return null;
  const grupo = t < ps[4].s - 0.05 ? ps.slice(0, 4) : ps.slice(4);
  const clave = ["ocho", "catorce."];
  return (
    <div style={{ position: "absolute", top: 1500, left: 60, right: 60, textAlign: "center", fontFamily: F, fontWeight: 800, fontSize: 52, color: C.texto, textShadow: "0 4px 14px rgba(0,0,0,0.9)" }}>
      {grupo.map((p, i) => (
        <span key={i} style={{ color: clave.includes(p.w) ? C.amarillo : C.texto }}>
          {p.w}{" "}
        </span>
      ))}
    </div>
  );
};

export const Calle3D: React.FC = () => {
  const frame = useCurrentFrame();
  const km = Math.round(interpolate(frame, [T_CATORCE, T_CATORCE + 16], [8, 14], clamp));
  const tachar = ap(frame, T_MANEJAS, 8);
  return (
    <AbsoluteFill style={{ background: "linear-gradient(180deg, #2B2A7A 0%, #8E4B9C 45%, #F08A5D 100%)" }}>
      <ThreeCanvas width={1080} height={1920} camera={{ fov: 55, position: [3, 7, 15], near: 0.5, far: 400 }}>
        <Escena />
      </ThreeCanvas>

      <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 450, background: "linear-gradient(180deg, rgba(20,18,52,0.8) 0%, rgba(20,18,52,0.45) 65%, rgba(20,18,52,0) 100%)", opacity: 1 - ap(frame, FIN, 10) }} />
      <div style={{ position: "absolute", top: 110, left: 0, right: 0, display: "flex", justifyContent: "center", opacity: ap(frame, 2, 10) * (1 - ap(frame, FIN, 10)) }}>
        <Caja color={C.amarillo}>OFERTA · $80</Caja>
      </div>
      <div style={{ position: "absolute", top: 210, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 16, opacity: 1 - ap(frame, FIN, 10) }}>
        <div style={{ opacity: ap(frame, T_PAGO + 10) }}>
          <Caja color={C.cian} tam={28}>8 KM · PAGADOS</Caja>
        </div>
        <div style={{ opacity: ap(frame, T_MANEJAS + 8) }}>
          <Caja color={C.magenta} tam={28}>6 KM · SIN PAGO</Caja>
        </div>
      </div>

      {/* número gigante con franja oscura */}
      <div style={{ position: "absolute", top: 300, left: 0, right: 0, display: "flex", flexDirection: "column", alignItems: "center", opacity: ap(frame, T_CATORCE - 2, 8) * (1 - ap(frame, FIN, 10)) }}>
        <div style={{ position: "relative", fontFamily: PX, fontWeight: 700, fontSize: 130, lineHeight: 1, color: frame >= T_CATORCE ? C.magenta : C.amarillo, textShadow: "0 6px 0 #14102e" }}>
          {km} KM
          {frame < T_CATORCE && tachar > 0 ? <div style={{ position: "absolute", left: -10, top: "48%", height: 12, width: `calc(${tachar * 100}% + 20px)`, background: C.magenta }} /> : null}
        </div>
        <div style={{ fontFamily: "FiraCode", fontSize: 32, color: C.tenue, marginTop: 10 }}>{frame >= T_CATORCE ? "los que manejas" : "los que te pagan"}</div>
      </div>

      {frame >= FIN ? (
        <AbsoluteFill style={{ background: "linear-gradient(180deg, #2B2A7A 0%, #8E4B9C 60%, #F08A5D 100%)", opacity: ap(frame, FIN, 10), alignItems: "center", justifyContent: "center", gap: 40, display: "flex", flexDirection: "column" }}>
          <Caja color={C.cian} tam={40}>CHECA CUENTA LOS 14</Caja>
          <Logo tam={110} />
        </AbsoluteFill>
      ) : null}

      <div style={{ position: "absolute", top: 1440, left: 0, right: 0, height: 480, background: "linear-gradient(180deg, rgba(20,18,52,0) 0%, rgba(20,18,52,0.6) 45%)" }} />
      <Subs />
      <Sequence from={VOZ}>
        <Audio src={staticFile("hibrido/l1.mp3")} />
      </Sequence>
      <Audio src={staticFile("musica-calma.mp3")} volume={(f) => interpolate(f, [0, 10, 160, 180], [0, 0.14, 0.14, 0], clamp)} />
      <Sequence from={T_PAGO - 4} durationInFrames={40}>
        <Audio src={staticFile("whoosh.wav")} volume={0.12} />
      </Sequence>
      <Sequence from={T_CATORCE} durationInFrames={40}>
        <Audio src={staticFile("pop.mp3")} volume={0.18} />
      </Sequence>
      <Sequence from={FIN} durationInFrames={40}>
        <Audio src={staticFile("ding.wav")} volume={0.15} />
      </Sequence>
    </AbsoluteFill>
  );
};
