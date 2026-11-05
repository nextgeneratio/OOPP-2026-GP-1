-- Faculty of Technology AMS — complete local demonstration installer.
-- DESTRUCTIVE: after schema preflight, this replaces ALL rows in all 30 application tables.
-- Back up an existing university_management database before running this file.
-- DDL (database/user/tables/helper routine) may implicitly commit; only the data replacement is transactional.
-- Run with a database administrator account using the MariaDB/MySQL command-line client.
-- The default JDBC password below is a PUBLIC LOCAL DEMO VALUE; change it for non-demo deployments.
CREATE DATABASE IF NOT EXISTS `university_management`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `university_management`;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dedicated local application account. This explicitly resets its password and grants only CRUD on this schema.
CREATE USER IF NOT EXISTS 'ams_app'@'localhost' IDENTIFIED BY 'AmsLocalOnly-ChangeMe-2026!';
ALTER USER 'ams_app'@'localhost' IDENTIFIED BY 'AmsLocalOnly-ChangeMe-2026!';
REVOKE ALL PRIVILEGES, GRANT OPTION FROM 'ams_app'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON `university_management`.* TO 'ams_app'@'localhost';

CREATE TABLE IF NOT EXISTS `users` (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM(
        'ADMIN',
        'LECTURER',
        'TECHNICAL_OFFICER',
        'UNDERGRADUATE'
    ) NOT NULL,
    email VARCHAR(120) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,

    PRIMARY KEY (user_id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admins` (
    admin_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,

    PRIMARY KEY (admin_id),
    UNIQUE KEY uk_admins_user_id (user_id),

    CONSTRAINT fk_admins_user
        FOREIGN KEY (user_id)
        REFERENCES `users` (user_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `departments` (
    department_id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(15) NOT NULL,
    name VARCHAR(120) NOT NULL,
    office_location VARCHAR(80) NULL,
    is_active BOOLEAN DEFAULT TRUE,

    PRIMARY KEY (department_id),
    UNIQUE KEY uk_departments_code (code),
    CONSTRAINT chk_departments_code CHECK (code IN ('ICT', 'BST', 'ET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `batches` (
    batch_id INT NOT NULL AUTO_INCREMENT,
    department_id INT NOT NULL,
    intake_year YEAR NOT NULL,
    programme VARCHAR(100) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,
    level_no TINYINT NOT NULL DEFAULT 1,

    PRIMARY KEY (batch_id),
    UNIQUE KEY uk_batches_batch_code (batch_code),

    CONSTRAINT chk_batches_level_no
        CHECK (level_no BETWEEN 1 AND 4),

    CONSTRAINT fk_batches_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `undergraduates` (
    student_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    batch_id INT NOT NULL,
    student_no VARCHAR(25) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    profile_picture VARCHAR(255) NULL,

    PRIMARY KEY (student_id),
    UNIQUE KEY uk_undergraduates_user_id (user_id),
    UNIQUE KEY uk_undergraduates_student_no (student_no),
    INDEX idx_undergraduates_batch_id (batch_id),

    CONSTRAINT fk_undergraduates_user
        FOREIGN KEY (user_id)
        REFERENCES `users` (user_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_undergraduates_batch
        FOREIGN KEY (batch_id)
        REFERENCES `batches` (batch_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `lecturers` (
    lecturer_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id INT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    designation VARCHAR(80) NULL,

    PRIMARY KEY (lecturer_id),
    UNIQUE KEY uk_lecturers_user_id (user_id),
    INDEX idx_lecturers_department_id (department_id),

    CONSTRAINT fk_lecturers_user
        FOREIGN KEY (user_id)
        REFERENCES `users` (user_id),

    CONSTRAINT fk_lecturers_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `technical_officers` (
    officer_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id INT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    designation VARCHAR(80) NULL,

    PRIMARY KEY (officer_id),
    UNIQUE KEY uk_technical_officers_user_id (user_id),
    INDEX idx_technical_officers_department_id (department_id),

    CONSTRAINT fk_technical_officers_user
        FOREIGN KEY (user_id)
        REFERENCES `users` (user_id),

    CONSTRAINT fk_technical_officers_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `courses` (
    course_id INT NOT NULL AUTO_INCREMENT,
    department_id INT NOT NULL,
    course_code VARCHAR(20) NOT NULL,
    title VARCHAR(180) NOT NULL,
    total_credits DECIMAL(3,1) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,

    PRIMARY KEY (course_id),
    UNIQUE KEY uk_courses_course_code (course_code),
    INDEX idx_courses_department_id (department_id),

    CONSTRAINT chk_courses_total_credits
        CHECK (total_credits > 0),

    CONSTRAINT fk_courses_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_components` (
    component_id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    component_type ENUM('THEORY', 'PRACTICAL') NOT NULL,
    credits DECIMAL(3,1) NOT NULL,
    planned_sessions INT NOT NULL DEFAULT 15,

    PRIMARY KEY (component_id),
    INDEX idx_course_components_course_id (course_id),
    UNIQUE KEY uk_course_components_course_type (course_id, component_type),

    CONSTRAINT chk_course_components_credits
        CHECK (credits > 0),

    CONSTRAINT chk_course_components_planned_sessions
        CHECK (planned_sessions > 0),

    CONSTRAINT fk_course_components_course
        FOREIGN KEY (course_id)
        REFERENCES `courses` (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `semesters` (
    semester_id INT NOT NULL AUTO_INCREMENT,
    academic_year VARCHAR(9) NOT NULL,
    semester_no TINYINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    PRIMARY KEY (semester_id),
    UNIQUE KEY uk_semesters_academic_year_no (academic_year, semester_no),

    CONSTRAINT chk_semesters_semester_no
        CHECK (semester_no IN (1, 2)),

    CONSTRAINT chk_semesters_date_range
        CHECK (end_date > start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_offerings` (
    offering_id INT NOT NULL AUTO_INCREMENT,
    course_id INT NOT NULL,
    batch_id INT NOT NULL,
    semester_id INT NOT NULL,
    coordinator_id INT NULL,
    status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',

    PRIMARY KEY (offering_id),
    UNIQUE KEY uk_course_offerings_instance (
        course_id,
        batch_id,
        semester_id
    ),

    CONSTRAINT fk_course_offerings_course
        FOREIGN KEY (course_id)
        REFERENCES `courses` (course_id),

    CONSTRAINT fk_course_offerings_batch
        FOREIGN KEY (batch_id)
        REFERENCES `batches` (batch_id),

    CONSTRAINT fk_course_offerings_semester
        FOREIGN KEY (semester_id)
        REFERENCES `semesters` (semester_id),

    CONSTRAINT fk_course_offerings_coordinator
        FOREIGN KEY (coordinator_id)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_offering_lecturers` (
    offering_id INT NOT NULL,
    lecturer_id INT NOT NULL,

    PRIMARY KEY (offering_id, lecturer_id),
    UNIQUE KEY uk_course_offering_lecturers_one_lecturer (offering_id),
    INDEX idx_course_offering_lecturers_lecturer_id (lecturer_id),

    CONSTRAINT fk_course_offering_lecturers_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_offering_lecturers_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_enrollments` (
    enrollment_id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    offering_id INT NOT NULL,
    attempt_no TINYINT NOT NULL DEFAULT 1,
    student_status ENUM('NORMAL', 'REPEAT', 'BATCH_MISSED') NOT NULL,
    enrollment_status ENUM('ACTIVE', 'COMPLETED', 'WITHDRAWN') NOT NULL,
    active_offering_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN enrollment_status = 'ACTIVE' THEN 1 ELSE NULL END) STORED,

    PRIMARY KEY (enrollment_id),
    UNIQUE KEY uk_course_enrollments_attempt (
        student_id,
        offering_id,
        attempt_no
    ),
    UNIQUE KEY uk_course_enrollments_one_active_offering (
        student_id,
        offering_id,
        active_offering_guard
    ),
    INDEX idx_course_enrollments_student_id (student_id),
    INDEX idx_course_enrollments_offering_id (offering_id),

    CONSTRAINT fk_course_enrollments_student
        FOREIGN KEY (student_id)
        REFERENCES `undergraduates` (student_id),

    CONSTRAINT fk_course_enrollments_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `assessments` (
    assessment_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    assessment_type ENUM(
        'ASSIGNMENT',
        'QUIZ',
        'MID',
        'PRACTICAL',
        'CA',
        'FINAL',
        'OTHER'
    ) NOT NULL,
    weight_percent DECIMAL(5,2) NOT NULL,
    is_ca_component BOOLEAN NOT NULL,

    PRIMARY KEY (assessment_id),
    UNIQUE KEY uk_assessments_offering_name (offering_id, name),
    INDEX idx_assessments_offering_id (offering_id),

    CONSTRAINT chk_assessments_weight_percent
        CHECK (weight_percent BETWEEN 0 AND 100),

    CONSTRAINT fk_assessments_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `marks` (
    mark_id INT NOT NULL AUTO_INCREMENT,
    assessment_id INT NOT NULL,
    enrollment_id INT NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    entered_by INT NOT NULL,

    PRIMARY KEY (mark_id),
    UNIQUE KEY uk_marks_assessment_enrollment (
        assessment_id,
        enrollment_id
    ),

    CONSTRAINT chk_marks_score
        CHECK (score BETWEEN 0 AND 100),

    CONSTRAINT fk_marks_assessment
        FOREIGN KEY (assessment_id)
        REFERENCES `assessments` (assessment_id),

    CONSTRAINT fk_marks_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES `course_enrollments` (enrollment_id),

    CONSTRAINT fk_marks_entered_by
        FOREIGN KEY (entered_by)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attendance_sessions` (
    session_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    component_id INT NOT NULL,
    session_date DATE NOT NULL,
    timetable_entry_id INT NULL,
    recorded_by INT NOT NULL,
    recorded_by_lecturer INT NULL,

    PRIMARY KEY (session_id),
    UNIQUE KEY uk_attendance_sessions_offering_component_date (
        offering_id,
        component_id,
        session_date
    ),
    INDEX idx_attendance_sessions_offering_id (offering_id),

    CONSTRAINT fk_attendance_sessions_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_attendance_sessions_component
        FOREIGN KEY (component_id)
        REFERENCES `course_components` (component_id),

    CONSTRAINT fk_attendance_sessions_recorded_by
        FOREIGN KEY (recorded_by)
        REFERENCES `technical_officers` (officer_id),

    CONSTRAINT fk_attendance_sessions_recorded_by_lecturer
        FOREIGN KEY (recorded_by_lecturer)
        REFERENCES `lecturers` (lecturer_id),

    CONSTRAINT chk_attendance_sessions_technical_officer_only
        CHECK (recorded_by IS NOT NULL AND recorded_by_lecturer IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `attendance_records` (
    attendance_id INT NOT NULL AUTO_INCREMENT,
    session_id INT NOT NULL,
    enrollment_id INT NOT NULL,
    is_present BOOLEAN NOT NULL,

    PRIMARY KEY (attendance_id),
    UNIQUE KEY uk_attendance_records_session_enrollment (
        session_id,
        enrollment_id
    ),
    INDEX idx_attendance_records_enrollment_id (enrollment_id),

    CONSTRAINT fk_attendance_records_session
        FOREIGN KEY (session_id)
        REFERENCES `attendance_sessions` (session_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_attendance_records_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES `course_enrollments` (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `medical_records` (
    medical_id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    offering_id INT NULL,
    medical_date DATE NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason_details VARCHAR(500) NOT NULL,
    evidence_reference VARCHAR(255) NULL,
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED')
        NOT NULL DEFAULT 'PENDING',
    affects_eligibility BOOLEAN NOT NULL DEFAULT FALSE,
    rejection_reason VARCHAR(500) NULL,

    PRIMARY KEY (medical_id),
    INDEX idx_medical_records_student_id (student_id),
    INDEX idx_medical_records_approval_status (approval_status),

    CONSTRAINT chk_medical_records_date_range
        CHECK (end_date >= start_date),

    CONSTRAINT fk_medical_records_student
        FOREIGN KEY (student_id)
        REFERENCES undergraduates(student_id),

    CONSTRAINT fk_medical_records_offering
        FOREIGN KEY (offering_id)
        REFERENCES course_offerings(offering_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_materials` (
    material_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    lecturer_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (material_id),
    INDEX idx_course_materials_offering_id (offering_id),
    INDEX idx_course_materials_lecturer_id (lecturer_id),

    CONSTRAINT fk_course_materials_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_course_materials_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_quizzes` (
    quiz_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    lecturer_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT FALSE,
    opens_at DATETIME NOT NULL,
    closes_at DATETIME NOT NULL,
    PRIMARY KEY (quiz_id),
    INDEX idx_course_quizzes_offering_visibility (offering_id, is_visible),
    CONSTRAINT chk_course_quizzes_window CHECK (closes_at > opens_at),
    CONSTRAINT fk_course_quizzes_offering FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),
    CONSTRAINT fk_course_quizzes_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_questions` (
    question_id INT NOT NULL AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    ordinal_no SMALLINT NOT NULL,
    prompt VARCHAR(1000) NOT NULL,
    option_a VARCHAR(500) NOT NULL,
    option_b VARCHAR(500) NOT NULL,
    option_c VARCHAR(500) NOT NULL,
    option_d VARCHAR(500) NOT NULL,
    correct_option TINYINT NOT NULL,
    PRIMARY KEY (question_id),
    UNIQUE KEY uk_quiz_questions_ordinal (quiz_id, ordinal_no),
    CONSTRAINT chk_quiz_questions_correct_option CHECK (correct_option BETWEEN 0 AND 3),
    CONSTRAINT fk_quiz_questions_quiz FOREIGN KEY (quiz_id)
        REFERENCES `course_quizzes` (quiz_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `quiz_attempts` (
    attempt_id BIGINT NOT NULL AUTO_INCREMENT,
    quiz_id INT NOT NULL,
    student_id INT NOT NULL,
    enrollment_id INT NOT NULL,
    score SMALLINT NOT NULL,
    total_questions SMALLINT NOT NULL,
    submitted_at DATETIME NOT NULL,
    PRIMARY KEY (attempt_id),
    UNIQUE KEY uk_quiz_attempts_one_per_student (quiz_id, student_id),
    CONSTRAINT chk_quiz_attempts_score CHECK (score >= 0 AND score <= total_questions),
    CONSTRAINT chk_quiz_attempts_total CHECK (total_questions > 0),
    CONSTRAINT fk_quiz_attempts_quiz FOREIGN KEY (quiz_id)
        REFERENCES `course_quizzes` (quiz_id),
    CONSTRAINT fk_quiz_attempts_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES `course_enrollments` (enrollment_id),
    CONSTRAINT fk_quiz_attempts_student FOREIGN KEY (student_id)
        REFERENCES `undergraduates` (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_assignments` (
    assignment_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    lecturer_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    file_path VARCHAR(255) NULL,
    is_visible BOOLEAN NOT NULL DEFAULT FALSE,
    due_at DATETIME NOT NULL,
    PRIMARY KEY (assignment_id),
    INDEX idx_course_assignments_offering_visibility (offering_id, is_visible),
    CONSTRAINT fk_course_assignments_offering FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),
    CONSTRAINT fk_course_assignments_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `assignment_submissions` (
    submission_id BIGINT NOT NULL AUTO_INCREMENT,
    assignment_id INT NOT NULL,
    enrollment_id INT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    submitted_at DATETIME NOT NULL,
    PRIMARY KEY (submission_id),
    UNIQUE KEY uk_assignment_submissions_one_per_student (assignment_id, enrollment_id),
    CONSTRAINT fk_assignment_submissions_assignment FOREIGN KEY (assignment_id)
        REFERENCES `course_assignments` (assignment_id),
    CONSTRAINT fk_assignment_submissions_enrollment FOREIGN KEY (enrollment_id)
        REFERENCES `course_enrollments` (enrollment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notices` (
    notice_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    audience ENUM('ALL', 'ROLE', 'BATCH') NOT NULL,
    target_role ENUM('ADMIN', 'LECTURER', 'TECHNICAL_OFFICER', 'UNDERGRADUATE') NULL,
    target_batch_id INT NULL,
    publish_date DATETIME NOT NULL,
    expiry_date DATETIME NULL,

    PRIMARY KEY (notice_id),
    INDEX idx_notices_publish_date (publish_date),
    INDEX idx_notices_expiry_date (expiry_date),

    CONSTRAINT chk_notices_expiry_date
        CHECK (expiry_date IS NULL OR expiry_date > publish_date),

    CONSTRAINT chk_notices_audience_target
        CHECK (
            (audience = 'ALL' AND target_role IS NULL AND target_batch_id IS NULL)
            OR (audience = 'ROLE' AND target_role IS NOT NULL AND target_batch_id IS NULL)
            OR (audience = 'BATCH' AND target_role IS NULL AND target_batch_id IS NOT NULL)
        ),

    CONSTRAINT fk_notices_target_batch
        FOREIGN KEY (target_batch_id)
        REFERENCES `batches` (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `timetables` (
    timetable_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    semester_id INT NOT NULL,
    level_no TINYINT NOT NULL DEFAULT 1,
    created_by_user_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    course_offering_id INT NULL,
    active_course_offering_id INT GENERATED ALWAYS AS (CASE WHEN is_active=TRUE THEN course_offering_id ELSE NULL END) STORED,

    PRIMARY KEY (timetable_id),
    UNIQUE KEY uk_timetables_active_offering (active_course_offering_id),
    CONSTRAINT chk_timetables_level_no CHECK (level_no BETWEEN 1 AND 4),

    CONSTRAINT fk_timetables_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id),

    CONSTRAINT fk_timetables_semester
        FOREIGN KEY (semester_id)
        REFERENCES `semesters` (semester_id),

    CONSTRAINT fk_timetables_course_offering
        FOREIGN KEY (course_offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_timetables_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES `users` (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `timetable_entries` (
    timetable_entry_id INT NOT NULL AUTO_INCREMENT,
    timetable_id INT NOT NULL,
    offering_id INT NULL,
    component_id INT NULL,
    day_of_week ENUM(
        'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'
    ) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(80) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    PRIMARY KEY (timetable_entry_id),
    INDEX idx_timetable_entries_timetable_id (timetable_id),
    INDEX idx_timetable_entries_day_of_week (day_of_week),

    CONSTRAINT chk_timetable_entries_time_range
        CHECK (start_time < end_time),

    CONSTRAINT chk_timetable_entries_optional_course_pair
        CHECK ((offering_id IS NULL AND component_id IS NULL) OR (offering_id IS NOT NULL AND component_id IS NOT NULL)),

    CONSTRAINT fk_timetable_entries_timetable
        FOREIGN KEY (timetable_id)
        REFERENCES `timetables` (timetable_id),

    CONSTRAINT fk_timetable_entries_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_timetable_entries_component
        FOREIGN KEY (component_id)
        REFERENCES `course_components` (component_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `grades` (
    grade_id INT NOT NULL AUTO_INCREMENT,
    scheme_version VARCHAR(30) NOT NULL,
    grade_letter VARCHAR(3) NOT NULL,
    min_mark DECIMAL(5,2) NOT NULL,
    max_mark DECIMAL(5,2) NOT NULL,
    grade_point DECIMAL(3,2) NOT NULL,

    PRIMARY KEY (grade_id),
    UNIQUE KEY uk_grades_scheme_letter (
        scheme_version,
        grade_letter
    ),

    CONSTRAINT chk_grades_mark_range
        CHECK (
            min_mark BETWEEN 0 AND 100
            AND max_mark BETWEEN 0 AND 100
            AND min_mark <= max_mark
        ),

    CONSTRAINT chk_grades_grade_point
        CHECK (grade_point BETWEEN 0 AND 4)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `course_results` (
    result_id BIGINT NOT NULL AUTO_INCREMENT,
    enrollment_id INT NOT NULL,
    final_mark DECIMAL(5,2) NOT NULL,
    scheme_version VARCHAR(30) NOT NULL,
    grade_letter VARCHAR(3) NOT NULL,
    grade_point DECIMAL(3,2) NOT NULL,
    credits_counted DECIMAL(3,1) NOT NULL,
    completed_at DATETIME NOT NULL,
    recorded_by INT NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    current_enrollment_guard TINYINT
        GENERATED ALWAYS AS (CASE WHEN is_current = TRUE THEN 1 ELSE NULL END) STORED,

    PRIMARY KEY (result_id),
    UNIQUE KEY uk_course_results_one_current (enrollment_id, current_enrollment_guard),
    INDEX idx_course_results_scheme_version (scheme_version),

    CONSTRAINT chk_course_results_final_mark
        CHECK (final_mark BETWEEN 0 AND 100),

    CONSTRAINT chk_course_results_grade_point
        CHECK (grade_point BETWEEN 0 AND 4),

    CONSTRAINT chk_course_results_credits
        CHECK (credits_counted > 0),

    CONSTRAINT fk_course_results_enrollment
        FOREIGN KEY (enrollment_id)
        REFERENCES `course_enrollments` (enrollment_id),

    CONSTRAINT fk_course_results_grade
        FOREIGN KEY (scheme_version, grade_letter)
        REFERENCES `grades` (scheme_version, grade_letter),

    CONSTRAINT fk_course_results_recorded_by
        FOREIGN KEY (recorded_by)
        REFERENCES `lecturers` (lecturer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_logs` (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    entity_name VARCHAR(80) NOT NULL,
    entity_id BIGINT NOT NULL,
    occurred_at DATETIME NOT NULL,

    PRIMARY KEY (audit_id),
    INDEX idx_audit_logs_user_id (user_id),
    INDEX idx_audit_logs_occurred_at (occurred_at),

    CONSTRAINT fk_audit_logs_user
        FOREIGN KEY (user_id)
        REFERENCES `users` (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Manual replacement of university_management with a clearly synthetic demonstration dataset.
-- Destructive DML only: DELETE FROM application tables. No table/database is dropped or truncated.
-- Verify the selected database and files before running. All 30 tables were read-only inspected as InnoDB.
USE `university_management`;
SELECT DATABASE() AS selected_database, VERSION() AS server_version;
SET @demo_replacement_failed = 0;
SET @demo_replacement_sqlstate = NULL;
SET @demo_replacement_error = NULL;
DELIMITER $$
DROP PROCEDURE IF EXISTS `ams_install_replace_demo_data`$$
CREATE PROCEDURE `ams_install_replace_demo_data`()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  IF DATABASE() IS NULL OR DATABASE() <> 'university_management' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Wrong database selected; expected university_management.';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name IN ('users', 'admins', 'departments', 'batches', 'undergraduates', 'lecturers', 'technical_officers', 'courses', 'course_components', 'semesters', 'course_offerings', 'course_offering_lecturers', 'course_enrollments', 'assessments', 'marks', 'attendance_sessions', 'attendance_records', 'medical_records', 'course_materials', 'course_quizzes', 'quiz_questions', 'quiz_attempts', 'course_assignments', 'assignment_submissions', 'notices', 'timetables', 'timetable_entries', 'grades', 'course_results', 'audit_logs')
        AND engine='InnoDB' AND table_collation='utf8mb4_unicode_ci') <> 30 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema preflight failed: all 30 application tables must exist as InnoDB with utf8mb4_unicode_ci.';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name IN ('users', 'admins', 'departments', 'batches', 'undergraduates', 'lecturers', 'technical_officers', 'courses', 'course_components', 'semesters', 'course_offerings', 'course_offering_lecturers', 'course_enrollments', 'assessments', 'marks', 'attendance_sessions', 'attendance_records', 'medical_records', 'course_materials', 'course_quizzes', 'quiz_questions', 'quiz_attempts', 'course_assignments', 'assignment_submissions', 'notices', 'timetables', 'timetable_entries', 'grades', 'course_results', 'audit_logs')) <> 197
     OR EXISTS (
       SELECT 1 FROM (
      SELECT 'users' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'username' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'password_hash' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'role' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'email' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'is_active' AS column_name
      UNION ALL
      SELECT 'users' AS table_name, 'created_at' AS column_name
      UNION ALL
      SELECT 'admins' AS table_name, 'admin_id' AS column_name
      UNION ALL
      SELECT 'admins' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'admins' AS table_name, 'full_name' AS column_name
      UNION ALL
      SELECT 'admins' AS table_name, 'phone' AS column_name
      UNION ALL
      SELECT 'departments' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'departments' AS table_name, 'code' AS column_name
      UNION ALL
      SELECT 'departments' AS table_name, 'name' AS column_name
      UNION ALL
      SELECT 'departments' AS table_name, 'office_location' AS column_name
      UNION ALL
      SELECT 'departments' AS table_name, 'is_active' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'batch_id' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'intake_year' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'programme' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'batch_code' AS column_name
      UNION ALL
      SELECT 'batches' AS table_name, 'level_no' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'student_id' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'batch_id' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'student_no' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'full_name' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'phone' AS column_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'profile_picture' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'lecturer_id' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'full_name' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'phone' AS column_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'designation' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'officer_id' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'full_name' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'phone' AS column_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'designation' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'course_id' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'course_code' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'total_credits' AS column_name
      UNION ALL
      SELECT 'courses' AS table_name, 'is_active' AS column_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'component_id' AS column_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'course_id' AS column_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'component_type' AS column_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'credits' AS column_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'planned_sessions' AS column_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'semester_id' AS column_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'academic_year' AS column_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'semester_no' AS column_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'start_date' AS column_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'end_date' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'course_id' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'batch_id' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'semester_id' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'coordinator_id' AS column_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'status' AS column_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'lecturer_id' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'student_id' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'attempt_no' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'student_status' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'enrollment_status' AS column_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'active_offering_guard' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'assessment_id' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'name' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'assessment_type' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'weight_percent' AS column_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'is_ca_component' AS column_name
      UNION ALL
      SELECT 'marks' AS table_name, 'mark_id' AS column_name
      UNION ALL
      SELECT 'marks' AS table_name, 'assessment_id' AS column_name
      UNION ALL
      SELECT 'marks' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'marks' AS table_name, 'score' AS column_name
      UNION ALL
      SELECT 'marks' AS table_name, 'entered_by' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'session_id' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'component_id' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'session_date' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'timetable_entry_id' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'recorded_by' AS column_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'recorded_by_lecturer' AS column_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'attendance_id' AS column_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'session_id' AS column_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'is_present' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'medical_id' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'student_id' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'medical_date' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'start_date' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'end_date' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'reason_details' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'evidence_reference' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'approval_status' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'affects_eligibility' AS column_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'rejection_reason' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'material_id' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'lecturer_id' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'file_path' AS column_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'is_visible' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'quiz_id' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'lecturer_id' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'password_hash' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'is_visible' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'opens_at' AS column_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'closes_at' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'question_id' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'quiz_id' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'ordinal_no' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'prompt' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_a' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_b' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_c' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_d' AS column_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'correct_option' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'attempt_id' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'quiz_id' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'student_id' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'score' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'total_questions' AS column_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'submitted_at' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'assignment_id' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'lecturer_id' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'description' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'file_path' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'is_visible' AS column_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'due_at' AS column_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'submission_id' AS column_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'assignment_id' AS column_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'file_path' AS column_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'submitted_at' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'notice_id' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'body' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'audience' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'target_role' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'target_batch_id' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'publish_date' AS column_name
      UNION ALL
      SELECT 'notices' AS table_name, 'expiry_date' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'timetable_id' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'title' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'department_id' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'semester_id' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'level_no' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'created_by_user_id' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'is_active' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'course_offering_id' AS column_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'active_course_offering_id' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'timetable_entry_id' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'timetable_id' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'offering_id' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'component_id' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'day_of_week' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'start_time' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'end_time' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'location' AS column_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'is_active' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_id' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'scheme_version' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_letter' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'min_mark' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'max_mark' AS column_name
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_point' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'result_id' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'enrollment_id' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'final_mark' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'scheme_version' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'grade_letter' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'grade_point' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'credits_counted' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'completed_at' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'recorded_by' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'is_current' AS column_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'current_enrollment_guard' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'audit_id' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'user_id' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'action' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'entity_name' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'entity_id' AS column_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'occurred_at' AS column_name
       ) AS expected
       LEFT JOIN information_schema.columns actual
         ON actual.table_schema=DATABASE()
        AND actual.table_name=expected.table_name
        AND actual.column_name=expected.column_name
       WHERE actual.column_name IS NULL
     ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema preflight failed: application columns do not match the installer schema; no records were deleted.';
  END IF;
  IF EXISTS (
       SELECT 1 FROM (
      SELECT 'users' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'username' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 50 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'password_hash' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'role' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'email' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'is_active' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'users' AS table_name, 'created_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'admins' AS table_name, 'admin_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'admins' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'admins' AS table_name, 'full_name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'admins' AS table_name, 'phone' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'departments' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'departments' AS table_name, 'code' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 15 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'departments' AS table_name, 'name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'departments' AS table_name, 'office_location' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 80 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'departments' AS table_name, 'is_active' AS column_name, 'tinyint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'batch_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'intake_year' AS column_name, 'year' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'programme' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 100 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'batch_code' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'batches' AS table_name, 'level_no' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'student_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'batch_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'student_no' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 25 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'full_name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'phone' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'profile_picture' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'lecturer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'full_name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'phone' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'lecturers' AS table_name, 'designation' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 80 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'officer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'full_name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 120 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'phone' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'designation' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 80 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'course_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'course_code' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 20 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 180 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'total_credits' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 3 AS numeric_precision, 1 AS numeric_scale
      UNION ALL
      SELECT 'courses' AS table_name, 'is_active' AS column_name, 'tinyint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_components' AS table_name, 'component_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_components' AS table_name, 'course_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_components' AS table_name, 'component_type' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_components' AS table_name, 'credits' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 3 AS numeric_precision, 1 AS numeric_scale
      UNION ALL
      SELECT 'course_components' AS table_name, 'planned_sessions' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'semesters' AS table_name, 'semester_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'semesters' AS table_name, 'academic_year' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 9 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'semesters' AS table_name, 'semester_no' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'semesters' AS table_name, 'start_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'semesters' AS table_name, 'end_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'course_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'batch_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'semester_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'coordinator_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'status' AS column_name, 'enum' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'lecturer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'student_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'attempt_no' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'student_status' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'enrollment_status' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'active_offering_guard' AS column_name, 'tinyint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'assessment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 100 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'assessment_type' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'weight_percent' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 5 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'assessments' AS table_name, 'is_ca_component' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'marks' AS table_name, 'mark_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'marks' AS table_name, 'assessment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'marks' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'marks' AS table_name, 'score' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 5 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'marks' AS table_name, 'entered_by' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'session_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'component_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'session_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'timetable_entry_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'recorded_by' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'recorded_by_lecturer' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'attendance_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'session_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'is_present' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'medical_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'student_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'medical_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'start_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'end_date' AS column_name, 'date' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'reason_details' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'evidence_reference' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'approval_status' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'affects_eligibility' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'medical_records' AS table_name, 'rejection_reason' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'material_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'lecturer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 150 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'file_path' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_materials' AS table_name, 'is_visible' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'quiz_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'lecturer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 150 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'password_hash' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'is_visible' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'opens_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'closes_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'question_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'quiz_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'ordinal_no' AS column_name, 'smallint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'prompt' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 1000 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_a' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_b' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_c' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'option_d' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 500 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'correct_option' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'attempt_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'quiz_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'student_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'score' AS column_name, 'smallint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'total_questions' AS column_name, 'smallint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'submitted_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'assignment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'lecturer_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 150 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'description' AS column_name, 'text' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'file_path' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'is_visible' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'due_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'submission_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'assignment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'file_path' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 255 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'submitted_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'notice_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 180 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'body' AS column_name, 'text' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'audience' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'target_role' AS column_name, 'enum' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'target_batch_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'publish_date' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'notices' AS table_name, 'expiry_date' AS column_name, 'datetime' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'timetable_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'title' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 100 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'department_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'semester_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'level_no' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'created_by_user_id' AS column_name, 'bigint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'is_active' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'course_offering_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetables' AS table_name, 'active_course_offering_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'timetable_entry_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'timetable_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'offering_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'component_id' AS column_name, 'int' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'day_of_week' AS column_name, 'enum' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'start_time' AS column_name, 'time' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'end_time' AS column_name, 'time' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'location' AS column_name, 'varchar' AS data_type, 'YES' AS is_nullable, 80 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'is_active' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'scheme_version' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 30 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_letter' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 3 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'min_mark' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 5 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'max_mark' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 5 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'grades' AS table_name, 'grade_point' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 3 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'result_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'enrollment_id' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'final_mark' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 5 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'scheme_version' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 30 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'grade_letter' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 3 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'grade_point' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 3 AS numeric_precision, 2 AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'credits_counted' AS column_name, 'decimal' AS data_type, 'NO' AS is_nullable, NULL AS char_length, 3 AS numeric_precision, 1 AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'completed_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'recorded_by' AS column_name, 'int' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'is_current' AS column_name, 'tinyint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'course_results' AS table_name, 'current_enrollment_guard' AS column_name, 'tinyint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'audit_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'user_id' AS column_name, 'bigint' AS data_type, 'YES' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'action' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 50 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'entity_name' AS column_name, 'varchar' AS data_type, 'NO' AS is_nullable, 80 AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'entity_id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'occurred_at' AS column_name, 'datetime' AS data_type, 'NO' AS is_nullable, NULL AS char_length, NULL AS numeric_precision, NULL AS numeric_scale
       ) AS expected
       LEFT JOIN information_schema.columns actual
         ON actual.table_schema=DATABASE()
        AND actual.table_name=expected.table_name
        AND actual.column_name=expected.column_name
       WHERE actual.column_name IS NULL
          OR LOWER(actual.data_type) <> expected.data_type
          OR actual.is_nullable <> expected.is_nullable
          OR (expected.char_length IS NOT NULL
              AND actual.character_maximum_length <> expected.char_length)
          OR (expected.numeric_precision IS NOT NULL
              AND (actual.numeric_precision <> expected.numeric_precision
                   OR actual.numeric_scale <> expected.numeric_scale))
     ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema preflight failed: application column types or nullability do not match; no records were deleted.';
  END IF;
  IF EXISTS (
       SELECT 1 FROM (
      SELECT 'admins' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'admins' AS table_name, 'uk_admins_user_id' AS index_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'idx_assessments_offering_id' AS index_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'uk_assessments_offering_name' AS index_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'uk_assignment_submissions_one_per_student' AS index_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'idx_attendance_records_enrollment_id' AS index_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'uk_attendance_records_session_enrollment' AS index_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'idx_attendance_sessions_offering_id' AS index_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'uk_attendance_sessions_offering_component_date' AS index_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'idx_audit_logs_occurred_at' AS index_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'idx_audit_logs_user_id' AS index_name
      UNION ALL
      SELECT 'batches' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'batches' AS table_name, 'uk_batches_batch_code' AS index_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'idx_course_assignments_offering_visibility' AS index_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'idx_course_components_course_id' AS index_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'uk_course_components_course_type' AS index_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'idx_course_enrollments_offering_id' AS index_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'idx_course_enrollments_student_id' AS index_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'uk_course_enrollments_attempt' AS index_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'uk_course_enrollments_one_active_offering' AS index_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'idx_course_materials_lecturer_id' AS index_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'idx_course_materials_offering_id' AS index_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'uk_course_offering_lecturers_one_lecturer' AS index_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'idx_course_offering_lecturers_lecturer_id' AS index_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'uk_course_offerings_instance' AS index_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'idx_course_quizzes_offering_visibility' AS index_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'idx_course_results_scheme_version' AS index_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'uk_course_results_one_current' AS index_name
      UNION ALL
      SELECT 'courses' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'courses' AS table_name, 'idx_courses_department_id' AS index_name
      UNION ALL
      SELECT 'courses' AS table_name, 'uk_courses_course_code' AS index_name
      UNION ALL
      SELECT 'departments' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'departments' AS table_name, 'uk_departments_code' AS index_name
      UNION ALL
      SELECT 'grades' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'grades' AS table_name, 'uk_grades_scheme_letter' AS index_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'idx_lecturers_department_id' AS index_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'uk_lecturers_user_id' AS index_name
      UNION ALL
      SELECT 'marks' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'marks' AS table_name, 'uk_marks_assessment_enrollment' AS index_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'idx_medical_records_approval_status' AS index_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'idx_medical_records_student_id' AS index_name
      UNION ALL
      SELECT 'notices' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'notices' AS table_name, 'idx_notices_expiry_date' AS index_name
      UNION ALL
      SELECT 'notices' AS table_name, 'idx_notices_publish_date' AS index_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'uk_quiz_attempts_one_per_student' AS index_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'uk_quiz_questions_ordinal' AS index_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'uk_semesters_academic_year_no' AS index_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'idx_technical_officers_department_id' AS index_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'uk_technical_officers_user_id' AS index_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'idx_timetable_entries_day_of_week' AS index_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'idx_timetable_entries_timetable_id' AS index_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'uk_timetables_active_offering' AS index_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'idx_undergraduates_batch_id' AS index_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'uk_undergraduates_student_no' AS index_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'uk_undergraduates_user_id' AS index_name
      UNION ALL
      SELECT 'users' AS table_name, 'PRIMARY' AS index_name
      UNION ALL
      SELECT 'users' AS table_name, 'idx_users_role' AS index_name
      UNION ALL
      SELECT 'users' AS table_name, 'uk_users_email' AS index_name
      UNION ALL
      SELECT 'users' AS table_name, 'uk_users_username' AS index_name
       ) AS expected
       LEFT JOIN information_schema.statistics actual
         ON actual.table_schema=DATABASE()
        AND actual.table_name=expected.table_name
        AND actual.index_name=expected.index_name
       WHERE actual.index_name IS NULL
     ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema preflight failed: a required application index is missing; no records were deleted.';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.table_constraints
      WHERE constraint_schema=DATABASE() AND table_name IN ('users', 'admins', 'departments', 'batches', 'undergraduates', 'lecturers', 'technical_officers', 'courses', 'course_components', 'semesters', 'course_offerings', 'course_offering_lecturers', 'course_enrollments', 'assessments', 'marks', 'attendance_sessions', 'attendance_records', 'medical_records', 'course_materials', 'course_quizzes', 'quiz_questions', 'quiz_attempts', 'course_assignments', 'assignment_submissions', 'notices', 'timetables', 'timetable_entries', 'grades', 'course_results', 'audit_logs')
        AND constraint_type IN ('PRIMARY KEY','UNIQUE','FOREIGN KEY','CHECK')) <> 135
     OR EXISTS (
       SELECT 1 FROM (
      SELECT 'admins' AS table_name, 'fk_admins_user' AS constraint_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'chk_assessments_weight_percent' AS constraint_name
      UNION ALL
      SELECT 'assessments' AS table_name, 'fk_assessments_offering' AS constraint_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'fk_assignment_submissions_assignment' AS constraint_name
      UNION ALL
      SELECT 'assignment_submissions' AS table_name, 'fk_assignment_submissions_enrollment' AS constraint_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'fk_attendance_records_enrollment' AS constraint_name
      UNION ALL
      SELECT 'attendance_records' AS table_name, 'fk_attendance_records_session' AS constraint_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'chk_attendance_sessions_technical_officer_only' AS constraint_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'fk_attendance_sessions_component' AS constraint_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'fk_attendance_sessions_offering' AS constraint_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'fk_attendance_sessions_recorded_by' AS constraint_name
      UNION ALL
      SELECT 'attendance_sessions' AS table_name, 'fk_attendance_sessions_recorded_by_lecturer' AS constraint_name
      UNION ALL
      SELECT 'audit_logs' AS table_name, 'fk_audit_logs_user' AS constraint_name
      UNION ALL
      SELECT 'batches' AS table_name, 'chk_batches_level_no' AS constraint_name
      UNION ALL
      SELECT 'batches' AS table_name, 'fk_batches_department' AS constraint_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'fk_course_assignments_lecturer' AS constraint_name
      UNION ALL
      SELECT 'course_assignments' AS table_name, 'fk_course_assignments_offering' AS constraint_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'chk_course_components_credits' AS constraint_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'chk_course_components_planned_sessions' AS constraint_name
      UNION ALL
      SELECT 'course_components' AS table_name, 'fk_course_components_course' AS constraint_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'fk_course_enrollments_offering' AS constraint_name
      UNION ALL
      SELECT 'course_enrollments' AS table_name, 'fk_course_enrollments_student' AS constraint_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'fk_course_materials_lecturer' AS constraint_name
      UNION ALL
      SELECT 'course_materials' AS table_name, 'fk_course_materials_offering' AS constraint_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'fk_course_offering_lecturers_lecturer' AS constraint_name
      UNION ALL
      SELECT 'course_offering_lecturers' AS table_name, 'fk_course_offering_lecturers_offering' AS constraint_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'fk_course_offerings_batch' AS constraint_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'fk_course_offerings_coordinator' AS constraint_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'fk_course_offerings_course' AS constraint_name
      UNION ALL
      SELECT 'course_offerings' AS table_name, 'fk_course_offerings_semester' AS constraint_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'chk_course_quizzes_window' AS constraint_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'fk_course_quizzes_lecturer' AS constraint_name
      UNION ALL
      SELECT 'course_quizzes' AS table_name, 'fk_course_quizzes_offering' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'chk_course_results_credits' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'chk_course_results_final_mark' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'chk_course_results_grade_point' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'fk_course_results_enrollment' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'fk_course_results_grade' AS constraint_name
      UNION ALL
      SELECT 'course_results' AS table_name, 'fk_course_results_recorded_by' AS constraint_name
      UNION ALL
      SELECT 'courses' AS table_name, 'chk_courses_total_credits' AS constraint_name
      UNION ALL
      SELECT 'courses' AS table_name, 'fk_courses_department' AS constraint_name
      UNION ALL
      SELECT 'departments' AS table_name, 'chk_departments_code' AS constraint_name
      UNION ALL
      SELECT 'grades' AS table_name, 'chk_grades_grade_point' AS constraint_name
      UNION ALL
      SELECT 'grades' AS table_name, 'chk_grades_mark_range' AS constraint_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'fk_lecturers_department' AS constraint_name
      UNION ALL
      SELECT 'lecturers' AS table_name, 'fk_lecturers_user' AS constraint_name
      UNION ALL
      SELECT 'marks' AS table_name, 'chk_marks_score' AS constraint_name
      UNION ALL
      SELECT 'marks' AS table_name, 'fk_marks_assessment' AS constraint_name
      UNION ALL
      SELECT 'marks' AS table_name, 'fk_marks_enrollment' AS constraint_name
      UNION ALL
      SELECT 'marks' AS table_name, 'fk_marks_entered_by' AS constraint_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'chk_medical_records_date_range' AS constraint_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'fk_medical_records_offering' AS constraint_name
      UNION ALL
      SELECT 'medical_records' AS table_name, 'fk_medical_records_student' AS constraint_name
      UNION ALL
      SELECT 'notices' AS table_name, 'chk_notices_audience_target' AS constraint_name
      UNION ALL
      SELECT 'notices' AS table_name, 'chk_notices_expiry_date' AS constraint_name
      UNION ALL
      SELECT 'notices' AS table_name, 'fk_notices_target_batch' AS constraint_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'chk_quiz_attempts_score' AS constraint_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'chk_quiz_attempts_total' AS constraint_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'fk_quiz_attempts_enrollment' AS constraint_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'fk_quiz_attempts_quiz' AS constraint_name
      UNION ALL
      SELECT 'quiz_attempts' AS table_name, 'fk_quiz_attempts_student' AS constraint_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'chk_quiz_questions_correct_option' AS constraint_name
      UNION ALL
      SELECT 'quiz_questions' AS table_name, 'fk_quiz_questions_quiz' AS constraint_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'chk_semesters_date_range' AS constraint_name
      UNION ALL
      SELECT 'semesters' AS table_name, 'chk_semesters_semester_no' AS constraint_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'fk_technical_officers_department' AS constraint_name
      UNION ALL
      SELECT 'technical_officers' AS table_name, 'fk_technical_officers_user' AS constraint_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'chk_timetable_entries_time_range' AS constraint_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'chk_timetable_entries_optional_course_pair' AS constraint_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'fk_timetable_entries_component' AS constraint_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'fk_timetable_entries_offering' AS constraint_name
      UNION ALL
      SELECT 'timetable_entries' AS table_name, 'fk_timetable_entries_timetable' AS constraint_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'chk_timetables_level_no' AS constraint_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'fk_timetables_course_offering' AS constraint_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'fk_timetables_created_by_user' AS constraint_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'fk_timetables_department' AS constraint_name
      UNION ALL
      SELECT 'timetables' AS table_name, 'fk_timetables_semester' AS constraint_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'fk_undergraduates_batch' AS constraint_name
      UNION ALL
      SELECT 'undergraduates' AS table_name, 'fk_undergraduates_user' AS constraint_name
       ) AS expected
       LEFT JOIN information_schema.table_constraints actual
         ON actual.constraint_schema=DATABASE()
        AND actual.table_name=expected.table_name
        AND actual.constraint_name=expected.constraint_name
       WHERE actual.constraint_name IS NULL
     ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Schema preflight failed: application constraints do not match the installer schema; no records were deleted.';
  END IF;
  START TRANSACTION;
  -- Delete dependent rows before parents; FK checks remain enabled.
  DELETE FROM `audit_logs`;
  DELETE FROM `course_results`;
  DELETE FROM `assignment_submissions`;
  DELETE FROM `quiz_attempts`;
  DELETE FROM `attendance_records`;
  DELETE FROM `marks`;
  DELETE FROM `medical_records`;
  DELETE FROM `course_materials`;
  DELETE FROM `quiz_questions`;
  DELETE FROM `course_quizzes`;
  DELETE FROM `course_assignments`;
  DELETE FROM `notices`;
  DELETE FROM `attendance_sessions`;
  DELETE FROM `timetable_entries`;
  DELETE FROM `course_enrollments`;
  DELETE FROM `assessments`;
  DELETE FROM `course_offering_lecturers`;
  DELETE FROM `course_offerings`;
  DELETE FROM `timetables`;
  DELETE FROM `course_components`;
  DELETE FROM `courses`;
  DELETE FROM `admins`;
  DELETE FROM `lecturers`;
  DELETE FROM `technical_officers`;
  DELETE FROM `undergraduates`;
  DELETE FROM `batches`;
  DELETE FROM `semesters`;
  DELETE FROM `grades`;
  DELETE FROM `departments`;
  DELETE FROM `users`;
  -- Demo departments and matching cohorts. Department placement is provisional, not official.
  INSERT INTO `departments` (code,name,office_location,is_active) VALUES
    ('ICT','Information and Communication Technology','DEMO office',TRUE),
    ('BST','BST Demonstration Department (provisional)','DEMO office',TRUE),
    ('ET','ET Demonstration Department (provisional)','DEMO office',TRUE);
  INSERT INTO `batches` (department_id,intake_year,programme,batch_code,level_no)
  SELECT d.department_id,2026,CONCAT('Synthetic ',d.code,' Demonstration Cohort'),CONCAT('DEMO-',d.code,'-2026'),2
  FROM departments d WHERE d.code='ICT';
  -- Synthetic demonstration semester; dates are not official university dates.
  INSERT INTO `semesters` (academic_year,semester_no,start_date,end_date) VALUES ('2026/2027',1,'2026-07-13','2027-01-31');
  -- Unique synthetic accounts. Password hashes were created and verified by the application PasswordHasher.
  INSERT INTO `users` (username,password_hash,role,email,is_active,created_at) VALUES
    ('admin', 'PBKDF2$210000$krmRqNWMtRrV3ensidhmkg==$MYE9+WLbCZqmqLNWeWVS2BAxYbKq/FGI6mjZl7Sv3Sk=', 'ADMIN', 'demo.admin@example.invalid', TRUE, NOW()),
    ('LE001', 'PBKDF2$210000$VgF0h+jeg1L8Z4WVVEnjLA==$hk83ZsFYzpFuG/OJQ7NsFqyES7nRQagkQCt3ss8OWPI=', 'LECTURER', 'demo_lec001@example.invalid', TRUE, NOW()),
    ('LE002', 'PBKDF2$210000$T7b678mbFKgndLN4LHJYbQ==$ulH/NbjxXrz3GS5ihb0y7fAvNJ+TkIcU6a4DqqXvCxM=', 'LECTURER', 'demo_lec002@example.invalid', TRUE, NOW()),
    ('LE003', 'PBKDF2$210000$NfldvpmrinfQoHmAimJNiw==$3NNtIeCM/8vpsWSjJ1Or54I38Woii8B038iyn2F3bS8=', 'LECTURER', 'demo_lec003@example.invalid', TRUE, NOW()),
    ('LE004', 'PBKDF2$210000$fKLvdQgTNjBZdyC+RPoXFw==$/dPLKwStdSHMmpU8m3E90rTNgkfGBP7r9on1uVYat2M=', 'LECTURER', 'demo_lec004@example.invalid', TRUE, NOW()),
    ('LE005', 'PBKDF2$210000$/HWJQ30/yPDWwPz/rJl2lw==$a77CFC7RlWEV0XU7NaHuP0G1/l02utSoaI0ZZPwnsJ8=', 'LECTURER', 'demo_lec005@example.invalid', TRUE, NOW()),
    ('LE006', 'PBKDF2$210000$jRrKSSdHLi9G8Orp8pc4NA==$4uYFjMNggwvxnZ3AeQ4IePBJ9kZNCdIyXzQo7z2WrQE=', 'LECTURER', 'LE006_Demo_Lecturer_006@fot.ac.lk', TRUE, NOW()),
    ('LE007', 'PBKDF2$210000$Pjh5+keTx8+t3QWzNNCKcA==$yixBE5y5fkRYuOJLk/ZaLBpYjyhrpOs1VjtJOnbKsG4=', 'LECTURER', 'LE007_Demo_Lecturer_007@fot.ac.lk', TRUE, NOW()),
    ('LE008', 'PBKDF2$210000$BI1ACyZHGAzPx2BqjNpVyg==$oxn2H+jkA+lzVyKoyfcaW21kcGiKA9+EJSNugCzDg0A=', 'LECTURER', 'LE008_Demo_Lecturer_008@fot.ac.lk', TRUE, NOW()),
    ('LE009', 'PBKDF2$210000$3mP6W0J/zQbC09dc2iF+kA==$q+nfm8FvtCJnOEZ5ws9HEwQrAdUiRHYAY3fy7xmIZwc=', 'LECTURER', 'LE009_Demo_Lecturer_009@fot.ac.lk', TRUE, NOW()),
    ('TE001', 'PBKDF2$210000$nJ+BIYkPLIsBDiAXRUaVoA==$n1Ydezb/l3cJhEyRpMfn++8H4q/fqUoOF24L4fibFp0=', 'TECHNICAL_OFFICER', 'demo_to001@example.invalid', TRUE, NOW()),
    ('TE002', 'PBKDF2$210000$Y3pF8t+aaVPM9TMU07Y5OA==$/uQt0p9p58U59m+AZF2ie6V8qNpd3Tf4BV5QbMc9M/U=', 'TECHNICAL_OFFICER', 'demo_to002@example.invalid', TRUE, NOW()),
    ('TE003', 'PBKDF2$210000$r9L4c6Vpq7lrw7h6OUpsSQ==$/7mg2674iyhxol7vIEV1dZ6GFgRBVdBNzd+fnFJ/r6s=', 'TECHNICAL_OFFICER', 'demo_to003@example.invalid', TRUE, NOW()),
    ('TE004', 'PBKDF2$210000$6ATM7y8gcuNRVh3uENKexQ==$CbssTEchunP4ZU88M8ctmfxKNakXdY+fWSCpL33Hy3I=', 'TECHNICAL_OFFICER', 'demo_to004@example.invalid', TRUE, NOW()),
    ('ST001', 'PBKDF2$210000$Uuv70nbVDXT+4FOTOkhm+Q==$G9p/fUleqaRnNuKPvjAs5P6mljWKE+1Ooc9YkmXHyN8=', 'UNDERGRADUATE', 'demo_stu001@example.invalid', TRUE, NOW()),
    ('ST002', 'PBKDF2$210000$Y6HCngZQtpiiewk9Ft3EYA==$SE6efuCkXO+OlvAELt0dJOy5cDpq1/mtPKBzS+zRMZY=', 'UNDERGRADUATE', 'demo_stu002@example.invalid', TRUE, NOW()),
    ('ST003', 'PBKDF2$210000$mc1ybGbhvuSdQCTfhDXnhA==$PzWY3ah3MOgT+ITsqFzuL01eYhlwAiUlkbdm21zTFGU=', 'UNDERGRADUATE', 'demo_stu003@example.invalid', TRUE, NOW()),
    ('ST004', 'PBKDF2$210000$QT/K7a1VyCTZqYwpeEZdVw==$d0wHMdJ6sBBrDKQ7Rbu6TmMkndfAGMdVVbbyB20MYuE=', 'UNDERGRADUATE', 'demo_stu004@example.invalid', TRUE, NOW()),
    ('ST005', 'PBKDF2$210000$LK1W+5Argvu2e0oKNLj5sA==$gh26UGFZa0VdyCRzU9HBElkQ/PTjOozxxlc8vg8C+vg=', 'UNDERGRADUATE', 'demo_stu005@example.invalid', TRUE, NOW()),
    ('ST006', 'PBKDF2$210000$zMhMjQW8L5fB+9TPBLF6wg==$94uiNSsHtEBlFdEwqPMKPhQSiyCS1ZSgNhQRdAMNKfY=', 'UNDERGRADUATE', 'demo_stu006@example.invalid', TRUE, NOW()),
    ('ST007', 'PBKDF2$210000$aCm34uCq7bZCqhgaIQs6kQ==$fs/FdKozqxaADdBgjOm36wxbsVcPinWTUS0nATBOpA0=', 'UNDERGRADUATE', 'demo_stu007@example.invalid', TRUE, NOW()),
    ('ST008', 'PBKDF2$210000$JE8oKy9jF7OMUgEXljejxw==$NYu3W5+/OmHC6jfVVLU9ZtnDcEc6uHFUq8MfE/GYNVw=', 'UNDERGRADUATE', 'demo_stu008@example.invalid', TRUE, NOW()),
    ('ST009', 'PBKDF2$210000$6Z1z3AW40HSEbrziOvwukw==$fbmi4w1aimXitsZe9j+HvGuYGGAfnku5ZsaVHakhakk=', 'UNDERGRADUATE', 'demo_stu009@example.invalid', TRUE, NOW()),
    ('ST010', 'PBKDF2$210000$/lDl8EUrdrpoYK//fvzqCA==$SPaea2X50+oOEkO2VnXPB8AVjb3WUI0sNb1ePddSXmo=', 'UNDERGRADUATE', 'demo_stu010@example.invalid', TRUE, NOW()),
    ('ST011', 'PBKDF2$210000$uYjLstB+ccFKBl6SQ0dwRw==$foJpz0c5iM0YqHGJBZIo2a2fvppvJuhxBu/PYsBQ2ps=', 'UNDERGRADUATE', 'demo_stu011@example.invalid', TRUE, NOW()),
    ('ST012', 'PBKDF2$210000$FwQZ5imzLv+iNBqo7wFNjg==$0wv2+5+eVt5Wm+KEOBbps6+7jC5wXte3X95ZeAOeYqo=', 'UNDERGRADUATE', 'demo_stu012@example.invalid', TRUE, NOW()),
    ('ST013', 'PBKDF2$210000$MEM/H2aYYyHIeUjmKRcC/Q==$Dzlkg4m4osM9WtHNH9haFnsq4dpcwB+lDq8ZkV5Bl9w=', 'UNDERGRADUATE', 'demo_stu013@example.invalid', TRUE, NOW()),
    ('ST014', 'PBKDF2$210000$AeYa4QHN4KceZVCoUBFQ0g==$JPXJiLjQkPTPdjD2VEKSlcKEMgD0wIwtAT3DqnB4qgc=', 'UNDERGRADUATE', 'demo_stu014@example.invalid', TRUE, NOW()),
    ('ST015', 'PBKDF2$210000$bD8CWWrkdHFZFEXp9k/28w==$ompjO9SY/9HUGedVwpXVPWOhzBAqmHBUG0gwqqIeHlY=', 'UNDERGRADUATE', 'demo_stu015@example.invalid', TRUE, NOW()),
    ('ST016', 'PBKDF2$210000$9nPv4n8UXV4ESGGDIhor7Q==$Ty9qngkbPqkxwOXq5jQRNOJCE6djWWZeccmzdluxGkc=', 'UNDERGRADUATE', 'demo_stu016@example.invalid', TRUE, NOW()),
    ('ST017', 'PBKDF2$210000$hedu5HcRPuiljLqrA3miNw==$I0EZSFzKvjeXz8+4CI0e6QTajgP4Ue1fDSAcBr4OQyY=', 'UNDERGRADUATE', 'demo_stu017@example.invalid', TRUE, NOW()),
    ('ST018', 'PBKDF2$210000$jH7ngkqrkhI64NB+RqJ2tw==$3fCZ1cWftOC6//RVbTaGZWvwy+iseKVnRLkVS5Zox5Y=', 'UNDERGRADUATE', 'demo_stu018@example.invalid', TRUE, NOW()),
    ('ST019', 'PBKDF2$210000$0ernomcWcPrlmA8eFBCJLA==$4hL+Mvo37PplwDj7HVNxh3ZV8EbJM18qJ5zuOzl8030=', 'UNDERGRADUATE', 'demo_stu019@example.invalid', TRUE, NOW()),
    ('ST020', 'PBKDF2$210000$Svk3Xwl5fjZQMjHQt3CG8A==$b+/Yj9WeC+XczsgTzsjgcVjVZ1Hy38+ZqNlheAxKWiM=', 'UNDERGRADUATE', 'demo_stu020@example.invalid', TRUE, NOW());
  INSERT INTO `admins` (user_id,full_name,phone) SELECT user_id,'Demo Administrator',NULL FROM users WHERE username='admin';
  -- Each synthetic lecturer is assigned to exactly the provisional department of one demo course.
  INSERT INTO `lecturers` (user_id,department_id,full_name,phone,designation)
  SELECT u.user_id,d.department_id,CONCAT('Demo Lecturer ',RIGHT(m.username,3)),NULL,'Synthetic demonstration lecturer'
  FROM (SELECT 'LE001' AS `username`, 'ICT' AS `dept`
       UNION ALL SELECT 'LE002', 'ICT'
       UNION ALL SELECT 'LE003', 'BST'
       UNION ALL SELECT 'LE004', 'ICT'
       UNION ALL SELECT 'LE005', 'ICT'
       UNION ALL SELECT 'LE006', 'ICT'
       UNION ALL SELECT 'LE007', 'ICT'
       UNION ALL SELECT 'LE008', 'BST'
       UNION ALL SELECT 'LE009', 'ET') m
  JOIN users u ON u.username=m.username JOIN departments d ON d.code=m.dept;
  INSERT INTO `technical_officers` (user_id,department_id,full_name,phone,designation)
  SELECT u.user_id,d.department_id,CONCAT('Demo Technical Officer ',RIGHT(u.username,3)),NULL,'Synthetic demonstration officer'
  FROM users u JOIN departments d ON d.code=CASE RIGHT(u.username,3) WHEN '001' THEN 'ICT' WHEN '002' THEN 'BST' WHEN '003' THEN 'ET' ELSE 'ICT' END
  WHERE u.role='TECHNICAL_OFFICER';
  INSERT INTO `undergraduates` (user_id,batch_id,student_no,full_name,profile_picture)
  SELECT u.user_id,b.batch_id,u.username,CONCAT('Demo Student ',RIGHT(u.username,3)),NULL
  FROM users u JOIN batches b ON b.batch_code='DEMO-ICT-2026'
  WHERE u.role='UNDERGRADUATE';
  -- Course descriptions/titles use the source where compatible; ICT2142 title and two seed catalog credits/components follow the established app catalog.
  INSERT INTO `courses` (department_id,course_code,title,total_credits,is_active)
  SELECT d.department_id,v.course_code,v.title,v.credits,TRUE FROM (SELECT 'ICT2113' AS `course_code`, 'Data Structures and Algorithms' AS `title`, 'ICT' AS `dept`, 3.0 AS `credits`
       UNION ALL SELECT 'ICT2122', 'Object Oriented Programming', 'ICT', 2.0
       UNION ALL SELECT 'ICT2132', 'Object Oriented Programming Practicum', 'ICT', 3.0
       UNION ALL SELECT 'ICT2142', 'Database Systems', 'ICT', 3.0
       UNION ALL SELECT 'ICT2152', 'Object Oriented Analysis and Design', 'ICT', 2.0
       UNION ALL SELECT 'ICT2162', 'Management Information Systems', 'ICT', 2.0
       UNION ALL SELECT 'TCS2112', 'Business Economics', 'BST', 2.0
       UNION ALL SELECT 'TCS2121', 'Soft Skills', 'BST', 1.0
       UNION ALL SELECT 'ENG2112', 'English III', 'ET', 2.0) v JOIN departments d ON d.code=v.dept;
  INSERT INTO `course_components` (course_id,component_type,credits,planned_sessions)
  SELECT c.course_id,v.component_type,v.credits,15 FROM (SELECT 'ICT2113' AS `course_code`, 'THEORY' AS `component_type`, 2.0 AS `credits`
       UNION ALL SELECT 'ICT2113', 'PRACTICAL', 1.0
       UNION ALL SELECT 'ICT2122', 'THEORY', 2.0
       UNION ALL SELECT 'ICT2132', 'THEORY', 2.0
       UNION ALL SELECT 'ICT2132', 'PRACTICAL', 1.0
       UNION ALL SELECT 'ICT2142', 'THEORY', 2.0
       UNION ALL SELECT 'ICT2142', 'PRACTICAL', 1.0
       UNION ALL SELECT 'ICT2152', 'THEORY', 2.0
       UNION ALL SELECT 'ICT2162', 'THEORY', 2.0
       UNION ALL SELECT 'TCS2112', 'THEORY', 2.0
       UNION ALL SELECT 'TCS2121', 'THEORY', 1.0
       UNION ALL SELECT 'ENG2112', 'THEORY', 2.0) v JOIN courses c ON c.course_code=v.course_code;
  -- Each lecturer is responsible for exactly one course offering in the demonstration semester.
  INSERT INTO `course_offerings` (course_id,batch_id,semester_id,coordinator_id,status)
  SELECT c.course_id,b.batch_id,s.semester_id,l.lecturer_id,'OPEN' FROM (SELECT 'ICT2113' AS `course_code`, 'LE001' AS `lecturer_username`
       UNION ALL SELECT 'ICT2122', 'LE002'
       UNION ALL SELECT 'ICT2132', 'LE004'
       UNION ALL SELECT 'ICT2142', 'LE005'
       UNION ALL SELECT 'ICT2152', 'LE006'
       UNION ALL SELECT 'ICT2162', 'LE007'
       UNION ALL SELECT 'TCS2112', 'LE003'
       UNION ALL SELECT 'TCS2121', 'LE008'
       UNION ALL SELECT 'ENG2112', 'LE009') m
  JOIN courses c ON c.course_code=m.course_code JOIN departments d ON d.department_id=c.department_id
  JOIN batches b ON b.batch_code='DEMO-ICT-2026'
  JOIN semesters s ON s.academic_year='2026/2027' AND s.semester_no=1
  JOIN users u ON u.username=m.lecturer_username JOIN lecturers l ON l.user_id=u.user_id;
  INSERT INTO `course_offering_lecturers` (offering_id,lecturer_id)
  SELECT o.offering_id,l.lecturer_id FROM (SELECT 'ICT2113' AS `course_code`, 'LE001' AS `lecturer_username`
       UNION ALL SELECT 'ICT2122', 'LE002'
       UNION ALL SELECT 'ICT2132', 'LE004'
       UNION ALL SELECT 'ICT2142', 'LE005'
       UNION ALL SELECT 'ICT2152', 'LE006'
       UNION ALL SELECT 'ICT2162', 'LE007'
       UNION ALL SELECT 'TCS2112', 'LE003'
       UNION ALL SELECT 'TCS2121', 'LE008'
       UNION ALL SELECT 'ENG2112', 'LE009') m
  JOIN courses c ON c.course_code=m.course_code JOIN course_offerings o ON o.course_id=c.course_id
  JOIN users u ON u.username=m.lecturer_username JOIN lecturers l ON l.user_id=u.user_id;
  -- Register every undergraduate in every offering in their department.
  INSERT INTO `course_enrollments` (student_id,offering_id,attempt_no,student_status,enrollment_status)
  SELECT ug.student_id,o.offering_id,1,CASE WHEN u.username IN ('ST018','ST019') THEN 'REPEAT' WHEN u.username='ST020' THEN 'BATCH_MISSED' ELSE 'NORMAL' END,'ACTIVE'
  FROM undergraduates ug JOIN users u ON u.user_id=ug.user_id
  JOIN batches student_batch ON student_batch.batch_id=ug.batch_id
  CROSS JOIN course_offerings o
  JOIN batches offering_batch ON offering_batch.batch_id=o.batch_id
                              AND offering_batch.department_id=student_batch.department_id;
  -- One aggregate quiz assessment preserves the source quiz weight; individual best-two attempts are omitted because scores cannot be represented losslessly as question counts.
  INSERT INTO `assessments` (offering_id,name,assessment_type,weight_percent,is_ca_component)
  SELECT o.offering_id,v.name,v.assessment_type,v.weight_percent,v.is_ca FROM (SELECT 'ICT2113' AS `course_code`, 'DEMO Quiz Aggregate' AS `name`, 'QUIZ' AS `assessment_type`, 10 AS `weight_percent`, 1 AS `is_ca`
       UNION ALL SELECT 'ICT2113', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2113', 'DEMO Midterm Theory', 'MID', 10, 1
       UNION ALL SELECT 'ICT2113', 'DEMO Midterm Practical', 'PRACTICAL', 10, 1
       UNION ALL SELECT 'ICT2113', 'DEMO Final Theory', 'FINAL', 40, 0
       UNION ALL SELECT 'ICT2113', 'DEMO Final Practical', 'FINAL', 20, 0
       UNION ALL SELECT 'ICT2122', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'ICT2122', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2122', 'DEMO Midterm Theory', 'MID', 20, 1
       UNION ALL SELECT 'ICT2122', 'DEMO Final Theory', 'FINAL', 60, 0
       UNION ALL SELECT 'ICT2132', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'ICT2132', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2132', 'DEMO Midterm Practical', 'PRACTICAL', 30, 1
       UNION ALL SELECT 'ICT2132', 'DEMO Final Practical', 'FINAL', 50, 0
       UNION ALL SELECT 'ICT2142', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'ICT2142', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2142', 'DEMO Midterm Theory', 'MID', 20, 1
       UNION ALL SELECT 'ICT2142', 'DEMO Final Theory', 'FINAL', 60, 0
       UNION ALL SELECT 'ICT2152', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'ICT2152', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2152', 'DEMO Midterm Theory', 'MID', 20, 1
       UNION ALL SELECT 'ICT2152', 'DEMO Final Theory', 'FINAL', 60, 0
       UNION ALL SELECT 'ICT2162', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'ICT2162', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'ICT2162', 'DEMO Midterm Theory', 'MID', 20, 1
       UNION ALL SELECT 'ICT2162', 'DEMO Final Theory', 'FINAL', 60, 0
       UNION ALL SELECT 'TCS2112', 'DEMO Quiz Aggregate', 'QUIZ', 10, 1
       UNION ALL SELECT 'TCS2112', 'DEMO Assignment', 'ASSIGNMENT', 10, 1
       UNION ALL SELECT 'TCS2112', 'DEMO Midterm Theory', 'MID', 10, 1
       UNION ALL SELECT 'TCS2112', 'DEMO Final Theory', 'FINAL', 70, 0
       UNION ALL SELECT 'TCS2121', 'DEMO Quiz Aggregate', 'QUIZ', 20, 1
       UNION ALL SELECT 'TCS2121', 'DEMO Assignment', 'ASSIGNMENT', 30, 1
       UNION ALL SELECT 'TCS2121', 'DEMO Final Theory', 'FINAL', 50, 0
       UNION ALL SELECT 'ENG2112', 'DEMO Quiz Aggregate', 'QUIZ', 20, 1
       UNION ALL SELECT 'ENG2112', 'DEMO Assignment', 'ASSIGNMENT', 30, 1
       UNION ALL SELECT 'ENG2112', 'DEMO Final Theory', 'FINAL', 50, 0) v
  JOIN courses c ON c.course_code=v.course_code JOIN course_offerings o ON o.course_id=c.course_id;
  -- Allocate all coursework/CA marks to every enrolled student. Final marks are
  -- inserted only after attendance is generated and eligibility is evaluated.
  INSERT INTO `marks` (assessment_id,enrollment_id,score,entered_by)
  SELECT a.assessment_id,e.enrollment_id,CASE MOD(CAST(RIGHT(u.username,3) AS UNSIGNED)+o.offering_id,5) WHEN 0 THEN 88.00 WHEN 1 THEN 78.00 WHEN 2 THEN 68.00 WHEN 3 THEN 52.00 ELSE 32.00 END,o.coordinator_id
  FROM assessments a JOIN course_offerings o ON o.offering_id=a.offering_id JOIN course_enrollments e ON e.offering_id=o.offering_id
  JOIN undergraduates ug ON ug.student_id=e.student_id JOIN users u ON u.user_id=ug.user_id
  WHERE a.is_ca_component=TRUE;
  -- One synthetic course-specific timetable per offering used in the demo schedule.
  INSERT INTO `timetables` (title,department_id,semester_id,level_no,created_by_user_id,course_offering_id)
  SELECT CONCAT(c.course_code,' — ',b.batch_code,' — 2026/2027 Semester 1'),b.department_id,o.semester_id,b.level_no,u.user_id,o.offering_id
  FROM course_offerings o JOIN courses c ON c.course_id=o.course_id JOIN batches b ON b.batch_id=o.batch_id
  JOIN semesters s ON s.semester_id=o.semester_id AND s.academic_year='2026/2027' AND s.semester_no=1
  CROSS JOIN users u WHERE u.username='admin'
    AND c.course_code IN ('ICT2113','ICT2122','ICT2132','ICT2142','ICT2152','ICT2162','TCS2112','TCS2121','ENG2112');
  INSERT INTO `timetable_entries` (timetable_id,offering_id,component_id,day_of_week,start_time,end_time,location)
  SELECT t.timetable_id,o.offering_id,cc.component_id,v.day_of_week,v.start_time,v.end_time,v.location FROM (SELECT 'ICT2113' AS `course_code`, 'THEORY' AS `component_type`, 'ICT' AS `dept`, 'MON' AS `day_of_week`, '08:00:00' AS `start_time`, '10:00:00' AS `end_time`, 'DEMO ICT teaching room' AS `location`, '2026-10-19' AS `session_date`
       UNION ALL SELECT 'ICT2113', 'PRACTICAL', 'ICT', 'TUE', '08:00:00', '10:00:00', 'DEMO ICT teaching room', '2026-10-20'
       UNION ALL SELECT 'ICT2122', 'THEORY', 'ICT', 'WED', '08:00:00', '10:00:00', 'DEMO ICT teaching room', '2026-10-21'
       UNION ALL SELECT 'ICT2132', 'THEORY', 'ICT', 'THU', '08:00:00', '10:00:00', 'DEMO ICT teaching room', '2026-10-22'
       UNION ALL SELECT 'ICT2132', 'PRACTICAL', 'ICT', 'FRI', '08:00:00', '10:00:00', 'DEMO ICT teaching room', '2026-10-23'
       UNION ALL SELECT 'ICT2142', 'THEORY', 'ICT', 'MON', '10:00:00', '12:00:00', 'DEMO ICT teaching room', '2026-10-19'
       UNION ALL SELECT 'ICT2142', 'PRACTICAL', 'ICT', 'TUE', '10:00:00', '12:00:00', 'DEMO ICT teaching room', '2026-10-20'
       UNION ALL SELECT 'ICT2152', 'THEORY', 'ICT', 'WED', '10:00:00', '12:00:00', 'DEMO ICT teaching room', '2026-10-21'
       UNION ALL SELECT 'ICT2162', 'THEORY', 'ICT', 'THU', '10:00:00', '12:00:00', 'DEMO ICT teaching room', '2026-10-22'
       UNION ALL SELECT 'TCS2112', 'THEORY', 'BST', 'MON', '08:00:00', '10:00:00', 'DEMO BST teaching room', '2026-10-19'
       UNION ALL SELECT 'TCS2121', 'THEORY', 'BST', 'TUE', '08:00:00', '10:00:00', 'DEMO BST teaching room', '2026-10-20'
       UNION ALL SELECT 'ENG2112', 'THEORY', 'ET', 'MON', '08:00:00', '10:00:00', 'DEMO ET teaching room', '2026-10-19') v
  JOIN departments d ON d.code=v.dept
  JOIN semesters s ON s.academic_year='2026/2027' AND s.semester_no=1
  JOIN courses c ON c.course_code=v.course_code JOIN course_components cc ON cc.course_id=c.course_id AND cc.component_type=v.component_type
  JOIN course_offerings o ON o.course_id=c.course_id AND o.semester_id=s.semester_id
  JOIN batches b ON b.batch_id=o.batch_id AND b.department_id=d.department_id
  JOIN timetables t ON t.course_offering_id=o.offering_id;
  -- Fifteen weekly lecture slots per offering, distributed across its timetable
  -- components and spread across the semester. Attendance is randomized per student/course
  -- with each student attending between 9 and 15 sessions.
  INSERT INTO `attendance_sessions` (offering_id,component_id,session_date,timetable_entry_id,recorded_by,recorded_by_lecturer)
  SELECT schedule.offering_id,schedule.component_id,
         DATE_ADD('2026-07-13', INTERVAL (
           FLOOR(FLOOR((n.session_no-1)/schedule.entry_count)*14 /
             (FLOOR((15+schedule.entry_count-schedule.entry_no)/schedule.entry_count)-1))*7
           + CASE schedule.day_of_week WHEN 'MON' THEN 0 WHEN 'TUE' THEN 1 WHEN 'WED' THEN 2
                 WHEN 'THU' THEN 3 WHEN 'FRI' THEN 4 END
         ) DAY),
         schedule.timetable_entry_id,toff.officer_id,NULL
  FROM (
    SELECT e.offering_id,e.component_id,e.day_of_week,e.timetable_entry_id,e.timetable_id,
           t.department_id,
           ROW_NUMBER() OVER (PARTITION BY e.offering_id ORDER BY e.timetable_entry_id) AS entry_no,
           COUNT(*) OVER (PARTITION BY e.offering_id) AS entry_count
    FROM timetable_entries e JOIN timetables t ON t.timetable_id=e.timetable_id
    WHERE t.title LIKE 'DEMO %'
  ) schedule
  JOIN timetables t ON t.timetable_id=schedule.timetable_id
  JOIN departments d ON d.department_id=t.department_id
  JOIN users u ON u.username=CASE d.code WHEN 'ICT' THEN 'TE001' WHEN 'BST' THEN 'TE002' ELSE 'TE003' END
  JOIN technical_officers toff ON toff.user_id=u.user_id
  JOIN (SELECT 1 AS session_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
        UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
        UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15) n
    ON schedule.entry_no=MOD(n.session_no-1,schedule.entry_count)+1;
  UPDATE course_components cc
  JOIN (
    SELECT sessions.component_id,COUNT(*) AS planned_sessions
    FROM attendance_sessions sessions
    JOIN timetable_entries entries ON entries.timetable_entry_id=sessions.timetable_entry_id
    JOIN timetables timetable ON timetable.timetable_id=entries.timetable_id
    WHERE timetable.title LIKE 'DEMO %'
    GROUP BY sessions.component_id
  ) session_counts ON session_counts.component_id=cc.component_id
  SET cc.planned_sessions=session_counts.planned_sessions;
  INSERT INTO `attendance_records` (session_id,enrollment_id,is_present)
  SELECT randomized.session_id,randomized.enrollment_id,randomized.attendance_rank<=randomized.attendance_target
  FROM (
    SELECT sessions.session_id,enrollments.enrollment_id,
           ROW_NUMBER() OVER (PARTITION BY sessions.offering_id,enrollments.enrollment_id ORDER BY RAND()) AS attendance_rank,
           targets.attendance_target
    FROM attendance_sessions sessions
    JOIN course_enrollments enrollments ON enrollments.offering_id=sessions.offering_id
    JOIN (
      SELECT assigned.enrollment_id,assigned.offering_id,
             CASE WHEN assigned.course_rank=1 THEN 15 ELSE 9+FLOOR(RAND()*7) END AS attendance_target
      FROM (
        SELECT enrollment.enrollment_id,enrollment.offering_id,enrollment.student_id,
               ROW_NUMBER() OVER (PARTITION BY enrollment.student_id
                 ORDER BY CASE WHEN MOD(CAST(RIGHT(app_user.username,3) AS UNSIGNED)+enrollment.offering_id,5)=4
                               THEN 1 ELSE 0 END,enrollment.offering_id) AS course_rank
        FROM course_enrollments enrollment
        JOIN undergraduates undergraduate ON undergraduate.student_id=enrollment.student_id
        JOIN users app_user ON app_user.user_id=undergraduate.user_id
      ) assigned
    ) targets ON targets.enrollment_id=enrollments.enrollment_id
             AND targets.offering_id=enrollments.offering_id
  ) randomized;
  -- Final exam marks are recorded only when the student's weighted CA is at
  -- least 40% and attendance is at least 80% in every course component.
  INSERT INTO `marks` (assessment_id,enrollment_id,score,entered_by)
  SELECT final_assessment.assessment_id,enrollment.enrollment_id,
         CASE MOD(CAST(RIGHT(app_user.username,3) AS UNSIGNED)+final_assessment.assessment_id,5)
           WHEN 0 THEN 88.00 WHEN 1 THEN 78.00 WHEN 2 THEN 68.00 WHEN 3 THEN 52.00 ELSE 42.00 END,
         offering.coordinator_id
  FROM course_enrollments enrollment
  JOIN undergraduates undergraduate ON undergraduate.student_id=enrollment.student_id
  JOIN users app_user ON app_user.user_id=undergraduate.user_id
  JOIN course_offerings offering ON offering.offering_id=enrollment.offering_id
  JOIN assessments final_assessment ON final_assessment.offering_id=offering.offering_id
                                  AND final_assessment.assessment_type='FINAL'
  JOIN (
    SELECT mark.enrollment_id,assessment.offering_id,
           SUM(mark.score*assessment.weight_percent)/SUM(assessment.weight_percent) AS ca_mark
    FROM marks mark JOIN assessments assessment ON assessment.assessment_id=mark.assessment_id
    WHERE assessment.is_ca_component=TRUE
    GROUP BY mark.enrollment_id,assessment.offering_id
  ) ca ON ca.enrollment_id=enrollment.enrollment_id AND ca.offering_id=enrollment.offering_id
  JOIN (
    SELECT component_attendance.enrollment_id,component_attendance.offering_id
    FROM (
      SELECT session.offering_id,record.enrollment_id,session.component_id,
             SUM(CASE WHEN record.is_present=TRUE THEN 1 ELSE 0 END)/COUNT(*) AS attendance_rate
      FROM attendance_sessions session
      JOIN attendance_records record ON record.session_id=session.session_id
      GROUP BY session.offering_id,record.enrollment_id,session.component_id
    ) component_attendance
    GROUP BY component_attendance.enrollment_id,component_attendance.offering_id
    HAVING MIN(component_attendance.attendance_rate)>=0.80
  ) attendance_ok ON attendance_ok.enrollment_id=enrollment.enrollment_id
                 AND attendance_ok.offering_id=enrollment.offering_id
  WHERE ca.ca_mark>=40.00;
  -- Course material records exactly match the verified PDF manifest and upload-root paths.
  INSERT INTO `course_materials` (offering_id,lecturer_id,title,file_path,is_visible)
  SELECT o.offering_id,l.lecturer_id,v.title,v.file_path,TRUE FROM (SELECT 'ICT2113' AS `course_code`, 'Foundations and Orientation' AS `title`, 'uploads/materials/demo-readings/ICT2113/01_Foundations.pdf' AS `file_path`, 'LE001' AS `lecturer_username`
       UNION ALL SELECT 'ICT2113', 'Core Concepts', 'uploads/materials/demo-readings/ICT2113/02_Core_Concepts.pdf', 'LE001'
       UNION ALL SELECT 'ICT2113', 'Worked Examples', 'uploads/materials/demo-readings/ICT2113/03_Worked_Examples.pdf', 'LE001'
       UNION ALL SELECT 'ICT2113', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2113/04_Practical_Tutorial.pdf', 'LE001'
       UNION ALL SELECT 'ICT2113', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2113/05_Revision_Guide.pdf', 'LE001'
       UNION ALL SELECT 'ICT2122', 'Foundations and Orientation', 'uploads/materials/demo-readings/ICT2122/01_Foundations.pdf', 'LE002'
       UNION ALL SELECT 'ICT2122', 'Core Concepts', 'uploads/materials/demo-readings/ICT2122/02_Core_Concepts.pdf', 'LE002'
       UNION ALL SELECT 'ICT2122', 'Worked Examples', 'uploads/materials/demo-readings/ICT2122/03_Worked_Examples.pdf', 'LE002'
       UNION ALL SELECT 'ICT2122', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2122/04_Practical_Tutorial.pdf', 'LE002'
       UNION ALL SELECT 'ICT2122', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2122/05_Revision_Guide.pdf', 'LE002'
       UNION ALL SELECT 'ICT2132', 'Foundations and Orientation', 'uploads/materials/demo-readings/ICT2132/01_Foundations.pdf', 'LE004'
       UNION ALL SELECT 'ICT2132', 'Core Concepts', 'uploads/materials/demo-readings/ICT2132/02_Core_Concepts.pdf', 'LE004'
       UNION ALL SELECT 'ICT2132', 'Worked Examples', 'uploads/materials/demo-readings/ICT2132/03_Worked_Examples.pdf', 'LE004'
       UNION ALL SELECT 'ICT2132', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2132/04_Practical_Tutorial.pdf', 'LE004'
       UNION ALL SELECT 'ICT2132', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2132/05_Revision_Guide.pdf', 'LE004'
       UNION ALL SELECT 'ICT2142', 'Foundations and Orientation', 'uploads/materials/demo-readings/ICT2142/01_Foundations.pdf', 'LE005'
       UNION ALL SELECT 'ICT2142', 'Core Concepts', 'uploads/materials/demo-readings/ICT2142/02_Core_Concepts.pdf', 'LE005'
       UNION ALL SELECT 'ICT2142', 'Worked Examples', 'uploads/materials/demo-readings/ICT2142/03_Worked_Examples.pdf', 'LE005'
       UNION ALL SELECT 'ICT2142', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2142/04_Practical_Tutorial.pdf', 'LE005'
       UNION ALL SELECT 'ICT2142', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2142/05_Revision_Guide.pdf', 'LE005'
       UNION ALL SELECT 'ICT2152', 'Foundations and Orientation', 'uploads/materials/demo-readings/ICT2152/01_Foundations.pdf', 'LE006'
       UNION ALL SELECT 'ICT2152', 'Core Concepts', 'uploads/materials/demo-readings/ICT2152/02_Core_Concepts.pdf', 'LE006'
       UNION ALL SELECT 'ICT2152', 'Worked Examples', 'uploads/materials/demo-readings/ICT2152/03_Worked_Examples.pdf', 'LE006'
       UNION ALL SELECT 'ICT2152', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2152/04_Practical_Tutorial.pdf', 'LE006'
       UNION ALL SELECT 'ICT2152', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2152/05_Revision_Guide.pdf', 'LE006'
       UNION ALL SELECT 'ICT2162', 'Foundations and Orientation', 'uploads/materials/demo-readings/ICT2162/01_Foundations.pdf', 'LE007'
       UNION ALL SELECT 'ICT2162', 'Core Concepts', 'uploads/materials/demo-readings/ICT2162/02_Core_Concepts.pdf', 'LE007'
       UNION ALL SELECT 'ICT2162', 'Worked Examples', 'uploads/materials/demo-readings/ICT2162/03_Worked_Examples.pdf', 'LE007'
       UNION ALL SELECT 'ICT2162', 'Practical Tutorial', 'uploads/materials/demo-readings/ICT2162/04_Practical_Tutorial.pdf', 'LE007'
       UNION ALL SELECT 'ICT2162', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ICT2162/05_Revision_Guide.pdf', 'LE007'
       UNION ALL SELECT 'TCS2112', 'Foundations and Orientation', 'uploads/materials/demo-readings/TCS2112/01_Foundations.pdf', 'LE003'
       UNION ALL SELECT 'TCS2112', 'Core Concepts', 'uploads/materials/demo-readings/TCS2112/02_Core_Concepts.pdf', 'LE003'
       UNION ALL SELECT 'TCS2112', 'Worked Examples', 'uploads/materials/demo-readings/TCS2112/03_Worked_Examples.pdf', 'LE003'
       UNION ALL SELECT 'TCS2112', 'Practical Tutorial', 'uploads/materials/demo-readings/TCS2112/04_Practical_Tutorial.pdf', 'LE003'
       UNION ALL SELECT 'TCS2112', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/TCS2112/05_Revision_Guide.pdf', 'LE003'
       UNION ALL SELECT 'TCS2121', 'Foundations and Orientation', 'uploads/materials/demo-readings/TCS2121/01_Foundations.pdf', 'LE008'
       UNION ALL SELECT 'TCS2121', 'Core Concepts', 'uploads/materials/demo-readings/TCS2121/02_Core_Concepts.pdf', 'LE008'
       UNION ALL SELECT 'TCS2121', 'Worked Examples', 'uploads/materials/demo-readings/TCS2121/03_Worked_Examples.pdf', 'LE008'
       UNION ALL SELECT 'TCS2121', 'Practical Tutorial', 'uploads/materials/demo-readings/TCS2121/04_Practical_Tutorial.pdf', 'LE008'
       UNION ALL SELECT 'TCS2121', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/TCS2121/05_Revision_Guide.pdf', 'LE008'
       UNION ALL SELECT 'ENG2112', 'Foundations and Orientation', 'uploads/materials/demo-readings/ENG2112/01_Foundations.pdf', 'LE009'
       UNION ALL SELECT 'ENG2112', 'Core Concepts', 'uploads/materials/demo-readings/ENG2112/02_Core_Concepts.pdf', 'LE009'
       UNION ALL SELECT 'ENG2112', 'Worked Examples', 'uploads/materials/demo-readings/ENG2112/03_Worked_Examples.pdf', 'LE009'
       UNION ALL SELECT 'ENG2112', 'Practical Tutorial', 'uploads/materials/demo-readings/ENG2112/04_Practical_Tutorial.pdf', 'LE009'
       UNION ALL SELECT 'ENG2112', 'Revision Guide and Exercises', 'uploads/materials/demo-readings/ENG2112/05_Revision_Guide.pdf', 'LE009') v
  JOIN courses c ON c.course_code=v.course_code JOIN course_offerings o ON o.course_id=c.course_id
  JOIN users u ON u.username=v.lecturer_username JOIN lecturers l ON l.user_id=u.user_id;
  -- Canonical UGC-2024 grade configuration, not the conflicting borrowed grade thresholds.
  INSERT INTO `grades` (scheme_version,grade_letter,min_mark,max_mark,grade_point) VALUES
    ('UGC-2024-v1', 'A+', 85, 100, 4.0),
    ('UGC-2024-v1', 'A', 80, 84.99, 4.0),
    ('UGC-2024-v1', 'A-', 75, 79.99, 3.7),
    ('UGC-2024-v1', 'B+', 70, 74.99, 3.3),
    ('UGC-2024-v1', 'B', 65, 69.99, 3.0),
    ('UGC-2024-v1', 'B-', 60, 64.99, 2.7),
    ('UGC-2024-v1', 'C+', 55, 59.99, 2.3),
    ('UGC-2024-v1', 'C', 50, 54.99, 2.0),
    ('UGC-2024-v1', 'C-', 45, 49.99, 1.7),
    ('UGC-2024-v1', 'D+', 40, 44.99, 1.3),
    ('UGC-2024-v1', 'D', 35, 39.99, 1.0),
    ('UGC-2024-v1', 'E', 0, 34.99, 0.0);
  -- Materialize demo results only for complete marks and attempts meeting the same
  -- CA (40%) and per-component attendance (80%) rules used by the application.
  INSERT INTO `course_results` (enrollment_id,final_mark,scheme_version,grade_letter,grade_point,
      credits_counted,completed_at,recorded_by)
  SELECT enrollment.enrollment_id,calculated.final_mark,grade.scheme_version,grade.grade_letter,
         grade.grade_point,course.total_credits,'2026-10-20 12:00:00',offering.coordinator_id
  FROM course_enrollments enrollment
  JOIN course_offerings offering ON offering.offering_id=enrollment.offering_id
  JOIN courses course ON course.course_id=offering.course_id
  JOIN (
    SELECT mark.enrollment_id,assessment.offering_id,
           ROUND(SUM(mark.score*assessment.weight_percent)/SUM(assessment.weight_percent),2) AS final_mark,
           ROUND(SUM(CASE WHEN assessment.is_ca_component=TRUE
               THEN mark.score*assessment.weight_percent ELSE 0 END)
             /SUM(CASE WHEN assessment.is_ca_component=TRUE
               THEN assessment.weight_percent ELSE 0 END),8) AS ca_mark,
           COUNT(*) AS mark_count
    FROM marks mark JOIN assessments assessment ON assessment.assessment_id=mark.assessment_id
    GROUP BY mark.enrollment_id,assessment.offering_id
  ) calculated ON calculated.enrollment_id=enrollment.enrollment_id
              AND calculated.offering_id=enrollment.offering_id
  JOIN (
    SELECT components.enrollment_id,components.offering_id
    FROM (
      SELECT session.offering_id,record.enrollment_id,session.component_id,
             SUM(CASE WHEN record.is_present=TRUE THEN 1 ELSE 0 END)/COUNT(*) AS attendance_rate
      FROM attendance_sessions session
      JOIN attendance_records record ON record.session_id=session.session_id
      GROUP BY session.offering_id,record.enrollment_id,session.component_id
    ) components
    GROUP BY components.enrollment_id,components.offering_id
    HAVING MIN(components.attendance_rate)>=0.80
  ) eligible_attendance ON eligible_attendance.enrollment_id=enrollment.enrollment_id
                       AND eligible_attendance.offering_id=enrollment.offering_id
  JOIN grades grade ON grade.scheme_version='UGC-2024-v1'
                   AND calculated.final_mark BETWEEN grade.min_mark AND grade.max_mark
  WHERE enrollment.enrollment_status<>'WITHDRAWN'
    AND calculated.ca_mark>=40.00
    AND calculated.mark_count=(SELECT COUNT(*) FROM assessments required
                               WHERE required.offering_id=enrollment.offering_id);
  UPDATE course_enrollments enrollment
  JOIN course_results result ON result.enrollment_id=enrollment.enrollment_id
                            AND result.is_current=TRUE
  SET enrollment.enrollment_status='COMPLETED';
  INSERT INTO `notices` (title,body,audience,target_role,target_batch_id,publish_date,expiry_date) VALUES
    ('DEMO: semester resource access','Synthetic demonstration notice. Accounts and related activity in this dataset are test data, not official academic records.','ALL',NULL,NULL,'2026-09-01 00:00:00','2027-01-31 23:59:59');
  -- Borrowed identity fields and medical cases are excluded; quiz attempt scores are omitted due incompatible score semantics.
  COMMIT;
END$$
CALL `ams_install_replace_demo_data`()$$
DROP PROCEDURE `ams_install_replace_demo_data`$$
DELIMITER ;

-- Prevent one lecturer from being responsible for multiple offerings in a semester.
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_assignment`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_assignment_update`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_coordinator_insert`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_coordinator_update`;
DELIMITER //
CREATE TRIGGER `trg_one_course_per_lecturer_assignment`
BEFORE INSERT ON `course_offering_lecturers` FOR EACH ROW
BEGIN
    DECLARE target_semester INT;
    SELECT semester_id INTO target_semester FROM course_offerings WHERE offering_id=NEW.offering_id;
    IF EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=target_semester
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.lecturer_id OR other_assignment.lecturer_id=NEW.lecturer_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_assignment_update`
BEFORE UPDATE ON `course_offering_lecturers` FOR EACH ROW
BEGIN
    DECLARE target_semester INT;
    SELECT semester_id INTO target_semester FROM course_offerings WHERE offering_id=NEW.offering_id;
    IF EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=target_semester
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.lecturer_id OR other_assignment.lecturer_id=NEW.lecturer_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_coordinator_insert`
BEFORE INSERT ON `course_offerings` FOR EACH ROW
BEGIN
    IF NEW.coordinator_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=NEW.semester_id
          AND (other_offering.coordinator_id=NEW.coordinator_id OR other_assignment.lecturer_id=NEW.coordinator_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_coordinator_update`
BEFORE UPDATE ON `course_offerings` FOR EACH ROW
BEGIN
    IF NEW.coordinator_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=NEW.semester_id
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.coordinator_id OR other_assignment.lecturer_id=NEW.coordinator_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
DELIMITER ;

-- Post-import row counts: expected counts documented in docs/dataset-transformation-report.md.
SELECT 'admins' table_name,COUNT(*) row_count FROM admins UNION ALL
SELECT 'assessments',COUNT(*) FROM `assessments` UNION ALL
SELECT 'assignment_submissions',COUNT(*) FROM `assignment_submissions` UNION ALL
SELECT 'attendance_records',COUNT(*) FROM `attendance_records` UNION ALL
SELECT 'attendance_sessions',COUNT(*) FROM `attendance_sessions` UNION ALL
SELECT 'audit_logs',COUNT(*) FROM `audit_logs` UNION ALL
SELECT 'batches',COUNT(*) FROM `batches` UNION ALL
SELECT 'courses',COUNT(*) FROM `courses` UNION ALL
SELECT 'course_assignments',COUNT(*) FROM `course_assignments` UNION ALL
SELECT 'course_components',COUNT(*) FROM `course_components` UNION ALL
SELECT 'course_enrollments',COUNT(*) FROM `course_enrollments` UNION ALL
SELECT 'course_materials',COUNT(*) FROM `course_materials` UNION ALL
SELECT 'course_offerings',COUNT(*) FROM `course_offerings` UNION ALL
SELECT 'course_offering_lecturers',COUNT(*) FROM `course_offering_lecturers` UNION ALL
SELECT 'course_quizzes',COUNT(*) FROM `course_quizzes` UNION ALL
SELECT 'course_results',COUNT(*) FROM `course_results` UNION ALL
SELECT 'departments',COUNT(*) FROM `departments` UNION ALL
SELECT 'grades',COUNT(*) FROM `grades` UNION ALL
SELECT 'lecturers',COUNT(*) FROM `lecturers` UNION ALL
SELECT 'marks',COUNT(*) FROM `marks` UNION ALL
SELECT 'medical_records',COUNT(*) FROM `medical_records` UNION ALL
SELECT 'notices',COUNT(*) FROM `notices` UNION ALL
SELECT 'quiz_attempts',COUNT(*) FROM `quiz_attempts` UNION ALL
SELECT 'quiz_questions',COUNT(*) FROM `quiz_questions` UNION ALL
SELECT 'semesters',COUNT(*) FROM `semesters` UNION ALL
SELECT 'technical_officers',COUNT(*) FROM `technical_officers` UNION ALL
SELECT 'timetables',COUNT(*) FROM `timetables` UNION ALL
SELECT 'timetable_entries',COUNT(*) FROM `timetable_entries` UNION ALL
SELECT 'undergraduates',COUNT(*) FROM `undergraduates` UNION ALL
SELECT 'users',COUNT(*) FROM `users`;

SELECT COUNT(*) AS offerings_with_invalid_assessment_total FROM (SELECT o.offering_id,SUM(a.weight_percent) total_weight FROM course_offerings o LEFT JOIN assessments a ON a.offering_id=o.offering_id GROUP BY o.offering_id HAVING ABS(COALESCE(SUM(a.weight_percent),0)-100)>0.01) bad;
SELECT COUNT(*) AS invalid_marks FROM marks WHERE score<0 OR score>100;
SELECT COUNT(*) AS invalid_attendance_values FROM attendance_records WHERE is_present NOT IN (0,1);
SELECT COUNT(*) AS orphan_materials FROM course_materials m LEFT JOIN course_offerings o ON o.offering_id=m.offering_id LEFT JOIN lecturers l ON l.lecturer_id=m.lecturer_id WHERE o.offering_id IS NULL OR l.lecturer_id IS NULL;
SELECT 'duplicate_username' AS check_name,COUNT(*) duplicate_groups FROM (SELECT username FROM users GROUP BY username HAVING COUNT(*)>1) d UNION ALL SELECT 'duplicate_email',COUNT(*) FROM (SELECT email FROM users GROUP BY email HAVING COUNT(*)>1) d UNION ALL SELECT 'duplicate_course_code',COUNT(*) FROM (SELECT course_code FROM courses GROUP BY course_code HAVING COUNT(*)>1) d UNION ALL SELECT 'duplicate_student_no',COUNT(*) FROM (SELECT student_no FROM undergraduates GROUP BY student_no HAVING COUNT(*)>1) d;
SELECT 'orphan_enrollment' AS check_name,COUNT(*) orphan_rows FROM course_enrollments e LEFT JOIN undergraduates u ON u.student_id=e.student_id LEFT JOIN course_offerings o ON o.offering_id=e.offering_id WHERE u.student_id IS NULL OR o.offering_id IS NULL UNION ALL SELECT 'orphan_mark',COUNT(*) FROM marks m LEFT JOIN assessments a ON a.assessment_id=m.assessment_id LEFT JOIN course_enrollments e ON e.enrollment_id=m.enrollment_id LEFT JOIN lecturers l ON l.lecturer_id=m.entered_by WHERE a.assessment_id IS NULL OR e.enrollment_id IS NULL OR l.lecturer_id IS NULL UNION ALL SELECT 'orphan_attendance',COUNT(*) FROM attendance_records r LEFT JOIN attendance_sessions s ON s.session_id=r.session_id LEFT JOIN course_enrollments e ON e.enrollment_id=r.enrollment_id WHERE s.session_id IS NULL OR e.enrollment_id IS NULL;
SELECT COUNT(*) AS sessions_outside_demo_semester FROM attendance_sessions a JOIN timetable_entries e ON e.timetable_entry_id=a.timetable_entry_id JOIN timetables t ON t.timetable_id=e.timetable_id JOIN semesters s ON s.semester_id=t.semester_id WHERE a.session_date<s.start_date OR a.session_date>s.end_date;
SELECT COUNT(*) AS invalid_recorder_policy FROM attendance_sessions WHERE recorded_by IS NULL OR recorded_by_lecturer IS NOT NULL;
SELECT COUNT(*) AS missing_student_course_enrollments
FROM undergraduates ug
JOIN batches student_batch ON student_batch.batch_id=ug.batch_id
CROSS JOIN course_offerings offering
JOIN batches offering_batch ON offering_batch.batch_id=offering.batch_id
                           AND offering_batch.department_id=student_batch.department_id
LEFT JOIN course_enrollments enrollment ON enrollment.student_id=ug.student_id
                                        AND enrollment.offering_id=offering.offering_id
WHERE enrollment.enrollment_id IS NULL;
SELECT COUNT(*) AS missing_ca_marks
FROM course_enrollments enrollment
JOIN assessments assessment ON assessment.offering_id=enrollment.offering_id
                            AND assessment.is_ca_component=TRUE
LEFT JOIN marks mark ON mark.assessment_id=assessment.assessment_id
                    AND mark.enrollment_id=enrollment.enrollment_id
WHERE mark.mark_id IS NULL;
SELECT COUNT(*) AS final_marks_for_ineligible_students
FROM marks final_mark
JOIN assessments final_assessment ON final_assessment.assessment_id=final_mark.assessment_id
                                  AND final_assessment.assessment_type='FINAL'
JOIN course_enrollments enrollment ON enrollment.enrollment_id=final_mark.enrollment_id
LEFT JOIN (
  SELECT mark.enrollment_id,assessment.offering_id,
         SUM(mark.score*assessment.weight_percent)/SUM(assessment.weight_percent) AS ca_mark
  FROM marks mark JOIN assessments assessment ON assessment.assessment_id=mark.assessment_id
  WHERE assessment.is_ca_component=TRUE
  GROUP BY mark.enrollment_id,assessment.offering_id
) ca ON ca.enrollment_id=enrollment.enrollment_id AND ca.offering_id=enrollment.offering_id
LEFT JOIN (
  SELECT component.enrollment_id,component.offering_id
  FROM (
    SELECT session.offering_id,record.enrollment_id,session.component_id,
           SUM(CASE WHEN record.is_present=TRUE THEN 1 ELSE 0 END)/COUNT(*) AS attendance_rate
    FROM attendance_sessions session
    JOIN attendance_records record ON record.session_id=session.session_id
    GROUP BY session.offering_id,record.enrollment_id,session.component_id
  ) component
  GROUP BY component.enrollment_id,component.offering_id
  HAVING MIN(component.attendance_rate)>=0.80
) attendance_ok ON attendance_ok.enrollment_id=enrollment.enrollment_id
               AND attendance_ok.offering_id=enrollment.offering_id
WHERE ca.ca_mark IS NULL OR ca.ca_mark<40 OR attendance_ok.enrollment_id IS NULL;
SELECT COUNT(*) AS canonical_grade_rows FROM grades WHERE scheme_version='UGC-2024-v1';
SELECT course_code,COUNT(*) AS resources,MIN(is_visible) AS all_visible FROM courses c JOIN course_offerings o ON o.course_id=c.course_id JOIN course_materials m ON m.offering_id=o.offering_id GROUP BY course_code ORDER BY course_code;
SELECT role,COUNT(*) AS account_count,SUM(is_active=TRUE) AS active_count FROM users GROUP BY role ORDER BY role;
SELECT COUNT(*) AS missing_offering_lecturer_assignments FROM course_offerings o LEFT JOIN course_offering_lecturers cl ON cl.offering_id=o.offering_id WHERE cl.offering_id IS NULL;
SELECT COUNT(*) AS lecturers_assigned_to_multiple_courses_in_a_semester
FROM (
  SELECT lecturer_id,semester_id
  FROM (
    SELECT o.semester_id,COALESCE(a.lecturer_id,o.coordinator_id) AS lecturer_id,o.offering_id
    FROM course_offerings o
    LEFT JOIN course_offering_lecturers a ON a.offering_id=o.offering_id
  ) assignments
  WHERE lecturer_id IS NOT NULL
  GROUP BY lecturer_id,semester_id
  HAVING COUNT(DISTINCT offering_id)>1
) duplicates;
-- Filesystem validation is not available inside SQL: verify all 45 manifest paths exist under src/main/resources before importing.
