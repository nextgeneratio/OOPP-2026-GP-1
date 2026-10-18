package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;

public final class JdbcAttendanceDao implements AttendanceDao {
    private final JdbcExecutor jdbc;

    public JdbcAttendanceDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertSession(Connection connection, SessionRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO attendance_sessions "
                        + "(offering_id, component_id, session_date, timetable_entry_id, recorded_by) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.offeringId());
                    statement.setInt(2, row.componentId());
                    statement.setDate(3, Date.valueOf(row.date()));
                    if (row.timetableEntryId() == null) {
                        statement.setNull(4, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(4, row.timetableEntryId());
                    }
                    statement.setInt(5, row.recordedBy());
                });
    }

    @Override
    public long insertRecord(Connection connection, AttendanceRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO attendance_records (session_id, enrollment_id, is_present) VALUES (?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.sessionId());
                    statement.setInt(2, row.enrollmentId());
                    statement.setBoolean(3, row.present());
                });
    }

    @Override
    public int reviseRecord(Connection connection, AttendanceRow row) {
        return jdbc.update(connection,
                "UPDATE attendance_records SET is_present = ? WHERE session_id = ? AND enrollment_id = ?",
                statement -> {
                    statement.setBoolean(1, row.present());
                    statement.setInt(2, row.sessionId());
                    statement.setInt(3, row.enrollmentId());
                });
    }

    @Override
    public List<SessionRow> findSessions(int offeringId) {
        return jdbc.query("SELECT session_id, offering_id, component_id, session_date, "
                        + "timetable_entry_id, recorded_by FROM attendance_sessions "
                        + "WHERE offering_id = ? ORDER BY session_date, component_id",
                statement -> statement.setInt(1, offeringId), result -> {
                    int timetableEntry = result.getInt("timetable_entry_id");
                    Integer timetableEntryId = result.wasNull() ? null : timetableEntry;
                    return new SessionRow(result.getInt("session_id"), result.getInt("offering_id"),
                            result.getInt("component_id"), result.getDate("session_date").toLocalDate(),
                            timetableEntryId, result.getInt("recorded_by"));
                });
    }

    @Override
    public List<AttendanceHistoryRow> findHistory(int enrollmentId, int offeringId) {
        String sql = "SELECT s.session_id, r.enrollment_id, s.component_id, c.component_type, "
                + "s.session_date, r.is_present FROM attendance_records r "
                + "JOIN attendance_sessions s ON s.session_id = r.session_id "
                + "JOIN course_components c ON c.component_id = s.component_id "
                + "WHERE r.enrollment_id = ? AND s.offering_id = ? ORDER BY s.session_date";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, enrollmentId);
            statement.setInt(2, offeringId);
        }, this::mapHistory);
    }

    private AttendanceHistoryRow mapHistory(ResultSet result) throws SQLException {
        Date sessionDate = result.getDate("session_date");
        return new AttendanceHistoryRow(result.getInt("session_id"), result.getInt("enrollment_id"),
                result.getInt("component_id"), ComponentType.valueOf(result.getString("component_type")),
                sessionDate.toLocalDate(), result.getBoolean("is_present"));
    }
}
