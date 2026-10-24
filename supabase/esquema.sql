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

-- Equipos donde se usa cada cuenta (máximo 2).
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

-- No más equipos de los que permite la licencia (2 si no tiene).
create or replace function public.limite_equipos() returns trigger
language plpgsql security definer set search_path = public as $$
declare
  usados integer;
  permitidos integer;
begin
  select count(*) into usados from equipos where correo = new.correo and equipo <> new.equipo;
  select coalesce(max(max_equipos), 2) into permitidos from licencias where correo = new.correo;
  if usados >= permitidos then
    raise exception 'limite_equipos';
  end if;
  return new;
end $$;
drop trigger if exists limite_equipos on public.equipos;
create trigger limite_equipos before insert on public.equipos
  for each row execute function public.limite_equipos();

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

-- Medición de la web (checa-web). La página solo puede insertar; nadie de fuera puede leer.
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

-- Registro de testers (checa-web/registro/). La página solo puede insertar; nadie de fuera puede leer.
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

-- ===================== Cupos del Plan Fundador =====================
-- El precio de fundador (40 % menos de por vida) es solo para los testers: quien está en la tabla `testers`
-- (registro de checa-web) Y entra con una cuenta de Google. Son 50 lugares en total, no por paquete.
-- mp-pago reclama el lugar con reclamar_fundador() (siempre con una sesión de Google); la tabla solo la
-- toca el servidor (RLS sin políticas). Para liberar un lugar: delete from public.fundadores where correo = '…';
create table if not exists public.fundadores (
  correo text primary key,
  creado timestamptz not null default now()
);
alter table public.fundadores enable row level security;

-- Devuelve true si la cuenta tiene (o acaba de tomar) un lugar de fundador. p_crear = false solo consulta.
-- El candado evita que dos cuentas simultáneas pasen de p_cupos.
create or replace function public.reclamar_fundador(p_correo text, p_cupos integer, p_crear boolean default true)
returns boolean language plpgsql security definer set search_path = public as $$
declare
  c text := lower(p_correo);
begin
  if c is null or c = '' then return false; end if;
  if exists (select 1 from fundadores where correo = c) then return true; end if;
  if not p_crear then return false; end if;
  -- Solo los testers registrados en la web pueden tomar un lugar.
  if not exists (select 1 from testers where lower(correo) = c) then return false; end if;
  perform pg_advisory_xact_lock(hashtext('cupos_fundador'));
  if exists (select 1 from fundadores where correo = c) then return true; end if;
  if (select count(*) from fundadores) >= p_cupos then return false; end if;
  insert into fundadores (correo) values (c);
  return true;
end $$;
revoke all on function public.reclamar_fundador(text, integer, boolean) from public, anon, authenticated;
grant execute on function public.reclamar_fundador(text, integer, boolean) to service_role;

-- Testers que ya tienen cuenta de Google: ocupan los lugares libres en orden de registro. Primero quien ya
-- pagó con precio de fundador; se omiten las licencias sin vencimiento (propietaria y pruebas). Solo cuenta
-- quien se registró antes de que termine la beta (1 feb 2027, 00:00 Cancún). Se puede correr más de una vez.
-- No borra filas: para liberar un lugar, delete a mano.
do $$
begin
  perform pg_advisory_xact_lock(hashtext('cupos_fundador'));
  insert into public.fundadores (correo)
  select lower(u.email)
  from auth.users u
  where u.email is not null
    and exists (select 1 from public.testers t where lower(t.correo) = lower(u.email))
    and u.created_at < timestamptz '2027-02-01 05:00:00+00'
    and not exists (select 1 from public.licencias l where l.correo = lower(u.email) and l.vence = 99991231)
  order by exists (select 1 from public.pagos p where p.correo = lower(u.email) and p.precio_beta) desc, u.created_at
  limit greatest(0, 50 - (select count(*) from public.fundadores))
  on conflict (correo) do nothing;
end $$;
