-- liquibase formatted sql
--changeset Peesa:202312181200-seed-schema-1


CREATE TABLE IF not EXISTS interlocks
(
    id                  BIGINT NOT NULL,
    target_entity_id    VARCHAR NOT NULL,
    target_entity_type  VARCHAR(50) NOT NULL,
    validations         jsonb,
    created_at          BIGINT NOT NULL,
    created_by          BIGINT NOT NULL,
    modified_by         BIGINT NOT NULL,
    modified_at         BIGINT NOT NULL,
    constraint interlocks_pkey primary key (id)
);
