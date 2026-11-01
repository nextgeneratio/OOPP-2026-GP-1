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
