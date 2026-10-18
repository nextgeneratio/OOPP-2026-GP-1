package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.MarkRow;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class MarksService {
    public record MarkInput(int assessmentId, int enrollmentId, BigDecimal score) {
    }

    private static final BigDecimal MAX_MARK = new BigDecimal("100");

    private final AssessmentDao assessmentDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final AssessmentCalculator calculator;
    private final Clock clock;

    public MarksService(
            AssessmentDao assessmentDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            AssessmentCalculator calculator,
            Clock clock) {
        this.assessmentDao = Objects.requireNonNull(assessmentDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.calculator = Objects.requireNonNull(calculator);
        this.clock = Objects.requireNonNull(clock);
    }

    public int save(AuthenticatedSession actor, int offeringId, List<MarkInput> entries) {
        authorization.requireLecturerForOffering(actor, offeringId);
        if (entries == null || entries.isEmpty() || entries.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("At least one valid mark entry is required.");
        }
        int lecturerId = authorization.lecturerId(actor);
        Set<Integer> assessmentIds = assessmentDao.findAssessments(offeringId).stream()
                .map(AssessmentRow::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<Integer> enrollmentIds = courseDao.findEnrollments(offeringId).stream()
                .map(CourseDao.EnrollmentRow::id)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> uniqueKeys = new HashSet<>();
        for (MarkInput input : entries) {
            if (input.assessmentId() <= 0 || input.enrollmentId() <= 0
                    || input.score() == null || input.score().signum() < 0
                    || input.score().compareTo(MAX_MARK) > 0
                    || !assessmentIds.contains(input.assessmentId())
                    || !enrollmentIds.contains(input.enrollmentId())) {
                throw new ValidationException("Mark entry is invalid or outside this course offering.");
            }
            if (!uniqueKeys.add(input.assessmentId() + ":" + input.enrollmentId())) {
                throw new ValidationException("A mark can be submitted only once per assessment and enrolment.");
            }
        }

        return transactions.execute(connection -> {
            Map<Integer, Map<Integer, MarkRow>> existingByEnrollment = new HashMap<>();
            for (int enrollmentId : entries.stream().map(MarkInput::enrollmentId).distinct().toList()) {
                Map<Integer, MarkRow> existing = new HashMap<>();
                assessmentDao.findMarks(connection, offeringId, enrollmentId)
                        .forEach(mark -> existing.put(mark.assessmentId(), mark));
                existingByEnrollment.put(enrollmentId, existing);
            }
            for (MarkInput input : entries) {
                MarkRow existing = existingByEnrollment.get(input.enrollmentId()).get(input.assessmentId());
                if (existing == null) {
                    long markId = assessmentDao.insertMark(connection,
                            new MarkRow(0, input.assessmentId(), input.enrollmentId(), input.score(), lecturerId));
                    audit(connection, actor.userId(), "MARK_CREATE", markId);
                } else {
                    assessmentDao.reviseMark(connection,
                            new MarkRow(existing.id(), input.assessmentId(), input.enrollmentId(),
                                    input.score(), lecturerId));
                    audit(connection, actor.userId(), "MARK_REVISE", existing.id());
                }
            }
            return entries.size();
        });
    }

    public BigDecimal calculateCaMark(
            AuthenticatedSession actor, int offeringId, int enrollmentId) {
        authorizeRead(actor, offeringId, enrollmentId);
        List<AssessmentRow> assessments = assessmentDao.findAssessments(offeringId);
        Map<Integer, MarkRow> marks = marksByAssessment(
                assessmentDao.findMarks(offeringId, enrollmentId));
        List<AssessmentCalculator.WeightedMark> caMarks = assessments.stream()
                .filter(AssessmentRow::caComponent)
                .map(assessment -> weightedMark(assessment, marks))
                .toList();
        return calculator.calculateCaMark(caMarks);
    }

    public BigDecimal calculateFinalMark(
            AuthenticatedSession actor, int offeringId, int enrollmentId) {
        authorizeRead(actor, offeringId, enrollmentId);
        List<AssessmentRow> assessments = assessmentDao.findAssessments(offeringId);
        Map<Integer, MarkRow> marks = marksByAssessment(
                assessmentDao.findMarks(offeringId, enrollmentId));
        List<AssessmentCalculator.WeightedMark> allMarks = assessments.stream()
                .map(assessment -> weightedMark(assessment, marks))
                .toList();
        return calculator.calculateFinalMark(allMarks);
    }

    private void authorizeRead(AuthenticatedSession actor, int offeringId, int enrollmentId) {
        if (actor != null && actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
            return;
        }
        if (actor != null && actor.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.UNDERGRADUATE) {
            int studentId = authorization.studentId(actor);
            if (courseDao.findEnrollments(offeringId).stream().noneMatch(enrollment ->
                    enrollment.id() == enrollmentId && enrollment.studentId() == studentId)) {
                throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException(
                        "You may only view your own course results.");
            }
            return;
        }
        throw new lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException(
                "You do not have permission to view marks.");
    }

    private Map<Integer, MarkRow> marksByAssessment(List<MarkRow> marks) {
        Map<Integer, MarkRow> byAssessment = new HashMap<>();
        marks.forEach(mark -> byAssessment.put(mark.assessmentId(), mark));
        return byAssessment;
    }

    private AssessmentCalculator.WeightedMark weightedMark(
            AssessmentRow assessment, Map<Integer, MarkRow> marks) {
        MarkRow mark = marks.get(assessment.id());
        if (mark == null) {
            throw new BusinessRuleException("Results are unavailable until all required marks are entered.");
        }
        return new AssessmentCalculator.WeightedMark(
                mark.score(), assessment.weight(), assessment.caComponent());
    }

    private void audit(java.sql.Connection connection, long userId, String action, long markId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, userId, action, "MARK", markId, LocalDateTime.now(clock)));
    }
}
