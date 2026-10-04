package lk.ac.ruhuna.fot.ams.api.controller;

import java.math.BigDecimal;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.GpaService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class GpaController {
    public record GpaRequest(int studentId, Integer semesterId) {
    }

    public record GpaResponse(BigDecimal value) {
    }

    private final GpaService service;
    private final ApiControllerSupport support;

    public GpaController(GpaService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<GpaResponse> calculateSgpa(
            AuthenticatedSession actor, GpaRequest request) {
        return support.execute(request, () -> new GpaResponse(
                service.calculateSgpa(actor, request.studentId(), request.semesterId())));
    }

    public ApiResponse<GpaResponse> calculateCgpa(
            AuthenticatedSession actor, GpaRequest request) {
        return support.execute(request, () -> new GpaResponse(
                service.calculateCgpa(actor, request.studentId())));
    }
}
