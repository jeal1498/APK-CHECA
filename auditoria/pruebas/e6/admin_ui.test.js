// E6/E7 — pruebas de la página del Admin con Playwright y un cliente Supabase SIMULADO (no hay red ni Supabase).
// Ejecutar: NODE_PATH=$(npm root -g) node --test auditoria/pruebas/e6/admin_ui.test.js
const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const { chromium } = require("playwright");

const ADMIN = path.resolve(__dirname, "../../../admin");
let srv, url, nav;

before(async () => {
  srv = http.createServer((req, res) => {
    const f = path.join(ADMIN, decodeURIComponent(req.url.split("?")[0]).replace(/^\/+/, "") || "index.html");
    if (!f.startsWith(ADMIN) || !fs.existsSync(f)) { res.writeHead(404); return res.end(); }
    const tipo = { ".html": "text/html", ".js": "text/javascript", ".css": "text/css" }[path.extname(f)] || "text/plain";
    res.writeHead(200, { "Content-Type": tipo + "; charset=utf-8" }); res.end(fs.readFileSync(f));
  });
  await new Promise((r) => srv.listen(0, "127.0.0.1", r));
  url = `http://127.0.0.1:${srv.address().port}/index.html`;
  nav = await chromium.launch();
});
after(async () => { await nav.close(); srv.close(); });

// Fábrica del cliente simulado: corre DENTRO de la página. `escenario` decide qué responde cada RPC.
function clienteSimulado(escenario) {
  const XSS = '<img src=x onerror="window.__xss=1">';
  const ofertas = Array.from({ length: 50 }, (_, i) => ({ id: "id" + i, ts_evento: "2026-11-15T10:00:00Z", app: i % 2 ? "didi" : "uber",
    version_app: "10.81", lector: "ocr", resultado: "oferta", motivo: i === 0 ? XSS : null, tarifa: 100 + i, km: 5, min: 12,
    nivel: "verde", captura_vinculo: "exacto", estado_veredicto: "sin_veredicto", referencia: null,
    etapas: [{ etapa: "captura", estado: "ok" }], conductor: "abcd1234" }));
  const respuestas = {
    admin_inicio: { lecturas: 15, conductores: 2, errores_lector: 40, errores_sin_veredicto_humano: 40, reintentos_recibidos: 1,
      fallas_ia: 0, ultima_recepcion: "2026-11-15T10:00:00Z", por_resultado: { oferta: 12, duplicada: 3 }, alertas: ["conflictos_humanos"] },
    admin_errores: { total: 1, pagina: 1, por_pagina: 50, filas: [{ id: 9001, creado: "2026-11-14T12:00:00Z", app: "DiDi", version: "10.77",
      tipo: "diferencia", lector_lectura: { tarifa: 50, km: 5, min: 12 }, ia_lectura: { tarifa: 55, km: 5, min: 12 },
      estado_veredicto: "heredado_no_revisado", referencia: null, tiene_captura: true }] },
    admin_ofertas: (a) => ({ total: 120, pagina: a.pagina, por_pagina: 50, filas: ofertas }),
    admin_metricas: { metricas: [
      { clave: "lecturas", grupo: "operacion", nombre: "Eventos de lectura", formula: "count(*)", poblacion: "p", fuente: "f", k: 15, n: 15, valor: 15, estado: "insuficiente" },
      { clave: "precision_humana", grupo: "calidad_humana", nombre: "Precisión de pop-ups (humana)", formula: "x", poblacion: "p", fuente: "f", k: 0, n: 0, valor: null, estado: "sin_datos" },
      { clave: "recall", grupo: "calidad_humana", nombre: "Ofertas reales que el lector no mostró", formula: "NO MEDIBLE", poblacion: "p", fuente: "—", k: null, n: null, valor: null, estado: "no_medible", motivo: "sin denominador" },
      { clave: "acuerdo_ia_lector", grupo: "acuerdo_ia", nombre: "Acuerdo IA/lector (NO es exactitud)", formula: "x", poblacion: "p", fuente: "f", k: 90, n: 100, valor: 0.9, estado: "suficiente" }],
      segmentos: [{ app: "uber", version: "10.81", fabricante: "samsung", n: 12, ofertas: 10, abstenciones: 1, suficiente: false }], tendencia: [] },
    admin_bitacora: { filas: [{ creado: "2026-11-15T10:00:00Z", admin: "abcd1234", accion: "inicio", resultado: "ok", filas: 1 }] },
  };
  window.__llamadas = [];
  window.CHECA_CLIENTE = {
    auth: { getSession: async () => ({ data: { session: escenario === "sin_sesion" ? null : { user: {} } } }),
            signInWithOAuth: async (o) => { window.__login = o.provider; }, signOut: async () => {} },
    rpc: async (fn, args) => {
      window.__llamadas.push([fn, args]);
      if (escenario === "no_autorizado") return { data: { error: "no_autorizado" }, error: null };
      if (escenario === "red") throw new Error("red caída");
      if (escenario === "error_servidor") return { data: null, error: { message: "boom" } };
      const r = respuestas[fn];
      return { data: typeof r === "function" ? r(args) : r, error: null };
    },
  };
}

async function abrir(escenario, ancho = 1200) {
  const p = await nav.newPage({ viewport: { width: ancho, height: 800 } });
  const errores = [];
  p.on("pageerror", (e) => errores.push(String(e)));
  await p.addInitScript(clienteSimulado, escenario);
  await p.goto(url);
  return { p, errores };
}
const llamadas = (p) => p.evaluate(() => window.__llamadas);

test("sin sesión: solo el botón de entrar, ninguna consulta", async () => {
  const { p } = await abrir("sin_sesion");
  await p.click("#btn-entrar");
  assert.equal(await p.evaluate(() => window.__login), "google");
  assert.deepEqual(await llamadas(p), []);
  await p.close();
});

test("inicio muestra los números y las alertas", async () => {
  const { p, errores } = await abrir("ok");
  await p.waitForSelector("#inicio");
  assert.match(await p.textContent("#inicio"), /Lecturas15/);
  assert.match(await p.textContent("#alertas"), /conflictos_humanos/);
  assert.deepEqual(errores, []);
  await p.close();
});

test("errores: los filtros llegan a la función", async () => {
  const { p } = await abrir("ok");
  await p.click("#t-errores");
  await p.waitForSelector("#vista-errores table");
  await p.selectOption("#f-app", "didi");
  await p.fill("#f-version", "10.77");
  await p.selectOption("#f-estado", "heredado_no_revisado");
  await p.click("#f-aplicar");
  await p.waitForFunction(() => window.__llamadas.length >= 3);
  const [fn, a] = (await llamadas(p)).at(-1);
  assert.equal(fn, "admin_errores");
  assert.equal(a.app, "didi"); assert.equal(a.version, "10.77"); assert.equal(a.estado_veredicto, "heredado_no_revisado"); assert.equal(a.pagina, 1);
  assert.match(await p.textContent("#vista-errores"), /acceso restringido/);
  await p.close();
});

test("ofertas: paginación y datos pintados como texto (sin inyección)", async () => {
  const { p } = await abrir("ok");
  await p.click("#t-ofertas");
  await p.waitForSelector("#p-info");
  assert.match(await p.textContent("#p-info"), /Página 1 de 3 · 120 en total/);
  assert.equal(await p.isDisabled("#p-ant"), true);
  await p.click("#p-sig");
  await p.waitForFunction(() => document.querySelector("#p-info")?.textContent.includes("Página 2"));
  assert.equal((await llamadas(p)).at(-1)[1].pagina, 2);
  assert.equal(await p.evaluate(() => window.__xss), undefined);
  assert.equal(await p.locator("img").count(), 0);
  assert.match(await p.textContent("#vista-ofertas"), /<img src=x onerror/);
  await p.close();
});

test("métricas: insuficiente, sin datos y no medible se ven; el acuerdo IA va aparte", async () => {
  const { p } = await abrir("ok");
  await p.click("#t-metricas");
  await p.waitForSelector("[data-clave=recall]");
  assert.match(await p.textContent("[data-clave=lecturas]"), /insuficiente/);
  assert.match(await p.textContent("[data-clave=precision_humana]"), /—.*sin datos/s);
  assert.match(await p.textContent("[data-clave=recall]"), /no medible.*sin denominador/s);
  assert.match(await p.textContent("[data-clave=acuerdo_ia_lector]"), /90\.0 %/);
  assert.match(await p.textContent("#vista-metricas"), /Acuerdo IA \/ lector \(no es exactitud\)/);
  assert.match(await p.textContent("#vista-metricas"), /insuficiente \(< 30\)/);
  await p.close();
});

test("no autorizado, error del servidor y red caída se avisan sin romper la página", async () => {
  for (const [esc, rx] of [["no_autorizado", /No autorizado/], ["error_servidor", /Error del servidor: boom/], ["red", /Sin conexión/]]) {
    const { p, errores } = await abrir(esc);
    await p.waitForSelector("#aviso:not([hidden])");
    assert.match(await p.textContent("#aviso"), rx, esc);
    assert.deepEqual(errores, [], esc);
    assert.equal(await p.locator("#inicio").count(), 0);
    await p.close();
  }
});

test("en un teléfono (390 px) no hay desplazamiento horizontal de la página", async () => {
  const { p } = await abrir("ok", 390);
  await p.waitForSelector("#inicio");
  assert.equal(await p.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
  await p.close();
});

test("la página no trae llaves ni secretos", () => {
  for (const f of ["index.html", "app.js", "config.js", "estilo.css"]) {
    const t = fs.readFileSync(path.join(ADMIN, f), "utf8");
    assert.doesNotMatch(t, /eyJ[A-Za-z0-9_-]{10,}\.|sb_secret_|service_role\s*[:=]/, f);
  }
  assert.match(fs.readFileSync(path.join(ADMIN, "index.html"), "utf8"), /Content-Security-Policy/);
});

test("A6: la biblioteca externa va con versión fija, archivo real y huella SRI", () => {
  const js = fs.readFileSync(path.join(ADMIN, "app.js"), "utf8");
  assert.match(js, /supabase-js@2\.45\.4\/dist\/umd\/supabase\.js"/);
  assert.match(js, /sc\.integrity = "sha384-[A-Za-z0-9+/]{64}"/);
  assert.doesNotMatch(js, /supabase\.min\.js/);
  const codigo = js.split("\n").filter((l) => !l.trim().startsWith("//")).join("\n");   // sin los comentarios
  assert.doesNotMatch(codigo + fs.readFileSync(path.join(ADMIN, "index.html"), "utf8"), /innerHTML|document\.write|eval\(|localStorage|sessionStorage/);
});
