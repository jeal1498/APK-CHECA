// Pruebas de la lectura con imagen (vision.ts): validación, petición, interruptores y nombres. Ejecutar:
//   node --experimental-strip-types --no-warnings --test auditoria/pruebas/e9/vision.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";
import { LECTOR_IMAGEN, limpiarImagen, peticionVision, leerInterruptores, elegirRuta, nombreCapturaOk, esWebp, decodificar, MAX_IMAGEN_B64 } from "../../../supabase/functions/confirmar-oferta/vision.ts";
import { calcular } from "../../../supabase/functions/confirmar-oferta/lector.ts";

const B64 = "UklGRg".padEnd(400, "A");   // «RIFF…» en base64, largo razonable

test("limpiarImagen acepta base64 con o sin prefijo y rechaza lo demás", () => {
  assert.equal(limpiarImagen(B64), B64);
  assert.equal(limpiarImagen("data:image/webp;base64," + B64), B64);
  assert.equal(limpiarImagen(B64.slice(0, 100) + "\n" + B64.slice(100)), B64);              // saltos de línea
  for (const malo of [null, 5, "", "corto", "A".repeat(MAX_IMAGEN_B64 + 4), "no es base64 !!" + "A".repeat(300), "A".repeat(300) + "=a"]) {
    assert.equal(limpiarImagen(malo), null, String(malo).slice(0, 20));
  }
});

test("la petición manda la imagen WebP en base64, sin razonamiento, en json y con el mismo prompt de tramos", () => {
  const p = peticionVision(B64, "didi", "MXN") as any;
  assert.equal(p.model, "deepseek-flash");
  assert.deepEqual(p.thinking, { type: "disabled" });
  assert.deepEqual(p.response_format, { type: "json_object" });
  assert.equal(p.temperature, 0);
  const u = p.messages[1].content;
  assert.equal(u[1].type, "image_url");
  assert.equal(u[1].image_url.url, "data:image/webp;base64," + B64);
  assert.match(u[0].text, /didi/);
  assert.match(p.messages[0].content, /IGNORA el recuadro oscuro de Checa/);
  assert.match(LECTOR_IMAGEN, /recogida y viaje/);
  assert.match(LECTOR_IMAGEN, /NO hagas cuentas/);
});

test("la salida de la IA con imagen entra al mismo cálculo que la de texto (el caso #2165)", () => {
  // Lo que leería la IA mirando la imagen: recogida 3 min · 761 m y viaje 16 min · 5.6 km.
  const l = calcular({ oferta: true, tarifa: 45.62, multi: 0, tarjetas: 1, recogida: { h: 0, min: 3, dist: 761, unidad: "m" },
    viaje: { h: 0, min: 16, dist: 5.6, unidad: "km" }, resumen: null, porKmEst: 0 }, "didi");
  assert.equal(l.oferta, true);
  assert.equal(l.tarifa, 45.62);
  assert.equal(Math.round(l.km * 1000) / 1000, 6.361);
  assert.equal(l.min, 19);
  assert.equal(calcular({ oferta: false, tarifa: 0, recogida: { h: 0, min: 0, dist: 0, unidad: "km" }, viaje: { h: 0, min: 0, dist: 0, unidad: "km" } }, "uber").oferta, false);
});

test("interruptores: sin fila o con datos raros todo queda encendido; apagar uno no apaga el otro", () => {
  assert.deepEqual(leerInterruptores([]), { vision: true, texto: true });
  assert.deepEqual(leerInterruptores(null), { vision: true, texto: true });
  assert.deepEqual(leerInterruptores([{ vision: "no", texto: 0 }]), { vision: true, texto: true });
  assert.deepEqual(leerInterruptores([{ vision: false, texto: true }]), { vision: false, texto: true });
  assert.deepEqual(leerInterruptores({ vision: true, texto: false }), { vision: true, texto: false });
});

test("ruta: con imagen manda la visión, sin imagen el texto, y nunca se mezclan", () => {
  const on = { vision: true, texto: true }, sinVision = { vision: false, texto: true }, sinTexto = { vision: true, texto: false }, nada = { vision: false, texto: false };
  assert.equal(elegirRuta(true, on), "vision");
  assert.equal(elegirRuta(false, on), "texto");
  assert.equal(elegirRuta(true, sinVision), "vision_apagada");     // no cae al texto: el teléfono sigue con su lector
  assert.equal(elegirRuta(false, sinTexto), "texto_apagada");
  assert.equal(elegirRuta(true, sinTexto), "vision");
  assert.equal(elegirRuta(true, nada), "vision_apagada");
  assert.equal(elegirRuta(false, nada), "texto_apagada");
});

test("nombre de la captura normal igual al que arma el teléfono", () => {
  assert.equal(nombreCapturaOk("rojo", 51.31, 9.5, 15, 1794989385415, 640), "ok-rojo-51.31-9.5-15-1794989385415-n640.webp");
  assert.equal(nombreCapturaOk(null, 45.6, 0.76, 2.6, 1, 7), "ok-sin-45.60-0.8-3-1-n7.webp");
  assert.match(nombreCapturaOk("verde", 99.59, 16, 43, 1794984882409, 1), /^ok-verde-99\.59-16\.0-43-\d{10,14}-n\d+\.webp$/);
});

test("esWebp reconoce el encabezado RIFF/WEBP y decodificar hace el viaje de ida y vuelta", () => {
  const webp = new Uint8Array([0x52, 0x49, 0x46, 0x46, 1, 2, 3, 4, 0x57, 0x45, 0x42, 0x50, 9]);
  assert.equal(esWebp(webp), true);
  assert.equal(esWebp(new Uint8Array([0x89, 0x50, 0x4e, 0x47, 1, 2, 3, 4, 5, 6, 7, 8, 9])), false);   // PNG
  assert.equal(esWebp(new Uint8Array(5)), false);
  assert.deepEqual(decodificar(btoa(String.fromCharCode(...webp))), webp);
});
