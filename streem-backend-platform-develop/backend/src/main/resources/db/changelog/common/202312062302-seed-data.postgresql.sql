-- liquibase formatted sql

--changeset siba:202312062302-seed-data-1
--comment Add parameterValueId in calculation parameter choices of parameter_values table of calculation parameter
WITH updated_values AS (SELECT pv1.id as pv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   old_choices,
                                   '{parameterValueId}',
                                   to_jsonb(
                                     (SELECT pv2.id::text
                                      FROM parameter_values pv2
                                      WHERE old_choices ->> 'parameterId' = pv2.parameters_id::text
                                        AND pv1.jobs_id = pv2.jobs_id
                                      LIMIT 1))
                                 )
                               )      AS new_choices
                        FROM parameter_values pv1
                               INNER JOIN parameters p on p.id = pv1.parameters_id and p.type = 'CALCULATION'
                               CROSS JOIN jsonb_array_elements(pv1.choices) as old_choices
                        WHERE p.type = 'CALCULATION'
                        GROUP BY pv1.id)
UPDATE parameter_values
SET choices = updated_values.new_choices
FROM updated_values
WHERE parameter_values.id = updated_values.pv1_id
  and (choices != '[]' or choices is not null);


--changeset siba:202312062302-seed-data-2
--comment add parameterValueId in calculation parameter choices of temp_parameter_values of calculation parameter
WITH updated_values AS (SELECT tpv1.id as tpv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   old_choices,
                                   '{parameterValueId}',
                                   to_jsonb(
                                     (SELECT tpv2.id::text
                                      FROM temp_parameter_values tpv2
                                      WHERE old_choices ->> 'parameterId' = tpv2.parameters_id::text
                                        AND tpv1.jobs_id = tpv2.jobs_id
                                      LIMIT 1))
                                 )
                               )      AS new_choices
                        FROM temp_parameter_values tpv1
                               INNER JOIN parameters p on p.id = tpv1.parameters_id and p.type = 'CALCULATION'
                               CROSS JOIN jsonb_array_elements(tpv1.choices) as old_choices
                        WHERE p.type = 'CALCULATION'
                        GROUP BY tpv1.id)
UPDATE temp_parameter_values
SET choices = updated_values.new_choices
FROM updated_values
WHERE temp_parameter_values.id = updated_values.tpv1_id
  and (choices != '[]' or choices is not null);

--changeset siba:202312062302-seed-data-3
--comment add taskExecutionId in calculation parameter choices of parameter_values table of calculation parameter

WITH updated_values AS (SELECT pv1.id as pv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   old_choices,
                                   '{taskExecutionId}',
                                   to_jsonb(
                                     (SELECT pv2.task_executions_id::text
                                      FROM parameter_values pv2
                                      WHERE old_choices ->> 'parameterId' = pv2.parameters_id::text
                                        AND pv1.jobs_id = pv2.jobs_id
                                      LIMIT 1))
                                 )
                               )      AS new_choices
                        FROM parameter_values pv1
                               INNER JOIN parameters p on p.id = pv1.parameters_id and p.type = 'CALCULATION'
                               CROSS JOIN jsonb_array_elements(pv1.choices) as old_choices
                        WHERE p.type = 'CALCULATION'
                        GROUP BY pv1.id)
UPDATE parameter_values
SET choices = updated_values.new_choices
FROM updated_values
WHERE parameter_values.id = updated_values.pv1_id
  and (choices != '[]' or choices is not null);


-- changeset siba:202312062302-seed-data-4
-- comment add taskExecutionId in calculation parameter choices of temp_parameter_values table of calculation parameter

WITH updated_values AS (SELECT tpv1.id as tpv1_id,
                               jsonb_agg(
                                 jsonb_set(
                                   old_choices,
                                   '{taskExecutionId}',
                                   to_jsonb(
                                     (SELECT tpv2.task_executions_id::text
                                      FROM temp_parameter_values tpv2
                                      WHERE old_choices ->> 'parameterId' = tpv2.parameters_id::text
                                        AND tpv1.jobs_id = tpv2.jobs_id
                                      LIMIT 1))
                                 )
                               )      AS new_choices
                        FROM temp_parameter_values tpv1
                               INNER JOIN parameters p on p.id = tpv1.parameters_id and p.type = 'CALCULATION'
                               CROSS JOIN jsonb_array_elements(tpv1.choices) as old_choices
                        WHERE p.type = 'CALCULATION'
                        GROUP BY tpv1.id)
UPDATE temp_parameter_values
SET choices = updated_values.new_choices
FROM updated_values
WHERE temp_parameter_values.id = updated_values.tpv1_id
  and (choices != '[]' or choices is not null);
