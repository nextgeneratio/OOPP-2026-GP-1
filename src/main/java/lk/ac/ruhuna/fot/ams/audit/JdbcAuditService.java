package lk.ac.ruhuna.fot.ams.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import javax.sql.DataSource;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcAuditService implements AuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcAuditService.class);
    private final DataSource dataSource;

    public JdbcAuditService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void log(AuditLog log) {
        if (log == null) {
            return;
        }

        String sql = "INSERT INTO audit_logs (user_id, action, entity_name, entity_id, occurred_at) "
                   + "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (log.userId() != null) {
                statement.setLong(1, log.userId());
            } else {
                statement.setNull(1, Types.BIGINT);
            }

            statement.setString(2, log.action());
            statement.setString(3, log.entityName());
            statement.setLong(4, log.entityId());
            statement.setTimestamp(5, Timestamp.valueOf(log.occurredAt()));

            statement.executeUpdate();
        } catch (SQLException failure) {
            LOGGER.error("Failed to write audit log: {}", log, failure);
            throw new DataAccessException("Unable to persist audit log entry.", failure);
        }
    }
}
