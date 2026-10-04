package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.GradeCalculator.GradeResult;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.ResultSnapshot;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AcademicResultService {
    private final CourseDao courseDao;
    private final CourseResultDao resultDao;
    private final MarksService marksService;
    private final EligibilityService eligibilityService;
    private final GradeService gradeService;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;
    private final String gradeSchemeVersion;

    public AcademicResultService(
            CourseDao courseDao,
            CourseResultDao resultDao,
            MarksService marksService,
            EligibilityService eligibilityService,
            GradeService gradeService,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock,
            String gradeSchemeVersion) {
        this.courseDao = Objects.requireNonNull(courseDao);
        this.resultDao = Objects.requireNonNull(resultDao);
        this.marksService = Objects.requireNonNull(marksService);
        this.eligibilityService = Objects.requireNonNull(eligibilityService);
        this.gradeService = Objects.requireNonNull(gradeService);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
        if (gradeSchemeVersion == null || gradeSchemeVersion.isBlank()) {
            throw new IllegalArgumentException("Grade scheme version is required.");
        }
        this.gradeSchemeVersion = gradeSchemeVersion;
    }

    public ResultSnapshot finalizeAttempt(AuthenticatedSession actor, int enrollmentId) {
        if (actor == null || actor.role() != Role.LECTURER) {
            throw new AuthorizationException("Only an assigned lecturer may finalize course results.");
        }
        CourseDao.EnrollmentRow enrollment = courseDao.findEnrollment(enrollmentId)
                .orElseThrow(() -> new NotFoundException("Course enrolment was not found."));
        if ("WITHDRAWN".equals(enrollment.enrollmentStatus())) {
            throw new BusinessRuleException("Withdrawn enrolments cannot be finalized.");
        }
        authorization.requireLecturerForOffering(actor, enrollment.offeringId());
        var eligibility = eligibilityService.evaluateForEnrollment(
                actor, enrollment.studentId(), enrollment.id(), enrollment.offeringId());
        if (!eligibility.eligible()) {
            throw new BusinessRuleException(
                    "Course result cannot be finalized: " + String.join("; ", eligibility.failedConditions()));
        }
        BigDecimal finalMark = marksService.calculateFinalMark(
                actor, enrollment.offeringId(), enrollment.id());
        GradeResult grade = gradeService.grade(gradeSchemeVersion, finalMark);
        CourseDao.OfferingRow offering = courseDao.findOffering(enrollment.offeringId())
                .orElseThrow(() -> new NotFoundException("Course offering was not found."));
        CourseDao.CourseRow course = courseDao.findCourse(offering.courseId())
                .orElseThrow(() -> new NotFoundException("Course was not found."));
        ResultSnapshot previous = resultDao.findByEnrollment(enrollmentId).orElse(null);
        ResultSnapshot snapshot = new ResultSnapshot(enrollmentId, finalMark, grade.schemeVersion(),
                grade.grade(), grade.gradePoint(), BigDecimal.valueOf(course.credits()),
                previous == null ? LocalDateTime.now(clock) : previous.completedAt(),
                authorization.lecturerId(actor));

        return transactions.execute(connection -> {
            if (previous == null) {
                resultDao.insert(connection, snapshot);
            } else {
                resultDao.archiveCurrent(connection, enrollmentId);
                resultDao.insert(connection, snapshot);
            }
            if (!"COMPLETED".equals(enrollment.enrollmentStatus())) {
                courseDao.updateEnrollmentStatus(connection, enrollmentId, "COMPLETED");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), previous == null ? "RESULT_FINALIZE" : "RESULT_REVISE",
                    "COURSE_RESULT", enrollmentId, LocalDateTime.now(clock)));
            return snapshot;
        });
    }
}
