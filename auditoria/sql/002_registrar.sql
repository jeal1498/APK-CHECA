-- E2 · 002 — Escritura idempotente de eventos (PREPARADO, NO APLICADO). Requiere 001.
-- La función registrar-lectura valida el contrato en TypeScript y llama a public.auditoria_registrar_lote
-- con el usuario que verificó en el JWT. Aquí se vuelve a validar (las restricciones de 001) y cada evento
-- se guarda en su propia subtransacción: uno inválido no tumba al resto del lote.

create or replace function auditoria.registrar_lote(u uuid, eventos jsonb) returns jsonb
language plpgsql security definer set search_path = auditoria, pg_temp as $$
declare
  ev jsonb; res jsonb := '[]'::jsonb; huella text; previa auditoria.recepciones; dueno uuid; idv uuid; et jsonb; i integer;
begin
  if u is null then raise exception 'usuario requerido' using errcode = '22004'; end if;
  if jsonb_typeof(eventos) <> 'array' or jsonb_array_length(eventos) = 0 or jsonb_array_length(eventos) > 50 then
    raise exception 'lote inválido: de 1 a 50 eventos' using errcode = '22023';
  end if;
  for ev in select * from jsonb_array_elements(eventos) loop
    begin
      idv := (ev->>'id')::uuid;
      huella := encode(sha256(convert_to(ev::text, 'UTF8')), 'hex');
      select * into previa from auditoria.recepciones where lectura_id = idv;
      if found then
        select usuario into dueno from auditoria.lecturas where id = idv;
        if previa.payload_sha256 = huella and dueno is not distinct from u then
          update auditoria.recepciones set intentos = intentos + 1, ultima = now() where lectura_id = idv;
          res := res || jsonb_build_object('id', idv, 'estado', 'repetido');
        else
          res := res || jsonb_build_object('id', idv, 'estado', 'conflicto');
        end if;
        continue;
      end if;
      insert into auditoria.lecturas (id, usuario, contrato, app, version_app, version_parser, lector, fotograma_id,
        captura_sha256, captura_vinculo, ts_fotograma, ts_evento, resultado, motivo, tarifa, km, min, rec_km, rec_min,
        multi, moneda, nivel, tarjetas, ms_lectura, ms_popup, lineas_sha256, lineas_redactadas, telefono, android, orientacion)
      values (idv, u, (ev->>'contrato')::smallint, ev->>'app', ev->>'version_app', ev->>'version_parser', ev->>'lector',
        (ev->>'fotograma_id')::uuid, ev->>'captura_sha256', coalesce(ev->>'captura_vinculo', 'sin_captura'),
        (ev->>'ts_fotograma')::timestamptz, (ev->>'ts_evento')::timestamptz, ev->>'resultado', ev->>'motivo',
        (ev->>'tarifa')::numeric, (ev->>'km')::numeric, (ev->>'min')::numeric, (ev->>'rec_km')::numeric,
        (ev->>'rec_min')::numeric, (ev->>'multi')::numeric, ev->>'moneda', ev->>'nivel', (ev->>'tarjetas')::smallint,
        (ev->>'ms_lectura')::integer, (ev->>'ms_popup')::integer, ev->>'lineas_sha256', ev->'lineas_redactadas',
        ev->>'telefono', ev->>'android', ev->>'orientacion');
      i := 0;
      for et in select * from jsonb_array_elements(coalesce(ev->'etapas', '[]'::jsonb)) loop
        insert into auditoria.etapas (lectura_id, orden, etapa, estado, ms, motivo)
        values (idv, i, et->>'etapa', et->>'estado', (et->>'ms')::integer, et->>'motivo');
        i := i + 1;
      end loop;
      insert into auditoria.recepciones (lectura_id, payload_sha256) values (idv, huella);
      res := res || jsonb_build_object('id', idv, 'estado', 'guardado');
    exception when others then
      -- Subtransacción revertida: nada parcial queda guardado. El código SQL ayuda a diagnosticar sin datos.
      res := res || jsonb_build_object('id', ev->>'id', 'estado', 'invalido', 'error', sqlstate);
    end;
  end loop;
  return res;
end $$;
revoke all on function auditoria.registrar_lote(uuid, jsonb) from public, anon, authenticated;

-- Puerta en `public` (el único esquema que expone PostgREST): solo service_role puede ejecutarla.
create or replace function public.auditoria_registrar_lote(u uuid, eventos jsonb) returns jsonb
language sql security definer set search_path = auditoria, pg_temp as $$ select auditoria.registrar_lote(u, eventos) $$;
revoke all on function public.auditoria_registrar_lote(uuid, jsonb) from public, anon, authenticated;
grant execute on function public.auditoria_registrar_lote(uuid, jsonb) to service_role;

insert into auditoria.version_esquema (version, descripcion)
values (2, 'registrar_lote idempotente (E2)') on conflict (version) do nothing;
