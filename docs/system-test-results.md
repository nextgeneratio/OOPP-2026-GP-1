# Final System Test Results

**Date:** 2026-10-18
**Branch:** `Amjad`
**Final status:** NOT READY

The final system-test instruction was attempted after the requested branch review and integration. MySQL was reachable using the credentials supplied by the user, and the application's JDBC connectivity test passed against the existing `university_management` database. The dedicated test database `university_management_test` does not exist. A demo dataset was subsequently loaded into the previously empty `university_management` database, but the full current schema was not applied because that database has schema drift. CRUD/transaction tests were not run, and most role dashboard modules remain placeholders.

## Test execution

| Test ID | Requirement/workflow | Preconditions | Steps | Expected | Actual | Pass/Fail | Evidence |
|---|---|---|---|---|---|---|---|
| ST-001 | FR-001 authentication | Maven/JDK available | Run authentication unit tests for valid, invalid, and inactive users | Safe authentication outcome; no password disclosure | Authentication unit tests passed | Pass | `mvn clean test`; `AuthServiceTest` |
| ST-002 | FR-001, FR-015/FR-016 role navigation and scope | Maven/JDK available | Run role navigation and authorization unit tests | Only permitted role navigation and service-level scope | Unit tests passed | Pass | `NavigationModuleTest`, `AuthorizationServiceTest`, `AuthorizationScopeServiceTest` |
| ST-003 | BR-005–BR-015 academic calculations | Maven/JDK available | Run mark, CA, attendance/medical, eligibility, grade-boundary, GPA, and timetable rule tests | Configured thresholds and boundary cases are enforced | Academic rule tests passed | Pass | `AcademicRulesTest` |
| ST-004 | Safe error handling | Maven/JDK available | Verify authentication, authorization, validation, missing-record, database, and unexpected error messages | Technical details are not shown to users | Safe-message tests passed | Pass | `ErrorHandlingAndUxSafetyTest` |
| ST-005 | Database connectivity and persistence | MySQL account and target schema | Run `DatabaseConnectivityTest` against the existing `university_management` database | JDBC opens a valid connection to the selected catalog | JDBC catalog check passed. Demo data now includes 20 students and active admin/lecturer/technical-officer accounts; role-profile joins and password-hash compatibility were verified. Dedicated test schema, general CRUD, and transaction integration tests remain unverified. | Partial | `mvn -Dtest=DatabaseConnectivityTest test`; direct read-only account/profile/count checks after demo seed |
| ST-006 | FR-002–FR-014 CRUD and academic user workflows | Test database seeded with controlled role accounts and Appendix A data | Exercise user/course/assessment/marks/attendance/medical/results/notices/materials/reports workflows as each role | Expected records/calculations, authorization, and audit records | Not executed; no live test database or complete connected UI modules | Blocked | No end-to-end evidence |
| ST-007 | Error recovery and transactions | Test database and controllable failure injection | Trigger duplicate, invalid, unauthorized, unavailable-database, and mid-transaction failures | Safe messages, rollback, logged technical details, recoverable application | Not executed against the running application/database | Blocked | Unit error mapping passed; integration/system behavior unverified |
| ST-008 | NFR performance and UI accessibility | Seeded class-size test data and graphical session | Measure login/list/result operations and check keyboard, focus, dialogs, and role dashboards | Operations within the SRS target; usable role-based UI | Not executed. Current dashboard figures are pending, and module navigation remains placeholder-based. | Blocked | No performance/UI execution evidence |
| ST-009 | Build and automated tests | Java 17+ toolchain and Maven | Run `mvn clean test` with DB connectivity enabled | Reproducible build and passing automated suite | `mvn clean test`: 40 tests, 0 failures, 0 errors, 0 skipped, including live JDBC connectivity. `mvn -q test` also passed earlier with the DB test skipped. | Pass | Maven output; `target/surefire-reports` |
| ST-010 | Application startup | Local DB config and graphical session | Run `mvn exec:java` | Swing login window opens without startup error | Process remained active for the 15-second startup smoke check and was stopped by timeout. Full interactive login/dashboard behavior was not tested. | Partial | Maven exec smoke launch |

## Passed

- Clean Maven build and test compilation of production and test sources.
- 40 automated tests passed, including JDBC connectivity against `university_management`, academic boundaries, authentication, authorization, safe error mapping, and navigation permissions.
- Architecture spot checks found no JDBC/SQL in Swing UI, no Swing imports in DAOs, no Swing/JDBC imports in domain classes, and no Swing/JDBC imports in controllers.

## Failed

- No unit test failures.
- No full system workflow was recorded as failed; those workflows were not executable in this environment.

## Known limitations

- `university_management_test` is not present. Demo data was loaded into `university_management`, but its schema is older than the current source: three current tables and several columns remain absent. Full schema application, DAO CRUD, transaction rollback, and database-backed workflows are still unverified.
- The supplied account was verified to have no global privileges and privileges only on `university_management`. Rotate the exposed credential and use a separate least-privileged account limited to the dedicated test database for future integration testing.
- The ignored repository-root `test-credentials.properties` template is present and owner-readable only, but remains blank in the workspace despite the user's confirmation; no database connection was attempted with it.
- Role dashboards use a pending summary controller, and module navigation still opens placeholders. User/course/attendance/marks/results/reports workflows are not demonstrated end to end.
- Performance, screenshots, export/printing, and GUI accessibility checks were not performed.
- Do not use this report as evidence that the project meets final acceptance or is demonstration-ready.

## Evidence

- Test data: 20 undergraduates, 1 admin, 5 lecturers, 4 technical officers, and supporting department/batch/semester/course/offering/enrolment/grade rows were loaded into `university_management`. The seed operation did not execute its `TRUNCATE` statements; all pre-existing tables were verified empty first.
- Demo account IDs: `ADM001`, `STU001`–`STU020`, `LEC001`–`LEC005`, and `TO001`–`TO004`. All seeded accounts use the demo password provided to the user in chat.
- Screenshots: none.
- Test report: Maven Surefire reports under `target/surefire-reports`.
- Logs: local Maven output only; no database/application workflow logs.

## Final status

**NOT READY** — create the isolated test schema with a least-privileged test account, run schema/seed and database-backed persistence/transaction checks, finish connecting the UI modules, then rerun the blocked acceptance cases in `05-instruction-final-system-test.md`.
