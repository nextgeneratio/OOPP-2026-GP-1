# Final System Test Results

## Documentation consistency check — 2026-10-27

**Verified baseline:** pushed `Amjad` commit `06ead33d2a6aa0a34da3e20f192c7d7d39074f45`, matching `origin/Amjad` after fetch with zero divergence. The GPA production changes, focused audit tests, and prior audit documentation are already on the remote branch. Only this documentation-consistency correction is local/uncommitted; no database action, commit, or push was performed.

- PASS — Detailed Lecturer result retrieval remains offering-scoped end to end: authorization requires the selected assigned offering before the DAO query filters by both student and offering. The old unscoped Lecturer detail operation remains denied; undergraduate detail retrieval remains self-only.
- PASS — Aggregate GPA authority is intentionally narrower than unrestricted detailed-result access. A Lecturer must be assigned to the selected offering and the student must be enrolled in that offering before any GPA result query executes. The GPA path returns only a numeric SGPA/CGPA value; it does not return unrelated course or mark rows. Undergraduate GPA remains self-only. Completed-result, repeat-attempt, and credit-counting rules remain in the established calculator/DAO path.
- PASS — The current pushed branch includes focused coverage for unassigned Lecturer aggregate/detail denial, no result-DAO access following denied aggregate authorization, successful multi-course Lecturer CGPA, and Undergraduate denial of Lecturer-specific GPA methods. Fresh `mvn clean package` result: **68 tests, 0 failures, 0 errors, 1 skipped**.
- BLOCKED — `DatabaseConnectivityTest` skips with **"Test database credentials are not configured"** because neither environment credentials nor both fields in the ignored local file are configured for the dedicated `university_management_test` database. Therefore real JDBC/persistence and interactive Swing acceptance were not run; no screenshots or live-dialog evidence exists. The development database was not used as a fallback, and no DDL/DML/seed/migration was run.

## Final integration audit update — 2026-10-26

**Branch/baseline:** `Amjad` / `8dd0ada82293090cc0147b9ea408f16e7fc1c411`; subsequent fixes are local and uncommitted as requested.

- Fixed a cross-course result disclosure risk: a Lecturer's selected offering now reaches an offering-scoped result query, rather than fetching every finalized result for that student. GPA aggregates validate every contributing enrolment's offering scope before calculating.
- `AcademicResultScopeTest` verifies Lecturer scoped access, denial of the obsolete unscoped Lecturer path, and student self-access. `mvn test` passed: 60 tests, 0 failures, 0 errors, 1 skipped.
- The canonical schema includes the prior `rejection_reason` and `created_by_user_id` fields. No migration is required for this code-only fix and no database was changed.
- BLOCKED: isolated JDBC and real Swing acceptance. The local `university_management_test` credential fields are blank; no application was launched against the development database. There are no new screenshots or GUI-evidence artifacts.

## Swing screen implementation update — 2026-10-26

**Branch/baseline:** `Amjad` / `fbfec84298bae221b21069145b6f205be3f0072a`. The changes below are intentionally uncommitted because this task did not authorize a commit or push.

- PASS — `ApplicationComponents` controllers are now explicitly injected through `Application` and `LoginFrame` into `MainFrame`. Assessments & Marks, Eligibility, Grades & GPA, and Reports now instantiate functional controller-backed screens instead of `PlaceholderScreen`.
- PASS — assessment entry uses authorized offering/assessment/enrolment selectors and validates score input; eligibility, GPA/finalized-result, and report views call their existing controllers. `AssessmentService` now rejects an Admin direct publish attempt; `AssessmentServiceSecurityTest` proves that regression.
- PASS — `mvn clean test` and `mvn clean package`: 57 tests, 0 failures, 0 errors, 1 skipped. `git diff --check` passed. The single skipped `DatabaseConnectivityTest` is intentional: both fields in the ignored local test-credentials file are blank.
- BLOCKED — visible Swing interaction, screenshots, and JDBC persistence confirmation. A graphical display is present, but there is no authenticated, verified `university_management_test` connection. The application was not pointed at the populated development database, and no DDL/DML or migration was executed anywhere.

Current acceptance: Lecturer controller routing/forms PASS by static/build verification but BLOCKED for live persistence; student Eligibility/Grades/GPA routing BLOCKED for live interaction; Reports routing BLOCKED for live data/role testing; medical and timetable UI workflows remain BLOCKED for the same isolated-database prerequisite. There are no new screenshots or `target/ui-test-evidence` artifacts.

## Acceptance audit update — 2026-10-26

**Branch/HEAD:** `Amjad` / `bf15cad092fc9d062347f5041eba287f783ed64f` at audit start. The audit preserved the existing worktree; no live database operation was performed.

### Baseline and executable evidence

- PASS — `mvn clean test`: 55 tests, 0 failures, 0 errors, 1 skipped. The skipped `DatabaseConnectivityTest` requires the deliberately isolated `university_management_test` database and credentials.
- PASS — `mvn clean package`: package build completed after the same test suite.
- PASS — `git diff --check`: no whitespace errors.
- BLOCKED — interactive Swing acceptance: a graphical display is available, but the required isolated database has neither supplied credentials nor a confirmed schema. Launching the configured development database could mutate a live system, so it was not launched.

### Code-to-requirement map

- Medical: `MedicalScreen` → `MedicalController` → `MedicalService` → `JdbcMedicalDao` → `medical_records`; authorization is enforced by `AuthorizationService`/`JdbcAuthorizationScopeDao`.
- Timetable: `TimetableScreen` → `TimetableController` → `TimetableService` → `JdbcTimetableDao` → `timetables`/`timetable_entries`.
- Attendance: `AttendanceScreen` → `AttendanceController` → `AttendanceService` → `JdbcAttendanceDao`; Technical Officer access is denied in the service layer.
- Course/learning activities: `CourseOfferingsScreen`/`CourseMaterialsScreen`/`LearningActivitiesScreen` → their controllers/services/DAOs; their database-backed acceptance still needs the isolated DB.
- Unwired modules found: `MainFrame` sends `ELIGIBILITY`, `GRADES_GPA`, and `REPORTS` to `PlaceholderScreen`. These are FAIL for visible UI acceptance, irrespective of their service/controller implementations.

### Acceptance matrix

- A1–A6 Student navigation, medical submission/PDF persistence, reopen/re-login, invalid PDF/date/duplicate cases, and cross-student privacy: BLOCKED — isolated database and synthetic test accounts are unavailable. Static inspection confirms submission resolves the authenticated student ID, not a form-provided student ID.
- A7 Student cannot approve, change status, traverse evidence paths, or read another student's proof: PASS for service guards covered by `MedicalServiceSecurityTest`; database-backed/UI proof retrieval remains BLOCKED.
- B1–B6 Reviewer pending queue, proof view, approval/rejection persistence, concurrent decisions, and Admin repeat: BLOCKED for live UI/persistence. The verified defects were fixed: reviewer queue query, authorised PDF viewer path, required rejection reason, legacy no-reason display, and conditional pending-only update.
- B7 Lecturer medical denial: PASS for service authorization regression (`MedicalServiceSecurityTest`) and role navigation tests.
- C1–C9 Lecturer materials, quiz/assignment, marks, eligibility, attendance and reports: BLOCKED for isolated JDBC/UI execution. Reports are additionally FAIL at the visible UI level because the Reports menu opens `PlaceholderScreen`.
- D1 Technical Officer attendance denial: PARTIAL PASS — navigation and core service restriction are covered, but the full direct-call matrix needs additional service tests and an isolated DB.
- D2–D8 Technical Officer course/lecturer dropdown, timetable persistence/creator display, notice persistence, reports, and medical review: BLOCKED. Timetable creation ownership is now persisted for newly created records after migration, but its GUI acceptance requires a test DB. Reports remain visible-UI FAIL due to `PlaceholderScreen`.
- E1–E7 Admin course offering, safe deactivation, marks denial, attendance, medical, timetable/notice and reports: BLOCKED for isolated JDBC/UI; the Admin marks navigation denial is covered by `NavigationModuleTest`. Reports are visible-UI FAIL due to `PlaceholderScreen`.
- F Cross-role persistence, rollback, keyboard/focus/accessibility, resize, SQL integrity and stale-state checks: BLOCKED — no safe isolated DB/test accounts; no interactive run was simulated.

### Migration decision

**MIGRATION REQUIRED BUT NOT APPLIED TO LIVE DATABASE.** The canonical schema lacked `medical_records.rejection_reason` required to persist/display reviewer reasons, and `timetables.created_by_user_id` required to attribute new Technical Officer-created timetables. The additive migration is [migration_20261026_medical_timetable_audit.sql](../database/migration_20261026_medical_timetable_audit.sql). It was not applied anywhere during this audit.

Prerequisites and safe test-only commands (the MySQL client prompts for a password; no password belongs in these commands):

```sh
mysql -u "$DB_USERNAME" -p -e "SHOW DATABASES LIKE 'university_management_test';"
mysqldump -u "$DB_USERNAME" -p university_management_test > university_management_test-before-20261026.sql
mysql -u "$DB_USERNAME" -p university_management_test < database/migration_20261026_medical_timetable_audit.sql
mysql -u "$DB_USERNAME" -p -D university_management_test -e "SELECT table_name,column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND ((table_name='medical_records' AND column_name='rejection_reason') OR (table_name='timetables' AND column_name='created_by_user_id'));"
```

The migration is additive and nullable, so historical rows remain valid. Rollback, if required, is a separately reviewed `ALTER TABLE ... DROP FOREIGN KEY`, `DROP INDEX`, and `DROP COLUMN`; do not run it on a database containing new audited records.

**Date:** 2026-10-18
**Branch:** `Amjad`
**Final status:** NOT READY

The final system-test instruction was attempted after the requested branch review and integration. The existing `university_management` database was additively migrated to the current application schema and populated with synthetic linked academic records; no existing user or enrollment rows were replaced. The dedicated test database `university_management_test` does not exist. A full system acceptance run is still blocked by unconnected academic UI destinations, missing isolated integration-test setup, and untested end-to-end GUI workflows.

**UI follow-up (2026-10-18):** Admin account/student listing and CRUD screens, self-profile view/edit, password-change controls, and role-specific dashboard metrics are connected. Live checks returned 30 users and all four profile types. Password UI use and account create/edit/deactivate remain untested interactively. Attendance, marks, medical, timetable editing, notices management, and reports remain placeholder destinations.

## Test execution

| Test ID | Requirement/workflow | Preconditions | Steps | Expected | Actual | Pass/Fail | Evidence |
|---|---|---|---|---|---|---|---|
| ST-001 | FR-001 authentication | Maven/JDK available | Run authentication unit tests for valid, invalid, and inactive users | Safe authentication outcome; no password disclosure | Authentication unit tests passed | Pass | `mvn clean test`; `AuthServiceTest` |
| ST-002 | FR-001, FR-015/FR-016 role navigation and scope | Maven/JDK available | Run role navigation and authorization unit tests | Only permitted role navigation and service-level scope | Unit tests passed | Pass | `NavigationModuleTest`, `AuthorizationServiceTest`, `AuthorizationScopeServiceTest` |
| ST-003 | BR-005–BR-015 academic calculations | Maven/JDK available | Run mark, CA, attendance/medical, eligibility, grade-boundary, GPA, and timetable rule tests | Configured thresholds and boundary cases are enforced | Academic rule tests passed | Pass | `AcademicRulesTest` |
| ST-004 | Safe error handling | Maven/JDK available | Verify authentication, authorization, validation, missing-record, database, and unexpected error messages | Technical details are not shown to users | Safe-message tests passed | Pass | `ErrorHandlingAndUxSafetyTest` |
| ST-005 | Database connectivity and persistence | MySQL account and target schema | Run database checks against the existing `university_management` database | JDBC opens a valid connection; records satisfy schema and linkage constraints | JDBC connection passed. Additive compatibility DDL was applied, all 25 tables contain data, the DML seed reran with zero inserts, and linkage checks returned zero invalid rows. All 5 lecturers have offering assignments and all 4 technical officers have recorded sessions. General CRUD and rollback integration tests remain unverified. | Partial | Live catalog/table-count/integrity checks; `demo-data-additive.sql` |
| ST-006 | FR-002–FR-014 CRUD and academic user workflows | Database populated with controlled role accounts and synthetic academic records | Exercise user/course/assessment/marks/attendance/medical/results/notices/materials/reports workflows as each role | Expected records/calculations, authorization, and audit records | Forty historical course results were finalized through `AcademicResultService` after valid marks and attendance were seeded. Full role-by-role CRUD and UI workflows were not executed; academic module screens remain placeholders. | Partial | Live result-finalization smoke check; `course_results` contains 40 linked completed attempts |
| ST-007 | Error recovery and transactions | Test database and controllable failure injection | Trigger duplicate, invalid, unauthorized, unavailable-database, and mid-transaction failures | Safe messages, rollback, logged technical details, recoverable application | Not executed against the running application/database | Blocked | Unit error mapping passed; integration/system behavior unverified |
| ST-008 | NFR performance and UI accessibility | Seeded class-size test data and graphical session | Measure login/list/result operations and check keyboard, focus, dialogs, and role dashboards | Operations within the SRS target; usable role-based UI | Not executed. Dashboard counts now load, but performance, keyboard/accessibility, and academic module navigation remain unverified. | Blocked | No performance/UI execution evidence |
| ST-009 | Build and automated tests | Java 25 LTS toolchain and Maven | Run `mvn clean test` | Reproducible build and passing automated suite | `mvn clean verify`: 93 tests, 0 failures, 0 errors, 1 skipped because the dedicated test database credentials are unavailable. | Pass | Maven output; `target/surefire-reports` |
| ST-010 | Application startup | Local DB config and graphical session | Run `mvn exec:java` | Swing login window opens without startup error | Process remained active for the 15-second startup smoke check and was stopped by timeout. Full interactive login/dashboard behavior was not tested. | Partial | Maven exec smoke launch |
| ST-011 | Admin account management and self-service profiles | Current schema-compatible database with role accounts | Search accounts; create, edit, and deactivate records as admin; view/edit own profile as each role | Changes are validated, persisted, authorization-checked, and audited | Read-only live checks confirmed account search (30 rows), profiles for all four roles, and role dashboard metrics. Automated tests pass. Live mutation/audit and interactive GUI flows remain untested. | Partial | `UserManagementScreen`, `ProfileScreen`, `UserService`, `ProfileService`; live read-only controller smoke check |
| ST-012 | All-role password change | Signed-in account and current password | Submit current password, new password, and confirmation from the shared profile screen | Verify current password; validate new password; persist hash and audit atomically; clear secret buffers | Service/controller implementation and unit tests pass for success, incorrect current password, mismatch, short/reused passwords, auditing, and buffer clearing. The live database password and interactive GUI flow were not changed/tested. | Partial | `ProfileServiceTest`; `ProfileService`, `ProfileScreen` |

## Passed

- Clean Maven build and test compilation of production and test sources.
- 45 automated tests passed; one dedicated database connectivity test was skipped because test-database credentials are absent. Unit coverage includes password change, academic boundaries, authentication, authorization, safe error mapping, profiles, dashboards, and navigation.
- Architecture spot checks found no JDBC/SQL in Swing UI, no Swing imports in DAOs, no Swing/JDBC imports in domain classes, and no Swing/JDBC imports in controllers.

## Failed

- No unit test failures.
- No complete system workflow was run end to end; those workflows remain unverified rather than passed.

## Known limitations

- `university_management_test` is not present. The existing development database was brought to current schema compatibility using additive DDL; isolated integration tests, broad DAO CRUD, and transaction rollback remain unverified.
- The supplied account was verified to have no global privileges and privileges only on `university_management`. Rotate the exposed credential and use a separate least-privileged account limited to the dedicated test database for future integration testing.
- The ignored repository-root `test-credentials.properties` template is present and owner-readable only, but remains blank in the workspace despite the user's confirmation; no database connection was attempted with it.
- Account management and role dashboards use live database queries; profile data was verified for all four roles. Academic module navigation still opens placeholders, and create/edit/deactivate UI mutations are not demonstrated end to end.
- Synthetic demo data was added additively. Existing 30 accounts and 22 active enrollments were retained; 40 historical results were produced through the application service. Live account CRUD and password mutation were not tested.
- Performance, screenshots, export/printing, and GUI accessibility checks were not performed.
- Do not use this report as evidence that the project meets final acceptance or is demonstration-ready.

## Evidence

- Test data in `university_management`: all 25 tables contain rows, including 30 users, 62 enrollments, 12 assessments, 186 marks, 100 attendance sessions, 1,748 attendance records, 8 materials, 3 notices, 2 medical records, and 40 course results. Existing 22 active enrollments remain active; historical results were finalized through the business service. All lecturers and technical officers have demo academic assignments/records.
- Demo account IDs: `ADM001`, `STU001`–`STU020`, `LEC001`–`LEC005`, and `TO001`–`TO004`. All seeded accounts use the demo password provided to the user in chat.
- Screenshots: none.
- Test report: Maven Surefire reports under `target/surefire-reports`.
- Logs: local Maven output only; no database/application workflow logs.

## Final status

**NOT READY** — create the isolated test schema with a least-privileged test account, run schema/seed and database-backed persistence/transaction checks, finish connecting the UI modules, then rerun the blocked acceptance cases in `05-instruction-final-system-test.md`.

## Academic workflow implementation update — 2026-10-18

- The previously missing course/offering administration, level-and-semester timetables, learning materials, quizzes, assignments, and attendance UI/service workflows have since been implemented. The earlier statement that these screens were still placeholders is historical and no longer describes the current code.
- Build validation passed with `mvn -q -DskipTests package`. Focused quiz and attendance permission tests passed. The full suite passed: 52 tests, 0 failures, 0 errors, and 1 skipped (database connectivity/integration, because a test database configuration is unavailable).
- These are build/unit-test results only. The new `academic-workflow-compatibility.sql` migration has not been applied, and the workflows have not been acceptance-tested against MySQL or through the Swing UI. The database-backed cases in `05-instruction-final-system-test.md` therefore remain unverified.
- **Current status remains NOT READY for final system-test sign-off** until the migration is reviewed/applied to an isolated test database and the required database/UI acceptance cases are exercised and recorded.
