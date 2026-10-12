# Project Progress

## Current phase
Phase 0 — Project setup / planning

## Completed
- [x] Project structure created
- [x] Maven configuration
- [x] Database schema
- [x] Seed data
- [x] Domain model
- [x] Error handling
- [x] Security/authentication
- [x] Data/DAO layer
- [x] Business logic layer
- [x] API/controller layer
- [x] Swing UI (Foundation)
- [x] Phase 1: Architecture Audit & Code Decoupling
- [ ] Unit tests (Full coverage)
- [ ] Integration tests (Full coverage)
- [ ] System tests
- [ ] Reports
- [ ] Audit logging

## In progress
- Phase 2: Domain Unit Tests & Boundary Values

## Blocked
- None

## Tests
- Unit: 9 passed
- Integration: 1 skipped (live MySQL not configured)
- System: 0

## Database
- Schema: Available at src/main/resources/db/schema.sql
- Seed data: Available at src/main/resources/db/seed.sql
- Test database: Available at src/test/resources/db/test-schema.sql
- Connection: JdbcConnectionProvider with AppConfig

## Architecture status
- Presentation: Clean (No JDBC/SQL imports, uses Controllers)
- API: Clean (Thin controllers, no hidden calculations)
- Business: Clean (Pure services, testable without Swing/GUI)
- Data: Clean (JDBC DAOs with PreparedStatements, no Swing imports)
- Error: Clean (Centralized ApplicationErrorHandler mapping exceptions safely)
- Security: Clean (Salted PBKDF2 hashing, session-based authorization, no credentials in git)
- Audit: Scaffolded
- Reporting: Scaffolded

## SRS traceability
- FR-001 to FR-014: Foundation in place
- FR-015/FR-016: Scaffolded
- BR-001 to BR-015: Scaffolded
- NFR-001 to NFR-014: Scaffolded

## Known issues
- None

## Files changed
- `mvn.cmd`: Created wrapper ensuring JDK 21 runtime
- `src/test/java/lk/ac/ruhuna/fot/ams/integration/DatabaseConnectivityTest.java`: Fixed assumption check
- `progress.md`: Updated architecture audit status

## Next task
Execute Phase 2: Domain Unit Tests & Boundary Values.

## Last updated
2026-10-13

## Structure Execution Update — 2026-09-30
- Executed `00-project-structure-prompt.md` only.
- Created the Maven project descriptor with Java 17 release settings and required dependency declarations.
- Created the Maven source/resource/test layout and the `lk.ac.ruhuna.fot.ams` package structure.
- Added architecture, database-design, and testing-strategy documentation placeholders.
- Added configuration examples, logging configuration, database/script placeholders, and test resource placeholders.
- Added ignore rules for build output, local credentials, logs, and user uploads.
- No Phase 1 implementation was started.
- Validation: Maven validation was attempted but could not run because `mvn` is not installed in the environment.
- Next task: await instruction to begin `01-instruction-foundation.md`.

## Phase 1 Foundation Update — 2026-09-30
- Read `progress.md` before starting Phase 1 execution.
- Implemented configuration loading with environment-variable support and ignored local credentials.
- Implemented JDBC connection and transaction abstractions using prepared-statement-compatible connection access.
- Imported the supplied MySQL schema into `src/main/resources/db/schema.sql` and deferred the attendance-session timetable foreign key until its parent table exists.
- Added development seed data for 1 Admin, 5 Lecturers, 4 Technical Officers, 20 Undergraduates, master data, offerings, enrolments, repeat/batch-missed attempts, and grade rows.
- Seed accounts use salted PBKDF2 hashes; no plaintext password was added to project files.
- Implemented the `User` inheritance hierarchy, role/status enums, repository/authentication/eligibility interfaces, and validated core domain objects for users, marks, assessments, courses, and medical records.
- Implemented application exceptions, safe GUI error translation, password hashing/verification, authenticated sessions, and service-layer authorization guards.
- Implemented a JDBC `UserDao`, authentication controller, Swing theme, reusable button, main frame, login frame, and EDT application bootstrap.
- Added foundation unit tests for domain validation, password hashing, and authorization, plus a MySQL connectivity integration test.
- Validation: foundation Java sources compile successfully with the available JDK.
- Validation blocked: Maven is not installed, Maven dependencies are not cached, and full JUnit/Maven test execution could not run.
- Validation blocked: live MySQL schema/seed execution could not be confirmed because the workspace `.envrc` fails while sourcing `.env/bin/activate`; no database changes were executed by the agent.
- Phase 1 is not marked complete until Maven tests, schema execution, seed execution, and MySQL integration connectivity are verified.
- Next task: install/enable Maven and repair or bypass the workspace direnv setup, then run foundation tests and execute schema/seed against a dedicated test database.

## Phase 1 Final Local Validation — 2026-09-30
- `git diff --check`: passed.
- JDK compilation of foundation sources excluding Maven-resolved logging classes: passed.
- Schema and seed resource presence checks: passed.
- Deferred attendance-session timetable foreign-key check: passed.
- Full Phase 1 status: INCOMPLETE pending Maven dependency resolution, JUnit execution, schema execution, seed execution, Swing launch verification, and MySQL integration connectivity.
