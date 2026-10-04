package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcAuditLogDao implements AuditLogDao {
    private final JdbcExecutor jdbc;

    public JdbcAuditLogDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int insert(Connection connection, AuditEvent event) {
        return jdbc.update(connection,
                "INSERT INTO audit_logs (user_id, action, entity_name, entity_id, occurred_at) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    if (event.userId() == null) {
                        statement.setNull(1, java.sql.Types.BIGINT);
                    } else {
                        statement.setLong(1, event.userId());
                    }
                    statement.setString(2, event.action());
                    statement.setString(3, event.entityName());
                    statement.setLong(4, event.entityId());
                    statement.setTimestamp(5, Timestamp.valueOf(event.occurredAt()));
                });
    }

    @Override
    public List<AuditEvent> findForEntity(String entityName, long entityId) {
        return jdbc.query("SELECT audit_id, user_id, action, entity_name, entity_id, occurred_at "
                        + "FROM audit_logs WHERE entity_name = ? AND entity_id = ? ORDER BY occurred_at DESC",
                statement -> {
                    statement.setString(1, entityName);
                    statement.setLong(2, entityId);
                }, this::map);
    }

    @Override
    public List<AuditEvent> findForUser(long userId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return jdbc.query("SELECT audit_id, user_id, action, entity_name, entity_id, occurred_at "
                        + "FROM audit_logs WHERE user_id = ? AND occurred_at >= ? AND occurred_at < ? "
                        + "ORDER BY occurred_at DESC",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setTimestamp(2, Timestamp.valueOf(from));
                    statement.setTimestamp(3, Timestamp.valueOf(to));
                }, this::map);
    }

    private AuditEvent map(ResultSet result) throws SQLException {
        long user = result.getLong("user_id");
        Long userId = result.wasNull() ? null : user;
        return new AuditEvent(result.getLong("audit_id"), userId,
                result.getString("action"), result.getString("entity_name"),
                result.getLong("entity_id"), result.getTimestamp("occurred_at").toLocalDateTime());
    }
}
