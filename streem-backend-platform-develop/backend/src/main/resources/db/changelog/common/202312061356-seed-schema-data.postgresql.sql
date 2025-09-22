-- liquibase formatted sql

--changeset Sathyam:202312061356-seed-schema-data-1
ALTER TABLE task_executions
  ADD COLUMN type VARCHAR(50);
UPDATE task_executions
SET type = 'MASTER';
ALTER TABLE task_executions
  ALTER COLUMN type SET NOT NULL;

--changeset Sathyam:202312061356-seed-schema-data-2
ALTER TABLE task_executions
  ADD COLUMN order_tree INTEGER;
UPDATE task_executions
SET order_tree = 1;
ALTER TABLE task_executions
  ALTER COLUMN order_tree SET NOT NULL;

CREATE TABLE task_recurrences
(
  id                  BIGINT  NOT NULL,
  start_date_interval INTEGER NOT NULL,
  start_date_duration JSONB   NOT NULL DEFAULT '{}',
  due_date_interval   INTEGER NOT NULL,
  due_date_duration   JSONB   NOT NULL DEFAULT '{}',
  created_at          BIGINT  NOT NULL,
  modified_at         BIGINT  NOT NULL,
  created_by          BIGINT  NOT NULL,
  modified_by         BIGINT  NOT NULL,
  CONSTRAINT pk_task_recurrences PRIMARY KEY (id)
);

--changeset Sathyam:202312061356-seed-schema-data-4
ALTER TABLE tasks
  ADD COLUMN enable_recurrence BOOLEAN DEFAULT false;
ALTER TABLE tasks
  ADD COLUMN task_recurrences_id BIGINT;
ALTER TABLE tasks
  ADD CONSTRAINT tasks FOREIGN KEY (task_recurrences_id) REFERENCES task_recurrences (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

--changeset Siba:202312061356-seed-schema-data-5

ALTER TABLE task_executions
  ADD COLUMN continue_recurrence BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE task_execution_user_mapping
  DROP CONSTRAINT task_execution_user_mapping_task_executions_id_fkey;

ALTER TABLE task_execution_user_mapping
  ADD CONSTRAINT task_execution_user_mapping_task_executions_id_fkey
    FOREIGN KEY (task_executions_id) REFERENCES task_executions (id)
      ON DELETE CASCADE;


ALTER TABLE task_executions
  ADD COLUMN recurring_premature_start_reason text;

ALTER TABLE task_executions
  ADD COLUMN recurring_overdue_completion_reason text;

ALTER TABLE task_executions
  ADD COLUMN recurring_overdue_start_reason text;

ALTER TABLE task_executions
  ADD COLUMN recurring_expected_started_at BIGINT;

ALTER TABLE task_executions
  ADD COLUMN recurring_expected_due_at BIGINT;

--changeset Siba:202312061356-seed-schema-data-6
ALTER TABLE parameter_values
  DROP CONSTRAINT parameter_values_task_executions_id_fkey;

ALTER TABLE temp_parameter_values
  DROP CONSTRAINT temp_parameter_values_task_executions_id_fkey;

ALTER TABLE parameter_values
  ADD CONSTRAINT parameter_values_task_executions_id_fkey
    FOREIGN KEY (task_executions_id) REFERENCES task_executions (id) ON DELETE CASCADE;

ALTER TABLE temp_parameter_values
  ADD CONSTRAINT temp_parameter_values_task_executions_id_fkey
    FOREIGN KEY (task_executions_id) REFERENCES task_executions (id) ON DELETE CASCADE;
