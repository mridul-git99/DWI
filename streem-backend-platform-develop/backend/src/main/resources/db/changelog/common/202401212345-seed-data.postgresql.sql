-- liquibase formatted sql

-- changeset siba:202401212345-seed-data-1

DROP INDEX IF EXISTS task_execution_user_mapping_task_executions_id_idx;
DROP INDEX IF EXISTS task_execution_user_mapping_users_id_idx;
DROP INDEX IF EXISTS task_executions_jobs_id_idx;
DROP INDEX IF EXISTS task_executions_tasks_id_idx;
DROP INDEX IF EXISTS activity_values_activities_id_idx;
DROP INDEX IF EXISTS activity_values_jobs_id_idx;
DROP INDEX IF EXISTS activity_value_media_mapping_activity_values_id_idx;
DROP INDEX IF EXISTS tasks_stages_id_idx;

CREATE INDEX IF NOT EXISTS idxd57bf5c28c064ae289f4148f ON task_execution_user_mapping (task_executions_id DESC);
CREATE INDEX IF NOT EXISTS idx2f04ad719017474d84d2147f ON task_execution_user_mapping (users_id);
CREATE INDEX IF NOT EXISTS idxf43796c5066e43a9801a191d ON task_executions (jobs_id DESC);
CREATE INDEX IF NOT EXISTS idx2e8386b34271485087cee3bf ON task_executions (tasks_id);
CREATE INDEX IF NOT EXISTS idx1dd1365bb6e9408ab6e7b1b0 ON parameter_values (parameters_id);
CREATE INDEX IF NOT EXISTS idx3567337e529d4b10bf1df804 ON parameter_values (jobs_id DESC);
CREATE INDEX IF NOT EXISTS idx1f6943827bd84f78993c79f0 ON parameter_value_media_mapping (parameter_values_id DESC);

CREATE INDEX IF NOT EXISTS idx132468d6e0404caa9ce0e56c ON tasks (stages_id);
CREATE INDEX IF NOT EXISTS idx214b5b2503514ccd9fc0c0d6 ON stages (checklists_id);
CREATE INDEX IF NOT EXISTS idxb90a74e43e5a4d1db3ec5011 ON parameters (tasks_id);
CREATE INDEX IF NOT EXISTS idxc8b6b31403a64696a80c4656 ON task_automation_mapping (tasks_id);
CREATE INDEX IF NOT EXISTS idx587debfd99a34fa58784d7ed ON task_automation_mapping (automations_id);
CREATE INDEX IF NOT EXISTS idxb72231f6da6344c09b0cde50 ON task_media_mapping (tasks_id);
CREATE INDEX IF NOT EXISTS idx92a47f1537a8483ba1c71848 ON task_media_mapping (medias_id);
