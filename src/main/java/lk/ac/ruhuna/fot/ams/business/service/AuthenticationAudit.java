package lk.ac.ruhuna.fot.ams.business.service;

public interface AuthenticationAudit {
    void recordAttempt(Long userId, boolean successful);
}
