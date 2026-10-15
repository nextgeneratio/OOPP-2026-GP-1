package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.data.dao.JdbcUserDao;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersistenceAndConstraintIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("5.2 - Read operation: finds active seeded user via JdbcUserDao")
    void findsActiveSeededUser() {
        JdbcUserDao userDao = new JdbcUserDao(connectionProvider);
        Optional<User> admin = userDao.findActiveByUsername("ADM001");

        assertThat(admin).isPresent();
        assertThat(admin.get().username()).isEqualTo("ADM001");
        assertThat(admin.get().role()).isEqualTo(Role.ADMIN);
        assertThat(admin.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("5.2 - Unique constraint: duplicate username insertion fails with constraint violation")
    void duplicateUsernameThrowsConstraintViolation() {
        String insertSql = "INSERT INTO users (username, password_hash, role, email, is_active, created_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, "ADM001"); // already exists in seed.sql
                stmt.setString(2, "some-hash");
                stmt.setString(3, "ADMIN");
                stmt.setString(4, "duplicate_adm@example.edu");
                stmt.setBoolean(5, true);

                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("5.2 - Foreign key constraint: inserting child record with nonexistent parent fails")
    void foreignKeyViolationOnOrphanRecord() {
        String insertSql = "INSERT INTO lecturers (user_id, department_id) VALUES (?, ?)";

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setLong(1, 99999999L); // non-existent user_id
                stmt.setInt(2, 1);

                assertThatThrownBy(stmt::executeUpdate)
                        .isInstanceOf(SQLException.class);
            } finally {
                conn.rollback();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("5.2 - Soft delete: deactivated user is not returned by findActiveByUsername")
    void softDeleteUserDeactivation() throws SQLException {
        JdbcUserDao userDao = new JdbcUserDao(connectionProvider);

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert a test user
                long testUserId;
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, role, email, is_active, created_at) VALUES (?, ?, ?, ?, ?, NOW())",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, "TEST_DEACTIVATE");
                    insert.setString(2, "test-hash");
                    insert.setString(3, "ADMIN");
                    insert.setString(4, "test_deactivate@example.edu");
                    insert.setBoolean(5, true);
                    insert.executeUpdate();
                    try (ResultSet rs = insert.getGeneratedKeys()) {
                        rs.next();
                        testUserId = rs.getLong(1);
                    }
                }
                try (PreparedStatement insertAdmin = conn.prepareStatement(
                        "INSERT INTO admins (user_id, full_name) VALUES (?, ?)")) {
                    insertAdmin.setLong(1, testUserId);
                    insertAdmin.setString(2, "Test Admin");
                    insertAdmin.executeUpdate();
                }
                conn.commit();

                // Verify user can be found while active
                assertThat(userDao.findActiveByUsername("TEST_DEACTIVATE")).isPresent();

                // Soft delete / deactivate user
                try (PreparedStatement deactivate = conn.prepareStatement(
                        "UPDATE users SET is_active = FALSE WHERE user_id = ?")) {
                    deactivate.setLong(1, testUserId);
                    deactivate.executeUpdate();
                }
                conn.commit();

                // Verify user is no longer returned as active
                assertThat(userDao.findActiveByUsername("TEST_DEACTIVATE")).isEmpty();
            } finally {
                try (PreparedStatement cleanAdmin = conn.prepareStatement("DELETE FROM admins WHERE user_id IN (SELECT user_id FROM users WHERE username = 'TEST_DEACTIVATE')");
                     PreparedStatement cleanUser = conn.prepareStatement("DELETE FROM users WHERE username = 'TEST_DEACTIVATE'")) {
                    cleanAdmin.executeUpdate();
                    cleanUser.executeUpdate();
                    conn.commit();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
