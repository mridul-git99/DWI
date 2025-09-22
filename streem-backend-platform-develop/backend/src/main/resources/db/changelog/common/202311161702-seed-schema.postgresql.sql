-- liquibase formatted sql

--changeset siba:202311161702-seed-schema-1
--comment alter parameter_values table and add task_executions_id column

ALTER TABLE parameter_values
  ADD COLUMN task_executions_id BIGINT;
ALTER TABLE parameter_values
  ADD CONSTRAINT parameter_values_task_executions_id_fkey FOREIGN KEY (task_executions_id) REFERENCES task_executions (id) ON UPDATE NO ACTION ON DELETE NO ACTION;


ALTER TABLE temp_parameter_values
  ADD COLUMN task_executions_id BIGINT;
ALTER TABLE temp_parameter_values
  ADD CONSTRAINT temp_parameter_values_task_executions_id_fkey FOREIGN KEY (task_executions_id) REFERENCES task_executions (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
