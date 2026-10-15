package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.api.request.EnrollStudentRequest;
import lk.ac.ruhuna.fot.ams.api.response.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.response.EnrollStudentResponse;
import lk.ac.ruhuna.fot.ams.business.service.EnrollmentService;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;

public final class EnrollmentController {
    private final EnrollmentService enrollmentService;
    private final ApplicationErrorHandler errorHandler;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this(enrollmentService, new ApplicationErrorHandler());
    }

    public EnrollmentController(EnrollmentService enrollmentService, ApplicationErrorHandler errorHandler) {
        this.enrollmentService = Objects.requireNonNull(enrollmentService, "EnrollmentService must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "ApplicationErrorHandler must not be null");
    }

    public ApiResponse<EnrollStudentResponse> enroll(EnrollStudentRequest request) {
        if (request == null) {
            return ApiResponse.error("Enrollment request cannot be null.");
        }
        if (request.studentId() == null || request.studentId() <= 0) {
            return ApiResponse.error("Valid student ID is required.");
        }
        if (request.offeringId() == null || request.offeringId() <= 0) {
            return ApiResponse.error("Valid course offering ID is required.");
        }
        if (request.studentStatus() == null) {
            return ApiResponse.error("Student status is required.");
        }

        try {
            var serviceResult = enrollmentService.enrollStudent(
                    request.studentId(),
                    request.offeringId(),
                    request.studentStatus()
            );
            var response = new EnrollStudentResponse(
                    serviceResult.studentId(),
                    serviceResult.offeringId(),
                    serviceResult.attemptNo(),
                    serviceResult.studentStatus(),
                    serviceResult.enrollmentStatus()
            );
            return ApiResponse.success(response);
        } catch (Exception failure) {
            errorHandler.log(failure);
            return ApiResponse.error(errorHandler.userMessage(failure));
        }
    }
}
