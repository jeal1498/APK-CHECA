-- E4 · 003 — Veredictos humanos, de IA y heredados (PREPARADO, NO APLICADO). Requiere 001.
-- Separa tres cosas que antes se mezclaban: lo que leyó el lector (auditoria.lecturas / errores_lector.local),
-- lo que dijo una IA y lo que verificó una persona contra la captura. Un veredicto nunca se edita: se agrega
-- otro que lo sustituye (`sustituye`). Las etiquetas heredadas se importan como «heredado» y NO cuentan como verdad.

create table if not exists auditoria.veredictos (
  id               bigserial primary key,
  -- Exactamente un objetivo: un evento nuevo, un caso histórico de errores_lector o un caso del banco.
  lectura_id       uuid references auditoria.lecturas (id) on delete cascade,
  error_lector_id  bigint,          -- sin FK: errores_lector puede purgarse; se conserva la referencia
  caso_banco       text check (caso_banco ~ '^[0-9]{4}-(uber|didi|otra)$'),
  fuente           text not null check (fuente in ('humano', 'ia', 'heredado')),
  autor            text not null check (length(autor) between 1 and 64),  -- seudónimo del revisor o id del modelo
  revisor_version  text check (length(revisor_version) <= 64),           -- modelo/prompt o versión de la guía
  guia_version     text check (length(guia_version) <= 16),
  clase            text not null check (clase in ('oferta', 'no_oferta', 'varias', 'ilegible', 'cambio_pantalla')),
  tarifa           numeric(10, 2) check (tarifa > 0),
  km               numeric(8, 2) check (km > 0),
  min              numeric(8, 1) check (min > 0),
  tarjeta          smallint check (tarjeta between 1 and 20),   -- qué tarjeta (1 = la de arriba) es la del botón
  confianza        text not null default 'media' check (confianza in ('alta', 'media', 'baja')),
  estado           text not null default 'propuesto' check (estado in ('propuesto', 'validado', 'descartado')),
  evidencia_sha256 text check (evidencia_sha256 ~ '^[0-9a-f]{64}$'),  -- sha256 de la captura que se miró
  nota             text check (length(nota) <= 280),
  origen_migracion text,          -- '003' para los importados de errores_lector.veredicto
  sustituye        bigint references auditoria.veredictos (id),
  creado           timestamptz not null default now(),
  constraint veredicto_un_objetivo check (num_nonnulls(lectura_id, error_lector_id, caso_banco) = 1),
  constraint veredicto_oferta_con_datos check (clase <> 'oferta' or (tarifa is not null and km is not null and min is not null)),
  constraint veredicto_sin_datos check (clase = 'oferta' or (tarifa is null and km is null and min is null)),
  -- Un humano solo VALIDA mirando la captura (queda su huella) y con la guía usada.
  constraint veredicto_humano_validado check (
    not (fuente = 'humano' and estado = 'validado') or (evidencia_sha256 is not null and guia_version is not null)),
  -- La IA y lo heredado nunca quedan «validados»: eso solo lo hace una persona.
  constraint veredicto_solo_humano_valida check (fuente = 'humano' or estado <> 'validado'),
  constraint veredicto_heredado_marcado check (fuente <> 'heredado' or origen_migracion is not null)
);
create index if not exists veredictos_lectura on auditoria.veredictos (lectura_id) where lectura_id is not null;
create index if not exists veredictos_error on auditoria.veredictos (error_lector_id) where error_lector_id is not null;
create index if not exists veredictos_banco on auditoria.veredictos (caso_banco) where caso_banco is not null;
create unique index if not exists veredictos_migracion_unica
  on auditoria.veredictos (error_lector_id, origen_migracion) where origen_migracion is not null;
-- Un mismo revisor no da dos veredictos vigentes al mismo objetivo (para sustituir se usa `sustituye`).
create unique index if not exists veredictos_autor_unico on auditoria.veredictos
  (coalesce(lectura_id::text, error_lector_id::text, caso_banco), autor) where sustituye is null and estado <> 'descartado';
alter table auditoria.veredictos enable row level security;
revoke all on auditoria.veredictos from public, anon, authenticated;

-- Los veredictos no se editan ni se borran a mano. Sí se van en cascada (al borrar la lectura o la cuenta):
-- un borrado directo tiene profundidad de disparador 1; el de una cascada, mayor.
create or replace function auditoria.veredicto_inmutable() returns trigger language plpgsql as $$
begin
  if tg_op = 'UPDATE' then
    raise exception 'los veredictos no se editan: agrega uno nuevo con «sustituye»' using errcode = '42501';
  end if;
  if pg_trigger_depth() = 1 then
    raise exception 'los veredictos no se borran' using errcode = '42501';
  end if;
  return old;
end $$;
drop trigger if exists veredicto_inmutable on auditoria.veredictos;
create trigger veredicto_inmutable before update or delete on auditoria.veredictos
  for each row execute function auditoria.veredicto_inmutable();

-- Veredicto vigente por objetivo, con precedencia explícita:
--   1. humanos validados que coinciden (≥ 1)            → 'referencia_humana' (≥ 2 que coinciden: 'referencia_doble')
--   2. humanos validados que NO coinciden entre sí      → 'conflicto_humano' (no se fuerza una clase)
--   3. sin humano validado, con IA                       → 'solo_ia'
--   4. solo heredado                                     → 'heredado_no_revisado'
-- Los sustituidos y descartados no cuentan.
create or replace view auditoria.veredicto_vigente with (security_invoker = on) as
with v as (
  select x.*, coalesce(x.lectura_id::text, 'e' || x.error_lector_id::text, 'b' || x.caso_banco) as objetivo
  from auditoria.veredictos x
  where x.estado <> 'descartado' and not exists (select 1 from auditoria.veredictos s where s.sustituye = x.id)
), h as (
  select objetivo, count(*) as n_humanos,
         count(distinct (clase, tarifa, km, min, tarjeta)) as distintos,
         min(clase) as clase, min(tarifa) as tarifa, min(km) as km, min(min) as min, min(tarjeta) as tarjeta
  from v where fuente = 'humano' and estado = 'validado' group by objetivo
), o as (
  select objetivo, bool_or(fuente = 'ia') as hay_ia, bool_or(fuente = 'heredado') as hay_heredado,
         min(lectura_id::text)::uuid as lectura_id, min(error_lector_id) as error_lector_id, min(caso_banco) as caso_banco
  from v group by objetivo
)
select o.objetivo, o.lectura_id, o.error_lector_id, o.caso_banco,
       case when h.n_humanos > 0 and h.distintos = 1 then case when h.n_humanos >= 2 then 'referencia_doble' else 'referencia_humana' end
            when h.n_humanos > 0 then 'conflicto_humano'
            when o.hay_ia then 'solo_ia'
            else 'heredado_no_revisado' end as estado,
       coalesce(h.n_humanos, 0) as n_humanos,
       case when h.distintos = 1 then h.clase end as clase,
       case when h.distintos = 1 then h.tarifa end as tarifa,
       case when h.distintos = 1 then h.km end as km,
       case when h.distintos = 1 then h.min end as min,
       case when h.distintos = 1 then h.tarjeta end as tarjeta
from o left join h using (objetivo);
revoke all on auditoria.veredicto_vigente from public, anon, authenticated;

-- Importación de lo heredado (idempotente, reversible). La tabla original NO se toca.
-- errores_lector.veredicto: ia_bien = «la revisión de IA tenía razón», ia_mal = «tenía razón el lector», dudoso.
-- Se importa la clase/datos según ese juicio, con fuente 'heredado' y autor 'revision_semanal': no es verdad humana.
with src as (
  select e.id, e.veredicto,
         case when e.veredicto = 'ia_bien' then e.ia when e.veredicto = 'ia_mal' then e.local end as j
  from public.errores_lector e
  where e.veredicto is not null
), dat as (
  select id, veredicto, j->>'oferta' = 'false' as no_oferta,
         case when (j->>'tarifa') ~ '^[0-9]+(\.[0-9]+)?$' then (j->>'tarifa')::numeric end as tarifa,
         case when (j->>'km') ~ '^[0-9]+(\.[0-9]+)?$' then (j->>'km')::numeric end as km,
         case when (j->>'min') ~ '^[0-9]+(\.[0-9]+)?$' then (j->>'min')::numeric end as min
  from src
), cls as (
  select *, case when veredicto = 'dudoso' or j_nula then 'ilegible'
                 when no_oferta then 'no_oferta'
                 when tarifa > 0 and km > 0 and min > 0 then 'oferta'
                 else 'ilegible' end as clase
  from (select *, veredicto not in ('ia_bien', 'ia_mal', 'dudoso') as j_nula from dat) d
)
insert into auditoria.veredictos (error_lector_id, fuente, autor, revisor_version, clase, tarifa, km, min, confianza, estado, nota, origen_migracion)
select id, 'heredado', 'revision_semanal', 'errores_lector.veredicto', clase,
       case when clase = 'oferta' then tarifa end, case when clase = 'oferta' then km end, case when clase = 'oferta' then min end,
       'baja', 'propuesto', 'importado de errores_lector.veredicto=' || veredicto || ' (no revisado de nuevo)', '003'
from cls
on conflict do nothing;

insert into auditoria.version_esquema (version, descripcion)
values (3, 'veredictos humano/IA/heredado y precedencia (E4)') on conflict (version) do nothing;
