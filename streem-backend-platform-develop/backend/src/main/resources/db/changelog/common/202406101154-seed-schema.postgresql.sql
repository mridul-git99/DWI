-- liquibase formatted sql

-- changeset peesa:202406101154-seed-schema
-- comment: added column time_zone to facilities table
ALTER TABLE facilities
  ADD COLUMN IF NOT EXISTS time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC'


-- changeset peesa:202406101154-seed-data
-- comment: correct validations for date and date_time types
UPDATE parameters
SET data = jsonb_set(
    data,
    '{propertyValidations}',
    (
        SELECT jsonb_agg(
            CASE
                WHEN (validation ->> 'constraint' = 'LTE'
                      AND (validation ->> 'propertyInputType' = 'DATE'
                           OR validation ->> 'propertyInputType' = 'DATE_TIME'))
                THEN jsonb_set(validation, '{value}', to_jsonb(((validation->>'value')::int + 1)::text))
                ELSE validation
            END
        )
        FROM jsonb_array_elements(data->'propertyValidations') AS validation
    )
)
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements(data->'propertyValidations') AS validation
    WHERE (validation ->> 'propertyInputType' IN ('DATE', 'DATE_TIME')
           AND validation ->> 'constraint' IN ('LTE'))
)
