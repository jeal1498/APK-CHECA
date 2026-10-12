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
