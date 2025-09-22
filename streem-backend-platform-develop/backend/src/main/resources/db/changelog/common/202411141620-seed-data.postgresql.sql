-- liquibase formatted sql

-- changeset nishant:202410141734-seed-data
-- comment: "data and date time validations" migration

WITH updated_validations AS (
  SELECT
    id,
    CASE
      WHEN jsonb_typeof(validations) = 'array'
           AND jsonb_array_length(validations) > 0
           AND NOT EXISTS (
             SELECT 1
             FROM jsonb_array_elements(validations) AS element
             WHERE element @> '{"ruleId": null}'
               AND element @> '{"exceptionApprovalType": null}'
               AND element @> '{"dateTimeParameterValidations": null}'
           )
      THEN (
        SELECT jsonb_agg(
                 jsonb_build_object(
                   'ruleId', uuid_generate_v4()::text,
                   'exceptionApprovalType', 'DEFAULT_FLOW',
                   'dateTimeParameterValidations', jsonb_build_array(element)
                 )
               )
        FROM jsonb_array_elements(validations) AS element
      )
      ELSE validations
    END AS new_validations
  FROM parameters
  WHERE type IN ('DATE', 'DATE_TIME')
)

UPDATE parameters
SET validations = updated_validations.new_validations
FROM updated_validations
WHERE parameters.id = updated_validations.id;



