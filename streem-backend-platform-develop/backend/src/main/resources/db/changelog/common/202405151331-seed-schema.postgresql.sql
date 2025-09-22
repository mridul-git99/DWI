-- liquibase formatted sql

--changeset nishant:202405151331-seed-schema
--comment: added column previous_execution_state in corrections table to keep track of parameterValue states at initiation

ALTER TABLE corrections
    ADD COLUMN previous_state VARCHAR(50) DEFAULT NULL;
