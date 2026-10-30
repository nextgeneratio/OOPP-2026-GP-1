# Testing Strategy

The project uses unit, integration, and system test sources under the Maven layout.
JUnit 5, Mockito, and AssertJ provide the automated test foundation; UI automation
coverage remains pending.

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
