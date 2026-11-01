package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao.UserProfile;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ProfileService {
    public record ProfileView(
            long id,
            String username,
            Role role,
            String email,
            boolean active,
            String fullName,
            String phone,
            Integer departmentId,
            Integer batchId,
            String studentNumber,
            String profilePicture,
            String departmentName,
            String batchCode,
            Integer academicLevel,
            Integer semesterNumber) {
    }

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public ProfileService(
            UserDao userDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            PasswordHasher passwordHasher,
            Clock clock) {
        this.userDao = Objects.requireNonNull(userDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public ProfileView ownProfile(AuthenticatedSession actor) {
        if (actor == null || actor.userId() <= 0) {
            throw new AuthorizationException("A signed-in user is required to view this profile.");
        }
        UserProfile profile = userDao.findProfileById(actor.userId())
                .orElseThrow(() -> new NotFoundException("User profile was not found."));
        return new ProfileView(profile.id(), profile.username(), profile.role(), profile.email(),
                profile.active(), profile.fullName(), profile.phone(), profile.departmentId(),
                profile.batchId(), profile.studentNumber(), profile.profilePicture(),
                profile.departmentName(), profile.batchCode(), profile.academicLevel(),
                profile.semesterNumber());
    }

    public void changePassword(
            AuthenticatedSession actor, char[] currentPassword, char[] newPassword, char[] confirmation) {
        try {
            if (actor == null || actor.userId() <= 0) {
                throw new AuthorizationException("A signed-in user is required to change this password.");
            }
            if (currentPassword == null || currentPassword.length == 0
                    || newPassword == null || confirmation == null) {
                throw new ValidationException("Enter the current password and confirm the new password.");
            }
            if (!Arrays.equals(newPassword, confirmation)) {
                throw new ValidationException("The new password and confirmation do not match.");
            }
            UserProfile profile = userDao.findProfileById(actor.userId())
                    .orElseThrow(() -> new NotFoundException("User profile was not found."));
            if (!profile.active() || !passwordHasher.verify(currentPassword, profile.passwordHash())) {
                throw new AuthenticationException("The current password is incorrect.");
            }
            if (passwordHasher.verify(newPassword, profile.passwordHash())) {
                throw new ValidationException("Choose a new password different from your current password.");
            }
            String newHash = passwordHasher.hash(newPassword);
            transactions.execute(connection -> {
                int updated = userDao.updatePassword(
                        connection, actor.userId(), profile.passwordHash(), newHash);
                if (updated != 1) {
                    throw new AuthenticationException("The current password is incorrect.");
                }
                auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                        0, actor.userId(), "PASSWORD_CHANGE", "USER", actor.userId(),
                        LocalDateTime.now(clock)));
                return null;
            });
        } finally {
            clear(currentPassword);
            clear(newPassword);
            clear(confirmation);
        }
    }

    public void updateStaffProfile(
            AuthenticatedSession actor, String email, String fullName, String phone) {
        if (actor == null || (actor.role() != Role.ADMIN && actor.role() != Role.LECTURER
                && actor.role() != Role.TECHNICAL_OFFICER)) {
            throw new AuthorizationException("You do not have permission to update this profile.");
        }
        validateContact(email, fullName);
        transactions.execute(connection -> {
            int updated = userDao.updateStaffContact(
                    connection, actor.userId(), actor.role(), email.trim(), fullName.trim(), phone);
            if (updated != 1) {
                throw new NotFoundException("User profile was not found.");
            }
            audit(connection, actor, "PROFILE_UPDATE");
            return null;
        });
    }

    public void updateUndergraduateProfile(
            AuthenticatedSession actor, String email, String profilePicture) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        if (!validEmail(email)) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (profilePicture != null
                && !profilePicture.matches("^uploads/[0-9a-fA-F-]{36}\\.(png|jpe?g)$")) {
            throw new ValidationException("Profile picture reference must use a controlled UUID file path.");
        }
        transactions.execute(connection -> {
            int updated = userDao.updateUndergraduateContact(
                    connection, actor.userId(), email.trim(), profilePicture);
            if (updated != 1) {
                throw new NotFoundException("User profile was not found.");
            }
            audit(connection, actor, "PROFILE_UPDATE");
            return null;
        });
    }

    private void validateContact(String email, String fullName) {
        if (!validEmail(email) || fullName == null || fullName.isBlank()) {
            throw new ValidationException("Enter a valid email address and full name.");
        }
    }

    private boolean validEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private void clear(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    private void audit(java.sql.Connection connection, AuthenticatedSession actor, String action)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, actor.userId(), action, "USER_PROFILE", actor.userId(), LocalDateTime.now(clock)));
    }
}
