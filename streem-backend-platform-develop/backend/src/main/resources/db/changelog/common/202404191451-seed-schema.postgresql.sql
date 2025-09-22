-- liquibase formatted sql

--changeset siba:202404191451-seed-schema-1
--comment: add unique index to trained_user_tasks_mapping

CREATE UNIQUE INDEX idx_trained_user_tasks_mapping ON trained_user_tasks_mapping (trained_users_id, tasks_id);
