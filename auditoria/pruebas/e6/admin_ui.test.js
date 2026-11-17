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

// Fábrica del cliente simulado (SOLO PRUEBAS: datos inventados, jamás se sirven en la página real).
function clienteSimulado(escenario) {
  const XSS = '<img src=x onerror="window.__xss=1">';
  const ofertas = Array.from({ length: 50 }, (_, i) => ({ id: "id" + i, ts_evento: "2026-11-15T10:00:00Z", app: i % 2 ? "didi" : "uber",
    version_app: "10.81", lector: "ocr", resultado: "oferta", motivo: i === 0 ? XSS : null, tarifa: 100 + i, km: 5, min: 12,
    nivel: "verde", captura_vinculo: "exacto", estado_veredicto: "sin_veredicto", referencia: null,
    etapas: [{ etapa: "captura", estado: "ok" }], conductor: "abcd1234" }));
  const comp = (clave, nombre, estado, extra) => Object.assign({ clave, nombre, estado, comprobacion: "c", ultima_comprobacion: "2026-11-15T10:00:00Z", depende_de: [] }, extra || {});
  const respuestas = {
    admin_config: { yo: { admin: "abcd1234", rol: escenario === "lector" ? "lector" : "revisor" }, administradores: [{ admin: "abcd1234", rol: "revisor", alta: "2026-11-01T00:00:00Z" }],
      permisos: { lector: ["consultar"], revisor: ["consultar", "dar veredictos"] },
      retencion: { lecturas_dias: 30, lecturas_vencidas: 0, capturas_dias: 7, purga_programada: false, nota: "nota retención" },
      almacenamiento: { capturas_objetos: 5, capturas_bytes: 1000, lecturas_filas: 15 },
      umbrales_alertas: [{ regla: "sin_recepciones", umbral: "24 h", gravedad: "alta" }], umbrales_editables: false, esquema: 6 },
    admin_salud: { desde: "2026-11-08T00:00:00Z", hasta: "2026-11-16T00:00:00Z", generado: "2026-11-15T10:00:00Z",
      estado_general: escenario === "gris" ? "gris" : "rojo",
      componentes: [comp("base_de_datos", "Base de datos", "operativo"), comp("ocr", "OCR", "sin_datos", { k: 0, n: 3 }), comp("revision_ia", "Revisión con IA", "fuera_de_servicio", { k: 2, n: 40, ms: 800 }),
        comp("mercado_pago", "Mercado Pago", "sin_comprobar"), comp("play_billing", "Play Billing", "no_aplicable")],
      atencion: [{ orden: 2, gravedad: "alta", componente: "revision_ia", titulo: "La revisión con IA no tiene saldo (http_402)", descripcion: "2 fallas", evidencia: "public.fallas_ia", estado: "abierta" }],
      indicadores: [
        { clave: "lecturas", nombre: "Auditorías procesadas", valor: 15, unidad: "lecturas", n: 15, definicion: "d1" },
        { clave: "tasa_errores", nombre: "Tasa de errores de procesamiento", k: 0, n: 15, valor: 0, unidad: "proporción", estado: "insuficiente", definicion: "d2" },
        { clave: "calidad_reconocimiento", nombre: "Calidad de reconocimiento (OCR)", k: 0, n: 3, valor: null, unidad: "proporción", estado: "insuficiente", definicion: "d3" },
        { clave: "calidad_interpretacion", nombre: "Calidad de interpretación (contra referencia humana)", k: 0, n: 0, valor: null, unidad: "proporción", definicion: "d4" },
        { clave: "veredictos_pendientes", nombre: "Errores sin veredicto humano", valor: 40, n: 40, unidad: "casos", definicion: "d5" },
        { clave: "incidentes", nombre: "Incidentes activos", valor: 1, unidad: "alertas", definicion: "d6" },
        { clave: "costo_ia_usd", nombre: "Costo de la revisión con IA (USD)", valor: null, n: 0, unidad: "USD", definicion: "d7" }],
      procesamiento: { recibidas: 15, procesadas: 15, fallidas: 0, pendientes: null, pendientes_nota: "no hay cola en el servidor", ms_lectura_p50: 420, ms_lectura_p90: 600, ms_lectura_max: 900, reintentos: 1 },
      calidad: { con_referencia: 0, sin_veredicto: 40, conflictos: 0, nota: "la exactitud solo se calcula con referencia" },
      historia: [{ dia: "2026-11-14", lecturas: 5, errores: 0, ofertas: 5, ms_p50: 400 }, { dia: "2026-11-15", lecturas: 10, errores: 1, ofertas: 9, ms_p50: 430 }], costos_historia: [] },
    admin_errores: { total: 1, pagina: 1, por_pagina: 50, filas: [{ id: 9001, creado: "2026-11-14T12:00:00Z", app: "DiDi", version: "10.77",
      tipo: "diferencia", lector_lectura: { tarifa: 50, km: 5, min: 12 }, ia_lectura: { tarifa: 55, km: 5, min: 12 },
      estado_veredicto: "heredado_no_revisado", referencia: null, tiene_captura: true }] },
    admin_error: { id: 9001, creado: "2026-11-14T12:00:00Z", app: "DiDi", version: "10.77", tipo: "diferencia", conductor: "abcd1234", clasificacion: "interpretacion_tarifa",
      evidencia: { lineas_guardadas: 4, captura: true, captura_acceso: "solo por la consola de Supabase (restringida)" }, lector: { tarifa: 50, km: 5, min: 12 }, ia: { tarifa: 55, km: 5, min: 12 },
      veredicto_heredado: "ia_bien", veredicto_humano: { estado: "heredado_no_revisado" }, estado_seguimiento: "nuevo", historial: [],
      relacionados: { misma_version: 3, mismo_conductor: 2 } },
    admin_seguimiento: { id: 1 },
    admin_ofertas: (a) => ({ total: 120, pagina: a.pagina, por_pagina: 50, filas: ofertas }),
    admin_metricas: { metricas: [
      { clave: "lecturas", grupo: "operacion", nombre: "Eventos de lectura", formula: "count(*)", poblacion: "p", fuente: "f", k: 15, n: 15, valor: 15, estado: "insuficiente" },
      { clave: "precision_humana", grupo: "calidad_humana", nombre: "Precisión de pop-ups (humana)", formula: "x", poblacion: "p", fuente: "f", k: 0, n: 0, valor: null, estado: "sin_datos" },
      { clave: "recall", grupo: "calidad_humana", nombre: "Ofertas reales que el lector no mostró", formula: "NO MEDIBLE", poblacion: "p", fuente: "—", k: null, n: null, valor: null, estado: "no_medible", motivo: "sin denominador" },
      { clave: "acuerdo_ia_lector", grupo: "acuerdo_ia", nombre: "Acuerdo IA/lector (NO es exactitud)", formula: "x", poblacion: "p", fuente: "f", k: 90, n: 100, valor: 0.9, estado: "suficiente" }], segmentos: [], tendencia: [] },
    admin_versiones: { versiones: [
      { version: "10.81", primera: "2026-11-15T00:00:00Z", lecturas: 15, errores_proceso: 0, errores_lector: 0, con_referencia: 5, coinciden: 4, ms_popup_p50: 650, conductores: 2 },
      { version: "10.80", primera: "2026-11-14T00:00:00Z", lecturas: 90, errores_proceso: 3, errores_lector: 4, con_referencia: 40, coinciden: 30, ms_popup_p50: 700, conductores: 3 }],
      nota_comparacion: "No se declara mejor una versión con muestras distintas" },
    admin_dispositivos: { dispositivos: [{ telefono: "samsung SM-A15", android: "14", versiones: ["10.81"], lecturas: 12, ofertas: 10, errores: 0, abstenciones: 1, ultima: "2026-11-15T10:00:00Z", conductores: 1, muestra_suficiente: false }],
      conductores: [{ conductor: "abcd1234", lecturas: 12, errores: 0, errores_lector: 2, versiones: ["10.81"], ultima: "2026-11-15T10:00:00Z" }], nota: "no prueba causalidad" },
    admin_ia: { casos: 40, con_referencia_humana: 0, discrepancias: { interpretacion_tarifa: 30, falso_positivo: 10 },
      componentes: [{ componente: "lector_local", n: 0, coinciden: 0 }, { componente: "ia", modelo: "deepseek-flash", n: 0, coinciden: 0 }],
      acuerdo_ia_lector: { n: 100, coinciden: 90 }, fallas: { http_402: 2 }, nota: "difíciles y sesgados" },
    admin_costos: { por_modelo: [{ modelo: "deepseek-flash", consultas: 100, tokens_entrada: 1000, tokens_salida: 100, usd: 0.1, ms_ia_p50: 800 }],
      por_modo: [], tarifas: [{ modelo: "deepseek-flash", usd_mtok_entrada: 0.3, usd_mtok_salida: 1.2 }], mes: null, nota: "tarifas de precios_modelo" },
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

test("tiene los siete módulos y la navegación se mantiene", async () => {
  const { p, errores } = await abrir("ok");
  await p.waitForSelector("#inicio");
  for (const m of ["inicio", "errores", "ofertas", "versiones", "dispositivos", "ia", "config"]) assert.equal(await p.locator("#t-" + m).count(), 1, m);
  for (const m of ["errores", "ofertas", "versiones", "dispositivos", "ia", "config"]) {
    await p.click("#t-" + m);
    await p.waitForSelector("#vista-" + m);
    assert.equal(await p.locator("#menu").count(), 1);
  }
  assert.deepEqual(errores, []);
  await p.close();
});

test("inicio: estado general, 7 indicadores, componentes honestos y atención requerida", async () => {
  const { p, errores } = await abrir("ok");
  await p.waitForSelector("#estado-general");
  assert.match(await p.textContent("h1"), /SALUD DE CHECA/);
  assert.match(await p.textContent("#estado-general"), /Incidente/);
  assert.equal(await p.locator("#inicio .tarjeta").count(), 7);
  assert.match(await p.textContent("[data-clave=calidad_interpretacion]"), /n = 0/);
  assert.match(await p.textContent("[data-clave=calidad_interpretacion]"), /—/);             // sin referencia: no hay porcentaje
  assert.match(await p.textContent("[data-clave=costo_ia_usd]"), /—/);                       // sin consumo: no es 0
  assert.equal(await p.locator("[data-clave=ocr] .punto.nd").count(), 1);                     // sin datos = gris
  assert.equal(await p.locator("[data-clave=revision_ia] .punto.mal").count(), 1);
  assert.equal(await p.locator("[data-clave=mercado_pago] .insignia.nd").count(), 1);
  assert.equal(await p.locator("[data-clave=ocr] .punto.ok").count(), 0);                     // nunca verde sin datos
  assert.match(await p.textContent("#alertas"), /http_402/);
  assert.match(await p.textContent("#procesamiento"), /sin dato/);                            // pendientes: desconocido, no cero
  assert.equal(await p.locator("#graficas svg").count() >= 2, true);
  assert.deepEqual(errores, []);
  await p.close();
});

test("inicio: sin datos el estado general es gris, nunca verde", async () => {
  const { p } = await abrir("gris");
  await p.waitForSelector("#estado-general");
  assert.equal(await p.locator("#estado-general .insignia.nd").count(), 1);
  assert.equal(await p.locator("#estado-general .insignia.ok").count(), 0);
  await p.close();
});

test("el periodo cambia la consulta", async () => {
  const { p } = await abrir("ok");
  await p.waitForSelector("#inicio");
  await p.click("[data-dias='30']");
  await p.waitForFunction(() => window.__llamadas.filter((l) => l[0] === "admin_salud").length >= 2);
  const [a, b] = (await llamadas(p)).filter((l) => l[0] === "admin_salud").map((l) => new Date(l[1].hasta) - new Date(l[1].desde));
  assert.equal(b > a, true);
  await p.close();
});

test("errores: filtros, expediente enlazado y seguimiento (solo revisor)", async () => {
  const { p } = await abrir("ok");
  await p.click("#t-errores");
  await p.waitForSelector("#vista-errores table");
  await p.selectOption("#f-app", "didi"); await p.fill("#f-version", "10.77"); await p.selectOption("#f-estado", "heredado_no_revisado");
  await p.click("#f-aplicar");
  await p.waitForFunction(() => window.__llamadas.filter((l) => l[0] === "admin_errores").length >= 2);
  const a = (await llamadas(p)).filter((l) => l[0] === "admin_errores").at(-1)[1];
  assert.equal(a.app, "didi"); assert.equal(a.version, "10.77"); assert.equal(a.estado_veredicto, "heredado_no_revisado"); assert.equal(a.pagina, 1);
  await p.click("text=#9001");
  await p.waitForSelector("#exp-titulo");
  assert.match(await p.textContent("#comparar"), /Lector.*Veredicto humano/s);
  assert.match(await p.textContent("#evidencia"), /no se muestran: traen direcciones/);
  await p.selectOption("#s-estado", "causa_identificada"); await p.selectOption("#s-causa", "ocr");
  await p.click("#s-guardar");
  await p.waitForFunction(() => window.__llamadas.some((l) => l[0] === "admin_seguimiento"));
  assert.deepEqual((await llamadas(p)).find((l) => l[0] === "admin_seguimiento")[1], { id: 9001, estado: "causa_identificada", causa: "ocr", nota: null });
  await p.click("text=Auditoría IA");
  await p.waitForSelector("#vista-ia");
  await p.close();
});

test("un lector (no revisor) no ve el formulario de seguimiento", async () => {
  const { p } = await abrir("lector");
  await p.click("#t-errores"); await p.waitForSelector("#vista-errores table");
  await p.click("text=#9001"); await p.waitForSelector("#exp-titulo");
  assert.equal(await p.locator("#s-guardar").count(), 0);
  assert.match(await p.textContent("#vista-errores"), /solo lectura/);
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
  assert.equal((await llamadas(p)).filter((l) => l[0] === "admin_ofertas").at(-1)[1].pagina, 2);
  assert.equal(await p.evaluate(() => window.__xss), undefined);
  assert.equal(await p.locator("img").count(), 0);
  assert.match(await p.textContent("#vista-ofertas"), /<img src=x onerror/);
  assert.match(await p.textContent("[data-clave=recall]"), /no medible.*sin denominador/s);
  assert.match(await p.textContent("[data-clave=precision_humana]"), /—.*sin datos/s);
  assert.match(await p.textContent("#vista-ofertas"), /Acuerdo IA \/ lector \(no es exactitud\)/);
  await p.close();
});

test("versiones: comparar exige muestra y no declara mejoras con n < 30", async () => {
  const { p } = await abrir("ok");
  await p.click("#t-versiones");
  await p.waitForSelector("#comparacion");
  assert.match(await p.textContent("#comparacion"), /Marca dos o más versiones/);
  await p.check("[data-version='10.81']"); await p.check("[data-version='10.80']");
  assert.match(await p.textContent("#aviso-comparacion"), /10\.81 tiene solo 5 casos con referencia \(< 30\)/);
  assert.match(await p.textContent("#comparacion"), /80\.0 %|75\.0 %/);
  await p.close();
});

test("conductores y dispositivos, auditoría IA y configuración muestran sus datos", async () => {
  const { p, errores } = await abrir("ok");
  await p.click("#t-dispositivos"); await p.waitForSelector("#vista-dispositivos table");
  assert.match(await p.textContent("#vista-dispositivos"), /insuficiente \(< 30\)/);
  assert.match(await p.textContent("#vista-dispositivos"), /no prueba causalidad/);
  await p.click("#t-ia"); await p.waitForSelector("#vista-ia table");
  assert.match(await p.textContent("#vista-ia"), /sin referencia/);
  assert.match(await p.textContent("#vista-ia"), /no es exactitud/);
  await p.click("#t-config"); await p.waitForSelector("#vista-config table");
  assert.match(await p.textContent("#vista-config"), /Costos de la IA/);
  assert.match(await p.textContent("#vista-config"), /Umbrales editables: no/);
  assert.match(await p.textContent("#vista-config"), /Historial de acciones/);
  assert.deepEqual(errores, []);
  await p.close();
});

test("no autorizado, error del servidor y red caída se avisan sin romper la página", async () => {
  for (const [esc, rx] of [["no_autorizado", /No autorizado/], ["error_servidor", /Error del servidor: boom/], ["red", /Sin conexión/]]) {
    const { p, errores } = await abrir(esc);
    await p.waitForSelector("#aviso:not([hidden])");
    assert.match(await p.textContent("#aviso"), rx, esc);
    assert.deepEqual(errores, [], esc);
    assert.equal(await p.locator("#inicio .tarjeta").count(), 0);
    await p.close();
  }
});

test("en un teléfono (390 px) no hay desplazamiento horizontal de la página", async () => {
  const { p } = await abrir("ok", 390);
  await p.waitForSelector("#inicio");
  assert.equal(await p.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
  await p.click("#t-versiones"); await p.waitForSelector("#comparacion");
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
