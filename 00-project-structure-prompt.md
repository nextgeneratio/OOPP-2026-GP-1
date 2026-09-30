# Academic Management System — Project Structure Prompt

## Purpose

Use this prompt with the coding assistant inside IntelliJ IDEA before implementation starts. It defines the project structure and architectural rules for the Faculty of Technology Academic Management System in the SRS.

## Ready-to-paste prompt

You are the lead Java architect and senior Swing developer for our **Faculty of Technology Academic Management System (AMS)** mini project.

Read the attached/available SRS and treat it as the authoritative functional specification. Do not silently add, remove, or change business rules. Where the SRS explicitly marks a rule as an assumption, keep it configurable and document the assumption in code.

### 1. Technology baseline

Build the project as an IntelliJ IDEA Maven Java desktop application:

- Java 17 or newer.
- Swing for all desktop UI.
- MySQL 8+ as the database.
- JDBC for database access.
- Maven for dependency/build management.
- JUnit 5 for testing.
- Mockito for service/controller unit tests.
- AssertJ for fluent assertions.
- AssertJ Swing may be used for UI/system smoke tests.
- Use a modern Swing look-and-feel library such as FlatLaf for a clean UI, but do not allow it to leak business logic into presentation code.
- Use a secure password hashing implementation based on PBKDF2WithHmacSHA256 or another approved Java-supported password-hashing algorithm. Hashing/salt configuration must be centralized and no password may be stored in plaintext.
- Use a logging framework such as SLF4J + Logback.
- Do not use Spring or Spring Boot. This is a standalone desktop OOP project.

### 2. Interpretation of the required layers

The application MUST have clearly separated layers. The required **API handling layer is an internal application API layer**, not an HTTP REST API.

The required dependency direction is:

Presentation/Swing UI
        ↓
API handling / Controllers
        ↓
Business Logic / Services / Policies
        ↓
Data Handling / Repositories / DAOs
        ↓
JDBC / MySQL

Cross-cutting error handling and security may be called by the appropriate layers, but business rules must not move into the UI or DAO.

Never allow:

- Swing components to execute SQL.
- Swing components to calculate grades/SGPA/CGPA/eligibility.
- DAOs to decide role permissions or academic business rules.
- SQL strings inside services/controllers/views.
- Controllers to contain large business calculations.
- Exceptions to be swallowed silently.
- GUI button visibility to be the only authorization mechanism.

### 3. Package name

Use a consistent base package:

`lk.ac.ruhuna.fot.ams`

Do not create random package names.

### 4. Required project structure

Create this Maven layout:

```text
AcademicManagementSystem/
├── pom.xml
├── README.md
├── progress.md
├── .gitignore
├── docs/
│   ├── architecture.md
│   ├── database-design.md
│   └── testing-strategy.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── lk/
│   │   │       └── ac/
│   │   │           └── ruhuna/
│   │   │               └── fot/
│   │   │                   └── ams/
│   │   │                       ├── Application.java
│   │   │                       │
│   │   │                       ├── domain/
│   │   │                       │   ├── model/
│   │   │                       │   ├── enums/
│   │   │                       │   └── valueobject/
│   │   │                       │
│   │   │                       ├── api/
│   │   │                       │   ├── controller/
│   │   │                       │   ├── request/
│   │   │                       │   ├── response/
│   │   │                       │   └── mapper/
│   │   │                       │
│   │   │                       ├── business/
│   │   │                       │   ├── service/
│   │   │                       │   ├── policy/
│   │   │                       │   ├── calculator/
│   │   │                       │   └── validator/
│   │   │                       │
│   │   │                       ├── data/
│   │   │                       │   ├── dao/
│   │   │                       │   ├── repository/
│   │   │                       │   ├── mapper/
│   │   │                       │   ├── connection/
│   │   │                       │   └── transaction/
│   │   │                       │
│   │   │                       ├── error/
│   │   │                       │   ├── exception/
│   │   │                       │   ├── handler/
│   │   │                       │   └── message/
│   │   │                       │
│   │   │                       ├── security/
│   │   │                       │   ├── password/
│   │   │                       │   ├── session/
│   │   │                       │   └── authorization/
│   │   │                       │
│   │   │                       ├── audit/
│   │   │                       └── reporting/
│   │   │
│   │   └── resources/
│   │       ├── application.properties.example
│   │       ├── logback.xml
│   │       ├── db/
│   │       │   ├── schema.sql
│   │       │   └── seed.sql
│   │       └── ui/
│   │           └── icons/
│   │
│   └── test/
│       ├── java/
│       │   └── lk/
│       │       └── ac/
│       │           └── ruhuna/
│       │               └── fot/
│       │                   └── ams/
│       │                       ├── unit/
│       │                       ├── integration/
│       │                       └── system/
│       └── resources/
│           ├── test-application.properties
│           ├── db/
│           │   ├── test-schema.sql
│           │   └── test-seed.sql
│           └── test-data/
│
└── scripts/
    ├── create_database.sql
    ├── reset_database.sql
    └── backup_database.sql
```

### 5. Naming rules

Use clear names:

- `User`, `Admin`, `Lecturer`, `TechnicalOfficer`, `Undergraduate`
- `Course`, `CourseComponent`, `Semester`, `Batch`, `CourseOffering`
- `CourseEnrollment`, `Assessment`, `Mark`
- `AttendanceSession`, `AttendanceRecord`
- `MedicalRecord`
- `CourseMaterial`
- `Notice`, `Timetable`, `TimetableEntry`
- `Grade`
- `AuditLog`

DAO classes should follow names such as:

- `UserDao`
- `CourseDao`
- `EnrollmentDao`
- `AssessmentDao`
- `MarkDao`
- `AttendanceSessionDao`
- `AttendanceRecordDao`
- `MedicalRecordDao`
- `CourseMaterialDao`
- `NoticeDao`
- `TimetableDao`
- `GradeDao`
- `AuditLogDao`

Services should follow names such as:

- `AuthService`
- `UserService`
- `CourseService`
- `MaterialService`
- `AttendanceService`
- `MedicalService`
- `AssessmentService`
- `MarksService`
- `EligibilityService`
- `GradeService`
- `GpaService`
- `NoticeService`
- `TimetableService`
- `ReportService`
- `AuditService`

Controllers should follow names such as:

- `AuthController`
- `UserController`
- `CourseController`
- `AttendanceController`
- `MedicalController`
- `MarksController`
- `EligibilityController`
- `GradeController`
- `GpaController`
- `NoticeController`
- `TimetableController`
- `MaterialController`
- `ReportController`
- `ProfileController`

### 6. Domain and OOP requirements

The SRS explicitly requires OOP concepts. Implement them deliberately:

- Encapsulation through private fields and validated methods.
- Abstract `User` with role-specific subclasses.
- Interfaces such as `Repository<T>`, `Authenticatable`, and `EligibilityPolicy`.
- Polymorphism for dashboard permissions and eligibility policies.
- Composition between course offerings, components, assessments, and related records.
- Value objects where they simplify validation, such as email/phone/percentage/mark representations.
- Keep domain objects independent of Swing and JDBC.

Never put `JFrame`, `JPanel`, `Connection`, `PreparedStatement`, or SQL code inside domain classes.

### 7. Internal API layer

The API handling layer must provide a stable boundary between UI and business logic.

For each module, create request/response DTOs where useful. For example:

```text
LoginRequest
LoginResponse
CreateCourseRequest
UpdateCourseRequest
EnterMarkRequest
AttendanceSaveRequest
EligibilityResponse
GradeResultResponse
GpaResponse
ReportRequest
```

Controllers should:

1. Receive a request from the Swing view.
2. Perform lightweight request mapping.
3. Call the correct business service.
4. Convert domain/service results to response DTOs.
5. Pass business exceptions to the centralized error handler.
6. Return a safe result for the GUI.

### 8. Error handling layer

Create centralized custom exceptions at minimum:

- `ValidationException`
- `AuthenticationException`
- `AuthorizationException`
- `NotFoundException`
- `DataAccessException`
- `BusinessRuleException`

Create an `ApplicationErrorHandler` / equivalent that converts technical/business exceptions to safe GUI messages.

Rules:

- Never display SQL text to ordinary users.
- Never display stack traces to ordinary users.
- Log technical details securely.
- Preserve a correlation/reference ID when practical.
- Controllers must not swallow errors.
- DAO code must wrap checked SQL failures in `DataAccessException`.
- Transaction failures must roll back and propagate a safe failure.

### 9. Data handling layer

All MySQL operations go here.

Use:

- JDBC prepared statements.
- `try-with-resources`.
- Explicit transaction boundaries for multi-table operations.
- Row mappers / result mapping classes.
- Repository interfaces where useful.
- Connection provider.
- Transaction manager/helper.

The GUI and business services must never construct SQL statements.

The schema must follow the SRS table/relationship design and preserve:

- foreign keys,
- unique constraints,
- checks,
- indexes,
- soft-delete/deactivation,
- attempt numbers for repeat/batch-missed enrolments.

### 10. Business logic layer

All academic rules belong here.

At minimum centralize:

- CA calculation.
- CA eligibility: CA >= 40.00%.
- Theory attendance percentage.
- Practical attendance percentage.
- Combined attendance percentage.
- Attendance eligibility.
- Medical-excused attendance handling.
- Final examination eligibility.
- Final mark calculation from weighted assessments.
- Grade and grade-point lookup.
- SGPA.
- CGPA.
- Repeat-attempt selection policy.
- Role and data-scope authorization checks.

Never duplicate these calculations in multiple controllers/views.

### 11. UI layer organization

Organize Swing screens by role/module:

```text
presentation/ui/
├── common/
├── auth/
├── admin/
├── lecturer/
├── technical/
└── undergraduate/
```

Use a shared `MainFrame` / navigation shell and reusable components:

- `AppButton`
- `AppTextField`
- `AppPasswordField`
- `AppComboBox`
- `AppTable`
- `ValidationLabel`
- `StatusBadge`
- `ConfirmationDialog`
- `LoadingDialog`
- `ErrorDialog`

Screens required by the SRS include:

- Login
- Admin dashboard
- Lecturer dashboard
- Technical Officer dashboard
- Undergraduate dashboard
- User Management
- Student Management
- Course Management
- Attendance
- Medical Management
- Marks Entry
- Eligibility
- Grade / course results
- SGPA / CGPA
- Timetable
- Notices
- Course Materials
- Profile
- Reports

### 12. Visual design system

Create one centralized UI theme/config class. Do not hard-code colors throughout individual panels.

Use this academic/desktop palette:

- Primary: `#123B5D` — dark academic blue.
- Primary hover: `#1D5A87`.
- Secondary: `#2F7D8C` — teal.
- Accent: `#D9A441` — restrained academic gold.
- Background: `#F5F7FA`.
- Surface/card: `#FFFFFF`.
- Main text: `#1F2937`.
- Muted text: `#667085`.
- Border: `#D0D5DD`.
- Success: `#2E7D32`.
- Warning: `#B7791F`.
- Error: `#C62828`.
- Info: `#1565C0`.

Do not depend on color alone for status. Use text/icons as required by the SRS.

Typography:

- Main font: `Inter` when available; otherwise system sans-serif.
- Title: 24–28 px equivalent, bold.
- Section heading: 18–20 px, semibold/bold.
- Body: 14 px.
- Table/body secondary text: 13 px.
- Button labels: 14 px, semibold.
- Small helper/error text: 12–13 px.

UI principles:

- Consistent 8 px spacing rhythm.
- 16–24 px outer padding.
- Clear hierarchy.
- Visible keyboard focus.
- Tooltips for unfamiliar icons.
- Confirm destructive/locking actions.
- Disable or hide unauthorized actions according to role, while always enforcing authorization in the service layer.
- Keep forms aligned and tables readable.
- Use icons plus text for critical actions.
- Avoid excessive gradients, shadows, animations, or decorative elements.

### 13. Progress tracking

Create `/progress.md` at the repository root.

After every meaningful implementation task, update it.

It must contain:

```markdown
# Project Progress

## Current phase
...

## Completed
- ...

## In progress
- ...

## Blocked
- ...

## Tests
- Unit:
- Integration:
- System:

## Database
- Schema:
- Seed data:
- Connection:

## Architecture status
- Presentation:
- API:
- Business:
- Data:
- Error:
- Security:
- Audit:

## Known issues
- ...

## Files changed
- ...

## Next task
...

## Last updated
YYYY-MM-DD HH:mm
```

Never mark a feature complete unless its implementation and relevant tests are complete.

### 14. Git/change discipline

Prefer small, coherent changes. Do not refactor unrelated modules during a feature task.

Before making a change:

1. Read `progress.md`.
2. Inspect existing architecture.
3. Reuse existing patterns.
4. Implement.
5. Run relevant tests.
6. Update `progress.md`.
7. Record remaining issues.

### 15. Definition of done

A module is complete only when:

- Domain model exists and is validated.
- API/controller boundary exists.
- Business service exists.
- Data access exists.
- Error mapping exists.
- Authorization is enforced in the service layer.
- Required database constraints exist.
- Unit tests cover business rules.
- Integration tests cover DAO/database behavior where applicable.
- UI is connected to the internal API/controller layer.
- Role restrictions are verified.
- `progress.md` is updated.

Do not build a monolithic `Main.java` or a giant service class. Keep each responsibility in the correct layer.
