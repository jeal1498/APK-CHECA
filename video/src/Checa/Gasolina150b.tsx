import React from "react";
import { AbsoluteFill, Audio, Easing, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { Carro } from "./graficos";
import { F, FD, Logo, Popup } from "./ui";
import { LINEAS } from "./lineasGasolina150b";
import { Linea, armarEscenas, clamp, crearEn } from "./plantilla";

// «Tus $150 no son $150» v2 · estilo explicativo tranquilo (referencia de Eduardo):
// un solo mundo sin cortes (calle de noche con dos carros), marcador fijo abajo, 3 capítulos con pestañas y gráficas encima.
const { esc: ESC, duracion } = armarEscenas(LINEAS, 8, 10, 70);
export const DURACION_GASOLINA150B = duracion;
const en = crearEn(LINEAS, ESC);

const P = {
  cielo1: "#070C1F",
  cielo2: "#18234D",
  monte: "#0E1736",
  monte2: "#142045",
  calle: "#121625",
  linea: "#323A58",
  cian: "#22D3EE",
  morado: "#A855F7",
  luna: "#FCD34D",
  texto: "#E8ECF8",
  tenue: "#7C86A8",
  rojo: "#F43F5E",
};
const M = "FiraCode";
const XA = 270;
const XB = 810;

// 0 → 1 con salida suave
const ap = (frame: number, desde: number, dur = 14) =>
  interpolate(frame, [desde, desde + dur], [0, 1], { ...clamp, easing: Easing.out(Easing.cubic) });
// visible entre dos cuadros, con entrada y salida suaves
const vis = (frame: number, desde: number, hasta: number, dur = 14) => Math.min(ap(frame, desde, dur), 1 - ap(frame, hasta - dur, dur));

const CAP1 = ESC[3].desde;
const CAP2 = ESC[5].desde;
const CAP3 = ESC[7].desde;
const RECAP = ESC[9].desde;
const CIERRE = ESC[11].desde;

const Abs: React.FC<{ x?: number; y: number; ancho?: number; o?: number; style?: React.CSSProperties; children: React.ReactNode }> = ({ x, y, ancho, o = 1, style, children }) => (
  <div
    style={{
      position: "absolute",
      top: y,
      left: x === undefined ? 0 : x - (ancho ?? 0) / 2,
      width: x === undefined ? "100%" : ancho,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      opacity: o,
      ...style,
    }}
  >
    {children}
  </div>
);

const Etiqueta: React.FC<{ children: React.ReactNode; color?: string; tam?: number }> = ({ children, color = P.tenue, tam = 32 }) => (
  <div style={{ fontFamily: M, fontSize: tam, color, letterSpacing: 1, whiteSpace: "nowrap" }}>{children}</div>
);

// ───────── Mundo fijo ─────────
const Mundo: React.FC = () => {
  const frame = useCurrentFrame();
  const luna = 1 - 0.8 * vis(frame, CAP1, CIERRE, 20);
  const estrellas = React.useMemo(
    () => Array.from({ length: 70 }).map((_, i) => ({ x: (i * 197) % 1080, y: (i * 89) % 860, r: 1 + (i % 3) * 0.8, f: i * 0.7 })),
    [],
  );
  const dash = (frame * 3) % 120;
  return (
    <AbsoluteFill>
      <AbsoluteFill style={{ background: `linear-gradient(180deg, ${P.cielo1} 0%, ${P.cielo2} 55%, ${P.cielo2} 100%)` }} />
      <svg width={1080} height={1920} style={{ position: "absolute" }}>
        {estrellas.map((s, i) => (
          <circle key={i} cx={s.x} cy={s.y} r={s.r} fill="#fff" opacity={0.25 + 0.25 * Math.sin(frame / 20 + s.f)} />
        ))}
        <g opacity={luna}>
          <circle cx={880} cy={250} r={120} fill={P.luna} opacity={0.06} />
          <circle cx={880} cy={250} r={80} fill={P.luna} opacity={0.1} />
          <circle cx={880} cy={250} r={46} fill={P.luna} />
        </g>
        <path d="M0 900 L140 830 L260 870 L420 780 L560 860 L700 800 L860 870 L1080 790 L1080 1010 L0 1010 Z" fill={P.monte2} />
        <path d="M0 950 L180 900 L340 940 L520 880 L700 950 L900 905 L1080 940 L1080 1010 L0 1010 Z" fill={P.monte} />
        {/* postes y cable con luces que fluyen */}
        {[60, 540, 1020].map((x) => (
          <rect key={x} x={x - 5} y={760} width={10} height={250} fill="#3A2F2A" />
        ))}
        <path d="M0 790 Q300 830 540 790 Q780 830 1080 790" stroke="#2A3150" strokeWidth={3} fill="none" />
        {Array.from({ length: 36 }).map((_, i) => {
          const t = ((i / 36 + frame / 900) % 1) * 1080;
          const y = 790 + 40 * Math.sin((t % 540) / 540 * Math.PI) * 1;
          return <circle key={i} cx={t} cy={y} r={4} fill={i % 2 ? P.cian : P.morado} opacity={0.75} />;
        })}
      </svg>
      {/* calle */}
      <div style={{ position: "absolute", top: 1010, left: 0, right: 0, height: 170, background: P.calle }}>
        <div style={{ position: "absolute", top: 0, left: 0, right: 0, height: 6, background: "#1C2236" }} />
        <div style={{ position: "absolute", top: 120, left: -dash, display: "flex", gap: 60 }}>
          {Array.from({ length: 14 }).map((_, i) => (
            <div key={i} style={{ width: 60, height: 8, background: P.linea, borderRadius: 4 }} />
          ))}
        </div>
      </div>
      <div style={{ position: "absolute", top: 1180, left: 0, right: 0, bottom: 0, background: "#0A0D18" }} />
    </AbsoluteFill>
  );
};

const Carros: React.FC = () => {
  const frame = useCurrentFrame();
  const carro = (x: number, color: string, nombre: string, fase: number) => (
    <div style={{ position: "absolute", top: 905 + Math.sin(frame / 9 + fase) * 2, left: x - 190 }}>
      <Carro ancho={380} color={color} avance={frame * 4} />
      <div style={{ position: "absolute", top: 160, width: 380, textAlign: "center" }}>
        <Etiqueta color={color}>{nombre}</Etiqueta>
      </div>
    </div>
  );
  return (
    <>
      {carro(XA, P.cian, "Viaje A", 0)}
      {carro(XB, P.morado, "Viaje B", 2)}
    </>
  );
};

// ───────── Pestañas de capítulo ─────────
const Pestanas: React.FC = () => {
  const frame = useCurrentFrame();
  const caps = ["Kilómetros", "Recogida", "Tiempo"];
  const activa = frame >= CAP3 ? 2 : frame >= CAP2 ? 1 : frame >= CAP1 ? 0 : -1;
  const o = 1 - ap(frame, RECAP - 10, 14);
  return (
    <div style={{ position: "absolute", top: 80, left: 0, right: 0, display: "flex", justifyContent: "center", gap: 18, opacity: o }}>
      {caps.map((c, i) => {
        const e = ap(frame, en(2, "tres") + i * 6, 12);
        const on = i === activa;
        return (
          <div
            key={c}
            style={{
              fontFamily: M,
              fontSize: 30,
              color: on ? P.texto : P.tenue,
              border: `3px solid ${on ? P.cian : "#2C3558"}`,
              background: on ? "rgba(34,211,238,0.12)" : "rgba(10,14,30,0.6)",
              borderRadius: 999,
              padding: "10px 26px",
              opacity: e,
              transform: `translateY(${(1 - e) * -20}px)`,
              fontWeight: on ? 700 : 400,
            }}
          >
            {i + 1} {c}
          </div>
        );
      })}
    </div>
  );
};

// ───────── Marcador fijo ─────────
const Marcador: React.FC = () => {
  const frame = useCurrentFrame();
  const o = ap(frame, ESC[1].voz, 16) * (1 - ap(frame, CIERRE, 14));
  const a = Math.round(interpolate(frame, [en(1, "treinta,"), en(1, "treinta,") + 20], [150, 130], { ...clamp, easing: Easing.out(Easing.cubic) }));
  const b = Math.round(interpolate(frame, [en(1, "noventa."), en(1, "noventa.") + 20], [150, 90], { ...clamp, easing: Easing.out(Easing.cubic) }));
  const pulso = 1 + 0.06 * Math.sin(Math.min(1, Math.max(0, (frame - RECAP) / 20)) * Math.PI);
  const col = (x: number, v: number, color: string) => (
    <Abs x={x} y={1235} ancho={420}>
      <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 150, color: P.texto, lineHeight: 1, transform: `scale(${pulso})` }}>${v}</div>
      <div style={{ width: 220, height: 10, borderRadius: 5, background: "#1E2440", marginTop: 14, overflow: "hidden" }}>
        <div style={{ width: `${(v / 150) * 100}%`, height: "100%", background: color }} />
      </div>
      <div style={{ marginTop: 10 }}>
        <Etiqueta>te quedan</Etiqueta>
      </div>
    </Abs>
  );
  return (
    <div style={{ opacity: o }}>
      <Abs y={1198}>
        <Etiqueta tam={26}>Ejemplo · gasolina a $2 por km</Etiqueta>
      </Abs>
      {col(XA, a, P.cian)}
      {col(XB, b, P.morado)}
    </div>
  );
};

// ───────── Intro ─────────
const Intro: React.FC = () => {
  const frame = useCurrentFrame();
  const o = 1 - ap(frame, CAP1 - 12, 12);
  const tag = (x: number, d: number, color: string) => {
    const e = ap(frame, d, 16);
    return (
      <Abs x={x} y={720 + (1 - e) * -40} ancho={300} o={e}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 90, color: P.texto, border: `4px solid ${color}`, borderRadius: 24, padding: "4px 30px", background: "rgba(7,12,31,0.7)" }}>
          $150
        </div>
      </Abs>
    );
  };
  const q = vis(frame, en(2, "¿Cuál"), en(2, "tres") + 4);
  return (
    <div style={{ opacity: o }}>
      <Abs y={300} o={ap(frame, 0, 10) * (1 - ap(frame, en(2, "¿Cuál") - 6, 10))}>
        <Etiqueta tam={40} color={P.texto}>
          2 viajes · mismo carro
        </Etiqueta>
        <div style={{ height: 14 }} />
        <Etiqueta tam={34}>misma gasolina</Etiqueta>
      </Abs>
      {tag(XA, 2, P.cian)}
      {tag(XB, en(0, "ciento"), P.morado)}
      <Abs y={330} o={q}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 200, color: P.luna, lineHeight: 1 }}>?</div>
        <Etiqueta tam={36} color={P.texto}>
          3 cosas que la oferta no dice
        </Etiqueta>
      </Abs>
    </div>
  );
};

// ───────── Capítulo 1 · Kilómetros ─────────
const Bombita: React.FC<{ tam: number }> = ({ tam }) => (
  <svg width={tam} height={tam * 1.3} viewBox="0 0 100 130">
    <rect x={10} y={10} width={60} height={110} rx={10} fill="#1F2A55" stroke={P.cian} strokeWidth={3} />
    <rect x={20} y={22} width={40} height={26} rx={4} fill="#0A0F24" />
    <path d="M70 40 L86 50 L86 100 Q86 110 78 108" stroke={P.cian} strokeWidth={4} fill="none" />
    <rect x={4} y={118} width={72} height={8} rx={3} fill="#1F2A55" />
  </svg>
);

const Cap1: React.FC = () => {
  const frame = useCurrentFrame();
  const o1 = vis(frame, CAP1 + 4, ESC[4].voz);
  const base = 880;
  const barra = (x: number, km: number, dKm: number, dGas: number, gas: number, color: string) => {
    const h = 15 * km * ap(frame, dKm, 22);
    const g = ap(frame, dGas, 12);
    return (
      <div key={x}>
        <div style={{ position: "absolute", left: x - 60, top: base - h, width: 120, height: h, background: `linear-gradient(180deg, ${color}, ${color}55)`, borderRadius: "14px 14px 0 0" }} />
        <Abs x={x} y={base - h - 120} ancho={320} o={ap(frame, dKm + 10, 10)}>
          <Etiqueta tam={40} color={P.texto}>
            {km} km
          </Etiqueta>
          <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 64, color: P.luna, opacity: g, transform: `scale(${0.8 + 0.2 * g})` }}>−${gas}</div>
        </Abs>
      </div>
    );
  };
  const o2 = vis(frame, ESC[4].desde, CAP2);
  return (
    <>
      <Abs y={260} o={o1}>
        <div style={{ display: "flex", alignItems: "center", gap: 40 }}>
          <Bombita tam={170} />
          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div style={{ opacity: ap(frame, en(3, "veinticuatro")) }}>
              <Etiqueta tam={40} color={P.texto}>
                Litro · $24
              </Etiqueta>
            </div>
            <div style={{ opacity: ap(frame, en(3, "doce,")) }}>
              <Etiqueta tam={40} color={P.texto}>
                Rinde · 12 km/l
              </Etiqueta>
            </div>
          </div>
        </div>
        <div style={{ marginTop: 40, opacity: ap(frame, en(3, "dos")), transform: `scale(${0.9 + 0.1 * ap(frame, en(3, "dos"))})` }}>
          <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 120, color: P.luna }}>
            $2 <span style={{ fontSize: 60, color: P.texto }}>por km</span>
          </div>
        </div>
      </Abs>
      <div style={{ opacity: o2 }}>
        {barra(XA, 10, en(4, "diez"), en(4, "veinte"), 20, P.cian)}
        {barra(XB, 30, en(4, "treinta:"), en(4, "sesenta."), 60, P.morado)}
      </div>
    </>
  );
};

// ───────── Capítulo 2 · Recogida ─────────
const Ruta: React.FC<{ y: number; rec: number; via: number; dRec: number; color: string; nombre: string }> = ({ y, rec, via, dRec, color, nombre }) => {
  const frame = useCurrentFrame();
  const k = 28;
  const x0 = 120;
  const r = rec * k * ap(frame, dRec, 16);
  const v = via * k * ap(frame, dRec + 16, 24);
  const sinPago = ap(frame, en(6, "nadie"), 12);
  const gas = ap(frame, en(6, "gasolina"), 12);
  return (
    <>
      <div style={{ position: "absolute", top: y - 60, left: x0 }}>
        <Etiqueta color={color}>{nombre}</Etiqueta>
      </div>
      <svg width={1080} height={200} style={{ position: "absolute", top: y - 20, left: 0, overflow: "visible" }}>
        <line x1={x0} y1={20} x2={x0 + 30 * k} y2={20} stroke="#222A48" strokeWidth={12} strokeLinecap="round" />
        <line x1={x0} y1={20} x2={x0 + r} y2={20} stroke={P.rojo} strokeWidth={10} strokeDasharray="14 12" />
        <line x1={x0 + rec * k} y1={20} x2={x0 + rec * k + v} y2={20} stroke={color} strokeWidth={12} strokeLinecap="round" opacity={v > 0 ? 1 : 0} />
        <circle cx={x0} cy={20} r={14} fill={P.texto} />
        {r >= rec * k - 1 ? <circle cx={x0 + rec * k} cy={20} r={16} fill={color} stroke={P.texto} strokeWidth={4} /> : null}
      </svg>
      <div style={{ position: "absolute", top: y + 22, left: x0 - 20, opacity: ap(frame, dRec, 10) }}>
        <Etiqueta tam={32} color={P.texto}>
          recogida {rec} km
        </Etiqueta>
        <div style={{ opacity: sinPago, marginTop: 6 }}>
          <Etiqueta tam={30} color={P.rojo}>
            sin pago · <span style={{ color: P.luna, opacity: gas }}>−${rec * 2} gasolina</span>
          </Etiqueta>
        </div>
      </div>
    </>
  );
};

const Cap2: React.FC = () => {
  const frame = useCurrentFrame();
  const o = vis(frame, CAP2 + 4, CAP3);
  return (
    <div style={{ opacity: o }}>
      <Abs y={200} o={ap(frame, ESC[5].voz + 10)}>
        <Etiqueta tam={34} color={P.texto}>
          Antes de que suba el pasajero…
        </Etiqueta>
      </Abs>
      <Ruta y={380} rec={1} via={9} dRec={en(6, "un")} color={P.cian} nombre="A" />
      <Ruta y={620} rec={5} via={25} dRec={en(6, "cinco.")} color={P.morado} nombre="B" />
    </div>
  );
};

// ───────── Capítulo 3 · Tiempo ─────────
const Reloj: React.FC<{ x: number; min: number; d: number; color: string }> = ({ x, min, d, color }) => {
  const frame = useCurrentFrame();
  const R = 130;
  const c = 2 * Math.PI * R;
  const p = (min / 60) * ap(frame, d, 26);
  return (
    <Abs x={x} y={330} ancho={360} o={ap(frame, CAP3 + 6)}>
      <div style={{ position: "relative", width: 320, height: 320 }}>
        <svg width={320} height={320} viewBox="0 0 320 320">
          <circle cx={160} cy={160} r={R} stroke="#222A48" strokeWidth={22} fill="none" />
          <circle cx={160} cy={160} r={R} stroke={color} strokeWidth={22} fill="none" strokeDasharray={`${c * p} ${c}`} transform="rotate(-90 160 160)" strokeLinecap="round" />
        </svg>
        <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
          <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 86, color: P.texto, lineHeight: 1 }}>{Math.round(p * 60)}</div>
          <Etiqueta>min</Etiqueta>
        </div>
      </div>
    </Abs>
  );
};

const Cap3: React.FC = () => {
  const frame = useCurrentFrame();
  const o1 = vis(frame, CAP3 + 4, ESC[8].voz);
  const o2 = vis(frame, ESC[8].desde, RECAP);
  const fila = (y: number, v: number, d: number, color: string, nombre: string) => (
    <div style={{ position: "absolute", top: y, left: 120 }}>
      <Etiqueta color={color}>{nombre}</Etiqueta>
      <div style={{ display: "flex", alignItems: "center", gap: 20, marginTop: 10 }}>
        <div style={{ width: (v / 390) * 600 * ap(frame, d, 24), height: 56, background: color, borderRadius: 12 }} />
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 64, color: P.texto, opacity: ap(frame, d + 10) }}>
          ${v}
          <span style={{ fontFamily: M, fontWeight: 400, fontSize: 32, color: P.tenue }}> /hora</span>
        </div>
      </div>
    </div>
  );
  const x36 = ap(frame, en(8, "tres"), 16);
  return (
    <>
      <div style={{ opacity: o1 }}>
        <Reloj x={XA} min={20} d={en(7, "veinte")} color={P.cian} />
        <Reloj x={XB} min={50} d={en(7, "cincuenta.")} color={P.morado} />
      </div>
      <div style={{ opacity: o2 }}>
        <Abs y={200} o={ap(frame, ESC[8].voz)}>
          <Etiqueta tam={34} color={P.texto}>
            Por hora, ya sin gasolina
          </Etiqueta>
        </Abs>
        {fila(300, 390, en(8, "cuatrocientos"), P.cian, "A")}
        {fila(470, 108, en(8, "cien."), P.morado, "B")}
        <Abs y={680} o={x36}>
          <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 110, color: P.luna, transform: `scale(${0.85 + 0.15 * x36})` }}>3,6 veces</div>
        </Abs>
      </div>
    </>
  );
};

// ───────── Repaso y Checa ─────────
const Repaso: React.FC = () => {
  const frame = useCurrentFrame();
  const o = vis(frame, RECAP + 6, ESC[10].voz);
  const nota = (x: number, d: number, l1: string, l2: string, color: string) => (
    <Abs x={x} y={600} ancho={480} o={ap(frame, d)}>
      <div style={{ border: `3px solid ${color}`, borderRadius: 20, padding: "18px 26px", background: "rgba(7,12,31,0.75)", display: "flex", flexDirection: "column", alignItems: "center", gap: 8 }}>
        <Etiqueta tam={34} color={P.texto}>
          {l1}
        </Etiqueta>
        <Etiqueta tam={30}>{l2}</Etiqueta>
      </div>
    </Abs>
  );
  return (
    <div style={{ opacity: o }}>
      <Abs y={300} o={ap(frame, RECAP + 6)}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 84, color: P.texto, textAlign: "center", lineHeight: 1.1 }}>
          Misma tarifa,
          <br />
          <span style={{ color: P.luna }}>otra cuenta</span>
        </div>
      </Abs>
      {nota(XA, en(9, "corto"), "corto", "pasajero cerca", P.cian)}
      {nota(XB, en(9, "largo"), "largo", "recogida lejos", P.morado)}
    </div>
  );
};

const Checa: React.FC = () => {
  const frame = useCurrentFrame();
  const o = vis(frame, ESC[10].voz, CIERRE);
  const pop = (x: number, d: number, children: React.ReactNode) => {
    const e = ap(frame, d, 18);
    return (
      <div style={{ position: "absolute", top: 420 + (1 - e) * -60, left: x - 235, opacity: e }}>
        {children}
      </div>
    );
  };
  return (
    <div style={{ opacity: o }}>
      <Abs y={220} o={ap(frame, ESC[10].voz)}>
        <Etiqueta tam={34} color={P.texto}>
          Mira km totales y tiempo
        </Etiqueta>
      </Abs>
      {pop(
        XA,
        en(10, "Checa"),
        <Popup
          ancho={470}
          nivel="verde"
          app="Viaje A"
          neta={130}
          viaje={150}
          gas={20}
          fichas={[
            { v: "$450", l: "por hora", c: "ok" },
            { v: "$15", l: "por km", c: "ok" },
            { v: "20 min", l: "10 km total", c: "neutro" },
          ]}
        />,
      )}
      {pop(
        XB,
        en(10, "Checa") + 8,
        <Popup
          ancho={470}
          nivel="amarillo"
          app="Viaje B"
          neta={90}
          viaje={150}
          gas={60}
          fichas={[
            { v: "$180", l: "por hora", c: "ok" },
            { v: "$5", l: "por km", c: "mal" },
            { v: "50 min", l: "30 km total", c: "neutro" },
          ]}
        />,
      )}
    </div>
  );
};

const Cierre: React.FC = () => {
  const frame = useCurrentFrame();
  const o = ap(frame, CIERRE, 18);
  if (o <= 0) return null;
  return (
    <AbsoluteFill style={{ background: `rgba(5,8,20,${0.85 * o})` }}>
      <Abs y={560} o={ap(frame, CIERRE + 6, 18)}>
        <Logo tam={150} />
      </Abs>
      <Abs y={820} o={ap(frame, en(11, "siete"))}>
        <div style={{ fontFamily: F, fontWeight: 800, fontSize: 50, color: "#08130C", background: "#22C55E", borderRadius: 999, padding: "14px 36px" }}>7 días gratis · link en el perfil</div>
      </Abs>
      <Abs y={1000} o={ap(frame, en(11, "¿Tú"))}>
        <div style={{ fontFamily: FD, fontWeight: 900, fontSize: 80, color: P.luna, textAlign: "center", lineHeight: 1.1 }}>
          ¿Tú en qué te
          <br />
          fijas primero?
        </div>
      </Abs>
    </AbsoluteFill>
  );
};

// ───────── Subtítulos discretos ─────────
const grupos = (l: Linea) => {
  const g: { i: number; f: number }[] = [];
  let ini = 0;
  l.palabras.forEach((p, i) => {
    if (/[.!?…,:]$/.test(p.w) || i - ini === 4 || i === l.palabras.length - 1) {
      g.push({ i: ini, f: i });
      ini = i + 1;
    }
  });
  return g;
};

const Subs: React.FC = () => {
  const frame = useCurrentFrame();
  const k = ESC.findIndex((e) => frame >= e.desde && frame < e.hasta);
  if (k < 0) return null;
  const l = LINEAS[k];
  const t = (frame - ESC[k].voz) / 30;
  if (t < 0 || t > l.dur + 0.3) return null;
  const gs = grupos(l);
  const g = gs.find((x) => t < l.palabras[x.f].e + 0.15) ?? gs[gs.length - 1];
  return (
    <div style={{ position: "absolute", top: frame >= CIERRE ? 1440 : 1500, left: 60, right: 60, textAlign: "center", fontFamily: F, fontWeight: 600, fontSize: 42, color: P.texto, textShadow: "0 3px 12px rgba(0,0,0,0.9)" }}>
      {l.palabras
        .slice(g.i, g.f + 1)
        .map((p) => p.w)
        .join(" ")}
    </div>
  );
};

// ───────── Video ─────────
const SFX: { f: number; src: string; vol: number }[] = [
  { f: en(2, "tres"), src: "whoosh.wav", vol: 0.12 },
  { f: CAP1, src: "whoosh.wav", vol: 0.12 },
  { f: CAP2, src: "whoosh.wav", vol: 0.12 },
  { f: CAP3, src: "whoosh.wav", vol: 0.12 },
  { f: en(8, "tres"), src: "ding.wav", vol: 0.15 },
  { f: en(10, "Checa"), src: "recogida2/pop.mp3", vol: 0.18 },
  { f: CIERRE, src: "ding.wav", vol: 0.15 },
];

export const Gasolina150b: React.FC = () => {
  const frame = useCurrentFrame();
  const zoom = interpolate(frame, [0, duracion], [1, 1.05]) + 0.015 * Math.sin(frame / 90);
  return (
    <AbsoluteFill style={{ background: P.cielo1, overflow: "hidden" }}>
      <AbsoluteFill style={{ transform: `scale(${zoom})`, transformOrigin: "50% 55%" }}>
        <Mundo />
        <Carros />
        <Intro />
        <Cap1 />
        <Cap2 />
        <Cap3 />
        <Repaso />
        <Checa />
        <Marcador />
        <Pestanas />
      </AbsoluteFill>
      <Cierre />
      <Subs />
      <Audio
        src={staticFile("gasolina150b/musica.mp3")}
        volume={(f) => interpolate(f, [0, 20, duracion - 50, duracion], [0, 0.14, 0.14, 0], clamp)}
      />
      {ESC.map((e, i) => (
        <Sequence key={i} from={e.voz}>
          <Audio src={staticFile(`gasolina150b/${LINEAS[i].archivo}.mp3`)} />
        </Sequence>
      ))}
      {SFX.map((s, i) => (
        <Sequence key={`s${i}`} from={s.f} durationInFrames={60}>
          <Audio src={staticFile(s.src)} volume={s.vol} />
        </Sequence>
      ))}
    </AbsoluteFill>
  );
};
