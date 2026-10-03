USE `university_management`;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `audit_logs`;
TRUNCATE TABLE `grades`;
TRUNCATE TABLE `attendance_records`;
TRUNCATE TABLE `attendance_sessions`;
TRUNCATE TABLE `medical_records`;
TRUNCATE TABLE `marks`;
TRUNCATE TABLE `assessments`;
TRUNCATE TABLE `course_enrollments`;
TRUNCATE TABLE `course_materials`;
TRUNCATE TABLE `timetable_entries`;
TRUNCATE TABLE `timetables`;
TRUNCATE TABLE `course_offerings`;
TRUNCATE TABLE `course_components`;
TRUNCATE TABLE `courses`;
TRUNCATE TABLE `semesters`;
TRUNCATE TABLE `undergraduates`;
TRUNCATE TABLE `lecturers`;
TRUNCATE TABLE `technical_officers`;
TRUNCATE TABLE `admins`;
TRUNCATE TABLE `batches`;
TRUNCATE TABLE `departments`;
TRUNCATE TABLE `users`;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `departments` (department_id, code, name, office_location) VALUES
	(1, 'ICT', 'Information and Communication Technology', 'Technology Building');

INSERT INTO `batches` (batch_id, department_id, intake_year, programme, batch_code) VALUES
	(1, 1, 2025, 'Bachelor of Information and Communication Technology', 'FOT-ICT-2025');

INSERT INTO `semesters` (semester_id, academic_year, semester_no, start_date, end_date) VALUES
	(1, '2026/2027', 1, '2026-09-01', '2027-01-31');

INSERT INTO `users` (user_id, username, password_hash, role, email, created_at) VALUES
	(1, 'ADM001', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'ADMIN', 'adm001@example.edu', NOW()),
	(2, 'LEC001', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'LECTURER', 'lec001@example.edu', NOW()),
	(3, 'LEC002', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'LECTURER', 'lec002@example.edu', NOW()),
	(4, 'LEC003', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'LECTURER', 'lec003@example.edu', NOW()),
	(5, 'LEC004', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'LECTURER', 'lec004@example.edu', NOW()),
	(6, 'LEC005', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'LECTURER', 'lec005@example.edu', NOW()),
	(7, 'TO001', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'TECHNICAL_OFFICER', 'to001@example.edu', NOW()),
	(8, 'TO002', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'TECHNICAL_OFFICER', 'to002@example.edu', NOW()),
	(9, 'TO003', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'TECHNICAL_OFFICER', 'to003@example.edu', NOW()),
	(10, 'TO004', 'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=', 'TECHNICAL_OFFICER', 'to004@example.edu', NOW());

INSERT INTO `users` (user_id, username, password_hash, role, email, created_at)
SELECT 10 + sequence_no,
	   CONCAT('STU', LPAD(sequence_no, 3, '0')),
	   'PBKDF2$210000$16hClb3Y3vZenhG1M1npKA==$WpOYYvegPT4KC2qPJ7/zjICjB8Be2pxl36vDO2Khl9Q=',
	   'UNDERGRADUATE',
	   CONCAT('stu', LPAD(sequence_no, 3, '0'), '@example.edu'),
	   NOW()
FROM (
	SELECT 1 AS sequence_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
	UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
	UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
	UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) AS student_numbers;

INSERT INTO `admins` (admin_id, user_id, full_name, phone) VALUES
	(1, 1, 'System Administrator', '+94110000001');

INSERT INTO `lecturers` (lecturer_id, user_id, department_id, full_name, phone, designation) VALUES
	(1, 2, 1, 'Lecturer 001', '+94110000002', 'Senior Lecturer'),
	(2, 3, 1, 'Lecturer 002', '+94110000003', 'Lecturer'),
	(3, 4, 1, 'Lecturer 003', '+94110000004', 'Lecturer'),
	(4, 5, 1, 'Lecturer 004', '+94110000005', 'Lecturer'),
	(5, 6, 1, 'Lecturer 005', '+94110000006', 'Lecturer');

INSERT INTO `technical_officers` (officer_id, user_id, department_id, full_name, phone, designation) VALUES
	(1, 7, 1, 'Technical Officer 001', '+94110000007', 'Technical Officer'),
	(2, 8, 1, 'Technical Officer 002', '+94110000008', 'Technical Officer'),
	(3, 9, 1, 'Technical Officer 003', '+94110000009', 'Technical Officer'),
	(4, 10, 1, 'Technical Officer 004', '+94110000010', 'Technical Officer');

INSERT INTO `undergraduates` (student_id, user_id, batch_id, student_no, full_name) VALUES
	(1, 11, 1, 'STU001', 'Undergraduate 001'), (2, 12, 1, 'STU002', 'Undergraduate 002'),
	(3, 13, 1, 'STU003', 'Undergraduate 003'), (4, 14, 1, 'STU004', 'Undergraduate 004'),
	(5, 15, 1, 'STU005', 'Undergraduate 005'), (6, 16, 1, 'STU006', 'Undergraduate 006'),
	(7, 17, 1, 'STU007', 'Undergraduate 007'), (8, 18, 1, 'STU008', 'Undergraduate 008'),
	(9, 19, 1, 'STU009', 'Undergraduate 009'), (10, 20, 1, 'STU010', 'Undergraduate 010'),
	(11, 21, 1, 'STU011', 'Undergraduate 011'), (12, 22, 1, 'STU012', 'Undergraduate 012'),
	(13, 23, 1, 'STU013', 'Undergraduate 013'), (14, 24, 1, 'STU014', 'Undergraduate 014'),
	(15, 25, 1, 'STU015', 'Undergraduate 015'), (16, 26, 1, 'STU016', 'Undergraduate 016'),
	(17, 27, 1, 'STU017', 'Undergraduate 017'), (18, 28, 1, 'STU018', 'Undergraduate 018'),
	(19, 29, 1, 'STU019', 'Undergraduate 019'), (20, 30, 1, 'STU020', 'Undergraduate 020');

INSERT INTO `courses` (course_id, department_id, course_code, title, total_credits) VALUES
	(1, 1, 'ICT2132', 'Object Oriented Programming Practicum', 3.0),
	(2, 1, 'ICT2142', 'Database Systems', 3.0);

INSERT INTO `course_components` (component_id, course_id, component_type, credits, planned_sessions) VALUES
	(1, 1, 'THEORY', 2.0, 15), (2, 1, 'PRACTICAL', 1.0, 15),
	(3, 2, 'THEORY', 2.0, 15), (4, 2, 'PRACTICAL', 1.0, 15);

INSERT INTO `course_offerings` (offering_id, course_id, batch_id, semester_id, coordinator_id) VALUES
	(1, 1, 1, 1, 1), (2, 2, 1, 1, 2);

INSERT INTO `course_enrollments` (enrollment_id, student_id, offering_id, attempt_no, student_status, enrollment_status)
SELECT student_id, student_id, 1, 1, 'NORMAL', 'ACTIVE'
FROM `undergraduates`;

INSERT INTO `course_enrollments` (enrollment_id, student_id, offering_id, attempt_no, student_status, enrollment_status) VALUES
	(21, 17, 2, 2, 'REPEAT', 'ACTIVE'),
	(22, 18, 2, 2, 'BATCH_MISSED', 'ACTIVE');

INSERT INTO `grades` (scheme_version, grade_letter, min_mark, max_mark, grade_point) VALUES
	('UGC-2024-v1', 'A+', 85, 100, 4.00), ('UGC-2024-v1', 'A', 80, 84.99, 4.00),
	('UGC-2024-v1', 'A-', 75, 79.99, 3.70), ('UGC-2024-v1', 'B+', 70, 74.99, 3.30),
	('UGC-2024-v1', 'B', 65, 69.99, 3.00), ('UGC-2024-v1', 'B-', 60, 64.99, 2.70),
	('UGC-2024-v1', 'C+', 55, 59.99, 2.30), ('UGC-2024-v1', 'C', 50, 54.99, 2.00),
	('UGC-2024-v1', 'C-', 45, 49.99, 1.70), ('UGC-2024-v1', 'D+', 40, 44.99, 1.30),
	('UGC-2024-v1', 'D', 35, 39.99, 1.00), ('UGC-2024-v1', 'E', 0, 34.99, 0.00);
