# Testing Strategy

The project uses unit, integration, and system test sources under the Maven layout.
JUnit 5, Mockito, and AssertJ provide the automated test foundation. Swing panel
construction/navigation checks are automated; live GUI acceptance remains a
separate requirement and must not be inferred from panel tests.

## Latest verified environment — 2026-10-30

- Build toolchain: OpenJDK 25.0.4.1 and Maven 3.9.11.
- Latest `mvn clean verify`: 90 tests discovered, 89 passed, 0 failures, 0 errors,
  and 1 skipped. The skipped case is `DatabaseConnectivityTest` because test
  database credentials are not configured.
- The configured application database was inspected read-only as
  `university_management` on MariaDB 11.8.8. No integration writes were run.
  The dedicated `university_management_test` is not configured for tests.
- One policy-hardening SQL migration is proposed, not applied: adding a
  Technical Officer-only check to `attendance_sessions`. Its read-only preflight
  and exact SQL are in `system-test-results.md`.
- Live database-backed controller smoke checks were read-only. They verified
  profile and offering reads for all four roles, attendance setup reads for all
  four roles, and assessment plan, marks sheet, and attendance session reads for
  a Lecturer. This is not a login, write-persistence, or GUI acceptance test.
- Separate `mvn clean test`: 90 tests, 0 failures, 0 errors, 1 skipped; build
  succeeded. The Surefire report identifies the same skipped connectivity test.

## Requirement-to-test coverage map

The full screen/controller/service/DAO/table/test mapping is in
[requirement-traceability.md](./requirement-traceability.md). Rows marked
blocked need the isolated database or interactive GUI before they can be
accepted; a matching service unit test alone does not establish persistence.

## Isolated MySQL integration database

The connectivity test targets `university_management_test`. Configure either
`DB_USERNAME` and `DB_PASSWORD` in the test process environment or put those two
properties in the ignored repository-root `test-credentials.properties` file.
The local credential file must not be committed. An optional `DB_URL` override
must select a database whose name ends in `_test`; the test checks the target
before opening a connection and refuses other database names.

Create the test database using `src/test/resources/db/test-schema.sql`, then apply
`src/main/resources/db/schema.sql` and `src/main/resources/db/seed.sql` with the
test database selected as the client's default database. The shared schema and
seed files do not select a database, preventing them from silently switching
from the test database to the development database.

Example with a local MariaDB/MySQL option file that contains credentials:

```sh
mariadb --defaults-extra-file="$HOME/.my.cnf" < src/test/resources/db/test-schema.sql
mariadb --defaults-extra-file="$HOME/.my.cnf" --database=university_management_test < src/main/resources/db/schema.sql
mariadb --defaults-extra-file="$HOME/.my.cnf" --database=university_management_test < src/main/resources/db/seed.sql
```

Only run the seed against the dedicated test database: it truncates academic and
user tables before inserting predictable demonstration data. Verify the selected
database before applying it.
