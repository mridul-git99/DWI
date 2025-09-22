-- liquibase formatted sql

-- changeset siba:202411271503-seed-schema
-- comment: add column message id in email audit

ALTER TABLE email_audits
  ADD COLUMN message_id text;
