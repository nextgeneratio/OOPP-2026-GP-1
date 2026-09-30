# Instruction 01 — Foundation, Architecture, Database and Core Domain

## Ready-to-paste prompt

You are continuing development of our Java 17+ Swing + MySQL Faculty of Technology Academic Management System in IntelliJ IDEA.

**Read these first:**
1. The SRS.
2. `progress.md`.
3. The project structure already created from `00-project-structure-prompt.md`.

Do not restart or redesign the project from scratch. Continue from the current repository state.

## Goal of this phase

Build the technical foundation so every later module can follow the required layered architecture.

The required dependency chain is:

`Swing UI → API/Controller → Business → Data/DAO → MySQL`

with centralized error handling and security/audit support.

The API layer is an internal desktop application API; do not introduce REST/HTTP.

## Step 1 — Maven and IntelliJ setup

Configure the Maven project for Java 17+.

Required dependencies:

- MySQL Connector/J.
- JUnit 5.
- Mockito.
- AssertJ.
- SLF4J + Logback.
- FlatLaf.
- AssertJ Swing optionally for later UI tests.

Configure the Maven Surefire plugin so tests run normally from IntelliJ and Maven.

Create `.gitignore` entries for:

- IDE metadata where appropriate.
- `target/`.
- local database configuration files containing passwords.
- generated logs.
- user-uploaded files.

Create:

`src/main/resources/application.properties.example`

Never hard-code database credentials in Java source or commit private credentials.

## Step 2 — Application bootstrap

Create:

`Application.java`

It should:

1. Configure logging.
2. Load application configuration.
3. Initialize database connection provider.
4. Initialize core repositories/DAOs.
5. Initialize business services.
6. Initialize controllers/API handlers.
7. Initialize the Swing look and feel.
8. Open the login screen on the Swing Event Dispatch Thread.

Do not construct the entire application as one giant method. Use small factories/builders/configuration classes.

## Step 3 — Configuration and database connection

Create a configuration component that loads:

- database URL,
- username,
- password,
- connection settings,
- application environment.

Create:

`ConnectionProvider`

and a transaction utility/manager.

Requirements:

- JDBC.
- Prepared statements only.
- `try-with-resources`.
- Clear handling of unavailable database.
- No connection creation in Swing views.
- No SQL in business classes.

## Step 4 — Database schema

Create `src/main/resources/db/schema.sql` based on the SRS.

Implement all major tables required by the SRS, including:

- users
- admins
- departments
- batches
- undergraduates
- lecturers
- technical_officers
- courses
- course_components
- semesters
- course_offerings
- course_enrollments
- assessments
- marks
- attendance_sessions
- attendance_records
- medical_records
- course_materials
- notices
- timetables
- timetable_entries
- grades
- audit_logs

Preserve the SRS constraints:

- unique username.
- unique email.
- unique student number.
- unique department code.
- unique course code.
- unique course offering combination.
- unique enrollment `(student_id, offering_id, attempt_no)`.
- unique assessment `(offering_id, name)`.
- unique mark `(assessment_id, enrollment_id)`.
- unique attendance record `(session_id, enrollment_id)`.
- relevant foreign-key indexes.
- marks 0–100.
- assessment weights 0–100.
- credits > 0.
- valid date/time constraints.
- controlled enum/status values.
- appropriate RESTRICT/CASCADE behavior.

Use soft deactivation where the SRS requires historical records to be preserved.

## Step 5 — Seed data skeleton

Create `seed.sql`.

At this stage, seed enough master data to let development begin:

- 1 Admin.
- at least 5 Lecturers.
- at least 4 Technical Officers.
- at least 20 Undergraduates.
- departments.
- batches.
- semesters.
- courses.
- course components.
- course offerings.
- basic enrolments.
- grade scheme rows.

Use the SRS Appendix A scenarios later for exact attendance demonstrations.

Never store plaintext passwords. Generate valid hashes.

## Step 6 — Domain model

Create the core domain classes and enums.

Ensure private fields and validation.

Examples:

- `Mark.setScore()` rejects invalid values.
- `Assessment.setWeight()` rejects invalid percentages.
- course credit validation rejects non-positive values.
- timetable times must satisfy start < end.
- medical ranges must be valid.
- statuses use enums rather than arbitrary strings inside Java code.

Implement the inheritance requirement:

```text
User
├── Admin
├── Lecturer
├── TechnicalOfficer
└── Undergraduate
```

Implement suitable interfaces such as:

- `Repository<T>`
- `Authenticatable`
- `EligibilityPolicy`

Do not couple domain objects to JDBC or Swing.

## Step 7 — Error handling foundation

Create:

- `ValidationException`
- `AuthenticationException`
- `AuthorizationException`
- `NotFoundException`
- `DataAccessException`
- `BusinessRuleException`

Create centralized safe error translation.

Examples from the SRS:

- invalid login → “Username or password is incorrect.”
- duplicate identity → meaningful duplicate-field message.
- database failure → safe connection/database message, no SQL.
- unauthorized operation → “You do not have permission for this action.”
- missing record → “The requested record is unavailable.”

Technical details belong in logs, not normal GUI messages.

## Step 8 — Security foundation

Create password hashing and password verification utilities.

Rules:

- random salt per password.
- secure iteration/work-factor configuration.
- constant-time verification where applicable.
- never log passwords.
- never retain plaintext password unnecessarily.
- session object contains only safe authenticated-user information.

Create a basic `AuthorizationService` or equivalent service-level guard.

Authorization MUST be checked by services, not only by the UI.

## Step 9 — Test the foundation

Create unit tests for:

- domain validation.
- password hashing/verification.
- custom exception behavior.
- authorization decisions.

Create an integration test that verifies:

- application can connect to MySQL.
- schema can be created.
- a known seed row can be read.

Keep test configuration separate from production configuration.

## Step 10 — UI foundation only

Do not build all screens yet.

Create only:

- base theme.
- main frame/navigation shell.
- reusable form/table/button components.
- basic login frame skeleton.

Apply the defined visual system:

Primary `#123B5D`, hover `#1D5A87`, secondary `#2F7D8C`, accent `#D9A441`, background `#F5F7FA`, surface `#FFFFFF`, text `#1F2937`, error `#C62828`, success `#2E7D32`.

Typography:

- titles 24–28 px bold.
- section headings 18–20 px.
- body 14 px.
- table text 13 px.
- helper/error text 12–13 px.

Use system-safe fallbacks.

## Required outputs of Phase 1

Before finishing:

- project compiles.
- database schema executes.
- seed data executes.
- foundation unit tests pass.
- MySQL integration connectivity test passes.
- base Swing theme launches.
- no layer has illegal dependencies.
- `progress.md` is updated.

## Mandatory progress.md update

Record:

- completed architecture.
- database status.
- domain objects created.
- tests executed and results.
- known issues.
- exact next task for Phase 2.

Do not claim Phase 1 complete when an essential item is broken.
