package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AssessmentController {
    public record PublishPlanRequest(int offeringId, List<AssessmentRow> assessments) {
    }

    public record PublishedPlanResponse(List<Long> assessmentIds) {
        public PublishedPlanResponse {
            assessmentIds = List.copyOf(assessmentIds);
        }
    }

    private final AssessmentService service;
    private final ApiControllerSupport support;

    public AssessmentController(AssessmentService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<PublishedPlanResponse> publishPlan(
            AuthenticatedSession actor, PublishPlanRequest request) {
        return support.execute(request, () -> new PublishedPlanResponse(
                service.publishPlan(actor, request.offeringId(), request.assessments())));
    }
}
