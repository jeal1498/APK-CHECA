// Casos compartidos por las pruebas de E2 (Node) y la prueba cruzada con Postgres (Python).
export const U = "11111111-1111-4111-8111-111111111111";
let n = 0;
export function uuid(): string {
  n++;
  const h = n.toString(16).padStart(12, "0");
  return `aaaaaaaa-bbbb-4ccc-8ddd-${h}`;
}
export function evento(k: Record<string, unknown> = {}): Record<string, unknown> {
  const e: Record<string, unknown> = {
    id: uuid(), contrato: 1, app: "didi", version_app: "10.81", version_parser: "10.79", lector: "ocr",
    fotograma_id: uuid(), captura_vinculo: "sin_captura", ts_fotograma: "2026-11-15T10:00:00Z", ts_evento: "2026-11-15T10:00:01Z",
    resultado: "oferta", tarifa: 120.5, km: 10.2, min: 25, rec_km: 1.5, rec_min: 5, moneda: "MXN", nivel: "verde", tarjetas: 1,
    ms_lectura: 420, ms_popup: 650, lineas_sha256: "a".repeat(64), lineas_redactadas: ["$120.50", "5 min (1.5 km)"],
    orientacion: "vertical", etapas: [{ etapa: "captura", estado: "ok", ms: 30 }, { etapa: "parser", estado: "ok", ms: 2 }],
    ...k,
  };
  for (const x of Object.keys(e)) if (e[x] === undefined) delete e[x];
  return e;
}
/** Casos de coherencia: [nombre, cambios, ¿válido?]. Los mismos deben dar lo mismo en TS y en la base. */
export const COHERENCIA: [string, Record<string, unknown>, boolean][] = [
  ["oferta completa", {}, true],
  ["oferta sin semáforo", { nivel: undefined }, false],
  ["oferta sin tarifa", { tarifa: undefined }, false],
  ["oferta km 0", { km: 0 }, false],
  ["semáforo sin oferta", { resultado: "no_oferta", tarifa: undefined, km: undefined, min: undefined }, false],
  ["no_oferta sin semáforo", { resultado: "no_oferta", tarifa: undefined, km: undefined, min: undefined, nivel: undefined }, true],
  ["varias con datos", { resultado: "varias", nivel: undefined }, false],
  ["varias sin datos", { resultado: "varias", nivel: undefined, tarifa: undefined, km: undefined, min: undefined, motivo: "tarjeta_ambigua" }, true],
  ["abstención", { resultado: "abstencion", nivel: undefined, tarifa: undefined, km: undefined, min: undefined, motivo: "datos_insuficientes" }, true],
  ["exacto sin sha", { captura_vinculo: "exacto" }, false],
  ["exacto con sha", { captura_vinculo: "exacto", captura_sha256: "b".repeat(64) }, true],
  ["ocr sin fotograma", { fotograma_id: undefined }, false],
  ["texto sin fotograma", { lector: "texto", fotograma_id: undefined, ts_fotograma: undefined }, true],
  ["app desconocida", { app: "cabify" }, false],
  ["sha mal", { lineas_sha256: "xyz" }, false],
  ["contrato 2", { contrato: 2 }, false],
  ["tarifa negativa", { tarifa: -5 }, false],
  ["31 líneas", { lineas_redactadas: Array(31).fill("$1") }, false],
  ["fotograma futuro", { ts_fotograma: "2026-11-15T11:00:00Z" }, false],
  ["etapa desconocida", { etapas: [{ etapa: "magia", estado: "ok" }] }, false],
  ["multiplicador", { multi: 1.4 }, true],
  ["multiplicador < 1", { multi: 0.5 }, false],
];
