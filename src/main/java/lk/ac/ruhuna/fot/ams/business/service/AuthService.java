package lk.ac.ruhuna.fot.ams.business.service;

import java.util.Arrays;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.audit.AuditLog;
import lk.ac.ruhuna.fot.ams.audit.AuditService;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthService {
    private final UserLookup userLookup;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;

    public AuthService(UserLookup userLookup, PasswordHasher passwordHasher) {
        this(userLookup, passwordHasher, null);
    }

    public AuthService(UserLookup userLookup, PasswordHasher passwordHasher, AuditService auditService) {
        this.userLookup = userLookup;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
    }

    public AuthenticatedSession authenticate(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            recordAuditFailure(null, username);
            throw new AuthenticationException("Username or password is incorrect.");
        }
        try {
            Optional<User> userOpt = userLookup.findActiveByUsername(username);
            if (userOpt.isEmpty()) {
                recordAuditFailure(null, username);
                throw new AuthenticationException("Username or password is incorrect.");
            }
            User user = userOpt.get();
            if (!user.isActive()) {
                recordAuditFailure(user.id(), username);
                throw new AuthenticationException("User account is inactive.");
            }
            if (!passwordHasher.verify(password, user.passwordHash())) {
                recordAuditFailure(user.id(), username);
                throw new AuthenticationException("Username or password is incorrect.");
            }
            if (auditService != null) {
                auditService.log(new AuditLog(user.id(), "LOGIN_SUCCESS", "users", user.id()));
            }
            return new AuthenticatedSession(user.id(), user.username(), user.role(), user.username());
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void recordAuditFailure(Long userId, String username) {
        if (auditService != null) {
            auditService.log(new AuditLog(userId, "LOGIN_FAILED", "users", userId != null ? userId : 0L));
        }
    }
}

