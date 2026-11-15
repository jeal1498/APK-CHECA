// Pruebas de E2 (contrato y manejador) con node:test. Ejecutar:
//   node --experimental-strip-types --no-warnings --test auditoria/pruebas/e2/
import { test } from "node:test";
import assert from "node:assert/strict";
import { validarEvento, validarLote, MAX_BYTES } from "../../../supabase/functions/registrar-lectura/contrato.ts";
import { crearManejador, type Resultado } from "../../../supabase/functions/registrar-lectura/manejador.ts";
import { COHERENCIA, evento, U } from "./casos.ts";

const AUTH = "Bearer " + "x".repeat(40);
function req(cuerpo: unknown, k: { metodo?: string; auth?: string } = {}) {
  const b = typeof cuerpo === "string" ? cuerpo : JSON.stringify(cuerpo);
  return new Request("http://x/registrar-lectura", {
    method: k.metodo ?? "POST", body: (k.metodo ?? "POST") === "GET" ? undefined : b,
    headers: { Authorization: k.auth ?? AUTH, "Content-Type": "application/json" },
  });
}
function base(over: Partial<Parameters<typeof crearManejador>[0]> = {}) {
  const guardados = new Map<string, string>();
  const llamadas: unknown[][] = [];
  const h = crearManejador({
    activa: true,
    verificarUsuario: async (a) => (a === AUTH ? U : null),
    registrar: async (u, evs) => {
      llamadas.push([u, evs]);
      return evs.map((e): Resultado => {
        const s = JSON.stringify(e);
        if (!guardados.has(e.id)) { guardados.set(e.id, s); return { id: e.id, estado: "guardado" }; }
        return { id: e.id, estado: guardados.get(e.id) === s ? "repetido" : "conflicto" };
      });
    },
    ...over,
  });
  return { h, guardados, llamadas };
}

for (const [nombre, k, valido] of COHERENCIA) {
  test(`coherencia: ${nombre}`, () => {
    const err = validarEvento(evento(k));
    assert.equal(err === null, valido, `error=${err}`);
  });
}

test("campos desconocidos se rechazan", () => {
  assert.match(validarEvento(evento({ direccion: "Calle 1" }))!, /^campo_desconocido/);
  assert.match(validarEvento(evento({ etapas: [{ etapa: "ocr", estado: "ok", texto: "x" }] }))!, /etapas/);
});

test("líneas con correo, teléfono o URL se rechazan (redacción en profundidad)", () => {
  for (const l of ["juan@correo.com", "Tel 55 1234 5678", "https://x.y/z"]) {
    assert.match(validarEvento(evento({ lineas_redactadas: ["$50.00", l] }))!, /lineas/, l);
  }
  for (const l of ["$151.34", "37min (13.2km)", "A 5 min (1.6 km)", "⚡1.4x", "$10.15/km est."]) {
    assert.equal(validarEvento(evento({ lineas_redactadas: [l] })), null, l);
  }
});

test("lote: forma y límites", () => {
  assert.deepEqual(validarLote("{"), { ok: false, status: 400, error: "json_invalido" });
  assert.equal((validarLote(JSON.stringify({ eventos: [evento()] })) as { error: string }).error, "contrato_no_soportado");
  assert.equal((validarLote(JSON.stringify({ contrato: 2, eventos: [evento()] })) as { error: string }).error, "contrato_no_soportado");
  assert.equal((validarLote(JSON.stringify({ contrato: 1, eventos: [] })) as { error: string }).error, "eventos");
  assert.equal((validarLote(JSON.stringify({ contrato: 1, eventos: Array.from({ length: 51 }, () => evento()) })) as { error: string }).error, "eventos");
  assert.equal((validarLote(JSON.stringify({ contrato: 1, eventos: [evento()], extra: 1 })) as { error: string }).error, "campo_desconocido");
  assert.equal((validarLote("x".repeat(MAX_BYTES + 1)) as { status: number }).status, 413);
});

test("lote: id repetido dentro del mismo lote", () => {
  const e = evento();
  const r = validarLote(JSON.stringify({ contrato: 1, eventos: [e, e] }));
  assert.ok(r.ok);
  if (r.ok) { assert.equal(r.validos.length, 1); assert.equal(r.invalidos[0].error, "id_repetido_en_lote"); }
});

test("manejador: método, sesión, desactivado", async () => {
  const { h } = base();
  assert.equal((await h(req({}, { metodo: "GET" }))).status, 405);
  assert.equal((await h(req({ contrato: 1, eventos: [evento()] }, { auth: "Bearer corto" }))).status, 401);
  assert.equal((await h(req({ contrato: 1, eventos: [evento()] }, { auth: "Bearer " + "y".repeat(40) }))).status, 401);
  const off = base({ activa: false }).h;
  const r = await off(req({ contrato: 1, eventos: [evento()] }));
  assert.equal(r.status, 503);
  assert.equal((await r.json()).reintentar, false);
});

test("manejador: guarda, reintento idempotente y conflicto", async () => {
  const { h, guardados } = base();
  const e = evento();
  const r1 = await (await h(req({ contrato: 1, eventos: [e] }))).json();
  assert.deepEqual(r1.resultados, [{ id: e.id, estado: "guardado" }]);
  const r2 = await (await h(req({ contrato: 1, eventos: [e] }))).json();
  assert.equal(r2.resultados[0].estado, "repetido");
  const r3 = await (await h(req({ contrato: 1, eventos: [{ ...e, tarifa: 999 }] }))).json();
  assert.equal(r3.resultados[0].estado, "conflicto");
  assert.equal(guardados.size, 1);
});

test("manejador: lote mixto, solo los válidos llegan a la base", async () => {
  const { h, llamadas } = base();
  const a = evento(), b = evento({ app: "x" });
  const r = await (await h(req({ contrato: 1, eventos: [a, b] }))).json();
  assert.equal(r.ok, true);
  assert.deepEqual(r.resultados.map((x: { estado: string }) => x.estado), ["guardado", "invalido"]);
  assert.equal((llamadas[0][1] as unknown[]).length, 1);
  assert.equal(llamadas[0][0], U); // el usuario sale del JWT, nunca del cuerpo
});

test("manejador: todo inválido no llama a la base", async () => {
  const { h, llamadas } = base();
  const r = await (await h(req({ contrato: 1, eventos: [evento({ app: "x" })] }))).json();
  assert.equal(r.ok, true);
  assert.equal(llamadas.length, 0);
});

test("manejador: base caída → 503 reintentar, sin confirmar nada", async () => {
  const { h } = base({ registrar: async () => { throw new Error("caída"); } });
  const r = await h(req({ contrato: 1, eventos: [evento()] }));
  assert.equal(r.status, 503);
  assert.equal((await r.json()).reintentar, true);
  const { h: h2 } = base({ registrar: async () => [] });
  assert.equal((await h2(req({ contrato: 1, eventos: [evento()] }))).status, 503);
});

test("manejador: auth caída → 503 reintentar", async () => {
  const { h } = base({ verificarUsuario: async () => { throw new Error("x"); } });
  assert.equal((await h(req({ contrato: 1, eventos: [evento()] }))).status, 503);
});

test("manejador: cuerpo grande por Content-Length → 413", async () => {
  const { h } = base();
  const r = new Request("http://x", { method: "POST", body: "{}", headers: { Authorization: AUTH, "Content-Length": String(MAX_BYTES + 1) } });
  assert.equal((await h(r)).status, 413);
});

test("compatibilidad: confirmar-oferta no se modificó (las apps anteriores siguen igual)", async () => {
  const { execFileSync } = await import("node:child_process");
  const d = execFileSync("git", ["diff", "--stat", "a7a732fb3fc874e42cda16b79592137a241bc9d9", "--", "supabase/functions/confirmar-oferta", "supabase/esquema.sql"], { encoding: "utf8" });
  assert.equal(d.trim(), "");
});
