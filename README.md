# Faculty of Technology Academic Management System

Java 17+, Swing, Maven, JDBC, and MySQL/MariaDB desktop application.

## Run the application

1. Ensure MySQL/MariaDB is running and the database `university_management`
   exists. Use an account scoped to this database; do not use the database root
   account for normal application use.
2. Create a local `application.properties` file in the repository root. This
   file is ignored by Git:

   ```properties
   app.environment=development
   db.url=jdbc:mysql://localhost:3306/university_management
   db.username=YOUR_DATABASE_USER
   db.password=YOUR_DATABASE_PASSWORD
   db.pool-size=5
   academic.attendance-threshold=80.00
   academic.cgpa-repeat-selection=LATEST_COMPLETED_ATTEMPT
   academic.grade-scheme-version=UGC-2024-v1
   ```

   Keep the username/password local and never commit them.
3. From the repository root, run:

   ```sh
   mvn exec:java
   ```

   Or open the project in IntelliJ IDEA, allow Maven to load dependencies, and
   run `lk.ac.ruhuna.fot.ams.Application` (`src/main/java/.../Application.java`).
   The login window should appear. Sign in with a valid existing application
   account for the configured database; the database account is not an app login.

Run the checks from a terminal with:

```sh
mvn clean test
```

The MySQL connectivity test uses the isolated `university_management_test`
database by default. Configure `DB_USERNAME` and `DB_PASSWORD`, or the ignored
repository-root `test-credentials.properties`, before running it. An optional
`DB_URL` override must name a database ending in `_test`; the test refuses to
connect to a development/live database.

## Database setup

`scripts/create_database.sql` creates the development database and requires
appropriate database-creation privileges. Select `university_management`
explicitly when applying `src/main/resources/db/schema.sql`.

The seed script truncates existing academic/user tables before inserting its
development sample data. Only run it against a database you intend to reset.
For the existing demo database, do not rerun that destructive seed. Its schema
was brought forward with the one-time additive
[live-schema-compatibility.sql](./src/main/resources/db/live-schema-compatibility.sql)
migration, and synthetic academic records were added with the rerunnable,
non-destructive [demo-data-additive.sql](./src/main/resources/db/demo-data-additive.sql)
script. Those scripts assume the demo users and base course records already
exist; historical course results are finalized through the application service.
For isolated test database instructions, see [docs/testing-strategy.md](./docs/testing-strategy.md).
The in-progress academic workflow changes also include
[academic-workflow-compatibility.sql](./src/main/resources/db/academic-workflow-compatibility.sql).
Do not apply it to a live database until existing department codes and batch
levels and timetable levels have been reviewed; it adds the ICT/BST/ET-only
constraint, and existing batch/timetable rows otherwise default to level 1.

## Current UI scope

The shared shell includes role-specific dashboards, administrator user/student
account management, and self-service profiles/password changes. Course
management, course offering lookup, level/semester timetables, course materials,
MCQ quizzes, assignment submission, attendance, assessments/marks, eligibility,
results/GPA, and reports have role-gated workflows.
Quiz submissions are one-attempt per student, server-scored, and password checked;
assignment uploads allow PDF/Office/text/image files up to 10 MB, with updates
allowed before the deadline. Use “Course Offerings” to view authorised offerings.
These database-backed workflows require
[academic-workflow-compatibility.sql](./src/main/resources/db/academic-workflow-compatibility.sql)
to be reviewed and applied to the target database first. Medical, marks, and
The four academic screens use controller/service authorization; final Swing and
persistence acceptance still requires the isolated test database described in
[docs/testing-strategy.md](./docs/testing-strategy.md).

For results and GPA, a Lecturer selects a student through an offering to which
they are assigned. Detailed finalized results remain limited to that offering.
The GPA view deliberately returns only the student's aggregate SGPA/CGPA after
that relationship is authorised; it does not reveal unrelated course rows or
marks. An undergraduate can view only their own results and GPA.
