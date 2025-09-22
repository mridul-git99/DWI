-- liquibase formatted sql

--changeset siba:202404081218-seed-schema-1
--comment: add meta data in parameters table

ALTER TABLE parameters
  ADD COLUMN metadata JSONB DEFAULT '{}'::JSONB;
