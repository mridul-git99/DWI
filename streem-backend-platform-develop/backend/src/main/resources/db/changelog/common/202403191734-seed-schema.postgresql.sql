-- liquibase formatted sql

--changeset siba:202403191734-seed-schema-1
--comment: create process level permissions

CREATE TABLE process_permissions
(
  id          BIGINT PRIMARY KEY NOT NULL,
  type        VARCHAR(50)        NOT NULL,
  description TEXT
);

INSERT INTO process_permissions (id, type)
VALUES (1, 'JOB_EXECUTOR');

INSERT INTO process_permissions (id, type)
VALUES (2, 'JOB_REVIEWER');

INSERT INTO process_permissions (id, type)
VALUES (3, 'JOB_MANAGER');

INSERT INTO process_permissions (id, type)
VALUES (4, 'JOB_ISSUER');

--changeset siba:202403191734-seed-schema-2
--comment: map permissiona to trained_users

CREATE TABLE trained_users_process_permissions_mapping
(
  id                     BIGINT PRIMARY KEY NOT NULL,
  process_permissions_id BIGINT             NOT NULL,
  trained_users_id       BIGINT             NOT NULL,
  created_at             BIGINT             NOT NULL,
  created_by             BIGINT             NOT NULL,
  modified_at            BIGINT             NOT NULL,
  modified_by            BIGINT             NOT NULL,
  FOREIGN KEY (process_permissions_id) REFERENCES process_permissions (id) ON UPDATE NO ACTION ON DELETE NO ACTION,
  FOREIGN KEY (trained_users_id) REFERENCES trained_users (id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES users (id),
  FOREIGN KEY (modified_by) REFERENCES users (id)
);

--changeset siba:202403191734-seed-schema-3
--comment: add unique constraint to process_permissions_trained_users

ALTER TABLE trained_users_process_permissions_mapping
  ADD CONSTRAINT process_permissions_trained_users_unique UNIQUE (process_permissions_id, trained_users_id);


