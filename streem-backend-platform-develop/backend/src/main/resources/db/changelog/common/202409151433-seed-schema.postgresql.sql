-- liquibase formatted sql

--changeset siba:2024091433-seed-schema-1
--comment: add time zone info in facility table

ALTER TABLE exceptions
  ALTER COLUMN task_executions_id DROP NOT NULL;

