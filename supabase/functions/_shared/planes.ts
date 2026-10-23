// Paquetes de Checa (precios en MXN). Deben coincidir con las tarjetas de la app y con la web.
// precioBeta = precio de fundador (40 % menos de por vida), solo para las primeras FUNDADOR_CUPOS
// cuentas de Google que lo reclaman durante la beta (ver mp-pago y la tabla `fundadores`).
export const PLANES: Record<string, { dias: number; precio: number; precioBeta: number; titulo: string }> = {
  "1m": { dias: 30, precio: 119, precioBeta: 71, titulo: "Checa · 1 mes (Plan Básico)" },
  "6m": { dias: 180, precio: 589, precioBeta: 353, titulo: "Checa · 6 meses (Plan Estratega)" },
  "12m": { dias: 365, precio: 1059, precioBeta: 635, titulo: "Checa · 1 año (Plan Pro)" },
};

/** Cupos totales del Plan Fundador (se comparten entre 1 mes, 6 meses y 1 año; no son 50 por paquete). */
export const FUNDADOR_CUPOS = 50;

/** Precio de fundador: la cuenta se creó antes de que termine la beta (31 ene 2027, hora de Cancún)… */
export const BETA_FIN = Date.parse("2027-02-01T05:00:00Z");
/** …y su primer pago es a más tardar el 28 feb 2027; después se conserva si renueva a tiempo. */
export const BETA_PRIMER_PAGO_HASTA = Date.parse("2027-03-01T05:00:00Z");
/** Días que puede pasar vencida la licencia sin perder el precio de fundador. */
export const BETA_GRACIA_DIAS = 30;
/** Marca en external_reference de un pago con precio de fundador. */
export const MARCA_BETA = "PF";
