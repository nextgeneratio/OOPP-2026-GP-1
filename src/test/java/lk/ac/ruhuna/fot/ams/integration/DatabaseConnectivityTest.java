package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.config.AppConfig;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectivityTest {
    @Test
    void connectsToConfiguredMysqlDatabase() throws Exception {
        Assumptions.assumeTrue((isConfigured("DB_USERNAME") && isConfigured("DB_PASSWORD"))
                        || hasLocalCredentials(),
                "Test database credentials are not configured");
        AppConfig config = AppConfig.loadFromClasspath("test-application.properties");
        assertThat(databaseName(config.databaseUrl()))
                .as("connectivity checks must never target a development/live database")
                .endsWith("_test");
        try (Connection connection = new JdbcConnectionProvider(config).getConnection()) {
            assertThat(connection.isValid(3)).isTrue();
            assertThat(connection.getCatalog()).isEqualTo(databaseName(config.databaseUrl()));
        }
    }

    private String databaseName(String jdbcUrl) {
        String database = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int queryStart = database.indexOf('?');
        return queryStart < 0 ? database : database.substring(0, queryStart);
    }

    private boolean isConfigured(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    private boolean hasLocalCredentials() throws Exception {
        Path credentialsFile = Path.of("test-credentials.properties");
        if (!Files.isRegularFile(credentialsFile)) {
            return false;
        }
        Properties credentials = new Properties();
        try (var input = Files.newInputStream(credentialsFile)) {
            credentials.load(input);
        }
        return !credentials.getProperty("db.username", "").isBlank()
                && !credentials.getProperty("db.password", "").isBlank();
    }
}
