-- Reversión de 001 (solo para entornos de prueba o con autorización expresa).
-- Borra ÚNICAMENTE el esquema `auditoria` y lo que contiene; las tablas públicas existentes no se tocan.
-- ATENCIÓN: borra los eventos de auditoría guardados. Respaldar antes si hay datos reales.
drop schema if exists auditoria cascade;
