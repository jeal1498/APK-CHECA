-- Revierte 011. ADVIERTE: la función confirmar-oferta vuelve a dejar todo encendido (sin tabla no hay interruptores).
drop function if exists public.admin_ia_interruptor(text, boolean);
drop function if exists public.admin_ia_estado();
drop table if exists public.ia_config;
