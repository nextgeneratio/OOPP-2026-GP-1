package lk.ac.ruhuna.fot.ams.data.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;

public final class TransactionManager {
    @FunctionalInterface
    public interface TransactionWork<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    public TransactionManager(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public <T> T execute(TransactionWork<T> work) {
        try (Connection connection = connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.apply(connection);
                connection.commit();
                return result;
            } catch (RuntimeException | Error failure) {
                rollback(connection, failure);
                throw failure;
            } catch (SQLException failure) {
                rollback(connection, failure);
                throw new DataAccessException("The database transaction could not be completed.", failure);
            }
        } catch (SQLException failure) {
            throw new DataAccessException("The database transaction could not be started.", failure);
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
