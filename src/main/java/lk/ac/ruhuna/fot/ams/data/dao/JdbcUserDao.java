package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.Lecturer;
import lk.ac.ruhuna.fot.ams.domain.model.TechnicalOfficer;
import lk.ac.ruhuna.fot.ams.domain.model.Undergraduate;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;

public final class JdbcUserDao implements UserDao {
    private final ConnectionProvider connectionProvider;
    private final JdbcExecutor jdbc;

    public JdbcUserDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
        this.jdbc = new JdbcExecutor(connectionProvider);
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

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = userSelect() + "WHERE u.username = ?";
        return jdbc.query(sql, statement -> statement.setString(1, username), this::map)
                .stream().findFirst();
    }

    @Override
    public Optional<User> findById(long userId) {
        String sql = userSelect() + "WHERE u.user_id = ?";
        return jdbc.query(sql, statement -> statement.setLong(1, userId), this::map)
                .stream().findFirst();
    }

    @Override
    public List<UserProfile> searchUsers(String term, Role role, Boolean active) {
        String sql = "SELECT u.user_id, u.username, u.password_hash, u.role, u.email, u.is_active, "
                + "COALESCE(a.full_name, l.full_name, t.full_name, s.full_name) AS full_name, "
                + "COALESCE(l.phone, t.phone, a.phone) AS phone, "
                + "COALESCE(l.department_id, t.department_id) AS department_id, "
                + "s.batch_id, s.student_no, s.profile_picture "
                + "FROM users u LEFT JOIN admins a ON a.user_id = u.user_id "
                + "LEFT JOIN lecturers l ON l.user_id = u.user_id "
                + "LEFT JOIN technical_officers t ON t.user_id = u.user_id "
                + "LEFT JOIN undergraduates s ON s.user_id = u.user_id "
                + "WHERE (? IS NULL OR u.username LIKE ? OR u.email LIKE ? "
                + "OR COALESCE(a.full_name, l.full_name, t.full_name, s.full_name) LIKE ?) "
                + "AND (? IS NULL OR u.role = ?) AND (? IS NULL OR u.is_active = ?) "
                + "ORDER BY u.username";
        return jdbc.query(sql, statement -> {
            String pattern = term == null || term.isBlank() ? null : "%" + term.trim() + "%";
            if (pattern == null) {
                statement.setNull(1, java.sql.Types.VARCHAR);
                statement.setNull(2, java.sql.Types.VARCHAR);
                statement.setNull(3, java.sql.Types.VARCHAR);
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(1, pattern);
                statement.setString(2, pattern);
                statement.setString(3, pattern);
                statement.setString(4, pattern);
            }
            if (role == null) {
                statement.setNull(5, java.sql.Types.VARCHAR);
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(5, role.name());
                statement.setString(6, role.name());
            }
            if (active == null) {
                statement.setNull(7, java.sql.Types.BOOLEAN);
                statement.setNull(8, java.sql.Types.BOOLEAN);
            } else {
                statement.setBoolean(7, active);
                statement.setBoolean(8, active);
            }
        }, this::mapProfile);
    }

    @Override
    public long create(Connection connection, UserProfile profile) {
        long userId = jdbc.insertAndReturnKey(connection,
                "INSERT INTO users (username, password_hash, role, email, is_active, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                statement -> {
                    statement.setString(1, profile.username());
                    statement.setString(2, profile.passwordHash());
                    statement.setString(3, profile.role().name());
                    statement.setString(4, profile.email());
                    statement.setBoolean(5, profile.active());
                });
        String table = switch (profile.role()) {
            case ADMIN -> "INSERT INTO admins (user_id, full_name, phone) VALUES (?, ?, ?)";
            case LECTURER -> "INSERT INTO lecturers "
                    + "(user_id, department_id, full_name, phone) VALUES (?, ?, ?, ?)";
            case TECHNICAL_OFFICER -> "INSERT INTO technical_officers "
                    + "(user_id, department_id, full_name, phone) VALUES (?, ?, ?, ?)";
            case UNDERGRADUATE -> "INSERT INTO undergraduates "
                    + "(user_id, batch_id, student_no, full_name, profile_picture) VALUES (?, ?, ?, ?, ?)";
        };
        jdbc.update(connection, table, statement -> bindProfile(statement, userId, profile));
        return userId;
    }

    @Override
    public int update(Connection connection, UserProfile profile) {
        if (!userExists(connection, profile.id())) {
            return 0;
        }
        if (!roleProfileExists(connection, profile.id(), profile.role())) {
            throw new DataAccessException("User role profile is missing.", new SQLException("Role profile missing."));
        }
        jdbc.update(connection, "UPDATE users SET email = ?, is_active = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, profile.email());
                    statement.setBoolean(2, profile.active());
                    statement.setLong(3, profile.id());
                });
        String sql = switch (profile.role()) {
            case ADMIN -> "UPDATE admins SET full_name = ?, phone = ? WHERE user_id = ?";
            case LECTURER -> "UPDATE lecturers SET department_id = ?, full_name = ?, phone = ? WHERE user_id = ?";
            case TECHNICAL_OFFICER -> "UPDATE technical_officers "
                    + "SET department_id = ?, full_name = ?, phone = ? WHERE user_id = ?";
            case UNDERGRADUATE -> "UPDATE undergraduates "
                    + "SET batch_id = ?, student_no = ?, full_name = ? WHERE user_id = ?";
        };
        jdbc.update(connection, sql, statement -> bindProfileUpdate(statement, profile));
        return 1;
    }

    @Override
    public int deactivate(Connection connection, long userId) {
        return jdbc.update(connection, "UPDATE users SET is_active = FALSE WHERE user_id = ? AND is_active = TRUE",
                statement -> statement.setLong(1, userId));
    }

    private boolean userExists(Connection connection, long userId) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id FROM users WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to update user profile.", failure);
        }
    }

    private boolean roleProfileExists(Connection connection, long userId, Role role) {
        String table = switch (role) {
            case ADMIN -> "admins";
            case LECTURER -> "lecturers";
            case TECHNICAL_OFFICER -> "technical_officers";
            case UNDERGRADUATE -> "undergraduates";
        };
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT user_id FROM " + table + " WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException failure) {
            throw new DataAccessException("Unable to update user profile.", failure);
        }
    }

    private void bindProfile(PreparedStatement statement, long userId, UserProfile profile) throws SQLException {
        switch (profile.role()) {
            case ADMIN -> {
                statement.setLong(1, userId);
                statement.setString(2, profile.fullName());
                statement.setString(3, profile.phone());
            }
            case LECTURER, TECHNICAL_OFFICER -> {
                statement.setLong(1, userId);
                statement.setInt(2, profile.departmentId());
                statement.setString(3, profile.fullName());
                statement.setString(4, profile.phone());
            }
            case UNDERGRADUATE -> {
                statement.setLong(1, userId);
                statement.setInt(2, profile.batchId());
                statement.setString(3, profile.studentNumber());
                statement.setString(4, profile.fullName());
                statement.setString(5, profile.profilePicture());
            }
        }
    }

    private void bindProfileUpdate(PreparedStatement statement, UserProfile profile) throws SQLException {
        switch (profile.role()) {
            case ADMIN -> {
                statement.setString(1, profile.fullName());
                statement.setString(2, profile.phone());
                statement.setLong(3, profile.id());
            }
            case LECTURER, TECHNICAL_OFFICER -> {
                statement.setInt(1, profile.departmentId());
                statement.setString(2, profile.fullName());
                statement.setString(3, profile.phone());
                statement.setLong(4, profile.id());
            }
            case UNDERGRADUATE -> {
                statement.setInt(1, profile.batchId());
                statement.setString(2, profile.studentNumber());
                statement.setString(3, profile.fullName());
                statement.setLong(4, profile.id());
            }
        }
    }

    @Override
    public int updateStaffContact(
            Connection connection, long userId, Role role, String email, String fullName, String phone) {
        if (role != Role.LECTURER && role != Role.TECHNICAL_OFFICER) {
            throw new IllegalArgumentException("Staff profile updates require a staff role.");
        }
        if (!userExists(connection, userId) || !roleProfileExists(connection, userId, role)) {
            return 0;
        }
        jdbc.update(connection, "UPDATE users SET email = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, email);
                    statement.setLong(2, userId);
                });
        String table = role == Role.LECTURER ? "lecturers" : "technical_officers";
        jdbc.update(connection, "UPDATE " + table + " SET full_name = ?, phone = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, fullName);
                    statement.setString(2, phone);
                    statement.setLong(3, userId);
                });
        return 1;
    }

    @Override
    public int updateUndergraduateContact(
            Connection connection, long userId, String email, String profilePicture) {
        if (!userExists(connection, userId) || !roleProfileExists(connection, userId, Role.UNDERGRADUATE)) {
            return 0;
        }
        jdbc.update(connection, "UPDATE users SET email = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, email);
                    statement.setLong(2, userId);
                });
        jdbc.update(connection, "UPDATE undergraduates SET profile_picture = ? WHERE user_id = ?",
                statement -> {
                    statement.setString(1, profilePicture);
                    statement.setLong(2, userId);
                });
        return 1;
    }

    private UserProfile mapProfile(ResultSet result) throws SQLException {
        int department = result.getInt("department_id");
        Integer departmentId = result.wasNull() ? null : department;
        int batch = result.getInt("batch_id");
        Integer batchId = result.wasNull() ? null : batch;
        return new UserProfile(result.getLong("user_id"), result.getString("username"),
                result.getString("password_hash"), Role.valueOf(result.getString("role")),
                result.getString("email"), result.getBoolean("is_active"),
                result.getString("full_name"), result.getString("phone"), departmentId,
                batchId, result.getString("student_no"), result.getString("profile_picture"));
    }

    private String userSelect() {
        return "SELECT u.user_id, u.username, u.password_hash, u.role, u.email, u.is_active, "
                + "a.admin_id, l.lecturer_id, l.department_id AS lecturer_department_id, "
                + "t.officer_id, t.department_id AS officer_department_id, "
                + "s.student_id, s.batch_id, s.student_no "
                + "FROM users u LEFT JOIN admins a ON a.user_id = u.user_id "
                + "LEFT JOIN lecturers l ON l.user_id = u.user_id "
                + "LEFT JOIN technical_officers t ON t.user_id = u.user_id "
                + "LEFT JOIN undergraduates s ON s.user_id = u.user_id ";
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
