-- Weekly timetable upgrade for existing MariaDB installations.
-- Review the read-only preflight output before running the ALTER statements.
-- Run from the repository root: mariadb -u root -p university_management < database/migrations/20261105_weekly_timetable.sql
-- This migration preserves all timetable/attendance row identifiers and does not modify application data.

-- Read-only preflight: confirm expected tables/columns and count historical references.
SELECT DATABASE() AS selected_database;
SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('timetables','timetable_entries','attendance_sessions')
ORDER BY table_name, ordinal_position;
SELECT COUNT(*) AS existing_timetable_slots FROM timetable_entries;
SELECT COUNT(*) AS attendance_slot_references
FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL;
SELECT COUNT(*) AS invalid_legacy_course_pairs
FROM timetable_entries WHERE (offering_id IS NULL) <> (component_id IS NULL);

-- Stop and resolve manually if invalid_legacy_course_pairs is non-zero.
-- Existing rows get active=TRUE via the column default; existing foreign keys remain intact.
ALTER TABLE timetables
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE timetable_entries
    MODIFY offering_id INT NULL,
    MODIFY component_id INT NULL,
    MODIFY location VARCHAR(80) NULL,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_timetable_entries_optional_course_pair
        CHECK ((offering_id IS NULL AND component_id IS NULL)
            OR (offering_id IS NOT NULL AND component_id IS NOT NULL));

-- Post-migration verification: both legacy references and generic weekly slots are supported.
SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND ((table_name='timetables' AND column_name='is_active')
    OR (table_name='timetable_entries' AND column_name IN ('offering_id','component_id','location','is_active')))
ORDER BY table_name, ordinal_position;
SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetable_entries'
  AND constraint_name IN ('chk_timetable_entries_optional_course_pair',
      'fk_timetable_entries_offering','fk_timetable_entries_component');
SELECT COUNT(*) AS attendance_references_still_present
FROM attendance_sessions s JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id;
SELECT COUNT(*) AS preserved_slot_rows FROM timetable_entries;
