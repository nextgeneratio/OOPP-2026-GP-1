# UniMis demonstration dataset transformation

**Purpose:** manual replacement of `university_management` with a clearly
synthetic demonstration dataset. No SQL was executed against a database.

## Audited baseline

- Repository branch: `Amjad`; base commit before these local changes:
  `6a80ea1477af14fcbca427bb7f03535155bdf89f`.
- Configured database: `university_management`, MariaDB 11.8.8.
- Read-only count: 2,650 rows in 30 application tables. Every table uses InnoDB.
- The canonical schema has 30 tables, matching the live table/column inventory.
- No table/schema migration is required for this demonstration import.

## Source inventory and mapping

The source dump defines 19 tables and 9 derived views. Counts below are derived
by inspection of literal and generated INSERT statements; dynamic values were
not imported or executed. They describe the supplied dump, not official records.

| Source table | Source rows (derived) | Destination and treatment |
|---|---:|---|
| `departments` | 1 | `departments`: create ICT, BST, ET. BST/ET course placement is explicitly provisional demo classification. |
| `users` | 30 | `users`: replace all identities with 30 synthetic DEMO accounts and unique app-compatible PBKDF2 hashes. Source names, contacts, addresses, IDs and plaintext passwords are discarded. |
| `admins` | 1 | `admins`: one synthetic `DEMO_ADMIN` profile. |
| `lecturers` | 5 | `lecturers`: five synthetic accounts and profiles; course assignments are preserved by source course-to-lecturer relationships. |
| `technical_officers` | 4 | `technical_officers`: four synthetic profiles assigned to demo departments. |
| `undergraduates` | 20 | `undergraduates`: twenty synthetic students retain the source sequence needed for deterministic relationship/score formulas; identifiers and PII are replaced. |
| `courses` | 9 | `courses`: retain codes and compatible titles. ICT2142 uses the established application title “Database Systems”; ICT2132/ICT2142 credits and components follow the application seed. Other course details follow the source. |
| `course_lecturers` | 9 | `course_offering_lecturers` and each offering coordinator: mapped using course codes and synthetic lecturer usernames. |
| `course_materials` | 2 | Source file references are excluded because the referenced PDFs were not supplied. Replaced by 45 verified generated PDFs. |
| `enrollments` | 175 | `course_enrollments`: preserve source course/student relationship exclusions and NORMAL/REPEAT/BATCH_MISSED categories, remapped to synthetic students. |
| `assessments` | 54 | `assessments`: 36 rows. The three half-weight quiz rows are represented by one aggregate quiz assessment at the source quiz weight; other assignment, midterm theory/practical, and final theory/practical components remain separate. Every plan totals exactly 100%. |
| `marks` | 1,053 | `marks`: 528 source formula-generated non-quiz scores are mapped to synthetic enrollments. Quiz scores are not put in `marks` because the source best-two-of-three rule is unsupported by the app’s final-mark calculation. |
| `attendance_sessions` | 150 | Source sessions are excluded because the generated schedule extends into future dates and does not consistently map to the target timetable. Replaced by 12 synthetic demo sessions. |
| `attendance_records` | 2,925 | Source attendance history is excluded. Replaced by 234 deterministic records for synthetic accounts only. |
| `medicals` | Up to 10 | Excluded; the records claim approval/evidence but no evidence files were supplied. No medical records are fabricated. |
| `notices` | 3 | Replaced by one clearly synthetic demonstration notice. |
| `timetable_entries` | 9 | Replaced by 12 synthetic entries, one for each target course component, with matching demo attendance dates. |
| `grade_scales` | 12 | Source thresholds are excluded because they conflict with the canonical scheme. Insert the 12 verified `UGC-2024-v1` rows from the application seed. |
| `system_settings` | 8 | Excluded; the application has no corresponding table. App configuration remains in its existing configuration files. |
| 9 source views | Derived | Excluded; views are not destination data and results are not persisted source records. |

## Explicit demonstration assumptions

- Academic period: `2026/2027`, semester 1, `2026-09-01` through `2027-01-31`.
  This follows the project seed and is labeled synthetic, not an official date
  claim.
- Course departments: ICT-prefixed codes map to ICT; TCS2112 (Business
  Economics) and TCS2121 (Soft Skills) map provisionally to BST; ENG2112 maps
  provisionally to ET. These are demo classifications, not verified university
  assignments. The courses are offered to a synthetic ICT demonstration cohort.
- The source title ICT2142 “E-Business Systems” conflicts with the application
  catalog. The import uses canonical “Database Systems.” Materials were
  regenerated to match that title and subject.
- The source’s three quiz records each carry half of `quiz_weight`; its
  best-two calculation yields one quiz-weight aggregate. The replacement keeps
  a single aggregate QUIZ plan weight and documents that best-two scoring is
  unsupported. Source quiz scores/attempts and final course results are omitted
  rather than misrepresented as ordinary marks.
- Non-quiz source marks are generated by explicit deterministic SQL formulas
  (scores 88, 78, 68, 52, or 42 based on the synthetic student sequence). They
  are demonstration scores, not official student marks. No final results are
  inserted because the quiz aggregation rule cannot be reproduced.
- Attendance rows are deterministic test scenarios for DEMO accounts only.
  Their dates are in the synthetic term and match synthetic timetable entries.
- No source personal details, medical cases, or unsupported file references
  are included.

## Expected imported row counts

| Table | Rows | Table | Rows |
|---|---:|---|---:|
| `users` | 30 | `admins` | 1 |
| `departments` | 3 | `batches` | 1 |
| `undergraduates` | 20 | `lecturers` | 5 |
| `technical_officers` | 4 | `courses` | 9 |
| `course_components` | 12 | `semesters` | 1 |
| `course_offerings` | 9 | `course_offering_lecturers` | 9 |
| `course_enrollments` | 175 | `assessments` | 36 |
| `marks` | 528 | `attendance_sessions` | 12 |
| `attendance_records` | 234 | `medical_records` | 0 |
| `course_materials` | 45 | `course_quizzes` | 0 |
| `quiz_questions` | 0 | `quiz_attempts` | 0 |
| `course_assignments` | 0 | `assignment_submissions` | 0 |
| `notices` | 1 | `timetables` | 3 |
| `timetable_entries` | 12 | `grades` | 12 |
| `course_results` | 0 | `audit_logs` | 0 |

**Expected total:** 1,162 rows across the 30 application tables.

## Credentials and material files

- The script stores only PBKDF2 hashes generated and verified using the
  application `PasswordHasher` (210,000 iterations). It contains no plaintext
  passwords.
- Unique synthetic credentials are stored outside the repository at
  `/home/Amjad/.local/share/ams-demo/credentials.csv` (directory mode 700, file
  mode 600). Do not commit or share this file publicly.
- The 45 PDFs and exact offering/uploader/path mapping are listed in
  `docs/generated-course-materials-manifest.csv`. Every registered path exists
  below `src/main/resources/uploads` and is marked visible only after file
  verification.

## Manual import safety

The SQL file is stored at
`/home/Amjad/.local/share/ams-demo/manual-demo-replacement.sql` and is also
provided in the chat. Its procedure verifies the selected database, runs
child-before-parent `DELETE` and inserts within one transaction with foreign-key
checks enabled, rolls back on a SQL exception, and is removed after the call.
All target tables were confirmed InnoDB. If the final status variable is `1`,
the transaction rolled back; do not rely on the subsequent row counts. The SQL
was only statically validated and has not been executed.
