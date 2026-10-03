package lk.ac.ruhuna.fot.ams.security.session;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public record AuthenticatedSession(long userId, String username, Role role, String displayName) {
    public AuthenticatedSession {
        if (username == null || username.isBlank() || role == null) {
            throw new IllegalArgumentException("Authenticated session requires identity and role.");
        }
    }
}
