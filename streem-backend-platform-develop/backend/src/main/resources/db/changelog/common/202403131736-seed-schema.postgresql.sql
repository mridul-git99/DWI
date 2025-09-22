-- liquibase formatted sql

--changeset peesa:202403131736-seed-schema
--comment: add task dependencies schema

create TABLE IF NOT EXISTS task_dependencies
  (
     id                   BIGINT NOT NULL,
     prerequisite_task_id BIGINT NOT NULL,
     dependent_task_id    BIGINT NOT NULL,
     created_by           BIGINT NOT NULL,
     created_at           BIGINT NOT NULL,
     modified_by          BIGINT NOT NULL,
     modified_at          BIGINT NOT NULL,
     CONSTRAINT fk_prerequisite_task FOREIGN KEY (prerequisite_task_id)
     REFERENCES tasks(id) ON delete CASCADE,
     CONSTRAINT fk_dependent_task FOREIGN KEY (dependent_task_id) REFERENCES
     tasks(id) ON delete CASCADE,
     CONSTRAINT task_dependencies_pkey PRIMARY KEY (id)
  );



