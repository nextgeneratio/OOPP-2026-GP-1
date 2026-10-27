-- Table: users
CREATE TABLE `users` (
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
);

-- Table: admins
CREATE TABLE `admins` (
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
);

-- Table: departments
CREATE TABLE `departments` (
    department_id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(15) NOT NULL,
    name VARCHAR(120) NOT NULL,
    office_location VARCHAR(80) NULL,
    is_active BOOLEAN DEFAULT TRUE,

    PRIMARY KEY (department_id),
    UNIQUE KEY uk_departments_code (code),
    CONSTRAINT chk_departments_code CHECK (code IN ('ICT', 'BST', 'ET'))
);

-- Table: batches
CREATE TABLE `batches` (
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
);

-- Table: undergraduates
CREATE TABLE `undergraduates` (
    student_id INT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    batch_id INT NOT NULL,
    student_no VARCHAR(25) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
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
);

-- Table: lecturers
CREATE TABLE `lecturers` (
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
);

-- Table: technical_officers
CREATE TABLE `technical_officers` (
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
);

-- Table: courses
CREATE TABLE `courses` (
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
);

-- Table: course_components
CREATE TABLE `course_components` (
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
);

-- Table: semesters
CREATE TABLE `semesters` (
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
);

-- Table: course_offerings
CREATE TABLE `course_offerings` (
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
);

-- An offering can have multiple lecturers in addition to its coordinator.
CREATE TABLE `course_offering_lecturers` (
    offering_id INT NOT NULL,
    lecturer_id INT NOT NULL,

    PRIMARY KEY (offering_id, lecturer_id),
    INDEX idx_course_offering_lecturers_lecturer_id (lecturer_id),

    CONSTRAINT fk_course_offering_lecturers_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_offering_lecturers_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
        ON DELETE RESTRICT
);

-- Table: course_enrollments
CREATE TABLE `course_enrollments` (
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
);

-- Table: assessments
CREATE TABLE `assessments` (
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
);


-- Table: marks
CREATE TABLE `marks` (
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
);

-- Table: attendance_sessions
CREATE TABLE `attendance_sessions` (
    session_id INT NOT NULL AUTO_INCREMENT,
    offering_id INT NOT NULL,
    component_id INT NOT NULL,
    session_date DATE NOT NULL,
    timetable_entry_id INT NULL,
    recorded_by INT NULL,
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

    CONSTRAINT chk_attendance_sessions_single_recorder
        CHECK ((recorded_by IS NULL) <> (recorded_by_lecturer IS NULL))
);

-- Table: attendance_records
CREATE TABLE `attendance_records` (
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
);

-- Table: medical_records
CREATE TABLE `medical_records` (
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
);

-- Table: course_materials
CREATE TABLE `course_materials` (
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
);

CREATE TABLE `course_quizzes` (
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
);

CREATE TABLE `quiz_questions` (
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
);

CREATE TABLE `quiz_attempts` (
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
);

CREATE TABLE `course_assignments` (
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
);

CREATE TABLE `assignment_submissions` (
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
);

-- Table: notices
CREATE TABLE `notices` (
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
);

-- Table: timetables
CREATE TABLE `timetables` (
    timetable_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    semester_id INT NOT NULL,
    level_no TINYINT NOT NULL DEFAULT 1,
    created_by_user_id BIGINT NULL,

    PRIMARY KEY (timetable_id),
    CONSTRAINT chk_timetables_level_no CHECK (level_no BETWEEN 1 AND 4),

    CONSTRAINT fk_timetables_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id),

    CONSTRAINT fk_timetables_semester
        FOREIGN KEY (semester_id)
        REFERENCES `semesters` (semester_id),

    CONSTRAINT fk_timetables_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES `users` (user_id)
);

-- Table: timetable_entries
CREATE TABLE `timetable_entries` (
    timetable_entry_id INT NOT NULL AUTO_INCREMENT,
    timetable_id INT NOT NULL,
    offering_id INT NOT NULL,
    component_id INT NOT NULL,
    day_of_week ENUM(
        'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'
    ) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    location VARCHAR(80) NOT NULL,

    PRIMARY KEY (timetable_entry_id),
    INDEX idx_timetable_entries_timetable_id (timetable_id),
    INDEX idx_timetable_entries_day_of_week (day_of_week),

    CONSTRAINT chk_timetable_entries_time_range
        CHECK (start_time < end_time),

    CONSTRAINT fk_timetable_entries_timetable
        FOREIGN KEY (timetable_id)
        REFERENCES `timetables` (timetable_id),

    CONSTRAINT fk_timetable_entries_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_timetable_entries_component
        FOREIGN KEY (component_id)
        REFERENCES `course_components` (component_id)
);

-- Table: grades
CREATE TABLE `grades` (
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
);

-- Persist the grade scheme version and credit snapshot used for each completed attempt.
CREATE TABLE `course_results` (
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
);

-- Table: audit_logs
CREATE TABLE `audit_logs` (
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
);

ALTER TABLE `attendance_sessions`
    ADD CONSTRAINT fk_attendance_sessions_timetable_entry
        FOREIGN KEY (timetable_entry_id)
        REFERENCES `timetable_entries` (timetable_entry_id);
