-- liquibase formatted sql

--changeset nishant:202404241251-seed-schema
--comment: create exception reviewers table

CREATE TABLE exception_reviewers (
    id BIGINT NOT NULL PRIMARY KEY,
    user_groups_id BIGINT,
    users_id BIGINT,
    exceptions_id BIGINT NOT NULL,
    action_performed BOOLEAN DEFAULT FALSE NOT NULL,
    created_by           BIGINT NOT NULL,
    created_at           BIGINT NOT NULL,
    modified_by          BIGINT NOT NULL,
    modified_at          BIGINT NOT NULL,
    CONSTRAINT c758e21212004bd4a11acefdf28c FOREIGN KEY (user_groups_id) REFERENCES user_groups(id),
    CONSTRAINT a59f386bd1414f739b572b52933n FOREIGN KEY (users_id) REFERENCES users(id),
    CONSTRAINT s00f287z95084fedaf56950c8fd5 FOREIGN KEY (exceptions_id) REFERENCES exceptions(id)
);

--changeset nishant:202403151635-seed-schema-2
--comment: add new column has_exceptions to parameter_values

ALTER TABLE parameter_values
ADD COLUMN has_exceptions BOOLEAN DEFAULT FALSE NOT NULL;
