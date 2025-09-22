-- liquibase formatted sql

-- changeset kousik:202411270130-seed-schema
-- comment: "alter parameterValue table add new key"

ALTER TABLE parameter_values
  ADD COLUMN has_active_exception BOOLEAN DEFAULT FALSE;
