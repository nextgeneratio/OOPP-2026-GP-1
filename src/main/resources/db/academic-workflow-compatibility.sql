-- Additive compatibility migration for databases created before academic workflow updates.
-- Review existing department codes before applying the ICT/BST/ET check constraint below.

ALTER TABLE `batches`
    ADD COLUMN `level_no` TINYINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT `chk_batches_level_no` CHECK (`level_no` BETWEEN 1 AND 4);

ALTER TABLE `departments`
    ADD CONSTRAINT `chk_departments_code` CHECK (`code` IN ('ICT', 'BST', 'ET'));

ALTER TABLE `course_materials`
    ADD COLUMN `is_visible` BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE `timetables`
    ADD COLUMN `level_no` TINYINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT `chk_timetables_level_no` CHECK (`level_no` BETWEEN 1 AND 4);

ALTER TABLE `attendance_sessions`
    DROP FOREIGN KEY `fk_attendance_sessions_recorded_by`,
    MODIFY COLUMN `recorded_by` INT NOT NULL,
    ADD COLUMN `recorded_by_lecturer` INT NULL,
    ADD CONSTRAINT `fk_attendance_sessions_recorded_by`
        FOREIGN KEY (`recorded_by`) REFERENCES `technical_officers` (`officer_id`),
    ADD CONSTRAINT `fk_attendance_sessions_recorded_by_lecturer`
        FOREIGN KEY (`recorded_by_lecturer`) REFERENCES `lecturers` (`lecturer_id`),
    ADD CONSTRAINT `chk_attendance_sessions_technical_officer_only`
        CHECK (`recorded_by` IS NOT NULL AND `recorded_by_lecturer` IS NULL);

CREATE TABLE `course_quizzes` (
    `quiz_id` INT NOT NULL AUTO_INCREMENT,
    `offering_id` INT NOT NULL,
    `lecturer_id` INT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `is_visible` BOOLEAN NOT NULL DEFAULT FALSE,
    `opens_at` DATETIME NOT NULL,
    `closes_at` DATETIME NOT NULL,
    PRIMARY KEY (`quiz_id`),
    INDEX `idx_course_quizzes_offering_visibility` (`offering_id`, `is_visible`),
    CONSTRAINT `chk_course_quizzes_window` CHECK (`closes_at` > `opens_at`),
    CONSTRAINT `fk_course_quizzes_offering` FOREIGN KEY (`offering_id`)
        REFERENCES `course_offerings` (`offering_id`),
    CONSTRAINT `fk_course_quizzes_lecturer` FOREIGN KEY (`lecturer_id`)
        REFERENCES `lecturers` (`lecturer_id`)
);

CREATE TABLE `quiz_questions` (
    `question_id` INT NOT NULL AUTO_INCREMENT,
    `quiz_id` INT NOT NULL,
    `ordinal_no` SMALLINT NOT NULL,
    `prompt` VARCHAR(1000) NOT NULL,
    `option_a` VARCHAR(500) NOT NULL,
    `option_b` VARCHAR(500) NOT NULL,
    `option_c` VARCHAR(500) NOT NULL,
    `option_d` VARCHAR(500) NOT NULL,
    `correct_option` TINYINT NOT NULL,
    PRIMARY KEY (`question_id`),
    UNIQUE KEY `uk_quiz_questions_ordinal` (`quiz_id`, `ordinal_no`),
    CONSTRAINT `chk_quiz_questions_correct_option` CHECK (`correct_option` BETWEEN 0 AND 3),
    CONSTRAINT `fk_quiz_questions_quiz` FOREIGN KEY (`quiz_id`)
        REFERENCES `course_quizzes` (`quiz_id`) ON DELETE CASCADE
);

CREATE TABLE `quiz_attempts` (
    `attempt_id` BIGINT NOT NULL AUTO_INCREMENT,
    `quiz_id` INT NOT NULL,
    `student_id` INT NOT NULL,
    `enrollment_id` INT NOT NULL,
    `score` SMALLINT NOT NULL,
    `total_questions` SMALLINT NOT NULL,
    `submitted_at` DATETIME NOT NULL,
    PRIMARY KEY (`attempt_id`),
    UNIQUE KEY `uk_quiz_attempts_one_per_student` (`quiz_id`, `student_id`),
    CONSTRAINT `chk_quiz_attempts_score`
        CHECK (`score` >= 0 AND `score` <= `total_questions`),
    CONSTRAINT `chk_quiz_attempts_total` CHECK (`total_questions` > 0),
    CONSTRAINT `fk_quiz_attempts_quiz` FOREIGN KEY (`quiz_id`)
        REFERENCES `course_quizzes` (`quiz_id`),
    CONSTRAINT `fk_quiz_attempts_enrollment` FOREIGN KEY (`enrollment_id`)
        REFERENCES `course_enrollments` (`enrollment_id`),
    CONSTRAINT `fk_quiz_attempts_student` FOREIGN KEY (`student_id`)
        REFERENCES `undergraduates` (`student_id`)
);

CREATE TABLE `course_assignments` (
    `assignment_id` INT NOT NULL AUTO_INCREMENT,
    `offering_id` INT NOT NULL,
    `lecturer_id` INT NOT NULL,
    `title` VARCHAR(150) NOT NULL,
    `description` TEXT NOT NULL,
    `file_path` VARCHAR(255) NULL,
    `is_visible` BOOLEAN NOT NULL DEFAULT FALSE,
    `due_at` DATETIME NOT NULL,
    PRIMARY KEY (`assignment_id`),
    INDEX `idx_course_assignments_offering_visibility` (`offering_id`, `is_visible`),
    CONSTRAINT `fk_course_assignments_offering` FOREIGN KEY (`offering_id`)
        REFERENCES `course_offerings` (`offering_id`),
    CONSTRAINT `fk_course_assignments_lecturer` FOREIGN KEY (`lecturer_id`)
        REFERENCES `lecturers` (`lecturer_id`)
);

CREATE TABLE `assignment_submissions` (
    `submission_id` BIGINT NOT NULL AUTO_INCREMENT,
    `assignment_id` INT NOT NULL,
    `enrollment_id` INT NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `submitted_at` DATETIME NOT NULL,
    PRIMARY KEY (`submission_id`),
    UNIQUE KEY `uk_assignment_submissions_one_per_student` (`assignment_id`, `enrollment_id`),
    CONSTRAINT `fk_assignment_submissions_assignment` FOREIGN KEY (`assignment_id`)
        REFERENCES `course_assignments` (`assignment_id`),
    CONSTRAINT `fk_assignment_submissions_enrollment` FOREIGN KEY (`enrollment_id`)
        REFERENCES `course_enrollments` (`enrollment_id`)
);
