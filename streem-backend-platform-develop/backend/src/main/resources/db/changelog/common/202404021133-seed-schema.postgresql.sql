-- liquibase formatted sql

--changeset nishant:202404021133-seed-schema
--comment: create corrections media mapping table

CREATE TABLE corrections_media_mapping (
    id BIGINT NOT NULL PRIMARY KEY,
    created_by           BIGINT NOT NULL,
    created_at           BIGINT NOT NULL,
    modified_by          BIGINT NOT NULL,
    modified_at          BIGINT NOT NULL,
    is_old_media BOOLEAN DEFAULT FALSE NOT NULL,
    archived BOOLEAN DEFAULT FALSE NOT NULL,
    corrections_id BIGINT NOT NULL,
    medias_id BIGINT NOT NULL,
    parameter_values_id BIGINT NOT NULL,
    CONSTRAINT c8538b12248a4ad9930ff096232a FOREIGN KEY (corrections_id) REFERENCES corrections (id),
    CONSTRAINT bb03720f4cb483880ceefa6cef98 FOREIGN KEY (medias_id) REFERENCES medias (id),
    CONSTRAINT cc54b22867a4fbdbf60b934fc3cc FOREIGN KEY (parameter_values_id) REFERENCES parameter_values (id)
);
