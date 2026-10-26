package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.StudentResult;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator.CourseResult;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator.RepeatSelectionRule;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class GpaService {
    private final GpaCalculator calculator;
    private final RepeatSelectionRule repeatSelectionRule;
    private final CourseResultDao resultDao;
    private final CourseDao courseDao;
    private final AuthorizationService authorization;

    public GpaService(
            GpaCalculator calculator,
            RepeatSelectionRule repeatSelectionRule,
            CourseResultDao resultDao,
            CourseDao courseDao,
            AuthorizationService authorization) {
        this.calculator = Objects.requireNonNull(calculator);
        this.repeatSelectionRule = Objects.requireNonNull(repeatSelectionRule);
        this.resultDao = Objects.requireNonNull(resultDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.authorization = Objects.requireNonNull(authorization);
    }

    public BigDecimal calculateSgpa(AuthenticatedSession actor, int studentId, int semesterId) {
        authorizeStudentReport(actor, studentId, ReportType.SGPA_CGPA);
        if (semesterId <= 0) {
            throw new lk.ac.ruhuna.fot.ams.error.exception.ValidationException("Semester identifier is invalid.");
        }
        return calculator.calculateSgpa(toCourseResults(authorizeResultScope(actor,
                resultDao.findByStudentAndSemester(studentId, semesterId))));
    }

    public BigDecimal calculateCgpa(AuthenticatedSession actor, int studentId) {
        authorizeStudentReport(actor, studentId, ReportType.SGPA_CGPA);
        return calculator.calculateCgpa(toCourseResults(authorizeResultScope(actor,
                resultDao.findByStudent(studentId))), repeatSelectionRule);
    }

    public BigDecimal calculateOwnSgpa(AuthenticatedSession actor, int semesterId) {
        return calculateSgpa(actor, authorization.studentId(actor), semesterId);
    }

    public BigDecimal calculateOwnCgpa(AuthenticatedSession actor) {
        return calculateCgpa(actor, authorization.studentId(actor));
    }

    private void authorizeStudentReport(AuthenticatedSession actor, int studentId, ReportType reportType) {
        authorization.requireReportAccess(actor, reportType);
        if (actor.role() == Role.UNDERGRADUATE) {
            authorization.requireUndergraduateForStudent(actor, studentId);
        } else if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForStudent(actor, studentId);
        } else {
            throw new AuthorizationException("You do not have permission to view this GPA report.");
        }
    }

    /** A lecturer may see an aggregate only when every contributing result is from an assigned offering. */
    private List<StudentResult> authorizeResultScope(
            AuthenticatedSession actor, List<StudentResult> resultRows) {
        if (actor.role() == Role.LECTURER) {
            for (StudentResult result : resultRows) {
                int offeringId = courseDao.findEnrollment(result.enrollmentId())
                        .orElseThrow(() -> new AuthorizationException("Result enrolment is unavailable."))
                        .offeringId();
                authorization.requireLecturerForOffering(actor, offeringId);
            }
        }
        return resultRows;
    }

    private Collection<CourseResult> toCourseResults(List<StudentResult> results) {
        return results.stream().map(result -> new CourseResult(
                result.courseCode(), result.creditsCounted(), result.gradePoint(),
                true, result.attemptNumber(), result.completedAt().toLocalDate())).toList();
    }
}
