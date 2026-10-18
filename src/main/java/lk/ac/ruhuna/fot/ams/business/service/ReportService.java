package lk.ac.ruhuna.fot.ams.business.service;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.MarkRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceHistoryRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ReportService {
    public record StudentAttendanceReport(
            int studentId, int enrollmentId, int offeringId, List<AttendanceHistoryRow> sessions) {
        public StudentAttendanceReport {
            sessions = List.copyOf(sessions);
        }
    }

    public record StudentMarksReport(
            int studentId, int enrollmentId, int offeringId,
            List<AssessmentRow> assessments, List<MarkRow> marks) {
        public StudentMarksReport {
            assessments = List.copyOf(assessments);
            marks = List.copyOf(marks);
        }
    }

    private final CourseDao courseDao;
    private final AttendanceDao attendanceDao;
    private final AssessmentDao assessmentDao;
    private final AuthorizationService authorization;

    public ReportService(
            CourseDao courseDao,
            AttendanceDao attendanceDao,
            AssessmentDao assessmentDao,
            AuthorizationService authorization) {
        this.courseDao = Objects.requireNonNull(courseDao);
        this.attendanceDao = Objects.requireNonNull(attendanceDao);
        this.assessmentDao = Objects.requireNonNull(assessmentDao);
        this.authorization = Objects.requireNonNull(authorization);
    }

    public StudentAttendanceReport studentAttendance(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.STUDENT_ATTENDANCE);
        requireOfferingScope(actor, offeringId, studentId);
        requireEnrollment(studentId, enrollmentId, offeringId);
        return new StudentAttendanceReport(studentId, enrollmentId, offeringId,
                attendanceDao.findHistory(enrollmentId, offeringId));
    }

    public List<StudentAttendanceReport> batchCourseAttendance(
            AuthenticatedSession actor, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.BATCH_COURSE_ATTENDANCE);
        requireOfferingScope(actor, offeringId, null);
        return courseDao.findEnrollments(offeringId).stream()
                .map(enrollment -> new StudentAttendanceReport(
                        enrollment.studentId(), enrollment.id(), offeringId,
                        attendanceDao.findHistory(enrollment.id(), offeringId)))
                .toList();
    }

    public StudentMarksReport studentMarks(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.CA_FINAL_MARKS);
        requireOfferingScope(actor, offeringId, studentId);
        requireEnrollment(studentId, enrollmentId, offeringId);
        return new StudentMarksReport(studentId, enrollmentId, offeringId,
                assessmentDao.findAssessments(offeringId),
                assessmentDao.findMarks(offeringId, enrollmentId));
    }

    private void requireOfferingScope(AuthenticatedSession actor, int offeringId, Integer studentId) {
        if (actor == null) {
            throw new AuthorizationException("You must sign in to access this report.");
        }
        if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
        } else if (actor.role() == Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        } else if (actor.role() == Role.UNDERGRADUATE) {
            if (studentId == null) {
                throw new AuthorizationException("Students may only request their own reports.");
            }
            authorization.requireUndergraduateForStudent(actor, studentId);
        } else {
            throw new AuthorizationException("You do not have permission for this report.");
        }
    }

    private void requireEnrollment(int studentId, int enrollmentId, int offeringId) {
        boolean matches = courseDao.findEnrollments(offeringId).stream()
                .anyMatch(enrollment -> enrollment.id() == enrollmentId
                        && enrollment.studentId() == studentId
                        && enrollment.offeringId() == offeringId);
        if (!matches) {
            throw new AuthorizationException("Report filters do not match an authorised enrolment.");
        }
    }
}
