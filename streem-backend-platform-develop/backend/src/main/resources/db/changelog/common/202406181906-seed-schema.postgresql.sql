-- liquibase formatted sql

-- changeset nishant:202406181906-seed-schema-1
-- comment: drop the column and set default for the columns


ALTER TABLE IF EXISTS task_recurrences
DROP COLUMN IF EXISTS positive_start_date_tolerance_interval,
DROP COLUMN IF EXISTS positive_start_date_tolerance_duration,
DROP COLUMN IF EXISTS negative_start_date_tolerance_interval,
DROP COLUMN IF EXISTS negative_start_date_tolerance_duration,
DROP COLUMN IF EXISTS positive_due_date_tolerance_interval,
DROP COLUMN IF EXISTS positive_due_date_tolerance_duration,
DROP COLUMN IF EXISTS negative_due_date_tolerance_interval,
DROP COLUMN IF EXISTS negative_due_date_tolerance_duration;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS positive_start_date_tolerance_interval INTEGER DEFAULT 0;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS positive_start_date_tolerance_duration JSONB DEFAULT '{}'::JSONB;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS negative_start_date_tolerance_interval INTEGER DEFAULT 0;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS negative_start_date_tolerance_duration JSONB DEFAULT '{}'::JSONB;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS positive_due_date_tolerance_interval INTEGER DEFAULT 0;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS positive_due_date_tolerance_duration JSONB DEFAULT '{}'::JSONB;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS negative_due_date_tolerance_interval INTEGER DEFAULT 0;

ALTER TABLE task_recurrences
ADD COLUMN IF NOT EXISTS negative_due_date_tolerance_duration JSONB DEFAULT '{}'::JSONB;
