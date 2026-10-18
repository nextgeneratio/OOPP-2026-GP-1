package lk.ac.ruhuna.fot.ams.data.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcAssessmentDao implements AssessmentDao {
    private final JdbcExecutor jdbc;

    public JdbcAssessmentDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertAssessment(Connection connection, AssessmentRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO assessments (offering_id, name, assessment_type, weight_percent, is_ca_component) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.offeringId());
                    statement.setString(2, row.name());
                    statement.setString(3, row.type());
                    statement.setBigDecimal(4, row.weight());
                    statement.setBoolean(5, row.caComponent());
                });
    }

    @Override
    public long insertMark(Connection connection, MarkRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO marks (assessment_id, enrollment_id, score, entered_by) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.assessmentId());
                    statement.setInt(2, row.enrollmentId());
                    statement.setBigDecimal(3, row.score());
                    statement.setInt(4, row.enteredBy());
                });
    }

    @Override
    public int reviseMark(Connection connection, MarkRow row) {
        return jdbc.update(connection,
                "UPDATE marks SET score = ?, entered_by = ? WHERE assessment_id = ? AND enrollment_id = ?",
                statement -> {
                    statement.setBigDecimal(1, row.score());
                    statement.setInt(2, row.enteredBy());
                    statement.setInt(3, row.assessmentId());
                    statement.setInt(4, row.enrollmentId());
                });
    }

    @Override
    public List<AssessmentRow> findAssessments(int offeringId) {
        return jdbc.query("SELECT assessment_id, offering_id, name, assessment_type, weight_percent, "
                        + "is_ca_component FROM assessments WHERE offering_id = ? ORDER BY assessment_id",
                statement -> statement.setInt(1, offeringId), result -> new AssessmentRow(
                        result.getInt("assessment_id"), result.getInt("offering_id"),
                        result.getString("name"), result.getString("assessment_type"),
                        result.getBigDecimal("weight_percent"), result.getBoolean("is_ca_component")));
    }

    @Override
    public List<MarkRow> findMarks(int offeringId, Integer enrollmentId) {
        String sql = "SELECT m.mark_id, m.assessment_id, m.enrollment_id, m.score, m.entered_by "
                + "FROM marks m JOIN assessments a ON a.assessment_id = m.assessment_id "
                + "WHERE a.offering_id = ? AND (? IS NULL OR m.enrollment_id = ?) "
                + "ORDER BY m.enrollment_id, m.assessment_id";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, offeringId);
            if (enrollmentId == null) {
                statement.setNull(2, java.sql.Types.INTEGER);
                statement.setNull(3, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, enrollmentId);
                statement.setInt(3, enrollmentId);
            }
        }, this::mapMark);
    }

    @Override
    public List<MarkRow> findMarks(Connection connection, int offeringId, int enrollmentId) {
        String sql = "SELECT m.mark_id, m.assessment_id, m.enrollment_id, m.score, m.entered_by "
                + "FROM marks m JOIN assessments a ON a.assessment_id = m.assessment_id "
                + "WHERE a.offering_id = ? AND m.enrollment_id = ? ORDER BY m.assessment_id";
        return jdbc.query(connection, sql, statement -> {
            statement.setInt(1, offeringId);
            statement.setInt(2, enrollmentId);
        }, this::mapMark);
    }

    private MarkRow mapMark(ResultSet result) throws SQLException {
        BigDecimal score = result.getBigDecimal("score");
        return new MarkRow(result.getInt("mark_id"), result.getInt("assessment_id"),
                result.getInt("enrollment_id"), score, result.getInt("entered_by"));
    }
}
