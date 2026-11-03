-- Remove demo final exam marks from students who fail CA (40%) or attendance (80%).
-- Existing eligible marks and finalized results are preserved.
USE `university_management`;
START TRANSACTION;

DELETE final_mark
FROM `marks` final_mark
JOIN `assessments` final_assessment
  ON final_assessment.assessment_id=final_mark.assessment_id
 AND final_assessment.assessment_type='FINAL'
JOIN `course_enrollments` enrollment
  ON enrollment.enrollment_id=final_mark.enrollment_id
LEFT JOIN (
  SELECT mark.enrollment_id,assessment.offering_id,
         SUM(mark.score*assessment.weight_percent)/SUM(assessment.weight_percent) AS ca_mark
  FROM `marks` mark
  JOIN `assessments` assessment ON assessment.assessment_id=mark.assessment_id
  WHERE assessment.is_ca_component=TRUE
  GROUP BY mark.enrollment_id,assessment.offering_id
) ca ON ca.enrollment_id=enrollment.enrollment_id
    AND ca.offering_id=enrollment.offering_id
LEFT JOIN (
  SELECT component_attendance.enrollment_id,component_attendance.offering_id
  FROM (
    SELECT session.offering_id,record.enrollment_id,session.component_id,
           SUM(CASE WHEN record.is_present=TRUE THEN 1 ELSE 0 END)/COUNT(*) AS attendance_rate
    FROM `attendance_sessions` session
    JOIN `attendance_records` record ON record.session_id=session.session_id
    GROUP BY session.offering_id,record.enrollment_id,session.component_id
  ) component_attendance
  GROUP BY component_attendance.enrollment_id,component_attendance.offering_id
  HAVING MIN(component_attendance.attendance_rate)>=0.80
) attendance_ok ON attendance_ok.enrollment_id=enrollment.enrollment_id
               AND attendance_ok.offering_id=enrollment.offering_id
WHERE ca.ca_mark IS NULL OR ca.ca_mark<40.00 OR attendance_ok.enrollment_id IS NULL;

SET @removed_ineligible_final_marks=ROW_COUNT();
COMMIT;

SELECT @removed_ineligible_final_marks AS removed_ineligible_final_marks;
SELECT COUNT(*) AS remaining_ineligible_final_marks
FROM `marks` final_mark
JOIN `assessments` final_assessment
  ON final_assessment.assessment_id=final_mark.assessment_id
 AND final_assessment.assessment_type='FINAL'
JOIN `course_enrollments` enrollment
  ON enrollment.enrollment_id=final_mark.enrollment_id
LEFT JOIN (
  SELECT mark.enrollment_id,assessment.offering_id,
         SUM(mark.score*assessment.weight_percent)/SUM(assessment.weight_percent) AS ca_mark
  FROM `marks` mark
  JOIN `assessments` assessment ON assessment.assessment_id=mark.assessment_id
  WHERE assessment.is_ca_component=TRUE
  GROUP BY mark.enrollment_id,assessment.offering_id
) ca ON ca.enrollment_id=enrollment.enrollment_id
    AND ca.offering_id=enrollment.offering_id
LEFT JOIN (
  SELECT component_attendance.enrollment_id,component_attendance.offering_id
  FROM (
    SELECT session.offering_id,record.enrollment_id,session.component_id,
           SUM(CASE WHEN record.is_present=TRUE THEN 1 ELSE 0 END)/COUNT(*) AS attendance_rate
    FROM `attendance_sessions` session
    JOIN `attendance_records` record ON record.session_id=session.session_id
    GROUP BY session.offering_id,record.enrollment_id,session.component_id
  ) component_attendance
  GROUP BY component_attendance.enrollment_id,component_attendance.offering_id
  HAVING MIN(component_attendance.attendance_rate)>=0.80
) attendance_ok ON attendance_ok.enrollment_id=enrollment.enrollment_id
               AND attendance_ok.offering_id=enrollment.offering_id
WHERE ca.ca_mark IS NULL OR ca.ca_mark<40.00 OR attendance_ok.enrollment_id IS NULL;
