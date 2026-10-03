package lk.ac.ruhuna.fot.ams.unit;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationServiceTest {
    private final AuthorizationService authorization = new AuthorizationService();
    private final AuthenticatedSession lecturer =
            new AuthenticatedSession(2, "LEC001", Role.LECTURER, "Lecturer 001");

    @Test
    void acceptsAllowedRole() {
        authorization.requireRole(lecturer, Role.LECTURER);
    }

    @Test
    void rejectsDisallowedRole() {
        assertThatThrownBy(() -> authorization.requireRole(lecturer, Role.ADMIN))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void rejectsAccessToAnotherUser() {
        assertThatThrownBy(() -> authorization.requireSelf(lecturer, 3))
                .isInstanceOf(AuthorizationException.class);
    }
}
