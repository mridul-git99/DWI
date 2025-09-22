-- liquibase formatted sql

--changeset siba:202311161703-data-1
--comment : Add task_executions_id to parameter_values

update parameter_values pv
set task_executions_id = te.id
from task_executions te
       inner join public.tasks t on te.tasks_id = t.id
       inner join parameters p on t.id = p.tasks_id
where p.id = pv.parameters_id
  and te.jobs_id = pv.jobs_id
  and p.target_entity_type='TASK';

--changeset siba:202311161703-data-2
--comment : Add task_executions_id to temp_parameter_values

update temp_parameter_values tpv
set task_executions_id = te.id
from task_executions te
       inner join public.tasks t on te.tasks_id = t.id
       inner join parameters p on t.id = p.tasks_id
where p.id = tpv.parameters_id
  and te.jobs_id = tpv.jobs_id
  and p.target_entity_type='TASK';
