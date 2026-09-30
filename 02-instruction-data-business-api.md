# Instruction 02 — Data Handling, Business Logic, API Layer and Academic Rules

## Ready-to-paste prompt

Continue the Academic Management System from the existing repository.

Read:

1. SRS.
2. `progress.md`.
3. Existing source code before adding anything.

Do not rebuild completed foundation work.

## Goal

Implement the complete data handling layer, business logic layer, API/controller layer, and security/authorization behavior for the academic modules.

The UI must remain thin. The services must be fully unit-testable without Swing.

Required flow:

`View → Controller/API → Service/Policy → Repository/DAO → JDBC/MySQL`

## A. Data handling layer

Implement DAO/repository interfaces and JDBC implementations for the SRS entities.

Each DAO must:

- use prepared statements.
- use try-with-resources.
- map `ResultSet` rows cleanly.
- translate SQL exceptions into `DataAccessException`.
- avoid business calculations.
- avoid authorization decisions.
- use transactions for multi-table operations where required.

Implement queries for:

### User and roles

- authenticate active user.
- retrieve user by username.
- retrieve role profile.
- create/update/deactivate user.
- search users.

### Course structure

- departments.
- batches.
- courses.
- components.
- semesters.
- offerings.
- enrolments.
- lecturer assignment.

### Academic data

- assessments.
- marks.
- attendance sessions.
- attendance records.
- medical records.
- course materials.
- notices.
- timetable/timetable entries.
- grade schemes.
- audit events.

Add all required unique/FK/index assumptions from the SRS.

## B. Transaction support

Implement transaction boundaries at the business-service level.

Examples:

- create course + components + offering configuration.
- publish assessment configuration and related data.
- save an attendance session and student attendance records.
- approve a medical record and create the corresponding audit event.
- update sensitive academic records plus audit log.

Pattern:

```text
begin transaction
    DAO operation 1
    DAO operation 2
    DAO operation 3
commit

on failure:
    rollback
    log technical cause
    rethrow safe application exception
```

Do not commit part of a logically atomic business operation.

## C. Authorization

Implement service-layer authorization.

The UI may hide unauthorized actions, but the service MUST reject unauthorized requests.

Enforce:

- Admin-wide user/master-data access.
- Lecturer access limited to assigned course offerings.
- Technical Officer access limited to permitted department/current-semester attendance and medical scope.
- Undergraduate access limited to their own permitted profile/results/notices/materials/timetable.
- Report access based on the SRS permission matrix.

Create reusable authorization policies rather than repeating role strings in every service.

## D. Authentication

Implement `AuthService`.

Flow:

```text
LoginView
  ↓
AuthController
  ↓
AuthService
  ↓
UserDAO
  ↓
password hash verification
  ↓
authenticated session + role
  ↓
dashboard
```

Audit successful/failed authentication without logging passwords.

Handle:

- blank username/password.
- unknown username.
- invalid password.
- inactive account.
- locked/unauthorized states if implemented.

## E. Course and enrollment business logic

Implement:

- unique course code validation.
- positive credits.
- valid components.
- duplicate offering prevention.
- enrolment conflict prevention.
- normal/repeat/batch-missed attempt status.
- only one active enrolment per offering/attempt combination.

Preserve historical attempts.

## F. Assessment and marks business logic

Assessment requirements:

- belongs to one offering.
- configurable assessment type.
- weight 0–100.
- published plan totals 100%.
- marks are 0–100.
- lecturer must be authorized for the offering.

Marks:

- one mark per assessment/enrolment.
- revise where allowed.
- audit sensitive changes.
- reject invalid mark range.
- do not permit unauthorized course entry.

Build a reusable calculator:

```text
Final Mark =
Σ(assessment mark × assessment weight / 100)
```

CA must use the configured CA components.

CA eligibility:

`CA >= 40.00`

## G. Attendance business logic

Implement separate calculations for:

- theory.
- practical.
- combined.

Rule:

`Attendance % = eligible attended sessions / scheduled sessions × 100`

Default planned sessions:

- 15 theory.
- 15 practical.

For a course with both components:

- display combined attendance.
- eligibility requires every required component to reach at least 80%, unless a formally configured policy changes it.

Use approved medical records according to BR-009:

- approved medical absence may remove eligible absence sessions from the denominator.
- do not count those sessions as present.
- pending/rejected medical records do not alter attendance.

The calculation code must be pure enough to unit test with no database.

## H. Medical business logic

Implement:

- student/enrolment validation.
- date validation.
- reason required.
- pending/approved/rejected states.
- duplicate/overlap detection.
- eligibility effect only when approved.
- audit approval/rejection.

Do not upload through a public web portal. Store controlled file path/reference according to the SRS.

## I. Eligibility business logic

Implement `EligibilityPolicy`.

The final examination is eligible only when:

`CA eligibility == true AND attendance eligibility == true`

Return failed conditions.

Example response:

```text
Eligible: false
CA: PASS
Attendance: FAIL
Failed conditions:
- Practical attendance below 80%
```

Never return a simple Boolean without an explanation because BR-010 requires failed conditions to be displayed.

## J. Grade, SGPA and CGPA

Implement grade lookup using the SRS grading scheme:

- 85–100 → A+ → 4.00
- 80–84 → A → 4.00
- 75–79 → A− → 3.70
- 70–74 → B+ → 3.30
- 65–69 → B → 3.00
- 60–64 → B− → 2.70
- 55–59 → C+ → 2.30
- 50–54 → C → 2.00
- 45–49 → C− → 1.70
- 40–44 → D+ → 1.30
- 35–39 → D → 1.00
- 0–34 → E → 0.00

Keep grade schemes versioned so historic results remain reproducible.

SGPA:

`Σ(course credit × grade point) / Σ(course credits counted in semester)`

CGPA:

`Σ(course credit × grade point across counted completed attempts) / Σ(counted course credits)`

Display both to 2 decimal places.

Keep the repeat-course result-selection rule configurable. The SRS default is latest completed attempt for CGPA while retaining all attempts for audit.

## K. Timetable business logic

Validate:

- start before end.
- no invalid room/lecturer/course overlap.
- correct department/semester.
- correct component/offering links.

Reject conflicting entries with a friendly message.

## L. Notice/material/profile/report services

Implement:

- notice validation, audience scope, publish/expiry rules.
- lecturer material ownership.
- student enrolled-course material read access.
- profile edit restrictions.
- report filtering and scope control.
- audit creation for sensitive changes.

## M. Internal API layer

For each major module create controller methods with request/response objects.

Example:

```java
public LoginResponse login(LoginRequest request);
public CourseResponse createCourse(CreateCourseRequest request);
public SaveMarksResponse saveMarks(SaveMarksRequest request);
public EligibilityResponse evaluate(EligibilityRequest request);
public GpaResponse calculateGpa(GpaRequest request);
```

Controllers should remain orchestration code.

They should not:

- query JDBC.
- calculate academic results directly.
- decide permissions independently.
- show Swing dialogs directly if avoidable.

Use an API result/error model so the UI receives safe, structured results.

## N. Unit testing

Write strong unit tests for business rules using mocks/fakes for DAOs.

Minimum test cases:

### Marks

- 0 accepted.
- 100 accepted.
- negative rejected.
- >100 rejected.
- assessment total not 100 rejected.

### CA

- 39.99 fails.
- 40.00 passes.
- values just above 40 pass.

### Attendance

- 12/15 = 80%.
- 13/15 > 80%.
- 10/15 < 80%.
- theory and practical calculated independently.
- combined value calculated correctly.
- both required components must meet threshold.
- approved medical changes denominator correctly.
- pending/rejected medical does not change result.

### Eligibility

- CA pass + attendance pass = eligible.
- CA fail + attendance pass = not eligible.
- CA pass + attendance fail = not eligible.
- both fail = not eligible.
- failure reasons returned.

### Grade

Test every grade boundary, especially:

- 84/85.
- 79/80.
- 74/75.
- 69/70.
- 64/65.
- 59/60.
- 54/55.
- 49/50.
- 44/45.
- 39/40.
- 34/35.

### GPA

Test:

- standard weighted GPA.
- multiple credits.
- decimal values.
- zero grade point.
- incomplete result.
- repeat selection rule.

### Authorization

Test every role against important protected operations.

## O. Integration tests

Against a real MySQL test database, verify:

- DAO CRUD.
- unique constraints.
- FK constraints.
- transaction rollback.
- seed/read operations.
- audit log creation.
- complex multi-table queries.

Use test schema/data isolation so tests do not destroy a developer's real database.

## O.1 Progress tracking

At the end:

1. Run unit tests.
2. Run integration tests.
3. Fix failures.
4. Update `progress.md`.

Record:

- services completed.
- DAOs completed.
- controllers/API completed.
- business rules tested.
- integration status.
- known limitations.
- next phase: Swing UI integration.
