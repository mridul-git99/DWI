
-- liquibase formatted sql

--changeset abhishek:202504291525-seed-data-1

UPDATE public.tasks
SET
  max_period = NULL,
  min_period  = CASE
                  WHEN min_period = 0 THEN NULL
                  ELSE min_period
                END
WHERE max_period = 0;

