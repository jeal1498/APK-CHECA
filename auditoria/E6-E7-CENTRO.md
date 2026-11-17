# E6/E7 — Checa Admin: centro de control, salud y calidad (17-nov-2026)

**Estado:** implementado y probado en local (PostgreSQL 16 con stubs de Supabase + Playwright con cliente simulado). **No** probado contra Supabase real ni con datos reales: la base de pruebas no se creó (costo sin confirmar). Nada de esto está aplicado en producción.

## Arquitectura
- `admin/` (página estática, CSP estricta, sin innerHTML): barra lateral con 7 módulos, enlaces entre módulos (`#errores/9001`), periodo 7/30/90 días, skeletons, estados vacíos, responsive (390 px), gráficas SVG propias.
- `auditoria/sql/006_centro.sql` (+ `.down`): funciones `public.admin_*` con rol comprobado en el servidor y bitácora: `admin_salud`, `admin_error`, `admin_seguimiento` (solo revisor), `admin_versiones`, `admin_dispositivos`, `admin_ia`, `admin_costos`, `admin_config`; más `admin_inicio/errores/ofertas/metricas/veredicto/bitacora` de 004–005. Tabla nueva solo-agregar: `auditoria.seguimiento_errores`.
- Reutiliza `auditoria.lecturas/etapas/recepciones`, `veredicto_vigente`, `public.errores_lector/revisiones/fallas_ia/precios_modelo`. No hay backend paralelo.

## Módulos
| # | Módulo | Fuente real | Notas |
|---|---|---|---|
| 1 | Inicio · Salud | `admin_salud` | componentes, indicadores, procesamiento, calidad, atención requerida, historia |
| 2 | Errores | `admin_errores`, `admin_error`, `admin_seguimiento` | expediente con lector/IA/humano, seguimiento en 7 estados con causa e historial |
| 3 | Ofertas | `admin_ofertas`, `admin_metricas` | métricas con definición, n e intervalo; recall NO MEDIBLE (la telemetría no ve lo que no se mostró) |
| 4 | Versiones | `admin_versiones` | comparación solo informativa: avisa si n de referencia < 30 |
| 5 | Conductores y dispositivos | `admin_dispositivos` | seudónimos; muestra suficiente = n ≥ 30; sin atribuir causalidad |
| 6 | Auditoría IA | `admin_ia` | clasificación de discrepancias; quién acierta solo contra referencia humana |
| 7 | Configuración | `admin_config`, `admin_costos`, `admin_bitacora` | administradores, retención, almacenamiento, umbrales (fijos), costos con tarifas de `precios_modelo` |

## Reglas de salud (documentadas y fijas)
- Ingesta y app Android: última recepción ≤ 24 h operativa, ≤ 72 h degradada, más fuera de servicio; sin eventos → `sin_datos`.
- OCR / Parser: fallos de etapa / etapas con n ≥ 30: < 5 % operativo, < 15 % degradado, ≥ 15 % fuera de servicio; n < 30 → `sin_datos` (gris, nunca verde).
- Revisión con IA: cualquier `http_402` → fuera de servicio; fallidas / revisiones con las mismas cotas.
- Mercado Pago `sin_comprobar` (sin sonda externa); Play Billing `no_aplicable` (Fase 7).
- Estado general: rojo si algún componente está fuera de servicio, amarillo si degradado, gris si nada está operativo, amarillo parcial si quedan sin comprobar.
- Atención requerida: 7 reglas con umbral fijo (ver `admin_config`).

## Fórmulas
- Tasa de errores = lecturas con resultado error/timeout o alguna etapa en fallo/timeout ÷ lecturas del periodo.
- Calidad de reconocimiento = 1 − fallos de etapa `ocr` ÷ etapas `ocr` (mide que no falle, no que lo leído sea correcto).
- Calidad de interpretación = ofertas mostradas que coinciden con la referencia humana (tarifa ±0.01, km ±0.05, min ±0.5) ÷ ofertas mostradas con referencia doble o adjudicada. Sin referencia → «—», n = 0.
- Acuerdo IA/lector = revisiones ok que coincidieron ÷ revisiones ok (no es exactitud).
- Costo = suma de `revisiones.costo_usd` (tarifa real de cada revisión); sin consumo → «—», no 0.

## Pruebas (resultado del 17-nov, local)
- `test_e6_centro.py`: 7 pruebas (acceso, anon, rango, estados honestos, expediente, seguimiento solo agregar, versiones/dispositivos/IA/costos/config, bitácora y reversión).
- `e6/admin_ui.test.js`: 14 pruebas Playwright con cliente SIMULADO (datos inventados solo en la prueba): 7 módulos, navegación, estados grises, no verde sin datos, XSS, paginación, móvil 390 px, sin secretos, SRI.
- Suite SQL/Python completa PG16: 105 pruebas; dos ajustes por el cambio autorizado del lector (huella `parser.sha256`, piso del banco 272).

## Criterios
| Criterio | Estado |
|---|---|
| Plataforma funciona (local) | APROBADO local, PENDIENTE con Supabase real |
| Acceso protegido; permisos en servidor | APROBADO (pruebas con rol anon/no admin/lector/revisor) |
| Salud con fuentes reales | PENDIENTE: usa tablas reales pero solo se probó con datos de prueba |
| Indicadores reproducibles | APROBADO (definición y fórmula en cada indicador) |
| Errores investigables, evidencias consultables | APROBADO local; la imagen no se muestra en la página (bucket privado): PENDIENTE |
| Auditoría IA inicial | APROBADO local |
| Estados desconocidos | APROBADO |
| Historial de alertas con estado y agrupación | PENDIENTE: las alertas se recalculan, no se persisten |
| Costos con datos verificables | PENDIENTE: sin datos reales |
| Banco de evaluación protegido y comparación entre versiones sobre casos comunes | PENDIENTE: solo comparación informativa |
| Umbrales y destinatarios de alertas configurables | RECHAZADO por ahora: umbrales fijos, sin destinatarios |
| Pruebas en Supabase real | BLOQUEADO: rama de pruebas sin crear (costo sin confirmar) |

## Bloqueos
1. Rama de pruebas de Supabase (confirmar costo) o un proyecto gratuito aparte.
2. Alta del primer administrador (a mano en el editor SQL) y llave anon en `admin/config.js` (no se versiona).
3. Imagen de la captura en el expediente: requiere URL firmada de un bucket privado; no se expone sin autorización.
