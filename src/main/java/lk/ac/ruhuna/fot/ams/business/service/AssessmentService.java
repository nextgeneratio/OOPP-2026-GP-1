package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.AssessmentType;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AssessmentService {
    private final AssessmentDao assessmentDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final AssessmentCalculator calculator;
    private final Clock clock;

    public AssessmentService(
            AssessmentDao assessmentDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            AssessmentCalculator calculator,
            Clock clock) {
        this.assessmentDao = Objects.requireNonNull(assessmentDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.calculator = Objects.requireNonNull(calculator);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<Long> publishPlan(
            AuthenticatedSession actor, int offeringId, Collection<AssessmentRow> assessments) {
        authorizeOffering(actor, offeringId);
        if (assessments == null || assessments.isEmpty()) {
            throw new ValidationException("A published assessment plan must contain assessments.");
        }
        if (assessments.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("Assessment plan contains an empty entry.");
        }
        List<AssessmentRow> plan = List.copyOf(assessments);
        if (plan.stream().anyMatch(item -> item.offeringId() != offeringId
                || item.name() == null || item.name().isBlank()
                || !isAssessmentType(item.type()))) {
            throw new ValidationException("Assessment plan contains invalid or mismatched entries.");
        }
        calculator.validatePublishedPlan(plan.stream()
                .map(item -> new AssessmentCalculator.WeightedMark(
                        BigDecimal.ZERO, item.weight(), item.caComponent()))
                .toList());
        BigDecimal caWeight = plan.stream()
                .filter(AssessmentRow::caComponent)
                .map(AssessmentRow::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (caWeight.signum() == 0) {
            throw new ValidationException("A published assessment plan must configure a non-zero CA component.");
        }
        return transactions.execute(connection -> {
            java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
            for (AssessmentRow item : plan) {
                long id = assessmentDao.insertAssessment(connection, item);
                ids.add(id);
                auditLogDao.insert(connection,
                        new AuditLogDao.AuditEvent(0, actor.userId(), "ASSESSMENT_CREATE",
                                "ASSESSMENT", id, LocalDateTime.now(clock)));
            }
            return List.copyOf(ids);
        });
    }

    private void authorizeOffering(AuthenticatedSession actor, int offeringId) {
        if (actor != null && actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.ADMIN) {
            authorization.requireAdmin(actor);
        } else {
            authorization.requireLecturerForOffering(actor, offeringId);
        }
    }

    private boolean isAssessmentType(String type) {
        if (type == null) {
            return false;
        }
        try {
            AssessmentType.valueOf(type);
            return true;
        } catch (IllegalArgumentException invalidType) {
            return false;
        }
    }
}
