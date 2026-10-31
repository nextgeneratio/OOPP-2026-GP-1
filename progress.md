# Project Progress

## Resource visibility and dataset follow-up — 2026-10-31

- Added resource storage validation: student lists omit unpublished, missing,
  unreadable, non-regular, traversal, and symlink-escaped files. Opening a file
  rechecks enrollment authorization and the file; publishing requires a valid
  file under the application's uploads directory.
- Added 11 temporary-file/service unit tests across `MaterialFileStoreTest` and
  `ContentMaterialVisibilityTest`, including unreadable files, lecturer warning,
  and service reconstruction.
- Generated/validated 45 original synthetic PDFs (five each for nine source
  courses) under `src/main/resources/uploads/materials/demo-readings/`; the
  manifest now includes offering natural keys and synthetic uploader usernames.
- Prepared `/home/Amjad/.local/share/ams-demo/manual-demo-replacement.sql` and
  `/home/Amjad/.local/share/ams-demo/credentials.csv`. The latter contains 30
  unique synthetic passwords and has mode 600; hashes were generated and
  verified with the application `PasswordHasher`.
- The replacement preserves deterministic source enrollment relationships and
  non-quiz demo scores, creates synthetic attendance, omits borrowed identities
  and medical evidence, and documents unsupported best-two quiz scoring.
  Transformation mapping and expected counts are in
  `docs/dataset-transformation-report.md`.
- Read-only live DB audit reconfirmed MariaDB 11.8.8, 30 InnoDB tables and 2,650
  rows. `mvn clean test` and `mvn clean verify`: 105 discovered, 0 failed,
  0 errors, 1 skipped isolated-DB connectivity check. PDF, manifest, SQL static
  validation and `git diff --check` passed. SQL was not executed; no database
  write, GUI acceptance, commit, or push occurred.
- Branch/HEAD before these local changes: `Amjad`,
  `6a80ea1477af14fcbca427bb7f03535155bdf89f`.

## Latest acceptance follow-up — 2026-10-30

- Fetched GitHub branches over HTTPS. Latest `Amjad` is
  `aa4eeb6732a712f2ac0806fc1fb1ab268018517c` (parent
  `a3e2574ad30c9f8271a3b288ac796ddba9e9b72f`); latest `Nirmal` is
  `348bdc1e9d592d67ac26ca98dca0f1b3091b06a9`. Their common ancestor is
  `2f96413e274db3a2f0d2a574458b5e497f443636`; divergence is 4/8 commits.
- Fixed session ID existence disclosure: Lecturer/Technical Officer now receive
  the same response for missing and out-of-scope sessions. Added allowed and
  denied service regression cases, including a no-roster-DAO-read assertion.
- Replaced raw semester IDs in the assessment and lecturer marks selectors with
  academic year and semester number from database semester data.
- Fixed the undergraduate Attendance screen loading error: setup previously
  requested an offering-wide session list, which the service correctly denies
  to students. The screen now skips that request; its regression test also caught
  and removed a duplicate initial setup call.
- Read-only database recheck found the Technical Officer-only attendance CHECK
  constraint already present, both recorder FKs present, and 120 session rows
  with no invalid/lecturer recorder values. No migration is currently required;
  no DDL/DML was executed.
- `mvn clean verify`: 94 discovered, 93 passed, 0 failed, 0 errors, 1 skipped
  (`DatabaseConnectivityTest`, isolated test credentials are blank). GUI
  inventory has no running application/window. Real persistence and GUI
  acceptance remain unverified. No commit or push was made.
- Readiness: **PARTIALLY VERIFIED**. Detailed evidence and gaps are in
  `docs/system-test-results.md` and `docs/requirement-traceability.md`.

## Historical full repository audit — before 2026-10-30 follow-up

## Full repository audit — 2026-10-30

- Fetched public `origin/Amjad` (`a3e2574ad30c9f8271a3b288ac796ddba9e9b72f`) and
  `origin/Nirmal` (`348bdc1e9d592d67ac26ca98dca0f1b3091b06a9`) over HTTPS after
  the configured SSH fetch failed. After common ancestor `2f96413`, Amjad has 3
  unique commits and Nirmal has 8. Preserved Amjad as the integration base.
- Compared Nirmal's attendance/medical/grade UI and service changes. Retained the
  common Amjad screens and policy checks; did not bring in changes that allow
  Lecturer attendance creation, Admin attendance updates, or duplicate GPA/result
  calculation paths.
- Fixed attendance session-load/update error handling and replaced N per-student
  history calls with an authorized single-session roster DAO read.
- Changed the assessment and marks UI to show course, semester, student number,
  and student name instead of internal IDs.
- Added assessment plan validation/commit/audit/rollback coverage, marks update
  and rollback coverage, and attendance session roster authorization tests.
- Read-only inspection of the configured `university_management` database found
  MariaDB 11.8.8 and the attendance recorder schema, assessment/marks schema,
  medical/timetable fields, and five learning-activity tables required by these
  workflows. An additive Technical Officer-only attendance CHECK constraint is
  proposed because the live table lacks one; its preflight and SQL are in
  `docs/system-test-results.md`. No DDL, DML, seed, or migration was executed.
- Read-only live API smoke checks passed for profile and offering reads across
  four simulated roles; attendance setup across four roles; and Lecturer
  assessment plan, marks sheet, and attendance session reads. No login or UI
  acceptance is claimed.
- `mvn clean verify`: 90 tests, 0 failures, 0 errors, 1 skipped. The skipped test
  is test database connectivity because isolated test credentials are not
  configured. The GUI inventory exposed no native windows; no development
  credentials were entered.
- Detailed findings and role/system status: `docs/system-test-results.md` and
  `docs/requirement-traceability.md`. Full persistence and GUI acceptance remain
  blocked on isolated test credentials and a graphical test session.

## Documentation consistency check — 2026-10-27
- Verified branch `Amjad` and `origin/Amjad` both at current pushed commit `2f96413e274db3a2f0d2a574458b5e497f443636`, with zero divergence. The GPA production fix, focused audit tests, and first documentation-consistency correction are already included in this commit. This follow-up wording correction is local and uncommitted; it does not commit or push.
- Confirmed detailed Lecturer results remain restricted by the selected assigned offering, including the JDBC student-and-offering filter, and the obsolete unscoped Lecturer detail method remains denied. Undergraduate result access remains self-only.
- Confirmed aggregate GPA separately requires Lecturer assignment to the selected offering and that student's enrolment in it before any result DAO lookup. It returns a numeric SGPA/CGPA only; it does not use the GPA flow to disclose unrelated result rows or marks. Undergraduate GPA remains self-only, and repeat/credit rules remain in `GpaCalculator`.
- Fresh `mvn clean package` verification passed with 68 tests, 0 failures, 0 errors, and 1 skipped test. The focused audit coverage for unassigned Lecturer aggregate/detail requests, no result-DAO access following denied GPA authorization, multi-course Lecturer CGPA, and Undergraduate denial of Lecturer-only GPA methods is included in the current pushed commit.
- Real JDBC persistence and interactive Swing acceptance remain **BLOCKED**. `DatabaseConnectivityTest` skips with "Test database credentials are not configured" because the environment and ignored local credential file do not provide both credentials for `university_management_test`. The development `university_management` database was not used as a fallback; no database connection, DDL, DML, seed, or migration was performed.

## Final integration audit — 2026-10-26
- Verified branch `Amjad` at `8dd0ada82293090cc0147b9ea408f16e7fc1c411`, matching `origin/Amjad` before the local fixes below. Existing work was preserved; no database operation, commit, or push was performed.
- Confirmed and fixed a result-scope defect: a Lecturer could select one authorised offering then retrieve every finalized result for that student. Lecturer result requests are now offering-scoped in the service and JDBC query. Student self-result access remains a distinct authenticated path.
- GPA calculation now verifies every contributing finalized result belongs to an offering assigned to the Lecturer; it refuses the aggregate if any source result is outside scope. This avoids leaking an unrelated course through SGPA/CGPA.
- Added `AcademicResultScopeTest` covering scoped Lecturer lookup, rejected unscoped Lecturer lookup, and student self-access. Latest automated run: 60 tests, 0 failures/errors, 1 skipped isolated-DB connectivity test.
- The canonical schema already contains `medical_records.rejection_reason` and `timetables.created_by_user_id`; no migration is required for these code-only fixes. The ignored test credentials remain blank, so GUI and JDBC persistence acceptance are BLOCKED and the development database was untouched.

## Missing Swing screens — implementation update — 2026-10-26
- Baseline commit remains `fbfec84298bae221b21069145b6f205be3f0072a` on `Amjad`; this task has uncommitted local implementation changes only. No reset, commit, push, schema change, seed, migration, or live-database access was performed.
- Fixed the UI root cause by explicitly wiring assessment, marks, eligibility, GPA, report, and result controllers from `ApplicationComponents` through `Application`, `LoginFrame`, and `MainFrame`.
- Replaced the former placeholder routes with `AssessmentsMarksScreen`, `EligibilityScreen`, `GradesGpaScreen`, and `ReportsScreen`; `MainFrame` now fails fast if a future non-dashboard module lacks a registered screen rather than silently rendering `PlaceholderScreen`.
- Assessment entry uses authorized offering, assessment, and enrolment dropdowns; eligibility, GPA/finalized results, and reports use existing controller/service calculations and permissions. Direct assessment publishing is now restricted to an assigned Lecturer; `AssessmentServiceSecurityTest` covers the Admin denial.
- Verification: `mvn clean test` and `mvn clean package` passed with 57 tests, 0 failures, 0 errors, and 1 skipped (`DatabaseConnectivityTest`). `git diff --check` passed.
- Interactive acceptance remains BLOCKED. A display server is available, but `test-credentials.properties` has blank username/password fields and no verified `university_management_test` target. The development database was deliberately not launched or mutated. No screenshots or GUI/persistence claims were made.

## Missing Swing screen audit — Phase 1 — 2026-10-26
- Repository verified at `fbfec84298bae221b21069145b6f205be3f0072a` on `Amjad`, matching `origin/Amjad`; worktree was clean before this task.
- Confirmed root cause: `ApplicationComponents` constructs assessment, marks, eligibility, GPA, report, and result controllers, but `Application`/`LoginFrame`/`MainFrame` do not inject them. `MainFrame` consequently routes `ASSESSMENTS_MARKS`, `ELIGIBILITY`, `GRADES_GPA`, and `REPORTS` to `PlaceholderScreen`.
- Planned work: preserve controller/service/DAO layers, explicitly inject existing controllers into the UI shell, implement role-appropriate screens, add navigation regression coverage, then run safe non-interactive checks. Real database persistence/UI acceptance remains contingent on a confirmed isolated test database.

## Historical project tracker — snapshot 2026-10-18 (superseded)

The checklist and status below are retained as a historical project snapshot.
They predate the 2026-10-30 screen integration, read-only schema audit, and
current test results above; consult the latest audit at the top of this file.

Phase 5 — Demo dataset and password self-service added; final system acceptance remains NOT READY

## Completed
- [x] Project structure created
- [x] Maven configuration
- [x] Database schema source
- [x] Seed data source
- [x] Domain model
- [x] Error handling
- [x] Security/authentication
- [x] Data/DAO layer
- [x] Business logic layer
- [x] API/controller layer
- [ ] Complete Swing UI (admin account CRUD, role dashboards, and self-profile/password screens connected; academic module screens remain placeholders)
- [x] Unit tests (current automated suite passes)
- [ ] Integration tests (live additive seed and result-finalization smoke checks completed; broader CRUD/rollback tests not executed)
- [ ] System tests (not executed end to end)
- [ ] Complete reports (attendance/marks service coverage only)
- [x] Audit logging implementation

## In progress
- Final system acceptance after a dedicated test database and UI modules are available

## Blocked
- Full test database (`university_management_test`) is not created; the existing `university_management` schema is older than the current source schema

## Tests
- Unit: 45 passed (including password-change, profile, and dashboard tests)
- Integration: live database schema/data checks passed; 40 historical results were finalized through `AcademicResultService`; the dedicated test-schema CRUD/rollback suite remains unavailable
- System: not run end to end; see `docs/system-test-results.md`
- Build: latest `mvn clean test` passed (46 tests: 45 passed, 0 failures, 0 errors, 1 skipped because dedicated test DB credentials are absent)

## Database
- Schema: `src/main/resources/db/schema.sql` remains canonical. The live demo database now has the missing current tables/columns via `live-schema-compatibility.sql`; existing rows were retained.
- Seed data: all 25 tables now contain data in `university_management`: 30 users, 62 enrolments (22 existing active plus 40 historical completed), 12 assessments, 186 marks, 100 attendance sessions, 1,748 attendance records, 8 materials, 3 notices, 2 medical records, and 40 course results, along with the existing master/profile/audit data. All 5 lecturers are assigned to demo offerings; all 4 technical officers recorded attendance sessions.
- Data safety: `demo-data-additive.sql` reran with zero inserted rows after the initial load. Foreign-key/linkage checks returned zero invalid rows. The original 22 active enrolments and all 30 user accounts were retained.
- Test database: isolated test profile points to `university_management_test`; database does not exist yet and schema/seed were not applied
- Login verification: 30 seeded account hashes match the app's PBKDF2 hasher; admin, student, lecturer, and technical-officer profile joins returned active role records
- Connection: JDBC connectivity and live schema/data checks were verified against `university_management`; general UI CRUD and transaction rollback remain unverified

## Architecture status
- Presentation: shared shell, live role-specific dashboard metrics, admin user/student account CRUD, and self-profile/password screens; academic module screens remain placeholders
- API: internal controllers and structured responses implemented for core modules
- Business: core services/calculators/policies implemented; report coverage is partial
- Data: JDBC DAOs and transactions implemented; historical results were exercised through the live application service, but general CRUD/rollback behavior remains unverified
- Error: centralized safe error mapping implemented and unit-tested
- Security: salted password hashing and service-layer authorization implemented and unit-tested
- Audit: audit services implemented; database audit workflows unverified
- Reporting: attendance and marks only; full SRS report set remains incomplete

## SRS traceability
- FR-001 to FR-014: core implementation exists, but full CRUD/report/system workflows are not acceptance-tested
- FR-015/FR-016: service-layer scope authorization has unit coverage; end-to-end role checks remain unverified
- BR-001 to BR-015: calculation boundaries and selected rules have unit coverage; database scenarios remain unverified
- NFR-001 to NFR-014: architecture spot checks passed; performance, usability, and full operational checks remain pending

## Known issues
- No dedicated test MySQL is available, so isolated schema/seed, broad persistence, transaction rollback, and audit integration suites remain unverified. The existing development DB has been compatibility-migrated and populated.
- Academic module navigation targets remain placeholders; attendance, medical, marks, timetable, notices, and report workflows are not connected to Swing UI.
- Full report coverage, UI smoke tests, performance checks, and Appendix A database scenarios remain outstanding.

## Files changed
- Branch integration: Anjana business/API/data modules and Nirmal role-dashboard shell
- `src/test/java/lk/ac/ruhuna/fot/ams/integration/DatabaseConnectivityTest.java`
- `src/test/java/lk/ac/ruhuna/fot/ams/unit/ErrorHandlingAndUxSafetyTest.java`
- `README.md`
- `docs/system-test-results.md`
- `progress.md`

## Next task
Configure an isolated test MySQL database and credentials; finish UI module integration and execute the blocked database/system acceptance cases.

## Last updated
2026-10-18 08:55

## Swing acceptance audit — Phase 1 started — 2026-10-26
- Scope: requirement-driven Swing UI, authorization, JDBC/schema, migration-safety, and persistence acceptance audit. Existing local modifications are preserved; no Git reset/checkout/commit/push or database write has been performed.
- Initial repository state recorded: branch `Amjad`; prior audit modifications are present in the worktree and are being treated as in-scope changes to verify. Baseline build, schema discovery, and UI capability checks are pending.

## Swing acceptance audit — implementation and verification update — 2026-10-26
- Baseline/verification: `mvn clean test` passed with 55 tests, 0 failures/errors, and 1 skipped isolated-database connectivity test. `mvn clean package` completed with the same test suite; `git diff --check` passed.
- Verified code defects fixed: medical reviewers previously had no safe proof-open action and no persisted rejection reason. The medical model/DAO/service/controller/UI now require and persist a rejection reason, show an honest legacy-row message where absent, list the authorized pending queue, and authorise before normalising/constraining a PDF path beneath `var/medical-evidence` (rejecting malformed/traversal/symlink evidence paths).
- Verified schema gap: canonical schemas had no `medical_records.rejection_reason` and no timetable creator reference. Added the ordered additive `database/migration_20261026_medical_timetable_audit.sql`, updated both checked-in schema sources and database-design documentation, and wired `timetables.created_by_user_id` into timetable creation. The nullable fields preserve existing records; the migration was NOT applied to any database.
- Technical Officer timetable scope now requires the authenticated officer's department and active semester, and a Technical Officer can alter only a timetable they created; Admin retains cross-timetable management. A new UI action is present for Technical Officer timetable management, but full database/UI execution remains blocked.
- Added `MedicalServiceSecurityTest`: lecturer medical denial, rejection reason before mutation, and tampered proof-reference denial. No credentials, PDFs, local uploads, or test data were committed.
- Acceptance result: compile/unit/package checks PASS; JDBC-backed and interactive GUI scenarios are BLOCKED because `university_management_test` credentials/schema were not safely available. Visible Reports, Grades/GPA, and Eligibility navigation are still connected to `PlaceholderScreen`, so those UI workflows are FAIL rather than claimed as complete. Full matrix and reproducible migration commands are in `docs/system-test-results.md`.

## Permission and medical-workflow audit update — 2026-10-26
- Audit scope: reviewed the tracked Swing navigation/screens, role dashboards, controllers, authorization service, attendance, medical, content/notice, course-management services, their DAOs/schema references, and existing authorization/navigation tests. Generated `target/` output was excluded from source review.
- Stack confirmed: Java 17, Maven, Swing UI, JDBC/MySQL with `src/main/resources/db/schema.sql` as the checked-in canonical schema; internal controller/service/DAO layering and role-session authorization are used instead of HTTP routes.
- Verified ER subset: `users` link to role profiles (`undergraduates`, `lecturers`, `technical_officers`); `course_offerings` link courses/batches/semesters and lecturer assignments; `medical_records` link students and optional offerings, with `evidence_reference` and approval status; attendance sessions/records link offerings and enrolments; notices are stored in `notices`.
- Fixed verified permission discrepancies: Technical Officers no longer receive attendance navigation or service/report access; attendance recording is lecturer-only and revision remains Admin-only. Admin no longer receives the assessments/marks navigation item. Lecturers no longer receive medical navigation or medical-record access. Medical review is restricted to Technical Officers and Admins.
- Implemented student-owned medical submission: a student must submit for their own profile, optionally for an enrolled offering, with a PDF proof. The service validates extension, PDF signature, and a 10 MB limit; copies the proof to a generated name below the non-classpath private `var/medical-evidence` directory; persists only the opaque reference; and deletes the copied file if persistence fails. New requests remain `PENDING`; review decisions remain conditional/transactional.
- Enabled Technical Officers (as well as Admins) to create notices and to create course/offering records and lecturer assignments through the existing service/controller flow. Admin course edits/deactivation remain Admin-only. Renamed the navigation label from “My Course Offerings” to “Course Offerings”.
- Updated navigation and authorization regression tests to reflect the agreed role matrix. Validation: `mvn test` passed — 52 tests, 0 failures/errors, 1 skipped (`DatabaseConnectivityTest` because isolated DB credentials/database are unavailable). No schema migration was needed for these changes.
- Remaining limitations: this Swing/JDBC application has no web endpoint layer, so the requested HTTP status semantics and browser upload progress do not apply. Secure evidence retrieval/viewing and reviewer decision reasons are not represented by the current schema and still need a backwards-compatible migration plus a dedicated authorised file-open flow. Full interactive GUI and live-database acceptance testing remains outstanding.

## Role Account UI Follow-up — 2026-10-18
- Connected Admin User Management and Student Management navigation to searchable account tables backed by `UserController`; administrators can create, edit, filter, and deactivate accounts, with server-side authorization retained.
- Added a self-profile view for all four roles. Admin, lecturer, and technical-officer users can edit their own contact fields; undergraduates can update their email while student identifiers remain read-only.
- Replaced the pending dashboard controller with database-backed role metrics using the existing user, department, batch, course, offering, enrolment, notice, and timetable tables. Cards only show metrics the current schema can provide.
- Updated progress and run documentation to identify which academic module destinations remain placeholders. Attendance, medical, marks, timetable editing, reports, and other academic CRUD workflows are not complete.
- Added unit tests for self-profile field safety and dashboard service delegation. `mvn clean test`: 42 passed, 0 failures, 0 errors, 1 skipped (dedicated test DB credentials unavailable).
- No database credentials or application properties were changed. Live GUI/database CRUD smoke testing remains outstanding.
- Follow-up: see the Password and Additive Demo Dataset update below; isolated integration tests and academic UI acceptance remain.

## Password and Additive Demo Dataset Follow-up — 2026-10-18
- Added a Change Password form to the shared profile screen, available to every active signed-in role. The flow verifies the current password, validates confirmation and minimum length, rejects password reuse, updates the PBKDF2 hash conditionally, writes an audit event transactionally, and clears password character buffers.
- Added unit coverage for successful password updates/auditing, incorrect current passwords, mismatched/short/reused passwords, and buffer clearing.
- Added `src/main/resources/db/live-schema-compatibility.sql` for the reviewed additive live-schema changes and `src/main/resources/db/demo-data-additive.sql` for rerunnable synthetic records. Neither script truncates, updates, or replaces existing users.
- Applied the schema compatibility changes and added demo records to the local `university_management` database. The set-based data script reran with zero inserted rows.
- Finalized 40 historical demo course results through the application's `AcademicResultService`, preserving attendance/CA eligibility and grading rules. Existing current enrollments remain active.
- Verified all 25 tables are populated and checked attendance, marks, enrollments, and results for valid links; integrity queries returned zero violations. All 5 lecturers have offering assignments and all 4 technical officers have recorded sessions.
- `mvn clean test`: 46 tests, 45 passed, 0 failures, 0 errors, 1 skipped (isolated test DB credentials unavailable).
- Remaining: interactive password UI verification, isolated integration/rollback tests, academic UI modules, and full system acceptance.

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

## Phase 3 (UI shell) — branch `ui-shell`
**Done (compiles; not yet run against MySQL by the author of this change)**
- UiTheme: full palette incl. Warning/Info, font helpers (Inter -> sans-serif fallback), spacing constants, FlatLaf setup.
- Shell: MainFrame, TopBar (name, role, account menu, logout with confirmation), NavigationPanel (role-filtered), StatusBar, PlaceholderScreen.
- Login: inline validation, background sign-in with "Signing in…" state, safe messages via ApplicationErrorHandler, Enter key, accessible names.
- Dashboards: Admin, Lecturer, Technical Officer, Undergraduate (summary cards + shortcuts, loading and error states).
- API contract: `DashboardController` + `DashboardSummary`; `PendingDashboardController` stand-in (cards show "—").
- Test: `NavigationModuleTest` (role navigation).

**Not done / gaps for the group**
- Module screens (users, students, courses, attendance, medical, marks, eligibility, GPA, timetable, notices, materials, profile, reports) show PlaceholderScreen — they need their controllers first.
- `AuthenticatedSession.displayName` is currently the username; top bar needs the full name from the business layer (profile tables).
- Dashboard figures need a real `DashboardController` implementation wired in `ApplicationComponents`.
- Navigation per role follows Instruction 03 shortcut lists; verify against SRS section 5.
- UI smoke tests (AssertJ Swing / Mockito) and the AssertJ Swing dependency are still to be added.

**Next:** implement module controllers, then replace each PlaceholderScreen.

## Branch Review, Integration, and Final System-Test Attempt — 2026-10-18
- Fetched GitHub updates and fast-forwarded local `Nirmal`, `Anjana`, and `Kalhara`; created local tracking branch `Kalhara-V2`. Returned to `Amjad`.
- Reviewed each branch against `Amjad`. Merged `Anjana` for the business/API/data layer and `Nirmal` for the role dashboard/navigation shell. Resolved the login conflict by retaining the structured API response while wiring asynchronous sign-in to the role-based shell.
- Excluded `Kalhara`'s separate legacy `Phase 01` implementation and excluded `Kalhara-V2` wholesale because it has unrelated Git history and overlapping/incompatible domain/API/test implementations. Reviewed its test additions; current academic-rule tests cover the equivalent calculation cases. Added focused safe-error-mapping tests for compatible coverage.
- Excluded the nonessential `.idea` module metadata from the merge. No changes were pushed to GitHub.
- Ran architecture spot checks: no JDBC/SQL in Swing UI, no Swing in DAOs, no Swing/JDBC in domain classes or controllers.
- Made the database-connectivity integration test skip explicitly when test credentials are absent; an actual connection failure still fails when credentials are configured.
- Validation at the initial integration stage: `mvn clean test` passed with 40 tests (39 passed, 1 database-connectivity test skipped); later the live connection was verified with all 40 tests passing. `git diff --check` passed.
- System test outcome: NOT READY. Role dashboards still show pending figures while module navigation uses placeholders. No database schema/seed, end-to-end role workflows, audit transactions, performance checks, or complete GUI workflows were claimed as tested.
- Detailed acceptance execution record: `docs/system-test-results.md`.
- Next task: provision a dedicated test database, connect the UI modules, then rerun the blocked acceptance cases in `05-instruction-final-system-test.md`.

## MySQL Availability Follow-up — 2026-10-18
- User confirmed the MySQL server is started. Server responds locally, but passwordless `root` access is denied and no DB credential variables are present in the workspace command environment.
- Added support for a local ignored `test-credentials.properties` file containing only `db.username` and `db.password`; created an empty ignored template. Test database URL remains fixed to `university_management_test`.
- Removed hard-coded database selection from shared `schema.sql` and `seed.sql`, and moved production DB creation to `scripts/create_database.sql`. This prevents test imports from switching to `university_management`.
- Documented test DB setup in `docs/testing-strategy.md`. No schema, seed, or user data was applied.
- Validation: `mvn clean test` passed (39 passed, 1 skipped for missing test credentials); SQL target safety check confirmed shared schema/seed contain no `CREATE DATABASE` or `USE` directives; `git diff --check` passed.
- The user selected local ignored configuration, but a non-disclosing check found the test credentials file blank; the connectivity test skipped for the test-schema profile.
- No DB commands were run beyond the denied passwordless-root probe; no schema/seed/data was changed.
- Next task: save credentials into the exact repository-root `test-credentials.properties`, then verify connectivity and run test schema/seed before database-backed tests.

## Live MySQL Connectivity and Run Instructions — 2026-10-18
- Used the credentials supplied in chat for a read-only JDBC connectivity check against the existing `university_management` database. `DatabaseConnectivityTest` passed with catalog validation; `mvn clean test` passed all 40 tests with zero skips.
- Verified via `information_schema` that `OOP_G01` has no global privileges and has privileges only on `university_management`. The account has no default schema on connect; this is expected. No grants were changed by this session.
- The dedicated `university_management_test` database does not exist. No DDL, seed, or data modifications were executed.
- Added README run instructions for configuring ignored local `application.properties`, launching `Application` from IntelliJ, and running Maven tests. Warned that the seed script truncates tables.
- Added the Maven `exec:java` launch target. `mvn clean package` passed (40 tests, no skips); `mvn exec:java` remained running for a 15-second Swing startup smoke check and was then stopped by the timeout.
- Verified the current `OOP_G01` account has no global privileges and has schema privileges only on `university_management`; root grant changes were not run by this session.
- No database schema/seed or user data was changed. The local test credentials file remains ignored and contains no persisted credentials.
- The database credentials shared in chat are exposed; rotate them and keep any replacement only in ignored local configuration.
- Full system status remains NOT READY: role dashboards use pending metrics and most module screens are placeholders; database-backed persistence/transaction and UI acceptance are not covered by current tests.
- Latest verified test outcome: `mvn clean test` with JDBC enabled against existing `university_management`: 40 tests, 0 failures, 0 errors, 0 skipped.
- Next task: create/seed an isolated test database using a least-privileged account and complete UI module integration before claiming final system readiness.

## Academic Workflow Permissions and Compatibility — 2026-10-18
- Expanded the feature-layer authorization/navigation for technical officers to include course management, assessment access, and attendance; attendance recording is scoped to assigned offerings, technical officers may revise attendance, and only admins may delete a session.
- Added lecturer-aware attendance recording fields in the canonical schema and an additive compatibility migration. Added `batches.level_no` support, validation for levels 1–4, and normalized department codes limited to ICT/BST/ET.
- Added student profile fields for department, batch, academic level, and semester; new course materials default to hidden and student material queries filter to visible items.
- Fixed Java compilation errors in attendance workflow placement and aligned navigation tests with the expanded admin/technical-officer permissions.
- Validation: `mvn -q test` passed (46 tests, 0 failures, 0 errors, 1 skipped); `git diff --check` passed.
- No live database migration was applied. Before applying `academic-workflow-compatibility.sql`, inspect existing department codes and batch levels; old department codes will prevent the new three-department constraint from being added, and existing batch levels default to 1 until explicitly verified/backfilled.
- Scope is still INCOMPLETE: course/timetable/content/attendance navigation screens remain placeholders; timetable level/semester CRUD and deletion are not wired to UI; quiz authoring/attempts/password verification/automatic scoring and assignment uploads/submissions are not implemented. This is not a completed final-system test and no readiness claim is made.
- Next task: implement and test the missing quiz, assignment, timetable, and attendance UI/service workflows, then apply reviewed migrations to an isolated database and execute `05-instruction-final-system-test.md`.

## Academic Platform Workflow Implementation — 2026-10-18
- Replaced navigation placeholders with working Swing flows for course/department/batch/semester/offering management, technical-officer course administration, level-specific timetable creation/listing/update/removal, learning materials, quizzes, assignments, and attendance.
- Added a role-scoped course-offering catalog so users can discover offering IDs; lecturer/technical-officer screens support material upload/edit/publish, MCQ quiz authoring (four choices per question, password-protected, hidden until published), assignment authoring/publishing, attendance recording, roster lookup, and revision where authorized. Students can view published materials/quizzes/assignments, attempt a quiz once, submit/update assignment files before the deadline, view their own attendance percentage, and list their enrolled offerings.
- Added quiz/assignment persistence and service authorization. Quiz passwords use PBKDF2 hashes; answer keys remain server-side; scores are computed server-side; a database uniqueness rule enforces one quiz attempt per student. Assignment files are stored under generated names, limited to 10 MB and PDF/Office/text/image formats, and replacements are accepted before the deadline.
- Timetables are now keyed by department, level 1–4, and semester. Timetable deletion removes entries transactionally and remains blocked by FK-protected attendance history.
- Extended `academic-workflow-compatibility.sql` with the timetable level column and quiz/assignment tables. This migration remains unapplied; inspect existing department codes, batches, timetable level assignments, and attendance recorder rows before applying.
- No live database migration, schema application, seed operation, Swing end-to-end run, or final system-test instruction was executed. Other existing modules (medical, marks, reports/notices) remain outside this feature integration.
- Validation: `mvn -q -DskipTests package` passed; focused quiz and attendance permission tests passed; `mvn -q -Dnet.bytebuddy.experimental=true test` passed (52 tests, 0 failures, 0 errors, 1 skipped); `git diff --check` passed. The Maven test configuration now enables the Byte Buddy experimental mode required by the Java 25 runtime, so the default `mvn test` command can be validated without an extra property.
- The skipped test is database connectivity/integration: test DB credentials and schema are not configured in this worktree. Database-backed behavior still requires applying the reviewed migration to an isolated test database and running the application acceptance flow.
- Next task: apply the compatibility migration to a verified isolated database, exercise the role-based UI with representative seeded courses and enrolments, then update `docs/system-test-results.md` with actual acceptance outcomes.
# Searchable UI selectors and scrollable forms — 2026-10-31
- Replaced manually entered internal IDs with searchable, labeled selectors across user/course setup, course offerings, timetable, attendance, assessments/marks, eligibility, grades/GPA, reports, course materials, learning activities, medical workflows, and batch-targeted notices. Selector models keep record IDs internally and display codes/names/academic terms; dependent selectors reset or filter when their parent changes.
- Kept options role-scoped through existing service authorization and added lecturer options with department context for course setup/assignment. Fixed dialog scroll-pane declarations found by the clean compile, and changed learning-activity validation copy to refer to selected courses instead of IDs.
- Added `SelectionOptionTest` for searchable labels and preservation of the actual selected record ID. Updated test/requirement docs with current coverage and GUI/database limits.
- Validation: `mvn clean test` and `mvn clean verify` passed (107 tests discovered, 0 failures/errors, 1 skipped DB connectivity check); `git diff --check` pending final check. GUI login/role workflows could not be exercised because no GUI window was available. No database connection, migration, data write, commit, or push was performed; no migration SQL is required for this UI work.
