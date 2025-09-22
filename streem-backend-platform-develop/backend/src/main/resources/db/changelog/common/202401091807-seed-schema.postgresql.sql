-- liquibase formatted sql

--changeset siba:202401091807-seed-schema-1
--comment add task scheduling premature start reason

ALTER TABLE task_executions
  ADD COLUMN IF NOT EXISTS schedule_premature_start_reason text;
