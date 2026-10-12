package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.Lecturer;
import lk.ac.ruhuna.fot.ams.domain.model.TechnicalOfficer;
import lk.ac.ruhuna.fot.ams.domain.model.Undergraduate;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;

public final class JdbcUserDao implements UserDao {
    private final ConnectionProvider connectionProvider;

    public JdbcUserDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Optional<User> findActiveByUsername(String username) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.role, u.email, u.is_active, "
                + "a.admin_id, l.lecturer_id, l.department_id AS lecturer_department_id, "
                + "t.officer_id, t.department_id AS officer_department_id, "
                + "s.student_id, s.batch_id, s.student_no "
                + "FROM users u "
                + "LEFT JOIN admins a ON a.user_id = u.user_id "
                + "LEFT JOIN lecturers l ON l.user_id = u.user_id "
                + "LEFT JOIN technical_officers t ON t.user_id = u.user_id "
                + "LEFT JOIN undergraduates s ON s.user_id = u.user_id "
                + "WHERE u.username = ? AND u.is_active = TRUE";
        try (Connection connection = connectionProvider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to authenticate user.", failure);
        }
    }

    private User map(ResultSet result) throws SQLException {
        long userId = result.getLong("user_id");
        String username = result.getString("username");
        String hash = result.getString("password_hash");
        String email = result.getString("email");
        boolean active = result.getBoolean("is_active");
        Role role = Role.valueOf(result.getString("role"));
        return switch (role) {
            case ADMIN -> new Admin(userId, username, hash, email, active);
            case LECTURER -> new Lecturer(userId, username, hash, email, active,
                    result.getInt("lecturer_department_id"));
            case TECHNICAL_OFFICER -> new TechnicalOfficer(userId, username, hash, email, active,
                    result.getInt("officer_department_id"));
            case UNDERGRADUATE -> new Undergraduate(userId, username, hash, email, active,
                    result.getInt("student_id"), result.getInt("batch_id"), result.getString("student_no"));
        };
    }
}
