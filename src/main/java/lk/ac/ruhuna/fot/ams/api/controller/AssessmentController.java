package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.api.request.CalculateGradeRequest;
import lk.ac.ruhuna.fot.ams.api.response.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.response.CalculateGradeResponse;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;

public final class AssessmentController {
    private final AssessmentService assessmentService;
    private final ApplicationErrorHandler errorHandler;

    public AssessmentController(AssessmentService assessmentService) {
        this(assessmentService, new ApplicationErrorHandler());
    }

    public AssessmentController(AssessmentService assessmentService, ApplicationErrorHandler errorHandler) {
        this.assessmentService = Objects.requireNonNull(assessmentService, "AssessmentService must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "ApplicationErrorHandler must not be null");
    }

    public ApiResponse<CalculateGradeResponse> calculateGrade(CalculateGradeRequest request) {
        if (request == null) {
            return ApiResponse.error("Grade calculation request cannot be null.");
        }
        if (request.offeringId() == null || request.offeringId() <= 0) {
            return ApiResponse.error("Valid course offering ID is required.");
        }
        if (request.studentScores() == null || request.studentScores().isEmpty()) {
            return ApiResponse.error("Student assessment scores map cannot be empty.");
        }

        try {
            GradeScheme grade = assessmentService.calculateStudentFinalGrade(
                    request.offeringId(),
                    request.studentScores()
            );
            return ApiResponse.success(new CalculateGradeResponse(
                    request.offeringId(),
                    grade.letter(),
                    grade.gradePoint()
            ));
        } catch (Exception failure) {
            errorHandler.log(failure);
            return ApiResponse.error(errorHandler.userMessage(failure));
        }
    }
}
