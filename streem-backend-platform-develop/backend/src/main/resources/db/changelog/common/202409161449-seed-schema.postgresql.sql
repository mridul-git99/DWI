-- liquibase formatted sql

--changeset kousik :202409161449-seed-schema
--comment: updated datatype of checklist name from VARCHAR(255) to Text

ALTER TABLE checklists
ALTER COLUMN name TYPE TEXT USING name::TEXT;
