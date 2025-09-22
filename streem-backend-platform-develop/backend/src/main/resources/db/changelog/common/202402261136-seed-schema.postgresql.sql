-- liquibase formatted sql

--changeset siba:202402261136-seed-schema-1
--comment: create user group table


create table user_groups
(
  id          BIGINT PRIMARY KEY NOT NULL,
  name        TEXT               NOT NULL,
  description TEXT               NOT NULL,
  active      BOOLEAN            NOT NULL,
  created_at  BIGINT             NOT NULL,
  modified_at BIGINT             NOT NULL,
  facility_id BIGINT             NOT NULL REFERENCES facilities (id),
  created_by  BIGINT             NOT NULL REFERENCES users (id),
  modified_by BIGINT             NOT NULL REFERENCES users (id)
);
--changeset siba:202402261136-seed-schema-2
--comment: add unique constraint to user group table

ALTER TABLE user_groups
  ADD CONSTRAINT idx71f7a56dd921446db4818e6eb
    UNIQUE (name, facility_id, active);

--changeset siba:202402261136-seed-schema-3
--comment: create user group members table

create table user_group_members
(
  users_id    BIGINT NOT NULL REFERENCES users (id),
  groups_id   BIGINT NOT NULL REFERENCES user_groups (id),
  created_at  BIGINT NOT NULL,
  modified_at BIGINT NOT NULL,
  created_by  BIGINT NOT NULL REFERENCES users (id),
  modified_by BIGINT NOT NULL REFERENCES users (id)
);

ALTER TABLE user_group_members
  ADD CONSTRAINT user_group_members_pk
    PRIMARY KEY (users_id, groups_id);

--changeset siba:202402261136-seed-schema-4
--comment: drop old primary key constraint

ALTER TABLE task_execution_user_mapping
  DROP CONSTRAINT task_execution_user_mapping_pkey;

--changeset siba:202402261136-seed-schema-5
--comment: drop not null constraint on users_id of task_execution_user_mapping

ALTER TABLE task_execution_user_mapping
  ALTER COLUMN users_id DROP NOT NULL;

--changeset siba:202402261136-seed-schema-6
-- comment: create user group column

ALTER TABLE task_execution_user_mapping
  ADD COLUMN user_groups_id BIGINT REFERENCES user_groups (id);

--changeset siba:202402261136-seed-schema-7
--comment: drop unique constraint task_execution_user_mapping

ALTER TABLE parameter_verifications
  ADD COLUMN user_groups_id BIGINT REFERENCES user_groups (id);

ALTER TABLE temp_parameter_verifications
  ADD COLUMN user_groups_id BIGINT REFERENCES user_groups (id);


-- changeset siba:202402261136-seed-schema-8
-- comment: create trained users table

create table trained_users
(
  id             bigint not null
    constraint trained_users_pkey
      primary key,
  checklists_id  bigint not null
    constraint fkn1ae4i3odmi0j3uhveq6c7cdk
      references checklists,
  users_id       bigint
    constraint fkm1ae4i3odmi0j3uhveq6c7cdk
      references users,
  facilities_id  bigint
    references facilities,
  user_groups_id bigint
    references user_groups,
  created_at     bigint not null,
  modified_at    bigint not null,
  created_by     bigint not null
    constraint fkm1ae4i3odmi0j3uhveq6c8cda
      references users,
  modified_by    bigint not null
    constraint fkm1ae4i3odmi0j3uhveq6c8cdb
      references users,
  constraint acabb5099974026a82d44c28f9
    unique (checklists_id, users_id, facilities_id),
  constraint acabb5099974026a82d44c28ff
    unique (checklists_id, user_groups_id, facilities_id)
);
