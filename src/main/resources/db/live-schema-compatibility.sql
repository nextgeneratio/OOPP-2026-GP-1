-- One-time additive migration for the previously seeded university_management database.
-- Review the target schema before applying. This migration does not drop or rewrite rows.

ALTER TABLE `course_enrollments`
    ADD COLUMN `active_offering_guard` TINYINT
        GENERATED ALWAYS AS (CASE WHEN `enrollment_status` = 'ACTIVE' THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY `uk_course_enrollments_one_active_offering`
        (`student_id`, `offering_id`, `active_offering_guard`);

ALTER TABLE `medical_records`
    ADD COLUMN `evidence_reference` VARCHAR(255) NULL;

ALTER TABLE `notices`
    ADD COLUMN `target_role` ENUM('ADMIN', 'LECTURER', 'TECHNICAL_OFFICER', 'UNDERGRADUATE') NULL,
    ADD COLUMN `target_batch_id` INT NULL,
    ADD INDEX `idx_notices_target_batch_id` (`target_batch_id`),
    ADD CONSTRAINT `chk_notices_audience_target`
        CHECK (
            (`audience` = 'ALL' AND `target_role` IS NULL AND `target_batch_id` IS NULL)
            OR (`audience` = 'ROLE' AND `target_role` IS NOT NULL AND `target_batch_id` IS NULL)
            OR (`audience` = 'BATCH' AND `target_role` IS NULL AND `target_batch_id` IS NOT NULL)
        ),
    ADD CONSTRAINT `fk_notices_target_batch`
        FOREIGN KEY (`target_batch_id`) REFERENCES `batches` (`batch_id`);

CREATE TABLE `attendance_sessions` (
    `session_id` INT NOT NULL AUTO_INCREMENT,
    `offering_id` INT NOT NULL,
    `component_id` INT NOT NULL,
    `session_date` DATE NOT NULL,
    `timetable_entry_id` INT NULL,
    `recorded_by` INT NOT NULL,
    PRIMARY KEY (`session_id`),
    UNIQUE KEY `uk_attendance_sessions_offering_component_date`
        (`offering_id`, `component_id`, `session_date`),
    INDEX `idx_attendance_sessions_offering_id` (`offering_id`),
    CONSTRAINT `fk_attendance_sessions_offering`
        FOREIGN KEY (`offering_id`) REFERENCES `course_offerings` (`offering_id`),
    CONSTRAINT `fk_attendance_sessions_component`
        FOREIGN KEY (`component_id`) REFERENCES `course_components` (`component_id`),
    CONSTRAINT `fk_attendance_sessions_recorded_by`
        FOREIGN KEY (`recorded_by`) REFERENCES `technical_officers` (`officer_id`),
    CONSTRAINT `fk_attendance_sessions_timetable_entry`
        FOREIGN KEY (`timetable_entry_id`) REFERENCES `timetable_entries` (`timetable_entry_id`)
);

CREATE TABLE `attendance_records` (
    `attendance_id` INT NOT NULL AUTO_INCREMENT,
    `session_id` INT NOT NULL,
    `enrollment_id` INT NOT NULL,
    `is_present` BOOLEAN NOT NULL,
    PRIMARY KEY (`attendance_id`),
    UNIQUE KEY `uk_attendance_records_session_enrollment` (`session_id`, `enrollment_id`),
    INDEX `idx_attendance_records_enrollment_id` (`enrollment_id`),
    CONSTRAINT `fk_attendance_records_session`
        FOREIGN KEY (`session_id`) REFERENCES `attendance_sessions` (`session_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_attendance_records_enrollment`
        FOREIGN KEY (`enrollment_id`) REFERENCES `course_enrollments` (`enrollment_id`)
);

CREATE TABLE `course_results` (
    `result_id` BIGINT NOT NULL AUTO_INCREMENT,
    `enrollment_id` INT NOT NULL,
    `final_mark` DECIMAL(5,2) NOT NULL,
    `scheme_version` VARCHAR(30) NOT NULL,
    `grade_letter` VARCHAR(3) NOT NULL,
    `grade_point` DECIMAL(3,2) NOT NULL,
    `credits_counted` DECIMAL(3,1) NOT NULL,
    `completed_at` DATETIME NOT NULL,
    `recorded_by` INT NOT NULL,
    `is_current` BOOLEAN NOT NULL DEFAULT TRUE,
    `current_enrollment_guard` TINYINT
        GENERATED ALWAYS AS (CASE WHEN `is_current` = TRUE THEN 1 ELSE NULL END) STORED,
    PRIMARY KEY (`result_id`),
    UNIQUE KEY `uk_course_results_one_current` (`enrollment_id`, `current_enrollment_guard`),
    INDEX `idx_course_results_scheme_version` (`scheme_version`),
    CONSTRAINT `chk_course_results_final_mark` CHECK (`final_mark` BETWEEN 0 AND 100),
    CONSTRAINT `chk_course_results_grade_point` CHECK (`grade_point` BETWEEN 0 AND 4),
    CONSTRAINT `chk_course_results_credits` CHECK (`credits_counted` > 0),
    CONSTRAINT `fk_course_results_enrollment`
        FOREIGN KEY (`enrollment_id`) REFERENCES `course_enrollments` (`enrollment_id`),
    CONSTRAINT `fk_course_results_grade`
        FOREIGN KEY (`scheme_version`, `grade_letter`)
        REFERENCES `grades` (`scheme_version`, `grade_letter`),
    CONSTRAINT `fk_course_results_recorded_by`
        FOREIGN KEY (`recorded_by`) REFERENCES `lecturers` (`lecturer_id`)
);
