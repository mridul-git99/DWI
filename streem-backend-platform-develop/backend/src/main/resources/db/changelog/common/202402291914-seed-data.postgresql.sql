-- liquibase formatted sql

--changeset siba:202402291914-seed-data-1
--comment: set create job entry in job audits table to lowest id for that job

update job_audits ja
set id = (select (min(id) - 1) from job_audits jai where jai.jobs_id = ja.jobs_id)
where  details ilike '%created the Job%'
