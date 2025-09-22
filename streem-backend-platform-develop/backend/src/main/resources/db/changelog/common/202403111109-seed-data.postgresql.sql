-- liquibase formatted sql

--changeset nishant:202403111109-seed-data
--comment: least count data migration

UPDATE parameters
SET data = JSONB_SET(
    data,
    '{leastCount}',
    jsonb_build_object(
        'selector', 'CONSTANT',
        'value', data->>'leastCount'
    )
)
WHERE type IN ('NUMBER', 'SHOULD_BE')
AND data->>'leastCount' IS NOT NULL
