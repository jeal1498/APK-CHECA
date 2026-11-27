// Avisos en tiempo real de Google Play (RTDN): renovaciones, cancelaciones, pagos fallidos y reembolsos.
// Google los manda a un tema de Pub/Sub y su suscripción «push» llama aquí (con ?k=SECRETO en la URL).
// Siempre se vuelve a preguntar a Google el estado real de la compra, así el aviso no se puede falsificar.
// Secretos: PLAY_RTDN_SECRETO, GOOGLE_PLAY_SA_B64 y LICENCIA_PRIVADA_B64.
import {
  consultarCompra,
  db,
  extenderLicencia,
  recortarLicencia,
  tokenGoogle,
} from "../_shared/play.ts";

const SECRETO = Deno.env.get("PLAY_RTDN_SECRETO") ?? "";

const resp = (texto: string, status = 200) => new Response(texto, { status });

Deno.serve(async (req) => {
  if (req.method !== "POST") return resp("metodo", 405);
  const k = new URL(req.url).searchParams.get("k") ?? "";
  if (!SECRETO || k !== SECRETO) return resp("no autorizado", 401);

  // Pub/Sub: {"message": {"data": "<base64 del aviso>"}}. Respuestas 2xx = recibido; 5xx = reintentar.
  let aviso: Record<string, any>;
  try {
    const cuerpo = await req.json();
    aviso = JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(cuerpo.message.data), (c) => c.charCodeAt(0))));
  } catch {
    return resp("mensaje raro"); // no sirve reintentarlo
  }
  if (aviso.testNotification) return resp("prueba ok");
  const token = String(
    aviso.subscriptionNotification?.purchaseToken ?? aviso.voidedPurchaseNotification?.purchaseToken ?? "",
  );
  if (!token) return resp("ignorado");

  try {
    // ¿De quién es? Solo se atienden compras que la app ya verificó (compras_play).
    const pr = await db(`compras_play?token=eq.${encodeURIComponent(token)}&select=correo,plan,expira`);
    if (!pr.ok) return resp("base", 500);
    const previa = (await pr.json())[0];
    if (!previa) return resp("compra desconocida");
    const correo = previa.correo as string;

    const acceso = await tokenGoogle();
    const { compra: c, codigo } = await consultarCompra(token, acceso);
    if (!c) return resp("google " + codigo, codigo >= 500 || codigo === 429 ? 500 : 200);

    await db(`compras_play?token=eq.${encodeURIComponent(token)}`, {
      method: "PATCH",
      body: JSON.stringify({ estado: c.estado, expira: new Date(c.expira).toISOString() }),
    });

    if (c.vigente) {
      // Renovación o recuperación: extender y anotar el pedido nuevo.
      await db("pagos", {
        method: "POST",
        headers: { Prefer: "resolution=ignore-duplicates,return=minimal" },
        body: JSON.stringify({ id: c.pedido, correo, plan: c.plan, monto: 0, precio_beta: c.fundador }),
      });
      const vence = await extenderLicencia(correo, null, c.plan, c.expira);
      await db(`pagos?id=eq.${encodeURIComponent(c.pedido)}`, { method: "PATCH", body: JSON.stringify({ vence }) });
      return resp("extendida");
    }
    // Reembolso, expiración o pago fallido sin recuperar: la licencia termina cuando dice Google.
    await recortarLicencia(correo, c.expira, Date.parse(previa.expira));
    return resp("recortada");
  } catch (e) {
    console.error(e);
    return resp("error", 500);
  }
});
