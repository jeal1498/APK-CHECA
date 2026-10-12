// Paquetes de Checa (precios en MXN). Deben coincidir con las tarjetas de la app.
export const PLANES: Record<string, { dias: number; precio: number; titulo: string }> = {
  "1m": { dias: 30, precio: 99, titulo: "Checa · 1 mes (Plan Básico)" },
  "6m": { dias: 180, precio: 399, titulo: "Checa · 6 meses (Plan Estratega)" },
  "12m": { dias: 365, precio: 899, titulo: "Checa · 1 año (Plan Pro)" },
};
