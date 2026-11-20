-- 012 · Calidad con lo que Eduardo revisa de verdad (avisos normales y errores), por versión y por app.
-- Antes Calidad solo contaba «referencia doble» (dos personas) y no veía las revisiones de pop-ups: salía «Aún no se puede medir».
-- Aditiva e idempotente. Reversión: 012_calidad_con_revision.down.sql.
--   avisos  = pop-ups normales (capturas_ok con captura) vistos por una persona: correcta / valores_mal / falso_positivo.
--   errores = casos de errores_lector con veredicto de una persona: acertó el lector, la IA, otro valor, no era oferta o ilegible.

create or replace function public.admin_calidad(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); g jsonb; r jsonb;
begin
  g := auditoria.guardia('calidad', f, desde, hasta); if g is not null then return g; end if;
  with pop as (
    select c.id, c.version, lower(c.app) as app, c.nivel, c.ia, c.local,
           v.resultado, (v.resultado is not null and v.resultado <> 'anulada') as rev,
           v.tarifa as vt, v.km as vk, v.min as vm
    from public.capturas_ok c left join auditoria.revision_ofertas_vigente v on v.captura_ok_id = c.id
    where c.nivel in ('verde','amarillo','rojo') and auditoria.ver_ok(c.version) and c.creado >= desde and c.creado < hasta
      and exists (select 1 from storage.objects o where o.bucket_id = 'capturas' and o.name like '%-n' || c.id || '.webp')
  ), pg as (
    select version, app, nivel, resultado, rev,
      (rev and ia is not null) as con_ia,
      coalesce(case when not rev or ia is null then null else case resultado
        when 'falso_positivo' then (ia->>'oferta') = 'false'
        when 'correcta' then (ia->>'oferta') is distinct from 'false' and abs((ia->>'tarifa')::numeric - (local->>'tarifa')::numeric) <= 0.01 and abs((ia->>'km')::numeric - (local->>'km')::numeric) <= 0.15 and abs((ia->>'min')::numeric - (local->>'min')::numeric) <= 1
        else (ia->>'oferta') is distinct from 'false' and abs((ia->>'tarifa')::numeric - vt) <= 0.01 and abs((ia->>'km')::numeric - vk) <= 0.15 and abs((ia->>'min')::numeric - vm) <= 1 end end, false) as ia_ok
    from pop
  ), err as (
    select e.version, lower(e.app) as app,
      case when v.id is null then null when v.clase in ('ilegible','no_oferta') then v.clase
           when v.clase = 'oferta' and abs((e.local->>'tarifa')::numeric - v.tarifa) <= 0.01 and abs((e.local->>'km')::numeric - v.km) <= 0.05 and abs((e.local->>'min')::numeric - v.min) <= 0.5 then 'lector'
           when v.clase = 'oferta' and abs((e.ia->>'tarifa')::numeric - v.tarifa) <= 0.01 and abs((e.ia->>'km')::numeric - v.km) <= 0.15 and abs((e.ia->>'min')::numeric - v.min) <= 1 then 'ia'
           else 'otro_valor' end as quien,
      (exists (select 1 from storage.objects o where o.bucket_id = 'capturas' and o.name = e.id::text || '.webp')
       or exists (select 1 from auditoria.enlace_capturas k where k.caso_tipo = 'error_lector' and k.caso_id = e.id and k.captura_sha256 is not null)) as hay
    from auditoria.errores_lector_v e
    left join lateral (select x.id, x.clase, x.tarifa, x.km, x.min from auditoria.veredictos x where x.error_lector_id = e.id and x.estado <> 'descartado' order by x.id desc limit 1) v on true
    where auditoria.ver_ok(e.version) and e.creado >= desde and e.creado < hasta
  ), claves as (select version, app from pg union select version, app from err)
  select coalesce(jsonb_agg(jsonb_build_object(
      'version', k.version, 'app', k.app,
      'avisos', jsonb_build_object(
        'revisados', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.rev),
        'correctas', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.resultado = 'correcta'),
        'valores_mal', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.resultado = 'valores_mal'),
        'falsos_positivos', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.resultado = 'falso_positivo'),
        'pendientes', (select count(*) from pg x where x.version = k.version and x.app = k.app and not x.rev),
        'con_ia', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.con_ia),
        'ia_acerto', (select count(*) from pg x where x.version = k.version and x.app = k.app and x.con_ia and x.ia_ok),
        'por_color', (select coalesce(jsonb_object_agg(n, jsonb_build_object(
            'revisados', (select count(*) from pg y where y.version = k.version and y.app = k.app and y.nivel = n and y.rev),
            'correctas', (select count(*) from pg y where y.version = k.version and y.app = k.app and y.nivel = n and y.resultado = 'correcta'))), '{}'::jsonb)
          from unnest(array['rojo','amarillo','verde']) n)),
      'errores', jsonb_build_object(
        'decididos', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien is not null),
        'lector', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien = 'lector'),
        'ia', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien = 'ia'),
        'otro_valor', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien = 'otro_valor'),
        'no_oferta', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien = 'no_oferta'),
        'ilegible', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien = 'ilegible'),
        'pendientes', (select count(*) from err x where x.version = k.version and x.app = k.app and x.quien is null and x.hay))
    ) order by k.version desc, k.app), '[]'::jsonb) into r
  from claves k;
  perform auditoria.anotar('calidad', null, f, jsonb_array_length(r), 'ok');
  return jsonb_build_object('filas', r, 'nota', 'Avisos = pop-ups normales revisados por una persona (la medida principal). Errores = casos donde el lector y la IA no coincidieron: son difíciles y sesgados, no una muestra de producción.');
end $$;

revoke all on function public.admin_calidad(timestamptz, timestamptz) from public, anon;
grant execute on function public.admin_calidad(timestamptz, timestamptz) to authenticated;
