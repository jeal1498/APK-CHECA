-- E6 · 004 — Checa Admin MVP: administradores, bitácora de acciones y consultas (PREPARADO, NO APLICADO).
-- Requiere 001–003. El Admin es una página estática que llama a estas funciones con la sesión de Google del
-- administrador (rol authenticated). Cada función: comprueba el rol, limita filtros y páginas, devuelve solo
-- columnas necesarias (sin usuario, sin líneas, sin imágenes) y deja constancia en la bitácora, también si
-- se niega. Devuelven jsonb {error:…} en vez de lanzar, para que la negativa quede registrada.

create table if not exists auditoria.administradores (
  usuario  uuid primary key references auth.users (id) on delete cascade,
  rol      text not null check (rol in ('lector', 'revisor')),   -- lector: consulta; revisor: además da veredictos
  alta     timestamptz not null default now(),
  nota     text check (length(nota) <= 120)
);
-- Alta de administradores: solo el propietario, a mano en el editor SQL (no hay función para hacerlo).
alter table auditoria.administradores enable row level security;
revoke all on auditoria.administradores from public, anon, authenticated;

create table if not exists auditoria.acciones_admin (
  id      bigserial primary key,
  admin   text not null,              -- seudónimo (8 hex del md5 del usuario), nunca el uuid ni el correo
  accion  text not null check (length(accion) <= 40),
  objeto  text check (length(objeto) <= 80),
  filtros jsonb,
  filas   integer,
  resultado text not null check (resultado in ('ok', 'denegado', 'invalido')),
  creado  timestamptz not null default now()
);
create index if not exists acciones_admin_creado on auditoria.acciones_admin (creado desc);
alter table auditoria.acciones_admin enable row level security;
revoke all on auditoria.acciones_admin from public, anon, authenticated;

create or replace function auditoria.solo_agregar() returns trigger language plpgsql as $$
begin
  raise exception 'la bitácora no se edita ni se borra' using errcode = '42501';
end $$;
drop trigger if exists acciones_admin_solo_agregar on auditoria.acciones_admin;
create trigger acciones_admin_solo_agregar before update or delete on auditoria.acciones_admin
  for each row execute function auditoria.solo_agregar();

create or replace function auditoria.seudonimo(u uuid) returns text language sql immutable as
$$ select left(md5(coalesce(u::text, '')), 8) $$;

-- Rol del que llama (null si no es administrador).
create or replace function auditoria.rol_actual() returns text language sql stable security definer
set search_path = auditoria, pg_temp as $$ select rol from auditoria.administradores where usuario = auth.uid() $$;

-- Las negativas y los filtros inválidos se anotan como mucho una vez por minuto por persona y acción: cualquier
-- cuenta de conductor puede llamar a estas funciones y no debe poder inflar la bitácora. (Auditoría de cierre, A5.)
create or replace function auditoria.anotar(accion text, objeto text, filtros jsonb, filas integer, resultado text) returns void
language sql security definer set search_path = auditoria, pg_temp as $$
  insert into auditoria.acciones_admin (admin, accion, objeto, filtros, filas, resultado)
  select auditoria.seudonimo(auth.uid()), anotar.accion, anotar.objeto, anotar.filtros, anotar.filas, anotar.resultado
  where anotar.resultado = 'ok' or not exists (
    select 1 from auditoria.acciones_admin a
    where a.admin = auditoria.seudonimo(auth.uid()) and a.accion = anotar.accion and a.resultado = anotar.resultado
      and a.creado > now() - interval '1 minute');
$$;

-- Valida filtros comunes. Devuelve null si están bien o el código de error.
create or replace function auditoria.filtros_invalidos(desde timestamptz, hasta timestamptz, pagina integer, por_pagina integer) returns text
language sql immutable as $$
  select case
    when desde is null or hasta is null or desde >= hasta then 'rango'
    when hasta - desde > interval '92 days' then 'rango_mayor_a_92_dias'
    when pagina is null or pagina < 1 or pagina > 1000 then 'pagina'
    when por_pagina is null or por_pagina < 1 or por_pagina > 100 then 'por_pagina'
  end
$$;

-- ---------- Inicio: salud, volumen, ingesta y alertas ----------
create or replace function public.admin_inicio(desde timestamptz, hasta timestamptz) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta); err text; r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('inicio', null, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  err := auditoria.filtros_invalidos(desde, hasta, 1, 1);
  if err is not null then perform auditoria.anotar('inicio', null, f, 0, 'invalido'); return jsonb_build_object('error', err); end if;
  select jsonb_build_object(
    'lecturas', (select count(*) from lecturas where ts_evento >= desde and ts_evento < hasta),
    'por_resultado', coalesce((select jsonb_object_agg(resultado, n) from (select resultado, count(*) n from lecturas
                     where ts_evento >= desde and ts_evento < hasta group by 1) x), '{}'::jsonb),
    'ultima_recepcion', (select max(recibido) from lecturas),
    'reintentos_recibidos', (select coalesce(sum(intentos - 1), 0) from recepciones rc join lecturas l on l.id = rc.lectura_id
                             where l.ts_evento >= desde and l.ts_evento < hasta),
    'conductores', (select count(distinct usuario) from lecturas where ts_evento >= desde and ts_evento < hasta),
    'versiones', coalesce((select jsonb_agg(jsonb_build_object('version', version_app, 'app', app, 'n', n) order by version_app desc, app)
                  from (select version_app, app, count(*) n from lecturas where ts_evento >= desde and ts_evento < hasta group by 1, 2) x), '[]'),
    'errores_lector', (select count(*) from public.errores_lector where creado >= desde and creado < hasta),
    'errores_sin_veredicto_humano', (select count(*) from public.errores_lector e where e.creado >= desde and e.creado < hasta
        and not exists (select 1 from veredicto_vigente v where v.error_lector_id = e.id and v.estado in ('referencia_humana', 'referencia_doble'))),
    'fallas_ia', (select coalesce(sum(n), 0) from public.fallas_ia where dia >= desde::date and dia < hasta::date + 1),
    'alertas', (select coalesce(jsonb_agg(a), '[]'::jsonb) from (
        select 'sin_recepciones_24h' a where (select max(recibido) from lecturas) < now() - interval '24 hours'
        union all select 'conflictos_humanos' where exists (select 1 from veredicto_vigente where estado = 'conflicto_humano')
        union all select 'muchas_abstenciones' where (select count(*) filter (where resultado in ('abstencion', 'varias'))::numeric
             / nullif(count(*), 0) from lecturas where ts_evento >= desde and ts_evento < hasta) > 0.2) y)
  ) into r;
  perform auditoria.anotar('inicio', null, f, 1, 'ok');
  return r;
end $$;

-- ---------- Errores de lectura (errores_lector) ----------
create or replace function public.admin_errores(desde timestamptz, hasta timestamptz, app text default null, version text default null,
  tipo text default null, estado_veredicto text default null, pagina integer default 1, por_pagina integer default 50) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta, 'app', app, 'version', version, 'tipo', tipo,
                                      'estado_veredicto', estado_veredicto, 'pagina', pagina, 'por_pagina', por_pagina);
        err text; total integer; filas jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('errores', null, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  err := auditoria.filtros_invalidos(desde, hasta, pagina, por_pagina);
  if err is null and estado_veredicto is not null and estado_veredicto not in
     ('referencia_humana', 'referencia_doble', 'conflicto_humano', 'solo_ia', 'heredado_no_revisado', 'sin_veredicto') then err := 'estado_veredicto'; end if;
  if err is not null then perform auditoria.anotar('errores', null, f, 0, 'invalido'); return jsonb_build_object('error', err); end if;
  with base as (
    select e.id, e.creado, e.app, e.version, e.tipo, e.lector,
           jsonb_build_object('tarifa', e.local->'tarifa', 'km', e.local->'km', 'min', e.local->'min') as lector_lectura,
           jsonb_build_object('oferta', e.ia->'oferta', 'tarifa', e.ia->'tarifa', 'km', e.ia->'km', 'min', e.ia->'min') as ia_lectura,
           coalesce(v.estado, 'sin_veredicto') as estado_veredicto,
           case when v.estado = 'referencia_doble'
                then jsonb_build_object('clase', v.clase, 'tarifa', v.tarifa, 'km', v.km, 'min', v.min) end as referencia,
           exists (select 1 from storage.objects o where o.bucket_id = 'capturas' and o.name = e.id::text || '.webp') as tiene_captura
    from public.errores_lector e left join veredicto_vigente v on v.error_lector_id = e.id
    where e.creado >= desde and e.creado < hasta
      and (admin_errores.app is null or lower(e.app) = lower(admin_errores.app))
      and (admin_errores.version is null or e.version = admin_errores.version)
      and (admin_errores.tipo is null or e.tipo = admin_errores.tipo)
      and (estado_veredicto is null or coalesce(v.estado, 'sin_veredicto') = estado_veredicto)
  )
  select (select count(*) from base),
         coalesce((select jsonb_agg(to_jsonb(b) order by b.creado desc, b.id desc) from
                  (select * from base order by creado desc, id desc offset (pagina - 1) * por_pagina limit por_pagina) b), '[]')
    into total, filas;
  perform auditoria.anotar('errores', null, f, jsonb_array_length(filas), 'ok');
  return jsonb_build_object('total', total, 'pagina', pagina, 'por_pagina', por_pagina, 'filas', filas);
end $$;

-- ---------- Ofertas (eventos de lectura de E3) ----------
create or replace function public.admin_ofertas(desde timestamptz, hasta timestamptz, app text default null, version text default null,
  resultado text default null, estado_veredicto text default null, pagina integer default 1, por_pagina integer default 50) returns jsonb
language plpgsql security definer set search_path = auditoria, public, pg_temp as $$
declare f jsonb := jsonb_build_object('desde', desde, 'hasta', hasta, 'app', app, 'version', version, 'resultado', resultado,
                                      'estado_veredicto', estado_veredicto, 'pagina', pagina, 'por_pagina', por_pagina);
        err text; total integer; filas jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('ofertas', null, f, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  err := auditoria.filtros_invalidos(desde, hasta, pagina, por_pagina);
  if err is null and resultado is not null and resultado not in ('oferta', 'no_oferta', 'duplicada', 'abstencion', 'varias', 'descartada', 'timeout', 'error') then err := 'resultado'; end if;
  if err is not null then perform auditoria.anotar('ofertas', null, f, 0, 'invalido'); return jsonb_build_object('error', err); end if;
  with base as (
    select l.id, l.ts_evento, l.app, l.version_app, l.version_parser, l.lector, l.resultado, l.motivo, l.tarifa, l.km, l.min,
           l.nivel, l.captura_vinculo, l.ms_popup, auditoria.seudonimo(l.usuario) as conductor,
           coalesce(v.estado, 'sin_veredicto') as estado_veredicto,
           case when v.estado = 'referencia_doble'
                then jsonb_build_object('clase', v.clase, 'tarifa', v.tarifa, 'km', v.km, 'min', v.min) end as referencia,
           (select jsonb_agg(jsonb_build_object('etapa', e.etapa, 'estado', e.estado, 'ms', e.ms, 'motivo', e.motivo) order by e.orden)
              from etapas e where e.lectura_id = l.id) as etapas
    from lecturas l left join veredicto_vigente v on v.lectura_id = l.id
    where l.ts_evento >= desde and l.ts_evento < hasta
      and (admin_ofertas.app is null or l.app = admin_ofertas.app)
      and (admin_ofertas.version is null or l.version_app = admin_ofertas.version)
      and (admin_ofertas.resultado is null or l.resultado = admin_ofertas.resultado)
      and (estado_veredicto is null or coalesce(v.estado, 'sin_veredicto') = estado_veredicto)
  )
  select (select count(*) from base),
         coalesce((select jsonb_agg(to_jsonb(b) order by b.ts_evento desc, b.id) from
                  (select * from base order by ts_evento desc, id offset (pagina - 1) * por_pagina limit por_pagina) b), '[]')
    into total, filas;
  perform auditoria.anotar('ofertas', null, f, jsonb_array_length(filas), 'ok');
  return jsonb_build_object('total', total, 'pagina', pagina, 'por_pagina', por_pagina, 'filas', filas);
end $$;

-- ---------- Veredicto humano desde el Admin (solo rol revisor; con la huella de la captura vista) ----------
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

-- ---------- Bitácora (solo lectura, para el propio Admin) ----------
create or replace function public.admin_bitacora(pagina integer default 1) returns jsonb
language plpgsql security definer set search_path = auditoria, pg_temp as $$
declare r jsonb;
begin
  if auditoria.rol_actual() is null then perform auditoria.anotar('bitacora', null, null, 0, 'denegado'); return '{"error":"no_autorizado"}'; end if;
  if pagina is null or pagina < 1 or pagina > 1000 then return '{"error":"pagina"}'; end if;
  select coalesce(jsonb_agg(to_jsonb(a) order by a.id desc), '[]') into r
    from (select * from acciones_admin order by id desc offset (pagina - 1) * 50 limit 50) a;
  perform auditoria.anotar('bitacora', null, jsonb_build_object('pagina', pagina), jsonb_array_length(r), 'ok');
  return jsonb_build_object('filas', r);
end $$;

do $$ declare fn text; begin
  foreach fn in array array['public.admin_inicio(timestamptz,timestamptz)',
    'public.admin_errores(timestamptz,timestamptz,text,text,text,text,integer,integer)',
    'public.admin_ofertas(timestamptz,timestamptz,text,text,text,text,integer,integer)',
    'public.admin_veredicto(text,text,text,numeric,numeric,numeric,smallint,text,text,text)',
    'public.admin_bitacora(integer)'] loop
    execute format('revoke all on function %s from public, anon', fn);
    execute format('grant execute on function %s to authenticated', fn);
  end loop;
end $$;
revoke all on function auditoria.rol_actual() from public, anon, authenticated;
revoke all on function auditoria.anotar(text, text, jsonb, integer, text) from public, anon, authenticated;

insert into auditoria.version_esquema (version, descripcion)
values (4, 'Admin MVP: administradores, bitácora y consultas (E6)') on conflict (version) do nothing;
