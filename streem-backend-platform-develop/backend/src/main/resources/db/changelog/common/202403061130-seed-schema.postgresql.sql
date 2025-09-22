-- liquibase formatted sql

--changeset siba:202403061130-seed-schema-1
--comment: added column color_code to checklists

ALTER TABLE checklists
  ADD COLUMN color_code varchar(50);
