package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.MedicalRecord;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class MedicalService {
    private static final long MAX_EVIDENCE_BYTES = 10L * 1024 * 1024;
    private static final String EVIDENCE_DIRECTORY = "medical-evidence";
    private final MedicalDao medicalDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;

    public MedicalService(
            MedicalDao medicalDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this.medicalDao = Objects.requireNonNull(medicalDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
    }

    public long submit(AuthenticatedSession actor, MedicalRow request) {
        if (request == null) {
            throw new ValidationException("Medical record details are required.");
        }
        if (actor == null || actor.role() != Role.UNDERGRADUATE) {
            throw new AuthorizationException("Only a student may submit a medical request.");
        }
        authorization.requireUndergraduateForStudent(actor, request.studentId());
        if (request.offeringId() != null) {
            authorization.requireUndergraduateForOffering(actor, request.offeringId());
        }
        new MedicalRecord(request.medicalDate(), request.startDate(), request.endDate(), request.reason());
        validateEvidenceReference(request.evidenceReference());
        if (request.offeringId() != null && courseDao.findActiveEnrollments(request.offeringId()).stream()
                .noneMatch(enrollment -> enrollment.studentId() == request.studentId())) {
            throw new ValidationException("Student is not actively enrolled in this course offering.");
        }
        List<MedicalRow> overlaps = medicalDao.findOverlapping(
                request.studentId(), request.offeringId(), request.startDate(), request.endDate());
        if (overlaps.stream().anyMatch(record -> record.status() != ApprovalStatus.REJECTED)) {
            throw new BusinessRuleException("An overlapping pending or approved medical record already exists.");
        }
        MedicalRow pending = new MedicalRow(0, request.studentId(), request.offeringId(),
                request.medicalDate(), request.startDate(), request.endDate(), request.reason(),
                request.evidenceReference(), ApprovalStatus.PENDING, false, null);
        return transactions.execute(connection -> {
            long medicalId = medicalDao.insert(connection, pending);
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MEDICAL_CREATE", "MEDICAL_RECORD",
                    medicalId, LocalDateTime.now(clock)));
            return medicalId;
        });
    }

    /** Stores a student-selected PDF outside the classpath/public resources before persisting its opaque reference. */
    public long submitWithPdf(AuthenticatedSession actor, MedicalRow request, Path sourcePdf) {
        if (request == null) {
            throw new ValidationException("Medical record details are required.");
        }
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        validatePdf(sourcePdf);
        Path privateDirectory = Path.of("var", EVIDENCE_DIRECTORY).toAbsolutePath().normalize();
        Path stored = privateDirectory.resolve(UUID.randomUUID() + ".pdf").normalize();
        if (!stored.startsWith(privateDirectory)) {
            throw new ValidationException("Medical proof storage path is invalid.");
        }
        try {
            Files.createDirectories(privateDirectory);
            Files.copy(sourcePdf, stored, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException exception) {
            throw new ValidationException("The medical proof could not be stored safely.");
        }
        try {
            MedicalRow securedRequest = new MedicalRow(request.id(), authorization.studentId(actor), request.offeringId(),
                    request.medicalDate(), request.startDate(), request.endDate(), request.reason(),
                    EVIDENCE_DIRECTORY + "/" + stored.getFileName(), request.status(), request.affectsEligibility(), null);
            return submit(actor, securedRequest);
        } catch (RuntimeException exception) {
            try {
                Files.deleteIfExists(stored);
            } catch (IOException ignored) {
                // The private file is inaccessible without an authorised retrieval flow.
            }
            throw exception;
        }
    }

    public void decide(AuthenticatedSession actor, int medicalId, ApprovalStatus decision, String rejectionReason) {
        if (decision != ApprovalStatus.APPROVED && decision != ApprovalStatus.REJECTED) {
            throw new ValidationException("A medical record must be approved or rejected.");
        }
        MedicalRow medical = medicalDao.findById(medicalId)
                .orElseThrow(() -> new NotFoundException("Medical record was not found."));
        authorizeReviewerScope(actor, medical.studentId(), medical.offeringId());
        if (medical.status() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Only pending medical records can be decided.");
        }
        String normalizedReason = normalizeDecisionReason(decision, rejectionReason);
        transactions.execute(connection -> {
            int updated = medicalDao.decide(connection, medicalId, decision, normalizedReason);
            if (updated != 1) {
                throw new BusinessRuleException("Medical record has already been decided.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), decision == ApprovalStatus.APPROVED
                            ? "MEDICAL_APPROVE" : "MEDICAL_REJECT",
                    "MEDICAL_RECORD", medicalId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<MedicalRow> findForStudent(AuthenticatedSession actor, int studentId, Integer offeringId) {
        if (actor == null) {
            throw new AuthorizationException("You do not have permission to view medical records.");
        }
        if (actor.role() == Role.UNDERGRADUATE) {
            authorization.requireUndergraduateForStudent(actor, studentId);
        } else if (actor.role() == Role.TECHNICAL_OFFICER) {
            authorizeOfficerScope(actor, studentId, offeringId);
        } else if (actor.role() == Role.ADMIN) {
            authorization.requireAdmin(actor);
        } else {
            throw new AuthorizationException("You do not have permission to view medical records.");
        }
        return medicalDao.findForStudent(studentId, offeringId);
    }

    public List<MedicalRow> findForCurrentStudent(AuthenticatedSession actor) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        return medicalDao.findForStudent(authorization.studentId(actor), null);
    }

    public List<MedicalRow> pendingForReviewer(AuthenticatedSession actor) {
        if (actor == null || (actor.role() != Role.ADMIN && actor.role() != Role.TECHNICAL_OFFICER)) {
            throw new AuthorizationException("You do not have permission to review medical requests.");
        }
        return medicalDao.findPending().stream().filter(record -> {
            if (actor.role() == Role.ADMIN) return true;
            try {
                authorizeOfficerScope(actor, record.studentId(), record.offeringId());
                return true;
            } catch (AuthorizationException denied) {
                return false;
            }
        }).toList();
    }

    public Path evidenceFor(AuthenticatedSession actor, int medicalId) {
        MedicalRow medical = medicalDao.findById(medicalId)
                .orElseThrow(() -> new NotFoundException("Medical record was not found."));
        if (actor == null) {
            throw new AuthorizationException("You do not have permission to view medical proof.");
        }
        if (actor.role() == Role.UNDERGRADUATE) {
            authorization.requireUndergraduateForStudent(actor, medical.studentId());
        } else {
            authorizeReviewerScope(actor, medical.studentId(), medical.offeringId());
        }
        return resolveStoredEvidence(medical.evidenceReference());
    }

    private void authorizeReviewerScope(
            AuthenticatedSession actor, int studentId, Integer offeringId) {
        if (actor != null && actor.role() == Role.ADMIN) {
            authorization.requireAdmin(actor);
            return;
        }
        authorizeOfficerScope(actor, studentId, offeringId);
    }

    private void validateEvidenceReference(String evidenceReference) {
        if (evidenceReference == null || !evidenceReference.matches("medical-evidence/[0-9a-fA-F-]{36}\\.pdf")) {
            throw new ValidationException("A valid PDF medical proof is required.");
        }
    }

    private String normalizeDecisionReason(ApprovalStatus decision, String rejectionReason) {
        if (decision == ApprovalStatus.APPROVED) {
            return null;
        }
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new ValidationException("A rejection reason is required.");
        }
        String normalized = rejectionReason.trim();
        if (normalized.length() > 500) {
            throw new ValidationException("A rejection reason must be 500 characters or fewer.");
        }
        return normalized;
    }

    private Path resolveStoredEvidence(String evidenceReference) {
        validateEvidenceReference(evidenceReference);
        Path directory = Path.of("var", EVIDENCE_DIRECTORY).toAbsolutePath().normalize();
        Path file = directory.resolve(Path.of(evidenceReference).getFileName()).normalize();
        try {
            if (!file.startsWith(directory) || !Files.isRegularFile(file) || Files.isSymbolicLink(file)) {
                throw new NotFoundException("Medical proof is unavailable.");
            }
        } catch (java.nio.file.InvalidPathException invalid) {
            throw new ValidationException("Medical proof reference is invalid.");
        }
        return file;
    }

    private void validatePdf(Path sourcePdf) {
        if (sourcePdf == null || !Files.isRegularFile(sourcePdf)
                || sourcePdf.getFileName() == null
                || !sourcePdf.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            throw new ValidationException("Select a PDF medical proof.");
        }
        try {
            if (Files.size(sourcePdf) <= 0 || Files.size(sourcePdf) > MAX_EVIDENCE_BYTES) {
                throw new ValidationException("Medical proof must be a PDF no larger than 10 MB.");
            }
            byte[] header;
            try (java.io.InputStream input = Files.newInputStream(sourcePdf)) {
                header = input.readNBytes(5);
            }
            if (header.length != 5 || header[0] != '%' || header[1] != 'P'
                    || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw new ValidationException("Medical proof is not a valid PDF file.");
            }
        } catch (IOException exception) {
            throw new ValidationException("The medical proof could not be read.");
        }
    }

    private void authorizeOfficerScope(
            AuthenticatedSession actor, int studentId, Integer offeringId) {
        if (actor == null || actor.role() != Role.TECHNICAL_OFFICER) {
            throw new AuthorizationException("You do not have permission to manage medical records.");
        }
        if (offeringId == null) {
            authorization.requireTechnicalOfficerForStudent(actor, studentId);
        } else {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        }
    }
}
