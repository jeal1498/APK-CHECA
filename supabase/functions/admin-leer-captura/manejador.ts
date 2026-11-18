// Lógica de admin-leer-captura, sin red ni Deno: todo lo externo entra por `Deps` (así se prueba con node:test).
// Una captura de oferta se lee con un modelo de visión SOLO cuando un administrador lo pide. Es una sugerencia:
// el veredicto lo da una persona. Aquí no se guarda nada.

export const NOMBRE_OK = /^ok-(rojo|amarillo|verde|sin)-\d{1,6}(\.\d{1,2})?-\d{1,4}(\.\d)?-\d{1,4}-\d{10,14}(-n\d{1,9})?\.webp$/;
export const NOMBRE_DUDOSA = /^\d{1,9}\.webp$/;
export const MAX_IMAGEN = 600_000;   // el almacén ya limita a 500 000

export const INSTRUCCIONES = `Lees capturas de pantalla de una oferta de viaje de Uber o DiDi en un teléfono Android.
Responde SOLO un JSON con estas claves:
- "estado": "vigente" (hay una oferta que se puede aceptar), "vencida" (la solicitud venció o ya no está disponible), "aceptada" (viaje aceptado, otro conductor aceptó o se toma por terceros) o "sin_oferta" (la pantalla no es una oferta: mapa, otra app).
- "tarifa": número en pesos (el precio grande o el del botón Aceptar).
- "km" y "min": TOTALES del viaje. Si se ven dos tramos (llegar al pasajero y el viaje), suma las distancias y suma los minutos.
- "confianza": "alta", "media" o "baja".
- "nota": texto corto, solo si algo no se ve claro.
Si un dato no se ve, pon null. Ignora el recuadro oscuro de Checa con RECHAZAR, CONSIDERAR o ACEPTAR y sus $/h y $/km: lo escribió una app, no es parte de la oferta. No escribas nada fuera del JSON.`;

export type Lectura = { estado: string; tarifa: number | null; km: number | null; min: number | null; confianza: string; nota: string | null };

export type Deps = {
  clave: string;
  origenPermitido: string;
  esAdministrador: (auth: string) => Promise<boolean>;
  descargar: (nombre: string) => Promise<Uint8Array | null | "grande">;
  preguntar: (cuerpo: unknown) => Promise<{ status: number; json: unknown }>;
  ahora?: () => number;
};

function num(v: unknown, max: number): number | null {
  const n = typeof v === "string" ? Number(v.replace(",", ".")) : v;
  return typeof n === "number" && Number.isFinite(n) && n > 0 && n <= max ? Math.round(n * 1000) / 1000 : null;
}

/** Acepta solo lo esperado del modelo; cualquier cosa rara queda en null o en el valor más prudente. */
export function normalizar(crudo: unknown): Lectura | null {
  if (!crudo || typeof crudo !== "object") return null;
  const o = crudo as Record<string, unknown>;
  const estado = ["vigente", "vencida", "aceptada", "sin_oferta"].includes(String(o.estado)) ? String(o.estado) : null;
  if (!estado) return null;
  const confianza = ["alta", "media", "baja"].includes(String(o.confianza)) ? String(o.confianza) : "baja";
  const nota = typeof o.nota === "string" && o.nota.trim() ? o.nota.trim().slice(0, 200) : null;
  return { estado, tarifa: num(o.tarifa, 100000), km: num(o.km, 500), min: num(o.min ?? o.minutos, 1000), confianza, nota };
}

export function base64(b: Uint8Array): string {
  let s = "";
  for (let i = 0; i < b.length; i += 0x8000) s += String.fromCharCode(...b.subarray(i, i + 0x8000));
  return btoa(s);
}

export function armarPeticion(imagen: Uint8Array) {
  return {
    model: "deepseek-flash",
    thinking: { type: "disabled" },
    response_format: { type: "json_object" },
    max_tokens: 300,
    temperature: 0,
    messages: [
      { role: "system", content: INSTRUCCIONES },
      { role: "user", content: [
        { type: "text", text: "Lee esta captura." },
        { type: "image_url", image_url: { url: "data:image/webp;base64," + base64(imagen) } },
      ] },
    ],
  };
}

export function crearManejador(d: Deps) {
  const cors = { "Access-Control-Allow-Origin": d.origenPermitido, "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
                 "Access-Control-Allow-Methods": "POST, OPTIONS", "Vary": "Origin" };
  const json = (b: unknown, status = 200) => new Response(JSON.stringify(b), { status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" } });
  const ahora = d.ahora ?? Date.now;
  return async (req: Request): Promise<Response> => {
    if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
    if (req.method !== "POST") return json({ error: "metodo" }, 405);
    if (!d.clave) return json({ error: "sin_clave" }, 503);
    const auth = req.headers.get("Authorization") ?? "";
    if (!/^Bearer \S{20,}$/.test(auth)) return json({ error: "no_autorizado" }, 401);
    let admin = false;
    try { admin = await d.esAdministrador(auth); } catch { return json({ error: "auth_no_disponible" }, 503); }
    if (!admin) return json({ error: "no_autorizado" }, 403);
    let nombre = "";
    try { const c = await req.json(); nombre = String(c?.nombre ?? ""); } catch { return json({ error: "cuerpo" }, 400); }
    if (!NOMBRE_OK.test(nombre) && !NOMBRE_DUDOSA.test(nombre)) return json({ error: "nombre_invalido" }, 400);
    const imagen = await d.descargar(nombre);
    if (imagen === null) return json({ error: "no_existe" }, 404);
    if (imagen === "grande" || imagen.length > MAX_IMAGEN) return json({ error: "imagen_grande" }, 413);
    const t0 = ahora();
    let r: { status: number; json: unknown };
    try { r = await d.preguntar(armarPeticion(imagen)); } catch { return json({ error: "ia_no_disponible", detalle: "red" }, 502); }
    if (r.status !== 200) return json({ error: "ia_no_disponible", detalle: `http_${r.status}` }, 502);
    const j = r.json as { choices?: { message?: { content?: unknown } }[]; usage?: { prompt_tokens?: number; completion_tokens?: number } };
    let lectura: Lectura | null = null;
    try { lectura = normalizar(JSON.parse(String(j.choices?.[0]?.message?.content ?? ""))); } catch { lectura = null; }
    if (!lectura) return json({ error: "respuesta_invalida" }, 502);
    return json({ ok: true, modelo: "deepseek-flash", lectura, tokens: { entrada: Number(j.usage?.prompt_tokens ?? 0), salida: Number(j.usage?.completion_tokens ?? 0) }, ms: ahora() - t0 });
  };
}
