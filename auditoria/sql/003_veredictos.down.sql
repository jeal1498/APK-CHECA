-- Reversión de 003: quita veredictos y su vista. errores_lector (la fuente heredada) no se toca.
-- ATENCIÓN: borra también los veredictos humanos registrados. Exportarlos antes si hay datos reales.
drop view if exists auditoria.veredicto_vigente;
drop table if exists auditoria.veredictos;
drop function if exists auditoria.veredicto_inmutable();
delete from auditoria.version_esquema where version = 3;
