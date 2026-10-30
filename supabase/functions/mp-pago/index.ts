// Crea el pago en Mercado Pago para la cuenta que llama (con su sesión de Supabase)
// y devuelve el link de pago. La licencia la activa mp-webhook cuando el pago se aprueba.
// Con {"consulta": true} solo dice si la cuenta tiene precio de fundador (uno de los 50 lugares; si aún
// hay, la consulta lo reserva) y los precios que le tocan.
import { BETA_FIN, BETA_GRACIA_DIAS, BETA_PRIMER_PAGO_HASTA, FUNDADOR_CUPOS, MARCA_BETA, PLANES } from "../_shared/planes.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const MP = Deno.env.get("MP_ACCESS_TOKEN")!;
const WEB = "https://jeal1498.github.io/PWA-Checa-Multimedia/";

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });

const db = (ruta: string) =>
  fetch(`${SUPA}/rest/v1/${ruta}`, { headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}` } });

/**
 * ¿La cuenta tiene un lugar del Plan Fundador (50 en total)? Con crear = true toma uno si aún hay;
 * la función de la base de datos lo hace con candado para que nunca pasen de 50.
 */
async function tieneCupo(correo: string, crear: boolean): Promise<boolean> {
  try {
    const r = await fetch(`${SUPA}/rest/v1/rpc/reclamar_fundador`, {
      method: "POST",
      headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}`, "Content-Type": "application/json" },
      body: JSON.stringify({ p_correo: correo, p_cupos: FUNDADOR_CUPOS, p_crear: crear }),
    });
    if (!r.ok) {
      console.error("cupo fundador", r.status, await r.text());
      return false;
    }
    return (await r.json()) === true;
  } catch (e) {
    console.error("cupo fundador", correo, e);
    return false;
  }
}

/** Fecha AAAAMMDD en Cancún (UTC−5), corrida [dias] días desde hoy. */
function diaCancun(dias = 0): number {
  return Number(new Date(Date.now() - 5 * 3600_000 + dias * 86_400_000).toISOString().slice(0, 10).replaceAll("-", ""));
}

/**
 * Precio de fundador: cuenta creada durante la beta que tiene uno de los 50 lugares y (primer pago
 * antes del 28 feb 2027, o ya pagó antes con ese precio y su licencia no lleva más de 30 días vencida).
 * Después del 28 feb ya no se toman lugares nuevos: solo conserva el suyo quien ya lo tiene.
 */
async function tienePrecioBeta(correo: string, creada: string): Promise<boolean> {
  const t = Date.parse(creada);
  if (!(t < BETA_FIN)) return false;
  if (Date.now() < BETA_PRIMER_PAGO_HASTA) return await tieneCupo(correo, true);
  if (!(await tieneCupo(correo, false))) return false;
  try {
    const c = encodeURIComponent(correo);
    const p = await db(`pagos?correo=eq.${c}&precio_beta=is.true&select=id&limit=1`);
    if (!p.ok || (await p.json()).length === 0) return false;
    const l = await db(`licencias?correo=eq.${c}&select=vence`);
    const vence = l.ok ? (await l.json())[0]?.vence ?? 0 : 0;
    return vence >= diaCancun(-BETA_GRACIA_DIAS);
  } catch (e) {
    console.error("precio beta", correo, e);
    return false;
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "metodo" }, 405);
  const auth = req.headers.get("Authorization") ?? "";
  const u = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
  if (!u.ok) return json({ error: "sesion" }, 401);
  const usuario = await u.json();
  const correo = String(usuario.email ?? "").toLowerCase();
  if (!correo) return json({ error: "sesion" }, 401);

  const cuerpo = await req.json().catch(() => ({}));
  const beta = await tienePrecioBeta(correo, String(usuario.created_at ?? ""));
  if (cuerpo.consulta === true) {
    const precios = Object.fromEntries(Object.entries(PLANES).map(([k, v]) => [k, beta ? v.precioBeta : v.precio]));
    return json({ beta, precios });
  }

  const plan = cuerpo.plan;
  const p = PLANES[plan];
  if (!p) return json({ error: "plan" }, 400);
  const precio = beta ? p.precioBeta : p.precio;

  const r = await fetch("https://api.mercadopago.com/checkout/preferences", {
    method: "POST",
    headers: { Authorization: `Bearer ${MP}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      items: [{ id: plan, title: p.titulo + (beta ? " · precio de fundador" : ""), quantity: 1, unit_price: precio, currency_id: "MXN" }],
      payer: { email: correo },
      external_reference: `${correo}|${plan}` + (beta ? `|${MARCA_BETA}` : ""),
      notification_url: `${SUPA}/functions/v1/mp-webhook`,
      back_urls: { success: WEB, pending: WEB, failure: WEB },
      auto_return: "approved",
      statement_descriptor: "CHECA",
    }),
  });
  const pref = await r.json();
  if (!r.ok || !pref.init_point) {
    console.error("preferencia", r.status, JSON.stringify(pref));
    return json({ error: "mercadopago" }, 502);
  }
  return json({ url: pref.init_point, beta, precio });
});
