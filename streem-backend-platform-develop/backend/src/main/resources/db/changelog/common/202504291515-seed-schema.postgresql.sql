-- liquibase formatted sql

-- changeset kousik:202504291515-seed-schema-1
-- comment: "alter emailTemplates table, add archived field"

ALTER TABLE email_templates
  ADD COLUMN archived BOOLEAN DEFAULT FALSE;
