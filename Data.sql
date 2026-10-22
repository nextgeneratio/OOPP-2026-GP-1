USE university_management;

INSERT INTO departments (department_id, code, name, office_location, is_active) VALUES
                                                                                    (1, 'IT', 'Information Technology', 'ICT Department', TRUE),
                                                                                    (2, 'ET', 'Engineering Technology', 'Engineering Technology Department', TRUE),
                                                                                    (3, 'BST', 'Biosystem Technology', 'Biosystem Technology Department', TRUE);

INSERT INTO batches (batch_id, department_id, intake_year, programme, batch_code) VALUES
                                                                                      (1, 1, 2024, 'Bachelor of Information and Communication Technology', 'IT-2024'),
                                                                                      (2, 1, 2025, 'Bachelor of Information and Communication Technology', 'IT-2025'),
                                                                                      (3, 2, 2024, 'Bachelor of Engineering Technology', 'ET-2024'),
                                                                                      (4, 2, 2025, 'Bachelor of Engineering Technology', 'ET-2025'),
                                                                                      (5, 3, 2024, 'Bachelor of Biosystems Technology', 'BST-2024');

-- 5 sample users for each role. Password hashes below are demo placeholders only.
INSERT INTO users (user_id, username, password_hash, role, email, is_active, created_at) VALUES
                                                                                             (1, 'admin01', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'ADMIN', 'admin01@example.com', TRUE, '2026-01-01 09:00:00'),
                                                                                             (2, 'admin02', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'ADMIN', 'admin02@example.com', TRUE, '2026-01-01 09:05:00'),
                                                                                             (3, 'admin03', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'ADMIN', 'admin03@example.com', TRUE, '2026-01-01 09:10:00'),
                                                                                             (4, 'admin04', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'ADMIN', 'admin04@example.com', TRUE, '2026-01-01 09:15:00'),
                                                                                             (5, 'admin05', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'ADMIN', 'admin05@example.com', TRUE, '2026-01-01 09:20:00'),
                                                                                             (6, 'lecturer01', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'LECTURER', 'lecturer01@example.com', TRUE, '2026-01-02 09:00:00'),
                                                                                             (7, 'lecturer02', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'LECTURER', 'lecturer02@example.com', TRUE, '2026-01-02 09:05:00'),
                                                                                             (8, 'lecturer03', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'LECTURER', 'lecturer03@example.com', TRUE, '2026-01-02 09:10:00'),
                                                                                             (9, 'lecturer04', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'LECTURER', 'lecturer04@example.com', TRUE, '2026-01-02 09:15:00'),
                                                                                             (10, 'lecturer05', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'LECTURER', 'lecturer05@example.com', TRUE, '2026-01-02 09:20:00'),
                                                                                             (11, 'officer01', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'TECHNICAL_OFFICER', 'officer01@example.com', TRUE, '2026-01-03 09:00:00'),
                                                                                             (12, 'officer02', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'TECHNICAL_OFFICER', 'officer02@example.com', TRUE, '2026-01-03 09:05:00'),
                                                                                             (13, 'officer03', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'TECHNICAL_OFFICER', 'officer03@example.com', TRUE, '2026-01-03 09:10:00'),
                                                                                             (14, 'officer04', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'TECHNICAL_OFFICER', 'officer04@example.com', TRUE, '2026-01-03 09:15:00'),
                                                                                             (15, 'officer05', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'TECHNICAL_OFFICER', 'officer05@example.com', TRUE, '2026-01-03 09:20:00'),
                                                                                             (16, 'student01', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'UNDERGRADUATE', 'student01@example.com', TRUE, '2026-01-04 09:00:00'),
                                                                                             (17, 'student02', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'UNDERGRADUATE', 'student02@example.com', TRUE, '2026-01-04 09:05:00'),
                                                                                             (18, 'student03', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'UNDERGRADUATE', 'student03@example.com', TRUE, '2026-01-04 09:10:00'),
                                                                                             (19, 'student04', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'UNDERGRADUATE', 'student04@example.com', TRUE, '2026-01-04 09:15:00'),
                                                                                             (20, 'student05', '$2y$10$sampleDemoHashReplaceBeforeUse000000000000000000000000000', 'UNDERGRADUATE', 'student05@example.com', TRUE, '2026-01-04 09:20:00');

INSERT INTO admins (admin_id, user_id, full_name, phone) VALUES
                                                             (1, 1, 'Admin One', '0771000001'), (2, 2, 'Admin Two', '0771000002'),
                                                             (3, 3, 'Admin Three', '0771000003'), (4, 4, 'Admin Four', '0771000004'),
                                                             (5, 5, 'Admin Five', '0771000005');

INSERT INTO lecturers (lecturer_id, user_id, department_id, full_name, phone, designation) VALUES
                                                                                               (1, 6, 1, 'Nimal Perera', '0712000001', 'Lecturer'),
                                                                                               (2, 7, 1, 'Kamal Silva', '0712000002', 'Senior Lecturer'),
                                                                                               (3, 8, 2, 'Sunil Fernando', '0712000003', 'Lecturer'),
                                                                                               (4, 9, 2, 'Amara Jayasinghe', '0712000004', 'Lecturer'),
                                                                                               (5, 10, 3, 'Saman Kumara', '0712000005', 'Lecturer');

INSERT INTO technical_officers (officer_id, user_id, department_id, full_name, phone, designation) VALUES
                                                                                                       (1, 11, 1, 'Ruwan Officer', '0723000001', 'Technical Officer'),
                                                                                                       (2, 12, 1, 'Chathuri Officer', '0723000002', 'Technical Officer'),
                                                                                                       (3, 13, 2, 'Thilina Officer', '0723000003', 'Technical Officer'),
                                                                                                       (4, 14, 2, 'Dilani Officer', '0723000004', 'Technical Officer'),
                                                                                                       (5, 15, 3, 'Pradeep Officer', '0723000005', 'Technical Officer');

INSERT INTO undergraduates (student_id, user_id, batch_id, student_no, full_name, profile_picture) VALUES
                                                                                                       (1, 16, 1, 'IT2024-001', 'Student One', NULL),
                                                                                                       (2, 17, 2, 'IT2025-002', 'Student Two', NULL),
                                                                                                       (3, 18, 3, 'ET2024-003', 'Student Three', NULL),
                                                                                                       (4, 19, 4, 'ET2025-004', 'Student Four', NULL),
                                                                                                       (5, 20, 5, 'BST2024-005', 'Student Five', NULL);