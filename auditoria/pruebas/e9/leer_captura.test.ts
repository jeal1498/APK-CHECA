// Pruebas de admin-leer-captura (manejador con dependencias simuladas). Ejecutar:
//   node --experimental-strip-types --no-warnings --test auditoria/pruebas/e9/
import { test } from "node:test";
import assert from "node:assert/strict";
import { crearManejador, normalizar, armarPeticion, base64, NOMBRE_OK, NOMBRE_DUDOSA, type Deps } from "../../../supabase/functions/admin-leer-captura/manejador.ts";

const AUTH = "Bearer " + "x".repeat(40);
const NOMBRE = "ok-rojo-45.62-0.8-3-1794984882409-n1.webp";
const IMG = new Uint8Array([82, 73, 70, 70, 1, 2, 3]);
function hacer(over: Partial<Deps> = {}) {
  const llamadas: unknown[] = [];
  const deps: Deps = {
    clave: "k", origenPermitido: "https://jeal1498.github.io",
    esAdministrador: async () => true,
    descargar: async () => IMG,
    preguntar: async (c) => { llamadas.push(c); return { status: 200, json: { choices: [{ message: { content: JSON.stringify({ estado: "vigente", tarifa: 45.62, km: 6.361, min: 19, confianza: "alta", nota: null }) } }], usage: { prompt_tokens: 700, completion_tokens: 40 } } }; },
    ahora: (() => { let t = 0; return () => (t += 120); })(),
    ...over,
  };
  return { h: crearManejador(deps), llamadas };
}
const pedir = (cuerpo: unknown, k: { metodo?: string; auth?: string } = {}) =>
  new Request("http://x/admin-leer-captura", { method: k.metodo ?? "POST", body: (k.metodo ?? "POST") === "GET" ? undefined : JSON.stringify(cuerpo), headers: { Authorization: k.auth ?? AUTH, "Content-Type": "application/json" } });

test("lee una captura y devuelve la sugerencia normalizada", async () => {
  const { h, llamadas } = hacer();
  const r = await h(pedir({ nombre: NOMBRE }));
  assert.equal(r.status, 200);
  const j = await r.json();
  assert.deepEqual(j.lectura, { estado: "vigente", tarifa: 45.62, km: 6.361, min: 19, confianza: "alta", nota: null });
  assert.equal(j.modelo, "deepseek-flash");
  assert.deepEqual(j.tokens, { entrada: 700, salida: 40 });
  assert.equal(llamadas.length, 1);
});

test("la petición lleva la imagen en base64 WebP, sin razonamiento y en JSON", () => {
  const p = armarPeticion(IMG) as any;
  assert.equal(p.model, "deepseek-flash");
  assert.deepEqual(p.thinking, { type: "disabled" });
  assert.deepEqual(p.response_format, { type: "json_object" });
  assert.equal(p.temperature, 0);
  const img = p.messages[1].content[1];
  assert.equal(img.type, "image_url");
  assert.equal(img.image_url.url, "data:image/webp;base64," + base64(IMG));
  assert.match(p.messages[0].content, /suma las distancias/);
  assert.match(p.messages[0].content, /Ignora el recuadro oscuro de Checa/);
});

test("solo administradores: sin sesión 401, no admin 403, y no llama a la IA", async () => {
  const a = hacer();
  assert.equal((await a.h(pedir({ nombre: NOMBRE }, { auth: "" }))).status, 401);
  const b = hacer({ esAdministrador: async () => false });
  assert.equal((await b.h(pedir({ nombre: NOMBRE }))).status, 403);
  assert.equal(b.llamadas.length, 0);
  const c = hacer({ esAdministrador: async () => { throw new Error("x"); } });
  assert.equal((await c.h(pedir({ nombre: NOMBRE }))).status, 503);
});

test("rechaza nombres que no son capturas (sin rutas ni caracteres raros)", async () => {
  for (const n of ["../x.webp", "ok-rojo-1-1-1-1.webp", "foo.webp", "ok-rojo-45.62-0.8-3-1794984882409-n1.webp/../../x", "", "123.png", "a/b.webp"]) {
    const { h, llamadas } = hacer();
    assert.equal((await h(pedir({ nombre: n }))).status, 400, n);
    assert.equal(llamadas.length, 0);
  }
  assert.ok(NOMBRE_OK.test(NOMBRE) && NOMBRE_DUDOSA.test("2165.webp") && NOMBRE_OK.test("ok-verde-45.62-0.8-3-1794984882409.webp"));
});

test("sin clave, método o cuerpo incorrectos", async () => {
  assert.equal((await hacer({ clave: "" }).h(pedir({ nombre: NOMBRE }))).status, 503);
  assert.equal((await hacer().h(pedir({}, { metodo: "GET" }))).status, 405);
  const mal = new Request("http://x", { method: "POST", body: "no json", headers: { Authorization: AUTH } });
  assert.equal((await hacer().h(mal)).status, 400);
  const o = await hacer().h(new Request("http://x", { method: "OPTIONS" }));
  assert.equal(o.status, 204);
  assert.equal(o.headers.get("Access-Control-Allow-Origin"), "https://jeal1498.github.io");
});

test("captura inexistente o muy grande", async () => {
  assert.equal((await hacer({ descargar: async () => null }).h(pedir({ nombre: NOMBRE }))).status, 404);
  assert.equal((await hacer({ descargar: async () => "grande" }).h(pedir({ nombre: NOMBRE }))).status, 413);
  assert.equal((await hacer({ descargar: async () => new Uint8Array(700_000) }).h(pedir({ nombre: NOMBRE }))).status, 413);
});

test("fallas de la IA se informan sin inventar datos", async () => {
  const f = async (status: number, json: unknown) => (await hacer({ preguntar: async () => ({ status, json }) }).h(pedir({ nombre: NOMBRE })));
  assert.equal((await f(402, {})).status, 502);                                              // sin saldo
  assert.equal((await f(200, { choices: [{ message: { content: "   " } }] })).status, 502);   // JSON vacío
  assert.equal((await f(200, { choices: [{ message: { content: "{\"estado\":\"quizas\"}" } }] })).status, 502);
  const r = await hacer({ preguntar: async () => { throw new Error("red"); } }).h(pedir({ nombre: NOMBRE }));
  assert.equal(r.status, 502);
});

test("normalizar: números dudosos quedan en null y el estado debe ser conocido", () => {
  assert.deepEqual(normalizar({ estado: "sin_oferta", tarifa: null, km: "x", min: -3, confianza: "rara", nota: "  " }),
    { estado: "sin_oferta", tarifa: null, km: null, min: null, confianza: "baja", nota: null });
  assert.equal(normalizar({ estado: "otro" }), null);
  assert.equal(normalizar("texto"), null);
  assert.equal(normalizar({ estado: "vigente", tarifa: "45,62", km: 6.3611111, minutos: 19 })!.km, 6.361);
  assert.equal(normalizar({ estado: "vigente", tarifa: 45.62, km: 6.3, minutos: 19 })!.min, 19);
});
