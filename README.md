# Faculty of Technology Academic Management System

## 1. Project overview

The Faculty of Technology Academic Management System is a desktop application
for academic administration at the University of Ruhuna. It uses Java 25,
Swing, JDBC, Maven, and MySQL or MariaDB. Role-based access is provided for
Administrators, Lecturers, Technical Officers, and Undergraduates.

## 2. Prerequisites

- JDK 25.
- Git.
- MySQL 8.0.16 or later, or MariaDB 10.6 or later. These floors provide the
  enforced `CHECK` constraints used by the schema. MariaDB Connector/J is used
  because it supports MySQL and MariaDB servers.
- A running database server and its command-line client (`mysql` or `mariadb`).
- The Maven Wrapper included here. It downloads the pinned Maven distribution
  the first time it runs; a separate Maven installation is optional.
- Write access to `src/main/resources/uploads` when the application is used to
  upload or replace course materials.

The SQL client must support `DELIMITER` and stored procedures. The `mariadb`
and `mysql` command-line clients do. The wrapper requires network access to
download Maven and project dependencies on first use.

## 3. Clone the repository

```sh
git clone https://github.com/nextgeneratio/OOPP-2026-GP-1.git
cd OOPP-2026-GP-1
```

All commands below run from this repository directory.

## 4. Start the database

On Linux with systemd, start the service installed on your machine:

```sh
sudo systemctl start mariadb
```

For MySQL, use `sudo systemctl start mysql`. On macOS with Homebrew, use
`brew services start mariadb` or `brew services start mysql`. On Windows, start
the installed MariaDB or MySQL service from Services. Confirm that the server
is listening on port 3306 before continuing.

## 5. Install the database

**Warning: `database/install.sql` replaces every row in the 30 application
tables in `university_management` with the demonstration dataset. Back up any
existing data before running it.** The script does not drop or recreate tables
and does not truncate tables. It creates missing tables, then checks that all
tables have the expected columns, indexes, constraints, InnoDB engine, and
collation. If that compatibility check fails, the data replacement does not
start. Database, table, and account DDL may remain because MySQL/MariaDB DDL is
not part of the later data transaction.

Run the one installer using a database administrator account:

```sh
mariadb -u root -p < database/install.sql
```

If you use the MySQL client, substitute `mysql` for `mariadb` in that command.
The administrator account is used only to install the database; the application
connects as the separate, database-scoped `ams_app` account.

For an existing demonstration database, add any missing aggregate quiz marks
without replacing other data by running
`mariadb -u root -p < database/repair_demo_quiz_marks.sql`.
To regenerate only synthetic demonstration attendance, with 15 sessions per
offering and 9-15 attended sessions per student/course, run
`mariadb -u root -p < database/repair_demo_attendance.sql`.
Then create finalized results for demo enrolments that meet the CA and
attendance rules, and make them available to calculated SGPA/CGPA, by running
`mariadb -u root -p < database/repair_demo_results_gpa.sql`.

The installer creates `university_management` with `utf8mb4`, creates all 30
tables, creates or resets `ams_app@localhost`, grants it only `SELECT`,
`INSERT`, `UPDATE`, and `DELETE` on this database, and transactionally replaces
all application rows. It seeds 34 demonstration accounts and academic records
and registers 45 learning materials. If an `ams_app@localhost` account already
exists, its password is reset to the documented local demonstration value and
its direct privileges are replaced by the four database-scoped grants.

For an existing database, save a backup first. For example:

```sh
mariadb-dump -u root -p university_management > university_management-backup.sql
```

The replacement data transaction rolls back on SQL errors. It does not roll
back database/table/user DDL that ran before the transaction.

## 6. Configure `application.properties`

Copy the safe example to an ignored local file in the project root:

```sh
cp src/main/resources/application.properties.example application.properties
```

The example contains the local database account created by the installer:

```properties
app.environment=development
db.url=jdbc:mariadb://localhost:3306/university_management
db.username=ams_app
db.password=AmsLocalOnly-ChangeMe-2026!
db.pool-size=5
academic.attendance-threshold=80.00
academic.cgpa-repeat-selection=LATEST_COMPLETED_ATTEMPT
academic.grade-scheme-version=UGC-2024-v1
```

The database login is separate from the application usernames and passwords
below. The installer’s JDBC password is a public local demo value, not a
production secret; change it before deploying outside a local demonstration.
`application.properties` is ignored by Git. Do not commit a real local
database password.

## 7. Run the application

From the repository root, run:

```sh
./mvnw exec:java
```

In IntelliJ IDEA, use JDK 25, import the `pom.xml` Maven project, set the
working directory to the repository root, and run
`lk.ac.ruhuna.fot.ams.Application` from
`src/main/java/lk/ac/ruhuna/fot/ams/Application.java`.

The supported demo launch is Maven/IDE execution from the repository root. The
built JAR is not a self-contained runtime distribution: uploaded resources are
resolved as filesystem paths under `src/main/resources/uploads`, so running
from a packaged JAR is not supported.

## 8. Demonstration login credentials

Every seeded application account uses the password `1234`:

| Role | Username(s) | Password |
|---|---|---|
| Administrator | `admin` | `1234` |
| Lecturer | `LE001`–`LE009` | `1234` |
| Technical Officer | `TE001`–`TE004` | `1234` |
| Undergraduate | `ST001`–`ST020` | `1234` |

These accounts are for local demonstration and testing only. The shared short
password must not be used for real users or a production deployment.

## 9. Learning materials

The installer registers 45 generated PDFs at
`src/main/resources/uploads/materials/demo-readings/`, five for each of nine
demo courses. Stored database references begin with
`uploads/materials/demo-readings/` and are resolved relative to
`src/main/resources` when the application runs from the repository root.
Students see only published materials whose files exist and are readable.
Keep these PDFs in place and ensure the application process has read permission;
grant write permission to the uploads directory only when uploading or replacing
materials. The PDFs are bundled for Maven/IDE execution and are not extracted
from a JAR.

## 10. Run tests

```sh
./mvnw clean test
./mvnw clean verify
```

Database connectivity tests are separate from unit tests and only use a
database whose name ends in `_test`. Configure `DB_USERNAME` and `DB_PASSWORD`
or the ignored `test-credentials.properties` file. An optional `DB_URL` must
also target an `_test` database; the connectivity test refuses other database
names. The installer is never used by the test suite and must not be pointed at
the live/demo database for testing.

## 11. Common installation problems

- **Connection refused:** confirm the database service is running and listening
  on port 3306; check the host and port in `db.url`.
- **Access denied for `ams_app`:** rerun the installer with a database
  administrator if its local password/grants need resetting, then use the exact
  `db.username` and `db.password` from the example. Do not use root in the app.
- **Unknown database:** check the installer output and confirm
  `db.url` names `university_management`.
- **Missing `application.properties`:** copy the example from section 6 and
  run the app from the repository root.
- **Missing PDF or unreadable material:** restore the file from Git, keep its
  relative path unchanged, and check read permissions on the uploads directory.
- **Wrong Java version:** check `java -version` and select JDK 25 for Maven and
  IntelliJ.
- **Maven dependency download fails:** check network/proxy access and rerun the
  wrapper; its first run downloads Maven and dependencies.
- **Installer schema preflight fails:** stop and inspect the reported
  compatibility issue. The installer intentionally leaves application rows
  untouched when existing columns, indexes, constraints, engines, or collations
  do not match the canonical schema. Back up and resolve schema drift first.

## 12. Security note

The shared application password `1234` and the JDBC account’s documented local
demo password are for local testing only. The JDBC account is separate from
application accounts and has only database-scoped CRUD grants; never configure
the application to use the database root account. Give real users individual
strong passwords, change the demo JDBC password before non-local use, and keep
`application.properties`, `.env`, `.envrc`, test credentials, and database
backups out of Git.

## Weekly timetable management

Administrators maintain one weekly timetable per Course Offering. Create and Update use one editor for department, academic term, level, course offering, and all Monday–Sunday slots; each slot selects one of the course's actual components. Existing course allocations are displayed while creating. Parallel courses are allowed when they use different rooms, batches and lecturers. Update versions the schedule so existing attendance references retain their original slot IDs. Delete deactivates the timetable and its active slots. Technical Officers can view authorized department schedules; lecturers and undergraduates see only their assigned/enrolled offering schedules.

For a new database, `database/install.sql` creates this model. Back up an existing database before migration:

```sh
mariadb-dump -u root -p --single-transaction university_management > university_management_before_timetable_migration.sql
```

If `20261105_weekly_timetable.sql` has not been applied, run these in order:

```sh
mariadb -u root -p university_management < database/migrations/20261105_weekly_timetable.sql
mariadb -u root -p university_management < database/migrations/20261106_course_specific_timetables.sql
```

If `20261105_weekly_timetable.sql` is already applied, run only:

```sh
mariadb -u root -p university_management < database/migrations/20261106_course_specific_timetables.sql
```

Review each migration's read-only preflight output first. Stop and resolve duplicate active offering assignments or invalid mappings before continuing. The course-specific migration only backfills a legacy header if all its slots have exactly one populated offering/component pair and the offering matches its department, term and level. Ambiguous and generic legacy schedules remain unassigned. Both migrations preserve timetable slot IDs and attendance references. Do not run `database/install.sql` as an upgrade; it is the demo-data installer. No migration was executed against the configured application database.
