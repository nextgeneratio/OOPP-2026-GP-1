# Database design

The final schema and local demonstration records are installed together by
[`../database/install.sql`](../database/install.sql). That file is the only
supported user-facing SQL setup. It defines all 30 InnoDB tables, keys,
constraints, indexes, defaults, and latest workflow fields, including attendance
recorder rules, medical review history, timetable ownership, learning
activities, and course results.

The installer uses `CREATE TABLE IF NOT EXISTS`; it does not alter or recreate
existing tables. Before deleting data it checks the selected database, table
set, engines/collations, all required columns, index count, and constraint
count. A schema mismatch stops the replacement before the data transaction.
The data operation deletes application rows in child-first order and inserts
the demonstration set inside one InnoDB transaction. Foreign-key checks stay
enabled. DDL executed before that transaction is not rolled back on a later
error.

The separate JDBC account is `ams_app@localhost` and has only `SELECT`,
`INSERT`, `UPDATE`, and `DELETE` privileges on `university_management`. The
installer resets its local demonstration password as documented in the README.
The application itself uses JDBC through MariaDB Connector/J and obtains its
connection settings from the ignored repository-root `application.properties`.

The database stores material references as relative paths under `uploads/`.
Maven/IDE execution from the repository root resolves those files under
`src/main/resources/uploads`; packaged-JAR execution is not a supported runtime
mode.

## Weekly timetable slots

Timetable headers and entries now have `is_active` flags. A generic weekly slot has both `offering_id` and `component_id` NULL; legacy course-specific slots keep both populated. `chk_timetable_entries_optional_course_pair` enforces the paired-null rule. Location is optional. Updating a weekly schedule deactivates its old slot rows and inserts new rows, preserving the stable IDs referenced by attendance sessions. Deletion deactivates both the header and active slots rather than physically deleting history.

Fresh installs receive the final model from `database/install.sql`. Existing installations require the reviewed manual upgrade `database/migrations/20261105_weekly_timetable.sql`. It changes only schema, retains existing rows and foreign keys, and reports row/reference counts before and after. Do not apply it to the populated application database without the operator's backup/review step.
