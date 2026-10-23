package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcLearningActivityDao implements LearningActivityDao {
    private final JdbcExecutor jdbc;

    public JdbcLearningActivityDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertQuiz(Connection connection, QuizRow quiz) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_quizzes (offering_id, lecturer_id, title, password_hash, is_visible, "
                        + "opens_at, closes_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, quiz.offeringId());
                    statement.setInt(2, quiz.lecturerId());
                    statement.setString(3, quiz.title());
                    statement.setString(4, quiz.passwordHash());
                    statement.setBoolean(5, quiz.visible());
                    statement.setTimestamp(6, Timestamp.valueOf(quiz.opensAt()));
                    statement.setTimestamp(7, Timestamp.valueOf(quiz.closesAt()));
                });
    }

    @Override
    public long insertQuestion(Connection connection, QuestionRow question) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO quiz_questions (quiz_id, ordinal_no, prompt, option_a, option_b, option_c, "
                        + "option_d, correct_option) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, question.quizId());
                    statement.setInt(2, question.ordinal());
                    statement.setString(3, question.prompt());
                    for (int index = 0; index < 4; index++) {
                        statement.setString(4 + index, question.options().get(index));
                    }
                    statement.setInt(8, question.correctOption());
                });
    }

    @Override
    public int setQuizVisibility(Connection connection, int quizId, int lecturerId,
                                 int offeringId, boolean visible) {
        return jdbc.update(connection,
                "UPDATE course_quizzes SET is_visible = ? WHERE quiz_id = ? AND lecturer_id = ? AND offering_id = ?",
                statement -> {
                    statement.setBoolean(1, visible);
                    statement.setInt(2, quizId);
                    statement.setInt(3, lecturerId);
                    statement.setInt(4, offeringId);
                });
    }

    @Override
    public List<QuizRow> findQuizzesForOffering(int offeringId) {
        return jdbc.query("SELECT * FROM course_quizzes WHERE offering_id = ? ORDER BY quiz_id",
                statement -> statement.setInt(1, offeringId), this::mapQuiz);
    }

    @Override
    public List<QuizRow> findVisibleQuizzesForOffering(int offeringId, LocalDateTime now) {
        return jdbc.query("SELECT * FROM course_quizzes WHERE offering_id = ? AND is_visible = TRUE "
                        + "AND opens_at <= ? AND closes_at >= ? ORDER BY quiz_id",
                statement -> {
                    statement.setInt(1, offeringId);
                    statement.setTimestamp(2, Timestamp.valueOf(now));
                    statement.setTimestamp(3, Timestamp.valueOf(now));
                }, this::mapQuiz);
    }

    @Override
    public Optional<QuizRow> findQuiz(int quizId) {
        return jdbc.query("SELECT * FROM course_quizzes WHERE quiz_id = ?",
                statement -> statement.setInt(1, quizId), this::mapQuiz).stream().findFirst();
    }

    @Override
    public List<QuestionRow> findQuestions(int quizId) {
        return jdbc.query("SELECT question_id, quiz_id, ordinal_no, prompt, option_a, option_b, option_c, "
                        + "option_d, correct_option FROM quiz_questions WHERE quiz_id = ? ORDER BY ordinal_no",
                statement -> statement.setInt(1, quizId), result -> new QuestionRow(
                        result.getInt("question_id"), result.getInt("quiz_id"),
                        result.getInt("ordinal_no"), result.getString("prompt"),
                        List.of(result.getString("option_a"), result.getString("option_b"),
                                result.getString("option_c"), result.getString("option_d")),
                        result.getInt("correct_option")));
    }

    @Override
    public boolean hasAttempt(int quizId, int studentId) {
        return !jdbc.query("SELECT attempt_id FROM quiz_attempts WHERE quiz_id = ? AND student_id = ?",
                statement -> {
                    statement.setInt(1, quizId);
                    statement.setInt(2, studentId);
                }, result -> result.getLong(1)).isEmpty();
    }

    @Override
    public long insertAttempt(Connection connection, int quizId, int studentId,
                              int enrollmentId, int score, int total) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO quiz_attempts (quiz_id, student_id, enrollment_id, score, total_questions, submitted_at) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                statement -> {
                    statement.setInt(1, quizId);
                    statement.setInt(2, studentId);
                    statement.setInt(3, enrollmentId);
                    statement.setInt(4, score);
                    statement.setInt(5, total);
                });
    }

    @Override
    public long insertAssignment(Connection connection, AssignmentRow assignment) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_assignments (offering_id, lecturer_id, title, description, file_path, "
                        + "is_visible, due_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, assignment.offeringId());
                    statement.setInt(2, assignment.lecturerId());
                    statement.setString(3, assignment.title());
                    statement.setString(4, assignment.description());
                    statement.setString(5, assignment.filePath());
                    statement.setBoolean(6, assignment.visible());
                    statement.setTimestamp(7, Timestamp.valueOf(assignment.dueAt()));
                });
    }

    @Override
    public int setAssignmentVisibility(Connection connection, int assignmentId, int lecturerId,
                                       int offeringId, boolean visible) {
        return jdbc.update(connection,
                "UPDATE course_assignments SET is_visible = ? WHERE assignment_id = ? AND lecturer_id = ? "
                        + "AND offering_id = ?",
                statement -> {
                    statement.setBoolean(1, visible);
                    statement.setInt(2, assignmentId);
                    statement.setInt(3, lecturerId);
                    statement.setInt(4, offeringId);
                });
    }

    @Override
    public List<AssignmentRow> findAssignmentsForOffering(int offeringId) {
        return jdbc.query("SELECT * FROM course_assignments WHERE offering_id = ? ORDER BY due_at",
                statement -> statement.setInt(1, offeringId), this::mapAssignment);
    }

    @Override
    public List<AssignmentRow> findVisibleAssignmentsForOffering(int offeringId, LocalDateTime now) {
        return jdbc.query("SELECT * FROM course_assignments WHERE offering_id = ? AND is_visible = TRUE "
                        + "ORDER BY due_at",
                statement -> statement.setInt(1, offeringId), this::mapAssignment);
    }

    @Override
    public Optional<AssignmentRow> findAssignment(int assignmentId) {
        return jdbc.query("SELECT * FROM course_assignments WHERE assignment_id = ?",
                statement -> statement.setInt(1, assignmentId), this::mapAssignment).stream().findFirst();
    }

    @Override
    public long insertSubmission(Connection connection, int assignmentId, int enrollmentId, String filePath) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO assignment_submissions (assignment_id, enrollment_id, file_path, submitted_at) "
                        + "VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                statement -> {
                    statement.setInt(1, assignmentId);
                    statement.setInt(2, enrollmentId);
                    statement.setString(3, filePath);
                });
    }

    @Override
    public int updateSubmission(Connection connection, int assignmentId, int enrollmentId, String filePath) {
        return jdbc.update(connection,
                "UPDATE assignment_submissions SET file_path = ?, submitted_at = CURRENT_TIMESTAMP "
                        + "WHERE assignment_id = ? AND enrollment_id = ?",
                statement -> {
                    statement.setString(1, filePath);
                    statement.setInt(2, assignmentId);
                    statement.setInt(3, enrollmentId);
                });
    }

    @Override
    public Optional<SubmissionRow> findSubmission(int assignmentId, int enrollmentId) {
        return jdbc.query("SELECT submission_id, assignment_id, enrollment_id, file_path, submitted_at "
                        + "FROM assignment_submissions WHERE assignment_id = ? AND enrollment_id = ?",
                statement -> {
                    statement.setInt(1, assignmentId);
                    statement.setInt(2, enrollmentId);
                }, result -> new SubmissionRow(result.getInt("submission_id"),
                        result.getInt("assignment_id"), result.getInt("enrollment_id"),
                        result.getString("file_path"), result.getTimestamp("submitted_at").toLocalDateTime()))
                .stream().findFirst();
    }

    private QuizRow mapQuiz(java.sql.ResultSet result) throws java.sql.SQLException {
        return new QuizRow(result.getInt("quiz_id"), result.getInt("offering_id"),
                result.getInt("lecturer_id"), result.getString("title"),
                result.getString("password_hash"), result.getBoolean("is_visible"),
                result.getTimestamp("opens_at").toLocalDateTime(),
                result.getTimestamp("closes_at").toLocalDateTime());
    }

    private AssignmentRow mapAssignment(java.sql.ResultSet result) throws java.sql.SQLException {
        return new AssignmentRow(result.getInt("assignment_id"), result.getInt("offering_id"),
                result.getInt("lecturer_id"), result.getString("title"), result.getString("description"),
                result.getString("file_path"), result.getBoolean("is_visible"),
                result.getTimestamp("due_at").toLocalDateTime());
    }
}
