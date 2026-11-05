USE `university_management`;

-- Replace attendance only for sessions attached to synthetic demo timetables.
-- Every demo offering gets 15 sessions; each enrolled student attends 9-15
-- randomly selected sessions per course. Safe to rerun.
START TRANSACTION;

UPDATE semesters sem
JOIN course_offerings o ON o.semester_id = sem.semester_id
JOIN batches b ON b.batch_id = o.batch_id AND b.batch_code = 'DEMO-ICT-2026'
SET sem.start_date = '2026-07-13', sem.end_date = '2027-01-31'
WHERE NOT EXISTS (
    SELECT 1
    FROM course_offerings other_o
    JOIN batches other_b ON other_b.batch_id = other_o.batch_id
    WHERE other_o.semester_id = sem.semester_id
      AND other_b.batch_code <> 'DEMO-ICT-2026'
);

DELETE ar
FROM attendance_records ar
JOIN attendance_sessions s ON s.session_id = ar.session_id
JOIN timetable_entries te ON te.timetable_entry_id = s.timetable_entry_id
JOIN timetables t ON t.timetable_id = te.timetable_id
WHERE t.title LIKE 'DEMO %';

DELETE s
FROM attendance_sessions s
JOIN timetable_entries te ON te.timetable_entry_id = s.timetable_entry_id
JOIN timetables t ON t.timetable_id = te.timetable_id
WHERE t.title LIKE 'DEMO %';

INSERT INTO attendance_sessions
    (offering_id, component_id, session_date, timetable_entry_id, recorded_by, recorded_by_lecturer)
SELECT schedule.offering_id,
       schedule.component_id,
       DATE_ADD('2026-07-13', INTERVAL (
           FLOOR(FLOOR((n.session_no - 1) / schedule.entry_count) * 14 /
               (FLOOR((15 + schedule.entry_count - schedule.entry_no) / schedule.entry_count) - 1)) * 7
           + CASE schedule.day_of_week
               WHEN 'MON' THEN 0 WHEN 'TUE' THEN 1 WHEN 'WED' THEN 2
               WHEN 'THU' THEN 3 WHEN 'FRI' THEN 4
             END
       ) DAY),
       schedule.timetable_entry_id,
       toff.officer_id,
       NULL
FROM (
    SELECT e.offering_id,
           e.component_id,
           e.day_of_week,
           e.timetable_entry_id,
           e.timetable_id,
           ROW_NUMBER() OVER (PARTITION BY e.offering_id ORDER BY e.timetable_entry_id) AS entry_no,
           COUNT(*) OVER (PARTITION BY e.offering_id) AS entry_count
    FROM timetable_entries e
    JOIN timetables t ON t.timetable_id = e.timetable_id
    WHERE t.title LIKE 'DEMO %'
) schedule
JOIN timetables t ON t.timetable_id = schedule.timetable_id
JOIN departments d ON d.department_id = t.department_id
JOIN users u ON u.username = CASE d.code WHEN 'ICT' THEN 'TE001' WHEN 'BST' THEN 'TE002' ELSE 'TE003' END
JOIN technical_officers toff ON toff.user_id = u.user_id
JOIN (
    SELECT 1 AS session_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
    UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
) n ON schedule.entry_no = MOD(n.session_no - 1, schedule.entry_count) + 1;

UPDATE course_components cc
JOIN (
    SELECT s.component_id, COUNT(*) AS planned_sessions
    FROM attendance_sessions s
    JOIN timetable_entries te ON te.timetable_entry_id = s.timetable_entry_id
    JOIN timetables t ON t.timetable_id = te.timetable_id
    WHERE t.title LIKE 'DEMO %'
    GROUP BY s.component_id
) session_counts ON session_counts.component_id = cc.component_id
SET cc.planned_sessions = session_counts.planned_sessions;

INSERT INTO attendance_records (session_id, enrollment_id, is_present)
SELECT randomized.session_id,
       randomized.enrollment_id,
       randomized.attendance_rank <= randomized.attendance_target
FROM (
    -- Use a stable pseudo-random numeric key so the row-number windows work on MySQL 9.6.
    SELECT s.session_id,
           e.enrollment_id,
           ROW_NUMBER() OVER (
               PARTITION BY s.offering_id, e.enrollment_id
               ORDER BY CAST(CRC32(CONCAT(s.session_id, ':', e.enrollment_id, ':demo-attendance')) AS UNSIGNED)
                        * 4294967296 + CAST(s.session_id AS UNSIGNED)
           ) AS attendance_rank,
           targets.attendance_target
    FROM attendance_sessions s
    JOIN course_enrollments e ON e.offering_id = s.offering_id
    JOIN (
        SELECT assigned.enrollment_id,
               assigned.offering_id,
               CASE WHEN assigned.course_rank = 1 THEN 15
                    ELSE 9 + FLOOR(RAND() * 7) END AS attendance_target
        FROM (
            SELECT e.enrollment_id,
                   e.offering_id,
                   e.student_id,
                   ROW_NUMBER() OVER (
                       PARTITION BY e.student_id
                       ORDER BY CAST(CRC32(CONCAT(e.enrollment_id, ':', e.offering_id, ':demo-course')) AS UNSIGNED)
                                * 4294967296 + e.enrollment_id
                   ) AS course_rank
            FROM course_enrollments e
            JOIN course_offerings o ON o.offering_id = e.offering_id
            JOIN batches b ON b.batch_id = o.batch_id AND b.batch_code = 'DEMO-ICT-2026'
        ) assigned
    ) targets ON targets.enrollment_id = e.enrollment_id
             AND targets.offering_id = e.offering_id
    JOIN timetable_entries te ON te.timetable_entry_id = s.timetable_entry_id
    JOIN timetables t ON t.timetable_id = te.timetable_id
    WHERE t.title LIKE 'DEMO %'
) randomized;

COMMIT;
