-- Upgrade active timetables so each new schedule belongs to one Course Offering.
-- Run only after database/migrations/20261105_weekly_timetable.sql when that migration
-- has not already been applied. If 20261105 is already installed, run this file alone.
-- Do not run this file against a populated application DB without a verified backup.
-- Example: mariadb -u root -p university_management < database/migrations/20261106_course_specific_timetables.sql

-- READ-ONLY PREFLIGHT. Review all output before proceeding. Resolve any duplicate
-- active offering assignments or invalid inferred mappings manually before the ALTERs.
SELECT DATABASE() AS selected_database;
SELECT table_name,column_name,column_type,is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name IN ('timetables','timetable_entries','course_offerings','batches','courses')
ORDER BY table_name,ordinal_position;

-- Existing duplicate active header assignments (normally none before this migration).
SELECT course_offering_id,COUNT(*) AS active_timetables
FROM timetables
WHERE is_active=TRUE AND course_offering_id IS NOT NULL
GROUP BY course_offering_id HAVING COUNT(*)>1;

-- Candidate legacy headers that are provably one offering, and only one.
SELECT t.timetable_id,MIN(e.offering_id) AS inferred_course_offering_id,COUNT(*) AS slot_count
FROM timetables t JOIN timetable_entries e ON e.timetable_id=t.timetable_id
WHERE t.is_active=TRUE AND t.course_offering_id IS NULL
GROUP BY t.timetable_id,t.department_id,t.semester_id,t.level_no
HAVING COUNT(*)>0
   AND COUNT(e.offering_id)=COUNT(*)
   AND COUNT(e.component_id)=COUNT(*)
   AND COUNT(DISTINCT e.offering_id)=1
   AND MIN(e.offering_id) IS NOT NULL
   AND EXISTS (
       SELECT 1 FROM course_offerings o
       JOIN batches b ON b.batch_id=o.batch_id
       JOIN courses c ON c.course_id=o.course_id
       WHERE o.offering_id=MIN(e.offering_id)
         AND o.semester_id=t.semester_id
         AND b.department_id=t.department_id
         AND b.level_no=t.level_no
         AND c.department_id=t.department_id
   );

-- Ambiguous, generic, mixed-course or scope-mismatched legacy schedules remain
-- unassigned. Inspect them here; the migration intentionally does not guess.
SELECT t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no,
       COUNT(e.timetable_entry_id) AS slot_count,
       COUNT(DISTINCT e.offering_id) AS distinct_offerings,
       SUM(e.offering_id IS NULL OR e.component_id IS NULL) AS generic_or_incomplete_slots
FROM timetables t LEFT JOIN timetable_entries e ON e.timetable_id=t.timetable_id
WHERE t.course_offering_id IS NULL
GROUP BY t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no
ORDER BY t.timetable_id;

-- 1. Add nullable relationship so ambiguous legacy headers remain valid.
ALTER TABLE timetables
    ADD COLUMN course_offering_id INT NULL,
    ADD COLUMN active_course_offering_id INT
      AS (CASE WHEN is_active=TRUE THEN course_offering_id ELSE NULL END) PERSISTENT,
    ADD CONSTRAINT fk_timetables_course_offering
      FOREIGN KEY (course_offering_id) REFERENCES course_offerings(offering_id);

-- 2. Backfill only the unambiguous rows matching the full legacy header scope.
UPDATE timetables t
JOIN (
    SELECT e.timetable_id,MIN(e.offering_id) AS offering_id
    FROM timetable_entries e
    GROUP BY e.timetable_id
    HAVING COUNT(*)>0
       AND COUNT(e.offering_id)=COUNT(*)
       AND COUNT(e.component_id)=COUNT(*)
       AND COUNT(DISTINCT e.offering_id)=1
) proven ON proven.timetable_id=t.timetable_id
JOIN course_offerings o ON o.offering_id=proven.offering_id AND o.semester_id=t.semester_id
JOIN batches b ON b.batch_id=o.batch_id AND b.department_id=t.department_id AND b.level_no=t.level_no
JOIN courses c ON c.course_id=o.course_id AND c.department_id=t.department_id
SET t.course_offering_id=proven.offering_id
WHERE t.course_offering_id IS NULL;

-- 3. Enforce one active timetable per offering. Deactivated rows have NULL in
-- the generated key and therefore keep their historical course relationship.
ALTER TABLE timetables
    ADD UNIQUE KEY uk_timetables_active_offering (active_course_offering_id);

-- POST-MIGRATION VERIFICATION.
SELECT COUNT(*) AS active_course_timetables_without_offering
FROM timetables WHERE is_active=TRUE AND course_offering_id IS NULL;
SELECT course_offering_id,COUNT(*) AS active_timetables
FROM timetables WHERE is_active=TRUE AND course_offering_id IS NOT NULL
GROUP BY course_offering_id HAVING COUNT(*)>1;
SELECT COUNT(*) AS timetable_slot_rows FROM timetable_entries;
SELECT COUNT(*) AS attendance_references_preserved
FROM attendance_sessions s JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id;
SELECT COUNT(*) AS still_unassigned_legacy_timetables
FROM timetables WHERE course_offering_id IS NULL;
