// Crea el pago en Mercado Pago para la cuenta que llama (con su sesión de Supabase)
// y devuelve el link de pago. La licencia la activa mp-webhook cuando el pago se aprueba.
import { PLANES } from "../_shared/planes.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const MP = Deno.env.get("MP_ACCESS_TOKEN")!;
const WEB = "https://jeal1498.github.io/checa-web/";

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "metodo" }, 405);
  const auth = req.headers.get("Authorization") ?? "";
  const u = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
  if (!u.ok) return json({ error: "sesion" }, 401);
  const correo = String((await u.json()).email ?? "").toLowerCase();
  if (!correo) return json({ error: "sesion" }, 401);

  const { plan } = await req.json().catch(() => ({}));
  const p = PLANES[plan];
  if (!p) return json({ error: "plan" }, 400);

  const r = await fetch("https://api.mercadopago.com/checkout/preferences", {
    method: "POST",
    headers: { Authorization: `Bearer ${MP}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      items: [{ id: plan, title: p.titulo, quantity: 1, unit_price: p.precio, currency_id: "MXN" }],
      payer: { email: correo },
      external_reference: `${correo}|${plan}`,
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
  return json({ url: pref.init_point });
});
