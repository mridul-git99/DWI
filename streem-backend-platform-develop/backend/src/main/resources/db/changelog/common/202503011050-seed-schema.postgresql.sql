
-- liquibase formatted sql

--changeset abhishek:202503011050-seed-schema-1



CREATE INDEX IF NOT EXISTS idxt1209prt8n8rnv5n87vou0pv ON public.parameter_values USING gin (choices);
