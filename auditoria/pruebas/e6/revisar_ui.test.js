// E4 — revisar.html: propuestas de la IA, controles ocultos y su importación. Ejecutar:
//   NODE_PATH=$(npm root -g) node --test auditoria/pruebas/e6/revisar_ui.test.js
const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), os = require("node:os");
const { execFileSync } = require("node:child_process");
const { chromium } = require("playwright");
const PAG = "file://" + path.resolve(__dirname, "../../etiquetado/revisar.html");
const ETQ = path.resolve(__dirname, "../../etiquetado/etiquetado.py");
const IDS = [960, 968, 973, 1300, 1301, 1302, 1303, 1304, 1305, 1306];
const propuesta = (id) => id % 3 === 0
  ? { captura: id + ".webp", clase: "no_oferta", app: "didi", tarjeta: null, tarifa: null, km: null, min: null, confianza: "alta", nota: "" }
  : { captura: id + ".webp", clase: "oferta", app: id % 2 ? "uber" : "didi", tarjeta: null, tarifa: 50 + id % 7, km: 5.5, min: 12, confianza: "alta", nota: "" };

async function revisar(nav, cap, rev, modo) {
  const p = await (await nav.newContext({ acceptDownloads: true })).newPage();
  const errores = []; p.on("pageerror", (e) => errores.push(String(e)));
  await p.goto(PAG);
  await p.fill("#rev", rev);
  // Se pega en dos tandas, como saldría de dos conversaciones con la IA.
  const todas = IDS.map(propuesta);
  await p.fill("#json", JSON.stringify(todas.slice(0, 5)) + "\n" + JSON.stringify(todas.slice(5)));
  await p.setInputFiles("#archivos", fs.readdirSync(cap).map((f) => path.join(cap, f)));
  await p.click("#empezar");
  await p.waitForSelector("#visor:not([hidden])");
  for (let k = 0; k < IDS.length; k++) {
    const nombre = (await p.textContent("#nombre")).split(" ")[0];
    const real = propuesta(parseInt(nombre, 10));
    if (modo === "bien") {   // la persona compara con la imagen (aquí: con la verdad) y corrige lo alterado
      await p.check(`input[name=clase][value=${real.clase}]`);
      await p.selectOption("#app", real.app);
      for (const c of ["tarifa", "km", "min"]) await p.fill("#" + c, real[c] == null ? "" : String(real[c]));
    }
    await p.click("#aprobar");
  }
  const fin = await p.textContent("#hechas");
  const [d] = await Promise.all([p.waitForEvent("download"), p.click("#exportar")]);
  const ruta = path.join(path.dirname(cap), `revision-${rev}.csv`); await d.saveAs(ruta);
  assert.deepEqual(errores, []);
  await p.close();
  return { ruta, fin };
}

test("revisión: aprobar a ciegas se detecta; revisar bien da filas válidas e importables", async () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "rev-"));
  const cap = path.join(dir, "capturas"); fs.mkdirSync(cap);
  for (const id of IDS) fs.writeFileSync(path.join(cap, id + ".webp"), "RIFF\0\0\0\0WEBP" + id);
  const nav = await chromium.launch();
  const ciego = await revisar(nav, cap, "ciego", "ciego");
  const bien = await revisar(nav, cap, "lalo", "bien");
  await nav.close();
  assert.match(ciego.fin, /Controles detectados: 0 de 5/);
  assert.match(bien.fin, /Controles detectados: 5 de 5/);
  const csv = fs.readFileSync(bien.ruta, "utf8");
  assert.match(csv.split("\n")[0], /,metodo,decision,control,control_campo,propuesta_ia,propuesta_mostrada$/);
  assert.equal((csv.match(/,revision_propuesta_ia,corregido,si,/g) || []).length, 5);

  const imp = (hoja, sal) => { execFileSync("python3", [ETQ, "importar-revision", "--manifiesto", cap, "--hoja", hoja, "--salida", sal]);
    return JSON.parse(fs.readFileSync(path.join(sal, "revision.json"), "utf8")); };
  const rc = imp(ciego.ruta, path.join(dir, "oc"));
  assert.equal(rc.revision_confiable, false); assert.equal(rc.controles.no_detectados.length, 5); assert.equal(rc.validas, IDS.length - 5);
  const rb = imp(bien.ruta, path.join(dir, "ob"));
  assert.equal(rb.revision_confiable, true); assert.equal(rb.validas, IDS.length); assert.deepEqual(rb.errores_de_hoja, []);
  assert.equal(rb.acuerdo_ia_persona.valor, 1);
  const sql = fs.readFileSync(path.join(dir, "ob", "veredictos_revision.sql"), "utf8");
  assert.equal((sql.match(/'humano', 'lalo', 'revision_propuesta_ia:/g) || []).length, IDS.length);
  assert.equal((sql.match(/'ia', 'claude-opus', 'prompt-opus-v1'/g) || []).length, IDS.length);
});
