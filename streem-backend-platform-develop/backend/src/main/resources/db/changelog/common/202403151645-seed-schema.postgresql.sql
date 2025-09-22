-- liquibase formatted sql

--changeset nishant:202403151635-seed-schema
--comment: create reviewers table

CREATE TABLE reviewers (
    id BIGINT NOT NULL PRIMARY KEY,
    user_groups_id BIGINT,
    users_id BIGINT,
    corrections_id BIGINT NOT NULL,
    action_performed BOOLEAN DEFAULT FALSE NOT NULL,
    created_by           BIGINT NOT NULL,
    created_at           BIGINT NOT NULL,
    modified_by          BIGINT NOT NULL,
    modified_at          BIGINT NOT NULL,
    CONSTRAINT c758e21712004bd4a11acefdf28c FOREIGN KEY (user_groups_id) REFERENCES user_groups(id),
    CONSTRAINT a59f386ad1414f739b572b52933n FOREIGN KEY (users_id) REFERENCES users(id),
    CONSTRAINT s00f287b95084fedaf56950c8fd5 FOREIGN KEY (corrections_id) REFERENCES corrections(id)
);

--changeset nishant:202403151635-seed-schema-2
--comment: add new column has_corrections to parameter_values

ALTER TABLE parameter_values
ADD COLUMN has_corrections BOOLEAN DEFAULT FALSE NOT NULL;
