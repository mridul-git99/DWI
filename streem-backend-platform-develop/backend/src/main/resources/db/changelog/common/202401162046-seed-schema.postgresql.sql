-- liquibase formatted sql

--changeset siba:202401162046-seed-schema-1
--comment drop constraints of temp_parameter_value_media_mapping and temp_parameter_verifications

ALTER TABLE temp_parameter_value_media_mapping
  DROP CONSTRAINT IF EXISTS temp_activity_value_media_mapping_temp_activity_values_id_fkey;

ALTER TABLE temp_parameter_verifications
  DROP CONSTRAINT IF EXISTS temp_parameter_verifications_temp_parameter_values_id_fkey;

--changeset siba:202401162046-seed-schema-2
--comment add constraints of temp_parameter_value_media_mapping and temp_parameter_verifications with on cascade delete

ALTER TABLE temp_parameter_value_media_mapping
  ADD CONSTRAINT idx0ceefbcaf338456caf296ce92c31067b FOREIGN KEY (temp_parameter_values_id) REFERENCES temp_parameter_values (id) ON DELETE CASCADE;


ALTER TABLE temp_parameter_verifications
  ADD CONSTRAINT idx8c5819fc920c4b55a2b7e43c2bc9ef64 FOREIGN KEY (temp_parameter_values_id) REFERENCES temp_parameter_values (id) ON DELETE CASCADE;
