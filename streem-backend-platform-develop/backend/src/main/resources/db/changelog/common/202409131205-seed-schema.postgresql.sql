-- liquibase formatted sql

--changeset kousik :202409131205-seed-schema
--comment: Added a new foreign key constraint with ON DELETE CASCADE
ALTER TABLE exception_reviewers
DROP CONSTRAINT s00f287z95084fedaf56950c8fd5;

ALTER TABLE exception_reviewers
ADD CONSTRAINT fk_exception_reviewers_exceptions
FOREIGN KEY (exceptions_id)
REFERENCES exceptions(id)
ON DELETE CASCADE;

