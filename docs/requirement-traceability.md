# SRS Requirement Traceability and Verification Status

**Basis:** current `SRS/SRS.html`, assignment brief `ICT2132 - Mini Project -
B09.pdf`, and the explicit role policies supplied with the 2026-10-30 audit.
This map identifies the implemented path and the evidence actually available;
it is not a claim that every workflow has passed acceptance.

| SRS requirement | UI screen | Controller | Service/policy | DAO | Database tables | Automated evidence | Live status |
|---|---|---|---|---|---|---|---|
| FR-001, BR-001: login, active account, password verification | `LoginPanel` | `AuthController` | `AuthService`, `PasswordHasher` | `JdbcUserDao` | `users`, role profile tables, `audit_logs` | `AuthServiceTest`, `PasswordHasherTest`, `ErrorHandlingAndUxSafetyTest` | Unit pass; live credential/login flow blocked |
| FR-002, FR-004: admin user maintenance and self profiles | `UserManagementScreen`, `ProfileScreen` | `UserController`, `ProfileController` | `UserService`, `ProfileService`, `AuthorizationService` | `JdbcUserDao`, `JdbcAuditLogDao` | `users`, `admins`, `lecturers`, `technical_officers`, `undergraduates`, `audit_logs` | `ProfileServiceTest`, `AuthorizationServiceTest` | Profile reads pass live for all roles; CRUD/password persistence blocked |
| FR-003: departments, batches, courses, offerings, components, lecturer assignment | `CourseManagementScreen`, `CourseOfferingsScreen` | `CourseController` | `CourseService`, authorization | `JdbcCourseDao` | `departments`, `batches`, `courses`, `course_components`, `semesters`, `course_offerings`, `course_offering_lecturers`, `course_enrollments`, `audit_logs` | `DomainValidationTest`, `AuthorizationScopeServiceTest` | Read paths pass; mutations and constraints need isolated DB |
| FR-003, FR-013: timetable creation and scoped viewing | `TimetableScreen` | `TimetableController` | `TimetableService`, `TimetableConflictValidator` | `JdbcTimetableDao` | `timetables`, `timetable_entries`, course/offering tables | `AcademicRulesTest`, `AuthorizationScopeServiceTest` | Service rules covered in part; live persistence/UI blocked |
| FR-005: assigned course materials | `CourseMaterialsScreen` | `ContentController` | `ContentService` | `JdbcContentDao` | `course_materials`, offerings, lecturer/enrollment tables | UI/navigation coverage; no dedicated persistence test | Schema/read access not acceptance-tested; file workflow blocked |
| Learning activities: quizzes, questions, attempts, assignments, submissions | `LearningActivitiesScreen` | `LearningActivityController` | `LearningActivityService` | `JdbcLearningActivityDao` | `course_quizzes`, `quiz_questions`, `quiz_attempts`, `course_assignments`, `assignment_submissions` | `LearningActivityServiceTest` | Service tests pass; isolated persistence blocked |
| FR-006/007, BR-004/011: assessment plans, 100% weights, CA, marks | `AssessmentsMarksScreen`, lecturer `MarksScreen` | `AssessmentController`, `MarksController` | `AssessmentService`, `MarksService`, `AssessmentCalculator` | `JdbcAssessmentDao` | `assessments`, `marks`, `audit_logs`, `course_enrollments` | `AssessmentServiceSecurityTest`, new `AssessmentServiceWorkflowTest`, `MarksServiceWorkflowTest`, `AcademicRulesTest` | Unit transaction/validation pass; live plan/mark reads pass; live writes blocked |
| FR-008/009, BR-006–009: attendance sessions, theory/practical, medical calculation | common role-adaptive `AttendanceScreen` | `AttendanceController` | `AttendanceService`, `AttendanceCalculator`, `AuthorizationService` | `JdbcAttendanceDao`, `JdbcCourseDao`, `JdbcMedicalDao` | `attendance_sessions`, `attendance_records`, `course_enrollments`, `course_components`, `medical_records`, timetable tables | `AttendanceWorkflowPermissionsTest`, `AttendanceTechnicalOfficerPermissionsTest`, `AttendanceSessionRosterTest`, `AcademicRulesTest` | Unit/service reads and calculations pass; live DB schema/constraint read passes; write/delete only mocked; GUI not exercised |
| FR-010: medical submission/review | shared `MedicalScreen` | `MedicalController` | `MedicalService`, authorization | `JdbcMedicalDao`, `JdbcAuditLogDao` | `medical_records`, `users`, `undergraduates`, `course_offerings`, `audit_logs` | `MedicalServiceSecurityTest` | Service security pass; evidence/file and persistence workflows blocked |
| FR-011, BR-005/008/010: CA and attendance eligibility | `EligibilityScreen` | `EligibilityController` | `EligibilityService`, `FinalExamEligibilityPolicy` | `JdbcAssessmentDao`, `JdbcAttendanceDao`, `JdbcMedicalDao`, `JdbcCourseDao` | `assessments`, `marks`, `attendance_*`, `medical_records`, `course_enrollments` | `AcademicRulesTest`, `AuthorizationScopeServiceTest` | Policy unit pass; live read eligibility not part of this run |
| FR-012, BR-012–014: results, grade, SGPA, CGPA | `GradesGpaScreen` | `ResultController`, `GpaController` | `AcademicResultService`, `GradeService`, `GpaService`, `GpaCalculator` | `JdbcCourseResultDao`, `JdbcGradeDao`, `JdbcCourseDao` | `course_results`, `grades`, course/enrollment tables | `AcademicResultScopeTest`, `GpaAggregateAuthorizationTest`, `AcademicRulesTest` | Scope/calculation tests pass; live role result acceptance blocked |
| FR-013: notices | `NoticesScreen` | `ContentController` | `ContentService` | `JdbcContentDao` | `notices`, `batches`, `audit_logs` | Navigation and service rule tests | Read-only API smoke not repeated for notice audiences; writes blocked |
| FR-014: scoped reports, preview/export | `ReportScreen` | `ReportController` plus report runner | `ReportService`, `AuthorizationService` | course/attendance/assessment DAOs | report-specific source tables and `audit_logs` | `CsvWriterTest`, report formatter/category tests, authorization tests | Unit formatting/scope coverage; live report/export and large-result tests blocked |
| Dashboards, navigation, audit | role dashboards, `NavigationPanel`, `StatusBar` | `DashboardController` | `DashboardService`, `AuditService` | dashboard/audit/course DAOs | `users`, offerings, notices, academic tables, `audit_logs` | `DashboardServiceTest`, `NavigationModuleTest` | Profile/offering smoke only; each dashboard and audit write not end-to-end tested |

## Role policy check

- **Admin:** user/master-data management and attendance session viewing/deletion
  are exposed; assessment publication and mark entry are service-denied. A direct
  attendance delete write was not run.
- **Lecturer:** assigned-offering assessment/marks and read-only attendance are
  service-scoped. Lecturer medical approval is not exposed. Assessment reads and
  marks sheet loaded live; save/publish were not run against the application DB.
- **Technical Officer:** department/current-semester-scoped attendance create,
  update and medical review are service-scoped; attendance deletion is denied.
  No write acceptance ran.
- **Undergraduate:** profile, course and academic access is self/enrollment
  scoped. Attendance setup live returned one roster row for the sampled student;
  the student cannot fetch a whole session roster. No other-student UI/login
  acceptance ran.

## SRS Section 22 report coverage

`ReportCategory` currently implements only Student attendance, Batch/course
attendance, CA/final marks, and Eligibility. The report runner calls scoped
services/controllers for those categories and the report screen provides table
preview, printing, and CSV export. Empty-state handling exists, but real CSV
encoding, very large exports, filters, and query behavior have not been tested
against the isolated database.

The other Section 22 outputs are provided through dedicated screens rather than
report categories: Grades/course results and SGPA/CGPA use `GradesGpaScreen`;
medical submission/status/review uses `MedicalScreen`; and profile reads/updates
use `ProfileScreen`. This avoids duplicating those service paths. Dedicated
screen access is not evidence that the corresponding Section 22 report format,
filter set, or export is implemented. The SRS's Lecturer medical visibility
conflicts with the explicit policy excluding Lecturer medical navigation and
approval; the implementation keeps the explicit restriction and exposes no
Lecturer medical report.

The admin and undergraduate roles do not currently have the Reports navigation
module. Undergraduate own attendance, marks/results, medical status, and profile
are available through their dedicated role screens, but not through CSV reports.
Admin reporting is not currently exposed. These are incomplete SRS report
requirements; no broader report access was added in this follow-up.

The SRS describes Lecturer visibility of some undergraduate medical records and
reports, while the explicit policy for this audit restricts medical approval to
Admin and Technical Officer and excludes Lecturer medical navigation. This
implementation retains the explicit role restriction; it does not grant
Lecturer approval access.

## Test boundary

Latest `mvn clean verify`: **94 discovered, 93 passed, 0 failed, 0 errors, 1
skipped** (`DatabaseConnectivityTest`, because test DB credentials are absent).
The configured `university_management` database was queried read-only; its
Technical Officer-only attendance CHECK constraint is present. The isolated
`university_management_test` database was not used. No live GUI acceptance,
login, schema write, seed, CRUD, or transaction-persistence test is claimed. See
[testing-strategy.md](./testing-strategy.md) for safe test DB setup.
