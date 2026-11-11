// Revisa en segundo plano la lectura de una oferta (Uber/DiDi) que hizo el lector local.
// Recibe solo líneas con números (montos, min, km, ⚡); nunca direcciones.
// Si la lectura no coincide, guarda el caso en `errores_lector` para corregir el lector.
// Sin saldo o con cualquier falla responde { ok: false }: la app sigue con su lectura.
import Anthropic from "npm:@anthropic-ai/sdk@1.2.0";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const CLAVE = Deno.env.get("ANTHROPIC_API_KEY") ?? "";
/** Consultas por cuenta al día. */
const TOPE_DIARIO = 300;

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

const headersServicio = {
  apikey: SERVICIO,
  Authorization: `Bearer ${SERVICIO}`,
  "Content-Type": "application/json",
};

async function rpc(nombre: string, args: Record<string, unknown>): Promise<unknown> {
  const r = await fetch(`${SUPA}/rest/v1/rpc/${nombre}`, {
    method: "POST",
    headers: headersServicio,
    body: JSON.stringify(args),
  });
  if (!r.ok) throw new Error(`rpc_${nombre}_${r.status}`);
  return await r.json();
}

async function insertar(tabla: string, fila: Record<string, unknown>) {
  await fetch(`${SUPA}/rest/v1/${tabla}`, {
    method: "POST",
    headers: { ...headersServicio, Prefer: "return=minimal" },
    body: JSON.stringify(fila),
  });
}

// Solo líneas cortas con un número y algo de oferta: así no viajan direcciones ni nombres.
const UTIL = /(min|km|\$|mxn|\/km|\dx\b|×|⚡|incluid|adicional|por inicio)/i;
function filtrar(lineas: unknown): string[] {
  if (!Array.isArray(lineas)) return [];
  return lineas
    .map((l) => String(l ?? "").trim())
    .filter((l) => l.length > 0 && l.length <= 60 && /\d/.test(l) && UTIL.test(l) && !/total/i.test(l))
    .slice(0, 30);
}

type Lectura = { tarifa: number; km: number; min: number; recKm: number; recMin: number; multi: number };

function num(v: unknown): number {
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : 0;
}

function leerLocal(v: unknown): Lectura | null {
  if (!v || typeof v !== "object") return null;
  const o = v as Record<string, unknown>;
  return { tarifa: num(o.tarifa), km: num(o.km), min: num(o.min), recKm: num(o.recKm), recMin: num(o.recMin), multi: num(o.multi) };
}

const ESQUEMA = {
  type: "object",
  properties: {
    oferta: { type: "boolean", description: "true si las líneas describen una oferta de viaje" },
    tarifa: { type: "number" },
    km: { type: "number" },
    min: { type: "number" },
    recKm: { type: "number" },
    recMin: { type: "number" },
    multi: { type: "number" },
  },
  required: ["oferta", "tarifa", "km", "min", "recKm", "recMin", "multi"],
  additionalProperties: false,
};

const SISTEMA = `Lees ofertas de viaje de Uber y DiDi a partir de líneas de texto de la pantalla (OCR o accesibilidad; puede haber errores como «B min» por «8 min» o «Ih» por «1 h»).
Devuelve:
- tarifa: lo que paga el viaje (el monto grande de la tarjeta). No sumes bonos («adicionales», «+$… por inicio»); «+$… incluido» ya viene dentro de la tarifa. No uses contraofertas de DiDi, el $/km ni montos de otras tarjetas: si hay un resumen «N min · X km · $Y/km est.», la tarifa ≈ Y × X.
- km y min: TOTALES (recogida + viaje). Si hay un resumen «N min · X km», ese es el total; no lo sumes con los tramos. Metros «720 m» = 0.72 km.
- recKm y recMin: el primer tramo (ir por el pasajero). 0 si no se distingue.
- multi: multiplicador «1.4x» de la tarifa; ignora rangos del mapa «1.8X-2.4X». 0 si no hay.
Si no hay una oferta clara, oferta=false y todo en 0. Nunca inventes números que no estén en las líneas.`;

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ ok: false, error: "metodo_no_permitido" }, 405);
  const auth = req.headers.get("Authorization") ?? "";
  if (!auth.startsWith("Bearer ")) return json({ ok: false, error: "sesion_requerida" }, 401);

  let usuario: { id?: string };
  try {
    const r = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
    if (!r.ok) return json({ ok: false, error: "sesion_invalida" }, 401);
    usuario = await r.json();
  } catch {
    return json({ ok: false, error: "no_se_pudo_validar_sesion" }, 503);
  }
  const id = String(usuario.id ?? "");
  if (!id) return json({ ok: false, error: "sesion_invalida" }, 401);

  let cuerpo: Record<string, unknown>;
  try {
    cuerpo = await req.json();
  } catch {
    return json({ ok: false, error: "cuerpo_invalido" }, 400);
  }
  const lineas = filtrar(cuerpo.lineas);
  const local = leerLocal(cuerpo.local);
  const app = String(cuerpo.app ?? "").slice(0, 10);
  const moneda = String(cuerpo.moneda ?? "MXN").slice(0, 3);
  const version = String(cuerpo.version ?? "").slice(0, 12);
  if (lineas.length === 0 || !local) return json({ ok: false, error: "sin_datos" }, 400);

  // Tope diario por cuenta (cuenta también las que fallan).
  try {
    const n = Number(await rpc("sumar_uso_confirmar", { u: id }));
    if (n > TOPE_DIARIO) return json({ ok: false, error: "tope_diario" });
  } catch {
    return json({ ok: false, error: "sin_contador" }, 503);
  }
  if (!CLAVE) return json({ ok: false, error: "sin_clave" });

  let ia: Lectura & { oferta: boolean };
  try {
    const cliente = new Anthropic({ apiKey: CLAVE, timeout: 8000, maxRetries: 0 });
    const r = await cliente.messages.create({
      model: "claude-haiku-5-5",
      max_tokens: 2048,
      system: SISTEMA,
      output_config: { effort: "low", format: { type: "json_schema", schema: ESQUEMA } },
      messages: [{ role: "user", content: `App: ${app || "?"} · moneda: ${moneda}\nLíneas:\n${lineas.join("\n")}` }],
    // deno-lint-ignore no-explicit-any
    } as any);
    if (r.stop_reason === "refusal") throw new Error("refusal");
    const texto = r.content.filter((b) => b.type === "text").map((b) => (b as { text: string }).text).join("");
    const d = JSON.parse(texto);
    ia = { oferta: d.oferta === true, tarifa: num(d.tarifa), km: num(d.km), min: num(d.min), recKm: num(d.recKm), recMin: num(d.recMin), multi: num(d.multi) };
  } catch (e) {
    // Sin saldo, clave mala, caída o tiempo agotado: se anota una vez por día y tipo.
    const status = (e as { status?: number })?.status ?? 0;
    try {
      await rpc("anotar_falla_ia", { tipo: status ? `http_${status}` : String((e as Error)?.message ?? "error").slice(0, 40) });
    } catch { /* nada */ }
    return json({ ok: false, error: "ia_no_disponible" });
  }

  const coincide = ia.oferta &&
    Math.abs(ia.tarifa - local.tarifa) < 1 && Math.abs(ia.km - local.km) < 0.15 && Math.abs(ia.min - local.min) < 1.1;
  if (!coincide) {
    await insertar("errores_lector", { usuario: id, app, moneda, version, lineas, local, ia });
  }
  return json({ ok: true, coincide, ia });
});
