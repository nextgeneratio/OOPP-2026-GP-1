# Project Progress

## Current phase
Phase 0 — Project setup / planning

## Completed
- [ ] Project structure created
- [ ] Maven configuration
- [ ] Database schema
- [ ] Seed data
- [ ] Domain model
- [ ] Error handling
- [ ] Security/authentication
- [ ] Data/DAO layer
- [ ] Business logic layer
- [ ] API/controller layer
- [ ] Swing UI
- [ ] Unit tests
- [ ] Integration tests
- [ ] System tests
- [ ] Reports
- [ ] Audit logging

## In progress
- None

## Blocked
- None

## Tests
- Unit:
- Integration:
- System:

## Database
- Schema:
- Seed data:
- Test database:
- Connection:

## Architecture status
- Presentation:
- API:
- Business:
- Data:
- Error:
- Security:
- Audit:
- Reporting:

## SRS traceability
- FR-001 to FR-014:
- FR-015/FR-016:
- BR-001 to BR-015:
- NFR-001 to NFR-014:

## Known issues
- None

## Files changed
- None

## Next task
Start Phase 1 using `01-instruction-foundation.md`.

## Last updated
2026-09-30

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

## Structure Prompt Execution — Stage 1 — 2026-10-03
- Reread this progress log before execution as requested.
- Inspected the project descriptor, documentation, source/test layout, resources, scripts, SRS header, and Git status.
- Existing Phase 1 implementation and its validation blockers remain as recorded; Git status was clean on branch `main`.
- Initial structural discrepancy for Stage 2 review: `src/test/resources/test-application.properties` is listed in the prompt but is not present in the current test-resource tree.
- No implementation files changed in this stage.
- Next task: complete the prompt compliance audit and determine whether any narrow structural correction is appropriate.

## Structure Prompt Execution — Stage 2 — 2026-10-03
- Compared the live project against the required Maven structure and technology baseline in `00-project-structure-prompt.md`.
- Confirmed the Java 17 Maven descriptor, base package, source/test layer directories, application resources, docs, and database scripts are present.
- Confirmed `src/test/resources/db/test-schema.sql` and `test-seed.sql` are present; the only identified required-layout gap is missing `src/test/resources/test-application.properties`, excluded by the existing broad ignore rule.
- Kept all existing functional work and Phase 1 blockers outside this structure-only correction.
- Next task: add a non-secret test profile placeholder and narrowly allow that tracked resource.

## Structure Prompt Execution — Stage 3 — 2026-10-03
- Added `src/test/resources/test-application.properties` with a test environment marker, isolated local test database URL, and pool size; DB username/password remain environment-provided.
- Added a narrow `.gitignore` exception so the non-secret test profile is included while local `test-application.properties` files remain ignored elsewhere.
- No application runtime behavior or Phase 1 functionality was changed.
- Next task: verify tracked-file status and run available structural/build validation.

## Structure Prompt Execution — Stage 4 — 2026-10-03
- `git diff --check`: passed.
- Verified all checked required structure paths exist, including test schema/seed resources and the new test configuration.
- Verified Git's ignore rule exception makes `src/test/resources/test-application.properties` visible for tracking.
- Build and JUnit validation could not run because Maven is not installed (`mvn` not found); Java 26 is available, but no Java sources changed.
- Next task: record final structure-prompt outcome and retain the Maven-validation blocker.

## Structure Prompt Execution — Stage 5 — 2026-10-03
- Completed the approved `00-project-structure-prompt.md` compliance pass; the previously missing test configuration scaffold is now present.
- Only structural/configuration scaffolding was changed; no later implementation instruction was executed.
- Validation is limited to structural path checks and `git diff --check`; Maven/JUnit validation remains blocked until Maven is available.
- Remaining work: resolve the previously recorded Maven/database validation blockers before treating Phase 1 as complete.
- Files changed in this execution: `.gitignore`, `progress.md`, and `src/test/resources/test-application.properties`.
- Next task: enable Maven and continue the pending Phase 1 verification recorded above.

## Instruction 02 Execution — Stage 1 Baseline — 2026-10-03
- Reread `progress.md` before beginning execution, then reviewed the current source tree, core auth/data/domain classes, schema table inventory, unit-test inventory, and SRS functional/role/business requirements.
- Existing foundation work confirmed: JDBC connection provider, transaction helper, user DAO, password hashing, authentication service/controller, generic role/self authorization helpers, core domain types, schema, and foundation tests.
- Major Phase 2 gaps confirmed: module-specific repositories/DAOs and services, academic calculators/policies, scope-aware authorization, auditable transactional workflows, module request/response DTOs/controllers, and the Phase 2 business-rule test matrix.
- Preserved existing uncommitted changes from the preceding approved structure task (`.gitignore`, `progress.md`, and test profile); no existing changes were reverted.
- Maven and live MySQL remain unavailable/blocking as noted earlier; no database operations were run.
- Next task: implement and test pure academic rules before database-backed service integration.

## Instruction 02 Execution — Stage 2 Academic Rules — 2026-10-03
- Added pure weighted assessment/final/CA calculators; assessment plans require exactly 100% total configured weight, marks and weights validate against 0–100, and CA eligibility is evaluated at 40.00%.
- Added theory/practical/combined attendance calculations with approved-medical denominator exclusion and per-required-component 80.00% eligibility; following user choice, a component with all sessions medically excused is treated as 100.00% and eligible.
- Added final-exam eligibility results with explicit failed-condition messages, versioned Technology-stream grade bands, two-decimal SGPA/CGPA calculations, configurable repeat-attempt selection, and timetable overlap validation.
- Aligned `Assessment` weight validation to allow 0% through 100%, as required by Instruction 02.
- Added unit tests for marks/weights, CA thresholds, component attendance/medical handling, all grade boundaries, weighted GPA/repeats/incomplete results, and timetable conflicts.
- IDE diagnostics reported no issues for changed Java/test files. Direct `javac` compilation and executable academic-rule smoke checks passed.
- The targeted test runner reported no tests found; Maven/JUnit execution is still unavailable and no JUnit pass is claimed.
- Next task: implement repository/DAO and transaction support for SRS data.

## Instruction 02 Execution — Stage 3 Data Handling — 2026-10-03
- Added `JdbcExecutor` prepared-statement query/update/generated-key support with row mapping and safe `DataAccessException` wrapping.
- Updated `TransactionManager` to accept checked SQL work on one shared connection, commit on success, roll back on SQL/runtime/error failures, and propagate safe database exceptions.
- Extended user DAO persistence/search/profile operations and added typed JDBC DAOs for course structures/offerings/enrolments, assessments/marks, attendance, medical records, materials/notices, timetables, grades, and audit events.
- Updated schema and development seed for multiple lecturer assignments, one active enrolment per student/offering while retaining historical attempts, unique theory/practical component types, explicit notice audience targets, medical evidence references, and query indexes.
- Added explicit test-profile loading and aligned its database URL with the isolated test database name. Updated architecture/database-design documentation to reflect implemented data infrastructure and remaining work.
- `git diff --check`, IDE diagnostics, and JDK compilation of data/business/API dependencies passed.
- No schema/seed statements were executed; live MySQL behavior remains unverified because no safe test database was available.
- Next task: implement scoped service-layer authorization, transactional academic workflows, and service-level audit/authentication behavior.

## Instruction 02 Execution — Stage 4 In Progress — 2026-10-03
- Began implementing reusable scope-aware authorization, report-role policies, audited authentication, and transactional business services using the Stage 3 DAOs.
- Work remains in progress; no feature is marked complete until service and API wiring plus relevant tests are validated.

## Instruction 02 Execution — Stage 4 Service Layer — 2026-10-03
- Completed service-layer authorization and business workflows for authentication/auditing, user profiles, courses/enrolments, assessments/marks, attendance, medical decisions, course materials/notices, timetables, eligibility, grades/GPA, versioned result snapshots, and current attendance/marks reports.
- Wired shared services and DAO dependencies through `ApplicationComponents`; updated architecture/database-design documentation for the implemented service layer and result snapshots.
- Validation: `git diff --check` passed; selected service/data/security/API Java sources compiled with the JDK; IDE diagnostics found no errors in checked configuration/error-handler files.
- Focused JUnit invocation could not discover tests in this workspace; Maven/JUnit execution remains unavailable. No MySQL instance was available, so schema/seed and transaction behavior have not been integration-tested.
- Remaining known limitation: report service coverage is currently attendance and marks rather than the entire SRS report set. Stage 5 will add thin internal controllers/DTOs and structured API result/error handling.

## Instruction 02 Execution — Stage 5 Internal API — 2026-10-03
- Added request/response DTO-backed controllers for users, courses, assessments, marks, attendance, medical records, eligibility, GPA, profiles, timetables, content/notices, reports, and academic-result finalization; retained the safe login response API.
- Added consistent structured validation errors for missing controller requests and wired all controller instances through `ApplicationComponents` using a shared error handler/support component.
- Updated the Swing login UI to consume the structured login response and updated architecture documentation to describe available controller coverage and the remaining report limitation.
- Validation: IDE diagnostics reported no issues across the API controllers, application wiring, and login UI; all production Java sources compiled with `javac` using temporary SLF4J API stubs because external dependencies are not locally cached; `git diff --check` passed before final validation.
- Full Maven/JUnit and live MySQL verification remain unavailable; report-service coverage remains limited to attendance and marks.
- Next task: perform final Phase 2 validation, confirm append-only progress behavior, and record remaining external validation blockers.

## Instruction 02 Execution — Stage 6 Final Validation — 2026-10-03
- Reread `progress.md` before final validation; preserved prior entries and appended this final stage without rewriting or deleting earlier content.
- Final `git diff --check` passed. IDE diagnostics reported no errors for the API controllers, `ApplicationComponents`, the application entry point, and the Swing login UI.
- Compiled all production Java sources successfully with `javac`; temporary SLF4J API stubs were used only for compilation because Maven dependencies are not cached locally and were removed afterward.
- Maven is not installed, so Maven/JUnit tests could not run. No live MySQL database was available; schema, seed, JDBC queries, and transaction behavior remain unverified against MySQL.
- Phase 2 implementation stages 1–5 are complete. Remaining external validation: install/enable Maven and provide an isolated MySQL test database; report-service coverage remains limited to attendance and marks.
