-- liquibase formatted sql

-- changeset kousik - abhishek :202408291700-seed-data-1
-- comment: Updated data column in Parameters table , added 'selector' field in propertyValidations

UPDATE parameters
SET data = jsonb_set(
    data,
    '{propertyValidations}',
    (
        SELECT jsonb_agg(
            jsonb_set(
                elem,
                '{selector}',
                '"CONSTANT"',
                true
            )
        )
        FROM jsonb_array_elements(data->'propertyValidations') AS elem
    ),
    true
)
WHERE data IS NOT NULL
  AND jsonb_typeof(data->'propertyValidations') = 'array'
  AND jsonb_array_length(CASE WHEN jsonb_typeof(data->'propertyValidations') = 'array' THEN data->'propertyValidations' ELSE '[]'::jsonb END) > 0;

