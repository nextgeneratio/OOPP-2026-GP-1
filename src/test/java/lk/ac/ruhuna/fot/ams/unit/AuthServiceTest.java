package lk.ac.ruhuna.fot.ams.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.UserLookup;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void authenticatesActiveUsersAuditsAndClearsPasswordBuffer() {
        String hash = hasher.hash("password-1".toCharArray());
        User user = new Admin(11, "ADM11", hash, "admin@example.edu", true);
        RecordingAudit audit = new RecordingAudit();
        AuthService service = new AuthService(usernameLookup(user), hasher, audit);
        char[] password = "password-1".toCharArray();

        AuthenticatedSession session = service.authenticate("ADM11", password);

        assertThat(session.userId()).isEqualTo(11);
        assertThat(session.role()).isEqualTo(Role.ADMIN);
        assertThat(password).containsOnly('\0');
        assertThat(audit.attempts).containsExactly(new Attempt(11L, true));
    }

    @Test
    void rejectsBadCredentialsAndAuditsWithoutPasswordData() {
        User user = new Admin(12, "ADM12", hasher.hash("password-2".toCharArray()),
                "admin2@example.edu", true);
        RecordingAudit audit = new RecordingAudit();
        AuthService service = new AuthService(usernameLookup(user), hasher, audit);
        char[] badPassword = "incorrect".toCharArray();

        assertThatThrownBy(() -> service.authenticate("ADM12", badPassword))
                .isInstanceOf(AuthenticationException.class);

        assertThat(badPassword).containsOnly('\0');
        assertThat(audit.attempts).containsExactly(new Attempt(12L, false));
    }

    @Test
    void auditsUnknownAndInactiveAccountsAndAcceptsNullPasswordWithoutLeakingDetails() {
        User inactive = new Admin(13, "ADM13", "unused", "admin3@example.edu", false);
        RecordingAudit audit = new RecordingAudit();
        UserLookup lookup = new UserLookup() {
            @Override
            public Optional<User> findActiveByUsername(String username) {
                return Optional.empty();
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return "ADM13".equals(username) ? Optional.of(inactive) : Optional.empty();
            }
        };
        AuthService service = new AuthService(lookup, hasher, audit);

        assertThatThrownBy(() -> service.authenticate("unknown", "password".toCharArray()))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> service.authenticate("ADM13", "password".toCharArray()))
                .isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> service.authenticate("", null))
                .isInstanceOf(AuthenticationException.class);

        assertThat(audit.attempts).containsExactly(
                new Attempt(null, false), new Attempt(13L, false), new Attempt(null, false));
    }

    private UserLookup usernameLookup(User user) {
        return new UserLookup() {
            @Override
            public Optional<User> findActiveByUsername(String username) {
                return user.username().equals(username) && user.isActive()
                        ? Optional.of(user) : Optional.empty();
            }

            @Override
            public Optional<User> findByUsername(String username) {
                return user.username().equals(username) ? Optional.of(user) : Optional.empty();
            }
        };
    }

    private record Attempt(Long userId, boolean successful) {
    }

    private static final class RecordingAudit implements lk.ac.ruhuna.fot.ams.business.service.AuthenticationAudit {
        private final List<Attempt> attempts = new ArrayList<>();

        @Override
        public void recordAttempt(Long userId, boolean successful) {
            attempts.add(new Attempt(userId, successful));
        }
    }
}
