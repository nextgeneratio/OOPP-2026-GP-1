-- Demo data for trying out the screens. Run AFTER schema.sql and seed.sql, on university_management.
-- Safe to run again: it clears only the tables it fills (users and academic master data are untouched).
-- Login password for every seeded user after this script: Ams@Dev2026

USE `university_management`;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE `attendance_records`;
TRUNCATE TABLE `attendance_sessions`;
TRUNCATE TABLE `marks`;
TRUNCATE TABLE `assessments`;
TRUNCATE TABLE `medical_records`;
TRUNCATE TABLE `course_results`;
TRUNCATE TABLE `timetable_entries`;
TRUNCATE TABLE `timetables`;
TRUNCATE TABLE `notices`;
SET FOREIGN_KEY_CHECKS = 1;

-- Remove the historical (finished) semester this script adds, so the script can be run again.
DELETE FROM `course_enrollments` WHERE enrollment_id > 22;
DELETE FROM `course_offering_lecturers` WHERE offering_id > 2;
DELETE FROM `course_offerings` WHERE offering_id > 2;
DELETE FROM `course_components` WHERE course_id > 2;
DELETE FROM `courses` WHERE course_id > 2;
DELETE FROM `semesters` WHERE semester_id > 1;

-- Timetable (department 1, semester 1): offering 1 = OOP Practicum, offering 2 = Database Systems.
INSERT INTO `timetables` (timetable_id, title, department_id, semester_id, level_no) VALUES
    (1, 'ICT Semester 1 Timetable', 1, 1, 1);
INSERT INTO `timetable_entries` (timetable_entry_id, timetable_id, offering_id, component_id, day_of_week, start_time, end_time, location) VALUES
    (1, 1, 1, 1, 'MON', '08:00:00', '10:00:00', 'Lecture Hall 1'),
    (2, 1, 1, 2, 'WED', '13:00:00', '16:00:00', 'ICT Lab 1'),
    (3, 1, 2, 3, 'TUE', '10:00:00', '12:00:00', 'Lecture Hall 2'),
    (4, 1, 2, 4, 'THU', '13:00:00', '16:00:00', 'ICT Lab 2');

-- Notices: two should NOT show (expired, and not yet published).
INSERT INTO `notices` (title, body, audience, target_role, target_batch_id, publish_date, expiry_date) VALUES
    ('Welcome to Semester 1', 'Lectures for 2026/2027 Semester 1 are under way. Check the timetable for rooms and times.',
        'ALL', NULL, NULL, NOW() - INTERVAL 20 DAY, NULL),
    ('Laboratory safety briefing', 'All undergraduates must attend the laboratory safety briefing before their first practical.',
        'ROLE', 'UNDERGRADUATE', NULL, NOW() - INTERVAL 2 DAY, NOW() + INTERVAL 30 DAY),
    ('FOT-ICT-2025 batch meeting', 'Batch meeting with the coordinator in Lecture Hall 1 on Friday at 2 pm.',
        'BATCH', NULL, 1, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 14 DAY),
    ('Marks submission reminder', 'Please submit continuous assessment marks before the end of the month.',
        'ROLE', 'LECTURER', NULL, NOW() - INTERVAL 3 DAY, NOW() + INTERVAL 20 DAY),
    ('Equipment inventory check', 'Technical officers: complete the laboratory equipment check this week.',
        'ROLE', 'TECHNICAL_OFFICER', NULL, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 10 DAY),
    ('Old registration notice (expired)', 'This notice has expired and should not be listed.',
        'ALL', NULL, NULL, NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 10 DAY),
    ('Upcoming holiday (not yet published)', 'This notice is scheduled for the future and should not be listed yet.',
        'ALL', NULL, NULL, NOW() + INTERVAL 10 DAY, NULL);

-- Assessments (weights add up to 100 per offering).
INSERT INTO `assessments` (assessment_id, offering_id, name, assessment_type, weight_percent, is_ca_component) VALUES
    (1, 1, 'Quiz 1', 'QUIZ', 10.00, TRUE),
    (2, 1, 'Assignment 1', 'ASSIGNMENT', 20.00, TRUE),
    (3, 1, 'Mid Exam', 'MID', 20.00, TRUE),
    (4, 1, 'Final Exam', 'FINAL', 50.00, FALSE),
    (5, 2, 'Quiz 1', 'QUIZ', 20.00, TRUE),
    (6, 2, 'Project', 'ASSIGNMENT', 30.00, TRUE),
    (7, 2, 'Final Exam', 'FINAL', 50.00, FALSE);

-- CA marks for offering 1 (all 20 students, entered by lecturer 1); the final exam is left empty on purpose.
INSERT INTO `marks` (assessment_id, enrollment_id, score, entered_by)
SELECT a.assessment_id, e.enrollment_id, 40 + ((e.enrollment_id * 7 + a.assessment_id * 13) MOD 55), 1
FROM `course_enrollments` e
JOIN `assessments` a ON a.offering_id = e.offering_id
WHERE e.offering_id = 1 AND a.assessment_id IN (1, 2, 3);

-- CA marks for the two offering-2 enrolments (entered by lecturer 2).
INSERT INTO `marks` (assessment_id, enrollment_id, score, entered_by) VALUES
    (5, 21, 72.00, 2), (6, 21, 65.50, 2),
    (5, 22, 81.00, 2), (6, 22, 78.00, 2);

-- Attendance: 15 theory sessions (component 1) and 15 practical sessions (component 2) for offering 1, recorded by officer 1.
INSERT INTO `attendance_sessions` (offering_id, component_id, session_date, timetable_entry_id, recorded_by)
SELECT 1, 1, DATE_ADD('2026-09-02', INTERVAL 3 * n.k DAY), 1, 1 FROM (
    SELECT 0 AS k UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) AS n;
INSERT INTO `attendance_sessions` (offering_id, component_id, session_date, timetable_entry_id, recorded_by)
SELECT 1, 2, DATE_ADD('2026-09-03', INTERVAL 3 * n.k DAY), 2, 1 FROM (
    SELECT 0 AS k UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) AS n;

-- Presence pattern: students 3 and 9 attend rarely (not eligible); student 5 attends exactly 11 of 15 (73.33%);
-- everyone else misses at most two sessions (eligible).
INSERT INTO `attendance_records` (session_id, enrollment_id, is_present)
SELECT s.session_id, e.enrollment_id,
    CASE
        WHEN e.student_id IN (3, 9) AND (s.k MOD 4) <> 0 THEN 0
        WHEN e.student_id = 5 AND s.k IN (0, 3, 6, 9) THEN 0
        WHEN e.student_id NOT IN (3, 5, 9) AND ((e.student_id + s.k) MOD 9) = 0 THEN 0
        ELSE 1
    END
FROM (
    SELECT session_id, offering_id, DATEDIFF(session_date, IF(component_id = 1, '2026-09-02', '2026-09-03')) DIV 3 AS k
    FROM `attendance_sessions`
) AS s
JOIN `course_enrollments` e ON e.offering_id = s.offering_id AND e.enrollment_status = 'ACTIVE';

-- Medical records for offering 1: one approved, one pending, one rejected.
INSERT INTO `medical_records` (student_id, offering_id, medical_date, start_date, end_date, reason_details,
                               evidence_reference, approval_status, affects_eligibility) VALUES
    (3, 1, '2026-09-20', '2026-09-18', '2026-09-22', 'Fever; physician advised five days of rest.',
        'evidence/medical-stu003.pdf', 'APPROVED', TRUE),
    (9, 1, '2026-10-05', '2026-10-03', '2026-10-06', 'Viral infection; rest advised.',
        'evidence/medical-stu009.pdf', 'PENDING', FALSE),
    (5, 1, '2026-09-30', '2026-09-28', '2026-09-29', 'Stomach upset.', NULL, 'REJECTED', FALSE);

-- Finished semester (2025/2026 Semester 2) so Grades & GPA has results. The app only lists results for
-- enrolments that are COMPLETED, so these enrolments are completed; the current semester stays ACTIVE.
INSERT INTO `semesters` (semester_id, academic_year, semester_no, start_date, end_date) VALUES
    (2, '2025/2026', 2, '2026-02-02', '2026-06-30');
INSERT INTO `courses` (course_id, department_id, course_code, title, total_credits) VALUES
    (3, 1, 'ICT1113', 'Programming Fundamentals', 3.0),
    (4, 1, 'ICT1123', 'Web Technologies', 3.0);
INSERT INTO `course_components` (component_id, course_id, component_type, credits, planned_sessions) VALUES
    (5, 3, 'THEORY', 2.0, 15), (6, 3, 'PRACTICAL', 1.0, 15),
    (7, 4, 'THEORY', 2.0, 15), (8, 4, 'PRACTICAL', 1.0, 15);
INSERT INTO `course_offerings` (offering_id, course_id, batch_id, semester_id, coordinator_id, status) VALUES
    (3, 3, 1, 2, 3, 'CLOSED'), (4, 4, 1, 2, 4, 'CLOSED');
INSERT INTO `course_offering_lecturers` (offering_id, lecturer_id) VALUES (3, 3), (4, 4);

-- Students 1-8 completed both courses (enrolment ids 23-30 for offering 3, 31-38 for offering 4).
INSERT INTO `course_enrollments` (enrollment_id, student_id, offering_id, attempt_no, student_status, enrollment_status)
SELECT 22 + student_id, student_id, 3, 1, 'NORMAL', 'COMPLETED' FROM `undergraduates` WHERE student_id BETWEEN 1 AND 8;
INSERT INTO `course_enrollments` (enrollment_id, student_id, offering_id, attempt_no, student_status, enrollment_status)
SELECT 30 + student_id, student_id, 4, 1, 'NORMAL', 'COMPLETED' FROM `undergraduates` WHERE student_id BETWEEN 1 AND 8;

-- Final marks 55-80; the grade and grade point come from the UGC-2024-v1 scheme.
INSERT INTO `course_results` (enrollment_id, final_mark, scheme_version, grade_letter, grade_point,
                              credits_counted, completed_at, recorded_by, is_current)
SELECT m.enrollment_id, m.final_mark, g.scheme_version, g.grade_letter, g.grade_point, 3.0,
       '2026-07-15 10:00:00', m.lecturer_id, TRUE
FROM (
    SELECT enrollment_id,
           CASE WHEN offering_id = 3 THEN 52 + 3 * student_id ELSE 48 + 4 * student_id END AS final_mark,
           CASE WHEN offering_id = 3 THEN 3 ELSE 4 END AS lecturer_id
    FROM `course_enrollments` WHERE enrollment_id > 22
) AS m
JOIN `grades` g ON g.scheme_version = 'UGC-2024-v1' AND m.final_mark BETWEEN g.min_mark AND g.max_mark;

-- Password for every user: Ams@Dev2026
UPDATE `users`
SET password_hash = 'PBKDF2$210000$bSvhPh4jS9PfxuyCxmwZ6Q==$hIdi0a3I3Uv1mi6aPh4IWafSnXR/SbhlyGPhb66NWZo=',
    is_active = TRUE;
