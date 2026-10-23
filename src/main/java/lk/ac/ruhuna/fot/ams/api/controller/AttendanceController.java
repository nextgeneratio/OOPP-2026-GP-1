package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;

public final class AttendanceController {
    public record SaveAttendanceRequest(SessionRow session, Map<Integer, Boolean> attendanceByEnrollment) {
    }

    public record EnrollmentAttendanceRequest(int studentId, int enrollmentId, int offeringId) {
    }

    public record ReviseAttendanceRequest(AttendanceRow record) {
    }

    public record SavedSessionResponse(long sessionId) {
    }

    private final AttendanceService service;
    private final ApiControllerSupport support;

    public AttendanceController(AttendanceService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<java.util.List<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary>> myOfferings(
            AuthenticatedSession actor) {
        return support.execute(() -> service.offeringsForTechnicalOfficer(actor));
    }

    public ApiResponse<AttendanceService.SessionSetup> sessionSetup(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.sessionSetup(actor, offeringId));
    }

    public ApiResponse<SavedSessionResponse> saveSession(
            AuthenticatedSession actor, SaveAttendanceRequest request) {
        return support.execute(request, () -> new SavedSessionResponse(service.saveSession(
                actor, request.session(), request.attendanceByEnrollment())));
    }

    public ApiResponse<AttendanceSummary> calculateForEnrollment(
            AuthenticatedSession actor, EnrollmentAttendanceRequest request) {
        return support.execute(request, () -> service.calculateForEnrollment(actor, request.studentId(),
                request.enrollmentId(), request.offeringId()));
    }

    public ApiResponse<Void> reviseRecord(AuthenticatedSession actor, ReviseAttendanceRequest request) {
        return support.execute(request, () -> {
            service.reviseRecord(actor, request.record());
            return null;
        });
    }

    public ApiResponse<Void> deleteSession(AuthenticatedSession actor, int sessionId) {
        return support.execute(sessionId, () -> {
            service.deleteSession(actor, sessionId);
            return null;
        });
    }

    public ApiResponse<List<SessionRow>> sessionsForOffering(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.sessionsForOffering(actor, offeringId));
    }

    public ApiResponse<List<EnrollmentRow>> activeEnrollments(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.activeEnrollments(actor, offeringId));
    }

    public ApiResponse<AttendanceSummary> calculateForCurrentStudent(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.calculateForCurrentStudent(actor, offeringId));
    }
}
