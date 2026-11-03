-- Give every demo course offering a different lecturer in its semester.
-- Safe for reruns: creates missing LE006-LE009 accounts and reapplies the same assignments.
USE `university_management`;

DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_assignment`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_assignment_update`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_coordinator_insert`;
DROP TRIGGER IF EXISTS `trg_one_course_per_lecturer_coordinator_update`;

INSERT INTO `users` (username,password_hash,role,email,is_active,created_at) VALUES
    ('LE006','PBKDF2$210000$VgF0h+jeg1L8Z4WVVEnjLA==$hk83ZsFYzpFuG/OJQ7NsFqyES7nRQagkQCt3ss8OWPI=','LECTURER','LE006_Demo_Lecturer_006@fot.ac.lk',TRUE,NOW()),
    ('LE007','PBKDF2$210000$VgF0h+jeg1L8Z4WVVEnjLA==$hk83ZsFYzpFuG/OJQ7NsFqyES7nRQagkQCt3ss8OWPI=','LECTURER','LE007_Demo_Lecturer_007@fot.ac.lk',TRUE,NOW()),
    ('LE008','PBKDF2$210000$VgF0h+jeg1L8Z4WVVEnjLA==$hk83ZsFYzpFuG/OJQ7NsFqyES7nRQagkQCt3ss8OWPI=','LECTURER','LE008_Demo_Lecturer_008@fot.ac.lk',TRUE,NOW()),
    ('LE009','PBKDF2$210000$VgF0h+jeg1L8Z4WVVEnjLA==$hk83ZsFYzpFuG/OJQ7NsFqyES7nRQagkQCt3ss8OWPI=','LECTURER','LE009_Demo_Lecturer_009@fot.ac.lk',TRUE,NOW())
ON DUPLICATE KEY UPDATE role=VALUES(role),email=VALUES(email),is_active=TRUE;

INSERT INTO `lecturers` (user_id,department_id,full_name,phone,designation)
SELECT u.user_id,d.department_id,CONCAT('Demo Lecturer ',RIGHT(u.username,3)),NULL,
       'Synthetic demonstration lecturer'
FROM `users` u
JOIN `departments` d ON d.code=CASE u.username
    WHEN 'LE008' THEN 'BST' WHEN 'LE009' THEN 'ET' ELSE 'ICT' END
WHERE u.username IN ('LE006','LE007','LE008','LE009')
  AND NOT EXISTS (SELECT 1 FROM `lecturers` l WHERE l.user_id=u.user_id);

UPDATE `lecturers` lecturer
JOIN `users` account ON account.user_id=lecturer.user_id
JOIN `departments` department ON department.code=CASE account.username
    WHEN 'LE008' THEN 'BST' WHEN 'LE009' THEN 'ET' ELSE 'ICT' END
SET lecturer.department_id=department.department_id,
    lecturer.full_name=CONCAT('Demo Lecturer ',RIGHT(account.username,3))
WHERE account.username IN ('LE006','LE007','LE008','LE009');

CREATE TEMPORARY TABLE `demo_lecturer_course_map` (
    course_code VARCHAR(20) PRIMARY KEY,
    lecturer_username VARCHAR(20) NOT NULL
);
INSERT INTO `demo_lecturer_course_map` VALUES
    ('ICT2113','LE001'),('ICT2122','LE002'),('ICT2132','LE004'),
    ('ICT2142','LE005'),('ICT2152','LE006'),('ICT2162','LE007'),
    ('TCS2112','LE003'),('TCS2121','LE008'),('ENG2112','LE009');

START TRANSACTION;

UPDATE `course_offerings` offering
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
SET offering.coordinator_id=NULL
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

DELETE assigned
FROM `course_offering_lecturers` assigned
JOIN `course_offerings` offering ON offering.offering_id=assigned.offering_id
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

UPDATE `course_offerings` offering
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
JOIN `users` account ON account.username=mapping.lecturer_username
JOIN `lecturers` lecturer ON lecturer.user_id=account.user_id
SET offering.coordinator_id=lecturer.lecturer_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

INSERT INTO `course_offering_lecturers` (offering_id,lecturer_id)
SELECT offering.offering_id,lecturer.lecturer_id
FROM `course_offerings` offering
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
JOIN `users` account ON account.username=mapping.lecturer_username
JOIN `lecturers` lecturer ON lecturer.user_id=account.user_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

UPDATE `course_materials` item
JOIN `course_offerings` offering ON offering.offering_id=item.offering_id
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
JOIN `users` account ON account.username=mapping.lecturer_username
JOIN `lecturers` lecturer ON lecturer.user_id=account.user_id
SET item.lecturer_id=lecturer.lecturer_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

UPDATE `course_quizzes` item
JOIN `course_offerings` offering ON offering.offering_id=item.offering_id
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
JOIN `users` account ON account.username=mapping.lecturer_username
JOIN `lecturers` lecturer ON lecturer.user_id=account.user_id
SET item.lecturer_id=lecturer.lecturer_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

UPDATE `course_assignments` item
JOIN `course_offerings` offering ON offering.offering_id=item.offering_id
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `demo_lecturer_course_map` mapping ON mapping.course_code=course.course_code
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
JOIN `users` account ON account.username=mapping.lecturer_username
JOIN `lecturers` lecturer ON lecturer.user_id=account.user_id
SET item.lecturer_id=lecturer.lecturer_id
WHERE semester.academic_year='2026/2027' AND semester.semester_no=1;

COMMIT;

DROP TEMPORARY TABLE `demo_lecturer_course_map`;

-- Enforce one responsible lecturer per semester for future direct SQL edits too.
DELIMITER //
CREATE TRIGGER `trg_one_course_per_lecturer_assignment`
BEFORE INSERT ON `course_offering_lecturers` FOR EACH ROW
BEGIN
    DECLARE target_semester INT;
    SELECT semester_id INTO target_semester FROM course_offerings WHERE offering_id=NEW.offering_id;
    IF EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=target_semester
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.lecturer_id OR other_assignment.lecturer_id=NEW.lecturer_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_assignment_update`
BEFORE UPDATE ON `course_offering_lecturers` FOR EACH ROW
BEGIN
    DECLARE target_semester INT;
    SELECT semester_id INTO target_semester FROM course_offerings WHERE offering_id=NEW.offering_id;
    IF EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=target_semester
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.lecturer_id OR other_assignment.lecturer_id=NEW.lecturer_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_coordinator_insert`
BEFORE INSERT ON `course_offerings` FOR EACH ROW
BEGIN
    IF NEW.coordinator_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=NEW.semester_id
          AND (other_offering.coordinator_id=NEW.coordinator_id OR other_assignment.lecturer_id=NEW.coordinator_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
CREATE TRIGGER `trg_one_course_per_lecturer_coordinator_update`
BEFORE UPDATE ON `course_offerings` FOR EACH ROW
BEGIN
    IF NEW.coordinator_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM course_offerings other_offering
        LEFT JOIN course_offering_lecturers other_assignment
          ON other_assignment.offering_id=other_offering.offering_id
        WHERE other_offering.semester_id=NEW.semester_id
          AND other_offering.offering_id<>NEW.offering_id
          AND (other_offering.coordinator_id=NEW.coordinator_id OR other_assignment.lecturer_id=NEW.coordinator_id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A lecturer can be responsible for only one course per semester.';
    END IF;
END//
DELIMITER ;

SELECT account.username,course.course_code,semester.academic_year,semester.semester_no
FROM `course_offering_lecturers` assigned
JOIN `lecturers` lecturer ON lecturer.lecturer_id=assigned.lecturer_id
JOIN `users` account ON account.user_id=lecturer.user_id
JOIN `course_offerings` offering ON offering.offering_id=assigned.offering_id
JOIN `courses` course ON course.course_id=offering.course_id
JOIN `semesters` semester ON semester.semester_id=offering.semester_id
ORDER BY account.username,semester.semester_id,course.course_code;

SELECT COUNT(*) AS lecturers_assigned_to_multiple_courses_in_a_semester
FROM (
    SELECT lecturer_id,semester_id
    FROM (
        SELECT offering.semester_id,COALESCE(assigned.lecturer_id,offering.coordinator_id) AS lecturer_id,
               offering.offering_id
        FROM `course_offerings` offering
        LEFT JOIN `course_offering_lecturers` assigned ON assigned.offering_id=offering.offering_id
    ) assignments
    WHERE lecturer_id IS NOT NULL
    GROUP BY lecturer_id,semester_id
    HAVING COUNT(DISTINCT offering_id)>1
) duplicates;
