package lk.ac.ruhuna.fot.ams.api.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.AcademicResultService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.ResultSnapshot;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class ResultController {
    public record FinalizeResultRequest(int enrollmentId) {
    }

    public record FinalizedResultResponse(
            int enrollmentId,
            BigDecimal finalMark,
            String schemeVersion,
            String grade,
            BigDecimal gradePoint,
            BigDecimal creditsCounted,
            LocalDateTime completedAt) {
        public static FinalizedResultResponse from(ResultSnapshot result) {
            return new FinalizedResultResponse(result.enrollmentId(), result.finalMark(),
                    result.schemeVersion(), result.gradeLetter(), result.gradePoint(),
                    result.creditsCounted(), result.completedAt());
        }
    }

    private final AcademicResultService service;
    private final ApiControllerSupport support;

    public ResultController(AcademicResultService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<FinalizedResultResponse> finalizeAttempt(
            AuthenticatedSession actor, FinalizeResultRequest request) {
        return support.execute(request, () -> FinalizedResultResponse.from(
                service.finalizeAttempt(actor, request.enrollmentId())));
    }
}
