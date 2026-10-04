package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogDao {
    record AuditEvent(
            long id, Long userId, String action, String entityName, long entityId, LocalDateTime occurredAt) {
    }

    int insert(Connection connection, AuditEvent event);

    List<AuditEvent> findForEntity(String entityName, long entityId);

    List<AuditEvent> findForUser(long userId, LocalDateTime from, LocalDateTime to);
}
