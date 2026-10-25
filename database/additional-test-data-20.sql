-- Additive test data for the existing university_management database.
-- Safe to run more than once: all inserted business keys use TEST2026 prefixes.
-- It does not delete or modify existing records.
USE university_management;

-- Twenty demo courses and their offerings in the existing ICT batch/current semester.
INSERT INTO courses (department_id, course_code, title, total_credits, is_active)
SELECT d.department_id, CONCAT('TEST2026', LPAD(n.n, 2, '0')),
       CONCAT('Integration Test Course ', LPAD(n.n, 2, '0')), 3.0, TRUE
FROM departments d
CROSS JOIN (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
            UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
            UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
            UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) n
WHERE d.code = 'ICT'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.course_code = CONCAT('TEST2026', LPAD(n.n, 2, '0')));

INSERT INTO course_components (course_id, component_type, credits, planned_sessions)
SELECT c.course_id, 'THEORY', 3.0, 15 FROM courses c
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM course_components cc WHERE cc.course_id = c.course_id AND cc.component_type = 'THEORY');

INSERT INTO course_offerings (course_id, batch_id, semester_id, coordinator_id, status)
SELECT c.course_id, b.batch_id, s.semester_id,
       (SELECT lecturer_id FROM lecturers ORDER BY lecturer_id LIMIT 1), 'OPEN'
FROM courses c JOIN departments d ON d.department_id = c.department_id
JOIN batches b ON b.department_id = d.department_id
JOIN semesters s ON s.academic_year = '2026/2027' AND s.semester_no = 1
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM course_offerings o WHERE o.course_id = c.course_id AND o.batch_id = b.batch_id AND o.semester_id = s.semester_id);

INSERT IGNORE INTO course_offering_lecturers (offering_id, lecturer_id)
SELECT o.offering_id, (SELECT lecturer_id FROM lecturers ORDER BY lecturer_id LIMIT 1)
FROM course_offerings o JOIN courses c ON c.course_id = o.course_id WHERE c.course_code LIKE 'TEST2026%';

-- Twenty students, paired with the twenty offerings. Password hashes deliberately reuse an existing test-account hash.
INSERT INTO users (username, password_hash, role, email, is_active, created_at)
SELECT CONCAT('teststudent', LPAD(n.n, 2, '0')), (SELECT password_hash FROM users ORDER BY user_id LIMIT 1),
       'UNDERGRADUATE', CONCAT('teststudent', LPAD(n.n, 2, '0'), '@fot.test'), TRUE, NOW()
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
      UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) n
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.username = CONCAT('teststudent', LPAD(n.n, 2, '0')));

INSERT INTO undergraduates (user_id, batch_id, student_no, full_name)
SELECT u.user_id, (SELECT batch_id FROM batches WHERE batch_code = 'FOT-ICT-2025' LIMIT 1),
       CONCAT('TEST/2026/', LPAD(CAST(SUBSTRING(u.username, 12) AS UNSIGNED), 3, '0')),
       CONCAT('Test Student ', SUBSTRING(u.username, 12))
FROM users u WHERE u.username LIKE 'teststudent%'
  AND NOT EXISTS (SELECT 1 FROM undergraduates s WHERE s.user_id = u.user_id);

INSERT INTO course_enrollments (student_id, offering_id, attempt_no, student_status, enrollment_status)
SELECT s.student_id, o.offering_id, 1, 'NORMAL', 'ACTIVE'
FROM undergraduates s JOIN courses c ON c.course_code = CONCAT('TEST2026', LPAD(CAST(SUBSTRING(s.student_no, 11) AS UNSIGNED), 2, '0'))
JOIN course_offerings o ON o.course_id = c.course_id
WHERE s.student_no LIKE 'TEST/2026/%'
  AND NOT EXISTS (SELECT 1 FROM course_enrollments e WHERE e.student_id = s.student_id AND e.offering_id = o.offering_id AND e.attempt_no = 1);

-- One CA assessment, mark, session, and attendance record per new offering/enrolment.
INSERT INTO assessments (offering_id, name, assessment_type, weight_percent, is_ca_component)
SELECT o.offering_id, 'Demo CA', 'CA', 100.00, TRUE FROM course_offerings o
JOIN courses c ON c.course_id = o.course_id WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM assessments a WHERE a.offering_id = o.offering_id AND a.name = 'Demo CA');

INSERT INTO marks (assessment_id, enrollment_id, score, entered_by)
SELECT a.assessment_id, e.enrollment_id, 65.00, (SELECT lecturer_id FROM lecturers ORDER BY lecturer_id LIMIT 1)
FROM assessments a JOIN course_offerings o ON o.offering_id = a.offering_id
JOIN courses c ON c.course_id = o.course_id JOIN course_enrollments e ON e.offering_id = o.offering_id
WHERE c.course_code LIKE 'TEST2026%' AND a.name = 'Demo CA'
  AND NOT EXISTS (SELECT 1 FROM marks m WHERE m.assessment_id = a.assessment_id AND m.enrollment_id = e.enrollment_id);

INSERT INTO attendance_sessions (offering_id, component_id, session_date, timetable_entry_id, recorded_by)
SELECT o.offering_id, cc.component_id, DATE_ADD('2026-10-01', INTERVAL CAST(SUBSTRING(c.course_code, 9) AS UNSIGNED) DAY), NULL,
       (SELECT officer_id FROM technical_officers ORDER BY officer_id LIMIT 1)
FROM course_offerings o JOIN courses c ON c.course_id = o.course_id JOIN course_components cc ON cc.course_id = c.course_id AND cc.component_type = 'THEORY'
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM attendance_sessions x WHERE x.offering_id = o.offering_id AND x.component_id = cc.component_id AND x.session_date = DATE_ADD('2026-10-01', INTERVAL CAST(SUBSTRING(c.course_code, 9) AS UNSIGNED) DAY));

INSERT INTO attendance_records (session_id, enrollment_id, is_present)
SELECT x.session_id, e.enrollment_id, TRUE FROM attendance_sessions x
JOIN course_enrollments e ON e.offering_id = x.offering_id JOIN course_offerings o ON o.offering_id = x.offering_id JOIN courses c ON c.course_id = o.course_id
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM attendance_records r WHERE r.session_id = x.session_id AND r.enrollment_id = e.enrollment_id);

INSERT INTO course_materials (offering_id, lecturer_id, title, file_path, is_visible)
SELECT o.offering_id, (SELECT lecturer_id FROM lecturers ORDER BY lecturer_id LIMIT 1),
       CONCAT('Demo material ', c.course_code), CONCAT('uploads/materials/offering-', o.offering_id, '/demo-', c.course_code, '.txt'), TRUE
FROM course_offerings o JOIN courses c ON c.course_id = o.course_id WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM course_materials m WHERE m.offering_id = o.offering_id AND m.title = CONCAT('Demo material ', c.course_code));

INSERT INTO timetable_entries (timetable_id, offering_id, component_id, day_of_week, start_time, end_time, location)
SELECT (SELECT t.timetable_id FROM timetables t WHERE t.department_id = 1 AND t.semester_id = 1 AND t.level_no = 1 ORDER BY t.timetable_id LIMIT 1),
       o.offering_id, cc.component_id, 'SAT', '08:00:00', '10:00:00', CONCAT('TEST-LAB-', c.course_code)
FROM course_offerings o JOIN courses c ON c.course_id = o.course_id JOIN course_components cc ON cc.course_id = c.course_id AND cc.component_type = 'THEORY'
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM timetable_entries te WHERE te.offering_id = o.offering_id AND te.component_id = cc.component_id AND te.location = CONCAT('TEST-LAB-', c.course_code));

-- Twenty medicals and notices for UI testing.
INSERT INTO medical_records (student_id, offering_id, medical_date, start_date, end_date, reason_details, approval_status, affects_eligibility)
SELECT e.student_id, e.offering_id, '2026-10-10', '2026-10-10', '2026-10-10', 'Demo medical record', 'APPROVED', TRUE
FROM course_enrollments e JOIN course_offerings o ON o.offering_id = e.offering_id JOIN courses c ON c.course_id = o.course_id
WHERE c.course_code LIKE 'TEST2026%'
  AND NOT EXISTS (SELECT 1 FROM medical_records m WHERE m.student_id = e.student_id AND m.offering_id = e.offering_id AND m.reason_details = 'Demo medical record');

INSERT INTO notices (title, body, audience, publish_date, expiry_date)
SELECT CONCAT('Test notice ', LPAD(n, 2, '0')), 'Additive test notice for user-interface testing.', 'ALL', NOW(), DATE_ADD(NOW(), INTERVAL 60 DAY)
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) n
WHERE NOT EXISTS (SELECT 1 FROM notices x WHERE x.title = CONCAT('Test notice ', LPAD(n, 2, '0')));
