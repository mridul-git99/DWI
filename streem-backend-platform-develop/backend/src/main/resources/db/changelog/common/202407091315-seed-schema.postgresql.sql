-- liquibase formatted sql

-- changeset siba:202407091315-seed-schema
-- comment: added job annotation 'code' column

ALTER TABLE job_annotations
  ADD COLUMN code VARCHAR(20);
