package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public final class EligibilityService {
    public record StudentOffering(int enrollmentId, int offeringId, String courseCode, String courseTitle,
            String academicYear, int semesterNumber) {
        public StudentOffering(int enrollmentId, int offeringId, String courseCode, String courseTitle) {
            this(enrollmentId, offeringId, courseCode, courseTitle, "", 0);
        }
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
        java.util.Map<Integer, CourseDao.SemesterRow> terms = courseDao.findSemesters().stream()
                .collect(java.util.stream.Collectors.toMap(CourseDao.SemesterRow::id, term -> term));
        return courseDao.findEnrollmentsForStudent(studentId).stream().map(enrollment -> {
            CourseDao.OfferingRow offering = courseDao.findOffering(enrollment.offeringId())
                    .orElseThrow(() -> new lk.ac.ruhuna.fot.ams.error.exception.ValidationException(
                            "Course offering was not found."));
            CourseDao.CourseRow course = courseDao.findCourse(offering.courseId())
                    .orElseThrow(() -> new lk.ac.ruhuna.fot.ams.error.exception.ValidationException(
                            "Course was not found."));
            CourseDao.SemesterRow term = terms.get(offering.semesterId());
            return new StudentOffering(enrollment.id(), enrollment.offeringId(), course.code(), course.title(),
                    term == null ? "" : term.academicYear(), term == null ? 0 : term.semesterNumber());
        }).toList();
    }

    public FinalExamEligibilityPolicy.Result evaluateForOwnEnrollment(
            AuthenticatedSession actor, int enrollmentId, int offeringId) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        int studentId = authorization.studentId(actor);
        return evaluateForEnrollment(actor, studentId, enrollmentId, offeringId);
    }

    public FinalExamEligibilityPolicy.Result evaluate(BigDecimal caMark, AttendanceSummary attendance) {
        return policy.evaluate(new FinalExamEligibilityPolicy.Input(caMark, attendance));
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
