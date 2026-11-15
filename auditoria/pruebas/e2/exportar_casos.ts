// Exporta los casos de coherencia con el veredicto del contrato TS (para la prueba cruzada con Postgres).
import { validarEvento } from "../../../supabase/functions/registrar-lectura/contrato.ts";
import { COHERENCIA, evento } from "./casos.ts";
console.log(JSON.stringify(COHERENCIA.map(([n, k, v]) => { const e = evento(k); return { nombre: n, evento: e, esperado: v, ts: validarEvento(e) === null }; })));
