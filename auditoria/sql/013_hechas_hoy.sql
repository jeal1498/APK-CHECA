-- 013 · Cuántas revisiones se hicieron hoy (día de Ciudad de México): avisos (último veredicto vigente) + errores decididos.
-- La tarjeta «Tu meta de hoy» de Admin › Hoy la usa para la barra de avance. Aditiva e idempotente. Reversión: 013_hechas_hoy.down.sql.
create or replace function public.admin_hechas_hoy() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := '{}'::jsonb; ini timestamptz := (date_trunc('day', now() at time zone 'America/Mexico_City')) at time zone 'America/Mexico_City'; n int;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('hechas_hoy', 'revisar', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  select (select count(*) from auditoria.revision_ofertas_vigente where creado >= ini and resultado <> 'anulada')
       + (select count(distinct error_lector_id) from auditoria.veredictos where error_lector_id is not null and estado <> 'descartado' and creado >= ini) into n;
  perform auditoria.anotar('hechas_hoy', 'revisar', f, 1, 'ok');
  return jsonb_build_object('hechas', n, 'desde', ini);
end $$;
revoke all on function public.admin_hechas_hoy() from public, anon;
grant execute on function public.admin_hechas_hoy() to authenticated;
