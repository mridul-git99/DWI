-- liquibase formatted sql

--changeset siba:202408291024-seed-schema-1
--comment: add time zone info in facility table

ALTER TABLE facilities
  ADD COLUMN IF NOT EXISTS time_zone VARCHAR(30) DEFAULT 'Asia/Kolkata' NOT NULL;

