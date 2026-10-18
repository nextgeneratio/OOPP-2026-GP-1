package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.MedicalService;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class MedicalController {
    public record DecideMedicalRequest(int medicalId, ApprovalStatus decision) {
    }

    public record MedicalSearchRequest(int studentId, Integer offeringId) {
    }

    public record MedicalIdResponse(long medicalId) {
    }

    private final MedicalService service;
    private final ApiControllerSupport support;

    public MedicalController(MedicalService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<MedicalIdResponse> submit(AuthenticatedSession actor, MedicalRow request) {
        return support.execute(request, () -> new MedicalIdResponse(service.submit(actor, request)));
    }

    public ApiResponse<Void> decide(AuthenticatedSession actor, DecideMedicalRequest request) {
        return support.execute(request, () -> {
            service.decide(actor, request.medicalId(), request.decision());
            return null;
        });
    }

    public ApiResponse<List<MedicalRow>> findForStudent(
            AuthenticatedSession actor, MedicalSearchRequest request) {
        return support.execute(request, () -> service.findForStudent(
                actor, request.studentId(), request.offeringId()));
    }
}
