package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.config.AppConfig;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class BaseIntegrationTest {
    protected static AppConfig config;
    protected static ConnectionProvider connectionProvider;

    @BeforeAll
    static void setUpDatabase() {
        try {
            config = AppConfig.load();
            if (config.databaseUrl() == null || config.databaseUrl().isBlank()) {
                Assumptions.assumeTrue(false, "Database configuration is not set");
                return;
            }
            connectionProvider = new JdbcConnectionProvider(config);
            try (Connection conn = connectionProvider.getConnection()) {
                Assumptions.assumeTrue(conn.isValid(3), "Database is not reachable");
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Database connection unavailable: " + e.getMessage());
        }
    }
}
