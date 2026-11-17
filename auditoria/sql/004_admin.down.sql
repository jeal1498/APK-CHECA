-- Reversión de 004: quita las funciones del Admin, la bitácora y la lista de administradores.
-- ATENCIÓN: borra la bitácora de accesos. Exportarla antes si hay datos reales.
drop function if exists public.admin_inicio(timestamptz, timestamptz);
drop function if exists public.admin_errores(timestamptz, timestamptz, text, text, text, text, integer, integer);
drop function if exists public.admin_ofertas(timestamptz, timestamptz, text, text, text, text, integer, integer);
drop function if exists public.admin_veredicto(text, text, text, numeric, numeric, numeric, smallint, text, text, text);
drop function if exists public.admin_bitacora(integer);
drop function if exists auditoria.filtros_invalidos(timestamptz, timestamptz, integer, integer);
drop function if exists auditoria.anotar(text, text, jsonb, integer, text);
drop function if exists auditoria.rol_actual();
drop table if exists auditoria.acciones_admin;
drop function if exists auditoria.solo_agregar();
drop function if exists auditoria.seudonimo(uuid);
drop table if exists auditoria.administradores;
delete from auditoria.version_esquema where version = 4;
