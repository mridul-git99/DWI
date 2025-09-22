-- liquibase formatted sql

-- changeset nishant:202406261700-seed-schema
-- comment: changed id for email template of Parameter Exception Requested
UPDATE email_templates
SET id = 32
WHERE name = 'PARAMETER_EXCEPTION_REQUESTED';

ALTER TABLE email_templates
ADD CONSTRAINT dd909f76a4d70a4d494e534a47de UNIQUE (id);
