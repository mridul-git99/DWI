-- liquibase formatted sql

--changeset nishant:202403151629-seed-schema
--comment: create corrections table

CREATE TABLE corrections (
    id BIGINT NOT NULL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    old_choices JSONB,
    new_choices JSONB,
    parameter_values_id BIGINT NOT NULL,
    task_executions_id BIGINT NOT NULL,
    facilities_id BIGINT NOT NULL,
    jobs_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    initiators_reason TEXT,
    correctors_reason TEXT,
    reviewers_reason TEXT,
    created_by           BIGINT NOT NULL,
    created_at           BIGINT NOT NULL,
    modified_by          BIGINT NOT NULL,
    modified_at          BIGINT NOT NULL,
    CONSTRAINT aefe9a36f6ba4173bc2c86dbef02 FOREIGN KEY (parameter_values_id) REFERENCES parameter_values(id),
    CONSTRAINT b9415b44a9a744d880c3b6f498a9 FOREIGN KEY (task_executions_id) REFERENCES task_executions(id),
    CONSTRAINT f9701af208fd40ca8f90baba1566 FOREIGN KEY (facilities_id) REFERENCES facilities(id),
    CONSTRAINT f381dec79bb34595bcc5b937e820 FOREIGN KEY (jobs_id) REFERENCES jobs(id)
);

