USE `university_management`;

-- Fill missing synthetic marks for the seeded aggregate quiz assessments only.
-- Safe to rerun: existing marks and all non-demo rows are left untouched.
START TRANSACTION;

INSERT INTO `marks` (assessment_id, enrollment_id, score, entered_by)
SELECT a.assessment_id,
       e.enrollment_id,
       CASE MOD(CAST(RIGHT(u.username, 3) AS UNSIGNED), 5)
           WHEN 0 THEN 88.00
           WHEN 1 THEN 78.00
           WHEN 2 THEN 68.00
           WHEN 3 THEN 52.00
           ELSE 42.00
       END,
       o.coordinator_id
FROM assessments a
JOIN course_offerings o ON o.offering_id = a.offering_id
JOIN course_enrollments e ON e.offering_id = o.offering_id
JOIN undergraduates ug ON ug.student_id = e.student_id
JOIN users u ON u.user_id = ug.user_id
WHERE a.name = 'DEMO Quiz Aggregate'
  AND a.assessment_type = 'QUIZ'
  AND u.username REGEXP '^ST[0-9]{3}$'
  AND NOT EXISTS (
      SELECT 1
      FROM marks existing
      WHERE existing.assessment_id = a.assessment_id
        AND existing.enrollment_id = e.enrollment_id
  );

COMMIT;
