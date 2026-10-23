-- Synthetic demonstration records for an already seeded university_management database.
-- Rerunnable: inserts are guarded by stable natural keys; existing rows are not updated.

INSERT INTO `semesters` (`academic_year`, `semester_no`, `start_date`, `end_date`)
SELECT '2025/2026', 1, '2025-09-01', '2026-01-31'
WHERE NOT EXISTS (
    SELECT 1 FROM `semesters` WHERE `academic_year` = '2025/2026' AND `semester_no` = 1
);

INSERT INTO `course_offerings` (`course_id`, `batch_id`, `semester_id`, `coordinator_id`, `status`)
SELECT current_offering.`course_id`, current_offering.`batch_id`, historical.`semester_id`,
       current_offering.`coordinator_id`, 'CLOSED'
FROM `course_offerings` current_offering
JOIN `semesters` historical
    ON historical.`academic_year` = '2025/2026' AND historical.`semester_no` = 1
WHERE current_offering.`semester_id` = (
    SELECT `semester_id` FROM `semesters` WHERE `academic_year` = '2026/2027' AND `semester_no` = 1
)
AND NOT EXISTS (
    SELECT 1 FROM `course_offerings` existing
    WHERE existing.`course_id` = current_offering.`course_id`
      AND existing.`batch_id` = current_offering.`batch_id`
      AND existing.`semester_id` = historical.`semester_id`
);

INSERT INTO `course_offering_lecturers` (`offering_id`, `lecturer_id`)
SELECT historical.`offering_id`, historical.`coordinator_id`
FROM `course_offerings` historical
JOIN `semesters` semester ON semester.`semester_id` = historical.`semester_id`
WHERE semester.`academic_year` = '2025/2026'
  AND historical.`coordinator_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `course_offering_lecturers` assigned
      WHERE assigned.`offering_id` = historical.`offering_id`
        AND assigned.`lecturer_id` = historical.`coordinator_id`
  );

INSERT INTO `course_offering_lecturers` (`offering_id`, `lecturer_id`)
SELECT offering.`offering_id`, lecturer.`lecturer_id`
FROM `course_offerings` offering
JOIN `courses` course ON course.`course_id` = offering.`course_id`
JOIN `semesters` semester ON semester.`semester_id` = offering.`semester_id`
JOIN `lecturers` lecturer
JOIN `users` user_account ON user_account.`user_id` = lecturer.`user_id`
WHERE semester.`academic_year` IN ('2025/2026', '2026/2027')
  AND ((course.`course_code` = 'ICT2132' AND user_account.`username` IN ('LEC003', 'LEC004'))
    OR (course.`course_code` = 'ICT2142' AND user_account.`username` = 'LEC005'))
  AND NOT EXISTS (
      SELECT 1 FROM `course_offering_lecturers` assigned
      WHERE assigned.`offering_id` = offering.`offering_id`
        AND assigned.`lecturer_id` = lecturer.`lecturer_id`
  );

INSERT INTO `course_enrollments`
    (`student_id`, `offering_id`, `attempt_no`, `student_status`, `enrollment_status`)
SELECT student.`student_id`, historical.`offering_id`, 1, 'NORMAL', 'ACTIVE'
FROM `undergraduates` student
CROSS JOIN `course_offerings` historical
JOIN `semesters` semester ON semester.`semester_id` = historical.`semester_id`
WHERE semester.`academic_year` = '2025/2026'
  AND NOT EXISTS (
      SELECT 1 FROM `course_enrollments` existing
      WHERE existing.`student_id` = student.`student_id`
        AND existing.`offering_id` = historical.`offering_id`
        AND existing.`attempt_no` = 1
  );

INSERT INTO `assessments`
    (`offering_id`, `name`, `assessment_type`, `weight_percent`, `is_ca_component`)
SELECT offering.`offering_id`, plan.`name`, plan.`assessment_type`,
       plan.`weight_percent`, plan.`is_ca_component`
FROM `course_offerings` offering
CROSS JOIN (
    SELECT 'Assignment 1' AS `name`, 'ASSIGNMENT' AS `assessment_type`,
           20.00 AS `weight_percent`, TRUE AS `is_ca_component`
    UNION ALL
    SELECT 'Quiz 1', 'QUIZ', 20.00, TRUE
    UNION ALL
    SELECT 'Final Examination', 'FINAL', 60.00, FALSE
) plan
WHERE NOT EXISTS (
    SELECT 1 FROM `assessments` existing
    WHERE existing.`offering_id` = offering.`offering_id`
      AND existing.`name` = plan.`name`
);

INSERT INTO `marks` (`assessment_id`, `enrollment_id`, `score`, `entered_by`)
SELECT assessment.`assessment_id`, enrollment.`enrollment_id`,
       CASE assessment.`name`
           WHEN 'Assignment 1' THEN 65 + MOD(enrollment.`student_id` * 7, 30)
           WHEN 'Quiz 1' THEN 70 + MOD(enrollment.`student_id` * 3, 25)
           ELSE 68 + MOD(enrollment.`student_id` * 5, 30)
       END,
       offering.`coordinator_id`
FROM `assessments` assessment
JOIN `course_offerings` offering ON offering.`offering_id` = assessment.`offering_id`
JOIN `course_enrollments` enrollment ON enrollment.`offering_id` = offering.`offering_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `marks` existing
    WHERE existing.`assessment_id` = assessment.`assessment_id`
      AND existing.`enrollment_id` = enrollment.`enrollment_id`
);

INSERT INTO `course_materials` (`offering_id`, `lecturer_id`, `title`, `file_path`)
SELECT offering.`offering_id`, assigned.`lecturer_id`,
       CONCAT(course.`course_code`, ' - Lecture Notes'),
       CONCAT('uploads/demo-materials/', course.`course_code`, '/lecture-notes.pdf')
FROM `course_offerings` offering
JOIN `courses` course ON course.`course_id` = offering.`course_id`
JOIN `course_offering_lecturers` assigned ON assigned.`offering_id` = offering.`offering_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `course_materials` existing
    WHERE existing.`offering_id` = offering.`offering_id`
      AND existing.`title` = CONCAT(course.`course_code`, ' - Lecture Notes')
);

INSERT INTO `course_materials` (`offering_id`, `lecturer_id`, `title`, `file_path`)
SELECT offering.`offering_id`, assigned.`lecturer_id`,
       CONCAT(course.`course_code`, ' - Practical Exercises'),
       CONCAT('uploads/demo-materials/', course.`course_code`, '/practical-exercises.pdf')
FROM `course_offerings` offering
JOIN `courses` course ON course.`course_id` = offering.`course_id`
JOIN `course_offering_lecturers` assigned ON assigned.`offering_id` = offering.`offering_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `course_materials` existing
    WHERE existing.`offering_id` = offering.`offering_id`
      AND existing.`title` = CONCAT(course.`course_code`, ' - Practical Exercises')
);

INSERT INTO `timetables` (`title`, `department_id`, `semester_id`)
SELECT CONCAT('ICT Demo Timetable ', semester.`academic_year`), department.`department_id`,
       semester.`semester_id`
FROM `semesters` semester
JOIN `departments` department ON department.`code` = 'ICT'
WHERE semester.`academic_year` IN ('2025/2026', '2026/2027')
  AND NOT EXISTS (
      SELECT 1 FROM `timetables` existing
      WHERE existing.`semester_id` = semester.`semester_id`
        AND existing.`department_id` = department.`department_id`
        AND existing.`title` = CONCAT('ICT Demo Timetable ', semester.`academic_year`)
  );

INSERT INTO `timetable_entries`
    (`timetable_id`, `offering_id`, `component_id`, `day_of_week`,
     `start_time`, `end_time`, `location`)
SELECT timetable.`timetable_id`, offering.`offering_id`, component.`component_id`,
       CASE
           WHEN semester.`academic_year` = '2025/2026' AND component.`component_type` = 'THEORY' THEN 'MON'
           WHEN semester.`academic_year` = '2025/2026' THEN 'WED'
           WHEN component.`component_type` = 'THEORY' THEN 'TUE'
           ELSE 'THU'
       END,
       CASE WHEN component.`component_type` = 'THEORY' THEN '09:00:00' ELSE '13:00:00' END,
       CASE WHEN component.`component_type` = 'THEORY' THEN '11:00:00' ELSE '15:00:00' END,
       CASE WHEN component.`component_type` = 'THEORY' THEN 'Technology Building - Room 1'
            ELSE 'Technology Building - Lab 1' END
FROM `timetables` timetable
JOIN `semesters` semester ON semester.`semester_id` = timetable.`semester_id`
JOIN `course_offerings` offering ON offering.`semester_id` = semester.`semester_id`
JOIN `course_components` component ON component.`course_id` = offering.`course_id`
WHERE semester.`academic_year` IN ('2025/2026', '2026/2027')
  AND NOT EXISTS (
      SELECT 1 FROM `timetable_entries` existing
      WHERE existing.`timetable_id` = timetable.`timetable_id`
        AND existing.`offering_id` = offering.`offering_id`
        AND existing.`component_id` = component.`component_id`
  );

INSERT INTO `attendance_sessions`
    (`offering_id`, `component_id`, `session_date`, `timetable_entry_id`, `recorded_by`)
SELECT offering.`offering_id`, component.`component_id`, dates.`session_date`,
       entry.`timetable_entry_id`, officer.`officer_id`
FROM (
    SELECT '2025/2026' AS `academic_year`, DATE('2025-09-01') AS `session_date`, 'MON' AS `day_of_week`
    UNION ALL SELECT '2025/2026', DATE('2025-09-03'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-09-08'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-09-10'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-09-15'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-09-17'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-09-22'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-09-24'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-09-29'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-10-01'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-10-06'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-10-08'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-10-13'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-10-15'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-10-20'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-10-22'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-10-27'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-10-29'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-11-03'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-11-05'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-11-10'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-11-12'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-11-17'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-11-19'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-11-24'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-11-26'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-12-01'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-12-03'), 'WED'
    UNION ALL SELECT '2025/2026', DATE('2025-12-08'), 'MON'
    UNION ALL SELECT '2025/2026', DATE('2025-12-10'), 'WED'
    UNION ALL SELECT '2026/2027', DATE('2026-09-01'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-09-03'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-09-08'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-09-10'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-09-15'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-09-17'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-09-22'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-09-24'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-09-29'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-10-01'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-10-06'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-10-08'), 'THU'
    UNION ALL SELECT '2026/2027', DATE('2026-10-13'), 'TUE'
    UNION ALL SELECT '2026/2027', DATE('2026-10-15'), 'THU'
) dates
JOIN `semesters` semester ON semester.`academic_year` = dates.`academic_year`
JOIN `course_offerings` offering ON offering.`semester_id` = semester.`semester_id`
JOIN `course_components` component ON component.`course_id` = offering.`course_id`
JOIN `timetable_entries` entry ON entry.`offering_id` = offering.`offering_id`
    AND entry.`component_id` = component.`component_id`
    AND entry.`day_of_week` = dates.`day_of_week`
JOIN `technical_officers` officer ON officer.`officer_id` = 1
WHERE NOT EXISTS (
    SELECT 1 FROM `attendance_sessions` existing
    WHERE existing.`offering_id` = offering.`offering_id`
      AND existing.`component_id` = component.`component_id`
      AND existing.`session_date` = dates.`session_date`
);

INSERT INTO `attendance_sessions`
    (`offering_id`, `component_id`, `session_date`, `timetable_entry_id`, `recorded_by`)
SELECT offering.`offering_id`, component.`component_id`, dates.`session_date`,
       entry.`timetable_entry_id`, officer.`officer_id`
FROM (
    SELECT DATE('2025-12-15') AS `session_date`, 'MON' AS `day_of_week`, 'TO002' AS `username`
    UNION ALL SELECT DATE('2025-12-17'), 'WED', 'TO002'
    UNION ALL SELECT DATE('2025-12-22'), 'MON', 'TO003'
    UNION ALL SELECT DATE('2025-12-24'), 'WED', 'TO003'
    UNION ALL SELECT DATE('2025-12-29'), 'MON', 'TO004'
    UNION ALL SELECT DATE('2025-12-31'), 'WED', 'TO004'
) dates
JOIN `semesters` semester ON semester.`academic_year` = '2025/2026'
JOIN `course_offerings` offering ON offering.`semester_id` = semester.`semester_id`
JOIN `course_components` component ON component.`course_id` = offering.`course_id`
JOIN `timetable_entries` entry ON entry.`offering_id` = offering.`offering_id`
    AND entry.`component_id` = component.`component_id`
    AND entry.`day_of_week` = dates.`day_of_week`
JOIN `technical_officers` officer ON TRUE
JOIN `users` user_account ON user_account.`user_id` = officer.`user_id`
    AND user_account.`username` = dates.`username`
WHERE NOT EXISTS (
    SELECT 1 FROM `attendance_sessions` existing
    WHERE existing.`offering_id` = offering.`offering_id`
      AND existing.`component_id` = component.`component_id`
      AND existing.`session_date` = dates.`session_date`
);

INSERT INTO `attendance_records` (`session_id`, `enrollment_id`, `is_present`)
SELECT session.`session_id`, enrollment.`enrollment_id`,
       CASE
           WHEN enrollment.`student_id` = 20
            AND session.`session_date` IN ('2026-09-15', '2026-09-29', '2026-09-17', '2026-10-01')
           THEN FALSE
           ELSE TRUE
       END
FROM `attendance_sessions` session
JOIN `course_enrollments` enrollment ON enrollment.`offering_id` = session.`offering_id`
WHERE NOT EXISTS (
    SELECT 1 FROM `attendance_records` existing
    WHERE existing.`session_id` = session.`session_id`
      AND existing.`enrollment_id` = enrollment.`enrollment_id`
);

INSERT INTO `medical_records`
    (`student_id`, `offering_id`, `medical_date`, `start_date`, `end_date`,
     `reason_details`, `approval_status`, `affects_eligibility`)
SELECT student.`student_id`, offering.`offering_id`, '2026-09-15', '2026-09-15', '2026-09-15',
       'Synthetic demo record for the medical review workflow.', 'APPROVED', TRUE
FROM `undergraduates` student
JOIN `course_offerings` offering ON offering.`offering_id` = 1
WHERE student.`student_no` = 'STU020'
  AND NOT EXISTS (
      SELECT 1 FROM `medical_records` existing
      WHERE existing.`student_id` = student.`student_id`
        AND existing.`offering_id` = offering.`offering_id`
        AND existing.`medical_date` = '2026-09-15'
  );

INSERT INTO `medical_records`
    (`student_id`, `offering_id`, `medical_date`, `start_date`, `end_date`,
     `reason_details`, `approval_status`, `affects_eligibility`)
SELECT student.`student_id`, offering.`offering_id`, '2026-09-18', '2026-09-18', '2026-09-18',
       'Synthetic pending demo record for the medical review workflow.', 'PENDING', FALSE
FROM `undergraduates` student
JOIN `course_offerings` offering ON offering.`offering_id` = 1
WHERE student.`student_no` = 'STU003'
  AND NOT EXISTS (
      SELECT 1 FROM `medical_records` existing
      WHERE existing.`student_id` = student.`student_id`
        AND existing.`offering_id` = offering.`offering_id`
        AND existing.`medical_date` = '2026-09-18'
  );

INSERT INTO `notices` (`title`, `body`, `audience`, `target_role`, `target_batch_id`,
                       `publish_date`, `expiry_date`)
SELECT 'ICT semester welcome', 'Synthetic demo notice for all users.', 'ALL', NULL, NULL,
       CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `notices` WHERE `title` = 'ICT semester welcome');

INSERT INTO `notices` (`title`, `body`, `audience`, `target_role`, `target_batch_id`,
                       `publish_date`, `expiry_date`)
SELECT 'Student assessment reminder', 'Synthetic demo notice for undergraduate users.',
       'ROLE', 'UNDERGRADUATE', NULL, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
WHERE NOT EXISTS (SELECT 1 FROM `notices` WHERE `title` = 'Student assessment reminder');

INSERT INTO `notices` (`title`, `body`, `audience`, `target_role`, `target_batch_id`,
                       `publish_date`, `expiry_date`)
SELECT 'ICT batch timetable update', 'Synthetic demo notice for the ICT 2025 batch.',
       'BATCH', NULL, batch.`batch_id`, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
FROM `batches` batch
WHERE batch.`batch_code` = 'FOT-ICT-2025'
  AND NOT EXISTS (SELECT 1 FROM `notices` WHERE `title` = 'ICT batch timetable update');
