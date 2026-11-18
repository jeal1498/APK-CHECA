// admin-leer-captura: lee con DeepSeek (visión) la captura de una oferta, a petición de un administrador del Admin.
// Solo sugiere; el veredicto lo da una persona. No guarda nada. Secreto necesario: DEEPSEEK_API_KEY (el mismo de confirmar-oferta).
// Quién puede: cualquier administrador (public.es_administrador). La imagen sale del almacén privado «capturas» con la llave de servicio.
import { crearManejador } from "./manejador.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(crearManejador({
  clave: Deno.env.get("DEEPSEEK_API_KEY") ?? "",
  origenPermitido: "https://jeal1498.github.io",
  esAdministrador: async (auth) => {
    const r = await fetch(`${SUPA}/rest/v1/rpc/es_administrador`, { method: "POST", headers: { apikey: ANON, Authorization: auth, "Content-Type": "application/json" }, body: "{}" });
    if (r.status === 401 || r.status === 403) return false;
    if (!r.ok) throw new Error(`rpc_${r.status}`);
    return (await r.json()) === true;
  },
  descargar: async (nombre) => {
    const r = await fetch(`${SUPA}/storage/v1/object/capturas/${encodeURIComponent(nombre)}`, { headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}` } });
    if (r.status === 404 || r.status === 400) return null;
    if (!r.ok) throw new Error(`storage_${r.status}`);
    const largo = Number(r.headers.get("content-length") ?? 0);
    if (largo > 600_000) return "grande";
    return new Uint8Array(await r.arrayBuffer());
  },
  preguntar: async (cuerpo) => {
    const r = await fetch("https://api.deepseek.com/chat/completions", {
      method: "POST",
      headers: { Authorization: `Bearer ${Deno.env.get("DEEPSEEK_API_KEY") ?? ""}`, "Content-Type": "application/json" },
      signal: AbortSignal.timeout(30_000),
      body: JSON.stringify(cuerpo),
    });
    let json: unknown = null;
    try { json = await r.json(); } catch { /* sin cuerpo */ }
    return { status: r.status, json };
  },
}));
