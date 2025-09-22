-- liquibase formatted sql

--changeset siba:202311212101-schema-1
--comment : create variation entity

CREATE TABLE variations
(
  id                  BIGINT                      NOT NULL
    constraint pk_variations
      primary key,
  name                TEXT                        NOT NULL,
  description         TEXT,
  parameter_values_id BIGINT                      NOT NULL,
  jobs_id             BIGINT                      NOT NULL,
  new_details             JSONB   DEFAULT '{}'::JSONB NOT NULL,
  old_details             JSONB   DEFAULT '{}'::JSONB NOT NULL,
  type                VARCHAR(50)                 NOT NULL,
  created_at          BIGINT                      NOT NULL,
  modified_at         BIGINT                      NOT NULL,
  created_by          BIGINT                      NOT NULL,
  modified_by         BIGINT                      NOT NULL,
  variation_number    TEXT                        NOT NULL,
  config_id           TEXT                        NOT NULL
);

ALTER TABLE variations
  ADD CONSTRAINT variations_parameter_values_id_fkey FOREIGN KEY (parameter_values_id) REFERENCES parameter_values (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE variations
  ADD CONSTRAINT variations_jobs_id_fkey FOREIGN KEY (jobs_id) REFERENCES jobs (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE variations
  ADD CONSTRAINT bf1701857ba1456c9cdf9965ec UNIQUE (name, jobs_id, config_id);

ALTER TABLE variations
  ADD CONSTRAINT fabe29b1b0764467b3247931d4 UNIQUE (variation_number, jobs_id, config_id);

ALTER TABLE variations
  ADD CONSTRAINT e0d4f71ce7a6434d91f52f7afa UNIQUE (config_id, parameter_values_id);


--changeset siba:202311212101-schema-2
--comment : add column has_variations to parameter_values and temp_parameter_values
ALTER TABLE parameter_values
  ADD COLUMN has_variations boolean default false;

ALTER TABLE temp_parameter_values
  ADD COLUMN has_variations boolean default false;

-- changeset siba:202311212101-schema-3
-- comment : create variation_media_mapping entity

CREATE TABLE variation_media_mapping
(
  medias_id          BIGINT NOT NULL,
  variations_id BIGINT NOT NULL,
  archived           BOOLEAN DEFAULT FALSE,
  created_at         BIGINT NOT NULL,
  modified_at        BIGINT NOT NULL,
  created_by         BIGINT NOT NULL,
  modified_by        BIGINT NOT NULL,
  PRIMARY KEY (medias_id, variations_id),
  FOREIGN KEY (created_by) REFERENCES users(id),
  FOREIGN KEY (modified_by) REFERENCES users(id),
  FOREIGN KEY (medias_id) REFERENCES medias(id),
  FOREIGN KEY (variations_id) REFERENCES variations(id)
);
