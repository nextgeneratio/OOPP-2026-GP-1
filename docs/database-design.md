# Database Design

The authoritative MySQL DDL is `src/main/resources/db/schema.sql`; development seed data is in
`src/main/resources/db/seed.sql`. The schema covers users and role profiles, departments, batches,
courses and components, semesters and offerings, enrolment attempts, assessments and marks,
attendance, medical records, materials, notices, timetables, versioned grades, and audit events.

The schema enforces foreign keys, unique identifiers, numeric/date checks, indexes, soft-deactivation
for users/departments/courses, repeat-attempt history, and one active enrolment per student/offering.
Course offering lecturer assignments are represented separately from the optional coordinator.
Notice audience targets and controlled medical evidence references are stored explicitly.
Completed results store a grade-scheme version and credit snapshot; re-finalization archives the
previous result snapshot instead of overwriting its scheme history.

Multi-row business operations use `TransactionManager`; DAO operations accept the transaction
connection so services can commit or roll back complete workflows. The test profile targets the
separate `university_management_test` database and contains no credentials.

Live MySQL execution and constraint behavior still require verification against an isolated test
database; they have not been executed as part of this change.
