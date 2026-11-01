# Architecture

The Academic Management System follows this dependency direction:

Presentation/Swing UI -> Internal API/controllers -> Business services/policies -> Data repositories/DAOs -> JDBC/MySQL.

Domain and calculation code remains independent of Swing and JDBC. JDBC access uses prepared
statements, `JdbcExecutor`, row mappers, and `DataAccessException`; `TransactionManager` supplies a
single connection to service-orchestrated multi-row operations. DAOs do not enforce role permissions
or calculate academic results.

Pure calculators cover assessment weighting, CA marks, attendance, eligibility, grading, and GPA;
policies are unit-testable without a database. Transactional services cover users/profiles,
courses/enrolments, assessments/marks, attendance, medical approvals, materials/notices, timetables,
versioned result snapshots, and attendance/marks reports. Role and data-scope checks are enforced in
services, and sensitive writes include audit events.

The internal API exposes structured controller/DTO operations for authentication, users, courses,
assessments, marks, attendance, medical records, eligibility, GPA, profiles, timetables,
materials/notices, reports, and result finalization. API failures are logged and returned as safe,
typed error responses. Report-service coverage remains limited to attendance and marks.
