-- liquibase formatted sql

--changeset peesa:202402291519-seed-schema-1
--comment: added column checklistAncestorId to Job table

ALTER TABLE jobs ADD COLUMN checklist_ancestor_id BIGINT;

--changeset peesa:202402291519-seed-data-1
--comment: sql script that gets ancestorId from versions table by joining with checklists table and updates the checklistAncestorId column in jobs table

UPDATE jobs
SET checklist_ancestor_id = v.ancestor
FROM checklists c
JOIN versions v ON c.versions_id = v.id
WHERE jobs.checklists_id = c.id;
