-- Review-and-apply migration for databases created from the earlier database.sql.
-- Back up first. Do not run this together with live-schema-compatibility.sql:
-- both add some of the same columns/constraints.
-- Run only the statements whose target table/column/index does not already exist.

ALTER TABLE departments
    ADD CONSTRAINT chk_departments_code CHECK (code IN ('ICT', 'BST', 'ET'));
ALTER TABLE batches
    ADD COLUMN level_no TINYINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_batches_level_no CHECK (level_no BETWEEN 1 AND 4);
ALTER TABLE course_components
    ADD CONSTRAINT uk_course_components_course_type UNIQUE (course_id, component_type);
CREATE TABLE course_offering_lecturers (
    offering_id INT NOT NULL, lecturer_id INT NOT NULL,
    PRIMARY KEY (offering_id, lecturer_id),
    INDEX idx_course_offering_lecturers_lecturer_id (lecturer_id),
    CONSTRAINT fk_course_offering_lecturers_offering FOREIGN KEY (offering_id)
        REFERENCES course_offerings(offering_id) ON DELETE CASCADE,
    CONSTRAINT fk_course_offering_lecturers_lecturer FOREIGN KEY (lecturer_id)
        REFERENCES lecturers(lecturer_id) ON DELETE RESTRICT
);
ALTER TABLE course_enrollments
    ADD COLUMN active_offering_guard TINYINT GENERATED ALWAYS AS
        (CASE WHEN enrollment_status = 'ACTIVE' THEN 1 ELSE NULL END) STORED,
    ADD CONSTRAINT uk_course_enrollments_one_active_offering
        UNIQUE (student_id, offering_id, active_offering_guard);
ALTER TABLE attendance_sessions
    DROP FOREIGN KEY fk_attendance_sessions_recorded_by,
    MODIFY recorded_by INT NULL,
    ADD COLUMN recorded_by_lecturer INT NULL,
    ADD CONSTRAINT fk_attendance_sessions_recorded_by FOREIGN KEY (recorded_by)
        REFERENCES technical_officers(officer_id),
    ADD CONSTRAINT fk_attendance_sessions_recorded_by_lecturer FOREIGN KEY (recorded_by_lecturer)
        REFERENCES lecturers(lecturer_id),
    ADD CONSTRAINT chk_attendance_sessions_single_recorder
        CHECK ((recorded_by IS NULL) <> (recorded_by_lecturer IS NULL));
ALTER TABLE attendance_records ADD INDEX idx_attendance_records_enrollment_id (enrollment_id);
ALTER TABLE medical_records ADD COLUMN evidence_reference VARCHAR(255) NULL;
ALTER TABLE course_materials
    ADD COLUMN is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    ADD INDEX idx_course_materials_offering_id (offering_id),
    ADD INDEX idx_course_materials_lecturer_id (lecturer_id);
ALTER TABLE notices
    ADD COLUMN target_role ENUM('ADMIN','LECTURER','TECHNICAL_OFFICER','UNDERGRADUATE') NULL,
    ADD COLUMN target_batch_id INT NULL,
    ADD CONSTRAINT chk_notices_audience_target CHECK (
        (audience = 'ALL' AND target_role IS NULL AND target_batch_id IS NULL) OR
        (audience = 'ROLE' AND target_role IS NOT NULL AND target_batch_id IS NULL) OR
        (audience = 'BATCH' AND target_role IS NULL AND target_batch_id IS NOT NULL)),
    ADD CONSTRAINT fk_notices_target_batch FOREIGN KEY (target_batch_id) REFERENCES batches(batch_id);
ALTER TABLE timetables
    ADD COLUMN level_no TINYINT NOT NULL DEFAULT 1,
    ADD CONSTRAINT chk_timetables_level_no CHECK (level_no BETWEEN 1 AND 4);

-- Also create course_quizzes, quiz_questions, quiz_attempts, course_assignments,
-- assignment_submissions, and course_results from database/database.sql if absent.
