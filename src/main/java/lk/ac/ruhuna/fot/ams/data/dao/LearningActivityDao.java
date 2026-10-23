package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LearningActivityDao {
    record QuizRow(int id, int offeringId, int lecturerId, String title, String passwordHash,
                   boolean visible, LocalDateTime opensAt, LocalDateTime closesAt) {
    }

    record QuestionRow(int id, int quizId, int ordinal, String prompt,
                       List<String> options, int correctOption) {
        public QuestionRow {
            options = List.copyOf(options);
        }
    }

    record AssignmentRow(int id, int offeringId, int lecturerId, String title,
                         String description, String filePath, boolean visible, LocalDateTime dueAt) {
    }

    record SubmissionRow(int id, int assignmentId, int enrollmentId,
                         String filePath, LocalDateTime submittedAt) {
    }

    long insertQuiz(Connection connection, QuizRow quiz);
    long insertQuestion(Connection connection, QuestionRow question);
    int setQuizVisibility(Connection connection, int quizId, int lecturerId, int offeringId, boolean visible);
    List<QuizRow> findQuizzesForOffering(int offeringId);
    List<QuizRow> findVisibleQuizzesForOffering(int offeringId, LocalDateTime now);
    Optional<QuizRow> findQuiz(int quizId);
    List<QuestionRow> findQuestions(int quizId);
    boolean hasAttempt(int quizId, int studentId);
    long insertAttempt(Connection connection, int quizId, int studentId,
                       int enrollmentId, int score, int total);
    long insertAssignment(Connection connection, AssignmentRow assignment);
    int setAssignmentVisibility(Connection connection, int assignmentId, int lecturerId,
                                int offeringId, boolean visible);
    List<AssignmentRow> findAssignmentsForOffering(int offeringId);
    List<AssignmentRow> findVisibleAssignmentsForOffering(int offeringId, LocalDateTime now);
    Optional<AssignmentRow> findAssignment(int assignmentId);
    long insertSubmission(Connection connection, int assignmentId, int enrollmentId, String filePath);
    int updateSubmission(Connection connection, int assignmentId, int enrollmentId, String filePath);
    Optional<SubmissionRow> findSubmission(int assignmentId, int enrollmentId);
}
