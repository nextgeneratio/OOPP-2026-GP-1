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

    public ApiResponse<GpaResponse> calculateOwnSgpa(AuthenticatedSession actor, int semesterId) {
        return support.execute(semesterId, () -> new GpaResponse(service.calculateOwnSgpa(actor, semesterId)));
    }

    public ApiResponse<GpaResponse> calculateOwnCgpa(AuthenticatedSession actor) {
        return support.execute(() -> new GpaResponse(service.calculateOwnCgpa(actor)));
    }

    public ApiResponse<GpaResponse> calculateLecturerSgpa(
            AuthenticatedSession actor, int studentId, int offeringId, int semesterId) {
        return support.execute(offeringId, () -> new GpaResponse(
                service.calculateLecturerSgpa(actor, studentId, offeringId, semesterId)));
    }

    public ApiResponse<GpaResponse> calculateLecturerCgpa(
            AuthenticatedSession actor, int studentId, int offeringId) {
        return support.execute(offeringId, () -> new GpaResponse(
                service.calculateLecturerCgpa(actor, studentId, offeringId)));
    }
}
