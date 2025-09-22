-- liquibase formatted sql

--changeset siba:202403211317-seed-schema-1
--comment: create the schema for the task executor lock table

CREATE TABLE task_executor_locks
(
  id                  BIGINT PRIMARY KEY NOT NULL,
  tasks_id            BIGINT             NOT NULL references tasks (id) ON DELETE CASCADE,
  lock_type           VARCHAR(50)        NOT NULL,
  referenced_tasks_id BIGINT             NOT NULL references tasks (id) ON DELETE CASCADE,
  created_at          BIGINT             NOT NULL,
  modified_at         BIGINT             NOT NULL,
  created_by          BIGINT             NOT NULL references users (id) ON DELETE CASCADE,
  modified_by         BIGINT             NOT NULL references users (id) ON DELETE CASCADE
);

--changeset siba:202403211317-seed-schema-2
--comment: add unique constraint to task executor lock table

ALTER TABLE task_executor_locks
  ADD CONSTRAINT unique_lock_type UNIQUE (tasks_id, referenced_tasks_id, lock_type);



--changeset siba:202403211317-seed-schema-3
--comment: add column has lock to tasks table

ALTER TABLE tasks
  ADD COLUMN has_executor_lock BOOLEAN NOT NULL DEFAULT FALSE;
