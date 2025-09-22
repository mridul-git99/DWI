
-- liquibase formatted sql

--changeset siba:202504221314-seed-data-1

UPDATE parameter_values
SET modified_at = NULL, modified_by = NULL
WHERE state = 'NOT_STARTED' AND modified_at is NOT NULL;
