-- 009 · La cola de «Revisar ofertas» se filtra por la VERSIÓN de la app (capturas_ok.version), no por fecha.
-- Corrige 007: se usó la fecha de salida de la 10.82 y entraron capturas de teléfonos que aún corren la 10.80.
-- Además cada captura trae lo que leyó el lector (`local`) y la revisión de IA (`ia`). Idempotente. Requiere 007 y 008.
-- Reversión: 009_cola_por_version.down.sql (vuelve a la cola por fecha de 007).

drop function if exists public.admin_capturas_pendientes(text, integer, timestamptz);
drop function if exists public.admin_capturas_pendientes(text, integer, text);
create or replace function public.admin_capturas_pendientes(
  p_nivel text default null, p_limite integer default 10, p_version_min text default '10.82') returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('nivel', p_nivel, 'limite', p_limite, 'version_min', p_version_min); r jsonb; vmin int[];
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('capturas_pendientes', 'ofertas', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  if (p_nivel is not null and p_nivel not in ('verde', 'amarillo', 'rojo')) or p_limite is null or p_limite < 1 or p_limite > 50
     or p_version_min is null or p_version_min !~ '^[0-9]{1,3}(\.[0-9]{1,3}){0,2}$' then
    perform auditoria.anotar('capturas_pendientes', 'ofertas', f, 0, 'invalido'); return '{"error":"filtro_invalido"}';
  end if;
  vmin := string_to_array(p_version_min, '.')::int[];
  with base as (
    select c.id, c.creado, c.app, c.version, c.nivel, c.local, c.ia, o.name as nombre, v.resultado as ultima
    from public.capturas_ok c
    join lateral (select o2.name from storage.objects o2 where o2.bucket_id = 'capturas' and o2.name like '%-n' || c.id || '.webp' limit 1) o on true
    left join auditoria.revision_ofertas_vigente v on v.captura_ok_id = c.id
    where c.nivel in ('verde', 'amarillo', 'rojo') and c.version ~ '^[0-9]{1,3}(\.[0-9]{1,3}){0,2}$'
      and string_to_array(c.version, '.')::int[] >= vmin
  ), pend as (select * from base where ultima is null or ultima = 'anulada')
  select jsonb_build_object(
    'version_min', p_version_min,
    'pendientes', jsonb_build_object(
      'rojo',     (select count(*) from pend where nivel = 'rojo'),
      'amarillo', (select count(*) from pend where nivel = 'amarillo'),
      'verde',    (select count(*) from pend where nivel = 'verde')),
    'revisadas', jsonb_build_object(
      'rojo',     (select count(*) from base where nivel = 'rojo' and ultima is not null and ultima <> 'anulada'),
      'amarillo', (select count(*) from base where nivel = 'amarillo' and ultima is not null and ultima <> 'anulada'),
      'verde',    (select count(*) from base where nivel = 'verde' and ultima is not null and ultima <> 'anulada')),
    'items', coalesce((select jsonb_agg(jsonb_build_object('id', id, 'nombre', nombre, 'creado', creado, 'app', app, 'version', version, 'nivel', nivel, 'local', local, 'ia', ia) order by creado desc)
                       from (select * from pend where p_nivel is null or nivel = p_nivel order by creado desc limit p_limite) x), '[]'))
  into r;
  perform auditoria.anotar('capturas_pendientes', 'ofertas', f, jsonb_array_length(r -> 'items'), 'ok');
  return r;
end $$;
revoke all on function public.admin_capturas_pendientes(text, integer, text) from public, anon;
grant execute on function public.admin_capturas_pendientes(text, integer, text) to authenticated;
