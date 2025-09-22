-- liquibase formatted sql
--changeset siba:202405201118-seed-data-1
--comment: update enable_exception_approval to true for should be parameters when the parameter is not started state

WITH UpdatedParameters AS (
  SELECT
    id,
    'NUMBER'::text AS new_type,
    data as shouldBeData,
    jsonb_build_object(
      'validationType', 'CRITERIA',
      'criteriaValidations', jsonb_agg(
        jsonb_build_object(
          'id', data->>'id',
          'value', data->>'value',
          'operator', data->>'operator',
          'lowerValue', data->>'lowerValue',
          'upperValue', data->>'upperValue',
          'criteriaType', 'CONSTANT',
          'errorMessage', CASE
                            WHEN data->>'operator' = 'EQUAL_TO' THEN label || ' should not be equal to ' || (data->>'value') || '. The value entered doesn''t satisfy this criteria.'
                            WHEN data->>'operator' = 'LESS_THAN' THEN label || ' should be less than ' || (data->>'value') || '. The value entered doesn''t satisfy this criteria.'
                            WHEN data->>'operator' = 'LESS_THAN_EQUAL_TO' THEN label || ' should be less than or equal to ' || (data->>'value') || '. The value entered doesn''t satisfy this criteria.'
                            WHEN data->>'operator' = 'MORE_THAN' THEN label || ' should be greater than ' || (data->>'value') || '. The value entered doesn''t satisfy this criteria.'
                            WHEN data->>'operator' = 'MORE_THAN_EQUAL_TO' THEN label || ' should be greater than or equal to ' || (data->>'value') || '. The value entered doesn''t satisfy this criteria.'
                            WHEN data->>'operator' = 'BETWEEN' THEN label || ' should be between ' || (data->>'lowerValue') || ' and ' || (data->>'upperValue') || '. The value entered doesn''t satisfy this criteria.'
                            ELSE 'Invalid operator'
            END,
          'valueParameterId', NULL,
          'lowerValueParameterId', NULL,
          'upperValueParameterId', NULL,
          'uom', data->>'uom'
        )
                             ),
      'resourceParameterValidations', '[]'::jsonb
    ) AS new_validations
  FROM parameters
  WHERE type = 'SHOULD_BE'
  GROUP BY id, label, data
)
UPDATE parameters p
SET
  type = UpdatedParameters.new_type,
  validations = UpdatedParameters.new_validations,
  enable_exception_approval = true,
  data = CASE
           WHEN shouldBeData::text ilike '%leastCount%'THEN jsonb_build_object('leastCount', shouldBeData->'leastCount')
           ELSE jsonb_set('{}'::jsonb, '{leastCount}', 'null'::jsonb)
    END
FROM UpdatedParameters
WHERE p.id = UpdatedParameters.id;
