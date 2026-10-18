package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class EligibilityService {
    /** One of a student's own course offerings, for their eligibility selector. */
    public record StudentOffering(int enrollmentId, int offeringId, String courseCode, String courseTitle) {
    }

    private final FinalExamEligibilityPolicy policy;
    private final MarksService marksService;
    private final AttendanceService attendanceService;
    private final AuthorizationService authorization;
    private final CourseDao courseDao;

    public EligibilityService(
            FinalExamEligibilityPolicy policy,
            MarksService marksService,
            AttendanceService attendanceService,
            AuthorizationService authorization,
            CourseDao courseDao) {
        this.policy = Objects.requireNonNull(policy);
        this.marksService = Objects.requireNonNull(marksService);
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.authorization = Objects.requireNonNull(authorization);
        this.courseDao = Objects.requireNonNull(courseDao);
    }

    public List<CourseDao.OfferingSummary> offeringsForLecturer(AuthenticatedSession actor) {
        authorization.requireRole(actor, Role.LECTURER);
        return courseDao.findOfferingSummariesForLecturer(authorization.lecturerId(actor));
    }

    public List<CourseDao.RosterRow> rosterForOffering(AuthenticatedSession actor, int offeringId) {
        authorization.requireLecturerForOffering(actor, offeringId);
        return courseDao.findRoster(offeringId);
    }

    public List<StudentOffering> offeringsForStudent(AuthenticatedSession actor) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        int studentId = authorization.studentId(actor);
        return courseDao.findEnrollmentsForStudent(studentId).stream()
                .map(enrollment -> {
                    CourseDao.OfferingRow offering = courseDao.findOffering(enrollment.offeringId())
                            .orElseThrow(() -> new lk.ac.ruhuna.fot.ams.error.exception.ValidationException(
                                    "Course offering was not found."));
                    CourseDao.CourseRow course = courseDao.findCourse(offering.courseId())
                            .orElseThrow(() -> new lk.ac.ruhuna.fot.ams.error.exception.ValidationException(
                                    "Course was not found."));
                    return new StudentOffering(enrollment.id(), enrollment.offeringId(),
                            course.code(), course.title());
                })
                .toList();
    }

    public FinalExamEligibilityPolicy.Result evaluate(BigDecimal caMark, AttendanceSummary attendance) {
        return policy.evaluate(new FinalExamEligibilityPolicy.Input(caMark, attendance));
    }

    /** For the signed-in undergraduate checking their own eligibility; the student id is never client-supplied. */
    public FinalExamEligibilityPolicy.Result evaluateForOwnEnrollment(
            AuthenticatedSession actor, int enrollmentId, int offeringId) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        int studentId = authorization.studentId(actor);
        return evaluateForEnrollment(actor, studentId, enrollmentId, offeringId);
    }

    public FinalExamEligibilityPolicy.Result evaluateForEnrollment(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.ELIGIBILITY);
        BigDecimal caMark = marksService.calculateCaMark(actor, offeringId, enrollmentId);
        AttendanceSummary attendance = attendanceService.calculateForEnrollment(
                actor, studentId, enrollmentId, offeringId);
        return evaluate(caMark, attendance);
    }
}
