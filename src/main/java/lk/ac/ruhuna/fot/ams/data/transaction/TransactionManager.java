package lk.ac.ruhuna.fot.ams.data.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;

public final class TransactionManager {
    private final ConnectionProvider connectionProvider;

    public TransactionManager(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public <T> T execute(Function<Connection, T> work) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                connection.setAutoCommit(originalAutoCommit);
                return result;
            } catch (RuntimeException | Error failure) {
                rollback(connection, failure);
                throw failure;
            } catch (SQLException failure) {
                rollback(connection, failure);
                throw failure;
            }
        }
    }

    private void rollback(Connection connection, Throwable failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
