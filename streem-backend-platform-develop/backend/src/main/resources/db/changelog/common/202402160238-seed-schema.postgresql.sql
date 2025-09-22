-- liquibase formatted sql

--changeset siba:202402160238-seed-schema-1
--comment: added index on task_executions_id in parameter_values table

CREATE INDEX IF NOT EXISTS idx21cc0138b45f4fb283a22ce7 ON parameter_values (task_executions_id);

CREATE INDEX IF NOT EXISTS idx21cc0138b45f4fb283a22ce8 ON temp_parameter_values (task_executions_id);
