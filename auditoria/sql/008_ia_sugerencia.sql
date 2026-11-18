-- 008 · Sugerencia de la IA junto a cada revisión de captura, y su precisión contra el veredicto humano.
-- Aditiva e idempotente (requiere 007). Reversión: 008_ia_sugerencia.down.sql (conserva las revisiones; quita la columna).

alter table auditoria.revisiones_ofertas add column if not exists ia_sugerencia jsonb;
do $$ begin
  alter table auditoria.revisiones_ofertas add constraint revof_ia_objeto check (ia_sugerencia is null or jsonb_typeof(ia_sugerencia) = 'object');
exception when duplicate_object then null; end $$;

-- Misma función que en 007, con un parámetro más (la sugerencia que vio la persona al decidir).
drop function if exists public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text);
create or replace function public.admin_revisar_captura(
  p_captura_ok_id bigint, p_resultado text, p_motivo text default null,
  p_tarifa numeric default null, p_km numeric default null, p_min numeric default null,
  p_nota text default null, p_evidencia_sha256 text default null, p_ia_sugerencia jsonb default null) returns jsonb
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
    insert into auditoria.revisiones_ofertas (captura_ok_id, captura_nombre, nivel, resultado, motivo, tarifa, km, min, nota, autor, evidencia_sha256, ia_sugerencia)
    values (c.id, nombre, c.nivel, p_resultado, p_motivo, p_tarifa, p_km, p_min, p_nota, 'admin_' || auditoria.seudonimo(auth.uid()), p_evidencia_sha256, p_ia_sugerencia)
    returning id into nuevo;
  exception when others then
    perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 0, 'invalido');
    return jsonb_build_object('error', 'revision_invalida', 'codigo', sqlstate);
  end;
  perform auditoria.anotar('revisar_captura', p_captura_ok_id::text, f, 1, 'ok');
  return jsonb_build_object('id', nuevo);
end $$;
revoke all on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text, jsonb) from public, anon;
grant execute on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text, jsonb) to authenticated;

-- Precisión de la IA contra la persona, solo con revisiones vigentes que traen sugerencia.
-- Verdad: «correcta» = lo que mostró el pop-up (sale del nombre del archivo: tarifa, km con 1 decimal, min enteros);
-- «valores_mal» = los valores de la persona; «falso_positivo» = no era una oferta vigente.
-- La IA acierta si (no era oferta y dijo algo distinto de «vigente») o (era oferta, dijo «vigente» y coinciden
-- tarifa ±0.01, km ±0.15, min ±1: el nombre del archivo redondea).
create or replace function public.admin_ia_precision() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare r jsonb; f jsonb := '{}'::jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('ia_precision', 'ofertas', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  with v as (
    select x.*, regexp_match(x.captura_nombre, '^ok-[a-z]+-([0-9.]+)-([0-9.]+)-([0-9.]+)-') as pop
    from auditoria.revision_ofertas_vigente x
    join auditoria.revisiones_ofertas y on y.id = x.id
    where x.resultado <> 'anulada' and y.ia_sugerencia is not null
  ), d as (
    select v.nivel, v.resultado, y.ia_sugerencia as ia,
      case v.resultado when 'valores_mal' then v.tarifa else (v.pop)[1]::numeric end as t,
      case v.resultado when 'valores_mal' then v.km else (v.pop)[2]::numeric end as k,
      case v.resultado when 'valores_mal' then v.min else (v.pop)[3]::numeric end as m
    from v join auditoria.revisiones_ofertas y on y.id = v.id
  ), e as (
    select *, (resultado <> 'falso_positivo') as es_oferta,
      (ia ->> 'estado') as ia_estado,
      abs(coalesce((ia ->> 'tarifa')::numeric, -1000) - t) <= 0.01 as t_ok,
      abs(coalesce((ia ->> 'km')::numeric, -1000) - k) <= 0.15 as k_ok,
      abs(coalesce((ia ->> 'min')::numeric, -1000) - m) <= 1 as m_ok
    from d
  )
  select jsonb_build_object(
    'n', count(*),
    'aciertos', count(*) filter (where (not es_oferta and ia_estado <> 'vigente') or (es_oferta and ia_estado = 'vigente' and t_ok and k_ok and m_ok)),
    'ofertas', count(*) filter (where es_oferta),
    'tarifa_ok', count(*) filter (where es_oferta and ia_estado = 'vigente' and t_ok),
    'km_ok', count(*) filter (where es_oferta and ia_estado = 'vigente' and k_ok),
    'min_ok', count(*) filter (where es_oferta and ia_estado = 'vigente' and m_ok),
    'falsos_positivos', count(*) filter (where not es_oferta),
    'falsos_positivos_detectados', count(*) filter (where not es_oferta and ia_estado <> 'vigente'),
    'minimo', 30) into r from e;
  perform auditoria.anotar('ia_precision', 'ofertas', f, 1, 'ok');
  return r;
end $$;
revoke all on function public.admin_ia_precision() from public, anon;
grant execute on function public.admin_ia_precision() to authenticated;
