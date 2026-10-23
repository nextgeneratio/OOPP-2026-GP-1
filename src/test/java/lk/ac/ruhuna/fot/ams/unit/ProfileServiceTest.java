package lk.ac.ruhuna.fot.ams.unit;

import java.sql.Connection;
import java.sql.SQLException;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lk.ac.ruhuna.fot.ams.business.service.ProfileService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao.UserProfile;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileServiceTest {
    @Test
    void returnsOnlyTheSignedInUsersProfileFields() {
        UserDao userDao = mock(UserDao.class);
        when(userDao.findProfileById(11L)).thenReturn(Optional.of(new UserProfile(
                11, "STU001", "password-hash-must-not-leak", Role.UNDERGRADUATE,
                "stu001@example.edu", true, "Undergraduate 001", null,
                null, 1, "STU001", null)));
        ProfileService service = service(userDao);

        ProfileService.ProfileView profile = service.ownProfile(
                new AuthenticatedSession(11, "STU001", Role.UNDERGRADUATE, "Undergraduate 001"));

        assertThat(profile.username()).isEqualTo("STU001");
        assertThat(profile.role()).isEqualTo(Role.UNDERGRADUATE);
        assertThat(profile.email()).isEqualTo("stu001@example.edu");
        assertThat(profile.studentNumber()).isEqualTo("STU001");
        assertThat(profile.toString()).doesNotContain("password-hash-must-not-leak");
    }

    @Test
    void rejectsProfileReadsWithoutAnAuthenticatedIdentity() {
        ProfileService service = service(mock(UserDao.class));

        assertThatThrownBy(() -> service.ownProfile(null))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void changesPasswordAuditsAndClearsAllPasswordBuffers() throws SQLException {
        UserDao userDao = mock(UserDao.class);
        AuditLogDao auditDao = mock(AuditLogDao.class);
        ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
        AtomicBoolean committed = new AtomicBoolean();
        Connection connection = connection(committed);
        PasswordHasher hasher = new PasswordHasher();
        String currentHash = hasher.hash("Current#26".toCharArray());
        when(userDao.findProfileById(11L)).thenReturn(Optional.of(userProfile(currentHash, true)));
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(userDao.updatePassword(eq(connection), eq(11L), eq(currentHash),
                argThat(hash -> hasher.verify("NewPassword#27".toCharArray(), hash)))).thenReturn(1);

        char[] current = "Current#26".toCharArray();
        char[] replacement = "NewPassword#27".toCharArray();
        char[] confirmation = "NewPassword#27".toCharArray();
        passwordService(userDao, auditDao, connectionProvider, hasher).changePassword(
                new AuthenticatedSession(11, "STU001", Role.UNDERGRADUATE, "Undergraduate 001"),
                current, replacement, confirmation);

        org.mockito.Mockito.verify(auditDao).insert(eq(connection), argThat(event ->
                event.action().equals("PASSWORD_CHANGE") && event.userId() == 11L));
        assertThat(committed.get()).isTrue();
        assertThat(current).containsOnly('\0');
        assertThat(replacement).containsOnly('\0');
        assertThat(confirmation).containsOnly('\0');
    }

    @Test
    void rejectsAnIncorrectCurrentPasswordAndClearsBuffers() {
        UserDao userDao = mock(UserDao.class);
        PasswordHasher hasher = new PasswordHasher();
        when(userDao.findProfileById(11L)).thenReturn(Optional.of(
                userProfile(hasher.hash("Current#26".toCharArray()), true)));
        char[] current = "WrongPassword#1".toCharArray();
        char[] replacement = "NewPassword#27".toCharArray();
        char[] confirmation = "NewPassword#27".toCharArray();

        assertThatThrownBy(() -> passwordService(userDao, mock(AuditLogDao.class),
                mock(ConnectionProvider.class), hasher).changePassword(
                        new AuthenticatedSession(11, "STU001", Role.UNDERGRADUATE, "Undergraduate 001"),
                        current, replacement, confirmation))
                .isInstanceOf(AuthenticationException.class);
        assertThat(current).containsOnly('\0');
        assertThat(replacement).containsOnly('\0');
        assertThat(confirmation).containsOnly('\0');
    }

    @Test
    void rejectsMismatchedShortAndReusedPasswords() {
        PasswordHasher hasher = new PasswordHasher();
        String currentHash = hasher.hash("Current#26".toCharArray());
        UserDao userDao = mock(UserDao.class);
        when(userDao.findProfileById(11L)).thenReturn(Optional.of(userProfile(currentHash, true)));
        ProfileService service = passwordService(
                userDao, mock(AuditLogDao.class), mock(ConnectionProvider.class), hasher);
        AuthenticatedSession actor =
                new AuthenticatedSession(11, "STU001", Role.UNDERGRADUATE, "Undergraduate 001");

        assertThatThrownBy(() -> service.changePassword(actor, "Current#26".toCharArray(),
                "NewPassword#27".toCharArray(), "DifferentPassword".toCharArray()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("do not match");
        assertThatThrownBy(() -> service.changePassword(actor, "Current#26".toCharArray(),
                "short".toCharArray(), "short".toCharArray()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("at least 8");
        assertThatThrownBy(() -> service.changePassword(actor, "Current#26".toCharArray(),
                "Current#26".toCharArray(), "Current#26".toCharArray()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("different");
    }

    private ProfileService service(UserDao userDao) {
        return new ProfileService(userDao, mock(AuditLogDao.class),
                new TransactionManager(mock(ConnectionProvider.class)),
                new AuthorizationService(), new PasswordHasher(), Clock.systemUTC());
    }

    private ProfileService passwordService(
            UserDao userDao, AuditLogDao auditDao, ConnectionProvider connectionProvider,
            PasswordHasher passwordHasher) {
        return new ProfileService(userDao, auditDao, new TransactionManager(connectionProvider),
                new AuthorizationService(), passwordHasher, Clock.systemUTC());
    }

    private UserProfile userProfile(String passwordHash, boolean active) {
        return new UserProfile(11, "STU001", passwordHash, Role.UNDERGRADUATE,
                "stu001@example.edu", active, "Undergraduate 001", null,
                null, 1, "STU001", null);
    }

    private Connection connection(AtomicBoolean committed) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if (method.getName().equals("commit")) {
                        committed.set(true);
                    }
                    if (method.getName().equals("isClosed")) {
                        return false;
                    }
                    return null;
                });
    }
}
