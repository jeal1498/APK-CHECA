-- Checa: tablas para la licencia ligada a la cuenta de Google.
-- Pegar completo en Supabase → SQL Editor → Run. Se puede correr más de una vez.

-- Licencias (las escribe solo el generador de GitHub con la llave service_role).
create table if not exists public.licencias (
  correo      text primary key,
  nombre      text,
  vence       integer not null,          -- AAAAMMDD; 99991231 = sin vencimiento
  fundador    boolean not null default false,
  firma       text not null,             -- ECDSA de 'LV|correo|vence[|F]'
  max_equipos integer not null default 2,
  creado      timestamptz not null default now()
);
alter table public.licencias enable row level security;
drop policy if exists "ver mi licencia" on public.licencias;
create policy "ver mi licencia" on public.licencias
  for select to authenticated
  using (correo = lower(auth.jwt() ->> 'email'));

-- Equipos donde se usa cada cuenta (sin límite).
create table if not exists public.equipos (
  correo     text not null,
  equipo     text not null,              -- código del equipo, p. ej. 05E7-8598
  nombre     text,
  ultimo_uso timestamptz not null default now(),
  primary key (correo, equipo)
);
alter table public.equipos enable row level security;
drop policy if exists "ver mis equipos" on public.equipos;
drop policy if exists "agregar mis equipos" on public.equipos;
drop policy if exists "actualizar mis equipos" on public.equipos;
drop policy if exists "quitar mis equipos" on public.equipos;
create policy "ver mis equipos" on public.equipos
  for select to authenticated using (correo = lower(auth.jwt() ->> 'email'));
create policy "agregar mis equipos" on public.equipos
  for insert to authenticated with check (correo = lower(auth.jwt() ->> 'email'));
create policy "actualizar mis equipos" on public.equipos
  for update to authenticated
  using (correo = lower(auth.jwt() ->> 'email'))
  with check (correo = lower(auth.jwt() ->> 'email'));
create policy "quitar mis equipos" on public.equipos
  for delete to authenticated using (correo = lower(auth.jwt() ->> 'email'));

-- Sin límite de equipos (6 nov 2026): se quita el control que antes frenaba al pasar de `max_equipos`.
-- `max_equipos` se queda en `licencias` solo porque marca el plan Básico (firma `|B`).
drop trigger if exists limite_equipos on public.equipos;
drop function if exists public.limite_equipos();

-- Pagos de Mercado Pago (los escribe solo la función mp-webhook). Evita activar dos veces el mismo pago.
create table if not exists public.pagos (
  id      text primary key,               -- id del pago en Mercado Pago
  correo  text not null,
  plan    text not null,                  -- 1m, 6m, 12m
  monto   numeric not null,
  vence   integer,                        -- AAAAMMDD que quedó en la licencia
  creado  timestamptz not null default now()
);
alter table public.pagos enable row level security;

-- Medición de la web (PWA-Checa-Multimedia). La página solo puede insertar; nadie de fuera puede leer.
-- Sin cookies ni datos personales: «visita» es un número al azar por pestaña.
create table if not exists public.web_eventos (
  id          uuid primary key default gen_random_uuid(),
  creado      timestamptz not null default now(),
  evento      text not null check (evento in ('visita','clic_descargar','descarga','llego_instalar','descarga_instalar','whatsapp_ayuda','beta','beta_whatsapp','video_play')),
  pagina      text check (char_length(pagina) <= 60),
  desde       text check (char_length(desde) <= 30),
  visita      text check (char_length(visita) <= 20),
  fuente      text check (char_length(fuente) <= 60),
  campana     text check (char_length(campana) <= 60),
  dispositivo text check (dispositivo in ('android','iphone','pc','otro'))
);
alter table public.web_eventos enable row level security;
drop policy if exists "la web anota eventos" on public.web_eventos;
create policy "la web anota eventos" on public.web_eventos
  for insert to anon with check (creado > now() - interval '1 minute');
grant insert on public.web_eventos to anon;
create index if not exists web_eventos_creado on public.web_eventos (creado);

-- Registro de testers (PWA-Checa-Multimedia/registro/). La página solo puede insertar; nadie de fuera puede leer.
-- Eduardo exporta los correos desde el panel de Supabase (CSV) y los sube a Google Play Console
-- (Pruebas → Prueba cerrada → Testers). Un correo no se repite: si ya existe, la página lo toma como registrado.
create table if not exists public.testers (
  id          uuid primary key default gen_random_uuid(),
  creado      timestamptz not null default now(),
  nombre      text not null check (char_length(btrim(nombre)) between 1 and 80),
  correo      text not null check (char_length(correo) <= 120 and correo ~* '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$'),
  fuente      text check (char_length(fuente) <= 60),
  campana     text check (char_length(campana) <= 60),
  dispositivo text check (dispositivo in ('android','iphone','pc','otro')),
  estado      text not null default 'pendiente' check (estado in ('pendiente','agregado'))
);
alter table public.testers enable row level security;
drop policy if exists "la web registra testers" on public.testers;
create policy "la web registra testers" on public.testers
  for insert to anon with check (estado = 'pendiente' and creado > now() - interval '1 minute');
grant insert on public.testers to anon;
create unique index if not exists testers_correo_unico on public.testers (lower(correo));

-- ===================== Invita a un amigo =====================
-- Cada cuenta tiene un código (p. ej. LALO-7K2). El amigo lo usa antes de su primer pago;
-- cuando paga cualquier paquete, mp-webhook da 30 días más a los dos (una sola vez por amigo).
create table if not exists public.codigos (
  correo text primary key,
  codigo text not null unique
);
alter table public.codigos enable row level security;

create table if not exists public.invitaciones (
  invitado  text primary key,               -- correo de quien usó el código
  codigo    text not null,
  invitador text not null,                  -- correo del dueño del código
  creado    timestamptz not null default now(),
  premiado  timestamptz                     -- cuándo se dio el mes a los dos
);
alter table public.invitaciones enable row level security;

-- Meses ganados por invitaciones (la app avisa «¡Ganaste 1 mes!» cuando sube).
alter table public.licencias add column if not exists premios integer not null default 0;

-- Código de la cuenta que llama (lo crea la primera vez).
create or replace function public.mi_codigo() returns text
language plpgsql security definer set search_path = public as $$
declare
  yo text := lower(auth.jwt() ->> 'email');
  c text;
  base text;
  letras text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
begin
  if yo is null then raise exception 'sin_sesion'; end if;
  select codigo into c from codigos where correo = yo;
  if c is not null then return c; end if;
  base := rpad(left(upper(regexp_replace(translate(split_part(yo, '@', 1), 'áéíóúñ', 'aeioun'), '[^a-zA-Z]', '', 'g')), 4), 4, 'X');
  loop
    c := base || '-' || substr(letras, 1 + floor(random() * 32)::int, 1)
                     || substr(letras, 1 + floor(random() * 32)::int, 1)
                     || substr(letras, 1 + floor(random() * 32)::int, 1);
    begin
      insert into codigos (correo, codigo) values (yo, c);
      return c;
    exception when unique_violation then
      select codigo into c from codigos where correo = yo;
      if c is not null then return c; end if;
    end;
  end loop;
end $$;

-- Usa el código de un amigo. Devuelve: ok, no_existe, propio, ya_tienes, ya_pagaste.
create or replace function public.usar_codigo(c text) returns text
language plpgsql security definer set search_path = public as $$
declare
  yo text := lower(auth.jwt() ->> 'email');
  limpio text := upper(regexp_replace(coalesce(c, ''), '[^a-zA-Z0-9]', '', 'g'));
  dueno text;
  cod text;
begin
  if yo is null then raise exception 'sin_sesion'; end if;
  if exists (select 1 from invitaciones where invitado = yo) then return 'ya_tienes'; end if;
  select correo, codigo into dueno, cod from codigos where replace(codigo, '-', '') = limpio;
  if dueno is null then return 'no_existe'; end if;
  if dueno = yo then return 'propio'; end if;
  if exists (select 1 from pagos where correo = yo) then return 'ya_pagaste'; end if;
  insert into invitaciones (invitado, codigo, invitador) values (yo, cod, dueno);
  return 'ok';
end $$;

-- Código con el que entró la cuenta que llama (o null).
create or replace function public.mi_invitacion() returns text
language sql security definer set search_path = public as $$
  select codigo from invitaciones where invitado = lower(auth.jwt() ->> 'email');
$$;

revoke all on function public.mi_codigo() from public, anon;
revoke all on function public.usar_codigo(text) from public, anon;
revoke all on function public.mi_invitacion() from public, anon;
grant execute on function public.mi_codigo() to authenticated;
grant execute on function public.usar_codigo(text) to authenticated;
grant execute on function public.mi_invitacion() to authenticated;

-- Precio de fundador (40 % menos) para quien usó la beta: se marca en cada pago con ese precio
-- (mp-pago decide quién califica; ver supabase/functions/_shared/planes.ts).
alter table public.pagos add column if not exists precio_beta boolean not null default false;


-- Ajustes de cada cuenta (filtros, gasolina, pop-up, avisos): viajan con el inicio de sesión.
create table if not exists public.ajustes (
  correo      text primary key,
  datos       jsonb not null,
  actualizado timestamptz not null default now()
);
alter table public.ajustes enable row level security;
drop policy if exists "ver mis ajustes" on public.ajustes;
drop policy if exists "agregar mis ajustes" on public.ajustes;
drop policy if exists "actualizar mis ajustes" on public.ajustes;
create policy "ver mis ajustes" on public.ajustes
  for select to authenticated using (correo = lower(auth.jwt() ->> 'email'));
create policy "agregar mis ajustes" on public.ajustes
  for insert to authenticated with check (correo = lower(auth.jwt() ->> 'email'));
create policy "actualizar mis ajustes" on public.ajustes
  for update to authenticated
  using (correo = lower(auth.jwt() ->> 'email'))
  with check (correo = lower(auth.jwt() ->> 'email'));

-- Nombre propio de cada equipo («Mi tablet»); el modelo sigue en «nombre».
alter table public.equipos add column if not exists alias text;

-- ===================== Plan Fundador: los primeros 50 testers =====================
-- El precio de fundador (40 % menos de por vida) es solo para los primeros 50 correos de la tabla `testers`
-- (registro de PWA-Checa-Multimedia), por orden de registro (`creado`), siempre que entren con una cuenta de Google.
-- mp-pago consulta con reclamar_fundador() (siempre con una sesión de Google). Nada se guarda ni se borra:
-- el lugar es la posición del correo en `testers`. Si una cuenta se elimina (eliminar-cuenta borra su fila
-- de `testers`), su lugar pasa al siguiente. La tabla `fundadores` quedó de la versión anterior y ya no se usa.
create table if not exists public.fundadores (
  correo text primary key,
  creado timestamptz not null default now()
);
alter table public.fundadores enable row level security;

-- true si el correo está entre los primeros p_cupos testers registrados. p_crear no hace nada: se conserva
-- solo para que mp-pago siga llamando igual.
create or replace function public.reclamar_fundador(p_correo text, p_cupos integer, p_crear boolean default true)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (
    select 1
    from (select lower(correo) as c from testers order by creado, id limit greatest(p_cupos, 0)) primeros
    where primeros.c = lower(p_correo)
  );
$$;
revoke all on function public.reclamar_fundador(text, integer, boolean) from public, anon, authenticated;
grant execute on function public.reclamar_fundador(text, integer, boolean) to service_role;

-- ===================== Registro de testers: tope de 50 y fecha límite =====================
-- La página de registro (PWA-Checa-Multimedia/registro/) muestra «Quedan N de 50 lugares» y cierra al llegar a 50 testers
-- o el 15 de noviembre de 2026 (23:59 Cancún = 16 nov 05:00 UTC). El cierre también se aplica en la base de datos.
-- Para cambiar la fecha o el tope, editar estas dos funciones y volver a desplegar.
create or replace function public.lugares_fundador()
returns integer language sql stable security definer set search_path = public as $$
  select greatest(0, 50 - count(*))::integer from testers;
$$;
create or replace function public.registro_abierto()
returns boolean language sql stable security definer set search_path = public as $$
  select now() < timestamptz '2026-11-16 05:00:00+00' and (select count(*) from testers) < 50;
$$;
revoke all on function public.lugares_fundador() from public;
revoke all on function public.registro_abierto() from public;
grant execute on function public.lugares_fundador() to anon, authenticated;
grant execute on function public.registro_abierto() to anon, authenticated;

drop policy if exists "la web registra testers" on public.testers;
create policy "la web registra testers" on public.testers
  for insert to anon
  with check (estado = 'pendiente' and creado > now() - interval '1 minute' and public.registro_abierto());

-- ===================== Testers: número de WhatsApp =====================
-- La página de registro ahora pide el WhatsApp (solo dígitos, 10 a 15). Filas anteriores quedan en null.
alter table public.testers add column if not exists whatsapp text
  check (whatsapp is null or whatsapp ~ '^[0-9]{10,15}$');

-- El registro ya no pide correo (solo nombre y WhatsApp); el correo pasa a ser opcional.
-- El WhatsApp no se repite: un registro repetido se toma como ya registrado.
alter table public.testers alter column correo drop not null;
create unique index if not exists testers_whatsapp_unico on public.testers (whatsapp);

-- ===================== Puente Checa ↔ Clientela (4 nov 2026) =====================
-- Proyecto de Supabase compartido: gqsadclvftkzwrihfipz. Misma cuenta de Google en las dos apps.
-- Pegar completo en Supabase → SQL Editor → Run (se puede correr más de una vez), o agregarlo al final
-- de supabase/esquema.sql del repo jeal1498/APK-CHECA para que lo despliegue supabase.yml.
--
-- Reglas:
--  · Cada fila lleva user_id = auth.uid(); RLS: cada quien solo lee y escribe lo suyo.
--  · user_id → auth.users on delete cascade: «Eliminar cuenta» de Checa (eliminar-cuenta borra el usuario
--    de Auth al final) se lleva todo esto sin tocar la función.
--  · La tarjeta pública se lee solo con tarjeta_publica(slug): devuelve lo que el conductor eligió mostrar.
--  · Clientes y viajes guardan el objeto completo de la app en `datos` (jsonb) más unas columnas para
--    filtrar; `borrado` permite sincronizar lo que se eliminó en otro teléfono.

-- Llena user_id con la sesión y marca la hora de cambio (el cliente no puede poner otro dueño).
create or replace function public.dueno_y_hora() returns trigger
language plpgsql set search_path = public as $$
begin
  new.user_id := auth.uid();
  new.actualizado := now();
  return new;
end $$;

-- ---------- Perfil del conductor (lo comparten Checa y Clientela) ----------
create table if not exists public.perfil_conductor (
  user_id         uuid primary key default auth.uid() references auth.users (id) on delete cascade,
  nombre          text check (char_length(nombre) <= 80),
  telefono        text check (telefono is null or telefono ~ '^[0-9]{10,15}$'),   -- con lada, solo dígitos
  ciudad          text check (char_length(ciudad) <= 80),
  -- Moneda y formato de montos
  moneda          text not null default 'MXN' check (moneda ~ '^[A-Z]{3}$'),
  simbolo         text not null default '$' check (char_length(simbolo) between 1 and 4),
  simbolo_despues boolean not null default false,          -- «100 €» en vez de «$100»
  sep_miles       text not null default ',' check (sep_miles in (',', '.', ' ', '''', '')),
  sep_decimal     text not null default '.' check (sep_decimal in ('.', ',')),
  decimales       smallint not null default 0 check (decimales between 0 and 2),
  -- Tarifas de Clientela: {perKm, perHour, extra:{base, minimum, airport, night, wait}}
  tarifas         jsonb not null default '{}'::jsonb,
  -- Costo de gasolina por km que escribe Checa (precio por litro / km por litro)
  gasolina_km     numeric check (gasolina_km is null or gasolina_km between 0 and 1000),
  -- Enlace público de la tarjeta: …/tarjeta.html?c=<slug>
  slug            text unique check (slug is null or slug ~ '^[a-z0-9][a-z0-9-]{2,39}$'),
  -- Lo que el conductor eligió mostrar en su tarjeta (mismo formato que el enlace #j…: n, w, c, h, a, r, p, b)
  tarjeta         jsonb,
  -- Resto de ajustes de Clientela (lugares guardados, mensajes…), privados
  ajustes         jsonb not null default '{}'::jsonb,
  creado          timestamptz not null default now(),
  actualizado     timestamptz not null default now()
);
alter table public.perfil_conductor enable row level security;
drop policy if exists "mi perfil" on public.perfil_conductor;
create policy "mi perfil" on public.perfil_conductor
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.perfil_conductor;
create trigger dueno_y_hora before insert or update on public.perfil_conductor
  for each row execute function public.dueno_y_hora();

-- Tarjeta pública por slug: solo los campos de la tarjeta y el formato de moneda. Nada más sale de aquí.
create or replace function public.tarjeta_publica(p_slug text) returns jsonb
language sql stable security definer set search_path = public as $$
  select coalesce(tarjeta, '{}'::jsonb) || jsonb_build_object(
           'moneda', moneda, 'simbolo', simbolo, 'simbolo_despues', simbolo_despues,
           'sep_miles', sep_miles, 'sep_decimal', sep_decimal, 'decimales', decimales)
  from perfil_conductor
  where slug = lower(p_slug) and tarjeta is not null;
$$;
revoke all on function public.tarjeta_publica(text) from public;
grant execute on function public.tarjeta_publica(text) to anon, authenticated;

-- ---------- Clientes de Clientela ----------
create table if not exists public.clientes (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id          text not null check (char_length(id) <= 40),      -- el id que ya usa la app
  nombre      text check (char_length(nombre) <= 120),
  telefono    text check (char_length(telefono) <= 20),
  datos       jsonb not null,                                   -- el cliente completo de la app
  borrado     boolean not null default false,
  actualizado timestamptz not null default now(),
  primary key (user_id, id)
);
alter table public.clientes enable row level security;
drop policy if exists "mis clientes" on public.clientes;
create policy "mis clientes" on public.clientes
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.clientes;
create trigger dueno_y_hora before insert or update on public.clientes
  for each row execute function public.dueno_y_hora();
create index if not exists clientes_cambios on public.clientes (user_id, actualizado);

-- ---------- Viajes privados (Clientela) ----------
create table if not exists public.viajes_privados (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id          text not null check (char_length(id) <= 40),
  cliente_id  text check (char_length(cliente_id) <= 40),
  fecha       date,
  hora        text check (char_length(hora) <= 8),
  precio      numeric,
  estado      text check (estado in ('agendado', 'hecho', 'cancelado')),
  pago        text check (pago in ('', 'efectivo', 'transferencia', 'tarjeta')),
  datos       jsonb not null,                                   -- el viaje completo de la app
  borrado     boolean not null default false,
  actualizado timestamptz not null default now(),
  primary key (user_id, id)
);
alter table public.viajes_privados enable row level security;
drop policy if exists "mis viajes privados" on public.viajes_privados;
create policy "mis viajes privados" on public.viajes_privados
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.viajes_privados;
create trigger dueno_y_hora before insert or update on public.viajes_privados
  for each row execute function public.dueno_y_hora();
create index if not exists viajes_privados_cambios on public.viajes_privados (user_id, actualizado);
create index if not exists viajes_privados_fecha on public.viajes_privados (user_id, fecha);

-- ---------- Contactos de emergencia (SOS de Checa, a futuro) ----------
create table if not exists public.contactos_sos (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id          uuid not null default gen_random_uuid(),
  nombre      text not null check (char_length(btrim(nombre)) between 1 and 80),
  telefono    text not null check (telefono ~ '^[0-9]{10,15}$'),
  relacion    text check (char_length(relacion) <= 40),
  actualizado timestamptz not null default now(),
  primary key (user_id, id)
);
alter table public.contactos_sos enable row level security;
drop policy if exists "mis contactos sos" on public.contactos_sos;
create policy "mis contactos sos" on public.contactos_sos
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.contactos_sos;
create trigger dueno_y_hora before insert or update on public.contactos_sos
  for each row execute function public.dueno_y_hora();

-- ---------- Viajes de Uber/DiDi que acepta en Checa (para «Mi día» unificado) ----------
create table if not exists public.viajes_checa (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  id          uuid not null default gen_random_uuid(),
  app         text check (app in ('uber', 'didi', 'otra')),
  cuando      timestamptz not null default now(),
  tarifa      numeric,
  km          numeric,
  minutos     numeric,
  gasolina    numeric,
  neto        numeric,
  datos       jsonb not null default '{}'::jsonb,
  actualizado timestamptz not null default now(),
  primary key (user_id, id)
);
alter table public.viajes_checa enable row level security;
drop policy if exists "mis viajes checa" on public.viajes_checa;
create policy "mis viajes checa" on public.viajes_checa
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.viajes_checa;
create trigger dueno_y_hora before insert or update on public.viajes_checa
  for each row execute function public.dueno_y_hora();
create index if not exists viajes_checa_cuando on public.viajes_checa (user_id, cuando);

-- Nadie anónimo toca las tablas directo (la tarjeta va por la función).
revoke all on public.perfil_conductor, public.clientes, public.viajes_privados, public.contactos_sos, public.viajes_checa from anon;
grant select, insert, update, delete on public.perfil_conductor, public.clientes, public.viajes_privados, public.contactos_sos, public.viajes_checa to authenticated;

-- ---------- «Mejores horas» en la cuenta (10.39): resumen por día y hora, no cada oferta ----------
-- dia 0 = lunes. n = ofertas de los últimos 30 días; tarifa y km son totales de esas ofertas.
create table if not exists public.horas_checa (
  user_id     uuid not null default auth.uid() references auth.users (id) on delete cascade,
  dia         smallint not null check (dia between 0 and 6),
  hora        smallint not null check (hora between 0 and 23),
  n           integer not null check (n >= 0),
  tarifa      numeric not null default 0,
  km          numeric not null default 0,
  actualizado timestamptz not null default now(),
  primary key (user_id, dia, hora)
);
alter table public.horas_checa enable row level security;
drop policy if exists "mis horas checa" on public.horas_checa;
create policy "mis horas checa" on public.horas_checa
  for all to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop trigger if exists dueno_y_hora on public.horas_checa;
create trigger dueno_y_hora before insert or update on public.horas_checa
  for each row execute function public.dueno_y_hora();
revoke all on public.horas_checa from anon;
grant select, insert, update, delete on public.horas_checa to authenticated;

-- ---------- Revisión de lecturas (10.48): la función confirmar-oferta guarda las diferencias ----------
-- Solo la función (service role) escribe y lee estas tablas; la app no tiene acceso directo.
create table if not exists public.errores_lector (
  id        bigserial primary key,
  usuario   uuid references auth.users (id) on delete cascade,
  app       text,
  moneda    text,
  version   text,
  lineas    jsonb not null,
  local     jsonb not null,
  ia        jsonb not null,
  resuelto  boolean not null default false,
  creado    timestamptz not null default now()
);
create index if not exists errores_lector_pendientes on public.errores_lector (creado) where not resuelto;
alter table public.errores_lector enable row level security;
revoke all on public.errores_lector from anon, authenticated;

create table if not exists public.uso_confirmar (
  usuario uuid not null references auth.users (id) on delete cascade,
  dia     date not null default ((now() at time zone 'America/Mexico_City')::date),
  n       integer not null default 0,
  primary key (usuario, dia)
);
alter table public.uso_confirmar enable row level security;
revoke all on public.uso_confirmar from anon, authenticated;

create table if not exists public.fallas_ia (
  dia    date not null default ((now() at time zone 'America/Mexico_City')::date),
  tipo   text not null,
  n      integer not null default 0,
  ultima timestamptz not null default now(),
  primary key (dia, tipo)
);
alter table public.fallas_ia enable row level security;
revoke all on public.fallas_ia from anon, authenticated;

-- Suma una consulta del día y devuelve cuántas lleva.
create or replace function public.sumar_uso_confirmar(u uuid) returns integer
language sql security definer set search_path = public as $$
  insert into public.uso_confirmar as t (usuario, dia, n)
  values (u, (now() at time zone 'America/Mexico_City')::date, 1)
  on conflict (usuario, dia) do update set n = t.n + 1
  returning n;
$$;
revoke all on function public.sumar_uso_confirmar(uuid) from public, anon, authenticated;
grant execute on function public.sumar_uso_confirmar(uuid) to service_role;

-- Anota que la revisión no respondió (sin saldo, clave, caída) para avisarlo en la revisión semanal.
create or replace function public.anotar_falla_ia(tipo text) returns integer
language sql security definer set search_path = public as $$
  insert into public.fallas_ia as f (dia, tipo, n)
  values ((now() at time zone 'America/Mexico_City')::date, anotar_falla_ia.tipo, 1)
  on conflict (dia, tipo) do update set n = f.n + 1, ultima = now()
  returning n;
$$;
revoke all on function public.anotar_falla_ia(text) from public, anon, authenticated;
grant execute on function public.anotar_falla_ia(text) to service_role;

-- ---------- Métricas de los lunes (10.49) ----------
-- Una fila por revisión: tiempos (pop-up local y revisión), tokens reales y resultado.
create table if not exists public.revisiones (
  id                   bigserial primary key,
  usuario              uuid references auth.users (id) on delete cascade,
  app                  text,
  version              text,
  modelo               text,
  ms_popup             integer,  -- captura (o evento) → pop-up, en el teléfono
  ms_ia                integer,  -- llamada al modelo, en el servidor
  ms_revision_anterior integer,  -- ida y vuelta de la revisión anterior, vista desde el teléfono
  tokens_entrada       integer not null default 0,
  tokens_salida        integer not null default 0,
  imagenes             integer not null default 0,
  reintentos           integer not null default 0,
  ok                   boolean not null,
  coincide             boolean,
  error                text,
  creado               timestamptz not null default now()
);
create index if not exists revisiones_creado on public.revisiones (creado);
alter table public.revisiones enable row level security;
revoke all on public.revisiones from anon, authenticated;

-- Juicio de la revisión semanal sobre cada diferencia: ¿la corrección fue correcta?
alter table public.errores_lector add column if not exists veredicto text
  check (veredicto in ('ia_bien', 'ia_mal', 'dudoso'));
alter table public.errores_lector add column if not exists nota text;

-- Capturas verificadas a mano (de «Guardar pantallas de ofertas»): base de la precisión real.
create table if not exists public.verificacion_manual (
  id        bigserial primary key,
  semana    date not null,            -- lunes de la semana revisada
  app       text,
  correcta  boolean not null,          -- el pop-up final coincidió con la pantalla
  error     text,                      -- qué falló, si falló
  creado    timestamptz not null default now()
);
alter table public.verificacion_manual enable row level security;
revoke all on public.verificacion_manual from anon, authenticated;
