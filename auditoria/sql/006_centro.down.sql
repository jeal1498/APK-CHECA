-- Reversión de 006: quita las funciones del centro de control y el seguimiento de errores.
drop function if exists public.admin_config();
drop function if exists public.admin_costos(timestamptz, timestamptz);
drop function if exists public.admin_ia(timestamptz, timestamptz);
drop function if exists public.admin_dispositivos(timestamptz, timestamptz);
drop function if exists public.admin_versiones(timestamptz, timestamptz);
drop function if exists public.admin_seguimiento(bigint, text, text, text);
drop function if exists public.admin_error(bigint);
drop function if exists public.admin_salud(timestamptz, timestamptz);
drop function if exists auditoria.clasificar_error(text, jsonb, jsonb);
drop function if exists auditoria.guardia(text, jsonb, timestamptz, timestamptz);
drop view if exists auditoria.enlace_capturas;
drop table if exists auditoria.seguimiento_errores;
delete from auditoria.version_esquema where version = 6;
