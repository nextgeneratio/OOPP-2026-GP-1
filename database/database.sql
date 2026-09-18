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
    UNIQUE KEY uk_departments_code (code)
);

-- Table: batches
CREATE TABLE `batches` (
    batch_id INT NOT NULL AUTO_INCREMENT,
    department_id INT NOT NULL,
    intake_year YEAR NOT NULL,
    programme VARCHAR(100) NOT NULL,
    batch_code VARCHAR(20) NOT NULL,

    PRIMARY KEY (batch_id),
    UNIQUE KEY uk_batches_batch_code (batch_code),

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

-- Table: course_enrollments
CREATE TABLE `course_enrollments` (
    enrollment_id INT NOT NULL AUTO_INCREMENT,
    student_id INT NOT NULL,
    offering_id INT NOT NULL,
    attempt_no TINYINT NOT NULL DEFAULT 1,
    student_status ENUM('NORMAL', 'REPEAT', 'BATCH_MISSED') NOT NULL,
    enrollment_status ENUM('ACTIVE', 'COMPLETED', 'WITHDRAWN') NOT NULL,

    PRIMARY KEY (enrollment_id),
    UNIQUE KEY uk_course_enrollments_attempt (
        student_id,
        offering_id,
        attempt_no
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

```sql
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
    recorded_by INT NOT NULL,

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

    CONSTRAINT fk_attendance_sessions_timetable_entry
        FOREIGN KEY (timetable_entry_id)
        REFERENCES `timetable_entries` (timetable_entry_id),

    CONSTRAINT fk_attendance_sessions_recorded_by
        FOREIGN KEY (recorded_by)
        REFERENCES `technical_officers` (officer_id)
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
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED')
        NOT NULL DEFAULT 'PENDING',
    affects_eligibility BOOLEAN NOT NULL DEFAULT FALSE,

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

    PRIMARY KEY (material_id),

    CONSTRAINT fk_course_materials_offering
        FOREIGN KEY (offering_id)
        REFERENCES `course_offerings` (offering_id),

    CONSTRAINT fk_course_materials_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES `lecturers` (lecturer_id)
);

-- Table: notices
CREATE TABLE `notices` (
    notice_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    audience ENUM('ALL', 'ROLE', 'BATCH') NOT NULL,
    publish_date DATETIME NOT NULL,
    expiry_date DATETIME NULL,

    PRIMARY KEY (notice_id),
    INDEX idx_notices_publish_date (publish_date),
    INDEX idx_notices_expiry_date (expiry_date),

    CONSTRAINT chk_notices_expiry_date
        CHECK (expiry_date IS NULL OR expiry_date > publish_date)
);

-- Table: timetables
CREATE TABLE `timetables` (
    timetable_id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    department_id INT NOT NULL,
    semester_id INT NOT NULL,

    PRIMARY KEY (timetable_id),

    CONSTRAINT fk_timetables_department
        FOREIGN KEY (department_id)
        REFERENCES `departments` (department_id),

    CONSTRAINT fk_timetables_semester
        FOREIGN KEY (semester_id)
        REFERENCES `semesters` (semester_id)
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

