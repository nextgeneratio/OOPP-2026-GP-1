package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao;
import lk.ac.ruhuna.fot.ams.data.dao.UserDao.UserProfile;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class UserService {
    public record CreateUser(
            String username,
            char[] password,
            Role role,
            String email,
            String fullName,
            String phone,
            Integer departmentId,
            Integer batchId,
            String studentNumber) {
    }

    public record UpdateUser(
            long id,
            Role role,
            String email,
            boolean active,
            String fullName,
            String phone,
            Integer departmentId,
            Integer batchId,
            String studentNumber) {
    }

    public record UserView(
            long id,
            String username,
            Role role,
            String email,
            boolean active,
            String fullName,
            String phone,
            Integer departmentId,
            Integer batchId,
            String studentNumber) {
    }

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public UserService(
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

    public long create(AuthenticatedSession actor, CreateUser request) {
        authorization.requireAdmin(actor);
        validateCreate(request);
        String hash;
        try {
            hash = passwordHasher.hash(request.password());
        } finally {
            if (request.password() != null) {
                Arrays.fill(request.password(), '\0');
            }
        }
        UserProfile profile = new UserProfile(0, request.username().trim(), hash, request.role(),
                request.email().trim(), true, request.fullName().trim(), request.phone(),
                request.departmentId(), request.batchId(), request.studentNumber(), null);
        return transactions.execute(connection -> {
            long userId = userDao.create(connection, profile);
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "USER_CREATE", "USER", userId, LocalDateTime.now(clock)));
            return userId;
        });
    }

    public void update(AuthenticatedSession actor, UpdateUser request) {
        authorization.requireAdmin(actor);
        validateUpdate(request);
        UserProfile profile = new UserProfile(request.id(), null, null, request.role(),
                request.email().trim(), request.active(), request.fullName().trim(), request.phone(),
                request.departmentId(), request.batchId(), request.studentNumber(), null);
        transactions.execute(connection -> {
            int updated = userDao.update(connection, profile);
            if (updated == 0) {
                throw new NotFoundException("User was not found.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "USER_UPDATE", "USER", request.id(), LocalDateTime.now(clock)));
            return null;
        });
    }

    public void deactivate(AuthenticatedSession actor, long userId) {
        authorization.requireAdmin(actor);
        if (userId <= 0) {
            throw new ValidationException("User identifier is invalid.");
        }
        transactions.execute(connection -> {
            int changed = userDao.deactivate(connection, userId);
            if (changed == 0) {
                if (userDao.findById(userId).isEmpty()) {
                    throw new NotFoundException("User was not found.");
                }
                throw new BusinessRuleException("User is already inactive.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "USER_DEACTIVATE", "USER", userId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<UserView> search(AuthenticatedSession actor, String term, Role role, Boolean active) {
        authorization.requireAdmin(actor);
        return userDao.searchUsers(term, role, active).stream().map(profile ->
                new UserView(profile.id(), profile.username(), profile.role(), profile.email(),
                        profile.active(), profile.fullName(), profile.phone(), profile.departmentId(),
                        profile.batchId(), profile.studentNumber())).toList();
    }

    private void validateCreate(CreateUser request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.role() == null || !validEmail(request.email())
                || request.fullName() == null || request.fullName().isBlank()) {
            throw new ValidationException("User identity, role, email, and full name are required.");
        }
        validateRoleProfile(request.role(), request.departmentId(), request.batchId(), request.studentNumber());
    }

    private void validateUpdate(UpdateUser request) {
        if (request == null || request.id() <= 0 || request.role() == null || !validEmail(request.email())
                || request.fullName() == null || request.fullName().isBlank()) {
            throw new ValidationException("User update details are invalid.");
        }
        validateRoleProfile(request.role(), request.departmentId(), request.batchId(), request.studentNumber());
    }

    private void validateRoleProfile(
            Role role, Integer departmentId, Integer batchId, String studentNumber) {
        if ((role == Role.LECTURER || role == Role.TECHNICAL_OFFICER)
                && (departmentId == null || departmentId <= 0)) {
            throw new ValidationException("A department is required for staff users.");
        }
        if (role == Role.UNDERGRADUATE
                && (batchId == null || batchId <= 0 || studentNumber == null || studentNumber.isBlank())) {
            throw new ValidationException("A batch and student number are required for undergraduates.");
        }
    }

    private boolean validEmail(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
}
