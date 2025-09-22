-- liquibase formatted sql

--changeset peesa:202404221317-seed-schema-data.postgresql.sql
ALTER TABLE parameters
ADD COLUMN enable_exception_approval boolean default false;
