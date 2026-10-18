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
repository-root `test-credentials.properties`, before running it. To point that
test explicitly at a database, set `DB_URL`.

## Database setup

`scripts/create_database.sql` creates the development database and requires
appropriate database-creation privileges. Select `university_management`
explicitly when applying `src/main/resources/db/schema.sql`.

The seed script truncates existing academic/user tables before inserting its
development sample data. Only run it against a database you intend to reset.
For isolated test database instructions, see [docs/testing-strategy.md](./docs/testing-strategy.md).

## Current UI scope

The login, shared application shell, and four role dashboards are present. Most
module navigation destinations remain placeholders; see [progress.md](./progress.md)
and [docs/system-test-results.md](./docs/system-test-results.md) for current
implementation and test status.
