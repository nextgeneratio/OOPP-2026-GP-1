-- Idempotently ensure every undergraduate has an active enrollment in every
-- offering from their department.
-- Restore the latest withdrawn attempt when possible; create a new repeat attempt
-- only when the latest attempt was completed (or no attempt exists).
START TRANSACTION;

UPDATE course_enrollments enrollment
JOIN undergraduates undergraduate ON undergraduate.student_id = enrollment.student_id
JOIN batches student_batch ON student_batch.batch_id = undergraduate.batch_id
JOIN course_offerings offering ON offering.offering_id = enrollment.offering_id
JOIN batches offering_batch ON offering_batch.batch_id = offering.batch_id
                             AND offering_batch.department_id = student_batch.department_id
JOIN (
    SELECT student_id, offering_id, MAX(attempt_no) AS latest_attempt
    FROM course_enrollments
    GROUP BY student_id, offering_id
) latest
  ON latest.student_id = enrollment.student_id
 AND latest.offering_id = enrollment.offering_id
 AND latest.latest_attempt = enrollment.attempt_no
SET enrollment.enrollment_status = 'ACTIVE'
WHERE enrollment.enrollment_status = 'WITHDRAWN'
  AND NOT EXISTS (
      SELECT 1
      FROM course_enrollments active_enrollment
      WHERE active_enrollment.student_id = enrollment.student_id
        AND active_enrollment.offering_id = enrollment.offering_id
        AND active_enrollment.enrollment_status = 'ACTIVE'
  );

INSERT INTO course_enrollments
    (student_id, offering_id, attempt_no, student_status, enrollment_status)
SELECT missing.student_id,
       missing.offering_id,
       missing.latest_attempt + 1,
       CASE WHEN missing.latest_attempt = 0 THEN 'NORMAL' ELSE 'REPEAT' END,
       'ACTIVE'
FROM (
    SELECT undergraduate.student_id,
           offering.offering_id,
           COALESCE(MAX(enrollment.attempt_no), 0) AS latest_attempt
    FROM undergraduates undergraduate
    JOIN batches student_batch ON student_batch.batch_id = undergraduate.batch_id
    CROSS JOIN course_offerings offering
    JOIN batches offering_batch ON offering_batch.batch_id = offering.batch_id
                             AND offering_batch.department_id = student_batch.department_id
    LEFT JOIN course_enrollments enrollment
      ON enrollment.student_id = undergraduate.student_id
     AND enrollment.offering_id = offering.offering_id
    GROUP BY undergraduate.student_id, offering.offering_id
) missing
WHERE NOT EXISTS (
    SELECT 1
    FROM course_enrollments active_enrollment
    WHERE active_enrollment.student_id = missing.student_id
      AND active_enrollment.offering_id = missing.offering_id
      AND active_enrollment.enrollment_status = 'ACTIVE'
);

COMMIT;

SELECT COUNT(*) AS missing_active_student_offerings
FROM undergraduates undergraduate
JOIN batches student_batch ON student_batch.batch_id = undergraduate.batch_id
CROSS JOIN course_offerings offering
JOIN batches offering_batch ON offering_batch.batch_id = offering.batch_id
                           AND offering_batch.department_id = student_batch.department_id
WHERE NOT EXISTS (
    SELECT 1
    FROM course_enrollments enrollment
    WHERE enrollment.student_id = undergraduate.student_id
      AND enrollment.offering_id = offering.offering_id
      AND enrollment.enrollment_status = 'ACTIVE'
);
