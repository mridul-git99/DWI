-- liquibase formatted sql

--changeset siba:202312061357-seed-schema-1

CREATE TABLE task_schedules
(
  id                  BIGINT      NOT NULL,
  type                VARCHAR(50) NOT NULL,
  referenced_tasks_id BIGINT,
  condition           VARCHAR(50) NOT NULL,
  start_date_interval INTEGER     NOT NULL,
  start_date_duration JSONB       NOT NULL DEFAULT '{}',
  due_date_interval   INTEGER     NOT NULL,
  due_date_duration   JSONB       NOT NULL DEFAULT '{}',
  created_at          BIGINT      NOT NULL,
  modified_at         BIGINT      NOT NULL,
  created_by          BIGINT      NOT NULL,
  modified_by         BIGINT      NOT NULL,
  CONSTRAINT pk_task_schedules PRIMARY KEY (id)
);

ALTER TABLE task_schedules
  ADD CONSTRAINT fk9a2942d0f644dc8557ee2cb7c FOREIGN KEY (referenced_tasks_id) REFERENCES tasks (id) ON UPDATE NO ACTION ON DELETE NO ACTION;



ALTER TABLE tasks
  ADD COLUMN enable_scheduling BOOLEAN DEFAULT false;

ALTER TABLE tasks
  ADD COLUMN task_schedules_id BIGINT;

ALTER TABLE task_executions
  ADD COLUMN is_scheduled BOOLEAN DEFAULT false;

ALTER TABLE task_executions
  ADD COLUMN scheduling_expected_due_at BIGINT;

ALTER TABLE task_executions
  ADD COLUMN scheduling_expected_started_at BIGINT;

ALTER TABLE task_executions
  ADD COLUMN schedule_overdue_completion_reason TEXT;

--changeset siba:202312061357-seed-data-1
--comment: migration for adding task execution ids in parameter_values table
update parameter_values pv
set task_executions_id = te.id
from task_executions te
       inner join public.tasks t on te.tasks_id = t.id
       inner join parameters p on t.id = p.tasks_id
where p.id = pv.parameters_id
  and te.jobs_id = pv.jobs_id;

--changeset siba:202312061357-seed-data-2
--comment: migration for impacted_by of branching rules

WITH updated_values AS (SELECT pv1.id as pv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   impactedBy::jsonb,
                                   ARRAY ['parameterValueId'],
                                   (pv2.id)::text::jsonb
                                 )
                               )      AS new_impacted_by
                        FROM parameter_values pv1
                               CROSS JOIN jsonb_array_elements(pv1.impacted_by) as impactedBy
                               INNER JOIN parameter_values pv2
                                          ON impactedBy ->> 'parameterId' = (pv2.parameters_id :: text)
                                            AND pv1.jobs_id = pv2.jobs_id
                        GROUP BY pv1.id)

UPDATE parameter_values
SET impacted_by = updated_values.new_impacted_by
FROM updated_values
WHERE parameter_values.id = updated_values.pv1_id;

--changeset siba:202312061357-seed-data-3
--comment: added parameter_values_id in impacted_by of branching rules in temp_parameter_values table

WITH updated_values AS (SELECT tpv1.id as tpv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   impactedBy::jsonb,
                                   ARRAY ['parameterValueId'],
                                   (tpv2.id)::text::jsonb
                                 )
                               )      AS new_impacted_by
                        FROM temp_parameter_values tpv1
                               CROSS JOIN jsonb_array_elements(tpv1.impacted_by) as impactedBy
                               INNER JOIN temp_parameter_values tpv2
                                          ON impactedBy ->> 'parameterId' = (tpv2.parameters_id :: text)
                                            AND tpv1.jobs_id = tpv2.jobs_id
                        GROUP BY tpv1.id)

UPDATE temp_parameter_values
SET impacted_by = updated_values.new_impacted_by
FROM updated_values
WHERE temp_parameter_values.id = updated_values.tpv1_id;
