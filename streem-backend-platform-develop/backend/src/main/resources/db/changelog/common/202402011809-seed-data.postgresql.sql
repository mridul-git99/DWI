-- liquibase formatted sql

--changeset siba:202402011809-seed-schema-data-1
--comment: update the 'actionDetails' column to add the 'selector' key for set, archive, and create object
UPDATE automations
SET action_details = jsonb_set(
    COALESCE(action_details, '{}'::jsonb),
    '{selector}',
    '"PARAMETER"'
)
WHERE action_type IN ('SET_RELATION');

UPDATE automations
SET action_details = jsonb_set(
  COALESCE(action_details, '{}'::jsonb),
  '{selector}',
  '"NONE"'
                     )
WHERE action_type IN ('CREATE_OBJECT', 'ARCHIVE_OBJECT');
