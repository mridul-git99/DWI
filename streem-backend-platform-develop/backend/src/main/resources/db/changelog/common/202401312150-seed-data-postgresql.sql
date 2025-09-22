--changeset Nishant:202401312150-seed-data-1
-- Update the blocked jobs state to in progress


UPDATE jobs
SET state = 'IN_PROGRESS'
where state = 'BLOCKED';
