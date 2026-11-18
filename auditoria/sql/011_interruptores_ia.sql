-- 011 · Interruptores remotos de la IA (visión y texto) y sus tiempos de respuesta, vistos desde el Admin.
-- La función confirmar-oferta lee public.ia_config cada 30 s. Aditiva e idempotente. Reversión: 011_interruptores_ia.down.sql.
--   vision = la IA lee la IMAGEN que manda el teléfono; texto = la IA de texto de siempre.
--   Apagado: la función responde «apagada» y el teléfono sigue solo con su lector. Sin fila, todo queda encendido.

create table if not exists public.ia_config (
  id          integer primary key default 1 check (id = 1),
  vision      boolean not null default true,
  texto       boolean not null default true,
  actualizado timestamptz not null default now(),
  por         text
);
insert into public.ia_config (id) values (1) on conflict (id) do nothing;
alter table public.ia_config enable row level security;
revoke all on public.ia_config from public, anon, authenticated;

-- Estado de los interruptores + tiempos de la IA (últimos 7 días). Cualquier administrador.
-- ms_ia = lo que tarda DeepSeek en el servidor; ms_total = ida y vuelta vista desde el teléfono (la manda en la siguiente revisión).
create or replace function public.admin_ia_estado() returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare r jsonb; c public.ia_config; f jsonb := '{}'::jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('ia_estado', 'ia', f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  select * into c from public.ia_config where id = 1;
  select jsonb_build_object(
    'vision', coalesce(c.vision, true), 'texto', coalesce(c.texto, true), 'actualizado', c.actualizado, 'por', c.por,
    'modos', (select coalesce(jsonb_object_agg(m.modo, m.d), '{}'::jsonb) from (
      select case when imagenes > 0 then 'vision' else 'texto' end as modo,
        jsonb_build_object(
          'n', count(*), 'ok', count(*) filter (where ok),
          'sin_saldo', count(*) filter (where error = 'http_402'),
          'apagadas', 0,
          'errores', count(*) filter (where not ok),
          'ms_ia_p50', round((percentile_cont(0.5) within group (order by ms_ia) filter (where ok and ms_ia is not null))::numeric),
          'ms_ia_p90', round((percentile_cont(0.9) within group (order by ms_ia) filter (where ok and ms_ia is not null))::numeric),
          'ms_total_p50', round((percentile_cont(0.5) within group (order by ms_revision_anterior) filter (where ms_revision_anterior > 0))::numeric),
          'ms_total_p90', round((percentile_cont(0.9) within group (order by ms_revision_anterior) filter (where ms_revision_anterior > 0))::numeric),
          'tokens_entrada', coalesce(sum(tokens_entrada), 0), 'costo_usd', round(coalesce(sum(costo_usd), 0), 4)) as d
      from public.revisiones where creado >= now() - interval '7 days' group by 1) m))
  into r;
  perform auditoria.anotar('ia_estado', 'ia', f, 1, 'ok');
  return r;
end $$;

-- Enciende o apaga una de las dos IA. Solo «revisor». Queda en la bitácora.
create or replace function public.admin_ia_interruptor(p_que text, p_activo boolean) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('que', p_que, 'activo', p_activo);
begin
  if auditoria.rol_actual() is distinct from 'revisor' then perform auditoria.anotar('ia_interruptor', p_que, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  if p_que is null or p_que not in ('vision', 'texto') or p_activo is null then perform auditoria.anotar('ia_interruptor', coalesce(p_que, '?'), f, 0, 'invalido'); return '{"error":"filtro_invalido"}'; end if;
  insert into public.ia_config (id) values (1) on conflict (id) do nothing;
  if p_que = 'vision' then update public.ia_config set vision = p_activo, actualizado = now(), por = 'admin_' || auditoria.seudonimo(auth.uid()) where id = 1;
  else update public.ia_config set texto = p_activo, actualizado = now(), por = 'admin_' || auditoria.seudonimo(auth.uid()) where id = 1; end if;
  perform auditoria.anotar('ia_interruptor', p_que, f, 1, 'ok');
  return jsonb_build_object('ok', true, 'que', p_que, 'activo', p_activo);
end $$;

revoke all on function public.admin_ia_estado() from public, anon;
revoke all on function public.admin_ia_interruptor(text, boolean) from public, anon;
grant execute on function public.admin_ia_estado() to authenticated;
grant execute on function public.admin_ia_interruptor(text, boolean) to authenticated;
