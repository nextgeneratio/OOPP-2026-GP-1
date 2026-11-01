package lk.ac.ruhuna.fot.ams.business.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.AssignmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.QuestionRow;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.QuizRow;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.SubmissionRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LearningActivityService {
    public record QuestionDraft(String prompt, List<String> options, int correctOption) {
        public QuestionDraft {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    public record QuizView(int id, int offeringId, String title,
                           LocalDateTime opensAt, LocalDateTime closesAt) {
    }

    public record QuestionView(int id, String prompt, List<String> options) {
        public QuestionView {
            options = List.copyOf(options);
        }
    }

    public record QuizAttemptView(int score, int total) {
    }

    private static final long MAX_UPLOAD_BYTES = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");

    private final LearningActivityDao learningDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public LearningActivityService(LearningActivityDao learningDao, CourseDao courseDao,
                                   AuditLogDao auditDao, TransactionManager transactions,
                                   AuthorizationService authorization, PasswordHasher passwordHasher,
                                   Clock clock) {
        this.learningDao = Objects.requireNonNull(learningDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditDao = Objects.requireNonNull(auditDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.clock = Objects.requireNonNull(clock);
    }

    public long createQuiz(AuthenticatedSession actor, int offeringId, String title, char[] password,
                           LocalDateTime opensAt, LocalDateTime closesAt, List<QuestionDraft> questions) {
        int lecturerId = managementLecturerId(actor, offeringId);
        if (title == null || title.isBlank() || opensAt == null || closesAt == null
                || !closesAt.isAfter(opensAt) || questions == null || questions.isEmpty()
                || questions.size() > 100) {
            throw new ValidationException("Quiz details, schedule, and at least one question are required.");
        }
        for (QuestionDraft question : questions) {
            if (question == null || question.prompt() == null || question.prompt().isBlank()
                    || question.options().size() != 4 || question.options().stream()
                    .anyMatch(option -> option == null || option.isBlank())
                    || question.correctOption() < 0 || question.correctOption() > 3) {
                throw new ValidationException("Each MCQ needs a prompt, four choices, and one correct choice.");
            }
        }
        String encodedPassword = passwordHasher.hash(password);
        return transactions.execute(connection -> {
            long quizId = learningDao.insertQuiz(connection,
                    new QuizRow(0, offeringId, lecturerId, title.trim(), encodedPassword, false, opensAt, closesAt));
            int ordinal = 1;
            for (QuestionDraft question : questions) {
                learningDao.insertQuestion(connection, new QuestionRow(0, Math.toIntExact(quizId), ordinal++,
                        question.prompt().trim(),
                        question.options().stream().map(String::trim).toList(), question.correctOption()));
            }
            auditDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "QUIZ_CREATE", "COURSE_QUIZ", quizId, LocalDateTime.now(clock)));
            return quizId;
        });
    }

    public void setQuizVisibility(AuthenticatedSession actor, int offeringId, int quizId, boolean visible) {
        QuizRow quiz = learningDao.findQuiz(quizId)
                .filter(row -> row.offeringId() == offeringId)
                .orElseThrow(() -> new ValidationException("Quiz was not found for this offering."));
        managementLecturerId(actor, offeringId);
        transactions.execute(connection -> {
            learningDao.setQuizVisibility(connection, quizId, quiz.lecturerId(), offeringId, visible);
            auditDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), visible ? "QUIZ_PUBLISH" : "QUIZ_HIDE",
                    "COURSE_QUIZ", quizId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<QuizView> quizzesForLecturer(AuthenticatedSession actor, int offeringId) {
        managementLecturerId(actor, offeringId);
        return learningDao.findQuizzesForOffering(offeringId).stream().map(this::toQuizView).toList();
    }

    public List<QuizView> quizzesForStudent(AuthenticatedSession actor, int offeringId) {
        authorization.requireUndergraduateForOffering(actor, offeringId);
        return learningDao.findVisibleQuizzesForOffering(offeringId, LocalDateTime.now(clock))
                .stream().map(this::toQuizView).toList();
    }

    public List<QuestionView> questionsForStudent(AuthenticatedSession actor, int quizId, char[] password) {
        QuizRow quiz = studentQuiz(actor, quizId);
        verifyQuizPassword(password, quiz);
        return learningDao.findQuestions(quiz.id()).stream()
                .map(question -> new QuestionView(question.id(), question.prompt(), question.options())).toList();
    }

    public QuizAttemptView attemptQuiz(AuthenticatedSession actor, int quizId, char[] password,
                                       Map<Integer, Integer> answers) {
        QuizRow quiz = studentQuiz(actor, quizId);
        int studentId = authorization.studentId(actor);
        int enrollmentId = activeEnrollment(studentId, quiz.offeringId());
        List<QuestionRow> questions = learningDao.findQuestions(quizId);
        verifyQuizPassword(password, quiz);
        if (questions.isEmpty() || answers == null || answers.size() != questions.size()) {
            throw new ValidationException("Answer every quiz question exactly once.");
        }
        Map<Integer, QuestionRow> indexed = questions.stream()
                .collect(Collectors.toMap(QuestionRow::id, question -> question));
        if (!answers.keySet().equals(indexed.keySet())
                || answers.values().stream().anyMatch(option -> option == null || option < 0 || option > 3)) {
            throw new ValidationException("Quiz answers do not match its questions.");
        }
        if (learningDao.hasAttempt(quizId, studentId)) {
            throw new ValidationException("You have already used your one attempt for this quiz.");
        }
        int score = (int) answers.entrySet().stream()
                .filter(answer -> indexed.get(answer.getKey()).correctOption() == answer.getValue()).count();
        transactions.execute(connection -> {
            learningDao.insertAttempt(connection, quizId, studentId, enrollmentId, score, questions.size());
            auditDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "QUIZ_ATTEMPT", "COURSE_QUIZ", quizId, LocalDateTime.now(clock)));
            return null;
        });
        return new QuizAttemptView(score, questions.size());
    }

    public long createAssignment(AuthenticatedSession actor, int offeringId, String title, String description,
                                 LocalDateTime dueAt, boolean visible) {
        int lecturerId = managementLecturerId(actor, offeringId);
        if (title == null || title.isBlank() || dueAt == null
                || !dueAt.isAfter(LocalDateTime.now(clock))
                || description == null || description.isBlank()) {
            throw new ValidationException("Assignment title, description, and due date are required.");
        }
        return transactions.execute(connection -> {
            long assignmentId = learningDao.insertAssignment(connection,
                    new AssignmentRow(0, offeringId, lecturerId, title.trim(), description.trim(),
                            "", visible, dueAt));
            auditDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "ASSIGNMENT_CREATE", "COURSE_ASSIGNMENT",
                    assignmentId, LocalDateTime.now(clock)));
            return assignmentId;
        });
    }

    public void setAssignmentVisibility(AuthenticatedSession actor, int offeringId,
                                        int assignmentId, boolean visible) {
        AssignmentRow assignment = learningDao.findAssignment(assignmentId)
                .filter(row -> row.offeringId() == offeringId)
                .orElseThrow(() -> new ValidationException("Assignment was not found for this offering."));
        managementLecturerId(actor, offeringId);
        transactions.execute(connection -> {
            learningDao.setAssignmentVisibility(connection, assignmentId,
                    assignment.lecturerId(), offeringId, visible);
            auditDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), visible ? "ASSIGNMENT_PUBLISH" : "ASSIGNMENT_HIDE",
                    "COURSE_ASSIGNMENT", assignmentId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<AssignmentRow> assignmentsForLecturer(AuthenticatedSession actor, int offeringId) {
        managementLecturerId(actor, offeringId);
        return learningDao.findAssignmentsForOffering(offeringId);
    }

    public List<AssignmentRow> assignmentsForStudent(AuthenticatedSession actor, int offeringId) {
        authorization.requireUndergraduateForOffering(actor, offeringId);
        return learningDao.findVisibleAssignmentsForOffering(offeringId, LocalDateTime.now(clock));
    }

    public SubmissionRow submitAssignment(AuthenticatedSession actor, int assignmentId, Path sourceFile) {
        if (actor == null || actor.role() != Role.UNDERGRADUATE) {
            throw new AuthorizationException("Only a student may submit an assignment.");
        }
        AssignmentRow assignment = learningDao.findAssignment(assignmentId)
                .filter(AssignmentRow::visible)
                .orElseThrow(() -> new ValidationException("Assignment is not available."));
        authorization.requireUndergraduateForOffering(actor, assignment.offeringId());
        if (LocalDateTime.now(clock).isAfter(assignment.dueAt())) {
            throw new ValidationException("The assignment deadline has passed.");
        }
        int studentId = authorization.studentId(actor);
        int enrollmentId = activeEnrollment(studentId, assignment.offeringId());
        SubmissionRow previous = learningDao.findSubmission(assignmentId, enrollmentId).orElse(null);
        Path storedFile = copyUpload(sourceFile, "assignments");
        String relativePath = Path.of("uploads", "assignments", storedFile.getFileName().toString())
                .toString().replace('\\', '/');
        try {
            transactions.execute(connection -> {
                if (previous == null) {
                    learningDao.insertSubmission(connection, assignmentId, enrollmentId, relativePath);
                } else if (learningDao.updateSubmission(connection, assignmentId, enrollmentId, relativePath) != 1) {
                    throw new ValidationException("The existing assignment submission could not be updated.");
                }
                auditDao.insert(connection, new AuditLogDao.AuditEvent(
                        0, actor.userId(), "ASSIGNMENT_SUBMIT", "COURSE_ASSIGNMENT",
                        assignmentId, LocalDateTime.now(clock)));
                return null;
            });
        } catch (RuntimeException failure) {
            try {
                Files.deleteIfExists(storedFile);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        if (previous != null) {
            deleteStoredUpload(previous.filePath());
        }
        return learningDao.findSubmission(assignmentId, enrollmentId)
                .orElseThrow(() -> new IllegalStateException("Saved assignment submission could not be reloaded."));
    }

    private QuizRow studentQuiz(AuthenticatedSession actor, int quizId) {
        QuizRow quiz = learningDao.findQuiz(quizId)
                .orElseThrow(() -> new ValidationException("Quiz was not found."));
        authorization.requireUndergraduateForOffering(actor, quiz.offeringId());
        LocalDateTime now = LocalDateTime.now(clock);
        if (!quiz.visible() || now.isBefore(quiz.opensAt()) || now.isAfter(quiz.closesAt())) {
            throw new ValidationException("Quiz is not currently available.");
        }
        return quiz;
    }

    private void verifyQuizPassword(char[] password, QuizRow quiz) {
        if (!passwordHasher.verify(password, quiz.passwordHash())) {
            throw new ValidationException("The quiz password is incorrect.");
        }
    }

    private int managementLecturerId(AuthenticatedSession actor, int offeringId) {
        if (actor != null && actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
            return authorization.lecturerId(actor);
        }
        if (actor != null && actor.role() == Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
            return courseDao.findLecturersForOffering(offeringId).stream().findFirst()
                    .orElseThrow(() -> new ValidationException(
                            "Assign a lecturer to this offering before adding learning activities."));
        }
        throw new AuthorizationException("Only an assigned lecturer or technical officer may manage activities.");
    }

    private int activeEnrollment(int studentId, int offeringId) {
        return courseDao.findActiveEnrollments(offeringId).stream()
                .filter(enrollment -> enrollment.studentId() == studentId)
                .map(CourseDao.EnrollmentRow::id).findFirst()
                .orElseThrow(() -> new AuthorizationException("An active course enrolment is required."));
    }

    private Path copyUpload(Path sourceFile, String category) {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new ValidationException("Select a readable assignment file.");
        }
        try {
            long size = Files.size(sourceFile);
            String filename = sourceFile.getFileName().toString();
            int dot = filename.lastIndexOf('.');
            String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
            if (size <= 0 || size > MAX_UPLOAD_BYTES || !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new ValidationException("Upload a non-empty PDF, Office document, text, or image up to 10 MB.");
            }
            Path directory = Path.of("uploads", category).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path destination = directory.resolve(UUID.randomUUID() + "." + extension);
            return Files.copy(sourceFile, destination);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to store the assignment upload.", failure);
        }
    }

    private void deleteStoredUpload(String relativePath) {
        Path path = Path.of(relativePath).normalize();
        if (path.isAbsolute() || path.startsWith("..")
                || !path.startsWith(Path.of("uploads", "assignments"))) {
            throw new IllegalStateException("Stored prior submission path is invalid.");
        }
        try {
            Files.deleteIfExists(path.toAbsolutePath().normalize());
        } catch (IOException failure) {
            throw new IllegalStateException("The previous assignment upload could not be removed.", failure);
        }
    }

    private QuizView toQuizView(QuizRow quiz) {
        return new QuizView(quiz.id(), quiz.offeringId(), quiz.title(), quiz.opensAt(), quiz.closesAt());
    }
}
