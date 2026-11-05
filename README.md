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

For the default local demo setup, the application loads the tracked example
configuration automatically when no project-root `application.properties`
exists. To customize the database settings, copy the example to an ignored
local file in the project root and edit it:

```sh
cp src/main/resources/application.properties.example application.properties
```

On Windows Command Prompt, use:

```bat
copy src\main\resources\application.properties.example application.properties
```

The example contains the local database account created by the installer:

```properties
app.environment=development
db.url=jdbc:mariadb://localhost:3306/university_management?allowPublicKeyRetrieval=true
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

`allowPublicKeyRetrieval=true` is included for the local MySQL demo connection,
which may use MySQL's default `caching_sha2_password` authentication without
TLS. For a remote database, use certificate-validated TLS or pin the server's
RSA public key instead of enabling unauthenticated key retrieval.

## 7. Run the application

From the repository root, run:

```sh
./mvnw exec:java
```

On Windows Command Prompt, run `mvnw.cmd exec:java` from the repository root.

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
- **SQLState `S1009` with MySQL:** update the local `db.url` to include
  `?allowPublicKeyRetrieval=true` for a local-only demo connection, or configure
  TLS/server-key pinning for a remote database.
- **Unknown database:** check the installer output and confirm
  `db.url` names `university_management`.
- **Custom database settings:** copy the example from section 6 to the project
  root as `application.properties` and edit it. Without that override, the app
  uses the bundled local demo configuration.
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

Administrators maintain one weekly timetable per Course Offering. Create and Update use one editor for department, academic term, level, course offering, and all Monday–Sunday slots; each slot selects a component belonging to that course. Update versions slots, while Delete deactivates the schedule. Historical timetable-entry identifiers remain available to attendance records.

### Upgrade a copy of an existing database for timetable testing (Fedora)

The commands below back up `university_management`, then restore that dump into the separate `university_management_timetable_test` database. Run the migration checks and migrations only against this test copy. Fedora provides the `mariadb` and `mariadb-dump` clients in the `mariadb` package.

**1. Back up the existing database.** Run from the repository root; the dump file is created in the current directory:

```sh
mariadb-dump -u root -p --single-transaction --routines --events --triggers university_management > university_management_before_timetable_migration.sql
```

**2. Create and restore into the isolated test database.** The `sed` changes only the dump's top-level `USE` directive, if present. It leaves the source database untouched:

```sh
mariadb -u root -p -e "CREATE DATABASE university_management_timetable_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sed 's/^USE `university_management`;/USE `university_management_timetable_test`;/' university_management_before_timetable_migration.sql | mariadb -u root -p university_management_timetable_test
```

If the test database already exists, stop and inspect it first; do not drop or recreate it blindly. The restore is intended for a newly created empty test database.

**3. Check migration state on the test copy.**

```sh
mariadb -u root -p university_management_timetable_test -e "SELECT table_name,column_name,is_nullable,extra FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('timetables','timetable_entries') AND column_name IN ('is_active','offering_id','component_id','location','course_offering_id','active_course_offering_id') ORDER BY table_name,ordinal_position; SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name IN ('timetables','timetable_entries') AND constraint_name IN ('chk_timetable_entries_optional_course_pair','fk_timetables_course_offering','uk_timetables_active_offering') ORDER BY table_name,constraint_name; SELECT table_name,index_name,non_unique FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='timetables' AND index_name='uk_timetables_active_offering';"
```

Interpretation:

- If this copy was created from the **current `database/install.sql`**, it already has the final schema. Run no migrations. Schema inspection cannot distinguish a fresh latest install from an upgraded database with the same schema; use the known database origin as well as the query.
- If neither migration is present, run the 20261105 preflight, then migration 20261105, then the 20261106 preflight, then migration 20261106.
- If 20261105 is present but `course_offering_id`, its foreign key, or the active-offering unique index is absent, run only the 20261106 preflight and migration.
- If 20261106 is present (column, FK, and unique index), do not run it again.
- If the results show a partial/mixed state, stop. Do not guess or rerun a non-idempotent migration; restore the test copy from the backup and review the schema discrepancy.

**4. Run only the preflight that matches the detected schema.** Both preflight files contain SELECT statements only. Run one command, inspect its output, and decide whether the next command applies. Do not paste or replay a whole sequence blindly: migrations are not safe to rerun.

For an old database with neither migration applied, first run:

```sh
mariadb -u root -p university_management_timetable_test < database/migrations/preflight_20261105_weekly_timetable.sql
```

Only if that output matches the old schema, reports zero invalid null pairs and zero orphan attendance references, run:

```sh
mariadb -u root -p university_management_timetable_test < database/migrations/20261105_weekly_timetable.sql
```

Then check the 20261106 state:

```sh
mariadb -u root -p university_management_timetable_test < database/migrations/preflight_20261106_course_specific_timetables.sql
```

Continue only if its first status row says `READY FOR 20261106 PREFLIGHT REVIEW`, the duplicate candidate query returns no rows, and attendance references have no orphans. If it says `ALREADY APPLIED` or `STOP`, do not run the migration. If all checks pass, run:

```sh
mariadb -u root -p university_management_timetable_test < database/migrations/20261106_course_specific_timetables.sql
```

If 20261105 is already applied and 20261106 is not, run only the 20261106 preflight above and, if it reports the ready state and the remaining checks pass, run the 20261106 migration once. If 20261106 is already applied, do not run it again. These instructions apply only to the restored test copy.

**5. Verify schema, uniqueness, and attendance references.** The migration files also print these post-checks. Run this combined check after applying the needed migration:

```sh
mariadb -u root -p university_management_timetable_test -e "SELECT table_name,column_name,column_type,is_nullable,extra FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('timetables','timetable_entries') AND column_name IN ('is_active','offering_id','component_id','location','course_offering_id','active_course_offering_id') ORDER BY table_name,ordinal_position; SELECT table_name,constraint_name,constraint_type FROM information_schema.table_constraints WHERE constraint_schema=DATABASE() AND table_name IN ('timetables','timetable_entries') AND constraint_name IN ('chk_timetable_entries_optional_course_pair','fk_timetables_course_offering','fk_timetable_entries_offering','fk_timetable_entries_component','fk_timetable_entries_timetable') ORDER BY table_name,constraint_name; SELECT course_offering_id,COUNT(*) AS active_count FROM timetables WHERE is_active=TRUE AND course_offering_id IS NOT NULL GROUP BY course_offering_id HAVING COUNT(*)>1; SELECT COUNT(*) AS timetable_entry_rows FROM timetable_entries; SELECT COUNT(*) AS attendance_references FROM attendance_sessions WHERE timetable_entry_id IS NOT NULL; SELECT COUNT(*) AS orphan_attendance_references FROM attendance_sessions s LEFT JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id WHERE s.timetable_entry_id IS NOT NULL AND e.timetable_entry_id IS NULL; SELECT COUNT(*) AS active_unassigned_legacy_headers FROM timetables WHERE is_active=TRUE AND course_offering_id IS NULL;"
```

No duplicate active counts and no orphan attendance references are expected. Active unassigned headers may remain: those are ambiguous/generic legacy schedules intentionally not guessed into a course. Compare entry/reference counts with the preflight output.

**6. Grant the existing application account access to the test database only.** This does not change permissions on `university_management`:

```sh
mariadb -u root -p -e "GRANT SELECT, INSERT, UPDATE, DELETE ON university_management_timetable_test.* TO 'ams_app'@'localhost';"
```

**7. Run the application against the test copy without changing `application.properties`.** From the repository root, inline environment variables override the file for this process only. Set `DB_PASSWORD` to the password already configured for `ams_app`:

```sh
DB_URL='jdbc:mariadb://localhost:3306/university_management_timetable_test' DB_USERNAME='ams_app' DB_PASSWORD='AmsLocalOnly-ChangeMe-2026!' ./mvnw exec:java
```

Do not run `database/install.sql` against the restored test copy: it replaces application rows with demo data. Do not point migration commands at `university_management`. No database was contacted and no migration was executed while preparing these instructions.
