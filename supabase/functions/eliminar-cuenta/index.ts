// Elimina los datos de la cuenta autenticada. La identidad se obtiene del token,
// nunca de un correo enviado por el cliente. Los pagos se conservan anonimizados.
const SUPA = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICIO = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" },
  });

const headersServicio = {
  apikey: SERVICIO,
  Authorization: `Bearer ${SERVICIO}`,
  "Content-Type": "application/json",
};

async function borrar(ruta: string) {
  const r = await fetch(`${SUPA}/rest/v1/${ruta}`, {
    method: "DELETE",
    headers: { ...headersServicio, Prefer: "return=minimal" },
  });
  if (!r.ok) throw new Error(`delete_failed_${r.status}`);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "metodo_no_permitido" }, 405);

  const auth = req.headers.get("Authorization") ?? "";
  if (!auth.startsWith("Bearer ")) return json({ error: "sesion_requerida" }, 401);

  let usuario: { id?: string; email?: string };
  try {
    const r = await fetch(`${SUPA}/auth/v1/user`, {
      headers: { apikey: ANON, Authorization: auth },
    });
    if (!r.ok) return json({ error: "sesion_invalida" }, 401);
    usuario = await r.json();
  } catch {
    return json({ error: "no_se_pudo_validar_sesion" }, 503);
  }

  const id = String(usuario.id ?? "");
  const correo = String(usuario.email ?? "").trim().toLowerCase();
  if (!id || !correo) return json({ error: "sesion_invalida" }, 401);

  // Las operaciones son idempotentes. Se borra primero el perfil y se elimina
  // el usuario de Auth al final, para que un fallo parcial pueda reintentarse.
  try {
    const c = encodeURIComponent(correo);
    await borrar(`equipos?correo=eq.${c}`);
    await borrar(`ajustes?correo=eq.${c}`);
    await borrar(`codigos?correo=eq.${c}`);
    await borrar(`invitaciones?invitado=eq.${c}`);
    await borrar(`invitaciones?invitador=eq.${c}`);
    await borrar(`licencias?correo=eq.${c}`);

    // Conserva el registro mínimo de transacción, sin mantener el correo del usuario.
    const anonimizar = await fetch(`${SUPA}/rest/v1/pagos?correo=eq.${c}`, {
      method: "PATCH",
      headers: { ...headersServicio, Prefer: "return=minimal" },
      body: JSON.stringify({ correo: `eliminada+${id}@cuentas.invalid` }),
    });
    if (!anonimizar.ok) throw new Error(`payments_anonymize_failed_${anonimizar.status}`);

    const borrarAuth = await fetch(`${SUPA}/auth/v1/admin/users/${encodeURIComponent(id)}`, {
      method: "DELETE",
      headers: { apikey: SERVICIO, Authorization: `Bearer ${SERVICIO}` },
    });
    if (!borrarAuth.ok) throw new Error(`auth_delete_failed_${borrarAuth.status}`);

    return json({ ok: true, pagosAnonimizados: true });
  } catch {
    // No registrar correo, ID de usuario, token ni cuerpos de error que puedan incluir datos personales.
    return json({
      error: "eliminacion_incompleta",
      mensaje: "No se pudo completar la eliminación. Puedes volver a intentarlo o contactar soporte.",
    }, 500);
  }
});
