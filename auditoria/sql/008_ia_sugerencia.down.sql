-- Revierte 008: vuelve a la función de 007 y quita la columna (ADVIERTE: se pierden las sugerencias guardadas).
drop function if exists public.admin_ia_precision();
drop function if exists public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text, jsonb);
alter table auditoria.revisiones_ofertas drop constraint if exists revof_ia_objeto;
alter table auditoria.revisiones_ofertas drop column if exists ia_sugerencia;
create or replace function public.admin_revisar_captura(
  p_captura_ok_id bigint, p_resultado text, p_motivo text default null,
  p_tarifa numeric default null, p_km numeric default null, p_min numeric default null,
  p_nota text default null, p_evidencia_sha256 text default null) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('captura', p_captura_ok_id, 'resultado', p_resultado); c public.capturas_ok; nombre text; nuevo bigint;
begin
  if auditoria.rol_actual() is distinct from 'revisor' then
    perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 0, 'denegado'); return '{"error":"no_autorizado"}';
  end if;
  select * into c from public.capturas_ok where id = p_captura_ok_id;
  select o.name into nombre from storage.objects o where o.bucket_id = 'capturas' and o.name like '%-n' || p_captura_ok_id || '.webp' limit 1;
  if c.id is null or nombre is null then
    perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 0, 'invalido'); return '{"error":"no_existe"}';
  end if;
  begin
    insert into auditoria.revisiones_ofertas (captura_ok_id, captura_nombre, nivel, resultado, motivo, tarifa, km, min, nota, autor, evidencia_sha256)
    values (c.id, nombre, c.nivel, p_resultado, p_motivo, p_tarifa, p_km, p_min, p_nota, 'admin_' || auditoria.seudonimo(auth.uid()), p_evidencia_sha256)
    returning id into nuevo;
  exception when others then
    perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 0, 'invalido');
    return jsonb_build_object('error', 'revision_invalida', 'codigo', sqlstate);
  end;
  perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 1, 'ok');
  return jsonb_build_object('id', nuevo);
end $$;
revoke all on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text) from public, anon;
grant execute on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text) to authenticated;
