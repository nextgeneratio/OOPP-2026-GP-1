package lk.ac.ruhuna.fot.ams.data.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;

public final class JdbcExecutor {
    @FunctionalInterface
    public interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet result) throws SQLException;
    }

    private final ConnectionProvider connectionProvider;

    public JdbcExecutor(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
    }

    public <T> List<T> query(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = connectionProvider.getConnection()) {
            return query(connection, sql, binder, mapper);
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to read application data.", failure);
        }
    }

    public <T> List<T> query(
            Connection connection,
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(binder);
        Objects.requireNonNull(mapper);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (result.next()) {
                    rows.add(mapper.map(result));
                }
                return List.copyOf(rows);
            }
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to read application data.", failure);
        }
    }

    public int update(String sql, StatementBinder binder) {
        try (Connection connection = connectionProvider.getConnection()) {
            return update(connection, sql, binder);
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to update application data.", failure);
        }
    }

    public int update(Connection connection, String sql, StatementBinder binder) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(binder);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to update application data.", failure);
        }
    }

    public long insertAndReturnKey(Connection connection, String sql, StatementBinder binder) {
        Objects.requireNonNull(connection);
        Objects.requireNonNull(binder);
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            if (statement.executeUpdate() != 1) {
                throw new DataAccessException("The database did not create the requested record.",
                        new SQLException("Insert did not affect exactly one row."));
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("The database did not return the created record identifier.",
                            new SQLException("Insert returned no generated key."));
                }
                return keys.getLong(1);
            }
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to create application data.", failure);
        }
    }
}
