// Manejador HTTP de registrar-lectura, con dependencias inyectadas (verificar usuario y registrar en la base)
// para poder probarlo sin Supabase. index.ts lo conecta con Deno.serve.
import { MAX_BYTES, validarLote, type ErrorEvento, type Evento } from "./contrato.ts";

export type Resultado = { id: string | null; estado: "guardado" | "repetido" | "conflicto" | "invalido"; error?: string };
export type Deps = {
  activa: boolean;
  verificarUsuario: (authorization: string) => Promise<string | null>; // id del usuario o null
  registrar: (usuario: string, eventos: Evento[]) => Promise<Resultado[]>;
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" } });

/** Respuestas: 200 {ok, resultados} · 400/413 error de forma (no reintentar) · 401 sin sesión · 405 · 503 reintentar. */
export function crearManejador(d: Deps) {
  return async (req: Request): Promise<Response> => {
    if (req.method !== "POST") return json({ ok: false, error: "metodo" }, 405);
    if (!d.activa) return json({ ok: false, error: "desactivado", reintentar: false }, 503);
    const auth = req.headers.get("Authorization") ?? "";
    if (!/^Bearer \S{20,}$/.test(auth)) return json({ ok: false, error: "sin_sesion" }, 401);
    const largo = Number(req.headers.get("Content-Length") ?? "0");
    if (largo > MAX_BYTES) return json({ ok: false, error: "demasiado_grande" }, 413);
    let usuario: string | null;
    try { usuario = await d.verificarUsuario(auth); } catch { return json({ ok: false, error: "auth_no_disponible", reintentar: true }, 503); }
    if (!usuario) return json({ ok: false, error: "sin_sesion" }, 401);
    let crudo: string;
    try { crudo = await req.text(); } catch { return json({ ok: false, error: "cuerpo" }, 400); }
    const lote = validarLote(crudo);
    if (!lote.ok) return json({ ok: false, error: lote.error }, lote.status);
    let guardados: Resultado[] = [];
    if (lote.validos.length) {
      try { guardados = await d.registrar(usuario, lote.validos); } catch {
        // La base no respondió: nada se confirmó; el teléfono reintenta y la idempotencia evita duplicados.
        return json({ ok: false, error: "base_no_disponible", reintentar: true }, 503);
      }
      if (!Array.isArray(guardados) || guardados.length !== lote.validos.length) {
        return json({ ok: false, error: "respuesta_base", reintentar: true }, 503);
      }
    }
    const resultados: (Resultado | ErrorEvento)[] = [...guardados, ...lote.invalidos];
    return json({ ok: true, resultados });
  };
}
