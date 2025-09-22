-- liquibase formatted sql

-- changeset siba:202407162125-seed-schema-1
-- comment: add has bulk verification column to task table

ALTER TABLE tasks
ADD COLUMN has_bulk_verification BOOLEAN DEFAULT FALSE;


-- changeset siba:202407162125-seed-schema-2
-- comment: add is bulk verification column to task table

ALTER TABLE parameter_verifications
ADD COLUMN IF NOT EXISTS is_bulk BOOLEAN DEFAULT FALSE;

ALTER TABLE temp_parameter_verifications
  ADD COLUMN IF NOT EXISTS is_bulk BOOLEAN DEFAULT FALSE;
