-- E6/E7 · 006 — Checa Admin como centro de control: salud, expediente de errores con seguimiento, versiones,
-- dispositivos, auditoría de IA, costos y configuración (PREPARADO, NO APLICADO). Requiere 001–005.
-- Todas las funciones: comprueban el rol, limitan filtros, no devuelven líneas ni direcciones ni usuarios
-- (solo seudónimos), y dejan constancia en la bitácora. Ningún dato se inventa: sin datos → estado «sin_datos».
-- Estados de un componente: operativo, degradado, fuera_de_servicio, sin_comprobar, sin_datos, no_aplicable.

-- Seguimiento de errores (solo agregar): quién cambió el estado de investigación y cuándo.
create table if not exists auditoria.seguimiento_errores (
  id              bigserial primary key,
  error_lector_id bigint not null references public.errores_lector (id) on delete cascade,
  estado          text not null check (estado in ('nuevo', 'pendiente_de_revision', 'en_investigacion', 'causa_identificada',
                                                  'corregido', 'verificado', 'cerrado')),
  causa           text check (causa in ('ocr', 'interpretacion', 'tarjeta_equivocada', 'pantalla_cambio', 'ia', 'dato_ambiguo', 'otra')),
  nota            text check (length(nota) <= 300),
  admin           text not null,
  creado          timestamptz not null default now()
);
create index if not exists seguimiento_errores_err on auditoria.seguimiento_errores (error_lector_id, id desc);
alter table auditoria.seguimiento_errores enable row level security;
revoke all on auditoria.seguimiento_errores from public, anon, authenticated;
drop trigger if exists seguimiento_solo_agregar on auditoria.seguimiento_errores;
create trigger seguimiento_solo_agregar before update or delete on auditoria.seguimiento_errores
  for each row execute function auditoria.solo_agregar();

-- Guardia común: devuelve null si todo está bien o el jsonb de error (ya anotado en la bitácora).
create or replace function auditoria.guardia(accion text, f jsonb, desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, pg_temp as $$
declare err text;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar(accion, null, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  err := auditoria.filtros_invalidos(desde, hasta, 1, 1);
  if err is not null then perform auditoria.anotar(accion, null, f, 0, 'invalido'); return jsonb_build_object('error', err); end if;
  return null;
end $$;
revoke all on function auditoria.guardia(text, jsonb, timestamptz, timestamptz) from public, anon, authenticated;

-- Enlace caso ↔ lectura ↔ captura por el fotograma exacto (10.82). La huella sha256 viene de la lectura (la calculó el
-- teléfono sobre los mismos bytes que subió); Storage solo guarda un md5, así que la comprobación de la huella se hace
-- bajando la imagen (herramientas/verificar_enlace_capturas.py). Sin fotograma (casos anteriores) no hay enlace.
create or replace view auditoria.enlace_capturas with (security_invoker = on) as
select 'error_lector'::text as caso_tipo, e.id as caso_id, e.fotograma_id, l.id as lectura_id, l.resultado, l.captura_vinculo, l.captura_sha256,
       e.id::text || '.webp' as objeto
from public.errores_lector e join auditoria.lecturas l on l.fotograma_id = e.fotograma_id and l.usuario = e.usuario
where e.fotograma_id is not null
union all
select 'captura_ok', c.id, c.fotograma_id, l.id, l.resultado, l.captura_vinculo, l.captura_sha256, '%-n' || c.id::text || '.webp'
from public.capturas_ok c join auditoria.lecturas l on l.fotograma_id = c.fotograma_id and l.usuario = c.usuario
where c.fotograma_id is not null;
revoke all on auditoria.enlace_capturas from public, anon, authenticated;

-- ---------- 01 Inicio: salud de Checa ----------
-- Componentes con el estado que SE PUEDE comprobar con datos reales; el resto se marca «sin_comprobar».
-- Reglas (documentadas en auditoria/E6-E7-CENTRO.md): recepción ≤ 24 h operativa, ≤ 72 h degradada, más = fuera de servicio;
-- etapas con fallo/timeout: < 5 % operativa, < 15 % degradada, ≥ 15 % fuera de servicio (con n ≥ 30; con menos, sin_datos).
create or replace function public.admin_salud(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; r jsonb;
        ult timestamptz; nl int; nerr int; ncola int; p50 numeric; p90 numeric; pmax int; comp jsonb := '[]'; aten jsonb := '[]';
        o_n int; o_f int; p_n int; p_f int; rv_n int; rv_ok int; rv_coi int; rv_ms numeric; fa int; fa402 int; ab int; ofe int;
        conf int; con_ref int; ref_ok int; sin_ver int; nerr_l int; nobj bigint; ant jsonb; prev_desde timestamptz;
        function_ok text := 'operativo';
begin
  g := auditoria.guardia('salud', f, desde, hasta); if g is not null then return g; end if;
  select max(recibido) into ult from lecturas;
  select count(*), count(*) filter (where resultado in ('error', 'timeout') or exists
           (select 1 from etapas e where e.lectura_id = l.id and e.estado in ('fallo', 'timeout'))),
         count(*) filter (where resultado in ('abstencion', 'varias')), count(*) filter (where resultado = 'oferta')
    into nl, nerr, ab, ofe from lecturas l where ts_evento >= desde and ts_evento < hasta;
  select percentile_cont(0.5) within group (order by ms_lectura), percentile_cont(0.9) within group (order by ms_lectura), max(ms_lectura)
    into p50, p90, pmax from lecturas where ts_evento >= desde and ts_evento < hasta and ms_lectura is not null;
  select count(*), count(*) filter (where e.estado in ('fallo', 'timeout')) into o_n, o_f from etapas e join lecturas l on l.id = e.lectura_id
    where e.etapa = 'ocr' and l.ts_evento >= desde and l.ts_evento < hasta;
  select count(*), count(*) filter (where e.estado in ('fallo', 'timeout')) into p_n, p_f from etapas e join lecturas l on l.id = e.lectura_id
    where e.etapa = 'parser' and l.ts_evento >= desde and l.ts_evento < hasta;
  select count(*), count(*) filter (where ok), count(*) filter (where ok and coincide), percentile_cont(0.5) within group (order by ms_ia)
    into rv_n, rv_ok, rv_coi, rv_ms from public.revisiones where creado >= desde and creado < hasta and modo <> 'rescate';
  select coalesce(sum(n), 0), coalesce(sum(n) filter (where tipo = 'http_402'), 0) into fa, fa402 from public.fallas_ia
    where dia >= desde::date and dia < hasta::date + 1;
  select count(*) into conf from veredicto_vigente where estado = 'conflicto_humano';
  select count(*) filter (where estado in ('referencia_doble', 'referencia_adjudicada')) into con_ref from veredicto_vigente;
  select count(*) into sin_ver from public.errores_lector e where e.creado >= desde and e.creado < hasta and e.tipo <> 'reporte'
    and not exists (select 1 from veredicto_vigente v where v.error_lector_id = e.id and v.estado in ('referencia_doble', 'referencia_adjudicada'));
  select count(*) into nerr_l from public.errores_lector where creado >= desde and creado < hasta;
  select count(*), coalesce(sum((metadata->>'size')::bigint), 0) into ncola, nobj from storage.objects where bucket_id = 'capturas';

  comp := jsonb_build_array(
    jsonb_build_object('clave', 'base_de_datos', 'nombre', 'Base de datos', 'estado', 'operativo', 'ultima_comprobacion', now(),
      'comprobacion', 'esta misma consulta respondió', 'ms', null, 'ultimo_error', null, 'depende_de', jsonb_build_array()),
    jsonb_build_object('clave', 'api', 'nombre', 'API de administración', 'estado', 'operativo', 'ultima_comprobacion', now(),
      'comprobacion', 'la función admin_salud respondió con sesión válida', 'ms', null, 'ultimo_error', null, 'depende_de', jsonb_build_array('base_de_datos')),
    jsonb_build_object('clave', 'ingesta', 'nombre', 'Ingesta de lecturas (registrar-lectura)',
      'estado', case when ult is null then 'sin_datos' when ult > now() - interval '24 hours' then 'operativo'
                     when ult > now() - interval '72 hours' then 'degradado' else 'fuera_de_servicio' end,
      'ultima_comprobacion', ult, 'comprobacion', 'última recepción de un evento', 'ms', null, 'ultimo_error', null,
      'depende_de', jsonb_build_array('base_de_datos', 'aplicacion_android')),
    jsonb_build_object('clave', 'aplicacion_android', 'nombre', 'Aplicación Android (telemetría)',
      'estado', case when ult is null then 'sin_datos' when ult > now() - interval '24 hours' then 'operativo'
                     when ult > now() - interval '72 hours' then 'degradado' else 'fuera_de_servicio' end,
      'ultima_comprobacion', ult, 'comprobacion', 'hay eventos recientes de al menos un teléfono', 'ms', null, 'ultimo_error', null,
      'depende_de', jsonb_build_array()),
    jsonb_build_object('clave', 'ocr', 'nombre', 'OCR (ML Kit en el teléfono)',
      'estado', case when o_n < 30 then 'sin_datos' when o_f::numeric / o_n < 0.05 then 'operativo' when o_f::numeric / o_n < 0.15 then 'degradado' else 'fuera_de_servicio' end,
      'ultima_comprobacion', ult, 'comprobacion', 'fallos o timeouts de la etapa ocr / etapas ocr', 'k', o_f, 'n', o_n, 'ms', null,
      'ultimo_error', null, 'depende_de', jsonb_build_array('aplicacion_android')),
    jsonb_build_object('clave', 'interpretacion', 'nombre', 'Interpretación (Parser)',
      'estado', case when p_n < 30 then 'sin_datos' when p_f::numeric / p_n < 0.05 then 'operativo' when p_f::numeric / p_n < 0.15 then 'degradado' else 'fuera_de_servicio' end,
      'ultima_comprobacion', ult, 'comprobacion', 'fallos de la etapa parser / etapas parser', 'k', p_f, 'n', p_n, 'ms', null,
      'ultimo_error', null, 'depende_de', jsonb_build_array('ocr')),
    jsonb_build_object('clave', 'revision_ia', 'nombre', 'Revisión con IA (confirmar-oferta)',
      'estado', case when fa402 > 0 then 'fuera_de_servicio' when rv_n < 30 then 'sin_datos'
                     when (rv_n - rv_ok)::numeric / rv_n < 0.05 then 'operativo' when (rv_n - rv_ok)::numeric / rv_n < 0.15 then 'degradado' else 'fuera_de_servicio' end,
      'ultima_comprobacion', (select max(creado) from public.revisiones), 'comprobacion', 'revisiones fallidas / revisiones; http_402 = sin saldo',
      'k', rv_n - rv_ok, 'n', rv_n, 'ms', round(rv_ms, 0), 'ultimo_error', (select max(ultima) from public.fallas_ia where dia >= desde::date),
      'depende_de', jsonb_build_array('api')),
    jsonb_build_object('clave', 'almacenamiento', 'nombre', 'Almacenamiento de capturas',
      'estado', 'operativo', 'ultima_comprobacion', now(), 'comprobacion', 'el bucket capturas respondió al contarlo', 'objetos', ncola, 'bytes', nobj,
      'ms', null, 'ultimo_error', null, 'depende_de', jsonb_build_array('base_de_datos')),
    jsonb_build_object('clave', 'mercado_pago', 'nombre', 'Mercado Pago (canal directo)', 'estado', 'sin_comprobar',
      'comprobacion', 'sin sonda: no se llama a servicios externos desde esta consulta', 'depende_de', jsonb_build_array('api')),
    jsonb_build_object('clave', 'play_billing', 'nombre', 'Google Play Billing', 'estado', 'no_aplicable',
      'comprobacion', 'Fase 7 pendiente: aún no existe', 'depende_de', jsonb_build_array()));

  -- Atención requerida (reglas fijas y documentadas; sin persistencia: se recalcula con cada consulta).
  aten := (select coalesce(jsonb_agg(a order by (a->>'orden')::int), '[]'::jsonb) from (
    select jsonb_build_object('orden', 1, 'gravedad', 'alta', 'componente', 'ingesta', 'titulo', 'Sin recepciones en más de 24 h',
      'descripcion', 'Última recepción: ' || ult::text, 'evidencia', 'max(auditoria.lecturas.recibido)', 'estado', 'abierta') a
      where ult is not null and ult < now() - interval '24 hours'
    union all select jsonb_build_object('orden', 2, 'gravedad', 'alta', 'componente', 'revision_ia', 'titulo', 'La revisión con IA no tiene saldo (http_402)',
      'descripcion', fa402 || ' fallas en el periodo', 'evidencia', 'public.fallas_ia', 'estado', 'abierta') where fa402 > 0
    union all select jsonb_build_object('orden', 3, 'gravedad', case when nerr::numeric / nullif(nl, 0) >= 0.10 then 'alta' else 'media' end, 'componente', 'interpretacion',
      'titulo', 'Tasa de errores de procesamiento alta', 'descripcion', nerr || ' de ' || nl || ' lecturas con fallo o timeout',
      'evidencia', 'auditoria.lecturas y auditoria.etapas', 'estado', 'abierta') where nl >= 30 and nerr::numeric / nullif(nl, 0) >= 0.05
    union all select jsonb_build_object('orden', 4, 'gravedad', 'media', 'componente', 'revision_ia', 'titulo', 'La IA y el lector discrepan en más del 30 %',
      'descripcion', (rv_ok - rv_coi) || ' de ' || rv_ok || ' revisiones no coincidieron (acuerdo ≠ exactitud)', 'evidencia', 'public.revisiones', 'estado', 'abierta')
      where rv_ok >= 30 and (rv_ok - rv_coi)::numeric / nullif(rv_ok, 0) > 0.30
    union all select jsonb_build_object('orden', 5, 'gravedad', 'media', 'componente', 'revision_humana', 'titulo', 'Hay conflictos humanos sin resolver',
      'descripcion', conf || ' casos', 'evidencia', 'auditoria.veredicto_vigente', 'estado', 'abierta') where conf > 0
    union all select jsonb_build_object('orden', 6, 'gravedad', 'media', 'componente', 'interpretacion', 'titulo', 'Muchas abstenciones del lector',
      'descripcion', ab || ' abstenciones de ' || (ab + ofe), 'evidencia', 'auditoria.lecturas', 'estado', 'abierta')
      where ab + ofe >= 30 and ab::numeric / nullif(ab + ofe, 0) > 0.20
    union all select jsonb_build_object('orden', 7, 'gravedad', 'baja', 'componente', 'revision_humana', 'titulo', 'Errores del lector sin referencia humana',
      'descripcion', sin_ver || ' de ' || nerr_l, 'evidencia', 'public.errores_lector × veredicto_vigente', 'estado', 'abierta') where sin_ver > 0
  ) x);

  ant := (select jsonb_build_object('lecturas', count(*), 'errores', count(*) filter (where resultado in ('error', 'timeout')))
          from lecturas where ts_evento >= desde - (hasta - desde) and ts_evento < desde);

  r := jsonb_build_object(
    'desde', desde, 'hasta', hasta, 'generado', now(),
    'estado_general', (select case when bool_or(c->>'estado' = 'fuera_de_servicio') then 'rojo'
                                   when bool_or(c->>'estado' = 'degradado') then 'amarillo'
                                   when count(*) filter (where c->>'estado' = 'operativo') = 0 then 'gris'
                                   when bool_or(c->>'estado' in ('sin_datos', 'sin_comprobar')) then 'amarillo_parcial'
                                   else 'verde' end from jsonb_array_elements(comp) c where c->>'estado' <> 'no_aplicable'),
    'componentes', comp, 'atencion', aten,
    'indicadores', jsonb_build_array(
      jsonb_build_object('clave', 'lecturas', 'nombre', 'Auditorías procesadas', 'valor', nl, 'unidad', 'lecturas', 'n', nl,
        'definicion', 'eventos de lectura recibidos con ts_evento en el periodo', 'anterior', ant->'lecturas'),
      jsonb_build_object('clave', 'tasa_errores', 'nombre', 'Tasa de errores de procesamiento', 'k', nerr, 'n', nl,
        'valor', case when nl > 0 then round(nerr::numeric / nl, 4) end, 'unidad', 'proporción', 'estado', case when nl < 30 then 'insuficiente' else 'suficiente' end,
        'definicion', 'lecturas con resultado error/timeout o alguna etapa en fallo/timeout, sobre todas las lecturas del periodo'),
      jsonb_build_object('clave', 'calidad_reconocimiento', 'nombre', 'Calidad de reconocimiento (OCR)', 'k', o_f, 'n', o_n,
        'valor', case when o_n > 0 then round(1 - o_f::numeric / o_n, 4) end, 'unidad', 'proporción de etapas ocr sin fallo',
        'estado', case when o_n < 30 then 'insuficiente' else 'suficiente' end,
        'definicion', 'mide que el OCR no falle; NO mide que lo leído sea correcto (eso es calidad de interpretación contra referencia humana)'),
      jsonb_build_object('clave', 'calidad_interpretacion', 'nombre', 'Calidad de interpretación (contra referencia humana)',
        'k', (select count(*) from lecturas l join veredicto_vigente v on v.lectura_id = l.id where l.ts_evento >= desde and l.ts_evento < hasta
              and l.resultado = 'oferta' and v.estado in ('referencia_doble', 'referencia_adjudicada') and v.clase = 'oferta'
              and abs(l.tarifa - v.tarifa) <= 0.01 and abs(l.km - v.km) <= 0.05 and abs(l.min - v.min) <= 0.5),
        'n', (select count(*) from lecturas l join veredicto_vigente v on v.lectura_id = l.id where l.ts_evento >= desde and l.ts_evento < hasta
              and l.resultado = 'oferta' and v.estado in ('referencia_doble', 'referencia_adjudicada')),
        'unidad', 'proporción', 'definicion', 'ofertas mostradas cuyos datos coinciden con la referencia humana (tarifa ±0.01, km ±0.05, min ±0.5), sobre ofertas mostradas con referencia',
        'cobertura_n', ofe),
      jsonb_build_object('clave', 'veredictos_pendientes', 'nombre', 'Errores sin veredicto humano', 'valor', sin_ver, 'n', nerr_l, 'unidad', 'casos',
        'definicion', 'errores_lector del periodo (sin reportes) sin referencia doble ni adjudicada'),
      jsonb_build_object('clave', 'incidentes', 'nombre', 'Incidentes activos', 'valor', jsonb_array_length(aten), 'unidad', 'alertas',
        'definicion', 'reglas de «atención requerida» que se cumplen ahora'),
      jsonb_build_object('clave', 'costo_ia_usd', 'nombre', 'Costo de la revisión con IA (USD)',
        'valor', (select case when count(*) > 0 then round(sum(costo_usd), 4) end from public.revisiones where creado >= desde and creado < hasta),
        'n', rv_n, 'unidad', 'USD', 'definicion', 'suma de revisiones.costo_usd (tarifas reales de precios_modelo al momento de cada revisión)')),
    'procesamiento', jsonb_build_object('recibidas', nl, 'procesadas', nl - nerr, 'fallidas', nerr,
      'pendientes', null, 'pendientes_nota', 'no hay cola en el servidor: el teléfono reintenta; sin dato',
      'ms_lectura_p50', round(p50, 0), 'ms_lectura_p90', round(p90, 0), 'ms_lectura_max', pmax,
      'reintentos', (select coalesce(sum(intentos - 1), 0) from recepciones rc join lecturas l on l.id = rc.lectura_id where l.ts_evento >= desde and l.ts_evento < hasta)),
    'calidad', jsonb_build_object('con_referencia', con_ref, 'sin_veredicto', sin_ver, 'conflictos', conf,
      'nota', 'la exactitud solo se calcula con casos con referencia confiable; los demás no cuentan como correctos'),
    'historia', (select coalesce(jsonb_agg(jsonb_build_object('dia', dia, 'lecturas', n, 'errores', e, 'ofertas', o, 'ms_p50', ms) order by dia), '[]')
                 from (select (ts_evento at time zone 'America/Mexico_City')::date dia, count(*) n, count(*) filter (where resultado in ('error', 'timeout')) e,
                              count(*) filter (where resultado = 'oferta') o, round((percentile_cont(0.5) within group (order by ms_lectura))::numeric, 0) ms
                       from lecturas where ts_evento >= desde and ts_evento < hasta group by 1) h),
    'costos_historia', (select coalesce(jsonb_agg(jsonb_build_object('dia', dia, 'usd', usd) order by dia), '[]')
                        from (select (creado at time zone 'America/Mexico_City')::date dia, round(sum(costo_usd), 4) usd from public.revisiones
                              where creado >= desde and creado < hasta group by 1) c));
  perform auditoria.anotar('salud', null, f, 1, 'ok');
  return r;
end $$;

-- ---------- 02 Errores: expediente y seguimiento ----------
-- Etapa de origen (regla documentada): reporte = conductor; sin lectura del lector = omisión; IA dice «no era oferta» = falso positivo;
-- tarifa distinta = interpretación de tarifa; km/min distintos = interpretación de tramos.
create or replace function auditoria.clasificar_error(tipo text, loc jsonb, ia jsonb) returns text
language sql immutable as $$
  select case
    when tipo = 'reporte' then 'reporte_conductor'
    when ia is null or ia = '{}'::jsonb then 'sin_revision_ia'
    when (ia->>'oferta') = 'false' then 'falso_positivo'
    when (loc->>'tarifa') is null and (ia->>'tarifa') is not null then 'omision'
    when (loc->>'tarifa') is distinct from (ia->>'tarifa') then 'interpretacion_tarifa'
    when (loc->>'km') is distinct from (ia->>'km') or (loc->>'min') is distinct from (ia->>'min') then 'interpretacion_tramos'
    else 'sin_diferencia' end
$$;

create or replace function public.admin_error(id bigint) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('id', id); e public.errores_lector; r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('error', id::text, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  select * into e from public.errores_lector x where x.id = admin_error.id;
  if not found then perform auditoria.anotar('error', id::text, f, 0, 'invalido'); return '{"error":"no_existe"}'; end if;
  select jsonb_build_object(
    'id', e.id, 'creado', e.creado, 'app', e.app, 'version', e.version, 'tipo', e.tipo, 'conductor', auditoria.seudonimo(e.usuario),
    'clasificacion', auditoria.clasificar_error(e.tipo, e.local, e.ia),
    -- Evidencia original (lo que se guardó) separada de lo interpretado después; sin líneas crudas (traen direcciones).
    'enlace', (select jsonb_build_object('lectura_id', k.lectura_id, 'resultado', k.resultado, 'captura_vinculo', k.captura_vinculo, 'captura_sha256', k.captura_sha256,
                 'fotograma_id', k.fotograma_id) from auditoria.enlace_capturas k where k.caso_tipo = 'error_lector' and k.caso_id = e.id
                 order by (k.resultado = 'oferta') desc, k.lectura_id limit 1),
    'evidencia', jsonb_build_object('lineas_guardadas', jsonb_array_length(e.lineas), 'captura', exists (select 1 from storage.objects o
                  where o.bucket_id = 'capturas' and o.name = e.id::text || '.webp'), 'captura_acceso', 'solo por la consola de Supabase (restringida)'),
    'lector', e.local, 'ia', e.ia, 'veredicto_heredado', e.veredicto,
    'veredicto_humano', (select jsonb_build_object('estado', v.estado, 'clase', v.clase, 'tarifa', v.tarifa, 'km', v.km, 'min', v.min, 'revisores', v.n_humanos)
                         from veredicto_vigente v where v.error_lector_id = e.id),
    'estado_seguimiento', coalesce((select s.estado from seguimiento_errores s where s.error_lector_id = e.id order by s.id desc limit 1), 'nuevo'),
    'historial', coalesce((select jsonb_agg(jsonb_build_object('creado', s.creado, 'estado', s.estado, 'causa', s.causa, 'nota', s.nota, 'admin', s.admin) order by s.id desc)
                           from seguimiento_errores s where s.error_lector_id = e.id), '[]'),
    'relacionados', jsonb_build_object('misma_version', (select count(*) from public.errores_lector x where x.version = e.version),
                                       'mismo_conductor', (select count(*) from public.errores_lector x where x.usuario = e.usuario))
  ) into r;
  perform auditoria.anotar('error', id::text, f, 1, 'ok');
  return r;
end $$;
-- (v.n_humanos puede no existir en la vista: ver la consulta de prueba; si no existe se omite con la vista actual.)

create or replace function public.admin_seguimiento(id bigint, estado text, causa text default null, nota text default null) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('id', id, 'estado', estado, 'causa', causa); nuevo bigint;
begin
  if auditoria.rol_actual() is distinct from 'revisor' then perform auditoria.anotar('seguimiento', id::text, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  begin
    insert into seguimiento_errores (error_lector_id, estado, causa, nota, admin)
    values (id, estado, causa, nota, auditoria.seudonimo(auth.uid())) returning seguimiento_errores.id into nuevo;
  exception when others then
    perform auditoria.anotar('seguimiento', id::text, f, 0, 'invalido'); return jsonb_build_object('error', 'seguimiento_invalido', 'codigo', sqlstate);
  end;
  perform auditoria.anotar('seguimiento', id::text, f, 1, 'ok');
  return jsonb_build_object('id', nuevo);
end $$;

-- ---------- 04 Versiones ----------
create or replace function public.admin_versiones(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; r jsonb;
begin
  g := auditoria.guardia('versiones', f, desde, hasta); if g is not null then return g; end if;
  select coalesce(jsonb_agg(v order by (v->>'primera') desc), '[]') into r from (
    select jsonb_build_object('version', l.version_app, 'primera', min(l.ts_evento), 'ultima', max(l.ts_evento), 'lecturas', count(*),
      'ofertas', count(*) filter (where l.resultado = 'oferta'),
      'errores_proceso', count(*) filter (where l.resultado in ('error', 'timeout')),
      'abstenciones', count(*) filter (where l.resultado in ('abstencion', 'varias')),
      'con_referencia', count(vv.lectura_id) filter (where l.resultado = 'oferta' and vv.estado in ('referencia_doble', 'referencia_adjudicada')),
      'coinciden', count(vv.lectura_id) filter (where l.resultado = 'oferta' and vv.estado in ('referencia_doble', 'referencia_adjudicada') and vv.clase = 'oferta'
                   and abs(l.tarifa - vv.tarifa) <= 0.01 and abs(l.km - vv.km) <= 0.05 and abs(l.min - vv.min) <= 0.5),
      'ms_popup_p50', round((percentile_cont(0.5) within group (order by l.ms_popup))::numeric, 0),
      'errores_lector', (select count(*) from public.errores_lector e where e.version = l.version_app and e.creado >= desde and e.creado < hasta),
      'conductores', count(distinct l.usuario)) v
    from lecturas l left join veredicto_vigente vv on vv.lectura_id = l.id
    where l.ts_evento >= desde and l.ts_evento < hasta group by l.version_app) x;
  perform auditoria.anotar('versiones', null, f, jsonb_array_length(r), 'ok');
  return jsonb_build_object('versiones', r,
    'nota_comparacion', 'No se declara mejor una versión con muestras distintas: compara solo versiones con n de referencia ≥ 30 cada una y revisa la cobertura.');
end $$;

-- ---------- 05 Conductores y dispositivos ----------
create or replace function public.admin_dispositivos(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; d jsonb; c jsonb;
begin
  g := auditoria.guardia('dispositivos', f, desde, hasta); if g is not null then return g; end if;
  select coalesce(jsonb_agg(x order by (x->>'lecturas')::int desc), '[]') into d from (
    select jsonb_build_object('telefono', coalesce(telefono, '(sin dato)'), 'android', coalesce(android, '?'),
      'versiones', jsonb_agg(distinct version_app), 'lecturas', count(*), 'ofertas', count(*) filter (where resultado = 'oferta'),
      'errores', count(*) filter (where resultado in ('error', 'timeout')), 'abstenciones', count(*) filter (where resultado in ('abstencion', 'varias')),
      'ultima', max(ts_evento), 'conductores', count(distinct usuario),
      'muestra_suficiente', count(*) >= 30) x
    from lecturas where ts_evento >= desde and ts_evento < hasta group by telefono, android) t;
  select coalesce(jsonb_agg(x order by (x->>'lecturas')::int desc), '[]') into c from (
    select jsonb_build_object('conductor', auditoria.seudonimo(usuario), 'lecturas', count(*), 'errores', count(*) filter (where resultado in ('error', 'timeout')),
      'versiones', jsonb_agg(distinct version_app), 'ultima', max(ts_evento),
      'errores_lector', (select count(*) from public.errores_lector e where e.usuario = l.usuario and e.creado >= desde and e.creado < hasta)) x
    from lecturas l where ts_evento >= desde and ts_evento < hasta group by usuario) t;
  perform auditoria.anotar('dispositivos', null, f, jsonb_array_length(d), 'ok');
  return jsonb_build_object('dispositivos', d, 'conductores', c,
    'nota', 'Una diferencia entre dispositivos no prueba causalidad: con n < 30 se marca muestra insuficiente.');
end $$;

-- ---------- 06 Auditoría IA ----------
create or replace function public.admin_ia(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; r jsonb;
begin
  g := auditoria.guardia('ia', f, desde, hasta); if g is not null then return g; end if;
  select jsonb_build_object(
    'discrepancias', coalesce((select jsonb_object_agg(k, n) from (select auditoria.clasificar_error(tipo, local, ia) k, count(*) n
        from public.errores_lector where creado >= desde and creado < hasta group by 1) x), '{}'::jsonb),
    'casos', (select count(*) from public.errores_lector where creado >= desde and creado < hasta),
    'con_referencia_humana', (select count(*) from public.errores_lector e join veredicto_vigente v on v.error_lector_id = e.id
        where e.creado >= desde and e.creado < hasta and v.estado in ('referencia_doble', 'referencia_adjudicada')),
    -- Contra la referencia humana: ¿quién acertó, el lector local o la IA? (n = casos con referencia de oferta)
    'componentes', jsonb_build_array(
      (select jsonb_build_object('componente', 'lector_local', 'n', count(*),
         'coinciden', count(*) filter (where abs((e.local->>'tarifa')::numeric - v.tarifa) <= 0.01 and abs((e.local->>'km')::numeric - v.km) <= 0.05 and abs((e.local->>'min')::numeric - v.min) <= 0.5))
       from public.errores_lector e join veredicto_vigente v on v.error_lector_id = e.id
       where e.creado >= desde and e.creado < hasta and v.estado in ('referencia_doble', 'referencia_adjudicada') and v.clase = 'oferta'),
      (select jsonb_build_object('componente', 'ia', 'modelo', (select string_agg(distinct modelo, ', ') from public.revisiones where creado >= desde and creado < hasta), 'n', count(*),
         'coinciden', count(*) filter (where abs((e.ia->>'tarifa')::numeric - v.tarifa) <= 0.01 and abs((e.ia->>'km')::numeric - v.km) <= 0.05 and abs((e.ia->>'min')::numeric - v.min) <= 0.5))
       from public.errores_lector e join veredicto_vigente v on v.error_lector_id = e.id
       where e.creado >= desde and e.creado < hasta and v.estado in ('referencia_doble', 'referencia_adjudicada') and v.clase = 'oferta')),
    'acuerdo_ia_lector', (select jsonb_build_object('n', count(*), 'coinciden', count(*) filter (where coincide)) from public.revisiones
                          where creado >= desde and creado < hasta and ok and modo <> 'rescate'),
    'fallas', coalesce((select jsonb_object_agg(tipo, n) from (select tipo, sum(n) n from public.fallas_ia where dia >= desde::date and dia < hasta::date + 1 group by 1) y), '{}'::jsonb),
    'nota', 'Casos de errores_lector: son difíciles y sesgados, no una muestra de producción. El acuerdo IA/lector no es exactitud. Solo cuenta como verdad la referencia humana.'
  ) into r;
  perform auditoria.anotar('ia', null, f, 1, 'ok');
  return r;
end $$;

-- ---------- 07 Configuración: costos ----------
create or replace function public.admin_costos(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; r jsonb;
begin
  g := auditoria.guardia('costos', f, desde, hasta); if g is not null then return g; end if;
  select jsonb_build_object(
    'por_modelo', coalesce((select jsonb_agg(jsonb_build_object('modelo', modelo, 'consultas', n, 'tokens_entrada', te, 'tokens_salida', ts, 'usd', usd, 'ms_ia_p50', ms) order by usd desc)
        from (select coalesce(modelo, '(sin dato)') modelo, count(*) n, sum(tokens_entrada) te, sum(tokens_salida) ts, round(sum(costo_usd), 4) usd,
                     round((percentile_cont(0.5) within group (order by ms_ia))::numeric, 0) ms
              from public.revisiones where creado >= desde and creado < hasta group by 1) m), '[]'),
    'por_modo', coalesce((select jsonb_agg(jsonb_build_object('modo', modo, 'consultas', n, 'usd', usd)) from (select modo, count(*) n, round(sum(costo_usd), 4) usd
        from public.revisiones where creado >= desde and creado < hasta group by 1) m), '[]'),
    'tarifas', coalesce((select jsonb_agg(to_jsonb(p)) from public.precios_modelo p), '[]'),
    'mes', coalesce((select to_jsonb(m) from public.consumo_global_mes m order by mes desc limit 1), 'null'::jsonb),
    'nota', 'Costos de revisiones.costo_usd con las tarifas de precios_modelo (no se inventan precios). Sin revisiones: arreglo vacío, no cero.') into r;
  perform auditoria.anotar('costos', null, f, 1, 'ok');
  return r;
end $$;

-- ---------- 07 Configuración: administradores, retención, almacenamiento y umbrales ----------
create or replace function public.admin_config() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('config', null, null, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  select jsonb_build_object(
    'yo', jsonb_build_object('admin', auditoria.seudonimo(auth.uid()), 'rol', auditoria.rol_actual()),
    'administradores', coalesce((select jsonb_agg(jsonb_build_object('admin', auditoria.seudonimo(usuario), 'rol', rol, 'alta', alta)) from administradores), '[]'),
    'permisos', jsonb_build_object('lector', jsonb_build_array('consultar'), 'revisor', jsonb_build_array('consultar', 'dar veredictos', 'cambiar seguimiento')),
    'retencion', jsonb_build_object('lecturas_dias', 30, 'lecturas_vencidas', (select count(*) from lecturas where expira < now()),
        'capturas_dias', 7, 'purga_programada', false, 'nota', 'la purga está definida pero no programada; no se modifica sin autorización'),
    'almacenamiento', jsonb_build_object('capturas_objetos', (select count(*) from storage.objects where bucket_id = 'capturas'),
        'capturas_bytes', (select coalesce(sum((metadata->>'size')::bigint), 0) from storage.objects where bucket_id = 'capturas'),
        'lecturas_filas', (select count(*) from lecturas)),
    'umbrales_alertas', jsonb_build_array(
        jsonb_build_object('regla', 'sin_recepciones', 'umbral', '24 h', 'gravedad', 'alta'),
        jsonb_build_object('regla', 'tasa_errores_proceso', 'umbral', '5 % (alta desde 10 %), n ≥ 30', 'gravedad', 'media/alta'),
        jsonb_build_object('regla', 'discrepancia_ia_lector', 'umbral', '30 %, n ≥ 30', 'gravedad', 'media'),
        jsonb_build_object('regla', 'abstenciones', 'umbral', '20 %, n ≥ 30', 'gravedad', 'media'),
        jsonb_build_object('regla', 'ia_sin_saldo', 'umbral', 'cualquier http_402', 'gravedad', 'alta')),
    'umbrales_editables', false,
    'esquema', (select coalesce(max(version), 0) from version_esquema)) into r;
  perform auditoria.anotar('config', null, null, 1, 'ok');
  return r;
end $$;

do $$ declare fn text; begin
  foreach fn in array array['public.admin_salud(timestamptz,timestamptz)', 'public.admin_error(bigint)',
    'public.admin_seguimiento(bigint,text,text,text)', 'public.admin_versiones(timestamptz,timestamptz)',
    'public.admin_dispositivos(timestamptz,timestamptz)', 'public.admin_ia(timestamptz,timestamptz)',
    'public.admin_costos(timestamptz,timestamptz)', 'public.admin_config()'] loop
    execute format('revoke all on function %s from public, anon', fn);
    execute format('grant execute on function %s to authenticated', fn);
  end loop;
end $$;
revoke all on function auditoria.clasificar_error(text, jsonb, jsonb) from public, anon, authenticated;

insert into auditoria.version_esquema (version, descripcion)
values (6, 'Centro de control: salud, expediente y seguimiento, versiones, dispositivos, IA, costos y configuración (E6/E7)') on conflict (version) do nothing;
