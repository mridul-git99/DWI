-- liquibase formatted sql

--changeset kousik:202409131555-seed-schema-1
--comment: altered tasks table  added has_interlocks

ALTER TABLE tasks
ADD COLUMN has_interlocks BOOLEAN DEFAULT false;

ALTER TABLE interlocks
ALTER COLUMN target_entity_id TYPE BIGINT USING target_entity_id::bigint;

ALTER TABLE interlocks
ADD CONSTRAINT fk_target_entity
FOREIGN KEY (target_entity_id) REFERENCES tasks(id);

update tasks SET has_interlocks = true where id in (select target_entity_id from interlocks)
