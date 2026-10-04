package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;

public final class AuditService implements AuthenticationAudit {
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactionManager;
    private final Clock clock;

    public AuditService(
            AuditLogDao auditLogDao,
            TransactionManager transactionManager,
            Clock clock) {
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.clock = Objects.requireNonNull(clock);
    }

    public void record(Long userId, String action, String entityName, long entityId) {
        transactionManager.execute(connection -> {
            auditLogDao.insert(connection,
                    new AuditLogDao.AuditEvent(0, userId, action, entityName, entityId,
                            LocalDateTime.now(clock)));
            return null;
        });
    }

    @Override
    public void recordAttempt(Long userId, boolean successful) {
        record(userId, successful ? "AUTH_SUCCESS" : "AUTH_FAILURE",
                "AUTHENTICATION", userId == null ? 0 : userId);
    }
}
