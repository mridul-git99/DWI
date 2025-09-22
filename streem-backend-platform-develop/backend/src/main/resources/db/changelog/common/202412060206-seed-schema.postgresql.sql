-- liquibase formatted sql

-- changeset siba:202412060206-seed-schema-1
-- comment: introduce actions and effect


CREATE TYPE actions_trigger_type AS ENUM ('START_TASK', 'COMPLETE_TASK'); -- TODO: ADD MORE type later


CREATE TABLE actions
(
  id                BIGINT PRIMARY KEY,
  name              TEXT                 NOT NULL,
  code              VARCHAR(50)          NOT NULL,
  description       TEXT,
  created_at        BIGINT               NOT NULL,
  modified_at       BIGINT               NOT NULL,
  created_by        BIGINT               NOT NULL,
  modified_by       BIGINT               NOT NULL,
  trigger_type      actions_trigger_type NOT NULL,
  trigger_entity_id BIGINT               NOT NULL,
  checklists_id     BIGINT               NOT NULL,
  archived          BOOLEAN DEFAULT FALSE,
  success_message   TEXT,
  failure_message   TEXT
);

ALTER TABLE actions
  ADD CONSTRAINT idx4a0db64k01ab4e2b9b58e723 FOREIGN KEY (created_by) REFERENCES users (id);


ALTER TABLE actions
  ADD CONSTRAINT idx4a0db64k04ab4e2b9b58e723 FOREIGN KEY (modified_by) REFERENCES users (id);

ALTER TABLE actions
  ADD CONSTRAINT idx4a0db64k04ab4e2b9b58e724 FOREIGN KEY (checklists_id) REFERENCES checklists (id);

CREATE TABLE action_facility_mapping
(
  actions_id    BIGINT NOT NULL,
  facilities_id BIGINT NOT NULL,
  created_at    BIGINT NOT NULL,
  modified_at   BIGINT NOT NULL,
  created_by    BIGINT NOT NULL,
  modified_by   BIGINT NOT NULL
);

ALTER TABLE action_facility_mapping
  ADD CONSTRAINT idx67de11f12c88415da1f4964f FOREIGN KEY (actions_id) REFERENCES actions (id);

ALTER TABLE action_facility_mapping
  ADD CONSTRAINT idx67de11f12c88415da1f4964g FOREIGN KEY (facilities_id) REFERENCES facilities (id);

ALTER TABLE action_facility_mapping
  ADD CONSTRAINT idx67de11f12c88415da1f4964h FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE action_facility_mapping
  ADD CONSTRAINT idx67de11f12c88415da1f4964i FOREIGN KEY (modified_by) REFERENCES users (id);


CREATE TYPE effects_type AS ENUM ('SQL_QUERY', 'MONGO_QUERY', 'REST_API');
-- CAN BE MORE

-- TODO: THink of effect results in subsequent effect (referenced_effect_id)
CREATE TABLE effects
(
  id           BIGINT PRIMARY KEY,
  actions_id   BIGINT       NOT NULL,
  order_tree   INTEGER      NOT NULL,
  type         effects_type NOT NULL,
  query        JSONB   DEFAULT '{}'::jsonb,
  api_endpoint JSONB   DEFAULT '{}'::jsonb,
  api_method   VARCHAR(20),
  api_payload  JSONB   DEFAULT '{}'::jsonb,
  api_headers  jsonb,
  created_at   BIGINT       NOT NULL,
  modified_at  BIGINT       NOT NULL,
  name         TEXT         NOT NULL,
  archived     BOOLEAN DEFAULT FALSE,
  description  TEXT,
  created_by   BIGINT       NOT NULL,
  modified_by  BIGINT       NOT NULL
);

ALTER TABLE effects
  ADD CONSTRAINT idx4a0db64c04ab4e2b9b58e723 FOREIGN KEY (actions_id) REFERENCES actions (id);


ALTER TABLE effects
  ADD CONSTRAINT idx4a0db64c04ab4e2b9b58e725 FOREIGN KEY (created_by) REFERENCES users (id);

ALTER TABLE effects
  ADD CONSTRAINT idx4a0db64c04ab4e2b9b58e726 FOREIGN KEY (modified_by) REFERENCES users (id);
