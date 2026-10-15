package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalRollbackIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("5.3 - Transaction commit: multiple statements commit atomically when no error occurs")
    void transactionCommitsAllMutationsOnSuccess() throws SQLException {
        TransactionManager tm = new TransactionManager(connectionProvider);

        String testUsername = "TX_COMMIT_USER";
        try {
            Long createdUserId = tm.execute(connection -> {
                try {
                    long userId;
                    try (PreparedStatement insertUser = connection.prepareStatement(
                            "INSERT INTO users (username, password_hash, role, email, is_active, created_at) "
                                    + "VALUES (?, ?, 'ADMIN', ?, TRUE, NOW())",
                            PreparedStatement.RETURN_GENERATED_KEYS)) {
                        insertUser.setString(1, testUsername);
                        insertUser.setString(2, "hash");
                        insertUser.setString(3, "tx_commit@example.edu");
                        insertUser.executeUpdate();
                        try (ResultSet rs = insertUser.getGeneratedKeys()) {
                            rs.next();
                            userId = rs.getLong(1);
                        }
                    }

                    try (PreparedStatement insertAdmin = connection.prepareStatement(
                            "INSERT INTO admins (user_id, full_name) VALUES (?, ?)")) {
                        insertAdmin.setLong(1, userId);
                        insertAdmin.setString(2, "Tx Success Admin");
                        insertAdmin.executeUpdate();
                    }

                    try (PreparedStatement insertAudit = connection.prepareStatement(
                            "INSERT INTO audit_logs (user_id, action, entity_name, entity_id, occurred_at) "
                                    + "VALUES (?, 'CREATE', 'users', ?, NOW())")) {
                        insertAudit.setLong(1, userId);
                        insertAudit.setLong(2, userId);
                        insertAudit.executeUpdate();
                    }

                    return userId;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            // Verify all 3 records were committed
            try (Connection checkConn = connectionProvider.getConnection()) {
                try (PreparedStatement checkUser = checkConn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                    checkUser.setString(1, testUsername);
                    try (ResultSet rs = checkUser.executeQuery()) {
                        rs.next();
                        assertThat(rs.getInt(1)).isEqualTo(1);
                    }
                }
                try (PreparedStatement checkAdmin = checkConn.prepareStatement("SELECT COUNT(*) FROM admins WHERE user_id = ?")) {
                    checkAdmin.setLong(1, createdUserId);
                    try (ResultSet rs = checkAdmin.executeQuery()) {
                        rs.next();
                        assertThat(rs.getInt(1)).isEqualTo(1);
                    }
                }
                try (PreparedStatement checkAudit = checkConn.prepareStatement("SELECT COUNT(*) FROM audit_logs WHERE user_id = ?")) {
                    checkAudit.setLong(1, createdUserId);
                    try (ResultSet rs = checkAudit.executeQuery()) {
                        rs.next();
                        assertThat(rs.getInt(1)).isEqualTo(1);
                    }
                }
            }
        } finally {
            cleanupUser(testUsername);
        }
    }

    @Test
    @DisplayName("5.3 - Transaction rollback: intermediate failure rolls back preceding mutations completely")
    void transactionRollsBackOnIntermediateFailure() {
        TransactionManager tm = new TransactionManager(connectionProvider);
        String testUsername = "TX_ROLLBACK_USER";

        cleanupUser(testUsername);

        assertThatThrownBy(() -> tm.execute(connection -> {
            try {
                // Step 1: Insert user (succeeds)
                long userId;
                try (PreparedStatement insertUser = connection.prepareStatement(
                        "INSERT INTO users (username, password_hash, role, email, is_active, created_at) "
                                + "VALUES (?, ?, 'ADMIN', ?, TRUE, NOW())",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertUser.setString(1, testUsername);
                    insertUser.setString(2, "hash");
                    insertUser.setString(3, "tx_rollback@example.edu");
                    insertUser.executeUpdate();
                    try (ResultSet rs = insertUser.getGeneratedKeys()) {
                        rs.next();
                        userId = rs.getLong(1);
                    }
                }

                // Step 2: Insert admin (succeeds)
                try (PreparedStatement insertAdmin = connection.prepareStatement(
                        "INSERT INTO admins (user_id, full_name) VALUES (?, ?)")) {
                    insertAdmin.setLong(1, userId);
                    insertAdmin.setString(2, "Tx Rollback Admin");
                    insertAdmin.executeUpdate();
                }

                // Step 3: Trigger deliberate failure (e.g., duplicate username or custom runtime exception)
                throw new IllegalStateException("Deliberate failure to trigger rollback");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        })).isInstanceOf(IllegalStateException.class)
           .hasMessage("Deliberate failure to trigger rollback");

        // Verify that NEITHER user nor admin was persisted
        try (Connection checkConn = connectionProvider.getConnection()) {
            try (PreparedStatement checkUser = checkConn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                checkUser.setString(1, testUsername);
                try (ResultSet rs = checkUser.executeQuery()) {
                    rs.next();
                    assertThat(rs.getInt(1)).isZero();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanupUser(String username) {
        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delAudit = conn.prepareStatement(
                    "DELETE FROM audit_logs WHERE user_id IN (SELECT user_id FROM users WHERE username = ?)");
                 PreparedStatement delAdmin = conn.prepareStatement(
                         "DELETE FROM admins WHERE user_id IN (SELECT user_id FROM users WHERE username = ?)");
                 PreparedStatement delUser = conn.prepareStatement(
                         "DELETE FROM users WHERE username = ?")) {
                delAudit.setString(1, username);
                delAudit.executeUpdate();

                delAdmin.setString(1, username);
                delAdmin.executeUpdate();

                delUser.setString(1, username);
                delUser.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
            }
        } catch (Exception ignored) {
        }
    }
}
