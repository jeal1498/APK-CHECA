-- Reversión de 005: quita las métricas y su catálogo. No toca datos.
drop function if exists public.admin_metricas(timestamptz, timestamptz, text, text);
drop function if exists auditoria.tendencia(timestamptz, timestamptz, text);
drop function if exists auditoria.segmentos(timestamptz, timestamptz);
drop function if exists auditoria.metricas(timestamptz, timestamptz, text, text);
drop function if exists auditoria.ventana(timestamptz, timestamptz, text, text);
drop function if exists auditoria.fila_metrica(text, numeric, numeric, numeric, text);
drop table if exists auditoria.metricas_def;
delete from auditoria.version_esquema where version = 5;
