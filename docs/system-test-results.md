# Release verification

## Database installer

`database/install.sql` is the sole supported installation file. It contains the
complete canonical 30-table InnoDB schema, local JDBC account creation, and a
transactional destructive replacement of application rows with the synthetic
demo data. Its preflight checks the selected catalog and existing schema before
any row deletion. It uses child-first `DELETE FROM` statements and does not drop
tables/databases or truncate data. Back up an existing database before running
it.

An isolated MariaDB 11.8.8 instance on a disposable datadir was used to execute
a test-renamed copy targeting `university_management_test`. The install and
repeat-install succeeded. The resulting dataset had 30 active users (1 admin,
5 lecturers, 4 technical officers, 20 undergraduates), 30 tables, 45 visible
materials, and no invalid assessment totals, marks, attendance values,
material links, enrollment links, marks links, attendance links, timetable
dates, recorder policy violations, duplicate natural identities, or missing
offering/lecturer assignments. Deliberately incompatible `_test` schemas with a missing column and a changed
column width were rejected before deletion; sentinel rows remained. The `DatabaseConnectivityTest`
passed against the isolated database. That temporary server and data directory
were shut down and removed. The configured `university_management` database
was not connected to or modified.

## Automated checks

The release-specific `DemoInstallerTest` verifies every seeded hash using the
production `PasswordHasher`, confirms unique salts and all expected roles,
authenticates all 30 users through `AuthService` with `1234`, checks that the
ordinary eight-character creation policy remains unchanged, and validates all
45 manifest PDF files and SQL references.

The final clean test, verify, and package commands passed with 110 tests
discovered, 109 passed, 0 failures/errors, and 1 optional DB connectivity test
skipped when no test DB is configured. The isolated connectivity run separately
passed against the disposable `_test` database. Unit and database checks are
distinct from interactive GUI acceptance; no screen layout or four-role desktop
login is claimed without a GUI run.
