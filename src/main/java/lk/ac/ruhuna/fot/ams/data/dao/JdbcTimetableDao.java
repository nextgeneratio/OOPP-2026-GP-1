package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;

public final class JdbcTimetableDao implements TimetableDao {
    private final JdbcExecutor jdbc;

    public JdbcTimetableDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertTimetable(Connection connection, TimetableRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO timetables (title, department_id, semester_id) VALUES (?, ?, ?)",
                statement -> {
                    statement.setString(1, row.title());
                    statement.setInt(2, row.departmentId());
                    statement.setInt(3, row.semesterId());
                });
    }

    @Override
    public long insertEntry(Connection connection, EntryRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO timetable_entries "
                        + "(timetable_id, offering_id, component_id, day_of_week, start_time, end_time, location) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                statement -> bindEntry(statement, row, false));
    }

    @Override
    public int updateEntry(Connection connection, EntryRow row) {
        return jdbc.update(connection,
                "UPDATE timetable_entries SET offering_id = ?, component_id = ?, day_of_week = ?, "
                        + "start_time = ?, end_time = ?, location = ? WHERE timetable_entry_id = ?",
                statement -> {
                    statement.setInt(1, row.offeringId());
                    statement.setInt(2, row.componentId());
                    statement.setString(3, row.dayOfWeek());
                    statement.setTime(4, Time.valueOf(row.start()));
                    statement.setTime(5, Time.valueOf(row.end()));
                    statement.setString(6, row.location());
                    statement.setInt(7, row.id());
                });
    }

    @Override
    public Optional<TimetableRow> findTimetable(int timetableId) {
        return jdbc.query("SELECT timetable_id, title, department_id, semester_id "
                        + "FROM timetables WHERE timetable_id = ?",
                statement -> statement.setInt(1, timetableId),
                result -> new TimetableRow(result.getInt("timetable_id"), result.getString("title"),
                        result.getInt("department_id"), result.getInt("semester_id")))
                .stream().findFirst();
    }

    @Override
    public Optional<EntryRow> findEntry(int entryId) {
        return jdbc.query("SELECT timetable_entry_id, timetable_id, offering_id, component_id, "
                        + "day_of_week, start_time, end_time, location "
                        + "FROM timetable_entries WHERE timetable_entry_id = ?",
                statement -> statement.setInt(1, entryId), this::map).stream().findFirst();
    }

    @Override
    public List<EntryRow> findEntries(int departmentId, int semesterId, String dayOfWeek) {
        String sql = "SELECT e.timetable_entry_id, e.timetable_id, e.offering_id, e.component_id, "
                + "e.day_of_week, e.start_time, e.end_time, e.location "
                + "FROM timetable_entries e JOIN timetables t ON t.timetable_id = e.timetable_id "
                + "WHERE t.department_id = ? AND t.semester_id = ? "
                + "AND (? IS NULL OR e.day_of_week = ?) ORDER BY e.day_of_week, e.start_time";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, departmentId);
            statement.setInt(2, semesterId);
            if (dayOfWeek == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
                statement.setNull(4, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, dayOfWeek);
                statement.setString(4, dayOfWeek);
            }
        }, this::map);
    }

    @Override
    public List<EntryRow> findConflicts(
            int departmentId, int semesterId, EntryRow candidate, int lecturerId) {
        String sql = "SELECT e.timetable_entry_id, e.timetable_id, e.offering_id, e.component_id, "
                + "e.day_of_week, e.start_time, e.end_time, e.location "
                + "FROM timetable_entries e JOIN timetables t ON t.timetable_id = e.timetable_id "
                + "JOIN course_offerings o ON o.offering_id = e.offering_id "
                + "WHERE t.department_id = ? AND t.semester_id = ? AND e.day_of_week = ? "
                + "AND e.timetable_entry_id <> ? AND e.start_time < ? AND e.end_time > ? "
                + "AND (e.location = ? OR e.offering_id = ? OR o.coordinator_id = ? "
                + "OR EXISTS (SELECT 1 FROM course_offering_lecturers a "
                + "WHERE a.offering_id = e.offering_id AND a.lecturer_id = ?)) "
                + "ORDER BY e.start_time";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, departmentId);
            statement.setInt(2, semesterId);
            statement.setString(3, candidate.dayOfWeek());
            statement.setInt(4, candidate.id());
            statement.setTime(5, Time.valueOf(candidate.end()));
            statement.setTime(6, Time.valueOf(candidate.start()));
            statement.setString(7, candidate.location());
            statement.setInt(8, candidate.offeringId());
            statement.setInt(9, lecturerId);
            statement.setInt(10, lecturerId);
        }, this::map);
    }

    @Override
    public boolean entryMatchesOfferingAndComponent(
            int entryId, int offeringId, int componentId, java.time.LocalDate sessionDate) {
        return !jdbc.query("SELECT timetable_entry_id FROM timetable_entries "
                        + "WHERE timetable_entry_id = ? AND offering_id = ? AND component_id = ? "
                        + "AND day_of_week = CASE DAYOFWEEK(?) "
                        + "WHEN 1 THEN 'SUN' WHEN 2 THEN 'MON' WHEN 3 THEN 'TUE' "
                        + "WHEN 4 THEN 'WED' WHEN 5 THEN 'THU' WHEN 6 THEN 'FRI' ELSE 'SAT' END",
                statement -> {
                    statement.setInt(1, entryId);
                    statement.setInt(2, offeringId);
                    statement.setInt(3, componentId);
                    statement.setDate(4, java.sql.Date.valueOf(sessionDate));
                }, result -> result.getInt(1)).isEmpty();
    }

    @Override
    public boolean offeringMatchesTimetable(int timetableId, int offeringId, int componentId) {
        return !jdbc.query("SELECT e.timetable_id FROM course_offerings o "
                        + "JOIN courses c ON c.course_id = o.course_id "
                        + "JOIN batches b ON b.batch_id = o.batch_id "
                        + "JOIN course_components cc ON cc.course_id = c.course_id "
                        + "JOIN timetables t ON t.timetable_id = ? "
                        + "WHERE o.offering_id = ? AND cc.component_id = ? "
                        + "AND t.department_id = b.department_id AND t.semester_id = o.semester_id",
                statement -> {
                    statement.setInt(1, timetableId);
                    statement.setInt(2, offeringId);
                    statement.setInt(3, componentId);
                }, result -> result.getInt(1)).isEmpty();
    }

    private void bindEntry(java.sql.PreparedStatement statement, EntryRow row, boolean includeId)
            throws SQLException {
        statement.setInt(1, row.timetableId());
        statement.setInt(2, row.offeringId());
        statement.setInt(3, row.componentId());
        statement.setString(4, row.dayOfWeek());
        statement.setTime(5, Time.valueOf(row.start()));
        statement.setTime(6, Time.valueOf(row.end()));
        statement.setString(7, row.location());
        if (includeId) {
            statement.setInt(8, row.id());
        }
    }

    private EntryRow map(ResultSet result) throws SQLException {
        return new EntryRow(result.getInt("timetable_entry_id"), result.getInt("timetable_id"),
                result.getInt("offering_id"), result.getInt("component_id"),
                result.getString("day_of_week"), result.getTime("start_time").toLocalTime(),
                result.getTime("end_time").toLocalTime(), result.getString("location"));
    }
}
