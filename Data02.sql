INSERT INTO courses (course_id, department_id, course_code, title, total_credits, is_active) VALUES
                                                                                                 (1, 1, 'ICT1101', 'Introduction to Information Technology', 3.0, TRUE),
                                                                                                 (2, 1, 'ICT1102', 'Database Management Systems', 3.0, TRUE),
                                                                                                 (3, 2, 'ET1101', 'Engineering Fundamentals', 3.0, TRUE),
                                                                                                 (4, 2, 'ET1102', 'Electrical and Electronic Technology', 3.0, TRUE),
                                                                                                 (5, 3, 'BST1101', 'Introduction to Biosystems Technology', 3.0, TRUE);

INSERT INTO course_components (component_id, course_id, component_type, credits, planned_sessions) VALUES
                                                                                                       (1, 1, 'THEORY', 2.0, 15), (2, 2, 'THEORY', 2.0, 15),
                                                                                                       (3, 3, 'PRACTICAL', 1.0, 15), (4, 4, 'PRACTICAL', 1.0, 15),
                                                                                                       (5, 5, 'THEORY', 3.0, 15);

INSERT INTO semesters (semester_id, academic_year, semester_no, start_date, end_date) VALUES
                                                                                          (1, '2024/2025', 1, '2024-10-01', '2025-02-28'),
                                                                                          (2, '2024/2025', 2, '2025-03-15', '2025-07-31'),
                                                                                          (3, '2025/2026', 1, '2025-10-01', '2026-02-28'),
                                                                                          (4, '2025/2026', 2, '2026-03-15', '2026-07-31'),
                                                                                          (5, '2026/2027', 1, '2026-10-01', '2027-02-28');

INSERT INTO course_offerings (offering_id, course_id, batch_id, semester_id, coordinator_id, status) VALUES
                                                                                                         (1, 1, 1, 1, 1, 'OPEN'), (2, 2, 2, 2, 2, 'OPEN'),
                                                                                                         (3, 3, 3, 3, 3, 'OPEN'), (4, 4, 4, 4, 4, 'OPEN'),
                                                                                                         (5, 5, 5, 5, 5, 'OPEN');

INSERT INTO course_offering_lecturers (offering_id, lecturer_id) VALUES
                                                                     (1, 1), (2, 2), (3, 3), (4, 4), (5, 5);

INSERT INTO course_enrollments (enrollment_id, student_id, offering_id, attempt_no, student_status, enrollment_status) VALUES
                                                                                                                           (1, 1, 1, 1, 'NORMAL', 'COMPLETED'),
                                                                                                                           (2, 2, 2, 1, 'NORMAL', 'COMPLETED'),
                                                                                                                           (3, 3, 3, 1, 'NORMAL', 'COMPLETED'),
                                                                                                                           (4, 4, 4, 1, 'NORMAL', 'COMPLETED'),
                                                                                                                           (5, 5, 5, 1, 'NORMAL', 'COMPLETED');

INSERT INTO assessments (assessment_id, offering_id, name, assessment_type, weight_percent, is_ca_component) VALUES
                                                                                                                 (1, 1, 'Assignment 1', 'ASSIGNMENT', 20.00, TRUE),
                                                                                                                 (2, 2, 'Quiz 1', 'QUIZ', 20.00, TRUE),
                                                                                                                 (3, 3, 'Practical Test 1', 'PRACTICAL', 25.00, TRUE),
                                                                                                                 (4, 4, 'Mid Semester Test', 'MID', 30.00, TRUE),
                                                                                                                 (5, 5, 'Final Examination', 'FINAL', 60.00, FALSE);

INSERT INTO marks (mark_id, assessment_id, enrollment_id, score, entered_by) VALUES
                                                                                 (1, 1, 1, 85.00, 1), (2, 2, 2, 78.00, 2),
                                                                                 (3, 3, 3, 67.00, 3), (4, 4, 4, 59.00, 4),
                                                                                 (5, 5, 5, 91.00, 5);
