// Valida con el contrato TS cada evento de un archivo JSON (lista). Imprime [{id, error}].
import { readFileSync } from "node:fs";
import { validarEvento } from "../../../supabase/functions/registrar-lectura/contrato.ts";
const l = JSON.parse(readFileSync(process.argv[2], "utf8")) as Record<string, unknown>[];
if (process.argv[3] === "--lineas") {
  // Modo corpus: [{caso, salen: [línea…]}] → líneas que el contrato rechazaría.
  const { lineaRedactada } = await import("../../../supabase/functions/registrar-lectura/contrato.ts");
  const malas = (l as unknown as { caso: string; salen: string[] }[]).flatMap((c) => c.salen.filter((x) => x.length > 60 || !lineaRedactada(x)).map((x) => ({ caso: c.caso, linea: x })));
  console.log(JSON.stringify(malas));
} else console.log(JSON.stringify(l.map((e) => ({ id: e.id, error: validarEvento(e) }))));
