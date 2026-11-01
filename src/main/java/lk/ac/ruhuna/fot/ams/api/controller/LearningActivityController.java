package lk.ac.ruhuna.fot.ams.api.controller;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionDraft;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuestionView;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuizAttemptView;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService.QuizView;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.AssignmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.SubmissionRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LearningActivityController {
    public record CreateQuizRequest(int offeringId, String title, char[] password,
                                   LocalDateTime opensAt, LocalDateTime closesAt,
                                   List<QuestionDraft> questions) {
    }

    public record CreateAssignmentRequest(int offeringId, String title, String description,
                                          LocalDateTime dueAt, boolean visible) {
    }

    public record VisibilityRequest(int offeringId, int itemId, boolean visible) {
    }

    public record AttemptRequest(int quizId, char[] password, Map<Integer, Integer> answers) {
    }

    public record QuestionRequest(int quizId, char[] password) {
    }

    public record SubmitRequest(int assignmentId, Path file) {
    }

    public record IdResponse(long id) {
    }

    private final LearningActivityService service;
    private final ApiControllerSupport support;

    public LearningActivityController(LearningActivityService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<IdResponse> createQuiz(AuthenticatedSession actor, CreateQuizRequest request) {
        return support.execute(request, () -> new IdResponse(service.createQuiz(actor, request.offeringId(),
                request.title(), request.password(), request.opensAt(), request.closesAt(), request.questions())));
    }

    public ApiResponse<Void> setQuizVisibility(AuthenticatedSession actor, VisibilityRequest request) {
        return support.execute(request, () -> {
            service.setQuizVisibility(actor, request.offeringId(), request.itemId(), request.visible());
            return null;
        });
    }

    public ApiResponse<List<QuizView>> quizzesForLecturer(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.quizzesForLecturer(actor, offeringId));
    }

    public ApiResponse<List<QuizView>> quizzesForStudent(AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.quizzesForStudent(actor, offeringId));
    }

    public ApiResponse<List<QuestionView>> questionsForStudent(
            AuthenticatedSession actor, QuestionRequest request) {
        return support.execute(request, () ->
                service.questionsForStudent(actor, request.quizId(), request.password()));
    }

    public ApiResponse<QuizAttemptView> attemptQuiz(AuthenticatedSession actor, AttemptRequest request) {
        return support.execute(request, () -> service.attemptQuiz(
                actor, request.quizId(), request.password(), request.answers()));
    }

    public ApiResponse<IdResponse> createAssignment(
            AuthenticatedSession actor, CreateAssignmentRequest request) {
        return support.execute(request, () -> new IdResponse(service.createAssignment(actor,
                request.offeringId(), request.title(), request.description(), request.dueAt(), request.visible())));
    }

    public ApiResponse<Void> setAssignmentVisibility(AuthenticatedSession actor, VisibilityRequest request) {
        return support.execute(request, () -> {
            service.setAssignmentVisibility(actor, request.offeringId(), request.itemId(), request.visible());
            return null;
        });
    }

    public ApiResponse<List<AssignmentRow>> assignmentsForLecturer(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.assignmentsForLecturer(actor, offeringId));
    }

    public ApiResponse<List<AssignmentRow>> assignmentsForStudent(
            AuthenticatedSession actor, int offeringId) {
        return support.execute(() -> service.assignmentsForStudent(actor, offeringId));
    }

    public ApiResponse<SubmissionRow> submitAssignment(AuthenticatedSession actor, SubmitRequest request) {
        return support.execute(request, () -> service.submitAssignment(actor, request.assignmentId(), request.file()));
    }
}
