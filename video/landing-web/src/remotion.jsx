// Escenas de Remotion que corren en vivo en la página con @remotion/player (sin render a mp4).
import React from "react";
import { createRoot } from "react-dom/client";
import { AbsoluteFill, useCurrentFrame, interpolate, Easing } from "remotion";
import { Player } from "@remotion/player";

const M = {
  asfalto: "#0C0C10", tablero: "#16161C", humo: "#3A3A45", hueso: "#F4F4EF", gris: "#A3A3AD",
  verde: "#22C55E", amarillo: "#FACC15", rojo: "#EF4444", sobreVerde: "#04210F",
  titulo: "'Bricolage Grotesque', 'Arial Black', sans-serif", texto: "'DM Sans', Arial, sans-serif", datos: "'JetBrains Mono', 'Courier New', monospace",
};
// Curva de la marca, sin rebote
const curva = Easing.bezier(0.2, 0.8, 0.2, 1);
const p = (f, inicio, dur) => interpolate(f, [inicio, inicio + dur], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp", easing: curva });
const lerp = (a, b, x) => a + (b - a) * x;
const dinero = (n) => "$" + Math.round(n).toLocaleString("es-MX");

/* 1 · El monto grande engaña: $95 se vacía hasta $57 */
const Bruto = () => {
  const f = useCurrentFrame();
  const entra = p(f, 0, 14);
  const corte = p(f, 30, 22);          // la gasolina se separa
  const cae = p(f, 52, 18);            // y se va
  const cuenta = p(f, 60, 26);         // el número baja
  const fin = p(f, 86, 12);
  const sale = p(f, 165, 14);
  const ancho = 840, neto = 57 / 95;
  return (
    <AbsoluteFill style={{ background: M.asfalto, color: M.hueso, fontFamily: M.texto, opacity: 1 - sale }}>
      <div style={{ position: "absolute", left: 60, top: 70, fontFamily: M.datos, fontWeight: 700, fontSize: 30, letterSpacing: 4, color: M.gris, opacity: entra }}>LA OFERTA DICE</div>
      <div style={{ position: "absolute", left: 56, top: 112, fontFamily: M.titulo, fontWeight: 800, fontSize: 170, letterSpacing: -6, lineHeight: 1, opacity: lerp(1, 0.55, fin) * entra, transform: `translateY(${(1 - entra) * 20}px)` }}>
        $95
        <div style={{ position: "absolute", left: -6, right: -6, top: "52%", height: 12, borderRadius: 6, background: M.rojo, transformOrigin: "left", transform: `scaleX(${fin})` }} />
      </div>
      {/* barra: lo que te queda (hueso) y la gasolina (rojo) */}
      <div style={{ position: "absolute", left: 60, top: 330, width: ancho, height: 110, borderRadius: 26, background: M.tablero, overflow: "hidden", opacity: entra }}>
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: ancho * neto, background: M.hueso, borderRadius: "26px 0 0 26px" }} />
        <div style={{
          position: "absolute", top: 0, bottom: 0, left: ancho * neto + corte * 14, width: ancho * (1 - neto),
          background: M.rojo, borderRadius: corte > 0.05 ? 22 : "0 26px 26px 0",
          transform: `translateY(${cae * 160}px) rotate(${cae * 8}deg)`, opacity: 1 - cae,
          display: "flex", alignItems: "center", justifyContent: "center", fontFamily: M.datos, fontWeight: 800, fontSize: 34, color: "#1F0000",
        }}>−$38</div>
      </div>
      <div style={{ position: "absolute", left: 60, top: 470, fontFamily: M.datos, fontWeight: 700, fontSize: 30, color: M.rojo, opacity: corte * (1 - fin * 0.4) }}>
        GASOLINA DE 19 KM · RECOGIDA INCLUIDA
      </div>
      <div style={{ position: "absolute", left: 60, top: 520, right: 60, display: "flex", alignItems: "baseline", gap: 20, opacity: p(f, 56, 10), transform: `translateY(${(1 - p(f, 56, 10)) * 16}px)` }}>
        <span style={{ fontFamily: M.datos, fontWeight: 700, fontSize: 32, letterSpacing: 4, color: M.gris }}>TE QUEDAN</span>
        <span style={{ fontFamily: M.titulo, fontWeight: 800, fontSize: 96, letterSpacing: -3, color: M.hueso, fontVariantNumeric: "tabular-nums" }}>{dinero(lerp(95, 57, cuenta))}</span>
        <span style={{ fontFamily: M.datos, fontWeight: 800, fontSize: 36, color: "#ff6b6b", opacity: fin }}>$5.0/km</span>
      </div>
      <div style={{ position: "absolute", left: 60, bottom: 30, fontFamily: M.datos, fontSize: 24, color: M.gris }}>Ejemplo · 12 km/L · $24/L</div>
    </AbsoluteFill>
  );
};

/* 2 · Así te avisa: el mismo aviso pasa por rojo, amarillo y verde (sin carrusel) */
const ESTADOS = [
  { c: M.rojo, t: "#1b0000", lbl: "RECHAZAR", neto: 57, h: "✕ $190/h", hok: false, km: "✕ $5.0/km", kok: false, pill: "Recogida 4.5 km", app: "Uber · $95.00", frase: "Recogida larga: rojo, y te dice por qué." },
  { c: M.amarillo, t: "#1b1500", lbl: "CONSIDERAR", neto: 56, h: "✓ $211/h", hok: true, km: "✕ $5.5/km", kok: false, pill: null, app: "DiDi · $88.00", frase: "Paga bien la hora, flojo el km: amarillo." },
  { c: M.verde, t: "#06210f", lbl: "ACEPTAR", neto: 97, h: "✓ $327/h", hok: true, km: "✓ $10.4/km", kok: true, pill: null, app: "Uber · $120.00", frase: "Cumple tu meta: verde, dale sin dudar." },
];
const TRAMO = 90;
const Avisa = () => {
  const f = useCurrentFrame();
  const i = Math.min(2, Math.floor(f / TRAMO)), g = f - i * TRAMO, e = ESTADOS[i];
  const entra = p(g, 0, 12), sale = i < 2 ? p(g, TRAMO - 10, 10) : p(f, 3 * TRAMO - 10, 10);
  const vis = entra * (1 - sale);
  const barra = 1 - interpolate(g, [12, TRAMO], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" });
  const vibra = i === 0 ? Math.sin(g * 2.2) * 7 * (1 - p(g, 12, 16)) * p(g, 6, 4) : 0;
  return (
    <AbsoluteFill style={{ background: M.asfalto, color: M.hueso, fontFamily: M.texto }}>
      <div style={{ position: "absolute", left: 50, right: 50, top: 46, fontFamily: M.titulo, fontWeight: 800, fontSize: 52, lineHeight: 1.08, letterSpacing: -1.5, opacity: vis, transform: `translateY(${(1 - entra) * 14}px)` }}>{e.frase}</div>
      <div style={{ position: "absolute", left: 50, right: 50, top: 230, borderRadius: 28, overflow: "hidden", border: `4px solid ${e.c}`, background: "#0C0C10", opacity: vis, transform: `translate(${vibra}px, ${(1 - entra) * -30}px) scale(${lerp(0.94, 1, entra)})`, boxShadow: "0 24px 50px -20px #000" }}>
        <div style={{ display: "grid", gridTemplateColumns: "42% 1fr" }}>
          <div style={{ background: e.c, color: e.t, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "26px 10px" }}>
            <div style={{ fontWeight: 800, fontSize: 30, letterSpacing: 2 }}>{e.lbl}</div>
            <div style={{ fontFamily: M.datos, fontWeight: 700, fontSize: 22, letterSpacing: 3, marginTop: 8, opacity: 0.8 }}>TE QUEDAN</div>
            <div style={{ fontFamily: M.titulo, fontWeight: 800, fontSize: 112, lineHeight: 1, letterSpacing: -4 }}>{dinero(lerp(0, e.neto, p(g, 4, 18)))}</div>
          </div>
          <div style={{ padding: "22px 24px", display: "flex", flexDirection: "column", justifyContent: "center", gap: 8 }}>
            <div style={{ fontFamily: M.datos, fontWeight: 800, fontSize: 44, color: e.hok ? M.verde : "#ff6b6b" }}>{e.h}</div>
            <div style={{ fontFamily: M.datos, fontWeight: 800, fontSize: 44, color: e.kok ? M.verde : "#ff6b6b" }}>{e.km}</div>
            <div style={{ fontFamily: M.datos, fontSize: 24, color: M.gris }}>{e.app}</div>
          </div>
        </div>
        {e.pill ? <div style={{ textAlign: "center", padding: 12, background: "#EF444418", borderTop: "2px solid #EF444466", fontFamily: M.datos, fontWeight: 800, fontSize: 28, color: "#ffd0d0" }}>{e.pill}</div> : null}
        <div style={{ height: 10, background: "#ffffff1a" }}><div style={{ height: "100%", width: `${barra * 100}%`, background: e.c }} /></div>
      </div>
      <div style={{ position: "absolute", left: 0, right: 0, bottom: 40, display: "flex", justifyContent: "center", gap: 14 }}>
        {ESTADOS.map((s, k) => <div key={k} style={{ width: k === i ? 44 : 14, height: 14, borderRadius: 7, background: k === i ? s.c : M.humo, transition: "none" }} />)}
      </div>
    </AbsoluteFill>
  );
};

/* 3 · Cierre: semáforo rojo → ámbar → verde → ✓ y «Descarga Checa» (de la pieza «La raya») */
const Semaforo = () => {
  const f = useCurrentFrame();
  const sem = p(f, 0, 14);
  const on = (a, b) => (f >= a && f < b ? 1 : 0);
  const rojo = on(14, 26), ambar = on(26, 38), verde = f >= 38 ? 1 : 0, paloma = p(f, 44, 8);
  const tit = p(f, 50, 14), sale = p(f, 170, 10);
  const ESC = 4.2;
  return (
    <AbsoluteFill style={{ background: M.asfalto, opacity: 1 - sale }}>
      <svg width={900} height={900} style={{ position: "absolute", left: 0, top: 0 }}>
        <g opacity={sem} transform={`translate(${450 - 30 * ESC} ${60 + (1 - sem) * 30}) scale(${ESC})`}>
          <rect x="6" y="2" width="48" height="96" rx="20" fill="#222826" stroke="#3A423F" strokeWidth="1.6" />
          <circle cx="30" cy="26" r="9" fill={rojo ? M.rojo : "#4A2222"} />
          <circle cx="30" cy="50" r="9" fill={ambar ? M.amarillo : "#4A3C16"} />
          <circle cx="30" cy="74" r="12" fill={verde ? M.verde : "#1d3a28"} />
          <path d="M24 74.5l4.5 4.5L36.5 69" fill="none" stroke={M.sobreVerde} strokeWidth="3.4" strokeLinecap="round" strokeLinejoin="round" opacity={paloma} />
        </g>
      </svg>
      <div style={{ position: "absolute", left: 0, right: 0, top: 560, textAlign: "center", fontFamily: M.titulo, fontWeight: 800, fontSize: 118, letterSpacing: -3, color: M.hueso, opacity: tit, transform: `translateY(${(1 - tit) * 22}px)` }}>Descarga Checa</div>
      <div style={{ position: "absolute", left: 0, right: 0, top: 710, textAlign: "center", fontFamily: M.texto, fontWeight: 600, fontSize: 40, color: M.gris, opacity: p(f, 60, 14) }}>Antes de aceptar, checa.</div>
    </AbsoluteFill>
  );
};

const ESCENAS = {
  bruto: { component: Bruto, durationInFrames: 180, compositionWidth: 960, compositionHeight: 720 },
  avisa: { component: Avisa, durationInFrames: 3 * TRAMO, compositionWidth: 960, compositionHeight: 720 },
  semaforo: { component: Semaforo, durationInFrames: 180, compositionWidth: 900, compositionHeight: 820 },
};

export function montarRemotion(quieto) {
  document.querySelectorAll("[data-remotion]").forEach((el) => {
    const e = ESCENAS[el.getAttribute("data-remotion")];
    if (!e) return;
    const ultimo = e.durationInFrames - 20;
    createRoot(el).render(
      <Player {...e} fps={30} autoPlay={!quieto} loop={!quieto} initialFrame={quieto ? Math.min(ultimo, 120) : 0}
        controls={false} clickToPlay={false} doubleClickToFullscreen={false} spaceKeyToPlayOrPause={false}
        acknowledgeRemotionLicense
        style={{ width: "100%", borderRadius: 22, overflow: "hidden" }} />
    );
  });
}
