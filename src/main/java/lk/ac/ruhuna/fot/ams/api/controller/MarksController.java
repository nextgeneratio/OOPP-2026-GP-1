package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.MarksService;
import lk.ac.ruhuna.fot.ams.business.service.MarksService.MarkInput;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class MarksController {
    public record SaveMarksRequest(int offeringId, List<MarkInput> marks) {
    }

    public record SaveMarksResponse(int savedCount) {
    }

    private final MarksService service;
    private final ApiControllerSupport support;

    public MarksController(MarksService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<SaveMarksResponse> saveMarks(
            AuthenticatedSession actor, SaveMarksRequest request) {
        return support.execute(request, () -> new SaveMarksResponse(
                service.save(actor, request.offeringId(), request.marks())));
    }
}
