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

    public record StudentMarks(int enrollmentId, String studentNumber, String fullName,
            Map<Integer, BigDecimal> scores, BigDecimal caContribution, boolean finalExamEligible) {
        public StudentMarks { scores = Map.copyOf(scores); }
    }

    public record MarkSheet(int offeringId, List<AssessmentRow> assessments, List<StudentMarks> students) {
        public MarkSheet { assessments = List.copyOf(assessments); students = List.copyOf(students); }
    }

    private static final BigDecimal MAX_MARK = new BigDecimal("100");

    private final AssessmentDao assessmentDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final AssessmentCalculator calculator;
    private final Clock clock;
    private final AttendanceService attendanceService;

    public MarksService(
            AssessmentDao assessmentDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            AssessmentCalculator calculator,
            Clock clock) {
        this(assessmentDao, courseDao, auditLogDao, transactions, authorization,
                calculator, clock, null);
    }

    public MarksService(
            AssessmentDao assessmentDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            AssessmentCalculator calculator,
            Clock clock,
            AttendanceService attendanceService) {
        this.assessmentDao = Objects.requireNonNull(assessmentDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.calculator = Objects.requireNonNull(calculator);
        this.clock = Objects.requireNonNull(clock);
        this.attendanceService = attendanceService;
    }

    public int save(AuthenticatedSession actor, int offeringId, List<MarkInput> entries) {
        authorization.requireLecturerForOffering(actor, offeringId);
        if (entries == null || entries.isEmpty() || entries.stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("At least one valid mark entry is required.");
        }
        int lecturerId = authorization.lecturerId(actor);
        List<AssessmentRow> assessments = assessmentDao.findAssessments(offeringId);
        Map<Integer, AssessmentRow> assessmentsById = assessments.stream()
                .collect(java.util.stream.Collectors.toMap(AssessmentRow::id, assessment -> assessment));
        Set<Integer> assessmentIds = assessmentsById.keySet();
        Map<Integer, CourseDao.EnrollmentRow> enrollmentsById = courseDao.findEnrollments(offeringId).stream()
                .collect(java.util.stream.Collectors.toMap(CourseDao.EnrollmentRow::id, enrollment -> enrollment));
        Set<Integer> enrollmentIds = enrollmentsById.keySet();
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
        Map<Integer, List<MarkInput>> marksByEnrollment = entries.stream()
                .collect(java.util.stream.Collectors.groupingBy(MarkInput::enrollmentId));
        for (MarkInput input : entries) {
            AssessmentRow assessment = assessmentsById.get(input.assessmentId());
            if ("FINAL".equalsIgnoreCase(assessment.type())) {
                CourseDao.EnrollmentRow enrollment = enrollmentsById.get(input.enrollmentId());
                requireFinalExamEligibility(actor, offeringId, enrollment,
                        assessments, marksByEnrollment.get(input.enrollmentId()));
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

    public List<CourseDao.OfferingSummary> offeringsForLecturer(AuthenticatedSession actor) {
        authorization.requireRole(actor, lk.ac.ruhuna.fot.ams.domain.enums.Role.LECTURER);
        return courseDao.findOfferingSummariesForLecturer(authorization.lecturerId(actor));
    }

    public MarkSheet sheet(AuthenticatedSession actor, int offeringId) {
        authorization.requireLecturerForOffering(actor, offeringId);
        List<AssessmentRow> assessments = assessmentDao.findAssessments(offeringId);
        Map<Integer, Map<Integer, MarkRow>> byEnrollment = new HashMap<>();
        for (MarkRow mark : assessmentDao.findMarks(offeringId, null)) {
            byEnrollment.computeIfAbsent(mark.enrollmentId(), ignored -> new HashMap<>())
                    .put(mark.assessmentId(), mark);
        }
        List<AssessmentRow> caAssessments = assessments.stream().filter(AssessmentRow::caComponent).toList();
        List<StudentMarks> students = new java.util.ArrayList<>();
        for (CourseDao.RosterRow roster : courseDao.findMarksRoster(offeringId)) {
            Map<Integer, MarkRow> marks = byEnrollment.getOrDefault(roster.enrollmentId(), Map.of());
            BigDecimal ca = null;
            if (!caAssessments.isEmpty() && caAssessments.stream().allMatch(row -> marks.containsKey(row.id()))) {
                ca = calculator.calculateCaMark(caAssessments.stream().map(row -> weightedMark(row, marks)).toList());
            }
            boolean finalExamEligible = isFinalExamEligible(actor, offeringId, roster.studentId(),
                    roster.enrollmentId(), ca);
            Map<Integer, BigDecimal> scores = new HashMap<>();
            boolean canViewFinalMarks = finalExamEligible;
            marks.forEach((assessmentId, mark) -> {
                AssessmentRow assessment = assessments.stream()
                        .filter(row -> row.id() == assessmentId).findFirst().orElse(null);
                if (assessment != null && (canViewFinalMarks || !"FINAL".equalsIgnoreCase(assessment.type()))) {
                    scores.put(assessmentId, mark.score());
                }
            });
            students.add(new StudentMarks(roster.enrollmentId(), roster.studentNumber(), roster.fullName(),
                    scores, ca, finalExamEligible));
        }
        return new MarkSheet(offeringId, assessments, students);
    }

    private void requireFinalExamEligibility(AuthenticatedSession actor, int offeringId,
            CourseDao.EnrollmentRow enrollment, List<AssessmentRow> assessments, List<MarkInput> changes) {
        if (attendanceService == null) {
            throw new BusinessRuleException("Final exam eligibility checks are unavailable.");
        }
        Map<Integer, BigDecimal> scores = new HashMap<>();
        assessmentDao.findMarks(offeringId, enrollment.id())
                .forEach(mark -> scores.put(mark.assessmentId(), mark.score()));
        changes.forEach(change -> scores.put(change.assessmentId(), change.score()));
        List<AssessmentRow> caAssessments = assessments.stream().filter(AssessmentRow::caComponent).toList();
        if (caAssessments.isEmpty() || caAssessments.stream().anyMatch(row -> !scores.containsKey(row.id()))) {
            throw new BusinessRuleException("Save all CA marks before entering a final exam mark.");
        }
        BigDecimal caMark = calculator.calculateCaMark(caAssessments.stream()
                .map(assessment -> new AssessmentCalculator.WeightedMark(scores.get(assessment.id()),
                        assessment.weight(), true))
                .toList());
        if (caMark.compareTo(new BigDecimal("40.00")) < 0) {
            throw new BusinessRuleException("A final exam mark cannot be entered when the CA mark is below 40%.");
        }
        if (!attendanceService.calculateForEnrollment(actor, enrollment.studentId(), enrollment.id(), offeringId)
                .eligible()) {
            throw new BusinessRuleException(
                    "A final exam mark cannot be entered when attendance is below 80% in any course component.");
        }
    }

    private boolean isFinalExamEligible(AuthenticatedSession actor, int offeringId,
            int studentId, int enrollmentId, BigDecimal caMark) {
        return attendanceService != null && caMark != null
                && caMark.compareTo(new BigDecimal("40.00")) >= 0
                && attendanceService.calculateForEnrollment(actor, studentId, enrollmentId, offeringId).eligible();
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
            throw new BusinessRuleException("Results are unavailable because a mark is missing for \""
                    + assessment.name() + "\".");
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
