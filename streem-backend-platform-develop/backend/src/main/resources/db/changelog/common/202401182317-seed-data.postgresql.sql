-- liquibase formatted sql

--changeset siba:202401182317-seed-data-1
--comment remove trained users entry where task is archived

delete
from checklist_default_users cdu using tasks t
where cdu.tasks_id = t.id
  and t.archived = true;
