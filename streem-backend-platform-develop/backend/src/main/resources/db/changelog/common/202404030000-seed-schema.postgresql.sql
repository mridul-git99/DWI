-- liquibase formatted sql

-- changeset siba:202406101154-seed-schema
-- comment: added column time_zone to facilities table
ALTER TABLE facilities
  ADD COLUMN IF NOT EXISTS time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC'
