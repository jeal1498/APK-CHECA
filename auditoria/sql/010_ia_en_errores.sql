-- 010 · La sugerencia de la IA que lee la imagen también se guarda con el veredicto de un error,
-- y se mide contra la persona junto al Lector y la IA de texto (los mismos casos). Idempotente. Requiere 003 y 004.
-- Reversión: 010_ia_en_errores.down.sql (conserva los veredictos; quita la columna).

alter table auditoria.veredictos add column if not exists ia_sugerencia jsonb;
do $$ begin
  alter table auditoria.veredictos add constraint veredicto_ia_objeto check (ia_sugerencia is null or jsonb_typeof(ia_sugerencia) = 'object');
exception when duplicate_object then null; end $$;

-- Misma función que en 004, con un parámetro más al final (la sugerencia que vio la persona al decidir) y una mejora:
-- dar un veredicto de nuevo sobre el mismo objetivo lo sustituye (antes fallaba por la regla «un voto por persona»).
drop function if exists public.admin_veredicto(text, text, text, numeric, numeric, numeric, smallint, text, text, text);
create or replace function public.admin_veredicto(objetivo text, objetivo_id text, clase text, tarifa numeric default null,
  km numeric default null, min numeric default null, tarjeta smallint default null, confianza text default 'media',
  evidencia_sha256 text default null, nota text default null, ia_sugerencia jsonb default null) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('objetivo', objetivo, 'objetivo_id', objetivo_id, 'clase', clase); nuevo bigint; previo bigint; yo text;
begin
  if auditoria.rol_actual() is distinct from 'revisor' then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'denegado'); return '{"error":"no_autorizado"}';
  end if;
  if objetivo not in ('lectura', 'error_lector') or evidencia_sha256 is null then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'invalido'); return '{"error":"objetivo_o_evidencia"}';
  end if;
  yo := 'admin_' || auditoria.seudonimo(auth.uid());
  begin
    -- Si la misma persona ya dio un veredicto vigente para este objetivo, el nuevo lo SUSTITUYE (así se corrige un error sin borrar nada).
    select v.id into previo from veredictos v
     where v.autor = yo and v.estado <> 'descartado'
       and ((objetivo = 'lectura' and v.lectura_id = objetivo_id::uuid) or (objetivo = 'error_lector' and v.error_lector_id = objetivo_id::bigint))
       and not exists (select 1 from veredictos s where s.sustituye = v.id)
     order by v.id desc limit 1;
    insert into veredictos (lectura_id, error_lector_id, fuente, autor, guia_version, clase, tarifa, km, min, tarjeta, confianza, estado, evidencia_sha256, nota, ia_sugerencia, sustituye)
    values (case when objetivo = 'lectura' then objetivo_id::uuid end, case when objetivo = 'error_lector' then objetivo_id::bigint end,
            'humano', yo, '1.0', clase, tarifa, km, min, tarjeta, confianza, 'validado',
            evidencia_sha256, nota, ia_sugerencia, previo)
    returning id into nuevo;
  exception when others then
    perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 0, 'invalido');
    return jsonb_build_object('error', 'veredicto_invalido', 'codigo', sqlstate);
  end;
  perform auditoria.anotar('veredicto', objetivo || ':' || left(objetivo_id, 40), f, 1, 'ok');
  return jsonb_build_object('id', nuevo);
end $$;
revoke all on function public.admin_veredicto(text, text, text, numeric, numeric, numeric, smallint, text, text, text, jsonb) from public, anon;
grant execute on function public.admin_veredicto(text, text, text, numeric, numeric, numeric, smallint, text, text, text, jsonb) to authenticated;

-- Quién acierta en los errores que tienen veredicto humano y sugerencia de la IA de imagen (último veredicto por error).
-- Verdad = el veredicto de la persona. Acierta si coincide (tarifa ±0.01, km ±0.15, min ±1) o, si la persona dijo que no era
-- una oferta, si la IA dijo algo distinto de «vigente». Lector e IA de texto se miden solo en las que SÍ eran oferta
-- (en las que no, el lector no deja una respuesta comparable). Mismos casos para los tres: la comparación es justa.
create or replace function public.admin_ia_precision_errores() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare r jsonb; f jsonb := '{}'::jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('ia_precision_errores', 'errores', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  with ult as (
    select distinct on (v.error_lector_id) v.error_lector_id, v.clase, v.tarifa, v.km, v.min, v.ia_sugerencia
    from auditoria.veredictos v
    where v.fuente = 'humano' and v.estado = 'validado' and v.error_lector_id is not null
    order by v.error_lector_id, v.id desc
  ), d as (
    select u.*, e.local as lec, e.ia as txt from ult u join public.errores_lector e on e.id = u.error_lector_id where u.ia_sugerencia is not null
  ), c as (
    select *, (clase = 'oferta') as es_oferta,
      (ia_sugerencia ->> 'estado') as v_estado,
      coalesce(abs((ia_sugerencia ->> 'tarifa')::numeric - tarifa) <= 0.01, false) and coalesce(abs((ia_sugerencia ->> 'km')::numeric - km) <= 0.15, false)
        and coalesce(abs((ia_sugerencia ->> 'min')::numeric - min) <= 1, false) as v_vals,
      coalesce(abs((lec ->> 'tarifa')::numeric - tarifa) <= 0.01, false) and coalesce(abs((lec ->> 'km')::numeric - km) <= 0.15, false)
        and coalesce(abs((lec ->> 'min')::numeric - min) <= 1, false) as l_vals,
      coalesce((txt ->> 'oferta') is distinct from 'false', true)
        and coalesce(abs((txt ->> 'tarifa')::numeric - tarifa) <= 0.01, false) and coalesce(abs((txt ->> 'km')::numeric - km) <= 0.15, false)
        and coalesce(abs((txt ->> 'min')::numeric - min) <= 1, false) as t_vals
    from d
  )
  select jsonb_build_object(
    'n', count(*),
    'ofertas', count(*) filter (where es_oferta),
    'vision', count(*) filter (where (not es_oferta and v_estado <> 'vigente') or (es_oferta and v_estado = 'vigente' and v_vals)),
    'vision_ofertas', count(*) filter (where es_oferta and v_estado = 'vigente' and v_vals),
    'lector_ofertas', count(*) filter (where es_oferta and l_vals),
    'texto_ofertas', count(*) filter (where es_oferta and t_vals),
    'minimo', 30) into r from c;
  perform auditoria.anotar('ia_precision_errores', 'errores', f, 1, 'ok');
  return r;
end $$;
revoke all on function public.admin_ia_precision_errores() from public, anon;
grant execute on function public.admin_ia_precision_errores() to authenticated;
