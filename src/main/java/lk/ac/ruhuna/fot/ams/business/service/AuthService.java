package lk.ac.ruhuna.fot.ams.business.service;

import java.util.Arrays;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthService {
    private final UserLookup userLookup;
    private final PasswordHasher passwordHasher;

    public AuthService(UserLookup userLookup, PasswordHasher passwordHasher) {
        this.userLookup = userLookup;
        this.passwordHasher = passwordHasher;
    }

    public AuthenticatedSession authenticate(String username, char[] password) {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            throw new AuthenticationException("Username or password is incorrect.");
        }
        try {
            User user = userLookup.findActiveByUsername(username)
                    .orElseThrow(() -> new AuthenticationException("Username or password is incorrect."));
            if (!user.isActive() || !passwordHasher.verify(password, user.passwordHash())) {
                throw new AuthenticationException("Username or password is incorrect.");
            }
            return new AuthenticatedSession(user.id(), user.username(), user.role(), user.username());
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
