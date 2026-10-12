package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.config.AppConfig;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConnectivityTest {
    @Test
    void connectsToConfiguredMysqlDatabase() {
        AppConfig config;
        try {
            config = AppConfig.load();
            if (config.databaseUrl() == null || config.databaseUrl().isBlank()) {
                Assumptions.assumeTrue(false, "Database configuration is unavailable");
                return;
            }
        } catch (Exception unavailable) {
            Assumptions.assumeTrue(false, "Database configuration is unavailable: " + unavailable.getMessage());
            return;
        }
        try (Connection connection = new JdbcConnectionProvider(config).getConnection()) {
            assertThat(connection.isValid(3)).isTrue();
        } catch (Exception unavailable) {
            Assumptions.assumeTrue(false, "Database connection is unavailable: " + unavailable.getMessage());
        }
    }
}
