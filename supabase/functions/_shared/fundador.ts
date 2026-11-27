// Plan Fundador: ¿esta cuenta tiene precio de fundador? Lo usan mp-pago (consulta de la app) y
// play-verificar (para no dar licencia a quien compra un plan «-fundador» sin tener derecho).
import { BETA_FIN, BETA_GRACIA_DIAS, BETA_PRIMER_PAGO_HASTA, FUNDADOR_CUPOS } from "./planes.ts";

const SUPA = Deno.env.get("SUPABASE_URL")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const db = (ruta: string) =>
  fetch(`${SUPA}/rest/v1/${ruta}`, { headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}` } });

/**
 * ¿La cuenta tiene un lugar del Plan Fundador (50 en total)? Con crear = true toma uno si aún hay;
 * la función de la base de datos lo hace con candado para que nunca pasen de 50.
 */
export async function tieneCupo(correo: string, crear: boolean): Promise<boolean> {
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
export async function tienePrecioBeta(correo: string, creada: string): Promise<boolean> {
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
