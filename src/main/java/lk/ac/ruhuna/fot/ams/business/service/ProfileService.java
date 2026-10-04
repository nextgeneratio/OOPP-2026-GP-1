package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ProfileService {
    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;

    public ProfileService(
            UserDao userDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this.userDao = Objects.requireNonNull(userDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
    }

    public void updateStaffProfile(
            AuthenticatedSession actor, String email, String fullName, String phone) {
        if (actor == null || (actor.role() != Role.LECTURER && actor.role() != Role.TECHNICAL_OFFICER)) {
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

    private void audit(java.sql.Connection connection, AuthenticatedSession actor, String action)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, actor.userId(), action, "USER_PROFILE", actor.userId(), LocalDateTime.now(clock)));
    }
}
