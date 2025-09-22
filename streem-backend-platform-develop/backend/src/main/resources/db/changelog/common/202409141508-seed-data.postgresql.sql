-- liquibase formatted sql

--changeset kousik :202409141508-seed-data
--comment: updated offset for old setProperty automations in action details

UPDATE automations
SET action_details = jsonb_set(
    jsonb_set(
        jsonb_set(
            jsonb_set(
                action_details,
                '{offsetDateUnit}', to_jsonb(action_details->>'dateUnit')
            ),
            '{offsetParameterId}', 'null'::jsonb
        ),
        '{offsetSelector}', '"CONSTANT"'::jsonb
    ),
    '{offsetValue}', to_jsonb(action_details->>'value')
)
WHERE action_details IS NOT NULL
  AND action_details->>'offsetSelector' IS NULL
  AND action_details->>'selector' = 'CONSTANT'
  AND (action_details->>'captureProperty' = 'START_TIME' OR action_details->>'captureProperty' = 'END_TIME')
  AND action_type = 'SET_PROPERTY'
  AND (action_details->>'propertyInputType' = 'DATE' OR action_details->>'propertyInputType' = 'DATE_TIME');
