# Instruction 04 — Quality Hardening, Unit Testing, Integration Testing and Test Readiness

## Ready-to-paste prompt

Continue from the current project state.

Read:

1. SRS.
2. `progress.md`.
3. All current source packages.
4. Existing unit/integration tests.

Do not add new features just because they are interesting. This phase is for making the implemented SRS reliable and testable.

## Goal

Harden the architecture and complete unit + integration coverage before the final system test.

Required test pyramid:

```text
                System / UI Tests
                     /\
                    /  \
          Integration Tests
                 /      \
                /        \
             Unit Tests
```

## 1. Architecture audit

Verify dependency direction:

```text
presentation
   ↓
api
   ↓
business
   ↓
data
   ↓
JDBC/MySQL
```

Check that:

- no UI class imports JDBC.
- no UI class contains SQL.
- no DAO imports Swing.
- domain classes do not depend on Swing/JDBC.
- business services are testable without starting the GUI.
- API controllers are thin.
- authorization is in the business/service layer.
- exceptions are mapped centrally.
- configuration does not contain secrets in source control.

Fix violations before adding tests.

## 2. Unit test structure

Put pure unit tests under:

`src/test/java/.../unit`

Group them by layer/module.

### Domain tests

Test:

- mark boundaries.
- weight boundaries.
- credits.
- timetable times.
- medical date range.
- enum/status constraints.

### Business tests

Mock DAOs/repositories.

Test:

- authentication rules.
- authorization.
- CA.
- attendance.
- medical effect.
- eligibility.
- final mark.
- grades.
- SGPA.
- CGPA.
- repeat selection.
- course/enrolment rules.
- notices/timetable conflicts.

### API/controller tests

Test:

- request validation/mapping.
- service invocation.
- successful response mapping.
- safe error mapping.

Controllers must not perform hidden business calculations.

## 3. Mandatory boundary-value tests

Marks:

- -0.01 rejected.
- 0 accepted.
- 100 accepted.
- 100.01 rejected.

CA:

- 39.99 fail.
- 40.00 pass.

Attendance:

- 79.99 fail.
- 80.00 pass.
- 100.00 pass.
- denominator zero handled safely.

Grade boundaries:

- 34/35.
- 39/40.
- 44/45.
- 49/50.
- 54/55.
- 59/60.
- 64/65.
- 69/70.
- 74/75.
- 79/80.
- 84/85.

## 4. Property-style calculation checks

Where practical, add reusable checks:

- percentage never below 0 or above 100.
- final mark never outside valid range.
- SGPA never below 0 or above 4.
- CGPA never below 0 or above 4.
- increasing a mark cannot unexpectedly reduce the weighted final mark under the same weights.

Do not over-engineer these tests; keep them understandable for a mini project demonstration.

## 5. Integration tests with MySQL

Create tests under:

`src/test/java/.../integration`

Use a dedicated test database/schema.

Before each test or suite:

- ensure known schema state.
- insert required seed data.
- clean/rollback as appropriate.

Verify:

### Persistence

- insert.
- read.
- update.
- deactivate/soft-delete.
- FK enforcement.
- unique constraints.

### Transactions

Test a multi-step operation that intentionally fails halfway.

Verify:

- earlier changes are rolled back.
- audit/event records do not create inconsistent partial state.
- no orphan records remain.

### Authentication

- active valid user succeeds.
- invalid password fails.
- inactive user fails.
- password hash is not the plaintext password.

### Academic calculations

Store known attendance/marks and verify service results against expected values.

## 6. Test data

Use predictable IDs/labels for tests.

Include scenarios corresponding to SRS Appendix A:

- STU001: theory 13/15, practical 14/15.
- STU002: theory 12/15, practical 12/15.
- STU003: theory 10/15, practical 10/15.
- STU004: theory 13/15 plus approved medical absence.
- STU005: theory 8/15 with approved medical that still does not reach 80%.

Also include:

- at least one repeat enrolment.
- at least one batch-missed enrolment.
- at least one failed CA.
- at least one failed attendance condition.
- at least one incomplete assessment configuration.
- at least one unauthorized access attempt.

## 7. Performance smoke checks

The SRS states normal list/login/single-result operations should respond within 3 seconds under expected class-size data.

Create a lightweight measurement or manual check for:

- login.
- student list.
- marks list.
- attendance list.
- result view.

Do not optimize prematurely. First identify any obviously inefficient query or repeated database call.

## 8. Error handling audit

Deliberately cause:

- duplicate username.
- duplicate enrolment.
- invalid mark.
- invalid timetable overlap.
- invalid medical date.
- missing record.
- database unavailable.
- unauthorized operation.

Verify that:

- correct exception type occurs.
- technical cause is logged.
- user sees safe message.
- application remains recoverable.
- no stack trace is shown in the GUI.

## 9. Security audit

Verify:

- no plaintext password in database.
- password hashes use salt.
- no credentials in Git-tracked files.
- all SQL uses prepared statements.
- service-layer authorization exists.
- logs do not contain passwords.
- sensitive changes create audit events.

## 10. Audit logging verification

Verify events for:

- login attempts.
- marks create/update.
- attendance create/update.
- medical approval/rejection.
- profile/master-data create/update/deactivate.

Audit entries must include actor and timestamp where required by the SRS.

## 11. Automated test command

Make sure both commands work:

```bash
mvn test
```

and, where useful:

```bash
mvn -q test
```

Resolve test failures instead of simply disabling failing tests.

Do not add `@Disabled` merely to get a green build.

## 12. Final readiness checklist

The project is ready for final system testing only when:

- build is reproducible.
- unit tests pass.
- integration tests pass against test MySQL.
- error paths are tested.
- authorization paths are tested.
- core calculations are tested.
- API/controller tests pass.
- UI smoke tests are available.
- progress.md is current.
- test data is reproducible.
- database can be reset and reseeded.

## Mandatory progress.md update

Record:

- total unit tests and status.
- integration status.
- system-smoke readiness.
- outstanding defects.
- performance observations.
- security observations.
- database reset/seed instructions.
- next task: execute final system test instruction.
