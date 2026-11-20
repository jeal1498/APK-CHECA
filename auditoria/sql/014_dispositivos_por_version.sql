-- 014 · Dispositivos por versión: la versión más reciente que mandó cada teléfono (cuenta + modelo) en los últimos 14 días.
-- Sale de public.revisiones. Solo cuenta teléfonos que han mandado lecturas; las filas sin modelo de teléfono (versiones viejas) se omiten.
-- Aditiva e idempotente. Reversión: 014_dispositivos_por_version.down.sql.
create or replace function public.admin_dispositivos_version() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := '{}'::jsonb; r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('dispositivos_version', 'versiones', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  select coalesce(jsonb_agg(jsonb_build_object('version', d.version, 'codigo', left(d.usuario::text, 8), 'telefono', d.telefono, 'android', d.android, 'ultima', d.creado)
           order by string_to_array(d.version, '.')::int[] desc, d.creado desc), '[]'::jsonb) into r
  from (select distinct on (usuario, telefono) usuario, telefono, android, version, creado
        from public.revisiones
        where creado >= now() - interval '14 days' and version ~ '^[0-9]+\.[0-9]+$' and telefono is not null and telefono <> ''
        order by usuario, telefono, creado desc) d;
  perform auditoria.anotar('dispositivos_version', 'versiones', f, jsonb_array_length(r), 'ok');
  return jsonb_build_object('dias', 14, 'filas', r);
end $$;
revoke all on function public.admin_dispositivos_version() from public, anon;
grant execute on function public.admin_dispositivos_version() to authenticated;
