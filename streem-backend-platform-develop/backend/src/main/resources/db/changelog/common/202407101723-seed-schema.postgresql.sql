-- liquibase formatted sql

-- changeset siba:202407101723-seed-schema-1
-- comment: Add indexes to parameter_verification and correction tables

CREATE INDEX idx_parameter_verifications_jobs_id ON parameter_verifications (jobs_id);
CREATE INDEX idx_parameter_verifications_parameter_values_id ON parameter_verifications (parameter_values_id);

CREATE INDEX idx_corrections_jobs_id ON corrections (jobs_id);
CREATE INDEX idx_corrections_parameter_values_id ON corrections (parameter_values_id);

