-- liquibase formatted sql

-- changeset peesa:202407260221-seed-schema
-- comment: add exception management for resource type parameters
ALTER TABLE EXCEPTIONS
ADD COLUMN IF NOT EXISTS CHOICES JSONB;


