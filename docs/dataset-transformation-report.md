# Demonstration dataset and transformations

The demonstration data in `database/install.sql` is synthetic. It is not an
official University of Ruhuna student or staff dataset. The importer uses
course codes and academic workflow scenarios from the existing transformed
dataset while excluding source personal details, addresses, identifiers, and
plaintext passwords.

## Seeded populations

- 1 Administrator: `admin`.
- 20 Undergraduates: `ST001`–`ST020`.
- 4 Technical Officers: `TE001`–`TE004`.
- 9 Lecturers: `LE001`–`LE009`, with one lecturer assigned to each course offering per semester.
- 3 synthetic departments, 1 batch, 9 courses, 12 course components, and one
  semester (`2026/2027`, semester 1).
- 9 course offerings, lecturer assignments, and 180 course enrollments; every
  undergraduate is registered in every offering.
- 36 assessment plan rows. Every enrolment receives all 26 coursework/CA marks;
  final exam marks are added only for students passing CA (at least 40%) and
  attendance (at least 80% in each course component). The final mark-row count
  varies with randomized attendance.
- 135 attendance sessions (15 per offering) and 2,700 synthetic attendance
  records. Each student attends 9-15 sessions per course; one randomly selected
  course per student has full attendance, so every undergraduate can view at
  least one finalized result when their CA mark passes.
- 3 timetables with 12 timetable entries.
- Finalized results for eligible demo enrolments. The exact count varies with
  randomized attendance; results are available to SGPA/CGPA calculations.
- 12 `UGC-2024-v1` grade boundaries and one synthetic notice.
- 45 generated PDF learning materials, five for each of nine courses.

The installer leaves medical records, quiz definitions and attempts, course
assignments and submissions, and audit history empty. In particular, medical
evidence is not fabricated. Source quiz best-two-of-three scores are omitted
because that calculation is not implemented in the application result
workflow; a synthetic aggregate quiz assessment and marks are used for the
demo's finalized-result workflow instead.

## Academic assumptions

The academic period is `2026/2027`, semester 1, with dates 2026-07-13 through
2027-01-31. The course placement of TCS2112 (Business Economics), TCS2121 (Soft
Skills), and ENG2112 to departments BST/ET is provisional demonstration data,
not an official faculty mapping. Courses are offered to a synthetic ICT batch.
ICT2142 uses the application catalog title “Database Systems.” Marks are
deterministic examples; attendance is randomized per enrollment. Neither
represents actual student records.

## Passwords and generated materials

All 34 accounts use the local demonstration password `1234`. The installer
stores a separate PBKDF2 hash per account, generated with the existing
`PBKDF2WithHmacSHA256` algorithm, 210,000 iterations, a unique 16-byte salt, and
a 256-bit derived key. The hashes were checked using the application's
`PasswordHasher.verify()` implementation. The normal eight-character password
policy for newly created accounts and password changes is unchanged.

The resource list in
[`generated-course-materials-manifest.csv`](./generated-course-materials-manifest.csv)
maps all 45 PDFs to their course offering, lecturer uploader, file path, and
visibility. Each path is relative to `src/main/resources` and resolves to a
tracked PDF under `src/main/resources/uploads/materials/demo-readings/`.

## Installer safety

[`../database/install.sql`](../database/install.sql) is the only user-facing
database installation script. It creates the database, complete 30-table
InnoDB schema, and local JDBC account. Its schema preflight runs before data
deletion. It then deletes rows child-first and inserts the demonstration data
inside one transaction with foreign-key checks enabled. An SQL exception rolls
back that DML transaction and is returned to the SQL client. Database, account,
table, and temporary procedure DDL run before the transaction and are not
rolled back by it. The installer is intentionally destructive to application
rows and must be preceded by a backup when a target database has data to keep.
