package lk.ac.ruhuna.fot.ams.data.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class JdbcDashboardDao implements DashboardDao {
    private final JdbcExecutor jdbc;

    public JdbcDashboardDao(JdbcExecutor jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Override
    public Map<String, String> loadMetrics(AuthenticatedSession session) {
        Objects.requireNonNull(session);
        Map<String, String> metrics = new LinkedHashMap<>();
        switch (session.role()) {
            case ADMIN -> loadAdminMetrics(metrics);
            case LECTURER -> loadLecturerMetrics(metrics, session.userId());
            case TECHNICAL_OFFICER -> loadTechnicalMetrics(metrics, session.userId());
            case UNDERGRADUATE -> loadUndergraduateMetrics(metrics, session.userId());
        }
        return Map.copyOf(metrics);
    }

    private void loadAdminMetrics(Map<String, String> metrics) {
        List<String> users = jdbc.query(
                "SELECT COUNT(*), SUM(role = 'ADMIN'), SUM(role = 'LECTURER'), "
                        + "SUM(role = 'TECHNICAL_OFFICER'), SUM(role = 'UNDERGRADUATE') FROM users",
                statement -> { },
                result -> result.getLong(1) + " total ("
                        + result.getLong(2) + " admins, "
                        + result.getLong(3) + " lecturers, "
                        + result.getLong(4) + " technical officers, "
                        + result.getLong(5) + " students)");
        putFirst(metrics, "Users", users);
        putFirst(metrics, "Departments / Batches",
                jdbc.query("SELECT CONCAT((SELECT COUNT(*) FROM departments), ' / ', "
                                + "(SELECT COUNT(*) FROM batches))",
                        statement -> { }, result -> result.getString(1)));
        putFirst(metrics, "Courses", count("SELECT COUNT(*) FROM courses"));
        putFirst(metrics, "Offerings", count("SELECT COUNT(*) FROM course_offerings"));
        putFirst(metrics, "Notices", count("SELECT COUNT(*) FROM notices"));
        putFirst(metrics, "Timetable status", jdbc.query(
                "SELECT CONCAT(COUNT(*), ' configured') FROM timetables",
                statement -> { }, result -> result.getString(1)));
    }

    private void loadLecturerMetrics(Map<String, String> metrics, long userId) {
        putFirst(metrics, "Assigned offerings", jdbc.query(
                "SELECT COUNT(*) FROM course_offerings co "
                        + "LEFT JOIN lecturers l ON l.lecturer_id = co.coordinator_id "
                        + "WHERE l.user_id = ? OR EXISTS (SELECT 1 FROM course_offering_lecturers col "
                        + "JOIN lecturers assigned ON assigned.lecturer_id = col.lecturer_id "
                        + "WHERE col.offering_id = co.offering_id AND assigned.user_id = ?)",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setLong(2, userId);
                }, result -> result.getString(1)));
        putFirst(metrics, "Notices", count("SELECT COUNT(*) FROM notices"));
        putFirst(metrics, "Assessments", jdbc.query(
                "SELECT COUNT(*) FROM assessments a JOIN course_offerings co "
                        + "ON co.offering_id = a.offering_id "
                        + "LEFT JOIN lecturers l ON l.lecturer_id = co.coordinator_id "
                        + "WHERE l.user_id = ? OR EXISTS (SELECT 1 FROM course_offering_lecturers col "
                        + "JOIN lecturers assigned ON assigned.lecturer_id = col.lecturer_id "
                        + "WHERE col.offering_id = co.offering_id AND assigned.user_id = ?)",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setLong(2, userId);
                }, result -> result.getString(1)));
        putFirst(metrics, "Enrolled students", jdbc.query(
                "SELECT COUNT(DISTINCT ce.student_id) FROM course_enrollments ce "
                        + "JOIN course_offerings co ON co.offering_id = ce.offering_id "
                        + "LEFT JOIN lecturers l ON l.lecturer_id = co.coordinator_id "
                        + "WHERE l.user_id = ? OR EXISTS (SELECT 1 FROM course_offering_lecturers col "
                        + "JOIN lecturers assigned ON assigned.lecturer_id = col.lecturer_id "
                        + "WHERE col.offering_id = co.offering_id AND assigned.user_id = ?)",
                statement -> {
                    statement.setLong(1, userId);
                    statement.setLong(2, userId);
                }, result -> result.getString(1)));
    }

    private void loadTechnicalMetrics(Map<String, String> metrics, long userId) {
        putFirst(metrics, "Department", jdbc.query(
                "SELECT d.name FROM technical_officers t "
                        + "JOIN departments d ON d.department_id = t.department_id "
                        + "WHERE t.user_id = ?",
                statement -> statement.setLong(1, userId), result -> result.getString(1)));
        putFirst(metrics, "Timetable", count("SELECT COUNT(*) FROM timetables"));
    }

    private void loadUndergraduateMetrics(Map<String, String> metrics, long userId) {
        putFirst(metrics, "Student number", jdbc.query(
                "SELECT student_no FROM undergraduates WHERE user_id = ?",
                statement -> statement.setLong(1, userId), result -> result.getString(1)));
        putFirst(metrics, "Current courses", jdbc.query(
                "SELECT COUNT(*) FROM course_enrollments ce "
                        + "JOIN undergraduates u ON u.student_id = ce.student_id "
                        + "WHERE u.user_id = ? AND ce.enrollment_status = 'ACTIVE'",
                statement -> statement.setLong(1, userId), result -> result.getString(1)));
        putFirst(metrics, "Notices", count("SELECT COUNT(*) FROM notices"));
    }

    private List<String> count(String sql) {
        return jdbc.query(sql, statement -> { }, result -> result.getString(1));
    }

    private void putFirst(Map<String, String> metrics, String label, List<String> values) {
        if (!values.isEmpty()) {
            metrics.put(label, values.get(0));
        }
    }
}
