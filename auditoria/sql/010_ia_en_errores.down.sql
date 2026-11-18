-- Revierte 010: vuelve a admin_veredicto de 004 y quita la columna (ADVIERTE: se pierden las sugerencias guardadas).
drop function if exists public.admin_ia_precision_errores();
drop function if exists public.admin_veredicto(text, text, text, numeric, numeric, numeric, smallint, text, text, text, jsonb);
alter table auditoria.veredictos drop constraint if exists veredicto_ia_objeto;
alter table auditoria.veredictos drop column if exists ia_sugerencia;
create or replace function public.admin_veredicto(objetivo text, objetivo_id text, clase text, tarifa numeric default null,
  km numeric default null, min numeric default null, tarjeta smallint default null, confianza text default 'media',
  evidencia_sha256 text default null, nota text default null) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('objetivo', objetivo, 'objetivo_id', objetivo_id, 'clase', clase); nuevo bigint;
begin
  if auditoria.rol_actual() is distinct from 'revisor' then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'denegado'); return '{"error":"no_autorizado"}';
  end if;
  if objetivo not in ('lectura', 'error_lector') or evidencia_sha256 is null then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'invalido'); return '{"error":"objetivo_o_evidencia"}';
  end if;
  begin
    insert into veredictos (lectura_id, error_lector_id, fuente, autor, guia_version, clase, tarifa, km, min, tarjeta, confianza, estado, evidencia_sha256, nota)
    values (case when objetivo = 'lectura' then objetivo_id::uuid end, case when objetivo = 'error_lector' then objetivo_id::bigint end,
            'humano', 'admin_' || auditoria.seudonimo(auth.uid()), '1.0', clase, tarifa, km, min, tarjeta, confianza, 'validado',
            evidencia_sha256, nota)
    returning id into nuevo;
  exception when others then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'invalido');
    return jsonb_build_object('error', 'veredicto_invalido', 'codigo', sqlstate);
  end;
  perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 1, 'ok');
  return jsonb_build_object('id', nuevo);
end $$;
revoke all on function public.admin_veredicto(text,text,text,numeric,numeric,numeric,smallint,text,text,text) from public, anon;
grant execute on function public.admin_veredicto(text,text,text,numeric,numeric,numeric,smallint,text,text,text) to authenticated;
