package lk.ac.ruhuna.fot.ams.data.dao;

import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;

public final class JdbcAuthorizationScopeDao implements AuthorizationScopeLookup {
    private final JdbcExecutor jdbc;

    public JdbcAuthorizationScopeDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean lecturerAssignedToOffering(long userId, int offeringId) {
        return exists("SELECT 1 FROM lecturers l JOIN course_offerings o "
                        + "ON o.coordinator_id = l.lecturer_id OR EXISTS ("
                        + "SELECT 1 FROM course_offering_lecturers a "
                        + "WHERE a.offering_id = o.offering_id AND a.lecturer_id = l.lecturer_id) "
                        + "WHERE l.user_id = ? AND o.offering_id = ?",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, offeringId);
                });
    }

    @Override
    public boolean lecturerAssignedToStudent(long userId, int studentId) {
        return exists("SELECT 1 FROM lecturers l "
                        + "JOIN course_offerings o ON o.coordinator_id = l.lecturer_id OR EXISTS ("
                        + "SELECT 1 FROM course_offering_lecturers a "
                        + "WHERE a.offering_id = o.offering_id AND a.lecturer_id = l.lecturer_id) "
                        + "JOIN course_enrollments e ON e.offering_id = o.offering_id "
                        + "WHERE l.user_id = ? AND e.student_id = ?",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, studentId);
                });
    }

    @Override
    public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) {
        return exists("SELECT 1 FROM technical_officers t "
                        + "JOIN batches b ON b.department_id = t.department_id "
                        + "JOIN course_offerings o ON o.batch_id = b.batch_id "
                        + "JOIN semesters s ON s.semester_id = o.semester_id "
                        + "WHERE t.user_id = ? AND o.offering_id = ? "
                        + "AND CURRENT_DATE BETWEEN s.start_date AND s.end_date",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, offeringId);
                });
    }

    @Override
    public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) {
        return exists("SELECT 1 FROM technical_officers t "
                        + "JOIN batches b ON b.department_id = t.department_id "
                        + "JOIN undergraduates u ON u.batch_id = b.batch_id "
                        + "WHERE t.user_id = ? AND u.student_id = ? "
                        + "AND EXISTS (SELECT 1 FROM course_offerings o "
                        + "JOIN semesters s ON s.semester_id = o.semester_id "
                        + "WHERE o.batch_id = u.batch_id "
                        + "AND CURRENT_DATE BETWEEN s.start_date AND s.end_date)",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, studentId);
                });
    }

    @Override
    public boolean undergraduateOwnsStudent(long userId, int studentId) {
        return exists("SELECT 1 FROM undergraduates WHERE user_id = ? AND student_id = ?",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, studentId);
                });
    }

    @Override
    public boolean undergraduateEnrolledInOffering(long userId, int offeringId) {
        return exists("SELECT 1 FROM undergraduates u JOIN course_enrollments e "
                        + "ON e.student_id = u.student_id WHERE u.user_id = ? AND e.offering_id = ? "
                        + "AND e.enrollment_status = 'ACTIVE'",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setInt(2, offeringId);
                });
    }

    @Override
    public OptionalInt lecturerIdForUser(long userId) {
        return findProfileId("SELECT lecturer_id FROM lecturers WHERE user_id = ?", userId);
    }

    @Override
    public OptionalInt technicalOfficerIdForUser(long userId) {
        return findProfileId("SELECT officer_id FROM technical_officers WHERE user_id = ?", userId);
    }

    @Override
    public OptionalInt studentIdForUser(long userId) {
        return findProfileId("SELECT student_id FROM undergraduates WHERE user_id = ?", userId);
    }

    @Override
    public OptionalInt studentBatchIdForUser(long userId) {
        return findProfileId("SELECT batch_id FROM undergraduates WHERE user_id = ?", userId);
    }

    private boolean exists(String sql, JdbcExecutor.StatementBinder binder) {
        return !jdbc.query(sql, binder, result -> result.getInt(1)).isEmpty();
    }

    private OptionalInt findProfileId(String sql, long userId) {
        return jdbc.query(sql, statement -> statement.setLong(1, userId), result -> result.getInt(1)).stream()
                .mapToInt(Integer::intValue)
                .findFirst();
    }
}
