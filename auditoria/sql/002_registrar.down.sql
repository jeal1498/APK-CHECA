-- Reversión de 002: quita solo las funciones de escritura; conserva tablas y datos de 001.
drop function if exists public.auditoria_registrar_lote(uuid, jsonb);
drop function if exists auditoria.registrar_lote(uuid, jsonb);
delete from auditoria.version_esquema where version = 2;
