// Contrato v1 de registrar-lectura (E2). Validación pura, sin red ni Deno: la prueban Node y Deno igual.
// El teléfono manda { contrato: 1, eventos: [...] } (1 a 50). Cada evento se valida por separado; los
// campos desconocidos se rechazan (así un cambio del teléfono no pasa en silencio). Ver auditoria/CONTRATO.md.

export const CONTRATO = 1;
export const MAX_EVENTOS = 50;
export const MAX_BYTES = 64 * 1024;

export type Evento = Record<string, unknown> & { id: string };
export type ErrorEvento = { id: string | null; estado: "invalido"; error: string };

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
const SHA = /^[0-9a-f]{64}$/;
const FECHA = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,6})?Z$/;
const VERSION = /^\d+\.\d+(\.\d+)?$/;

const RESULTADOS = ["oferta", "no_oferta", "duplicada", "abstencion", "varias", "descartada", "timeout", "error"];
const ETAPAS = ["captura", "ocr", "texto", "tarjetas", "parser", "dedupe", "popup", "revision", "envio"];
const ESTADOS = ["ok", "fallo", "omitida", "timeout"];

type Regla = (v: unknown) => string | null;
const texto = (max: number, rx?: RegExp): Regla => (v) =>
  typeof v !== "string" || v.length === 0 || v.length > max || (rx && !rx.test(v)) ? "texto" : null;
const uno = (lista: string[]): Regla => (v) => (typeof v === "string" && lista.includes(v) ? null : "valor");
const num = (min: number, max: number, entero = false): Regla => (v) =>
  typeof v !== "number" || !Number.isFinite(v) || v < min || v >= max || (entero && !Number.isInteger(v)) ? "numero" : null;
const fecha: Regla = (v) => (typeof v === "string" && FECHA.test(v) && !Number.isNaN(Date.parse(v)) ? null : "fecha");

const CAMPOS: Record<string, { regla: Regla; requerido?: boolean }> = {
  id: { regla: texto(36, UUID), requerido: true },
  contrato: { regla: (v) => (v === CONTRATO ? null : "contrato"), requerido: true },
  app: { regla: uno(["uber", "didi", "otra"]), requerido: true },
  version_app: { regla: texto(16, VERSION), requerido: true },
  version_parser: { regla: texto(64), requerido: true },
  lector: { regla: uno(["texto", "ocr", "ia"]), requerido: true },
  fotograma_id: { regla: texto(36, UUID) },
  captura_sha256: { regla: texto(64, SHA) },
  captura_vinculo: { regla: uno(["exacto", "sin_captura"]) },
  ts_fotograma: { regla: fecha },
  ts_evento: { regla: fecha, requerido: true },
  resultado: { regla: uno(RESULTADOS), requerido: true },
  motivo: { regla: texto(64, /^[a-z0-9_]+$/) },
  tarifa: { regla: num(0.01, 100000) },
  km: { regla: num(0, 1000) },
  min: { regla: num(0, 1440) },
  rec_km: { regla: num(0, 1000) },
  rec_min: { regla: num(0, 1440) },
  multi: { regla: num(1, 20) },
  moneda: { regla: texto(3, /^[A-Z]{3}$/) },
  nivel: { regla: uno(["verde", "amarillo", "rojo"]) },
  tarjetas: { regla: num(0, 21, true) },
  ms_lectura: { regla: num(0, 600001, true) },
  ms_popup: { regla: num(0, 600001, true) },
  lineas_sha256: { regla: texto(64, SHA) },
  lineas_redactadas: {
    regla: (v) => (Array.isArray(v) && v.length <= 30 && v.every((l) => typeof l === "string" && l.length <= 60 && lineaRedactada(l)) ? null : "lineas"),
  },
  telefono: { regla: texto(64) },
  android: { regla: texto(8) },
  orientacion: { regla: uno(["vertical", "horizontal"]) },
  etapas: {
    regla: (v) => {
      if (!Array.isArray(v) || v.length > 32) return "etapas";
      for (const e of v) {
        if (!e || typeof e !== "object" || Array.isArray(e)) return "etapas";
        const o = e as Record<string, unknown>;
        if (Object.keys(o).some((k) => !["etapa", "estado", "ms", "motivo"].includes(k))) return "etapas";
        if (uno(ETAPAS)(o.etapa) || uno(ESTADOS)(o.estado)) return "etapas";
        if (o.ms !== undefined && num(0, 600001, true)(o.ms)) return "etapas";
        if (o.motivo !== undefined && texto(64, /^[a-z0-9_]+$/)(o.motivo)) return "etapas";
      }
      return null;
    },
  },
};

// Lista blanca de la redacción: la MISMA gramática que Redaccion.PIEZA en Traza.kt (Android). Una línea solo se
// acepta si es una secuencia de piezas permitidas separadas por un espacio: montos, número + min/km/m/h/x, «/km»,
// signos y unas pocas palabras fijas. Una dirección, un nombre, un correo, una URL o un número suelto no caben.
const PALABRAS = "min|km|m|h|est|viaje|a|de|por|inicio|incluido|adicionales|adicional|aceptar|total|tarifa";
const FIN = "(?![\\p{L}\\d])";
const PIEZA = `[+-]?[$€£] ?\\d[\\d.,]{0,11}|/ ?(?:km|h)${FIN}|(?<![\\p{L}\\d.,])\\d{1,5}(?:[.,]\\d{1,3})? ?(?:min|km|m|h|x)${FIN}|[()⚡·:+]|(?<![\\p{L}\\d])(?:${PALABRAS})${FIN}`;
const LINEA_REDACTADA = new RegExp(`^(?:${PIEZA})(?: (?:${PIEZA}))*$`, "iu");
const CON_DATO = new RegExp(`[$€£] ?\\d|\\d ?(?:min|km|m|h|x)${FIN}`, "iu");

/** true si la línea cumple la lista blanca y trae al menos un dato. */
export function lineaRedactada(l: string): boolean {
  return LINEA_REDACTADA.test(l) && CON_DATO.test(l);
}

/** Valida un evento. Devuelve el código del primer error o null. */
export function validarEvento(e: unknown): string | null {
  if (!e || typeof e !== "object" || Array.isArray(e)) return "no_objeto";
  const o = e as Record<string, unknown>;
  for (const k of Object.keys(o)) if (!(k in CAMPOS)) return `campo_desconocido:${k.slice(0, 32)}`;
  for (const [k, c] of Object.entries(CAMPOS)) {
    const v = o[k];
    if (v === undefined || v === null) {
      if (c.requerido) return `falta:${k}`;
      continue;
    }
    const err = c.regla(v);
    if (err) return `${err}:${k}`;
  }
  // Coherencia (también la impone la base de datos, 001_auditoria.sql).
  const r = o.resultado;
  if (r === "oferta") {
    for (const k of ["tarifa", "km", "min", "nivel"]) if (o[k] === undefined || o[k] === null) return `oferta_sin:${k}`;
    if ((o.km as number) <= 0 || (o.min as number) <= 0) return "oferta_sin:tramo";
  } else if (o.nivel !== undefined && o.nivel !== null) return "semaforo_sin_oferta";
  if (r === "varias" && ["tarifa", "km", "min"].some((k) => o[k] !== undefined && o[k] !== null)) return "varias_con_datos";
  if (o.captura_vinculo === "exacto" && (!o.fotograma_id || !o.captura_sha256)) return "vinculo_exacto_incompleto";
  if (o.lector === "ocr" && !o.fotograma_id) return "ocr_sin_fotograma";
  if (o.ts_fotograma && Date.parse(o.ts_fotograma as string) > Date.parse(o.ts_evento as string) + 5000) return "fotograma_futuro";
  return null;
}

export type Lote =
  | { ok: true; validos: Evento[]; invalidos: ErrorEvento[] }
  | { ok: false; status: number; error: string };

/** Valida el cuerpo completo (texto crudo, para medir bytes reales). */
export function validarLote(crudo: string): Lote {
  if (new TextEncoder().encode(crudo).length > MAX_BYTES) return { ok: false, status: 413, error: "demasiado_grande" };
  let b: unknown;
  try { b = JSON.parse(crudo); } catch { return { ok: false, status: 400, error: "json_invalido" }; }
  if (!b || typeof b !== "object" || Array.isArray(b)) return { ok: false, status: 400, error: "no_objeto" };
  const o = b as Record<string, unknown>;
  if (o.contrato !== CONTRATO) return { ok: false, status: 400, error: "contrato_no_soportado" };
  if (Object.keys(o).some((k) => k !== "contrato" && k !== "eventos")) return { ok: false, status: 400, error: "campo_desconocido" };
  const ev = o.eventos;
  if (!Array.isArray(ev) || ev.length === 0 || ev.length > MAX_EVENTOS) return { ok: false, status: 400, error: "eventos" };
  const validos: Evento[] = [], invalidos: ErrorEvento[] = [], vistos = new Set<string>();
  for (const e of ev) {
    const id = e && typeof e === "object" && typeof (e as { id?: unknown }).id === "string" ? (e as { id: string }).id : null;
    const err = validarEvento(e) ?? (id && vistos.has(id) ? "id_repetido_en_lote" : null);
    if (err) invalidos.push({ id: id && UUID.test(id) ? id : null, estado: "invalido", error: err });
    else { validos.push(e as Evento); vistos.add(id!); }
  }
  return { ok: true, validos, invalidos };
}
