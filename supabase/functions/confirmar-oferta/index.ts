// Revisa en segundo plano (DeepSeek o Claude Haiku, según el secreto REVISOR) la lectura de una oferta (Uber/DiDi) que hizo el lector local.
// Recibe solo líneas con números (montos, min, km, ⚡); nunca direcciones.
// Si la lectura no coincide, guarda el caso en `errores_lector` para corregir el lector.
// Sin saldo o con cualquier falla responde { ok: false }: la app sigue con su lectura.
import Anthropic from "npm:@anthropic-ai/sdk@1.2.0";
import { calcular, ESQUEMA_LECTOR, LECTOR, type Lectura } from "./lector.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const CLAVE_ANTHROPIC = Deno.env.get("ANTHROPIC_API_KEY") ?? "";
const CLAVE_DEEPSEEK = Deno.env.get("DEEPSEEK_API_KEY") ?? "";
// Revisor: el secreto REVISOR manda («deepseek» o «anthropic»); si no está, DeepSeek si hay su clave.
const REVISOR = (Deno.env.get("REVISOR") ?? (CLAVE_DEEPSEEK ? "deepseek" : "anthropic")).toLowerCase();
const MODELO = REVISOR === "deepseek" ? "deepseek-flash" : "claude-haiku-5-5";
const CLAVE = REVISOR === "deepseek" ? CLAVE_DEEPSEEK : CLAVE_ANTHROPIC;
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

async function insertar(tabla: string, fila: Record<string, unknown> | Record<string, unknown>[]) {
  await fetch(`${SUPA}/rest/v1/${tabla}`, {
    method: "POST",
    headers: { ...headersServicio, Prefer: "return=minimal" },
    body: JSON.stringify(fila),
  });
}

// Igual que insertar, pero devuelve el id de la fila (para enlazar la captura del teléfono).
async function insertarId(tabla: string, fila: Record<string, unknown>): Promise<number | null> {
  try {
    const r = await fetch(`${SUPA}/rest/v1/${tabla}`, {
      method: "POST",
      headers: { ...headersServicio, Prefer: "return=representation" },
      body: JSON.stringify(fila),
    });
    const d = await r.json();
    return Array.isArray(d) && d[0]?.id ? Number(d[0].id) : null;
  } catch { return null; }
}

// Capturas de más de 7 días: se borran del almacén (de vez en cuando, en alguna llamada).
async function limpiarCapturas() {
  try {
    const l = await fetch(`${SUPA}/storage/v1/object/list/capturas`, {
      method: "POST", headers: headersServicio,
      body: JSON.stringify({ limit: 100, sortBy: { column: "created_at", order: "asc" } }),
    });
    const lista = await l.json();
    if (!Array.isArray(lista)) return;
    const limite = Date.now() - 7 * 86400_000;
    const viejas = lista.filter((o: { name: string; created_at?: string }) => o.created_at && Date.parse(o.created_at) < limite).map((o: { name: string }) => o.name);
    if (viejas.length === 0) return;
    await fetch(`${SUPA}/storage/v1/object/capturas`, {
      method: "DELETE", headers: headersServicio, body: JSON.stringify({ prefixes: viejas }),
    });
  } catch { /* nada */ }
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

// Precios por modelo (tabla precios_modelo, USD por millón de tokens); se leen a lo más cada 10 min.
type Precio = { entrada: number; cache: number; salida: number; fueraPico: number };
const PRECIO_BASE: Record<string, Precio> = {
  "deepseek-flash": { entrada: 0.30, cache: 0.006, salida: 1.20, fueraPico: 0.5 },
  "claude-haiku-5-5": { entrada: 0.10, cache: 0.01, salida: 0.50, fueraPico: 1 },
};
let precios = { tabla: PRECIO_BASE, leido: 0 };
async function precioDe(modelo: string): Promise<Precio> {
  if (Date.now() - precios.leido > 600_000) {
    try {
      const r = await fetch(`${SUPA}/rest/v1/precios_modelo?select=*`, { headers: headersServicio });
      const filas = await r.json();
      const tabla = { ...PRECIO_BASE };
      for (const f of filas) {
        tabla[f.modelo] = { entrada: Number(f.usd_mtok_entrada), cache: Number(f.usd_mtok_cache), salida: Number(f.usd_mtok_salida), fueraPico: Number(f.factor_fuera_pico) };
      }
      precios = { tabla, leido: Date.now() };
    } catch { /* se quedan los anteriores */ }
  }
  return precios.tabla[modelo] ?? PRECIO_BASE[modelo];
}

// DeepSeek cobra tarifa pico (lun–vie 01–04 y 06–10 UTC) y la mitad fuera de ella.
// No se descuentan los feriados chinos: en esos días el costo guardado queda un poco alto.
function esPico(modelo: string, d = new Date()): boolean {
  if (modelo !== "deepseek-flash") return true;
  const dia = d.getUTCDay();
  const h = d.getUTCHours();
  return dia >= 1 && dia <= 5 && ((h >= 1 && h < 4) || (h >= 6 && h < 10));
}

class FallaRevisor extends Error {
  constructor(public status: number, msg: string) { super(msg); }
}

type Respuesta = { datos: Record<string, unknown>; tIn: number; tCache: number; tOut: number; rechazo: boolean };

async function preguntarAnthropic(usuario: string): Promise<Respuesta> {
  const cliente = new Anthropic({ apiKey: CLAVE, timeout: 4000, maxRetries: 0 });
  const r = await cliente.messages.create({
    model: "claude-haiku-5-5",
    max_tokens: 2048,
    system: LECTOR,
    output_config: { effort: "low", format: { type: "json_schema", schema: ESQUEMA_LECTOR } },
    messages: [{ role: "user", content: usuario }],
  // deno-lint-ignore no-explicit-any
  } as any);
  const texto = r.content.filter((b) => b.type === "text").map((b) => (b as { text: string }).text).join("");
  return {
    datos: r.stop_reason === "refusal" || !texto.trim() ? {} : JSON.parse(texto),
    tIn: r.usage?.input_tokens ?? 0, tCache: 0, tOut: r.usage?.output_tokens ?? 0,
    rechazo: r.stop_reason === "refusal",
  };
}

async function preguntarDeepSeek(usuario: string): Promise<Respuesta> {
  const r = await fetch("https://api.deepseek.com/chat/completions", {
    method: "POST",
    headers: { Authorization: `Bearer ${CLAVE}`, "Content-Type": "application/json" },
    signal: AbortSignal.timeout(4000),  // más tarde la corrección ya no sirve (oferta de ~15 s)
    body: JSON.stringify({
      model: "deepseek-flash",
      // Sin razonamiento: es una extracción corta y así responde más rápido y más barato.
      thinking: { type: "disabled" },
      response_format: { type: "json_object" },
      max_tokens: 400,
      temperature: 0,
      messages: [
        { role: "system", content: LECTOR },
        { role: "user", content: usuario },
      ],
    }),
  });
  if (!r.ok) throw new FallaRevisor(r.status, `http_${r.status}`);
  const j = await r.json();
  const u = j.usage ?? {};
  const tIn = Number(u.prompt_tokens ?? 0);
  const tCache = Number(u.prompt_cache_hit_tokens ?? 0);
  const texto = String(j.choices?.[0]?.message?.content ?? "");
  // Si DeepSeek no logra un JSON válido devuelve espacios: se trata como falla (sin reintento).
  if (!texto.trim()) throw Object.assign(new FallaRevisor(0, "json_vacio"), { tIn, tCache, tOut: Number(u.completion_tokens ?? 0) });
  return { datos: JSON.parse(texto), tIn, tCache, tOut: Number(u.completion_tokens ?? 0), rechazo: false };
}

// Conteo del teléfono: { dia: "2026-11-11", ofertas, correcciones }.
async function anotarConteo(id: string, c: unknown) {
  if (!c || typeof c !== "object") return;
  const o = c as Record<string, unknown>;
  const dia = String(o.dia ?? "");
  if (!/^\d{4}-\d{2}-\d{2}$/.test(dia)) return;
  try {
    await rpc("anotar_consumo_dia", {
      u: id, d: dia,
      ofertas: Math.min(Math.round(num(o.ofertas)), 100000),
      correcciones: Math.min(Math.round(num(o.correcciones)), 100000),
    });
  } catch { /* la medición nunca frena la revisión */ }
}


// Teléfono y pantalla al leer (10.58): explican por qué falla un lector. Sin datos personales.
type Equipo = { telefono: string | null; android: string | null };
function leerEquipo(v: unknown): Equipo {
  const o = (v && typeof v === "object" ? v : {}) as Record<string, unknown>;
  return { telefono: String(o.telefono ?? "").slice(0, 40) || null, android: String(o.android ?? "").slice(0, 10) || null };
}
function leerPantalla(v: unknown): Record<string, unknown> {
  const o = (v && typeof v === "object" ? v : {}) as Record<string, unknown>;
  const t = Math.round(Number(o.tarjetas));
  return {
    orientacion: o.orientacion === "horizontal" || o.orientacion === "vertical" ? o.orientacion : null,
    zonas: typeof o.zonas === "boolean" ? o.zonas : null,
    tarjetas: Number.isFinite(t) && t > 0 && t < 20 ? t : null,
    nivel: o.nivel === "verde" || o.nivel === "amarillo" || o.nivel === "rojo" ? o.nivel : null,
  };
}

// Ofertas donde el texto y la foto del teléfono leyeron lo mismo (10.57): se anotan sin IA.
async function anotarCoincidencias(id: string, version: string, equipo: Equipo, v: unknown) {
  if (!Array.isArray(v) || v.length === 0) return;
  const filas = v.slice(0, 50).filter((c) => c && typeof c === "object").map((c) => {
    const o = c as Record<string, unknown>;
    const visto = Number(o.visto);
    const ms = (x: unknown) => { const n = Math.round(Number(x)); return Number.isFinite(n) && n >= 0 ? n : null; };
    return {
      usuario: id, version, app: String(o.app ?? "").slice(0, 10),
      tarifa: num(o.tarifa), km: num(o.km), min: num(o.min), rec_km: num(o.recKm), rec_min: num(o.recMin), multi: num(o.multi),
      ms_texto: ms(o.msTexto), ms_foto: ms(o.msFoto), ...equipo, ...leerPantalla(o.pantalla),
      visto: Number.isFinite(visto) && visto > 0 ? new Date(visto).toISOString() : null,
    };
  });
  if (filas.length > 0) {
    try { await insertar("coincidencias", filas); } catch { /* nunca frena la revisión */ }
  }
}

const igual = (a: Lectura, b: Lectura) =>
  Math.abs(a.tarifa - b.tarifa) < 1 && Math.abs(a.km - b.km) < 0.15 && Math.abs(a.min - b.min) < 1.1;

function num(v: unknown): number {
  const n = Number(v);
  return Number.isFinite(n) && n >= 0 ? n : 0;
}

function leerLocal(v: unknown): Lectura | null {
  if (!v || typeof v !== "object") return null;
  const o = v as Record<string, unknown>;
  return { tarifa: num(o.tarifa), km: num(o.km), min: num(o.min), recKm: num(o.recKm), recMin: num(o.recMin), multi: num(o.multi) };
}

/** Guarda en la fila los tokens reales y el costo con el precio vigente (y la hora pico). */
async function anotarCosto(registro: Record<string, unknown>, tIn: number, tCache: number, tOut: number) {
  const p = await precioDe(MODELO);
  const f = esPico(MODELO) ? 1 : p.fueraPico;
  const sinCache = Math.max(tIn - tCache, 0);
  registro.tokens_entrada = tIn;
  registro.tokens_cache = tCache;
  registro.tokens_salida = tOut;
  registro.usd_mtok_entrada = p.entrada * f;
  registro.usd_mtok_salida = p.salida * f;
  registro.costo_usd = (sinCache * p.entrada + tCache * p.cache + tOut * p.salida) * f / 1_000_000;
}

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
  const version = String(cuerpo.version ?? "").slice(0, 12);
  const equipo = leerEquipo(cuerpo.equipo);
  const pantalla = leerPantalla(cuerpo.pantalla);
  await anotarCoincidencias(id, version, equipo, cuerpo.coincidencias);
  // Solo coincidencias (lote de 10 sin otra revisión pendiente): no hay nada que preguntar.
  if (cuerpo.modo === "coincidencias") return json({ ok: true });
  const lineas = filtrar(cuerpo.lineas);
  // «rescate»: el lector del teléfono no entendió la oferta y no hay lectura local que comparar.
  // «desacuerdo» (10.57): el texto y la foto leyeron distinto; la IA ve las dos.
  const modo = cuerpo.modo === "rescate" ? "rescate" : cuerpo.modo === "desacuerdo" ? "desacuerdo" : "revision";
  const local = modo === "rescate"
    ? { tarifa: 0, km: 0, min: 0, recKm: 0, recMin: 0, multi: 0 }
    : leerLocal(cuerpo.local);
  const app = String(cuerpo.app ?? "").slice(0, 10);
  const moneda = String(cuerpo.moneda ?? "MXN").slice(0, 3);
  // De qué lector del teléfono vino (10.55): «ocr» (foto) o «texto» (texto de la app).
  const lector = cuerpo.lector === "texto" || cuerpo.lector === "ocr" ? cuerpo.lector : null;
  if (lineas.length === 0 || !local) return json({ ok: false, error: "sin_datos" }, 400);
  const lectorOtro = cuerpo.lectorOtro === "texto" || cuerpo.lectorOtro === "ocr" ? cuerpo.lectorOtro : null;
  const lineasOtro = modo === "desacuerdo" ? filtrar(cuerpo.lineasOtro) : [];
  const localOtro = modo === "desacuerdo" ? leerLocal(cuerpo.localOtro) : null;
  await anotarConteo(id, cuerpo.conteo);
  await anotarConteo(id, cuerpo.conteoAnterior);
  const msPopup = Math.round(Number(cuerpo.msPopup ?? -1));
  const msRevisionAnterior = Math.round(Number(cuerpo.msRevisionAnterior ?? -1));
  // Una fila por revisión: tiempos, tokens reales y resultado (métricas de los lunes).
  const registro: Record<string, unknown> = {
    usuario: id, app, version, modelo: MODELO, modo, lector,
    ms_popup: Number.isFinite(msPopup) ? msPopup : null,
    ms_revision_anterior: Number.isFinite(msRevisionAnterior) ? msRevisionAnterior : null,
    imagenes: 0, reintentos: 0,
    ms_foto: Number.isFinite(Number(cuerpo.msFoto)) && Number(cuerpo.msFoto) >= 0 ? Math.round(Number(cuerpo.msFoto)) : null,
    ...equipo, ...pantalla,
  };

  // Tope diario por cuenta (cuenta también las que fallan).
  try {
    const n = Number(await rpc("sumar_uso_confirmar", { u: id }));
    if (n > TOPE_DIARIO) {
      await insertar("revisiones", { ...registro, ok: false, error: "tope_diario" });
      return json({ ok: false, error: "tope_diario" });
    }
  } catch {
    return json({ ok: false, error: "sin_contador" }, 503);
  }
  if (!CLAVE) {
    await insertar("revisiones", { ...registro, ok: false, error: "sin_clave" });
    return json({ ok: false, error: "sin_clave" });
  }

  let ia: Lectura & { oferta: boolean };
  const t0 = Date.now();
  try {
    const nombre = (l: unknown) => l === "texto" ? "texto de la app (números exactos)" : "foto con OCR (lo que se ve; puede confundir letras)";
    const pregunta = modo === "desacuerdo" && lineasOtro.length > 0
      ? `App: ${app || "?"} · moneda: ${moneda}\nDos lecturas de la MISMA pantalla que no coinciden; usa las dos para dar la oferta correcta.\n` +
        `Lectura A, ${nombre(lector)}:\n${lineas.join("\n")}\n\nLectura B, ${nombre(lectorOtro)}:\n${lineasOtro.join("\n")}`
      : `App: ${app || "?"} · moneda: ${moneda}\nLíneas:\n${lineas.join("\n")}`;
    const r = REVISOR === "deepseek" ? await preguntarDeepSeek(pregunta) : await preguntarAnthropic(pregunta);
    await anotarCosto(registro, r.tIn, r.tCache, r.tOut);
    if (r.rechazo) throw new Error("refusal");
    const d = r.datos;
    // La IA solo lee; las cuentas (horas, metros, recogida + viaje, $/km est.) van en lector.ts.
    ia = calcular(d, app);
  } catch (e) {
    // Sin saldo, clave mala, caída o tiempo agotado: se anota una vez por día y tipo.
    const status = (e as { status?: number })?.status ?? 0;
    const cobrado = e as { tIn?: number; tCache?: number; tOut?: number };
    if (cobrado.tIn) await anotarCosto(registro, cobrado.tIn, cobrado.tCache ?? 0, cobrado.tOut ?? 0);
    try {
      await rpc("anotar_falla_ia", { tipo: status ? `http_${status}` : String((e as Error)?.message ?? "error").slice(0, 40) });
    } catch { /* nada */ }
    await insertar("revisiones", {
      ...registro, ms_ia: Date.now() - t0, ok: false,
      error: status ? `http_${status}` : String((e as Error)?.message ?? "error").slice(0, 40),
    });
    return json({ ok: false, error: "ia_no_disponible" });
  }

  // «coincide» = la IA da lo mismo que el pop-up que ya se ve (lectura A): no hay que corregir.
  const coincide = modo !== "rescate" && ia.oferta && igual(ia, local);
  registro.ms_ia = Date.now() - t0;
  await insertar("revisiones", { ...registro, ok: true, coincide });
  let caso: number | null = null;
  if (Math.random() < 0.02) await limpiarCapturas();
  if (modo === "desacuerdo") {
    // Siempre se guarda: los lectores no coincidieron. «acerto» dice cuál tenía razón.
    const acerto = !ia.oferta ? "ninguno" : igual(ia, local) ? lector : localOtro && igual(ia, localOtro) ? lectorOtro : "ninguno";
    caso = await insertarId("errores_lector", {
      usuario: id, app, moneda, version, lineas: [...lineas, "---", ...lineasOtro], local, ia, lector,
      lector_otro: lectorOtro, local_otro: localOtro, acerto, tipo: "desacuerdo",
      ms_texto: registro.ms_popup, ms_foto: registro.ms_foto, ...equipo, ...pantalla,
    });
  } else if (!coincide) {
    // En el rescate solo se guarda si el revisor sí vio una oferta (si no, no hay nada que aprender).
    if (modo === "revision" || ia.oferta) {
      caso = await insertarId("errores_lector", { usuario: id, app, moneda, version, lineas, local, ia, lector, tipo: modo === "rescate" ? "rescate" : "diferencia", ...equipo, ...pantalla });
    }
  }
  return json({ ok: true, coincide, ia, caso });
});
