-- liquibase formatted sql

--changeset siba:202311220018-data-1
--comment : add id in property filters

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


UPDATE parameters
SET data = jsonb_set(
  data,
  '{propertyFilters,fields}',
  (SELECT jsonb_agg(
            elem || jsonb_build_object('id', uuid_generate_v4())
          )
   FROM jsonb_array_elements(data -> 'propertyFilters' -> 'fields') AS elem)
           )
WHERE type = 'RESOURCE'
  and data ->> 'propertyFilters' != '{}';

--changeset siba:202311220018-data-2
--comment : add id in property validations

UPDATE parameters
SET data = jsonb_set(
  data,
  '{propertyValidations}',
  (SELECT jsonb_agg(
              elem || jsonb_build_object('id', uuid_generate_v4())
          )
   FROM jsonb_array_elements(data -> 'propertyValidations') AS elem)
           )
WHERE type = 'RESOURCE'
  and data ->> 'propertyValidations' != '[]';

--changeset siba:202311220018-data-3
--comment : add id in should be


UPDATE parameters
SET data = jsonb_set(
  data,
  '{id}',
  to_jsonb(uuid_generate_v4())
           )
WHERE type = 'SHOULD_BE';

--changeset siba:202311220018-data-4
--comment : add id in number validations

UPDATE parameters
SET validations = jsonb_set(
  validations,
  '{resourceParameterValidations}',
  (SELECT jsonb_agg(
              elem || jsonb_build_object('id', uuid_generate_v4())
          )
   FROM jsonb_array_elements(validations -> 'resourceParameterValidations') AS elem)
           )
WHERE validations ->> 'resourceParameterValidations' != '[]';
