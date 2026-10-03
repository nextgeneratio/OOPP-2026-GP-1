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
