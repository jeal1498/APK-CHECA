// Aviso de Mercado Pago. Consulta el pago directo en Mercado Pago (así no se puede falsificar)
// y, si está aprobado, extiende y firma la licencia de la cuenta en la tabla `licencias`.
import { createHmac, createSign } from "node:crypto";
import { MARCA_BETA, PLANES } from "../_shared/planes.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const MP = Deno.env.get("MP_ACCESS_TOKEN")!;
const SECRETO = Deno.env.get("MP_WEBHOOK_SECRET") ?? "";
const LLAVE = atob(Deno.env.get("LICENCIA_PRIVADA_B64") ?? "");
const PERMANENTE = 99991231;
/** Días que gana cada uno cuando un amigo invitado hace su primer pago. */
const PREMIO_DIAS = 30;

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

/** Hoy en Cancún (UTC−5, sin horario de verano) como AAAAMMDD. */
function hoy(): number {
  return Number(new Date(Date.now() - 5 * 3600_000).toISOString().slice(0, 10).replaceAll("-", ""));
}

function sumarDias(aaaammdd: number, dias: number): number {
  const s = String(aaaammdd);
  const d = new Date(Date.UTC(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8)));
  d.setUTCDate(d.getUTCDate() + dias);
  return Number(d.toISOString().slice(0, 10).replaceAll("-", ""));
}

/** Firma ECDSA P-256 (DER, base64 url sin relleno), igual que el generador de GitHub. */
function firmar(texto: string): string {
  return createSign("SHA256").update(texto).sign(LLAVE).toString("base64")
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

/** Revisa la firma x-signature de Mercado Pago (solo se anota en el log). */
function firmaValida(req: Request, id: string): boolean {
  const h = req.headers.get("x-signature") ?? "";
  const partes = Object.fromEntries(h.split(",").map((p) => p.trim().split("=", 2)));
  if (!SECRETO || !partes.ts || !partes.v1) return false;
  const rid = req.headers.get("x-request-id");
  const idUrl = /^[a-z0-9]+$/i.test(id) ? id.toLowerCase() : id;
  const manifiesto = `id:${idUrl};` + (rid ? `request-id:${rid};` : "") + `ts:${partes.ts};`;
  return createHmac("sha256", SECRETO).update(manifiesto).digest("hex") === partes.v1;
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
  const url = new URL(req.url);
  const cuerpo = await req.json().catch(() => ({}));
  const tipo = cuerpo.type ?? url.searchParams.get("type") ?? url.searchParams.get("topic") ?? "";
  const id = String(url.searchParams.get("data.id") ?? cuerpo.data?.id ?? url.searchParams.get("id") ?? "");
  if (tipo !== "payment" || !/^\d+$/.test(id)) return new Response("ignorado");
  if (!firmaValida(req, id)) console.warn("x-signature no coincide para el pago", id);

  const r = await fetch(`https://api.mercadopago.com/v1/payments/${id}`, {
    headers: { Authorization: `Bearer ${MP}` },
  });
  if (!r.ok) return new Response("pago no encontrado", { status: r.status === 404 ? 200 : 502 });
  const pago = await r.json();
  if (pago.status !== "approved") return new Response("sin aprobar");

  const [correo, plan, marca] = String(pago.external_reference ?? "").split("|");
  const p = PLANES[plan];
  // El external_reference lo pone mp-pago en el servidor: «PF» = precio de fundador (beta).
  const precioBeta = marca === MARCA_BETA;
  const minimo = p ? (precioBeta ? p.precioBeta : p.precio) : 0;
  if (!correo || !p || pago.currency_id !== "MXN" || Number(pago.transaction_amount) < minimo) {
    console.error("pago con datos raros", id, pago.external_reference, pago.transaction_amount);
    return new Response("ignorado");
  }

  // Anotar el pago primero: si Mercado Pago repite el aviso, no se suma dos veces.
  const nuevo = await db("pagos", {
    method: "POST",
    headers: { Prefer: "resolution=ignore-duplicates,return=representation" },
    body: JSON.stringify({ id, correo, plan, monto: pago.transaction_amount, precio_beta: precioBeta }),
  });
  if (!nuevo.ok) return new Response("error al anotar", { status: 500 });
  if ((await nuevo.json()).length === 0) return new Response("ya aplicado");

  // ¿Es el primer pago de alguien que entró con el código de un amigo? Se «reclama» la invitación
  // marcándola como premiada (solo una vez); si algo falla abajo, se desmarca.
  let invitador: string | null = null;
  const previos = await db(`pagos?correo=eq.${encodeURIComponent(correo)}&select=id`);
  if (previos.ok && (await previos.json()).length === 1) {
    const inv = await db(`invitaciones?invitado=eq.${encodeURIComponent(correo)}&premiado=is.null`, {
      method: "PATCH",
      headers: { Prefer: "return=representation" },
      body: JSON.stringify({ premiado: new Date().toISOString() }),
    });
    if (inv.ok) invitador = (await inv.json())[0]?.invitador ?? null;
  }

  try {
    const lr = await db(`licencias?correo=eq.${encodeURIComponent(correo)}&select=*`);
    if (!lr.ok) throw new Error("leer licencia " + lr.status);
    const actual = (await lr.json())[0];
    const h = hoy();
    if (actual && actual.vence >= PERMANENTE) return new Response("sin vencimiento");

    const vigente = actual && actual.vence >= h;
    const vence = sumarDias(vigente ? actual.vence : h, p.dias + (invitador ? PREMIO_DIAS : 0));
    // 1 mes = 1 equipo, salvo que ya tenga vigente un paquete de 2 equipos.
    const basico = plan === "1m" && !(vigente && (actual.max_equipos ?? 2) >= 2);
    const fundador = actual?.fundador === true;
    const texto = `LV|${correo}|${vence}` + (fundador ? "|F" : "") + (basico ? "|B" : "");
    const fila = {
      correo,
      nombre: actual?.nombre ?? [pago.payer?.first_name, pago.payer?.last_name].filter(Boolean).join(" "),
      vence,
      fundador,
      firma: firmar(texto),
      max_equipos: basico ? 1 : 2,
      premios: (actual?.premios ?? 0) + (invitador ? 1 : 0),
    };
    const up = await db("licencias", {
      method: "POST",
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
      body: JSON.stringify(fila),
    });
    if (!up.ok) throw new Error("guardar licencia " + up.status + " " + await up.text());
    await db(`pagos?id=eq.${id}`, { method: "PATCH", body: JSON.stringify({ vence }) });
    if (invitador) await premiarInvitador(invitador, correo);
    return new Response("ok");
  } catch (e) {
    // Quitar la marca para que el reintento de Mercado Pago lo vuelva a intentar.
    console.error(e);
    await db(`pagos?id=eq.${id}`, { method: "DELETE" });
    if (invitador) {
      await db(`invitaciones?invitado=eq.${encodeURIComponent(correo)}`, {
        method: "PATCH", body: JSON.stringify({ premiado: null }),
      });
    }
    return new Response("error", { status: 500 });
  }
});
