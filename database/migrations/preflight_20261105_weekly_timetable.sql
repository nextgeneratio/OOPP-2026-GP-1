-- READ-ONLY preflight for 20261105_weekly_timetable.sql.
-- Run against the restored test database, not the configured application database:
-- mariadb -u root -p university_management_timetable_test < database/migrations/preflight_20261105_weekly_timetable.sql

SELECT DATABASE() AS selected_database, VERSION() AS mariadb_version;
SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND table_name IN ('timetables','timetable_entries','attendance_sessions')
ORDER BY table_name, ordinal_position;

-- 20261105 is needed only if is_active is absent from both timetable tables,
-- offering/component are still NOT NULL, location is NOT NULL, and the pair check is absent.
SELECT
  SUM(table_name='timetables' AND column_name='is_active') AS timetable_active_column,
  SUM(table_name='timetable_entries' AND column_name='is_active') AS entry_active_column,
  SUM(table_name='timetable_entries' AND column_name='offering_id' AND is_nullable='YES') AS nullable_offering_column,
  SUM(table_name='timetable_entries' AND column_name='component_id' AND is_nullable='YES') AS nullable_component_column,
  SUM(table_name='timetable_entries' AND column_name='location' AND is_nullable='YES') AS nullable_location_column
FROM information_schema.columns
WHERE table_schema=DATABASE()
  AND ((table_name='timetables' AND column_name='is_active')
    OR (table_name='timetable_entries' AND column_name IN ('is_active','offering_id','component_id','location')));

SELECT constraint_name, constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetable_entries'
  AND constraint_name IN ('chk_timetable_entries_optional_course_pair',
      'fk_timetable_entries_offering','fk_timetable_entries_component','fk_timetable_entries_timetable')
ORDER BY constraint_name;

SELECT COUNT(*) AS invalid_existing_null_pairs
FROM timetable_entries
WHERE (offering_id IS NULL AND component_id IS NOT NULL)
   OR (offering_id IS NOT NULL AND component_id IS NULL);
SELECT COUNT(*) AS timetable_entries_before FROM timetable_entries;
SELECT COUNT(*) AS sessions_with_timetable_reference_before
FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL;
SELECT COUNT(*) AS orphaned_attendance_entry_references
FROM attendance_sessions s LEFT JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id
WHERE s.timetable_entry_id IS NOT NULL AND e.timetable_entry_id IS NULL;
