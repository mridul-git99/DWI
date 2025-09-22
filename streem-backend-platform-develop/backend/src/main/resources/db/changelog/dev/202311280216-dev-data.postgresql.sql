-- liquibase formatted sql

-- changeset mokshesh:202311280216-dev-data-01
-- comment add users
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115950, 1701115952, 1701115950, 1617625401, 'L-global.admin.01', 'ga.01@mailinator.com', 'Kalita', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115951, 1701115951, 1701115951, 1617625401, 'L-global.admin.02', 'ga.02@mailinator.com', 'Shantanu', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115952, 1701115952, 1701115952, 1617625401, 'L-global.admin.03', 'ga.03@mailinator.com', 'Sarvesh', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115953, 1701115953, 1701115953, 1617625401, 'L-global.admin.04', 'ga.04@mailinator.com', 'Priti', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115954, 1701115954, 1701115954, 1617625401, 'L-global.admin.05', 'ga.05@mailinator.com', 'Lilavati', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115955, 1701115955, 1701115955, 1617625401, 'L-global.admin.06', 'ga.06@mailinator.com', 'Mina', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115956, 1701115956, 1701115956, 1617625401, 'L-global.admin.07', 'ga.07@mailinator.com', 'Richa', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115957, 1701115957, 1701115957, 1617625401, 'L-global.admin.08', 'ga.08@mailinator.com', 'Sarthak', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115958, 1701115958, 1701115958, 1617625401, 'L-global.admin.09', 'ga.09@mailinator.com', 'Nilam', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115959, 1701115959, 1701115959, 1617625401, 'L-global.admin.10', 'ga.10@mailinator.com', 'Prasanna', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115960, 1701115962, 1701115960, 1617625401, 'L-process.publisher.01', 'pp.01@mailinator.com', 'Aarush', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115961, 1701115961, 1701115961, 1617625401, 'L-process.publisher.02', 'pp.02@mailinator.com', 'Deepak', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115962, 1701115962, 1701115962, 1617625401, 'L-process.publisher.03', 'pp.03@mailinator.com', 'Malini', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115963, 1701115963, 1701115963, 1617625401, 'L-process.publisher.04', 'pp.04@mailinator.com', 'Banhi', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115964, 1701115964, 1701115964, 1617625401, 'L-process.publisher.05', 'pp.05@mailinator.com', 'Shekhar', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115965, 1701115965, 1701115965, 1617625401, 'L-process.publisher.06', 'pp.06@mailinator.com', 'Isha', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115966, 1701115966, 1701115966, 1617625401, 'L-process.publisher.07', 'pp.07@mailinator.com', 'Sarita', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115967, 1701115967, 1701115967, 1617625401, 'L-process.publisher.08', 'pp.08@mailinator.com', 'Lakshmi', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115968, 1701115968, 1701115968, 1617625401, 'L-process.publisher.09', 'pp.09@mailinator.com', 'Namrata', false, 'L');
INSERT INTO public.users (id, created_at, modified_at, organisations_id, employee_id, email, first_name, archived, last_name) VALUES(1701115969, 1701115969, 1701115969, 1617625401, 'L-process.publisher.10', 'pp.10@mailinator.com', 'Kiran', false, 'L');

-- changeset mokshesh:202311280216-dev-data-02
-- comment remove old checklist
DELETE FROM checklist_collaborator_comments WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM checklist_collaborator_mapping WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM parameter_media_mapping WHERE parameters_id IN (SELECT id FROM parameters WHERE tasks_id IN (SELECT id FROM tasks WHERE stages_id IN (SELECT id FROM stages WHERE checklists_id IN (1620069400, 1620069501))));
DELETE FROM parameter_values where parameters_id in (select id from parameters where checklists_id in (1620069400, 1620069501));
DELETE FROM parameters WHERE tasks_id IN (SELECT id FROM tasks WHERE stages_id IN (SELECT id FROM stages WHERE checklists_id IN (1620069400, 1620069501)));
DELETE FROM parameters WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM task_media_mapping WHERE tasks_id IN (SELECT id FROM tasks WHERE stages_id IN (SELECT id FROM stages WHERE checklists_id IN (1620069400, 1620069501)));
DELETE FROM task_executions where tasks_id in (select t.id from tasks t inner join public.stages s on s.id = t.stages_id where s.checklists_id in (1620069400, 1620069501));
DELETE FROM tasks WHERE stages_id IN (SELECT id FROM stages WHERE checklists_id IN (1620069400, 1620069501));
DELETE FROM stages WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM checklist_property_values WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM checklist_facility_mapping WHERE checklists_id IN (1620069400, 1620069501);
DELETE FROM jobs where checklists_id in (1620069400, 1620069501);
DELETE FROM checklists WHERE id IN (1620069400, 1620069501);
DELETE FROM versions WHERE "self" IN (1620069400, 1620069501);

-- changeset mokshesh:202311280216-dev-data-03
-- comment remove Leucine-II
UPDATE facilities SET organisations_id = 1617625401 WHERE organisations_id = 1643103402;
UPDATE users SET organisations_id = 1617625401 WHERE organisations_id = 1643103402;
DELETE FROM organisations WHERE id = 1643103402;
