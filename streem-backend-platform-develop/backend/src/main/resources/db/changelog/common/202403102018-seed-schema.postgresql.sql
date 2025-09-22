-- liquibase formatted sql

--changeset nishant:202403102018-seed-schema
--comment: added column isTaskLocked to tasks table

ALTER TABLE tasks
ADD COLUMN is_solo_task BOOLEAN NOT NULL DEFAULT FALSE;
