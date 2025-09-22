-- liquibase formatted sql

--changeset nishant:202502121410-seed-schema-1

ALTER TABLE effects ADD COLUMN IF NOT EXISTS javascript_enabled BOOLEAN DEFAULT FALSE NOT NULL;

