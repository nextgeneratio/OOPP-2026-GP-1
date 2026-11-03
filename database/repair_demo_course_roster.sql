-- Ensure every undergraduate is enrolled in each offering and each offering
-- has exactly its coordinator as the responsible lecturer.
USE `university_management`;

START TRANSACTION;

INSERT INTO `course_enrollments`
    (student_id,offering_id,attempt_no,student_status,enrollment_status)
SELECT undergraduate.student_id,offering.offering_id,COALESCE(MAX(existing.attempt_no),0)+1,
       'NORMAL','ACTIVE'
FROM `undergraduates` undergraduate
CROSS JOIN `course_offerings` offering
LEFT JOIN `course_enrollments` existing
       ON existing.student_id=undergraduate.student_id
      AND existing.offering_id=offering.offering_id
      AND existing.enrollment_status<>'WITHDRAWN'
WHERE existing.enrollment_id IS NULL
GROUP BY undergraduate.student_id,offering.offering_id;

DELETE assigned
FROM `course_offering_lecturers` assigned
JOIN `course_offerings` offering ON offering.offering_id=assigned.offering_id
WHERE offering.coordinator_id IS NOT NULL
  AND assigned.lecturer_id<>offering.coordinator_id;

INSERT INTO `course_offering_lecturers` (offering_id,lecturer_id)
SELECT offering.offering_id,offering.coordinator_id
FROM `course_offerings` offering
LEFT JOIN `course_offering_lecturers` assigned
       ON assigned.offering_id=offering.offering_id
WHERE offering.coordinator_id IS NOT NULL
  AND assigned.offering_id IS NULL;

COMMIT;

-- Add a database constraint so later edits cannot assign multiple lecturers
-- to the same course offering. DDL commits implicitly in MariaDB.
SET @add_single_lecturer_index = IF(
    EXISTS (SELECT 1 FROM information_schema.statistics
            WHERE table_schema=DATABASE()
              AND table_name='course_offering_lecturers'
              AND index_name='uk_course_offering_lecturers_one_lecturer'),
    'SELECT 1',
    'ALTER TABLE `course_offering_lecturers` ADD UNIQUE KEY `uk_course_offering_lecturers_one_lecturer` (`offering_id`)'
);
PREPARE single_lecturer_statement FROM @add_single_lecturer_index;
EXECUTE single_lecturer_statement;
DEALLOCATE PREPARE single_lecturer_statement;

SELECT COUNT(*) AS missing_student_offering_pairs
FROM `undergraduates` undergraduate
CROSS JOIN `course_offerings` offering
LEFT JOIN `course_enrollments` enrollment
       ON enrollment.student_id=undergraduate.student_id
      AND enrollment.offering_id=offering.offering_id
      AND enrollment.enrollment_status<>'WITHDRAWN'
WHERE enrollment.enrollment_id IS NULL;

SELECT course.course_code,
       COUNT(DISTINCT CASE WHEN enrollment.enrollment_status<>'WITHDRAWN' THEN enrollment.student_id END) AS enrolled_students,
       (SELECT COUNT(*) FROM `undergraduates`) AS undergraduate_count
FROM `course_offerings` offering
JOIN `courses` course ON course.course_id=offering.course_id
LEFT JOIN `course_enrollments` enrollment ON enrollment.offering_id=offering.offering_id
GROUP BY offering.offering_id,course.course_code
ORDER BY course.course_code;

SELECT COUNT(*) AS offerings_without_exactly_one_lecturer
FROM (
    SELECT offering.offering_id
    FROM `course_offerings` offering
    LEFT JOIN `course_offering_lecturers` assigned ON assigned.offering_id=offering.offering_id
    GROUP BY offering.offering_id
    HAVING COUNT(DISTINCT assigned.lecturer_id)<>1
) invalid_assignments;
