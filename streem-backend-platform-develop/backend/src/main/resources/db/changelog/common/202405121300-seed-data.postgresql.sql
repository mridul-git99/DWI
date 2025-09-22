-- liquibase formatted sql

--changeset siba:202405121300-seed-data-1
--comment: delete all duplicate entries of timers where reason is TASK_COMPLETED

DELETE
FROM task_execution_timers teto
  USING (SELECT id
         FROM (SELECT id,
                      ROW_NUMBER() OVER (PARTITION BY task_executions_id ORDER BY created_at DESC) AS row_num
               FROM task_execution_timers
               WHERE reason = 'TASK_COMPLETED') ranked
         WHERE ranked.row_num > 1) teti
WHERE teto.id = teti.id;

