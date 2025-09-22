-- liquibase formatted sql

--changeset Siba:202305241140-seed-schema-1

CREATE TABLE IF NOT EXISTS temp_parameter_verifications
(
  id                    BIGINT        NOT NULL,
  comments              TEXT,
  jobs_id               BIGINT        NOT NULL,
  temp_parameter_values_id   BIGINT        NOT NULL,
  verification_status   VARCHAR(50)  NOT NULL,
  verification_type     VARCHAR(50)  NOT NULL,
  created_at            BIGINT        NOT NULL,
  created_by            BIGINT        NOT NULL,
  modified_by           BIGINT        NOT NULL,
  modified_at           BIGINT        NOT NULL,
  users_id              BIGINT        NOT NULL,
  CONSTRAINT temp_parameter_verifications_pkey PRIMARY KEY (id),
  FOREIGN KEY (temp_parameter_values_id) REFERENCES temp_parameter_values (id),
  FOREIGN KEY (jobs_id) REFERENCES jobs (id)
);

ALTER TABLE parameter_verifications ALTER COLUMN verification_status TYPE VARCHAR(50);
ALTER TABLE parameter_verifications ALTER COLUMN verification_type TYPE VARCHAR(50);
