package lk.ac.ruhuna.fot.ams.data.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import lk.ac.ruhuna.fot.ams.config.AppConfig;

public final class JdbcConnectionProvider implements ConnectionProvider {
    private final AppConfig config;

    public JdbcConnectionProvider(AppConfig config) {
        this.config = config;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.databaseUrl(),
                config.databaseUsername(),
                config.databasePassword());
    }
}
