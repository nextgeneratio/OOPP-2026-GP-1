USE `university_management`;

-- Rebuild finalized results for demo enrolments that have complete marks and
-- meet the same CA and per-component attendance rules as the application.
START TRANSACTION;

UPDATE course_enrollments e
JOIN course_offerings o ON o.offering_id = e.offering_id
JOIN batches b ON b.batch_id = o.batch_id AND b.batch_code = 'DEMO-ICT-2026'
SET e.enrollment_status = 'ACTIVE'
WHERE e.enrollment_status = 'COMPLETED';

DELETE r
FROM course_results r
JOIN course_enrollments e ON e.enrollment_id = r.enrollment_id
JOIN course_offerings o ON o.offering_id = e.offering_id
JOIN batches b ON b.batch_id = o.batch_id AND b.batch_code = 'DEMO-ICT-2026';

INSERT INTO course_results
    (enrollment_id, final_mark, scheme_version, grade_letter, grade_point,
     credits_counted, completed_at, recorded_by)
SELECT e.enrollment_id,
       calculated.final_mark,
       grade.scheme_version,
       grade.grade_letter,
       grade.grade_point,
       c.total_credits,
       '2026-10-20 12:00:00',
       o.coordinator_id
FROM course_enrollments e
JOIN course_offerings o ON o.offering_id = e.offering_id
JOIN batches b ON b.batch_id = o.batch_id AND b.batch_code = 'DEMO-ICT-2026'
JOIN courses c ON c.course_id = o.course_id
JOIN (
    SELECT m.enrollment_id,
           a.offering_id,
           ROUND(SUM(m.score * a.weight_percent) / SUM(a.weight_percent), 2) AS final_mark,
           ROUND(
               SUM(CASE WHEN a.is_ca_component = TRUE THEN m.score * a.weight_percent ELSE 0 END)
               / SUM(CASE WHEN a.is_ca_component = TRUE THEN a.weight_percent ELSE 0 END),
               8
           ) AS ca_mark,
           COUNT(*) AS mark_count
    FROM marks m
    JOIN assessments a ON a.assessment_id = m.assessment_id
    GROUP BY m.enrollment_id, a.offering_id
) calculated ON calculated.enrollment_id = e.enrollment_id
             AND calculated.offering_id = e.offering_id
JOIN (
    SELECT component_results.enrollment_id,
           component_results.offering_id
    FROM (
        SELECT s.offering_id,
               ar.enrollment_id,
               s.component_id,
               SUM(CASE WHEN ar.is_present = TRUE THEN 1 ELSE 0 END) / COUNT(*) AS attendance_rate
        FROM attendance_sessions s
        JOIN attendance_records ar ON ar.session_id = s.session_id
        GROUP BY s.offering_id, ar.enrollment_id, s.component_id
    ) component_results
    GROUP BY component_results.enrollment_id, component_results.offering_id
    HAVING MIN(component_results.attendance_rate) >= 0.80
) eligible_attendance ON eligible_attendance.enrollment_id = e.enrollment_id
                     AND eligible_attendance.offering_id = e.offering_id
JOIN grades grade ON grade.scheme_version = 'UGC-2024-v1'
                 AND calculated.final_mark BETWEEN grade.min_mark AND grade.max_mark
WHERE e.enrollment_status <> 'WITHDRAWN'
  AND calculated.ca_mark >= 40.00
  AND calculated.mark_count = (
      SELECT COUNT(*) FROM assessments required
      WHERE required.offering_id = e.offering_id
  );

UPDATE course_enrollments e
JOIN course_results r ON r.enrollment_id = e.enrollment_id AND r.is_current = TRUE
SET e.enrollment_status = 'COMPLETED';

COMMIT;
