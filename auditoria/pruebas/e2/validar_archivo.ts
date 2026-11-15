// Valida con el contrato TS cada evento de un archivo JSON (lista). Imprime [{id, error}].
import { readFileSync } from "node:fs";
import { validarEvento } from "../../../supabase/functions/registrar-lectura/contrato.ts";
const l = JSON.parse(readFileSync(process.argv[2], "utf8")) as Record<string, unknown>[];
console.log(JSON.stringify(l.map((e) => ({ id: e.id, error: validarEvento(e) }))));
