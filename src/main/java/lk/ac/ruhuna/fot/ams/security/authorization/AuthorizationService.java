package lk.ac.ruhuna.fot.ams.security.authorization;

import java.util.Arrays;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthorizationService {
    public void requireRole(AuthenticatedSession session, Role... allowedRoles) {
        if (session == null || Arrays.stream(allowedRoles).noneMatch(session.role()::equals)) {
            throw new AuthorizationException("You do not have permission for this action.");
        }
    }

    public void requireSelf(AuthenticatedSession session, long userId) {
        if (session == null || session.userId() != userId) {
            throw new AuthorizationException("You do not have permission for this action.");
        }
    }
}
