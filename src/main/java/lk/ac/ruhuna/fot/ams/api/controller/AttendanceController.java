package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AttendanceController {
    public record SaveAttendanceRequest(SessionRow session, Map<Integer, Boolean> attendanceByEnrollment) {
    }

    public record EnrollmentAttendanceRequest(int studentId, int enrollmentId, int offeringId) {
    }

    public record SavedSessionResponse(long sessionId) {
    }

    private final AttendanceService service;
    private final ApiControllerSupport support;

    public AttendanceController(AttendanceService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
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
}
