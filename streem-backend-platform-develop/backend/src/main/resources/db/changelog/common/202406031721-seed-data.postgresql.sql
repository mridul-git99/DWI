-- liquibase formatted sql

--changeset peesa:202406031721-seed-data
--comment: Remove label from all calculation type parameters data

update parameters p
set "data" = jsonb_set(
    "data",
    '{variables}',
    (
        select jsonb_object_agg(key, value - 'label')
        from jsonb_each("data" -> 'variables')
    )
)
where p."type" = 'CALCULATION'
