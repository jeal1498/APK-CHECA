// E4 — resolver.html: discrepancias entre dos revisores → resolución → referencia final. Ejecutar:
//   NODE_PATH=$(npm root -g) node --test auditoria/pruebas/e6/resolver_ui.test.js
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), os = require("node:os"), crypto = require("node:crypto");
const { execFileSync, spawnSync } = require("node:child_process");
const { chromium } = require("playwright");
const PAG = "file://" + path.resolve(__dirname, "../../etiquetado/resolver.html");
const ETQ = path.resolve(__dirname, "../../etiquetado/etiquetado.py");
const CAB = "caso,captura,captura_sha256,revisor,guia_version,clase,tarifa,km,min,tarjeta,confianza,nota";
const sha = (b) => crypto.createHash("sha256").update(b).digest("hex");

test("resolver discrepancias: elegir, corregir, dejar pendiente e importar la referencia final", async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "res-"));
  const cap = path.join(dir, "capturas"); fs.mkdirSync(cap);
  const ids = [1001, 1002, 1003, 1004, 1005, 1006];
  const shas = {};
  for (const id of ids) { const b = Buffer.from("RIFF\0\0\0\0WEBP" + id); fs.writeFileSync(path.join(cap, id + ".webp"), b); shas[id] = sha(b); }
  // [clase,app,tarifa,km,min,tarjeta] de cada revisor
  const A = { 1001: ["oferta", "didi", 50, 5, 12, ""], 1002: ["no_oferta", "didi", "", "", "", ""], 1003: ["oferta", "didi", 40, 7.7, 19, 2],
              1004: ["varias", "didi", "", "", "", ""], 1005: ["oferta", "uber", 60, 9, 20, ""], 1006: ["oferta", "didi", 30, 3, 9, ""] };
  const B = { ...A, 1003: ["oferta", "didi", 40, 7.2, 19, 2], 1004: ["oferta", "didi", 55, 6, 15, 1], 1005: ["oferta", "uber", 60, 9, 20, 1], 1006: ["oferta", "uber", 30, 3, 9, ""] };
  const hoja = (rev, M) => [CAB, ...ids.map((id) => { const [c, a, t, k, m, tj] = M[id];
    return `${id}-${a},${id}.webp,${shas[id]},${rev},1.0,${c},${t},${k},${m},${tj},alta,`; })].join("\n") + "\n";
  fs.writeFileSync(path.join(dir, "a.csv"), hoja("lalo", A)); fs.writeFileSync(path.join(dir, "b.csv"), hoja("pp", B));
  execFileSync("python3", [ETQ, "importar", "--sin-capturas", "--hoja", path.join(dir, "a.csv"), "--hoja", path.join(dir, "b.csv"), "--salida", path.join(dir, "o1")]);
  const conf = fs.readFileSync(path.join(dir, "o1", "conflictos.csv"), "utf8").trim().split("\n");
  assert.equal(conf.length - 1, 4);                        // 1003, 1004, 1005, 1006

  const nav = await chromium.launch();
  try {
  const p = await (await nav.newContext({ acceptDownloads: true })).newPage();
  const errores = []; p.on("pageerror", (e) => errores.push(String(e)));
  await p.goto(PAG);
  await p.fill("#rev", "lalo");                              // no puede ser uno de los revisores
  await p.setInputFiles("#csv", path.join(dir, "o1", "conflictos.csv"));
  await p.setInputFiles("#archivos", ids.map((id) => path.join(cap, id + ".webp")));
  await p.click("#empezar");
  await p.waitForFunction(() => document.querySelector("#aviso").textContent.length > 0);
  assert.match(await p.textContent("#aviso"), /no puede usar el seudónimo/);
  await p.fill("#rev", "consenso"); await p.click("#empezar");
  await p.waitForSelector("#visor:not([hidden])");
  assert.match(await p.textContent("#avance"), /1 de 4/);
  // 1003: km 7.7 (lalo) contra 7.2 (pp) → elegir la de lalo
  assert.match(await p.textContent("#nombre"), /^1003\.webp/);
  assert.match(await p.textContent("#tabla"), /7\.7/); assert.equal(await p.locator("td.dif").count() > 0, true);
  await p.click("#usaA"); await p.click("#sig");
  // 1004: clase varias contra oferta → elegir la de pp
  await p.click("#usaB"); await p.click("#sig");
  // 1005: solo tarjeta → un valor distinto de los dos (tarjeta 3) = «otro»
  await p.click("#usaA"); await p.fill("#tarjeta", "3"); await p.click("#sig");
  // 1006: sin elegir nada → no deja guardar; se salta y queda pendiente
  await p.click("#sig");
  assert.match(await p.textContent("#error"), /Elige qué es/);
  await p.click("#saltar");
  const [d] = await Promise.all([p.waitForEvent("download"), p.click("#exportar")]);
  await d.saveAs(path.join(dir, "res.csv"));
  assert.deepEqual(errores, []);
  } finally { await nav.close(); }
  const filas = fs.readFileSync(path.join(dir, "res.csv"), "utf8").trim().split("\n");
  assert.equal(filas.length - 1, 3);
  assert.match(filas.join("\n"), /1003-didi.*,consenso,lalo/); assert.match(filas.join("\n"), /1004-didi.*,consenso,pp/); assert.match(filas.join("\n"), /1005-uber.*,consenso,otro/);

  const salida = path.join(dir, "o2");
  execFileSync("python3", [ETQ, "importar-resolucion", "--sin-capturas", "--hoja", path.join(dir, "a.csv"), "--hoja", path.join(dir, "b.csv"),
                           "--resolucion", path.join(dir, "res.csv"), "--salida", salida]);
  const r = JSON.parse(fs.readFileSync(path.join(salida, "resumen_final.json"), "utf8"));
  assert.equal(r.doble_ciego, 2); assert.equal(r.adjudicadas, 3); assert.deepEqual(r.pendientes, ["1006"]); assert.equal(r.referencia_final, 5);
  assert.deepEqual(r.adjudicadas_a_favor_de, { lalo: 1, pp: 1, otro: 1 }); assert.deepEqual(r.adjudicadas_por_metodo, { consenso: 3, tercera_persona: 0 });
  const fin = fs.readFileSync(path.join(salida, "referencia_final.csv"), "utf8");
  assert.match(fin, /^1003,oferta,didi,40,7\.7,19,2,adjudicada,consenso$/m);
  assert.match(fin, /^1001,oferta,didi,50,5,12,,doble_ciego,$/m);
  const sql = fs.readFileSync(path.join(salida, "veredictos_final.sql"), "utf8");
  assert.equal((sql.match(/'adjudicacion:consenso'/g) || []).length, 3);
  assert.equal((sql.match(/'doble_ciego'/g) || []).length, 10);     // 5 casos × 2 revisores

  // Una resolución con el seudónimo de un revisor, o de un caso que no era discrepancia, se rechaza.
  const mala = fs.readFileSync(path.join(dir, "res.csv"), "utf8").split("\n").map((l, k) => k === 1 ? l.replace(",consenso,", ",lalo,") : l).join("\n")
    + `1001-didi,1001.webp,${shas[1001]},consenso,1.0,oferta,50,5,12,,media,,consenso,otro\n`;
  fs.writeFileSync(path.join(dir, "mala.csv"), mala);
  execFileSync("python3", [ETQ, "importar-resolucion", "--sin-capturas", "--hoja", path.join(dir, "a.csv"), "--hoja", path.join(dir, "b.csv"),
                           "--resolucion", path.join(dir, "mala.csv"), "--salida", path.join(dir, "o3")]);
  const m = JSON.parse(fs.readFileSync(path.join(dir, "o3", "resumen_final.json"), "utf8"));
  assert.deepEqual(m.errores_de_resolucion.map((e) => e.error).sort(), ["no_era_una_discrepancia", "resolvedor_es_uno_de_los_revisores"]);
  fs.writeFileSync(process.env.SQL_FINAL || path.join(dir, "final.sql"), sql);
});
