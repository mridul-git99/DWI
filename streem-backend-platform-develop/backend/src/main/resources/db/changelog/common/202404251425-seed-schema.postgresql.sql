-- liquibase formatted sql

--changeset mokshesh:202404091847-seed-schema-1
--comment: added column client_epoch for ensuring only recent client request is processed for parameter execution
ALTER TABLE parameter_values ADD COLUMN IF NOT EXISTS client_epoch BIGINT;
ALTER TABLE temp_parameter_values ADD COLUMN IF NOT EXISTS client_epoch BIGINT;

--changeset siba:202404091847-seed-schema-2
--comment: added column version to make sure during concurrent request older transactions dont get commited
ALTER TABLE parameter_values ADD COLUMN IF NOT EXISTS version BIGINT default 0;
ALTER TABLE temp_parameter_values ADD COLUMN IF NOT EXISTS version BIGINT default 0;
