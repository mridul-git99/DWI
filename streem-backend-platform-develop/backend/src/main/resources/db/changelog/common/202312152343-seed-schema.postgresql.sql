-- liquibase formatted sql

--changeset siba:202312152343-seed-schema-1
--comment drop unique constraints on variations table and recreate them at parameter value id level

ALTER TABLE variations
  DROP CONSTRAINT bf1701857ba1456c9cdf9965ec;

ALTER TABLE variations
  DROP CONSTRAINT fabe29b1b0764467b3247931d4;
