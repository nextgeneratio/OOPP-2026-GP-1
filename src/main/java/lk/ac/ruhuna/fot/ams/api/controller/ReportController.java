package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.ReportService;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentMarksReport;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ReportController {
    public record StudentReportRequest(int studentId, int enrollmentId, int offeringId) {
    }

    public record OfferingReportRequest(int offeringId) {
    }

    private final ReportService service;
    private final ApiControllerSupport support;

    public ReportController(ReportService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<StudentAttendanceReport> studentAttendance(
            AuthenticatedSession actor, StudentReportRequest request) {
        return support.execute(request, () -> service.studentAttendance(actor, request.studentId(),
                request.enrollmentId(), request.offeringId()));
    }

    public ApiResponse<List<StudentAttendanceReport>> batchCourseAttendance(
            AuthenticatedSession actor, OfferingReportRequest request) {
        return support.execute(request, () -> service.batchCourseAttendance(actor, request.offeringId()));
    }

    public ApiResponse<StudentMarksReport> studentMarks(
            AuthenticatedSession actor, StudentReportRequest request) {
        return support.execute(request, () -> service.studentMarks(actor, request.studentId(),
                request.enrollmentId(), request.offeringId()));
    }
}
