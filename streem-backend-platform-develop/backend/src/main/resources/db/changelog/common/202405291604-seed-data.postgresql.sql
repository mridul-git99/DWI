-- liquibase formatted sql

--changeset nishant:202405291604-seed-data
--comment: added column exception_approval_type in parameters table and removed enable_exception_approval


ALTER TABLE parameters
ADD COLUMN exception_approval_type varchar(50) NOT NULL DEFAULT 'NONE';

-- Update the new column based on the value of enable_exception_approval
UPDATE parameters
SET exception_approval_type = CASE
    WHEN enable_exception_approval = TRUE THEN 'APPROVER_REVIEWER_FLOW'
    ELSE 'NONE'
END;

ALTER TABLE parameters
DROP COLUMN enable_exception_approval;

--changeset nishant:202405291604-seed-data-2


ALTER TABLE exceptions
ADD COLUMN reason varchar(255) DEFAULT NULL;
