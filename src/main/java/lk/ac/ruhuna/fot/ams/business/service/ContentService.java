package lk.ac.ruhuna.fot.ams.business.service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.nio.file.Path;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.MaterialRow;
import lk.ac.ruhuna.fot.ams.data.dao.ContentDao.NoticeRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ContentService {
    private final ContentDao contentDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;
    private final MaterialFileStore materialFileStore;

    public ContentService(
            ContentDao contentDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this(contentDao, courseDao, auditLogDao, transactions, authorization, clock,
                new MaterialFileStore(Path.of("src", "main", "resources", "uploads")));
    }

    public ContentService(
            ContentDao contentDao, CourseDao courseDao, AuditLogDao auditLogDao,
            TransactionManager transactions, AuthorizationService authorization,
            Clock clock, MaterialFileStore materialFileStore) {
        this.contentDao = Objects.requireNonNull(contentDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
        this.materialFileStore = Objects.requireNonNull(materialFileStore);
    }

    public long addMaterial(AuthenticatedSession actor, int offeringId, String title, String filePath) {
        int lecturerId = contentManagerLecturerId(actor, offeringId);
        if (title == null || title.isBlank()) {
            throw new ValidationException("Course material title is required.");
        }
        String normalizedPath = validateControlledPath(filePath);
        return transactions.execute(connection -> {
            long materialId = contentDao.insertMaterial(connection,
                    new MaterialRow(0, offeringId, lecturerId, title.trim(), normalizedPath, false));
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MATERIAL_CREATE", "COURSE_MATERIAL",
                    materialId, LocalDateTime.now(clock)));
            return materialId;
        });
    }

    public void setMaterialVisibility(
            AuthenticatedSession actor, int offeringId, int materialId, boolean visible) {
        int lecturerId = contentManagerLecturerId(actor, offeringId);
        if (materialId <= 0) {
            throw new ValidationException("Course material identifier is invalid.");
        }
        MaterialRow material = contentDao.findMaterialsForOffering(offeringId).stream()
                .filter(row -> row.id() == materialId)
                .findFirst().orElseThrow(() -> new ValidationException("Course material was not found."));
        if (visible && !materialFileStore.isReadable(material.filePath())) {
            throw new ValidationException("Resource file is missing or unreadable. Students cannot access this material.");
        }
        transactions.execute(connection -> {
            contentDao.setMaterialVisibility(connection, materialId, material.lecturerId(), offeringId, visible);
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), visible ? "MATERIAL_PUBLISH" : "MATERIAL_HIDE",
                    "COURSE_MATERIAL", materialId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public void updateMaterial(
            AuthenticatedSession actor, int offeringId, int materialId, String title, String filePath) {
        contentManagerLecturerId(actor, offeringId);
        if (materialId <= 0 || title == null || title.isBlank()) {
            throw new ValidationException("Course material details are invalid.");
        }
        String normalizedPath = validateControlledPath(filePath);
        MaterialRow existing = contentDao.findMaterialsForOffering(offeringId).stream()
                .filter(row -> row.id() == materialId)
                .findFirst().orElseThrow(() -> new ValidationException("Course material was not found."));
        transactions.execute(connection -> {
            contentDao.updateMaterial(connection,
                    new MaterialRow(materialId, offeringId, existing.lecturerId(), title.trim(), normalizedPath));
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MATERIAL_UPDATE", "COURSE_MATERIAL",
                    materialId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<MaterialRow> materialsForStudent(AuthenticatedSession actor, int offeringId) {
        authorization.requireUndergraduateForOffering(actor, offeringId);
        return contentDao.findMaterialsForStudent(
                authorization.studentId(actor), offeringId).stream()
                .filter(MaterialRow::visible)
                .filter(row -> materialFileStore.isReadable(row.filePath())).toList();
    }

    /** Reauthorizes and rechecks the file when the student opens it, after list loading. */
    public Path studentMaterialFile(AuthenticatedSession actor, int offeringId, int materialId) {
        authorization.requireUndergraduateForOffering(actor, offeringId);
        if (materialId <= 0) throw new ValidationException("Course material identifier is invalid.");
        MaterialRow row = contentDao.findMaterialsForStudent(authorization.studentId(actor), offeringId).stream()
                .filter(material -> material.id() == materialId)
                .findFirst().orElseThrow(() -> new ValidationException("Course material is not available."));
        return materialFileStore.resolveReadable(row.filePath())
                .orElseThrow(() -> new ValidationException("This material file is no longer available."));
    }

    public List<MaterialRow> materialsForLecturer(AuthenticatedSession actor, int offeringId) {
        contentManagerLecturerId(actor, offeringId);
        return contentDao.findMaterialsForOffering(offeringId);
    }

    public boolean lecturerMaterialFileAvailable(
            AuthenticatedSession actor, int offeringId, int materialId) {
        contentManagerLecturerId(actor, offeringId);
        MaterialRow row = contentDao.findMaterialsForOffering(offeringId).stream()
                .filter(material -> material.id() == materialId)
                .findFirst().orElseThrow(() -> new ValidationException("Course material was not found."));
        return materialFileStore.isReadable(row.filePath());
    }

    private int contentManagerLecturerId(AuthenticatedSession actor, int offeringId) {
        if (actor != null && actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
            return authorization.lecturerId(actor);
        }
        throw new AuthorizationException("Only an assigned lecturer may manage learning materials.");
    }

    public long createNotice(AuthenticatedSession actor, NoticeRow notice) {
        authorization.requireRole(actor, Role.ADMIN, Role.TECHNICAL_OFFICER);
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
