-- liquibase formatted sql

-- changeset siba:202408201717-seed-data-1
-- comment: fix task execution timers completed with exception when task is paused

UPDATE task_execution_timers tet
SET resumed_at = te.ended_at
FROM task_executions te
WHERE tet.task_executions_id = te.id
  AND tet.reason IS NOT NULL
  AND tet.resumed_at IS NULL
  AND tet.reason <> 'TASK_COMPLETED'
  AND te.state IN ('COMPLETED', 'COMPLETED_WITH_EXCEPTION', 'SKIPPED');

