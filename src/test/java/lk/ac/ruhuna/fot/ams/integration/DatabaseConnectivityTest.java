package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.config.AppConfig;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectivityTest {
    @Test
    void connectsToConfiguredMysqlDatabase() throws Exception {
        AppConfig config;
        try {
            config = AppConfig.loadFromClasspath("test-application.properties");
        } catch (RuntimeException | java.io.IOException unavailable) {
            Assumptions.assumeTrue(false, "Database configuration is unavailable");
            return;
        }
        try (Connection connection = new JdbcConnectionProvider(config).getConnection()) {
            assertThat(connection.isValid(3)).isTrue();
        }
    }
}
