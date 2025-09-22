-- liquibase formatted sql

--changeset siba:202403110104-seed-schema-1
--comment: renaming the table checklist_default_users to trained_user_tasks_mapping

ALTER TABLE checklist_default_users
  RENAME TO trained_user_tasks_mapping;

--changeset siba:202403110104-seed-schema-2
--comment: dropping irrelevant columns from the table trained_user_tasks_mapping

ALTER TABLE trained_user_tasks_mapping
  DROP COLUMN IF EXISTS users_id,
  DROP COLUMN IF EXISTS facilities_id,
  DROP COLUMN IF EXISTS checklists_id;

--changeset siba:202403110104-seed-schema-3
--comment: adding audit related columns to the table trained_user_tasks_mapping

ALTER TABLE trained_user_tasks_mapping
  ADD COLUMN IF NOT EXISTS created_at  BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) ::BIGINT,
  ADD COLUMN IF NOT EXISTS modified_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) ::BIGINT,
  ADD COLUMN IF NOT EXISTS created_by  BIGINT NOT NULL DEFAULT 1,
  ADD COLUMN IF NOT EXISTS modified_by BIGINT NOT NULL DEFAULT 1;

--changeset siba:202403110104-seed-schema-4
--comment: create table user group audits table

create table if not exists user_group_audits
(
  id               BIGINT NOT NULL PRIMARY KEY,
  user_groups_id   BIGINT NOT NULL REFERENCES user_groups (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  organisations_id BIGINT NOT NULL REFERENCES organisations (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  facilities_id    BIGINT NOT NULL REFERENCES facilities (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  triggered_by     BIGINT NOT NULL REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  triggered_at     BIGINT NOT NULL,
  details          TEXT
);
