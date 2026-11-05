-- READ-ONLY preflight for 20261106_course_specific_timetables.sql.
-- Prerequisite: 20261105 is applied. Run this against the restored test DB:
-- mariadb -u root -p university_management_timetable_test < database/migrations/preflight_20261106_course_specific_timetables.sql
-- This file intentionally does not mention timetables.course_offering_id: that column
-- does not exist until the 20261106 migration is executed.

SELECT DATABASE() AS selected_database, VERSION() AS mariadb_version;
-- Treat this as a stop/go summary. Never run the migration when it says ALREADY APPLIED.
SELECT CASE
 WHEN EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema=DATABASE() AND table_name='timetables' AND column_name='course_offering_id')
  AND EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema=DATABASE() AND table_name='timetables'
                AND column_name='active_course_offering_id' AND extra LIKE '%GENERATED%')
  AND EXISTS (SELECT 1 FROM information_schema.table_constraints
              WHERE constraint_schema=DATABASE() AND table_name='timetables'
                AND constraint_name='fk_timetables_course_offering' AND constraint_type='FOREIGN KEY')
  AND EXISTS (SELECT 1 FROM information_schema.statistics
              WHERE table_schema=DATABASE() AND table_name='timetables'
                AND index_name='uk_timetables_active_offering' AND non_unique=0)
 THEN 'ALREADY APPLIED — do not run 20261106_course_specific_timetables.sql again'
 WHEN NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema=DATABASE() AND table_name='timetables' AND column_name='course_offering_id')
  AND EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema=DATABASE() AND table_name='timetables' AND column_name='is_active')
  AND EXISTS (SELECT 1 FROM information_schema.columns
              WHERE table_schema=DATABASE() AND table_name='timetable_entries'
                AND column_name='is_active' AND is_nullable='NO')
  AND EXISTS (SELECT 1 FROM information_schema.table_constraints
              WHERE constraint_schema=DATABASE() AND table_name='timetable_entries'
                AND constraint_name='chk_timetable_entries_optional_course_pair' AND constraint_type='CHECK')
 THEN 'READY FOR 20261106 PREFLIGHT REVIEW — still inspect duplicate candidates and references below'
 ELSE 'STOP — partial or unexpected migration state; do not run the migration'
 END AS migration_20261106_state;
SELECT table_name,column_name,column_type,is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE()
 AND table_name IN ('timetables','timetable_entries','course_offerings','course_components','batches','courses','attendance_sessions')
ORDER BY table_name,ordinal_position;

-- Confirm 20261105 prerequisites and confirm 20261106 is not already installed.
SELECT
 SUM(table_name='timetables' AND column_name='is_active') AS timetable_active_column,
 SUM(table_name='timetable_entries' AND column_name='is_active') AS entry_active_column,
 SUM(table_name='timetable_entries' AND column_name='offering_id' AND is_nullable='YES') AS nullable_offering,
 SUM(table_name='timetable_entries' AND column_name='component_id' AND is_nullable='YES') AS nullable_component,
 SUM(table_name='timetable_entries' AND column_name='location' AND is_nullable='YES') AS nullable_location,
 SUM(table_name='timetables' AND column_name='course_offering_id') AS course_offering_id_already_present
FROM information_schema.columns
WHERE table_schema=DATABASE()
 AND ((table_name='timetables' AND column_name IN ('is_active','course_offering_id'))
   OR (table_name='timetable_entries' AND column_name IN ('is_active','offering_id','component_id','location')));
SELECT constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetable_entries'
 AND constraint_name IN ('chk_timetable_entries_optional_course_pair','fk_timetable_entries_offering','fk_timetable_entries_component','fk_timetable_entries_timetable')
ORDER BY constraint_name;
SELECT constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetables'
 AND constraint_name IN ('fk_timetables_department','fk_timetables_semester','fk_timetables_created_by_user','fk_timetables_course_offering')
ORDER BY constraint_name;
SELECT index_name,non_unique,column_name,seq_in_index
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='timetables'
 AND index_name IN ('PRIMARY','uk_timetables_active_offering');

-- Candidate safe backfills: every slot, including inactive historical versions,
-- has a non-NULL offering/component pair; there is one offering; every component
-- belongs to that offering's course; header department/term/level match.
SELECT t.timetable_id,t.title,proof.offering_id,proof.slot_count
FROM timetables t
JOIN (
 SELECT e.timetable_id,MIN(e.offering_id) AS offering_id,COUNT(*) AS slot_count
 FROM timetable_entries e
 LEFT JOIN course_offerings o ON o.offering_id=e.offering_id
 LEFT JOIN course_components cc ON cc.component_id=e.component_id
 GROUP BY e.timetable_id
 HAVING COUNT(*)>0
    AND COUNT(e.offering_id)=COUNT(*)
    AND COUNT(e.component_id)=COUNT(*)
    AND COUNT(DISTINCT e.offering_id)=1
    AND SUM(o.offering_id IS NULL OR cc.component_id IS NULL OR cc.course_id<>o.course_id)=0
) proof ON proof.timetable_id=t.timetable_id
JOIN course_offerings o ON o.offering_id=proof.offering_id AND o.semester_id=t.semester_id
JOIN batches b ON b.batch_id=o.batch_id AND b.department_id=t.department_id AND b.level_no=t.level_no
JOIN courses c ON c.course_id=o.course_id AND c.department_id=t.department_id
WHERE t.is_active=TRUE;

-- These duplicate inferred active offerings would violate the one-active-timetable
-- constraint. Resolve them manually before applying 20261106.
SELECT safe.offering_id,COUNT(*) AS candidate_active_headers,
       GROUP_CONCAT(safe.timetable_id ORDER BY safe.timetable_id) AS timetable_ids
FROM (
 SELECT t.timetable_id,proof.offering_id
 FROM timetables t
 JOIN (
  SELECT e.timetable_id,MIN(e.offering_id) AS offering_id
  FROM timetable_entries e
  LEFT JOIN course_offerings o ON o.offering_id=e.offering_id
  LEFT JOIN course_components cc ON cc.component_id=e.component_id
  GROUP BY e.timetable_id
  HAVING COUNT(*)>0 AND COUNT(e.offering_id)=COUNT(*) AND COUNT(e.component_id)=COUNT(*)
   AND COUNT(DISTINCT e.offering_id)=1
   AND SUM(o.offering_id IS NULL OR cc.component_id IS NULL OR cc.course_id<>o.course_id)=0
 ) proof ON proof.timetable_id=t.timetable_id
 JOIN course_offerings o ON o.offering_id=proof.offering_id AND o.semester_id=t.semester_id
 JOIN batches b ON b.batch_id=o.batch_id AND b.department_id=t.department_id AND b.level_no=t.level_no
 JOIN courses c ON c.course_id=o.course_id AND c.department_id=t.department_id
 WHERE t.is_active=TRUE
) safe
GROUP BY safe.offering_id HAVING COUNT(*)>1;

-- Existing data that must remain unassigned: generic/mixed schedules, wrong scope,
-- missing course/component references, or components not belonging to the course.
SELECT t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no,
 COUNT(e.timetable_entry_id) AS slot_count,
 COUNT(DISTINCT e.offering_id) AS distinct_offerings,
 SUM(e.timetable_entry_id IS NOT NULL AND (e.offering_id IS NULL OR e.component_id IS NULL)) AS generic_or_incomplete_slots
FROM timetables t LEFT JOIN timetable_entries e ON e.timetable_id=t.timetable_id
WHERE t.is_active=TRUE
GROUP BY t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no
ORDER BY t.timetable_id;

SELECT COUNT(*) AS timetable_entries_before FROM timetable_entries;
SELECT COUNT(*) AS attendance_references_before
FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL;
SELECT COUNT(*) AS orphaned_attendance_entry_references
FROM attendance_sessions s LEFT JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id
WHERE s.timetable_entry_id IS NOT NULL AND e.timetable_entry_id IS NULL;
