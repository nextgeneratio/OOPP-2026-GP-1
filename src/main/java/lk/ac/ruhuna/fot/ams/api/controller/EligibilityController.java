package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy.Result;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class EligibilityController {
    public record EligibilityRequest(int studentId, int enrollmentId, int offeringId) {
    }

    public record OwnEligibilityRequest(int enrollmentId, int offeringId) { }

    public record EligibilityResponse(
            boolean eligible,
            boolean caEligible,
            boolean attendanceEligible,
            java.util.List<String> failedConditions) {
        public EligibilityResponse {
            failedConditions = java.util.List.copyOf(failedConditions);
        }

        public static EligibilityResponse from(Result result) {
            return new EligibilityResponse(result.eligible(), result.caEligible(),
                    result.attendanceEligible(), result.failedConditions());
        }
    }

    private final EligibilityService service;
    private final ApiControllerSupport support;

    public EligibilityController(EligibilityService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<EligibilityResponse> evaluate(
            AuthenticatedSession actor, EligibilityRequest request) {
        return support.execute(request, () -> EligibilityResponse.from(service.evaluateForEnrollment(
                actor, request.studentId(), request.enrollmentId(), request.offeringId())));
    }

    public ApiResponse<java.util.List<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary>> myOfferings(
            AuthenticatedSession actor) {
        return support.execute(() -> service.offeringsForLecturer(actor));
    }

    public ApiResponse<java.util.List<lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow>> roster(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.rosterForOffering(actor, offeringId));
    }

    public ApiResponse<java.util.List<EligibilityService.StudentOffering>> myOfferingsAsStudent(
            AuthenticatedSession actor) {
        return support.execute(() -> service.offeringsForStudent(actor));
    }

    public ApiResponse<EligibilityResponse> evaluateOwn(
            AuthenticatedSession actor, OwnEligibilityRequest request) {
        return support.execute(request, () -> EligibilityResponse.from(service.evaluateForOwnEnrollment(
                actor, request.enrollmentId(), request.offeringId())));
    }
}
