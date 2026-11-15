-- E7 · 005 — Métricas confiables (PREPARADO, NO APLICADO). Requiere 001–004.
-- Cada métrica sale de UNA fuente con fórmula, población y ventana explícitas (catálogo auditoria.metricas_def).
-- Reglas: sin denominador válido no hay valor («no_medible» con motivo); n < 30 = «insuficiente»;
-- el acuerdo IA/lector nunca se presenta como exactitud; lo histórico del banco va aparte de la telemetría.

create table if not exists auditoria.metricas_def (
  clave       text primary key,
  grupo       text not null check (grupo in ('operacion', 'calidad_humana', 'acuerdo_ia', 'banco_historico')),
  nombre      text not null,
  formula     text not null,
  poblacion   text not null,
  fuente      text not null,
  exclusiones text
);
alter table auditoria.metricas_def enable row level security;
revoke all on auditoria.metricas_def from public, anon, authenticated;

insert into auditoria.metricas_def (clave, grupo, nombre, formula, poblacion, fuente, exclusiones) values
 ('lecturas', 'operacion', 'Eventos de lectura', 'count(*)', 'eventos con ts_evento en la ventana', 'auditoria.lecturas', null),
 ('ofertas_mostradas', 'operacion', 'Pop-ups mostrados', 'count(resultado = oferta)', 'eventos de la ventana', 'auditoria.lecturas', null),
 ('duplicadas', 'operacion', 'Duplicadas suprimidas', 'duplicada / (oferta + duplicada)', 'eventos oferta o duplicada', 'auditoria.lecturas', null),
 ('abstenciones', 'operacion', 'Abstenciones', '(abstencion + varias) / (oferta + abstencion + varias)', 'eventos con decisión', 'auditoria.lecturas', 'duplicadas, errores'),
 ('captura_exacta', 'operacion', 'Ofertas con captura exacta', 'captura_vinculo = exacto / oferta', 'ofertas mostradas', 'auditoria.lecturas', null),
 ('ms_popup_p50', 'operacion', 'Latencia del pop-up p50 (ms)', 'percentil 50 de ms_popup', 'ofertas con ms_popup', 'auditoria.lecturas', 'sin ms_popup'),
 ('ms_popup_p90', 'operacion', 'Latencia del pop-up p90 (ms)', 'percentil 90 de ms_popup', 'ofertas con ms_popup', 'auditoria.lecturas', 'sin ms_popup'),
 ('reintentos', 'operacion', 'Reintentos de envío recibidos', 'sum(intentos - 1)', 'eventos de la ventana', 'auditoria.recepciones', null),
 ('con_referencia_humana', 'calidad_humana', 'Ofertas con referencia humana', 'ofertas con veredicto referencia_humana/doble / ofertas', 'ofertas mostradas', 'auditoria.veredicto_vigente', null),
 ('precision_humana', 'calidad_humana', 'Precisión de pop-ups (humana)', 'pop-ups correctos / pop-ups con referencia humana', 'ofertas con referencia humana', 'lecturas × veredicto_vigente', 'sin referencia, conflicto_humano, ilegible, cambio_pantalla'),
 ('falsos_positivos_humana', 'calidad_humana', 'Pop-ups sobre algo que no era oferta', 'referencia no_oferta / pop-ups con referencia humana', 'ofertas con referencia humana', 'lecturas × veredicto_vigente', 'igual que precision_humana'),
 ('mezcla_tarjetas_humana', 'calidad_humana', 'Pop-ups de una oferta con varias tarjetas', 'referencia varias / pop-ups con referencia humana', 'ofertas con referencia humana', 'lecturas × veredicto_vigente', 'igual que precision_humana'),
 ('recall', 'calidad_humana', 'Ofertas reales que el lector no mostró', 'NO MEDIBLE con telemetría', 'requiere ofertas reales sin pop-up registradas', '—', 'los eventos no registran ofertas no vistas'),
 ('conflictos_humanos', 'calidad_humana', 'Casos con desacuerdo humano', 'count(estado = conflicto_humano)', 'veredictos vigentes de lecturas de la ventana', 'auditoria.veredicto_vigente', null),
 ('acuerdo_ia_lector', 'acuerdo_ia', 'Acuerdo IA/lector (NO es exactitud)', 'coincide / revisadas', 'revisiones ok, sin rescates', 'public.revisiones', 'rescates'),
 ('banco_heredado', 'banco_historico', 'Banco con etiquetas heredadas (NO humanas)', 'aciertos / total de la última medición', 'banco de casos guardados', 'public.banco_lector', 'no es una muestra de producción')
on conflict (clave) do update set grupo = excluded.grupo, nombre = excluded.nombre, formula = excluded.formula,
  poblacion = excluded.poblacion, fuente = excluded.fuente, exclusiones = excluded.exclusiones;

-- Fila de métrica: valor solo si hay denominador; «suficiente» si n ≥ 30.
create or replace function auditoria.fila_metrica(clave text, k numeric, n numeric, valor numeric default null, motivo text default null) returns jsonb
language sql stable set search_path = auditoria, pg_temp as $$
  select jsonb_build_object('clave', d.clave, 'grupo', d.grupo, 'nombre', d.nombre, 'formula', d.formula, 'poblacion', d.poblacion,
    'fuente', d.fuente, 'exclusiones', d.exclusiones, 'k', k, 'n', n,
    'valor', case when motivo is not null then null when valor is not null then valor when n > 0 then round(k / n, 4) end,
    'estado', case when motivo is not null then 'no_medible' when coalesce(n, 0) = 0 then 'sin_datos'
                   when n < 30 then 'insuficiente' else 'suficiente' end,
    'motivo', motivo)
  from auditoria.metricas_def d where d.clave = fila_metrica.clave
$$;

-- Ventana filtrada de eventos (una sola definición para todas las métricas).
create or replace function auditoria.ventana(desde timestamptz, hasta timestamptz, p_app text, p_version text)
returns setof auditoria.lecturas language sql stable set search_path = auditoria, pg_temp as $$
  select * from auditoria.lecturas where ts_evento >= desde and ts_evento < hasta
    and (p_app is null or app = p_app) and (p_version is null or version_app = p_version)
$$;
revoke all on function auditoria.ventana(timestamptz, timestamptz, text, text) from public, anon, authenticated;

-- Tolerancias humanas (las de auditoria/etiquetado/metricas.py): tarifa ±0.01, km ±0.05, min ±0.5.
create or replace function auditoria.metricas(desde timestamptz, hasta timestamptz, p_app text default null, p_version text default null)
returns jsonb language plpgsql stable security definer set search_path = auditoria, public, pg_temp as $$
declare r jsonb := '[]'; ofe int; dup int; abs_ int; exa int; tot int; rei int; p50 numeric; p90 numeric; nlat int;
        conref int; ok_ int; fp int; mez int; conf int; rev int; coi int; b record;
begin
  select count(*), count(*) filter (where resultado = 'oferta'), count(*) filter (where resultado = 'duplicada'),
         count(*) filter (where resultado in ('abstencion', 'varias')), count(*) filter (where resultado = 'oferta' and captura_vinculo = 'exacto')
    into tot, ofe, dup, abs_, exa from auditoria.ventana(desde, hasta, p_app, p_version);
  select percentile_cont(0.5) within group (order by ms_popup), percentile_cont(0.9) within group (order by ms_popup), count(ms_popup)
    into p50, p90, nlat from auditoria.ventana(desde, hasta, p_app, p_version) where resultado = 'oferta' and ms_popup is not null;
  select coalesce(sum(rc.intentos - 1), 0) into rei from recepciones rc join auditoria.ventana(desde, hasta, p_app, p_version) w on w.id = rc.lectura_id;
  -- Calidad humana: solo ofertas mostradas con referencia humana (sin conflicto).
  select count(*),
         count(*) filter (where v.clase = 'oferta' and abs(l.tarifa - v.tarifa) <= 0.01 and abs(l.km - v.km) <= 0.05 and abs(l.min - v.min) <= 0.5),
         count(*) filter (where v.clase = 'no_oferta'), count(*) filter (where v.clase = 'varias')
    into conref, ok_, fp, mez
    from auditoria.ventana(desde, hasta, p_app, p_version) l join veredicto_vigente v on v.lectura_id = l.id
   where l.resultado = 'oferta' and v.estado in ('referencia_humana', 'referencia_doble') and v.clase in ('oferta', 'no_oferta', 'varias');
  select count(*) into conf from auditoria.ventana(desde, hasta, p_app, p_version) l join veredicto_vigente v on v.lectura_id = l.id where v.estado = 'conflicto_humano';
  select count(*) filter (where ok and modo <> 'rescate'), count(*) filter (where ok and modo <> 'rescate' and coincide)
    into rev, coi from public.revisiones x where x.creado >= desde and x.creado < hasta
      and (p_app is null or lower(x.app) = p_app) and (p_version is null or x.version = p_version);
  select * into b from public.banco_lector order by medido desc, version desc limit 1;

  r := jsonb_build_array(
    fila_metrica('lecturas', tot, tot, tot),
    fila_metrica('ofertas_mostradas', ofe, tot, ofe),
    fila_metrica('duplicadas', dup, ofe + dup),
    fila_metrica('abstenciones', abs_, ofe + abs_),
    fila_metrica('captura_exacta', exa, ofe),
    fila_metrica('ms_popup_p50', null, nlat, round(p50::numeric, 0)),
    fila_metrica('ms_popup_p90', null, nlat, round(p90::numeric, 0)),
    fila_metrica('reintentos', rei, tot, rei),
    fila_metrica('con_referencia_humana', conref, ofe),
    fila_metrica('precision_humana', ok_, conref),
    fila_metrica('falsos_positivos_humana', fp, conref),
    fila_metrica('mezcla_tarjetas_humana', mez, conref),
    fila_metrica('recall', null, null, null, 'sin denominador: la telemetría no registra ofertas reales que no se vieron'),
    fila_metrica('conflictos_humanos', conf, ofe, conf),
    fila_metrica('acuerdo_ia_lector', coi, rev),
    case when b is null then fila_metrica('banco_heredado', null, null, null, 'sin mediciones en banco_lector')
         else fila_metrica('banco_heredado', b.aciertos, b.total) || jsonb_build_object('version', b.version, 'medido', b.medido) end
  );
  -- En los conteos (lecturas, ofertas, reintentos, conflictos) «n» es el tamaño de la población que los produce.
  return jsonb_build_object('desde', desde, 'hasta', hasta, 'app', p_app, 'version', p_version, 'min_n', 30, 'metricas', r);
end $$;
revoke all on function auditoria.metricas(timestamptz, timestamptz, text, text) from public, anon, authenticated;

-- Segmentos (app × versión × fabricante) con su tamaño: el panel marca los pequeños.
create or replace function auditoria.segmentos(desde timestamptz, hasta timestamptz) returns jsonb
language sql stable security definer set search_path = auditoria, pg_temp as $$
  select coalesce(jsonb_agg(jsonb_build_object('app', app, 'version', version_app, 'fabricante', fab, 'n', n,
            'ofertas', ofe, 'abstenciones', abs_, 'suficiente', n >= 30) order by app, version_app desc, fab), '[]')
  from (select app, version_app, coalesce(nullif(split_part(telefono, ' ', 1), ''), '(sin dato)') as fab, count(*) n,
               count(*) filter (where resultado = 'oferta') ofe, count(*) filter (where resultado in ('abstencion', 'varias')) abs_
        from lecturas where ts_evento >= desde and ts_evento < hasta group by 1, 2, 3) s
$$;
revoke all on function auditoria.segmentos(timestamptz, timestamptz) from public, anon, authenticated;

-- Tendencia diaria (sin atribuir causas).
create or replace function auditoria.tendencia(desde timestamptz, hasta timestamptz, p_app text default null) returns jsonb
language sql stable security definer set search_path = auditoria, pg_temp as $$
  select coalesce(jsonb_agg(jsonb_build_object('dia', dia, 'lecturas', n, 'ofertas', ofe, 'duplicadas', dup) order by dia), '[]')
  from (select (ts_evento at time zone 'America/Mexico_City')::date as dia, count(*) n,
               count(*) filter (where resultado = 'oferta') ofe, count(*) filter (where resultado = 'duplicada') dup
        from lecturas where ts_evento >= desde and ts_evento < hasta and (p_app is null or app = p_app) group by 1) t
$$;
revoke all on function auditoria.tendencia(timestamptz, timestamptz, text) from public, anon, authenticated;

-- Puerta del panel (mismo control y bitácora que el Admin).
create or replace function public.admin_metricas(desde timestamptz, hasta timestamptz, app text default null, version text default null) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta, 'app', app, 'version', version); err text; r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('metricas', null, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  err := auditoria.filtros_invalidos(desde, hasta, 1, 1);
  if err is null and app is not null and app not in ('uber', 'didi', 'otra') then err := 'app'; end if;
  if err is not null then perform auditoria.anotar('metricas', null, f, 0, 'invalido'); return jsonb_build_object('error', err); end if;
  r := auditoria.metricas(desde, hasta, app, version)
       || jsonb_build_object('segmentos', auditoria.segmentos(desde, hasta), 'tendencia', auditoria.tendencia(desde, hasta, app));
  perform auditoria.anotar('metricas', null, f, jsonb_array_length(r->'metricas'), 'ok');
  return r;
end $$;
revoke all on function public.admin_metricas(timestamptz, timestamptz, text, text) from public, anon;
grant execute on function public.admin_metricas(timestamptz, timestamptz, text, text) to authenticated;

insert into auditoria.version_esquema (version, descripcion)
values (5, 'métricas con definiciones, segmentos y tendencia (E7)') on conflict (version) do nothing;
