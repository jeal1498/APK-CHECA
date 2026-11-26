// Compra de Google Play (Fase 7). La app manda el token de la suscripción `checa_acceso`; aquí se
// consulta directo a Google (así no se puede falsificar), se confirma la compra (acknowledge) y se
// extiende y firma la licencia de la cuenta en la tabla `licencias`.
// Secretos: GOOGLE_PLAY_SA_B64 (JSON de la cuenta de servicio en base64) y LICENCIA_PRIVADA_B64.
import { createSign } from "node:crypto";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const LLAVE = atob(Deno.env.get("LICENCIA_PRIVADA_B64") ?? "");
const PAQUETE = "com.jeal1498.checa";
const PRODUCTO = "checa_acceso";
const PERMANENTE = 99991231;
/** Días que gana cada uno cuando un amigo invitado hace su primer pago. */
const PREMIO_DIAS = 30;
/** Plan básico de Play → paquete de Checa. */
const PLANES: Record<string, string> = { mensual: "1m", semestral: "6m", anual: "12m" };
/** Estados en los que la compra da derecho a usar Checa (CANCELED sigue vigente hasta su vencimiento). */
const VIGENTES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED",
]);

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

const db = (ruta: string, init: RequestInit = {}) =>
  fetch(`${SUPA}/rest/v1/${ruta}`, {
    ...init,
    headers: {
      apikey: SERVICIO,
      Authorization: `Bearer ${SERVICIO}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });

const b64url = (s: string | Uint8Array) =>
  (typeof s === "string" ? btoa(s) : btoa(String.fromCharCode(...s)))
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");

/** Firma ECDSA P-256 (DER, base64 url sin relleno), igual que el generador de GitHub. */
function firmar(texto: string): string {
  return createSign("SHA256").update(texto).sign(LLAVE).toString("base64")
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

/** Hoy en Cancún (UTC−5, sin horario de verano) como AAAAMMDD. */
function hoy(): number {
  return Number(new Date(Date.now() - 5 * 3600_000).toISOString().slice(0, 10).replaceAll("-", ""));
}

function diaCancun(ms: number): number {
  return Number(new Date(ms - 5 * 3600_000).toISOString().slice(0, 10).replaceAll("-", ""));
}

function sumarDias(aaaammdd: number, dias: number): number {
  const s = String(aaaammdd);
  const d = new Date(Date.UTC(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8)));
  d.setUTCDate(d.getUTCDate() + dias);
  return Number(d.toISOString().slice(0, 10).replaceAll("-", ""));
}

/** Token de acceso de la cuenta de servicio de Google (permiso androidpublisher). */
async function tokenGoogle(): Promise<string> {
  const sa = JSON.parse(atob(Deno.env.get("GOOGLE_PLAY_SA_B64") ?? ""));
  const ahora = Math.floor(Date.now() / 1000);
  const cab = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const cuerpo = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    iat: ahora,
    exp: ahora + 3000,
  }));
  const firma = createSign("RSA-SHA256").update(`${cab}.${cuerpo}`).sign(sa.private_key).toString("base64")
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${cab}.${cuerpo}.${firma}`,
    }),
  });
  if (!r.ok) throw new Error("token google " + r.status + " " + await r.text());
  return (await r.json()).access_token;
}

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
  const compra = String(cuerpo.token ?? "");
  if (compra.length < 20 || compra.length > 1000) return json({ error: "token" }, 400);

  // Consulta a Google.
  let g: any;
  let acceso: string;
  try {
    acceso = await tokenGoogle();
    const r = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PAQUETE}/purchases/subscriptionsv2/tokens/${encodeURIComponent(compra)}`,
      { headers: { Authorization: `Bearer ${acceso}` } },
    );
    if (!r.ok) {
      console.error("google", r.status, await r.text());
      return json({ error: "google", codigo: r.status }, r.status === 400 || r.status === 404 ? 404 : 502);
    }
    g = await r.json();
  } catch (e) {
    console.error(e);
    return json({ error: "google" }, 502);
  }

  const linea = (g.lineItems ?? []).find((l: any) => l.productId === PRODUCTO);
  const base = String(linea?.offerDetails?.basePlanId ?? "");
  const plan = PLANES[base];
  const expira = Date.parse(linea?.expiryTime ?? "");
  const pedido = String(linea?.latestSuccessfulOrderId ?? "");
  if (!linea || !plan || !Number.isFinite(expira) || !pedido) return json({ error: "producto" }, 422);

  // Esta compra solo puede pertenecer a una cuenta.
  const prev = await db(`compras_play?token=eq.${encodeURIComponent(compra)}&select=correo`);
  if (!prev.ok) return json({ error: "base" }, 500);
  const dueno = (await prev.json())[0]?.correo as string | undefined;
  if (dueno && dueno !== correo) return json({ error: "otra_cuenta" }, 409);

  const estado = String(g.subscriptionState ?? "");
  const vigente = VIGENTES.has(estado) && expira > Date.now();
  if (!vigente) return json({ ok: false, estado });

  // Confirmar la compra (si no, Google la reembolsa a los 3 días). Si falla, el reintento lo vuelve a hacer.
  if (g.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    const a = await fetch(
      `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PAQUETE}/purchases/subscriptions/${PRODUCTO}/tokens/${encodeURIComponent(compra)}:acknowledge`,
      { method: "POST", headers: { Authorization: `Bearer ${acceso}`, "Content-Type": "application/json" }, body: "{}" },
    );
    if (!a.ok) {
      console.error("acknowledge", a.status, await a.text());
      return json({ error: "confirmar" }, 502);
    }
  }

  await db("compras_play", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({ token: compra, correo, plan, estado, expira: new Date(expira).toISOString() }),
  });

  // Anotar el pedido: cada renovación trae un pedido nuevo, y el mismo no se premia dos veces.
  const nuevo = await db("pagos", {
    method: "POST",
    headers: { Prefer: "resolution=ignore-duplicates,return=representation" },
    body: JSON.stringify({ id: pedido, correo, plan, monto: 0, precio_beta: false }),
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
    const lr = await db(`licencias?correo=eq.${encodeURIComponent(correo)}&select=*`);
    if (!lr.ok) throw new Error("leer licencia " + lr.status);
    const actual = (await lr.json())[0];
    if (actual && actual.vence >= PERMANENTE) return json({ ok: true, vence: actual.vence });

    const h = hoy();
    const vigenteAntes = actual && actual.vence >= h;
    // Google dice hasta cuándo está pagado; si ya había más días (regalo, premio), se conservan.
    const venceGoogle = diaCancun(expira);
    const vence = Math.max(venceGoogle, vigenteAntes ? actual.vence : 0);
    const conPremio = invitador ? sumarDias(vence, PREMIO_DIAS) : vence;
    // 1 mes = 1 equipo, salvo que ya tenga vigente un paquete de 2 equipos.
    const basico = plan === "1m" && !(vigenteAntes && (actual.max_equipos ?? 2) >= 2);
    const fundador = actual?.fundador === true;
    const texto = `LV|${correo}|${conPremio}` + (fundador ? "|F" : "") + (basico ? "|B" : "");
    const up = await db("licencias", {
      method: "POST",
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
      body: JSON.stringify({
        correo,
        nombre: actual?.nombre ?? (usuario.user_metadata?.full_name ?? null),
        vence: conPremio,
        fundador,
        firma: firmar(texto),
        max_equipos: basico ? 1 : 2,
        premios: (actual?.premios ?? 0) + (invitador ? 1 : 0),
      }),
    });
    if (!up.ok) throw new Error("guardar licencia " + up.status + " " + await up.text());
    await db(`pagos?id=eq.${encodeURIComponent(pedido)}`, { method: "PATCH", body: JSON.stringify({ vence: conPremio }) });
    if (invitador) await premiarInvitador(invitador, correo);
    return json({ ok: true, vence: conPremio, plan });
  } catch (e) {
    // Quitar las marcas para que el reintento de la app lo vuelva a intentar.
    console.error(e);
    if (esNuevo) await db(`pagos?id=eq.${encodeURIComponent(pedido)}`, { method: "DELETE" });
    if (invitador) {
      await db(`invitaciones?invitado=eq.${encodeURIComponent(correo)}`, {
        method: "PATCH", body: JSON.stringify({ premiado: null }),
      });
    }
    return json({ error: "licencia" }, 500);
  }
});
