import React from "react";
import { interpolate, spring, useCurrentFrame, useVideoConfig } from "remotion";

// Paleta de la app (Tema.kt)
export const C = {
  fondo: "#0E1110",
  tarjeta: "#181C1A",
  tarjeta2: "#222826",
  borde: "#2E3532",
  texto: "#F2F5F3",
  texto2: "#B4BCB7",
  tenue: "#7D8781",
  verde: "#22C55E",
  amarillo: "#F5B70A",
  rojo: "#EF4444",
  oro: "#F5C542",
};

export const F = "Inter";
export const FD = "InterDisplay";

export type Nivel = "verde" | "amarillo" | "rojo";
const colorNivel = { verde: C.verde, amarillo: C.amarillo, rojo: C.rojo };
const accion = { verde: "ACEPTAR", amarillo: "CONSIDERAR", rojo: "RECHAZAR" };

export const useSpring = (desde: number, cfg: { damping?: number; mass?: number } = { damping: 12, mass: 0.6 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  return spring({ frame: frame - desde, fps, config: cfg });
};

// Fondo oscuro con un brillo verde que se mueve despacio
export const Fondo: React.FC<{ tono?: string }> = ({ tono = "rgba(34,197,94,0.16)" }) => {
  const frame = useCurrentFrame();
  const x = 50 + Math.sin(frame / 60) * 15;
  const y = 40 + Math.cos(frame / 80) * 10;
  return (
    <div
      style={{
        position: "absolute",
        inset: 0,
        background: `radial-gradient(circle at ${x}% ${y}%, ${tono}, transparent 55%), ${C.fondo}`,
      }}
    />
  );
};

// Subtítulos palabra por palabra. ^ = verde, ! = rojo, ~ = amarillo, * = dorado
export const Subtitulo: React.FC<{ texto: string; desde?: number; top?: number; tam?: number }> = ({
  texto,
  desde = 0,
  top = 150,
  tam = 92,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const palabras = texto.split(" ");
  return (
    <div
      style={{
        position: "absolute",
        top,
        left: 60,
        right: 60,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: "center",
        gap: "0 24px",
        fontFamily: FD,
        fontWeight: 900,
        fontSize: tam,
        lineHeight: 1.12,
        textAlign: "center",
      }}
    >
      {palabras.map((p, i) => {
        let color = C.texto;
        let t = p;
        const marca = p[0];
        if (marca === "^") color = C.verde;
        if (marca === "!") color = C.rojo;
        if (marca === "~") color = C.amarillo;
        if (marca === "*") color = C.oro;
        if ("^!~*".includes(marca)) t = p.slice(1);
        const s = spring({ frame: frame - desde - i * 3, fps, config: { damping: 11, mass: 0.5 } });
        return (
          <span
            key={i}
            style={{
              color,
              display: "inline-block",
              opacity: s,
              transform: `translateY(${(1 - s) * 30}px) scale(${0.7 + 0.3 * s})`,
              textShadow: "0 6px 24px rgba(0,0,0,0.6)",
              WebkitTextStroke: "2px rgba(0,0,0,0.35)",
            }}
          >
            {t}
          </span>
        );
      })}
    </div>
  );
};

export type Ficha = { v: string; l: string; c: "ok" | "mal" | "neutro" };

// Pop-up de Checa (estilo B2 compacto)
export const Popup: React.FC<{
  nivel: Nivel;
  app: string;
  neta: number;
  viaje: number;
  gas: number;
  fichas: Ficha[];
  multi?: string;
  oro?: boolean;
  apagada?: boolean;
  ancho?: number;
  resaltarGas?: number;
}> = ({ nivel, app, neta, viaje, gas, fichas, multi, oro, apagada, ancho = 860, resaltarGas = 0 }) => {
  const u = ancho / 860;
  const col = colorNivel[nivel];
  const txtBanda = nivel === "rojo" ? "#FFFFFF" : "#08130C";
  return (
    <div
      style={{
        width: ancho,
        borderRadius: 40 * u,
        background: C.tarjeta,
        overflow: "hidden",
        boxShadow: `0 ${24 * u}px ${60 * u}px rgba(0,0,0,0.6)`,
        border: oro ? `${10 * u}px solid ${C.oro}` : `${3 * u}px solid ${C.borde}`,
        fontFamily: F,
      }}
    >
      <div
        style={{
          height: 100 * u,
          background: col,
          opacity: apagada ? 0.5 : 1,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: `0 ${34 * u}px`,
          color: txtBanda,
        }}
      >
        <div style={{ fontSize: 34 * u, fontWeight: 700, width: 170 * u, display: "flex", alignItems: "center", gap: 8 * u }}>
          {oro ? <Estrella tam={36 * u} color={txtBanda} /> : null}
          {app}
        </div>
        <div style={{ fontSize: 50 * u, fontWeight: 800, letterSpacing: 1 }}>{accion[nivel]}</div>
        <div style={{ fontSize: 34 * u, fontWeight: 700, width: 170 * u, textAlign: "right" }}>{multi ?? ""}</div>
      </div>
      <div style={{ padding: `${34 * u}px ${30 * u}px ${30 * u}px`, textAlign: "center" }}>
        <div style={{ fontFamily: FD, fontWeight: 700, fontSize: 124 * u, color: C.texto, lineHeight: 1 }}>
          ${neta} netos
        </div>
        <div style={{ fontSize: 38 * u, fontWeight: 500, color: C.texto2, marginTop: 22 * u }}>
          Viaje ${viaje} ·{" "}
          <span
            style={{
              display: "inline-block",
              color: resaltarGas > 0 ? C.rojo : C.texto2,
              fontWeight: resaltarGas > 0 ? 800 : 500,
              transform: `scale(${1 + 0.25 * resaltarGas})`,
              transformOrigin: "left center",
              marginLeft: 6 * u,
            }}
          >
            Gasolina −${gas}
          </span>
        </div>
        <div style={{ display: "flex", gap: 20 * u, marginTop: 40 * u }}>
          {fichas.map((f, i) => {
            const c = f.c === "ok" ? C.verde : f.c === "mal" ? C.rojo : C.texto;
            return (
              <div
                key={i}
                style={{
                  flex: 1,
                  height: 156 * u,
                  borderRadius: 28 * u,
                  background: f.c === "mal" ? "#2A1818" : C.tarjeta2,
                  border: f.c === "mal" ? `${3 * u}px solid ${C.rojo}` : `${3 * u}px solid transparent`,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                }}
              >
                <div style={{ fontFamily: FD, fontWeight: 700, fontSize: 54 * u, color: c }}>{f.v}</div>
                <div style={{ fontSize: 28 * u, fontWeight: 500, color: C.texto2, marginTop: 4 * u }}>{f.l}</div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export const Estrella: React.FC<{ tam: number; color: string }> = ({ tam, color }) => (
  <svg width={tam} height={tam} viewBox="0 0 24 24">
    <path d="M12 2l2.9 6.6 7.1.6-5.4 4.7 1.6 7-6.2-3.7-6.2 3.7 1.6-7L2 9.2l7.1-.6z" fill={color} />
  </svg>
);

// Oferta genérica de una app de viajes (no imita ninguna marca)
export const Oferta: React.FC<{
  tarifa: string;
  tramos: [string, string];
  app: string;
  tachado?: number;
  zoom?: number;
  ancho?: number;
}> = ({ tarifa, tramos, app, tachado = 0, zoom = 1, ancho = 640 }) => {
  const u = ancho / 640;
  return (
    <div
      style={{
        width: ancho,
        background: "#FFFFFF",
        borderRadius: 36 * u,
        padding: `${30 * u}px ${36 * u}px ${36 * u}px`,
        fontFamily: F,
        color: "#111",
        boxShadow: "0 -10px 40px rgba(0,0,0,0.4)",
      }}
    >
      <div style={{ fontSize: 26 * u, fontWeight: 600, color: "#666" }}>{app} · Nueva solicitud</div>
      <div style={{ position: "relative", display: "inline-block", marginTop: 10 * u }}>
        <div
          style={{
            fontFamily: FD,
            fontWeight: 900,
            fontSize: 96 * u,
            transform: `scale(${zoom})`,
            transformOrigin: "left center",
          }}
        >
          {tarifa}
        </div>
        <div
          style={{
            position: "absolute",
            left: -6 * u,
            top: "52%",
            height: 10 * u,
            width: `${tachado * 108}%`,
            background: C.rojo,
            borderRadius: 6 * u,
            transform: "rotate(-6deg)",
          }}
        />
      </div>
      {tramos.map((t, i) => (
        <div key={i} style={{ display: "flex", alignItems: "center", gap: 16 * u, marginTop: 16 * u, fontSize: 32 * u, fontWeight: 600 }}>
          <div
            style={{
              width: 18 * u,
              height: 18 * u,
              borderRadius: i === 0 ? 9 * u : 3 * u,
              background: i === 0 ? "#111" : "#888",
            }}
          />
          {t}
        </div>
      ))}
      <div
        style={{
          marginTop: 26 * u,
          height: 88 * u,
          borderRadius: 20 * u,
          background: "#111",
          color: "#fff",
          fontSize: 34 * u,
          fontWeight: 700,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        Aceptar
      </div>
    </div>
  );
};

// Mapa simple de fondo dentro del teléfono
export const Mapa: React.FC = () => (
  <svg width="100%" height="100%" viewBox="0 0 700 1240" preserveAspectRatio="xMidYMid slice" style={{ position: "absolute", inset: 0 }}>
    <rect width="700" height="1240" fill="#1B2120" />
    {[120, 330, 560].map((x) => (
      <rect key={x} x={x} y={0} width={34} height={1240} fill="#262E2C" />
    ))}
    {[180, 470, 760, 1040].map((y) => (
      <rect key={y} x={0} y={y} width={700} height={30} fill="#262E2C" />
    ))}
    <path d="M60 1100 C 200 900, 300 700, 520 300" stroke="#3B82F6" strokeWidth={14} fill="none" strokeLinecap="round" opacity={0.8} />
    <circle cx={60} cy={1100} r={20} fill="#fff" />
    <circle cx={520} cy={300} r={20} fill="#3B82F6" />
  </svg>
);

export const Telefono: React.FC<{ children: React.ReactNode; ancho?: number; alto?: number }> = ({
  children,
  ancho = 700,
  alto = 1240,
}) => (
  <div
    style={{
      width: ancho,
      height: alto,
      borderRadius: 80,
      border: "14px solid #2A302E",
      background: "#000",
      overflow: "hidden",
      position: "relative",
      boxShadow: "0 40px 100px rgba(0,0,0,0.7)",
    }}
  >
    {children}
  </div>
);

// Semáforo de la marca
export const Semaforo: React.FC<{ alto: number; brillo?: number }> = ({ alto, brillo = 1 }) => {
  const w = alto * 0.57;
  return (
    <svg width={w} height={alto} viewBox="0 0 32 56" style={{ overflow: "visible" }}>
      <defs>
        <filter id="glow" x="-100%" y="-100%" width="300%" height="300%">
          <feGaussianBlur stdDeviation="4" />
        </filter>
      </defs>
      <rect x="0.5" y="0.5" width="31" height="55" rx="10" fill="#222826" stroke="#2E3532" />
      <circle cx="16" cy="12" r="7" fill="#4A2222" />
      <circle cx="16" cy="28" r="7" fill="#4A3C16" />
      <circle cx="16" cy="44" r="9" fill={C.verde} filter="url(#glow)" opacity={0.8 * brillo} />
      <circle cx="16" cy="44" r="7" fill={C.verde} />
      <circle cx="13.5" cy="41.5" r="2" fill="#86EFAC" />
    </svg>
  );
};

export const Logo: React.FC<{ tam?: number; brillo?: number }> = ({ tam = 200, brillo = 1 }) => (
  <div style={{ display: "flex", alignItems: "center", gap: tam * 0.18 }}>
    <Semaforo alto={tam * 1.2} brillo={brillo} />
    <div style={{ fontFamily: FD, fontWeight: 700, fontSize: tam, color: C.texto, letterSpacing: -2 }}>Checa</div>
  </div>
);

export const aparecer = (frame: number, desde: number, dur = 10) =>
  interpolate(frame, [desde, desde + dur], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
