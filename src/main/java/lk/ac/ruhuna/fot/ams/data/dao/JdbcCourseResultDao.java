package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcCourseResultDao implements CourseResultDao {
    private final JdbcExecutor jdbc;

    public JdbcCourseResultDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int insert(Connection connection, ResultSnapshot snapshot) {
        return jdbc.update(connection,
                "INSERT INTO course_results (enrollment_id, final_mark, scheme_version, grade_letter, "
                        + "grade_point, credits_counted, completed_at, recorded_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> bind(statement, snapshot));
    }

    @Override
    public int archiveCurrent(Connection connection, int enrollmentId) {
        return jdbc.update(connection,
                "UPDATE course_results SET is_current = FALSE WHERE enrollment_id = ? AND is_current = TRUE",
                statement -> statement.setInt(1, enrollmentId));
    }

    @Override
    public Optional<ResultSnapshot> findByEnrollment(int enrollmentId) {
        return jdbc.query("SELECT enrollment_id, final_mark, scheme_version, grade_letter, grade_point, "
                        + "credits_counted, completed_at, recorded_by "
                        +                         "FROM course_results WHERE enrollment_id = ? AND is_current = TRUE",
                statement -> statement.setInt(1, enrollmentId), this::mapSnapshot).stream().findFirst();
    }

    @Override
    public List<StudentResult> findByStudent(int studentId) {
        return findStudentResults(studentId, null);
    }

    @Override
    public List<StudentResult> findByStudentAndSemester(int studentId, int semesterId) {
        return findStudentResults(studentId, semesterId);
    }

    private List<StudentResult> findStudentResults(int studentId, Integer semesterId) {
        String sql = "SELECT e.enrollment_id, e.student_id, c.course_code, o.semester_id, e.attempt_no, "
                + "r.credits_counted, r.grade_point, r.completed_at FROM course_results r "
                + "JOIN course_enrollments e ON e.enrollment_id = r.enrollment_id "
                + "JOIN course_offerings o ON o.offering_id = e.offering_id "
                + "JOIN courses c ON c.course_id = o.course_id "
                + "WHERE e.student_id = ? AND r.is_current = TRUE AND e.enrollment_status = 'COMPLETED' "
                + "AND (? IS NULL OR o.semester_id = ?) "
                + "ORDER BY r.completed_at, e.attempt_no";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, studentId);
            if (semesterId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, semesterId);
                statement.setInt(3, semesterId);
            }
        }, result -> new StudentResult(
                result.getInt("enrollment_id"), result.getInt("student_id"),
                result.getString("course_code"), result.getInt("semester_id"),
                result.getInt("attempt_no"), result.getBigDecimal("credits_counted"),
                result.getBigDecimal("grade_point"), result.getTimestamp("completed_at").toLocalDateTime()));
    }

    private ResultSnapshot mapSnapshot(ResultSet result) throws SQLException {
        return new ResultSnapshot(result.getInt("enrollment_id"), result.getBigDecimal("final_mark"),
                result.getString("scheme_version"), result.getString("grade_letter"),
                result.getBigDecimal("grade_point"), result.getBigDecimal("credits_counted"),
                result.getTimestamp("completed_at").toLocalDateTime(), result.getInt("recorded_by"));
    }

    private void bind(java.sql.PreparedStatement statement, ResultSnapshot snapshot) throws SQLException {
        statement.setInt(1, snapshot.enrollmentId());
        statement.setBigDecimal(2, snapshot.finalMark());
        statement.setString(3, snapshot.schemeVersion());
        statement.setString(4, snapshot.gradeLetter());
        statement.setBigDecimal(5, snapshot.gradePoint());
        statement.setBigDecimal(6, snapshot.creditsCounted());
        statement.setTimestamp(7, Timestamp.valueOf(snapshot.completedAt()));
        statement.setInt(8, snapshot.recordedBy());
    }
}
