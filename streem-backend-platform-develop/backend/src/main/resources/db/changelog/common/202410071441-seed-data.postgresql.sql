-- liquibase formatted sql

--changeset kousik :202410071441-seed-data
--comment: update rules for archived parameters

update parameters
set rules = null
where archived = true;
