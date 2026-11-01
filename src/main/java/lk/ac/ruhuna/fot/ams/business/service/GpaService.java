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
        authorizeOwnStudentReport(actor, studentId);
        if (semesterId <= 0) {
            throw new lk.ac.ruhuna.fot.ams.error.exception.ValidationException("Semester identifier is invalid.");
        }
        return calculator.calculateSgpa(toCourseResults(resultDao.findByStudentAndSemester(studentId, semesterId)));
    }

    public BigDecimal calculateCgpa(AuthenticatedSession actor, int studentId) {
        authorizeOwnStudentReport(actor, studentId);
        return calculator.calculateCgpa(toCourseResults(resultDao.findByStudent(studentId)), repeatSelectionRule);
    }

    public BigDecimal calculateOwnSgpa(AuthenticatedSession actor, int semesterId) {
        return calculateSgpa(actor, authorization.studentId(actor), semesterId);
    }

    public BigDecimal calculateOwnCgpa(AuthenticatedSession actor) {
        return calculateCgpa(actor, authorization.studentId(actor));
    }

    /**
     * The authorised offering establishes a Lecturer's relationship to the student.
     * Only the aggregate is returned; detailed results remain offering-scoped elsewhere.
     */
    public BigDecimal calculateLecturerSgpa(
            AuthenticatedSession actor, int studentId, int authorisedOfferingId, int semesterId) {
        authorizeLecturerAggregate(actor, studentId, authorisedOfferingId);
        if (semesterId <= 0) {
            throw new lk.ac.ruhuna.fot.ams.error.exception.ValidationException("Semester identifier is invalid.");
        }
        return calculator.calculateSgpa(toCourseResults(resultDao.findByStudentAndSemester(studentId, semesterId)));
    }

    public BigDecimal calculateLecturerCgpa(
            AuthenticatedSession actor, int studentId, int authorisedOfferingId) {
        authorizeLecturerAggregate(actor, studentId, authorisedOfferingId);
        return calculator.calculateCgpa(toCourseResults(resultDao.findByStudent(studentId)), repeatSelectionRule);
    }

    private void authorizeOwnStudentReport(AuthenticatedSession actor, int studentId) {
        authorization.requireReportAccess(actor, ReportType.SGPA_CGPA);
        authorization.requireUndergraduateForStudent(actor, studentId);
    }

    private void authorizeLecturerAggregate(
            AuthenticatedSession actor, int studentId, int authorisedOfferingId) {
        authorization.requireReportAccess(actor, ReportType.SGPA_CGPA);
        authorization.requireLecturerForOffering(actor, authorisedOfferingId);
        boolean enrolled = courseDao.findEnrollments(authorisedOfferingId).stream()
                .anyMatch(enrollment -> enrollment.studentId() == studentId);
        if (!enrolled) {
            throw new AuthorizationException("Student is not enrolled in the selected authorised offering.");
        }
    }

    private Collection<CourseResult> toCourseResults(List<StudentResult> results) {
        return results.stream().map(result -> new CourseResult(
                result.courseCode(), result.creditsCounted(), result.gradePoint(),
                true, result.attemptNumber(), result.completedAt().toLocalDate())).toList();
    }
}
