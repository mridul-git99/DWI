-- liquibase formatted sql

--changeset abhishek:202412191450-seed-schema-1

CREATE INDEX IF NOT EXISTS idxt1209prt8n8rnv5n87vou0ace ON parameter_values USING btree (state);
