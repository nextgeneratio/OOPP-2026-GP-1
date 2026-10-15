package lk.ac.ruhuna.fot.ams.audit;

import java.time.LocalDateTime;

public record AuditLog(
        Long auditId,
        Long userId,
        String action,
        String entityName,
        Long entityId,
        LocalDateTime occurredAt
) {
    public AuditLog(Long userId, String action, String entityName, Long entityId) {
        this(null, userId, action, entityName, entityId, LocalDateTime.now());
    }
}
