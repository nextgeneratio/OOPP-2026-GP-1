# Testing

Run automated checks from the repository root with JDK 25:

```sh
./mvnw clean test
./mvnw clean verify
./mvnw clean package
```

Unit tests include authentication, password hashing, role authorization,
academic rules, installation data, resource references, and Swing construction.
`DemoInstallerTest` reads the checked-in `database/install.sql`, verifies all
30 seeded password hashes using the production `PasswordHasher` and authenticates
each account through `AuthService`.

The optional `DatabaseConnectivityTest` is separate. It reads
`src/test/resources/test-application.properties` and may connect only to a
database whose name ends in `_test`. Set `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`, or set the username and password in the ignored repository-root
`test-credentials.properties`. Never use `database/install.sql` against a
populated database as a test; the installer is a destructive data replacement.

Interactive GUI login/role acceptance requires a graphical desktop and a
separately prepared database. Component construction tests do not constitute
visual acceptance.
