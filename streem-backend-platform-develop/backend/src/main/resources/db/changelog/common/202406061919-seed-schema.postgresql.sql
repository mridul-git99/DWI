-- liquibase formatted sql

-- changeset siba:202406061919-seed-schema-1
-- comment: drop not null constraints on task_recurrences table

ALTER TABLE task_recurrences
ALTER COLUMN start_date_duration DROP NOT NULL ;

ALTER TABLE task_recurrences
ALTER COLUMN start_date_interval DROP NOT NULL ;

ALTER TABLE task_recurrences
ALTER COLUMN due_date_interval DROP NOT NULL ;

ALTER TABLE task_recurrences
ALTER COLUMN due_date_duration DROP NOT NULL ;


-- changeset siba:202406061919-seed-schema-2
-- comment: added task recurrence tolerance columns

ALTER TABLE task_recurrences
ADD COLUMN positive_start_date_tolerance_interval INTEGER ;

ALTER TABLE task_recurrences
ADD COLUMN positive_start_date_tolerance_duration JSONB;

ALTER TABLE task_recurrences
ADD COLUMN negative_start_date_tolerance_interval INTEGER ;

ALTER TABLE task_recurrences
ADD COLUMN negative_start_date_tolerance_duration JSONB;


ALTER TABLE task_recurrences
ADD COLUMN positive_due_date_tolerance_interval INTEGER ;

ALTER TABLE task_recurrences
ADD COLUMN positive_due_date_tolerance_duration JSONB;

ALTER TABLE task_recurrences
ADD COLUMN negative_due_date_tolerance_interval INTEGER ;

ALTER TABLE task_recurrences
ADD COLUMN negative_due_date_tolerance_duration JSONB;


