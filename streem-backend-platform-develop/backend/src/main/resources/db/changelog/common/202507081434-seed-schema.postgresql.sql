-- liquibase formatted sql

-- changeset siba:202507081434-seed-schema-1
-- comment: "alter correction status column set type to text"

ALTER TABLE corrections
  ALTER COLUMN status TYPE TEXT;
