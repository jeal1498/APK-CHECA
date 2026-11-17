// E4 — la página de etiquetado: carga capturas locales, etiqueta, exporta un CSV que acepta etiquetado.py importar.
// Ejecutar: NODE_PATH=$(npm root -g) node --test auditoria/pruebas/e6/etiquetar_ui.test.js
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), os = require("node:os"), crypto = require("node:crypto");
const { execFileSync } = require("node:child_process");
const { chromium } = require("playwright");
const PAG = "file://" + path.resolve(__dirname, "../../etiquetado/etiquetar.html");
const ETQ = path.resolve(__dirname, "../../etiquetado/etiquetado.py");

test("dos revisores etiquetan las mismas capturas y el CSV entra a importar", async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "etq-"));
  const cap = path.join(dir, "capturas"); fs.mkdirSync(cap);
  for (const id of [960, 968, 1300, 1301, 1302]) fs.writeFileSync(path.join(cap, id + ".webp"), "RIFF\0\0\0\0WEBP" + id);
  fs.writeFileSync(path.join(cap, "ok-verde-1-2-3.webp"), "x");   // las normales no entran
  const nav = await chromium.launch();
  const csvs = [];
  for (const [rev, tarifa] of [["lalo", "50"], ["otra_persona", "50"]]) {
    const p = await (await nav.newContext({ acceptDownloads: true })).newPage();
    const errores = []; p.on("pageerror", (e) => errores.push(String(e)));
    await p.goto(PAG);
    await p.fill("#rev", rev); await p.fill("#n", "4");
    await p.setInputFiles("#archivos", fs.readdirSync(cap).map((f) => path.join(cap, f)));
    await p.waitForSelector("#visor:not([hidden])");
    const vistos = [];
    for (let k = 0; k < 4; k++) {
      vistos.push(await p.textContent("#nombre"));
      await p.check(`input[name=clase][value=${k === 3 ? "no_oferta" : "oferta"}]`);
      await p.selectOption("#app", "didi");
      if (k < 3) { await p.fill("#tarifa", tarifa); await p.fill("#km", "5.0"); await p.fill("#min", "12"); }
      else { await p.fill("#tarifa", ""); await p.fill("#km", ""); await p.fill("#min", ""); }
      await p.click("#sig");
    }
    const [d] = await Promise.all([p.waitForEvent("download"), p.click("#exportar")]);
    const ruta = path.join(dir, `${rev}.csv`); await d.saveAs(ruta); csvs.push(ruta);
    assert.deepEqual(errores, []);
    assert.equal(vistos.length, 4);
    if (rev === "lalo") globalThis.orden = vistos; else assert.deepEqual(vistos, globalThis.orden);   // mismo orden para los dos
  }
  await nav.close();
  const t = fs.readFileSync(csvs[0], "utf8").trim().split("\n");
  assert.equal(t[0], "caso,captura,captura_sha256,revisor,guia_version,clase,tarifa,km,min,tarjeta,confianza,nota");
  const [, nombre, sha] = t[1].split(",");
  assert.equal(sha, crypto.createHash("sha256").update(fs.readFileSync(path.join(cap, nombre))).digest("hex"));  // sha propio = sha real
  const out = path.join(dir, "out");
  execFileSync("python3", [ETQ, "importar", "--manifiesto", cap, "--hoja", csvs[0], "--hoja", csvs[1], "--salida", out]);
  const ac = JSON.parse(fs.readFileSync(path.join(out, "acuerdo.json"), "utf8"));
  assert.equal(ac.casos_comunes, 4); assert.equal(ac.acuerdo_total.valor, 1); assert.deepEqual(ac.errores_de_hoja, []);
});
