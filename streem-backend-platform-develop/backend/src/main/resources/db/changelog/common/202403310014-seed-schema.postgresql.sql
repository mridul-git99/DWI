-- liquibase formatted sql

--changeset siba:202403310014-seed-schema-1
--comment: add trained users index

CREATE INDEX idx_d554946a549ed913739e2388d9940 ON trained_users (user_groups_id);
CREATE INDEX idx_d554946a549ed913739e2388d9941 ON trained_users (facilities_id);

--changeset siba:202403310014-seed-schema-2
--comment: add trained user task mapping index

CREATE INDEX idx_d554946a549ed913739e2388d9942 ON trained_user_tasks_mapping (trained_users_id);
CREATE INDEX idx_d554946a549ed913739e2388d9943 ON trained_user_tasks_mapping (tasks_id);
