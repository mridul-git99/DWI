-- liquibase formatted sql

--changeset nishant:202405201437-seed-data
--comment: added column previous_state in exceptions table to keep track of parameterValue states at initiation

ALTER TABLE exceptions
    ADD COLUMN previous_state VARCHAR(50) DEFAULT NULL;
