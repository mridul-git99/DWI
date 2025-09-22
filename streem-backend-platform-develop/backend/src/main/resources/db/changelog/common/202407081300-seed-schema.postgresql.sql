-- liquibase formatted sql

-- changeset nishant:202407081300-seed-schema
-- comment: Updated Checklist Audits table

ALTER TABLE checklist_audits
ADD COLUMN stages_id BIGINT NULL,
ADD COLUMN tasks_id BIGINT NULL,
ADD COLUMN triggered_for BIGINT NULL;
