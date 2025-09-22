-- liquibase formatted sql

--changeset siba :202409172122-seed-data-1
--comment: convert jsonb object of validations into array and add exceptionApprovalType

ALTER TABLE exceptions
ADD COLUMN rules_id TEXT;


UPDATE parameters
SET validations = '{}'::jsonb
WHERE
  validations IS NOT NULL
  AND JSONB_TYPEOF(validations) = 'object'
  AND validations <> '{}'::jsonb
  AND type = 'NUMBER'
  AND COALESCE(NULLIF(validations->'customValidations', 'null'::jsonb), '[]'::jsonb) = '[]'::jsonb
  AND COALESCE(NULLIF(validations->'relationPropertyValidations', 'null'::jsonb), '[]'::jsonb) = '[]'::jsonb
  AND COALESCE(NULLIF(validations->'resourceParameterValidations', 'null'::jsonb), '[]'::jsonb) = '[]'::jsonb
  AND COALESCE(NULLIF(validations->'criteriaValidations', 'null'::jsonb),'[]'::jsonb) = '[]'::jsonb;


WITH generated_uuid AS (
  -- Generate the new ruleId and get the related parameterId
  SELECT uuid_generate_v4()::TEXT AS new_rule_id, id AS parameterId
  FROM parameters
  WHERE JSONB_TYPEOF(validations) = 'object'
    AND validations <> '{}'::jsonb
    AND type = 'NUMBER'
),
     updated_parameters AS (
       -- Update the parameters table with the generated UUID
       UPDATE parameters
         SET validations = JSONB_SET(
           JSONB_SET(
           CASE
           WHEN validations->>'validationType' IS NULL THEN
           JSONB_SET(validations, '{validationType}', '"RESOURCE"'::jsonb)
           ELSE validations
           END,
           '{exceptionApprovalType}',
           TO_JSONB(CASE
           WHEN exception_approval_type= 'NONE' THEN '"DEFAULT_FLOW"'::jsonb
           ELSE TO_JSONB(exception_approval_type)
           END)
           ),
           '{ruleId}', TO_JSONB(generated_uuid.new_rule_id)
         )
         FROM generated_uuid
         WHERE id = generated_uuid.parameterId
         RETURNING generated_uuid.parameterId, generated_uuid.new_rule_id
     )
-- Update the exceptions table with the new ruleId for matching parameter_values_id
UPDATE exceptions e
SET rules_id = up.new_rule_id
  FROM updated_parameters up, parameter_values pv
WHERE e.parameter_values_id = pv.id AND pv.parameters_id = up.parameterId;

UPDATE parameters
SET validations =
      CASE
        WHEN validations = '{}'::jsonb THEN '[]'::jsonb
        ELSE JSONB_BUILD_ARRAY(validations)
        END
WHERE JSONB_TYPEOF(validations) = 'object';



WITH updated_parameters AS (
  SELECT
    id,
    jsonb_set(
      data,
      '{propertyValidations}',
      (
        SELECT jsonb_agg(
                 jsonb_build_object(
                   'ruleId', uuid_generate_v4()::text,
                   'propertyValidations', jsonb_build_array(pv_element),
                   'exceptionApprovalType', 'DEFAULT_FLOW'
                 )
               )
        FROM jsonb_array_elements(data->'propertyValidations') AS pv_element
      )
    ) AS updated_data
  FROM parameters
  WHERE type IN ('RESOURCE', 'MULTI_RESOURCE')
    AND data->>'propertyValidations' IS NOT NULL
  AND data->'propertyValidations' != '[]'::jsonb
  )
UPDATE parameters
SET
  data = updated_parameters.updated_data,
  validations = updated_parameters.updated_data->'propertyValidations'
  FROM updated_parameters
WHERE parameters.id = updated_parameters.id;



UPDATE parameters
SET data = data - 'propertyValidations'
WHERE type IN ('RESOURCE', 'MULTI_RESOURCE')
  AND jsonb_exists(data, 'propertyValidations');


ALTER TABLE parameters
DROP COLUMN exception_approval_type;
