// Lectura de la oferta con la IMAGEN (DeepSeek, visión). Funciones puras: sin red ni Deno, para probarlas con node:test.
// Lo que devuelve el modelo tiene la misma forma que el de texto (tramos, tarifa, multi…); las cuentas siguen en lector.ts.

export const LECTOR_IMAGEN = `Lees una captura de pantalla de una oferta de viaje de Uber o DiDi en un teléfono Android.
Mira la imagen y devuelve un objeto json. NO hagas cuentas: copia cada número tal como se ve (no sumes tramos, no conviertas horas ni metros).
- oferta: true si la pantalla muestra una oferta de viaje vigente que se puede aceptar (un monto y al menos un tramo «N min … km/m»). Es false si es un mapa, otra pantalla, «Viaje aceptado», «La solicitud de viaje venció», «La solicitud ya no está disponible», «se toma por terceros» u «Otro conductor aceptó».
- IGNORA el recuadro oscuro de Checa con RECHAZAR, CONSIDERAR o ACEPTAR y sus $/h y $/km: lo escribió una app, no es parte de la oferta.
- tarifa: el monto grande de la oferta. DiDi: si hay «Aceptar $X», es X; si no, el monto con multiplicador «$X 1.3x». No uses contraofertas, bonos («adicionales», «+$… por inicio»), el $/km ni montos de otras tarjetas.
- multi: multiplicador «1.4x» (0 si no hay; ignora rangos «1.8X-2.4X»).
- tarjetas: cuántas ofertas distintas se ven (1 si solo una).
- recogida y viaje: los dos tramos de la MISMA tarjeta, en orden (el primero es ir por el pasajero). Cada uno {"h": horas o 0, "min": minutos escritos, "dist": número escrito, "unidad": "km" o "m"}. «1 h 12 min» → h 1, min 12. Si solo se ve uno, el otro va con ceros.
- resumen: si hay una línea «N min · X km · $Y/km est.» (total del viaje), {"h","min","dist","unidad"}; si no, null.
- porKmEst: el Y de «$Y/km est.» (0 si no hay).
Llena tarifa, recogida, viaje y porKmEst con lo que veas aunque oferta sea false.
Ejemplo: {"oferta": true, "tarifa": 103.26, "multi": 0, "tarjetas": 1, "recogida": {"h": 0, "min": 6, "dist": 1.9, "unidad": "km"}, "viaje": {"h": 0, "min": 37, "dist": 8.8, "unidad": "km"}, "resumen": null, "porKmEst": 9.65}`;

/** Tope de la imagen en base64 (≈150 KB): una captura WebP normal pesa 30–50 KB. */
export const MAX_IMAGEN_B64 = 200_000;
export const MIN_IMAGEN_B64 = 200;

/** Base64 limpio (sin prefijo data:) o null si no es una imagen razonable. */
export function limpiarImagen(v: unknown): string | null {
  if (typeof v !== "string") return null;
  const t = v.replace(/^data:image\/[a-z0-9.+-]+;base64,/i, "").replace(/\s+/g, "");
  if (t.length < MIN_IMAGEN_B64 || t.length > MAX_IMAGEN_B64 || !/^[A-Za-z0-9+/]+={0,2}$/.test(t)) return null;
  return t;
}

export function peticionVision(imagenB64: string, app: string, moneda: string) {
  return {
    model: "deepseek-flash",
    thinking: { type: "disabled" },
    response_format: { type: "json_object" },
    max_tokens: 400,
    temperature: 0,
    messages: [
      { role: "system", content: LECTOR_IMAGEN },
      { role: "user", content: [
        { type: "text", text: `App: ${app || "?"} · moneda: ${moneda}. Lee esta captura y responde solo el json.` },
        { type: "image_url", image_url: { url: "data:image/webp;base64," + imagenB64 } },
      ] },
    ],
  };
}

export type Interruptores = { vision: boolean; texto: boolean };

/** Fila de public.ia_config → interruptores. Sin fila o con datos raros, todo encendido (como hoy). */
export function leerInterruptores(fila: unknown): Interruptores {
  const f = (Array.isArray(fila) ? fila[0] : fila) as Record<string, unknown> | undefined;
  return { vision: f?.vision !== false, texto: f?.texto !== false };
}

export type Ruta = "vision" | "texto" | "vision_apagada" | "texto_apagada";

/** Con imagen → visión (si está encendida); sin imagen → texto (si está encendido). Nunca se mezclan. */
export function elegirRuta(tieneImagen: boolean, i: Interruptores): Ruta {
  if (tieneImagen) return i.vision ? "vision" : "vision_apagada";
  return i.texto ? "texto" : "texto_apagada";
}

/** Nombre de la captura de una oferta normal, igual que lo arma el teléfono: ok-<color>-<tarifa>-<km>-<min>-<ms>-n<id>.webp */
export function nombreCapturaOk(nivel: unknown, tarifa: number, km: number, min: number, ms: number, normal: number): string {
  const color = nivel === "verde" || nivel === "amarillo" || nivel === "rojo" ? nivel : "sin";
  return `ok-${color}-${tarifa.toFixed(2)}-${km.toFixed(1)}-${Math.round(min)}-${ms}-n${normal}.webp`;
}

/** WebP real: «RIFF....WEBP». Evita guardar en el almacén algo que no sea una captura. */
export function esWebp(b: Uint8Array): boolean {
  return b.length > 12 && b[0] === 0x52 && b[1] === 0x49 && b[2] === 0x46 && b[3] === 0x46 && b[8] === 0x57 && b[9] === 0x45 && b[10] === 0x42 && b[11] === 0x50;
}

export function decodificar(b64: string): Uint8Array {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
