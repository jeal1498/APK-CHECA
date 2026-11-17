// registrar-lectura (E2). Recibe eventos de trazabilidad del teléfono (10.81+).
// Se despliega a mano con .github/workflows/auditoria-produccion.yml (aplica antes auditoria/sql/001–006).
// Interruptor: secreto AUDITORIA_ACTIVA = "1"; sin él responde 503 desactivado (el teléfono no reintenta).
// No toca confirmar-oferta: las versiones anteriores de la app siguen igual.
import { crearManejador } from "./manejador.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

Deno.serve(crearManejador({
  activa: Deno.env.get("AUDITORIA_ACTIVA") === "1",
  verificarUsuario: async (auth) => {
    const r = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
    if (r.status === 401 || r.status === 403) return null;
    if (!r.ok) throw new Error(`auth_${r.status}`);
    const u = await r.json();
    return typeof u?.id === "string" ? u.id : null;
  },
  registrar: async (usuario, eventos) => {
    const r = await fetch(`${SUPA}/rest/v1/rpc/auditoria_registrar_lote`, {
      method: "POST",
      headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}`, "Content-Type": "application/json" },
      body: JSON.stringify({ u: usuario, eventos }),
    });
    if (!r.ok) throw new Error(`rpc_${r.status}`);
    return await r.json();
  },
}));
