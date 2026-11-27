// Compra de Google Play (Fase 7). La app manda el token de la suscripción `checa_acceso`; aquí se
// consulta directo a Google (así no se puede falsificar), se confirma la compra (acknowledge) y se
// extiende y firma la licencia de la cuenta en la tabla `licencias`.
// Secretos: GOOGLE_PLAY_SA_B64 y LICENCIA_PRIVADA_B64 (ver _shared/play.ts).
import {
  confirmarCompra,
  consultarCompra,
  db,
  extenderLicencia,
  firmar,
  hoy,
  PERMANENTE,
  PREMIO_DIAS,
  sumarDias,
  tokenGoogle,
} from "../_shared/play.ts";
import { tienePrecioBeta } from "../_shared/fundador.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

/** 30 días más para quien invitó (las licencias sin vencimiento no cambian). Si falla, solo se anota en el log. */
async function premiarInvitador(invitador: string, amigo: string) {
  try {
    const lr = await db(`licencias?correo=eq.${encodeURIComponent(invitador)}&select=*`);
    if (!lr.ok) throw new Error("leer licencia " + lr.status);
    const actual = (await lr.json())[0];
    if (actual && actual.vence >= PERMANENTE) {
      await db(`licencias?correo=eq.${encodeURIComponent(invitador)}`, {
        method: "PATCH", body: JSON.stringify({ premios: (actual.premios ?? 0) + 1 }),
      });
      return;
    }
    const h = hoy();
    const vence = sumarDias(actual && actual.vence >= h ? actual.vence : h, PREMIO_DIAS);
    const fundador = actual?.fundador === true;
    const basico = (actual?.max_equipos ?? 2) === 1;
    const texto = `LV|${invitador}|${vence}` + (fundador ? "|F" : "") + (basico ? "|B" : "");
    const up = await db("licencias", {
      method: "POST",
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
      body: JSON.stringify({
        correo: invitador,
        nombre: actual?.nombre ?? null,
        vence,
        fundador,
        firma: firmar(texto),
        max_equipos: basico ? 1 : 2,
        premios: (actual?.premios ?? 0) + 1,
      }),
    });
    if (!up.ok) throw new Error("guardar licencia " + up.status + " " + await up.text());
  } catch (e) {
    console.error("no se pudo premiar a", invitador, "por", amigo, e);
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "metodo" }, 405);

  // Quién es: siempre del token de sesión, nunca de un correo mandado por la app.
  const auth = req.headers.get("Authorization") ?? "";
  const u = await fetch(`${SUPA}/auth/v1/user`, { headers: { apikey: ANON, Authorization: auth } });
  if (!u.ok) return json({ error: "sesion" }, 401);
  const usuario = await u.json();
  const correo = String(usuario.email ?? "").trim().toLowerCase();
  if (!correo) return json({ error: "sesion" }, 401);

  const cuerpo = await req.json().catch(() => ({}));
  const token = String(cuerpo.token ?? "");
  if (token.length < 20 || token.length > 1000) return json({ error: "token" }, 400);

  // Consulta a Google.
  let acceso: string;
  let c;
  try {
    acceso = await tokenGoogle();
    const r = await consultarCompra(token, acceso);
    if (!r.compra) {
      return json({ error: r.codigo === 422 ? "producto" : "google", codigo: r.codigo },
        r.codigo === 400 || r.codigo === 404 ? 404 : r.codigo === 422 ? 422 : 502);
    }
    c = r.compra;
  } catch (e) {
    console.error(e);
    return json({ error: "google" }, 502);
  }

  // Esta compra solo puede pertenecer a una cuenta.
  const prev = await db(`compras_play?token=eq.${encodeURIComponent(token)}&select=correo`);
  if (!prev.ok) return json({ error: "base" }, 500);
  const dueno = (await prev.json())[0]?.correo as string | undefined;
  if (dueno && dueno !== correo) return json({ error: "otra_cuenta" }, 409);

  if (!c.vigente) return json({ ok: false, estado: c.estado });

  // Plan «-fundador»: solo para cuentas con precio de fundador (no se da licencia a quien no lo tenga).
  if (c.fundador && !(await tienePrecioBeta(correo, String(usuario.created_at ?? "")))) {
    console.error("compra fundador sin derecho", correo, c.base);
    return json({ error: "fundador_no_aplica" }, 403);
  }

  if (c.porConfirmar && !(await confirmarCompra(token, acceso))) return json({ error: "confirmar" }, 502);

  await db("compras_play", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({ token, correo, plan: c.plan, estado: c.estado, expira: new Date(c.expira).toISOString() }),
  });

  // Anotar el pedido: cada renovación trae un pedido nuevo, y el mismo no se premia dos veces.
  const nuevo = await db("pagos", {
    method: "POST",
    headers: { Prefer: "resolution=ignore-duplicates,return=representation" },
    body: JSON.stringify({ id: c.pedido, correo, plan: c.plan, monto: 0, precio_beta: c.fundador }),
  });
  if (!nuevo.ok) return json({ error: "base" }, 500);
  const esNuevo = (await nuevo.json()).length > 0;

  // ¿Primer pago de alguien que entró con el código de un amigo? Se reclama la invitación una sola vez.
  let invitador: string | null = null;
  if (esNuevo) {
    const previos = await db(`pagos?correo=eq.${encodeURIComponent(correo)}&select=id`);
    if (previos.ok && (await previos.json()).length === 1) {
      const inv = await db(`invitaciones?invitado=eq.${encodeURIComponent(correo)}&premiado=is.null`, {
        method: "PATCH",
        headers: { Prefer: "return=representation" },
        body: JSON.stringify({ premiado: new Date().toISOString() }),
      });
      if (inv.ok) invitador = (await inv.json())[0]?.invitador ?? null;
    }
  }

  try {
    const vence = await extenderLicencia(
      correo,
      usuario.user_metadata?.full_name ?? null,
      c.plan,
      c.expira,
      invitador ? PREMIO_DIAS : 0,
      invitador ? 1 : 0,
    );
    await db(`pagos?id=eq.${encodeURIComponent(c.pedido)}`, { method: "PATCH", body: JSON.stringify({ vence }) });
    if (invitador) await premiarInvitador(invitador, correo);
    return json({ ok: true, vence, plan: c.plan });
  } catch (e) {
    // Quitar las marcas para que el reintento de la app lo vuelva a intentar.
    console.error(e);
    if (esNuevo) await db(`pagos?id=eq.${encodeURIComponent(c.pedido)}`, { method: "DELETE" });
    if (invitador) {
      await db(`invitaciones?invitado=eq.${encodeURIComponent(correo)}`, {
        method: "PATCH", body: JSON.stringify({ premiado: null }),
      });
    }
    return json({ error: "licencia" }, 500);
  }
});
