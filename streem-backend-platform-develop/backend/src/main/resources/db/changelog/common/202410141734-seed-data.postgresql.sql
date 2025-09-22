-- liquibase formatted sql

-- changeset abhishek:202410141734-seed-data
-- comment: "leastCount" migrated FROM {"value":"", "selector":"CONSTANT"} TO "leastCount" : null

UPDATE parameters
SET data = JSONB_SET(
    data,
    '{leastCount}',
    'null'::jsonb
)
WHERE type IN ('NUMBER', 'SHOULD_BE')
AND (
    data->'leastCount'->>'value' = ''
    OR data->>'leastCount' = ''
    OR data->>'leastCount' = 'null'
    or data->'leastCount'->>'value' = 'null'
);
