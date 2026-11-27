// Piezas comunes de Google Play (Fase 7): play-verificar (compra desde la app) y play-notificaciones
// (avisos en tiempo real de Google). Secretos: GOOGLE_PLAY_SA_B64 (JSON de la cuenta de servicio,
// pegado tal cual o en base64) y LICENCIA_PRIVADA_B64.
import { createSign } from "node:crypto";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const LLAVE = atob(Deno.env.get("LICENCIA_PRIVADA_B64") ?? "");

export const PAQUETE = "com.jeal1498.checa";
export const PRODUCTO = "checa_acceso";
export const PERMANENTE = 99991231;
/** Días que gana cada uno cuando un amigo invitado hace su primer pago. */
export const PREMIO_DIAS = 30;
/** Plan básico de Play → paquete de Checa (los «-fundador» tienen el precio de fundador, −40 %). */
export const PLANES_PLAY: Record<string, { plan: string; fundador: boolean }> = {
  "mensual": { plan: "1m", fundador: false },
  "semestral": { plan: "6m", fundador: false },
  "anual": { plan: "12m", fundador: false },
  "mensual-fundador": { plan: "1m", fundador: true },
  "semestral-fundador": { plan: "6m", fundador: true },
  "anual-fundador": { plan: "12m", fundador: true },
};
/** Estados en los que la compra da derecho a usar Checa (CANCELED sigue vigente hasta su vencimiento). */
export const VIGENTES = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED",
]);

export const db = (ruta: string, init: RequestInit = {}) =>
  fetch(`${SUPA}/rest/v1/${ruta}`, {
    ...init,
    headers: {
      apikey: SERVICIO,
      Authorization: `Bearer ${SERVICIO}`,
      "Content-Type": "application/json",
      ...(init.headers ?? {}),
    },
  });

const b64url = (s: string) => btoa(s).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");

/** Firma ECDSA P-256 (DER, base64 url sin relleno), igual que el generador de GitHub. */
export function firmar(texto: string): string {
  return createSign("SHA256").update(texto).sign(LLAVE).toString("base64")
    .replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

/** AAAAMMDD en Cancún (UTC−5, sin horario de verano) de un instante en milisegundos. */
export function diaDe(ms: number): number {
  return Number(new Date(ms - 5 * 3600_000).toISOString().slice(0, 10).replaceAll("-", ""));
}

export const hoy = () => diaDe(Date.now());

export function sumarDias(aaaammdd: number, dias: number): number {
  const s = String(aaaammdd);
  const d = new Date(Date.UTC(+s.slice(0, 4), +s.slice(4, 6) - 1, +s.slice(6, 8)));
  d.setUTCDate(d.getUTCDate() + dias);
  return Number(d.toISOString().slice(0, 10).replaceAll("-", ""));
}

/** Token de acceso de la cuenta de servicio de Google (permiso androidpublisher). */
export async function tokenGoogle(): Promise<string> {
  const crudo = (Deno.env.get("GOOGLE_PLAY_SA_B64") ?? "").trim();
  // Acepta el JSON pegado tal cual o convertido a base64.
  const sa = JSON.parse(crudo.startsWith("{") ? crudo : atob(crudo));
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

/** Datos de la compra que dice Google (suscripción `checa_acceso`), o null si no es de este producto. */
export type Compra = {
  estado: string;
  base: string;
  plan: string;
  fundador: boolean;
  expira: number;
  pedido: string;
  porConfirmar: boolean;
  vigente: boolean;
};

export async function consultarCompra(token: string, acceso: string): Promise<{ compra: Compra | null; codigo: number }> {
  const r = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PAQUETE}/purchases/subscriptionsv2/tokens/${encodeURIComponent(token)}`,
    { headers: { Authorization: `Bearer ${acceso}` } },
  );
  if (!r.ok) {
    console.error("google", r.status, await r.text());
    return { compra: null, codigo: r.status };
  }
  const g = await r.json();
  const linea = (g.lineItems ?? []).find((l: { productId?: string }) => l.productId === PRODUCTO);
  const base = String(linea?.offerDetails?.basePlanId ?? "");
  const p = PLANES_PLAY[base];
  const expira = Date.parse(linea?.expiryTime ?? "");
  const pedido = String(linea?.latestSuccessfulOrderId ?? "");
  if (!linea || !p || !Number.isFinite(expira) || !pedido) return { compra: null, codigo: 422 };
  const estado = String(g.subscriptionState ?? "");
  return {
    codigo: 200,
    compra: {
      estado,
      base,
      plan: p.plan,
      fundador: p.fundador,
      expira,
      pedido,
      porConfirmar: g.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING",
      vigente: VIGENTES.has(estado) && expira > Date.now(),
    },
  };
}

/** Confirma la compra (si no, Google la reembolsa a los 3 días). */
export async function confirmarCompra(token: string, acceso: string): Promise<boolean> {
  const a = await fetch(
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PAQUETE}/purchases/subscriptions/${PRODUCTO}/tokens/${encodeURIComponent(token)}:acknowledge`,
    { method: "POST", headers: { Authorization: `Bearer ${acceso}`, "Content-Type": "application/json" }, body: "{}" },
  );
  if (!a.ok) console.error("acknowledge", a.status, await a.text());
  return a.ok;
}

/**
 * Extiende y firma la licencia de [correo] hasta el vencimiento que dice Google [expira] (ms). Si ya
 * tenía más días (regalo, premio de invitación) se conservan. [extraDias]/[premio]: premio de invitación.
 * Devuelve el AAAAMMDD que quedó (o PERMANENTE si la licencia no vence).
 */
export async function extenderLicencia(
  correo: string,
  nombre: string | null,
  plan: string,
  expira: number,
  extraDias = 0,
  premio = 0,
): Promise<number> {
  const lr = await db(`licencias?correo=eq.${encodeURIComponent(correo)}&select=*`);
  if (!lr.ok) throw new Error("leer licencia " + lr.status);
  const actual = (await lr.json())[0];
  if (actual && actual.vence >= PERMANENTE) return actual.vence;

  const h = hoy();
  const vigenteAntes = actual && actual.vence >= h;
  const base = Math.max(diaDe(expira), vigenteAntes ? actual.vence : 0);
  const vence = extraDias ? sumarDias(base, extraDias) : base;
  // 1 mes = 1 equipo, salvo que ya tenga vigente un paquete de 2 equipos.
  const basico = plan === "1m" && !(vigenteAntes && (actual.max_equipos ?? 2) >= 2);
  const fundador = actual?.fundador === true;
  const texto = `LV|${correo}|${vence}` + (fundador ? "|F" : "") + (basico ? "|B" : "");
  const up = await db("licencias", {
    method: "POST",
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    body: JSON.stringify({
      correo,
      nombre: actual?.nombre ?? nombre,
      vence,
      fundador,
      firma: firmar(texto),
      max_equipos: basico ? 1 : 2,
      premios: (actual?.premios ?? 0) + premio,
    }),
  });
  if (!up.ok) throw new Error("guardar licencia " + up.status + " " + await up.text());
  return vence;
}

/**
 * La compra dejó de valer (reembolso, cancelación inmediata, pago fallido): la licencia termina cuando
 * Google dice [expira] (ms). Solo se recorta si sus días venían de esa compra (no un regalo ni un premio).
 * [expiraAnterior] (ms) es lo que se había dado antes.
 */
export async function recortarLicencia(correo: string, expira: number, expiraAnterior: number): Promise<void> {
  const lr = await db(`licencias?correo=eq.${encodeURIComponent(correo)}&select=*`);
  if (!lr.ok) throw new Error("leer licencia " + lr.status);
  const actual = (await lr.json())[0];
  if (!actual || actual.vence >= PERMANENTE) return;
  if (actual.vence > sumarDias(diaDe(expiraAnterior), PREMIO_DIAS + 1)) return;
  const vence = Math.min(actual.vence, diaDe(Math.min(expira, Date.now())));
  if (vence === actual.vence) return;
  const basico = (actual.max_equipos ?? 2) === 1;
  const texto = `LV|${correo}|${vence}` + (actual.fundador ? "|F" : "") + (basico ? "|B" : "");
  const up = await db(`licencias?correo=eq.${encodeURIComponent(correo)}`, {
    method: "PATCH",
    body: JSON.stringify({ vence, firma: firmar(texto) }),
  });
  if (!up.ok) throw new Error("recortar licencia " + up.status + " " + await up.text());
}
