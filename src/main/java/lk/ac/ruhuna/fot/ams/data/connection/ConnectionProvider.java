package lk.ac.ruhuna.fot.ams.data.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
