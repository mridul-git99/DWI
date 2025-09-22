-- liquibase formatted sql

-- changeset peesa:202408161319-seed-data-1
-- comment: remove calculation verification types and remove deleted trained users

update parameters p set verification_type = 'NONE' where  "type" = 'CALCULATION' and verification_type != 'NONE';

update parameters p set verification_type = 'NONE' where is_auto_initialized = TRUE and verification_type != 'NONE';

DELETE FROM trained_users
USING users
WHERE trained_users.users_id = users.id
AND users.archived = true;
