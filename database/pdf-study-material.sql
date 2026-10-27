-- Link the in-project ICT2132 PDF study material to the current open ICT2132 offering.
-- The PDF is stored at src/main/resources/uploads/demo-materials/ICT2132/lecture-notes.pdf.
-- Run only if the matching material is absent or points at a different path.
USE university_management;

INSERT INTO course_materials (offering_id, lecturer_id, title, file_path, is_visible)
SELECT o.offering_id, o.coordinator_id, 'ICT2132 - Lecture Notes',
       'uploads/demo-materials/ICT2132/lecture-notes.pdf', TRUE
FROM course_offerings o JOIN courses c ON c.course_id = o.course_id
WHERE c.course_code = 'ICT2132' AND o.status = 'OPEN' AND o.coordinator_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM course_materials m
      WHERE m.offering_id = o.offering_id AND m.title = 'ICT2132 - Lecture Notes'
  );

UPDATE course_materials m
JOIN course_offerings o ON o.offering_id = m.offering_id
JOIN courses c ON c.course_id = o.course_id
SET m.file_path = 'uploads/demo-materials/ICT2132/lecture-notes.pdf', m.is_visible = TRUE
WHERE c.course_code = 'ICT2132' AND o.status = 'OPEN'
  AND m.title = 'ICT2132 - Lecture Notes';
