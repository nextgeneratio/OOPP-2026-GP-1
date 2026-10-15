package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.api.request.EligibilityEvaluationRequest;
import lk.ac.ruhuna.fot.ams.api.response.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.response.EligibilityEvaluationResponse;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;

public final class EligibilityController {
    private final EligibilityService eligibilityService;
    private final ApplicationErrorHandler errorHandler;

    public EligibilityController(EligibilityService eligibilityService) {
        this(eligibilityService, new ApplicationErrorHandler());
    }

    public EligibilityController(EligibilityService eligibilityService, ApplicationErrorHandler errorHandler) {
        this.eligibilityService = Objects.requireNonNull(eligibilityService, "EligibilityService must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "ApplicationErrorHandler must not be null");
    }

    public ApiResponse<EligibilityEvaluationResponse> evaluateEligibility(EligibilityEvaluationRequest request) {
        if (request == null) {
            return ApiResponse.error("Request payload cannot be null.");
        }
        if (request.caMark() == null) {
            return ApiResponse.error("CA mark is required.");
        }
        if (request.components() == null || request.components().isEmpty()) {
            return ApiResponse.error("At least one course component attendance must be provided.");
        }

        List<EligibilityService.AttendanceComponentInput> inputs = new ArrayList<>();
        for (var comp : request.components()) {
            if (comp.attendedSessions() == null || comp.scheduledSessions() == null) {
                return ApiResponse.error("Attended and scheduled session counts are required for each component.");
            }
            int medical = comp.approvedMedicalSessions() != null ? comp.approvedMedicalSessions() : 0;
            inputs.add(new EligibilityService.AttendanceComponentInput(
                    comp.attendedSessions(),
                    comp.scheduledSessions(),
                    medical
            ));
        }

        try {
            var serviceResult = eligibilityService.evaluateCourseEligibility(inputs, request.caMark());
            var response = new EligibilityEvaluationResponse(
                    serviceResult.attendanceEligible(),
                    serviceResult.caEligible(),
                    serviceResult.finalExamEligible(),
                    serviceResult.overallAttendancePercentage(),
                    serviceResult.caMark(),
                    serviceResult.failedReasons()
            );
            return ApiResponse.success(response);
        } catch (Exception failure) {
            errorHandler.log(failure);
            return ApiResponse.error(errorHandler.userMessage(failure));
        }
    }
}
