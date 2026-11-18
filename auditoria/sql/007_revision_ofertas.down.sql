-- Revierte 007. ADVIERTE: borra las revisiones guardadas en auditoria.revisiones_ofertas.
drop function if exists public.admin_revisar_captura(bigint, text, text, numeric, numeric, numeric, text, text);
drop function if exists public.admin_capturas_pendientes(text, integer, timestamptz);
drop view if exists auditoria.revision_ofertas_vigente;
drop table if exists auditoria.revisiones_ofertas;
