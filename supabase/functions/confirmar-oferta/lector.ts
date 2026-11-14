// La IA solo lee los textos de la oferta; las cuentas se hacen aquí (13 nov 2026):
// horas → minutos, metros → km, recogida + viaje y, en Uber con una tarjeta, km contra «$/km est.».
// Probado con 55 ofertas donde la IA fallaba, 20 donde acertaba y los 32 casos de app/src/test/resources/ofertas.

export const LECTOR = `Lees ofertas de viaje de Uber y DiDi a partir de líneas de texto de la pantalla (OCR; puede confundir letras: l o I por 1, O por 0, B por 8, S por 5).
NO hagas cuentas: copia cada número tal como está escrito (solo corrige esas letras por dígitos). No sumes, no conviertas horas ni metros.
Devuelve un objeto json:
- oferta: true si hay un monto y al menos un tramo «N min … km/m», aunque el OCR lo haya roto. Líneas «$N/h» o «$N/km» sin «est.» y los montos enteros junto a ACEPTAR/CONSIDERAR/RECHAZAR son de otro pop-up: no cuentan.
- tarifa: el monto grande de la oferta. DiDi: si hay «Aceptar $X», es X; si no, el monto con multiplicador «$X 1.3x». No uses contraofertas, bonos («adicionales», «+$… por inicio», «de tarifa …»), el $/km ni montos de otras tarjetas.
- Varias tarjetas (lista de DiDi): usa la primera tarjeta completa de arriba abajo: el monto con centavos justo antes de sus dos tramos (o «Aceptar $X» justo después). Un monto sin centavos («$67») es de otro pop-up; dos tramos sin monto antes son una tarjeta cortada.
- multi: multiplicador «1.4x» (0 si no hay; ignora rangos «1.8X-2.4X»).
- tarjetas: cuántas ofertas distintas se ven (1 si solo una).
- recogida y viaje: los dos tramos de la MISMA tarjeta, en orden (el primero es ir por el pasajero). Cada uno {"h": horas o 0, "min": minutos escritos, "dist": número escrito, "unidad": "km" o "m"}. «1 h 12 min» → h 1, min 12. Copia los tramos AUNQUE ESTÉN ROTOS: «8 lmin •0.2 km» → min 1, dist 0.2; «2 8 min• 2.0 km» → min 8, dist 2 (un dígito suelto delante es un ícono); «6 minl.2 km» → min 6, dist 1.2. Solo sale de líneas con «min»; nunca uses el $/km como distancia. null solo si no hay ninguna línea con «min».
- resumen: si hay una línea «N min · X km · $Y/km est.» (total del viaje), {"h","min","dist","unidad"}; si no, null.
- porKmEst: el Y de «$Y/km est.» (0 si no hay).
Llena tarifa, recogida, viaje y porKmEst con lo que veas aunque oferta sea false.
Ejemplo: {"oferta": true, "tarifa": 103.26, "multi": 0, "tarjetas": 1, "recogida": {"h": 0, "min": 6, "dist": 1.9, "unidad": "km"}, "viaje": {"h": 0, "min": 37, "dist": 8.8, "unidad": "km"}, "resumen": null, "porKmEst": 9.65}`;

const tramoEsquema = {
  type: "object",
  properties: { h: { type: "number" }, min: { type: "number" }, dist: { type: "number" }, unidad: { type: "string", enum: ["km", "m"] } },
  required: ["h", "min", "dist", "unidad"],
  additionalProperties: false,
};

/** Esquema para Claude (DeepSeek usa el ejemplo de LECTOR). Un tramo en 0 = no se ve. */
export const ESQUEMA_LECTOR = {
  type: "object",
  properties: {
    oferta: { type: "boolean" }, tarifa: { type: "number" }, multi: { type: "number" }, tarjetas: { type: "number" },
    recogida: tramoEsquema, viaje: tramoEsquema, resumen: tramoEsquema, porKmEst: { type: "number" },
  },
  required: ["oferta", "tarifa", "multi", "tarjetas", "recogida", "viaje", "resumen", "porKmEst"],
  additionalProperties: false,
};

export type Lectura = { tarifa: number; km: number; min: number; recKm: number; recMin: number; multi: number };

const n = (v: unknown) => { const x = Number(v); return Number.isFinite(x) && x >= 0 ? x : 0; };

type Tramo = { km: number; min: number } | null;

/**
 * DiDi (14 nov, igual que Parser.sinIcono): el ícono del tramo se lee como un 4, 6 o 9 pegado a los
 * minutos («92min (654 m)» = 2 min). Solo si la velocidad es imposible (< 4 km/h) y sin ese dígito no.
 */
export function sinIcono(min: number, km: number): number {
  const s = String(Math.round(min));
  if (!Number.isInteger(min) || s.length < 2 || !"469".includes(s[0]) || km <= 0) return min;
  const m1 = Number(s.slice(1));
  if (m1 < 1) return min;
  const v0 = km * 60 / min, v1 = km * 60 / m1;
  return v0 < 4 && v1 <= 90 && (v1 >= 4 || m1 <= 3) ? m1 : min;
}

function tramo(t: unknown): Tramo {
  if (!t || typeof t !== "object") return null;
  const o = t as Record<string, unknown>;
  const d = n(o.dist);
  const mins = n(o.h) * 60 + n(o.min);
  if (d === 0 && mins === 0) return null;
  // Metros reales son enteros ≥ 30 («720 m»); «2.4 m» es un km mal leído (igual que Parser.kt).
  const esM = String(o.unidad ?? "km").toLowerCase() === "m" && Number.isInteger(d) && d >= 30;
  return { km: esM ? d / 1000 : d, min: mins };
}

/** De lo que leyó la IA a la oferta: todas las cuentas en código. */
export function calcular(datos: unknown, app: string): Lectura & { oferta: boolean } {
  const d = (datos && typeof datos === "object" ? datos : {}) as Record<string, unknown>;
  const cero = { oferta: false, tarifa: 0, km: 0, min: 0, recKm: 0, recMin: 0, multi: 0 };
  const tarifa = n(d.tarifa);
  const uber = /uber/i.test(app);
  const conIcono = (x: Tramo): Tramo => x && !uber ? { km: x.km, min: sinIcono(x.min, x.km) } : x;
  const rec = conIcono(tramo(d.recogida)), via = conIcono(tramo(d.viaje)), res = tramo(d.resumen);
  const p = n(d.porKmEst);
  const pValido = p >= 2 && p <= 80;
  // Uber con «$/km est.», un monto y al menos un tramo sí es oferta aunque el OCR esté roto.
  // DiDi con monto y los dos tramos completos también (14 nov: la revisión decía «no es oferta» en
  // 60 de 220 ofertas vigentes). Las ofertas vencidas o tomadas no llegan: el teléfono no las manda.
  const didiCompleta = !uber && tarifa > 0 && rec !== null && via !== null && rec.km > 0 && via.km > 0 && rec.min > 0 && via.min > 0;
  if (d.oferta !== true && !(uber && pValido && tarifa > 0 && (rec || via)) && !didiCompleta) return cero;
  let km = 0, min = 0, recKm = 0, recMin = 0;
  if (rec && via) { km = rec.km + via.km; min = rec.min + via.min; recKm = rec.km; recMin = rec.min; }
  else if (rec || via) { const x = (rec ?? via)!; km = x.km; min = x.min; }
  if (res && res.km > 0) { km = res.km; min = res.min; }
  // Solo Uber con una tarjeta: si los km no dan el «$/km est.», mandan tarifa ÷ $/km. DiDi no se toca.
  if (uber && n(d.tarjetas) <= 1 && !res && pValido && tarifa > 0) {
    const est = tarifa / p;
    if (Math.abs(km - est) > Math.max(0.3, est * 0.08) && recKm < est) km = Math.round(est * 10) / 10;
  }
  return { oferta: true, tarifa, km, min, recKm, recMin, multi: n(d.multi) };
}
