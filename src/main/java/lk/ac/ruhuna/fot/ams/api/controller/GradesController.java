package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.GradesService;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.CourseResultView;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.GpaReport;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.SemesterOption;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class GradesController {
    /** studentId is required for lecturers and ignored for undergraduates, who only ever see their own record. */
    public record StudentScopeRequest(Integer studentId) {
    }

    public record GpaReportRequest(Integer studentId, Integer semesterId) {
    }

    private final GradesService service;
    private final ApiControllerSupport support;

    public GradesController(GradesService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<List<CourseResultView>> courseResults(AuthenticatedSession actor, StudentScopeRequest request) {
        return support.execute(request, () -> service.courseResults(actor, request.studentId()));
    }

    public ApiResponse<List<SemesterOption>> semesters(AuthenticatedSession actor, StudentScopeRequest request) {
        return support.execute(request, () -> service.semesters(actor, request.studentId()));
    }

    public ApiResponse<GpaReport> gpaReport(AuthenticatedSession actor, GpaReportRequest request) {
        return support.execute(request, () -> service.gpaReport(actor, request.studentId(), request.semesterId()));
    }
}
