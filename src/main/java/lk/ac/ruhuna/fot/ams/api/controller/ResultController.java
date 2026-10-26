package lk.ac.ruhuna.fot.ams.api.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.service.AcademicResultService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.ResultSnapshot;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
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

    public record StudentResultResponse(String courseCode, int semesterId, int attemptNumber,
            BigDecimal credits, BigDecimal gradePoint, LocalDateTime completedAt) {
        private static StudentResultResponse from(
                lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.StudentResult result) {
            return new StudentResultResponse(result.courseCode(), result.semesterId(), result.attemptNumber(),
                    result.creditsCounted(), result.gradePoint(), result.completedAt());
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

    public ApiResponse<List<StudentResultResponse>> studentResults(
            AuthenticatedSession actor, int studentId) {
        return support.execute(studentId, () -> service.resultsForStudent(actor, studentId).stream()
                .map(StudentResultResponse::from).toList());
    }

    public ApiResponse<List<StudentResultResponse>> studentResultsForOffering(
            AuthenticatedSession actor, int studentId, int offeringId) {
        return support.execute(offeringId, () -> service.resultsForStudentInOffering(actor, studentId, offeringId)
                .stream().map(StudentResultResponse::from).toList());
    }

    public ApiResponse<List<StudentResultResponse>> currentStudentResults(AuthenticatedSession actor) {
        return support.execute(() -> service.resultsForCurrentStudent(actor).stream()
                .map(StudentResultResponse::from).toList());
    }

    public ApiResponse<List<EnrollmentRow>> resultEnrollmentsForOffering(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(offeringId, () -> service.resultEnrollmentsForOffering(actor, offeringId));
    }
}
