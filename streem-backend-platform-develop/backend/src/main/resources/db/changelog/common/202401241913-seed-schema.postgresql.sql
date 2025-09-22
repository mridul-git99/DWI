-- liquibase formatted sql

--changeset Nishant:202401241913-seed-schema-data-1
-- Update the 'actionDetails' column to add the 'selector' key with the value 'CONSTANT'
UPDATE automations
SET action_details = jsonb_set(
    COALESCE(action_details, '{}'::jsonb),
    '{selector}',
    '"PARAMETER"'
)
WHERE action_type IN ('INCREASE_PROPERTY', 'DECREASE_PROPERTY', 'SET_PROPERTY') and action_details ->> 'parameterId' is not null;

UPDATE automations
SET action_details = jsonb_set(
    COALESCE(action_details, '{}'::jsonb),
    '{selector}',
    '"CONSTANT"'
)
WHERE action_type IN ('INCREASE_PROPERTY', 'DECREASE_PROPERTY', 'SET_PROPERTY') and action_details ->> 'parameterId' is null;
