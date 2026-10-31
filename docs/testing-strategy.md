# Testing Strategy

## 2026-10-31 resource visibility update

- `MaterialFileStoreTest` and `ContentMaterialVisibilityTest` use temporary
  directories and synthetic files. They cover readable files, missing files,
  directories, malformed/outside references, symlink escape rejection,
  student offering authorization, unpublished rows, and rechecking a file
  deleted after list loading.
- `mvn clean test` and `mvn clean verify`: 105 tests discovered, 104 passed,
  0 failures, 0 errors, 1 skipped (`DatabaseConnectivityTest`; isolated DB
  credentials are not configured).
- Real DAO/database integration and interactive Swing acceptance remain
  unverified. No production database writes were performed.
- Generated 45 synthetic PDFs (five for each of the nine source courses).
  They are under `src/main/resources/uploads/materials/demo-readings/`; the
  manifest is `docs/generated-course-materials-manifest.csv`. The manual SQL
  registers them to the synthetic course offerings; no database import ran.
- `manual-demo-replacement.sql` passed static schema/path/hash/FK-order checks.
  It has not been executed against any database.

The project uses unit, integration, and system test sources under the Maven layout.
JUnit 5, Mockito, and AssertJ provide the automated test foundation. Swing panel
construction/navigation checks are automated; live GUI acceptance remains a
separate requirement and must not be inferred from panel tests.

## Latest verified environment — 2026-10-30

- Build toolchain: OpenJDK 25.0.4.1 and Maven 3.9.11.
- Latest `mvn clean verify`: 94 tests discovered, 93 passed, 0 failures, 0 errors,
  and 1 skipped. `DatabaseConnectivityTest` is skipped because test database
  credentials are not configured. `mvn clean verify` also passed after the
  follow-up changes with the same counts.
- The configured application database was inspected read-only as
  `university_management` on MariaDB 11.8.8. Its attendance CHECK constraint
  is present. No integration writes were run. A read-only catalog check found
  that `university_management_test` does not exist.
- The Technical Officer-only attendance recorder CHECK constraint is now
  present in the application database. It was detected read-only; no DDL/DML
  was executed and no migration is currently needed.
- Live database-backed controller smoke checks were read-only. They verified
  profile and offering reads for all four roles, attendance setup reads for all
  four roles, and assessment plan, marks sheet, and attendance session reads for
  a Lecturer. This is not a login, write-persistence, or GUI acceptance test.
- Test type boundary: the suite has unit, mocked service/controller, and Swing
  construction/navigation coverage plus one skipped connectivity test. There
  are no real DAO integration workflows in the suite yet.

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

The catalog check during the latest audit found the dedicated database does not
exist. With a database administrator account, create and prepare it explicitly:

```sql
CREATE DATABASE `university_management_test`;
```

Then apply the checked-in scripts only with the `_test` catalog selected:

```sh
mariadb --database=university_management_test < src/main/resources/db/schema.sql
mariadb --database=university_management_test < src/main/resources/db/seed.sql
```

Set `DB_USERNAME` and `DB_PASSWORD` to a test-only database user, and keep
`DB_URL` at its default `jdbc:mysql://localhost:3306/university_management_test`
or another URL whose database ends in `_test`. Verify the JDBC catalog matches
the URL before adding persistence tests. `seed.sql` truncates tables, so never
run it against `university_management`.

Example with a local MariaDB/MySQL option file that contains credentials:

```sh
mariadb --defaults-extra-file="$HOME/.my.cnf" < src/test/resources/db/test-schema.sql
mariadb --defaults-extra-file="$HOME/.my.cnf" --database=university_management_test < src/main/resources/db/schema.sql
mariadb --defaults-extra-file="$HOME/.my.cnf" --database=university_management_test < src/main/resources/db/seed.sql
```

Only run the seed against the dedicated test database: it truncates academic and
user tables before inserting predictable demonstration data. Verify the selected
database before applying it.
