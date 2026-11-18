-- 007 · Revisión humana de capturas de ofertas normales (el pop-up salió verde, amarillo o rojo).
-- Aditiva e idempotente. No toca tablas existentes. Reversión: 007_revision_ofertas.down.sql.
-- Quién revisa: solo el rol «revisor». Cada consulta y cada revisión queda en auditoria.acciones_admin.
-- Qué se revisa: public.capturas_ok (lo que leyó el teléfono) con su captura en el almacén privado «capturas»
-- (nombre ok-<color>-<tarifa>-<km>-<min>-<ms>-n<id>.webp). Solo desde p_desde (por defecto: 18-nov-2026 03:48 UTC,
-- cuando salió la 10.82), porque capturas_ok.version no es confiable en esas filas.

create table if not exists auditoria.revisiones_ofertas (
  id               bigserial primary key,
  captura_ok_id    bigint not null,
  captura_nombre   text   not null,
  nivel            text   not null check (nivel in ('verde', 'amarillo', 'rojo')),
  resultado        text   not null check (resultado in ('correcta', 'valores_mal', 'falso_positivo', 'anulada')),
  motivo           text   check (motivo in ('no_oferta', 'vencida', 'otra_tarjeta', 'ilegible')),
  tarifa           numeric check (tarifa > 0),
  km               numeric check (km > 0),
  min              numeric check (min > 0),
  nota             text   check (length(nota) <= 280),
  autor            text   not null check (length(autor) between 1 and 64),
  evidencia_sha256 text   check (evidencia_sha256 ~ '^[0-9a-f]{64}$'),
  creado           timestamptz not null default now(),
  constraint revof_motivo  check ((resultado = 'falso_positivo') = (motivo is not null)),
  constraint revof_valores check (
    (resultado = 'valores_mal' and tarifa is not null and km is not null and min is not null)
    or (resultado <> 'valores_mal' and tarifa is null and km is null and min is null))
);
create index if not exists revof_captura on auditoria.revisiones_ofertas (captura_ok_id, id desc);
alter table auditoria.revisiones_ofertas enable row level security;
revoke all on auditoria.revisiones_ofertas from public, anon, authenticated;

-- Última revisión vigente por captura (una «anulada» deja la captura otra vez pendiente).
create or replace view auditoria.revision_ofertas_vigente as
  select distinct on (captura_ok_id) id, captura_ok_id, captura_nombre, nivel, resultado, motivo, tarifa, km, min, nota, autor, creado
  from auditoria.revisiones_ofertas
  order by captura_ok_id, id desc;
revoke all on auditoria.revision_ofertas_vigente from public, anon, authenticated;

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

-- Guarda la revisión de una captura. Solo «revisor». «anulada» deshace la anterior (no se borra nada).
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

revoke all on function public.admin_capturas_pendientes(text, integer, timestamptz) from public, anon;
revoke all on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text) from public, anon;
grant execute on function public.admin_capturas_pendientes(text, integer, timestamptz) to authenticated;
grant execute on function public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text) to authenticated;
