package lk.ac.ruhuna.fot.ams.business.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.MaterialRow;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.NoticeRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ContentService {
    private final ContentDao contentDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;

    public ContentService(
            ContentDao contentDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this.contentDao = Objects.requireNonNull(contentDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
    }

    public long addMaterial(AuthenticatedSession actor, int offeringId, String title, String filePath) {
        authorization.requireLecturerForOffering(actor, offeringId);
        if (title == null || title.isBlank()) {
            throw new ValidationException("Course material title is required.");
        }
        String normalizedPath = validateControlledPath(filePath);
        int lecturerId = authorization.lecturerId(actor);
        return transactions.execute(connection -> {
            long materialId = contentDao.insertMaterial(connection,
                    new MaterialRow(0, offeringId, lecturerId, title.trim(), normalizedPath));
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MATERIAL_CREATE", "COURSE_MATERIAL",
                    materialId, LocalDateTime.now(clock)));
            return materialId;
        });
    }

    public void updateMaterial(
            AuthenticatedSession actor, int offeringId, int materialId, String title, String filePath) {
        authorization.requireLecturerForOffering(actor, offeringId);
        if (materialId <= 0 || title == null || title.isBlank()) {
            throw new ValidationException("Course material details are invalid.");
        }
        String normalizedPath = validateControlledPath(filePath);
        int lecturerId = authorization.lecturerId(actor);
        transactions.execute(connection -> {
            int updated = contentDao.updateMaterial(connection,
                    new MaterialRow(materialId, offeringId, lecturerId, title.trim(), normalizedPath));
            if (updated == 0 && contentDao.findMaterialsForOffering(offeringId).stream()
                    .noneMatch(material -> material.id() == materialId && material.lecturerId() == lecturerId)) {
                throw new ValidationException("Course material was not found for this lecturer and offering.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MATERIAL_UPDATE", "COURSE_MATERIAL",
                    materialId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<MaterialRow> materialsForStudent(AuthenticatedSession actor, int offeringId) {
        authorization.requireUndergraduateForOffering(actor, offeringId);
        return contentDao.findMaterialsForStudent(
                authorization.studentId(actor), offeringId);
    }

    public List<MaterialRow> materialsForLecturer(AuthenticatedSession actor, int offeringId) {
        authorization.requireLecturerForOffering(actor, offeringId);
        return contentDao.findMaterialsForOffering(offeringId);
    }

    public long createNotice(AuthenticatedSession actor, NoticeRow notice) {
        authorization.requireAdmin(actor);
        validateNotice(notice);
        return transactions.execute(connection -> {
            long noticeId = contentDao.insertNotice(connection, notice);
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "NOTICE_CREATE", "NOTICE", noticeId, LocalDateTime.now(clock)));
            return noticeId;
        });
    }

    public List<NoticeRow> visibleNotices(AuthenticatedSession actor) {
        if (actor == null) {
            throw new AuthorizationException("You must sign in to view notices.");
        }
        Integer batchId = actor.role() == Role.UNDERGRADUATE
                ? authorization.studentBatchId(actor) : null;
        return contentDao.findVisibleNotices(actor.role(), batchId, LocalDateTime.now(clock));
    }

    private String validateControlledPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new ValidationException("Course material file reference is required.");
        }
        Path normalized;
        try {
            normalized = Path.of(filePath).normalize();
        } catch (java.nio.file.InvalidPathException invalidPath) {
            throw new ValidationException("Course material file reference is invalid.");
        }
        if (normalized.isAbsolute() || normalized.startsWith("..")
                || !normalized.toString().replace('\\', '/').startsWith("uploads/")) {
            throw new ValidationException("Course material must use a controlled application file path.");
        }
        return normalized.toString().replace('\\', '/');
    }

    private void validateNotice(NoticeRow notice) {
        if (notice == null || notice.title() == null || notice.title().isBlank()
                || notice.body() == null || notice.body().isBlank()
                || notice.audience() == null || notice.publishDate() == null) {
            throw new ValidationException("Notice title, body, and publication date are required.");
        }
        boolean validAudience = switch (notice.audience()) {
            case "ALL" -> notice.targetRole() == null && notice.targetBatchId() == null;
            case "ROLE" -> notice.targetRole() != null && notice.targetBatchId() == null;
            case "BATCH" -> notice.targetRole() == null
                    && notice.targetBatchId() != null && notice.targetBatchId() > 0;
            default -> false;
        };
        if (!validAudience
                || (notice.expiryDate() != null && !notice.expiryDate().isAfter(notice.publishDate()))) {
            throw new ValidationException("Notice audience or publication/expiry dates are invalid.");
        }
    }
}
