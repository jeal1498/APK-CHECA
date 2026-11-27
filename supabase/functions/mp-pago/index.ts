// Plan Fundador: con {"consulta": true} dice si la cuenta que llama (con su sesión de Supabase) tiene
// precio de fundador (uno de los 50 lugares; si aún hay, la consulta lo reserva) y los precios que le tocan.
// El nombre se queda por compatibilidad con la app. Mercado Pago se quitó el 25 nov 2026: los cobros
// serán solo con Google Play (Fase 7); cualquier otra petición responde 410.
import { PLANES } from "../_shared/planes.ts";
import { tienePrecioBeta } from "../_shared/fundador.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "metodo" }, 405);
  const auth = req.headers.get("Authorization") ?? "";
  const u = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
  if (!u.ok) return json({ error: "sesion" }, 401);
  const usuario = await u.json();
  const correo = String(usuario.email ?? "").toLowerCase();
  if (!correo) return json({ error: "sesion" }, 401);

  const cuerpo = await req.json().catch(() => ({}));
  if (cuerpo.consulta !== true) return json({ error: "sin_cobro", detalle: "Los pagos son solo con Google Play." }, 410);
  const beta = await tienePrecioBeta(correo, String(usuario.created_at ?? ""));
  const precios = Object.fromEntries(Object.entries(PLANES).map(([k, v]) => [k, beta ? v.precioBeta : v.precio]));
  return json({ beta, precios });
});
