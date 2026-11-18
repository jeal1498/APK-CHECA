-- Revierte 009: vuelve a la cola por fecha de 007.
drop function if exists public.admin_capturas_pendientes(text, integer, text);
-- Cola de capturas por revisar + conteos por color. p_nivel null = todos los colores.
create or replace function public.admin_capturas_pendientes(
  p_nivel text default null, p_limite integer default 10,
  p_desde timestamptz default '2026-11-18 03:48:00+00') returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('nivel', p_nivel, 'limite', p_limite, 'desde', p_desde); r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('capturas_pendientes', 'ofertas', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  if (p_nivel is not null and p_nivel not in ('verde', 'amarillo', 'rojo')) or p_limite is null or p_limite < 1 or p_limite > 50 then
    perform auditoria.anotar('capturas_pendientes', 'ofertas', f, 0, 'invalido'); return '{"error":"filtro_invalido"}';
  end if;
  with base as (
    select c.id, c.creado, c.app, c.nivel, c.local, o.name as nombre,
           v.resultado as ultima
    from public.capturas_ok c
    join lateral (select o2.name from storage.objects o2 where o2.bucket_id = 'capturas' and o2.name like '%-n' || c.id || '.webp' limit 1) o on true
    left join auditoria.revision_ofertas_vigente v on v.captura_ok_id = c.id
    where c.creado >= p_desde and c.nivel in ('verde', 'amarillo', 'rojo')
  ), pend as (select * from base where ultima is null or ultima = 'anulada')
  select jsonb_build_object(
    'pendientes', jsonb_build_object(
      'rojo',     (select count(*) from pend where nivel = 'rojo'),
      'amarillo', (select count(*) from pend where nivel = 'amarillo'),
      'verde',    (select count(*) from pend where nivel = 'verde')),
    'revisadas', jsonb_build_object(
      'rojo',     (select count(*) from base where nivel = 'rojo' and ultima is not null and ultima <> 'anulada'),
      'amarillo', (select count(*) from base where nivel = 'amarillo' and ultima is not null and ultima <> 'anulada'),
      'verde',    (select count(*) from base where nivel = 'verde' and ultima is not null and ultima <> 'anulada')),
    'items', coalesce((select jsonb_agg(jsonb_build_object('id', id, 'nombre', nombre, 'creado', creado, 'app', app, 'nivel', nivel, 'local', local) order by creado desc)
                       from (select * from pend where p_nivel is null or nivel = p_nivel order by creado desc limit p_limite) x), '[]'))
  into r;
  perform auditoria.anotar('capturas_pendientes', 'ofertas', f, jsonb_array_length(r -> 'items'), 'ok');
  return r;
end $$;

revoke all on function public.admin_capturas_pendientes(text, integer, timestamptz) from public, anon;
grant execute on function public.admin_capturas_pendientes(text, integer, timestamptz) to authenticated;
