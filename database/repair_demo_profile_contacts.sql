USE `university_management`;
START TRANSACTION;

ALTER TABLE undergraduates ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL;

UPDATE undergraduates s
JOIN users u ON u.user_id = s.user_id
SET s.full_name = CASE u.username
    WHEN 'ST002' THEN 'Amaya Perera' WHEN 'ST003' THEN 'Nethmi Silva'
    WHEN 'ST004' THEN 'Kavindu Fernando' WHEN 'ST005' THEN 'Dinuka Jayasuriya'
    WHEN 'ST006' THEN 'Tharushi Bandara' WHEN 'ST007' THEN 'Sahan Wijesinghe'
    WHEN 'ST008' THEN 'Anjali Rathnayake' WHEN 'ST009' THEN 'Pasindu Gunawardena'
    WHEN 'ST010' THEN 'Hiruni Karunaratne' WHEN 'ST011' THEN 'Chamod Dissanayake'
    WHEN 'ST012' THEN 'Piumi Abeysekara' WHEN 'ST013' THEN 'Ravindu Senanayake'
    WHEN 'ST014' THEN 'Kavisha De Silva' WHEN 'ST015' THEN 'Isuru Herath'
    WHEN 'ST016' THEN 'Sachini Amarasinghe' WHEN 'ST017' THEN 'Madhawa Wickramasinghe'
    WHEN 'ST018' THEN 'Shenali Ekanayake' WHEN 'ST019' THEN 'Dulaj Perera'
    WHEN 'ST020' THEN 'Oshini Fernando' ELSE s.full_name END
WHERE u.username BETWEEN 'ST002' AND 'ST020';

UPDATE users u JOIN undergraduates s ON s.user_id = u.user_id
SET u.email = CONCAT(u.username, '_', REPLACE(s.full_name, ' ', ''), '@fot.ac.lk')
WHERE u.username BETWEEN 'ST002' AND 'ST020';

UPDATE lecturers l JOIN users u ON u.user_id = l.user_id
SET l.full_name = CASE u.username WHEN 'LE001' THEN 'Nimal Perera' WHEN 'LE002' THEN 'Ishara Silva'
    WHEN 'LE003' THEN 'Kasun Fernando' WHEN 'LE004' THEN 'Malini Jayawardena'
    WHEN 'LE005' THEN 'Ruwan Senanayake' ELSE l.full_name END;
UPDATE users u JOIN lecturers l ON l.user_id = u.user_id
SET u.email = CONCAT(u.username, '_', REPLACE(l.full_name, ' ', ''), '@fot.ac.lk');

UPDATE technical_officers t JOIN users u ON u.user_id = t.user_id
SET t.full_name = CASE u.username WHEN 'TE001' THEN 'Nadeesha Perera' WHEN 'TE002' THEN 'Sampath Silva'
    WHEN 'TE003' THEN 'Chathuri Fernando' WHEN 'TE004' THEN 'Pradeep Kumara' ELSE t.full_name END;
UPDATE users u JOIN technical_officers t ON t.user_id = u.user_id
SET u.email = CONCAT(u.username, '_', REPLACE(t.full_name, ' ', ''), '@fot.ac.lk');

COMMIT;
