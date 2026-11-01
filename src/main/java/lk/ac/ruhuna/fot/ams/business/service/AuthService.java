package lk.ac.ruhuna.fot.ams.business.service;

import java.util.Arrays;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthService {
    private final UserLookup userLookup;
    private final PasswordHasher passwordHasher;
    private final AuthenticationAudit authenticationAudit;

    public AuthService(
            UserLookup userLookup,
            PasswordHasher passwordHasher,
            AuthenticationAudit authenticationAudit) {
        this.userLookup = userLookup;
        this.passwordHasher = passwordHasher;
        this.authenticationAudit = authenticationAudit;
    }

    public AuthenticatedSession authenticate(String username, char[] password) {
        Long attemptedUserId = null;
        try {
            if (username == null || username.isBlank() || password == null || password.length == 0) {
                throw new AuthenticationException("Username or password is incorrect.");
            }
            User user = userLookup.findByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("Username or password is incorrect."));
            attemptedUserId = user.id();
            if (!user.isActive() || !passwordHasher.verify(password, user.passwordHash())) {
                throw new AuthenticationException("Username or password is incorrect.");
            }
            authenticationAudit.recordAttempt(attemptedUserId, true);
            return new AuthenticatedSession(user.id(), user.username(), user.role(), user.username());
        } catch (AuthenticationException failure) {
            authenticationAudit.recordAttempt(attemptedUserId, false);
            throw failure;
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
        }
    }
}
