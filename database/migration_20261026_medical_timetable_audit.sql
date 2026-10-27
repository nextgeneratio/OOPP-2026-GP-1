-- Additive acceptance-audit migration for an isolated MySQL test database.
-- Prerequisite: inspect information_schema first and run this only if both columns are absent.
-- Existing medical and timetable records remain valid: both new columns are nullable.

ALTER TABLE medical_records
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER affects_eligibility;

ALTER TABLE timetables
    ADD COLUMN created_by_user_id BIGINT NULL AFTER level_no,
    ADD INDEX idx_timetables_created_by_user_id (created_by_user_id),
    ADD CONSTRAINT fk_timetables_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users (user_id);
