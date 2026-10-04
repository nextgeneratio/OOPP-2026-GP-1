package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        authorizeOfficerScope(actor, request.studentId(), request.offeringId());
        new MedicalRecord(request.medicalDate(), request.startDate(), request.endDate(), request.reason());
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
                request.evidenceReference(), ApprovalStatus.PENDING, false);
        return transactions.execute(connection -> {
            long medicalId = medicalDao.insert(connection, pending);
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "MEDICAL_CREATE", "MEDICAL_RECORD",
                    medicalId, LocalDateTime.now(clock)));
            return medicalId;
        });
    }

    public void decide(AuthenticatedSession actor, int medicalId, ApprovalStatus decision) {
        if (decision != ApprovalStatus.APPROVED && decision != ApprovalStatus.REJECTED) {
            throw new ValidationException("A medical record must be approved or rejected.");
        }
        MedicalRow medical = medicalDao.findById(medicalId)
                .orElseThrow(() -> new NotFoundException("Medical record was not found."));
        authorizeOfficerScope(actor, medical.studentId(), medical.offeringId());
        if (medical.status() != ApprovalStatus.PENDING) {
            throw new BusinessRuleException("Only pending medical records can be decided.");
        }
        transactions.execute(connection -> {
            int updated = medicalDao.decide(connection, medicalId, decision);
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
        } else if (actor.role() == Role.LECTURER) {
            if (offeringId == null) {
                throw new AuthorizationException("You do not have access to this medical record.");
            }
            authorization.requireLecturerForOffering(actor, offeringId);
            if (courseDao.findEnrollments(offeringId).stream()
                    .noneMatch(enrollment -> enrollment.studentId() == studentId)) {
                throw new AuthorizationException("You do not have access to this student medical record.");
            }
        } else {
            authorization.requireAdmin(actor);
        }
        return medicalDao.findForStudent(studentId, offeringId);
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
