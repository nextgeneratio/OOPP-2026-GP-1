-- Schema upgrade 2/2: associate timetable headers with one Course Offering.
-- Review preflight_20261106_course_specific_timetables.sql first.
-- Prerequisite: 20261105_weekly_timetable.sql has been applied.
-- MariaDB 10.6+ and MySQL 8.0.16+. Existing IDs and attendance references are retained.
-- Ambiguous/generic/mixed-course legacy timetables remain course_offering_id=NULL.

ALTER TABLE timetables
    ADD COLUMN course_offering_id INT NULL,
    ADD COLUMN active_course_offering_id INT
      GENERATED ALWAYS AS (CASE WHEN is_active=TRUE THEN course_offering_id ELSE NULL END) STORED,
    ADD CONSTRAINT fk_timetables_course_offering
      FOREIGN KEY (course_offering_id) REFERENCES course_offerings(offering_id),
    ADD UNIQUE KEY uk_timetables_active_offering (active_course_offering_id);

-- Safe backfill only: all timetable slots must reference one offering and valid
-- components from that course, and the offering must match the timetable scope.
-- No slot IDs or attendance references are updated or removed.
UPDATE timetables t
JOIN (
 SELECT e.timetable_id,MIN(e.offering_id) AS offering_id
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
SET t.course_offering_id=proof.offering_id
WHERE t.is_active=TRUE AND t.course_offering_id IS NULL;

-- Post-migration verification. A non-zero unassigned count is allowed: these are
-- intentionally preserved legacy schedules that were ambiguous or generic.
SELECT column_name,column_type,is_nullable,extra
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='timetables'
 AND column_name IN ('course_offering_id','active_course_offering_id','is_active')
ORDER BY ordinal_position;
SELECT constraint_name,constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema=DATABASE() AND table_name='timetables'
 AND constraint_name IN ('fk_timetables_course_offering','uk_timetables_active_offering')
ORDER BY constraint_name;
SELECT course_offering_id,COUNT(*) AS active_timetables
FROM timetables WHERE is_active=TRUE AND course_offering_id IS NOT NULL
GROUP BY course_offering_id HAVING COUNT(*)>1;
SELECT COUNT(*) AS active_unassigned_legacy_timetables
FROM timetables WHERE is_active=TRUE AND course_offering_id IS NULL;
SELECT COUNT(*) AS timetable_entries_after FROM timetable_entries;
SELECT COUNT(*) AS attendance_references_after
FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL;
SELECT COUNT(*) AS orphaned_attendance_entry_references
FROM attendance_sessions s LEFT JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id
WHERE s.timetable_entry_id IS NOT NULL AND e.timetable_entry_id IS NULL;
