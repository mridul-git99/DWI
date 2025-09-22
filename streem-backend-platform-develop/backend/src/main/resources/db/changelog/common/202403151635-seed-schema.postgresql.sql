-- liquibase formatted sql

--changeset nishant:202403151635-seed-schema
--comment: create correctors table

CREATE TABLE correctors (
    id BIGINT NOT NULL PRIMARY KEY,
    user_groups_id BIGINT ,
    users_id BIGINT,
    corrections_id BIGINT NOT NULL,
    action_performed BOOLEAN DEFAULT FALSE NOT NULL,
    created_by           BIGINT NOT NULL,
    created_at           BIGINT NOT NULL,
    modified_by          BIGINT NOT NULL,
    modified_at          BIGINT NOT NULL,
    CONSTRAINT b194dc66c0b84bbb969f77fbf472 FOREIGN KEY (user_groups_id) REFERENCES user_groups(id),
    CONSTRAINT n8f0ae13e8ba4f91b3ef72623cf5 FOREIGN KEY (users_id) REFERENCES users(id),
    CONSTRAINT ifbc41b4dd0b4df6baaec00c03ce FOREIGN KEY (corrections_id) REFERENCES corrections(id)
);


