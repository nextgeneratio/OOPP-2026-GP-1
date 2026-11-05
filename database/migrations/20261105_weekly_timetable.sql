-- Schema upgrade 1/2: generic weekly timetable slots and safe activity flags.
-- Run only after reviewing preflight_20261105_weekly_timetable.sql.
-- MariaDB 10.6+; schema DDL only. Existing row identifiers, foreign keys and attendance references remain.
-- Do not run on a fresh install created from the current database/install.sql.

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

-- Post-migration checks (read-only).
SELECT table_name,column_name,column_type,is_nullable,column_default
FROM information_schema.columns
WHERE table_schema=DATABASE()
 AND ((table_name='timetables' AND column_name='is_active')
   OR (table_name='timetable_entries' AND column_name IN ('offering_id','component_id','location','is_active')))
ORDER BY table_name,ordinal_position;
SELECT constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetable_entries'
 AND constraint_name IN ('chk_timetable_entries_optional_course_pair','fk_timetable_entries_offering','fk_timetable_entries_component','fk_timetable_entries_timetable')
ORDER BY constraint_name;
SELECT COUNT(*) AS timetable_entries_after FROM timetable_entries;
SELECT COUNT(*) AS sessions_with_timetable_reference_after
FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL;
SELECT COUNT(*) AS orphaned_attendance_entry_references
FROM attendance_sessions s LEFT JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id
WHERE s.timetable_entry_id IS NOT NULL AND e.timetable_entry_id IS NULL;
