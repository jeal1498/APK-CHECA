-- Consulta de REFERENCIA de E0.1 (solo lectura). Se pega en el editor SQL de Supabase
-- (proyecto gqsadclvftkzwrihfipz) con tu propia sesión, justo DESPUÉS de que termine respaldar.py.
-- Ejecuta cada consulta por separado y exporta el resultado como CSV:
--   consulta A → ref_objetos.csv        consulta B → ref_tablas.csv
-- No modifica nada. Las huellas se calculan igual que en comun.py (ver verificar.py).

-- ===== Consulta A: objetos del almacén (nombre y tamaño) =====
select name, (metadata->>'size')::bigint as bytes
from storage.objects
where bucket_id = 'capturas'
order by name collate "C";

-- ===== Consulta B: conteos y huellas (una sola fila) =====
select
  (select count(*) from storage.objects where bucket_id = 'capturas')                           as objetos,
  (select count(*) from public.errores_lector)                                                  as errores_lector,
  (select count(*) from public.errores_lector where version = '10.77')                          as errores_lector_10_77,
  (select count(*) from public.capturas_ok)                                                     as capturas_ok,
  (select encode(sha256(convert_to(string_agg(id::text, ',' order by id), 'UTF8')), 'hex')
     from public.errores_lector)                                                                as huella_ids_errores_lector,
  (select encode(sha256(convert_to(string_agg(id::text, ',' order by id), 'UTF8')), 'hex')
     from public.errores_lector where version = '10.77')                                        as huella_ids_errores_lector_10_77,
  (select encode(sha256(convert_to(string_agg(id::text, ',' order by id), 'UTF8')), 'hex')
     from public.capturas_ok)                                                                   as huella_ids_capturas_ok,
  (select encode(sha256(convert_to(
            string_agg(name || E'\t' || (metadata->>'size'), E'\n' order by name collate "C") || E'\n',
            'UTF8')), 'hex')
     from storage.objects where bucket_id = 'capturas')                                         as huella_listado,
  to_char(now() at time zone 'utc', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')                               as consultado_utc;
