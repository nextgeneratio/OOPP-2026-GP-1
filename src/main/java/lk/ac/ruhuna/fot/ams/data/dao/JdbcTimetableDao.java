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
                "INSERT INTO timetables (title, department_id, semester_id, level_no, created_by_user_id, is_active, course_offering_id) VALUES (?, ?, ?, ?, ?, TRUE, ?)",
                statement -> {
                    statement.setString(1, row.title());
                    statement.setInt(2, row.departmentId());
                    statement.setInt(3, row.semesterId());
                    statement.setInt(4, row.level());
                    if (row.createdByUserId() == null) {
                        statement.setNull(5, java.sql.Types.BIGINT);
                    } else {
                        statement.setLong(5, row.createdByUserId());
                    }
                    if(row.offeringId()<=0) statement.setNull(6,java.sql.Types.INTEGER); else statement.setInt(6,row.offeringId());
                });
    }

    @Override
    public long insertEntry(Connection connection, EntryRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO timetable_entries "
                        + "(timetable_id, offering_id, component_id, day_of_week, start_time, end_time, location, is_active) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)",
                statement -> bindEntry(statement, row, false));
    }

    @Override
    public List<EntryRow> findEntriesForTimetable(int timetableId) {
        return jdbc.query("SELECT timetable_entry_id,timetable_id,offering_id,component_id,day_of_week,start_time,end_time,location "
                        + "FROM timetable_entries WHERE timetable_id=? AND is_active=TRUE ORDER BY FIELD(day_of_week,'MON','TUE','WED','THU','FRI','SAT','SUN'),start_time",
                statement -> statement.setInt(1, timetableId), this::map);
    }

    @Override
    public int deactivateEntriesForTimetable(Connection connection, int timetableId) {
        return jdbc.update(connection, "UPDATE timetable_entries SET is_active=FALSE WHERE timetable_id=? AND is_active=TRUE",
                statement -> statement.setInt(1, timetableId));
    }

    @Override
    public int deactivateTimetable(Connection connection, int timetableId) {
        return jdbc.update(connection, "UPDATE timetables SET is_active=FALSE WHERE timetable_id=? AND is_active=TRUE",
                statement -> statement.setInt(1, timetableId));
    }

    @Override
    public boolean isTimetableActive(int timetableId) {
        return !jdbc.query("SELECT timetable_id FROM timetables WHERE timetable_id=? AND is_active=TRUE",
                statement -> statement.setInt(1, timetableId), result -> result.getInt(1)).isEmpty();
    }

    @Override
    public int countAttendanceReferences(int timetableId) {
        return jdbc.query("SELECT COUNT(*) FROM attendance_sessions s JOIN timetable_entries e ON e.timetable_entry_id=s.timetable_entry_id WHERE e.timetable_id=?",
                statement -> statement.setInt(1, timetableId), result -> result.getInt(1)).stream().findFirst().orElse(0);
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
    public int deactivateEntry(Connection connection,int entryId){
        return jdbc.update(connection,"UPDATE timetable_entries SET is_active=FALSE WHERE timetable_entry_id=? AND is_active=TRUE",statement->statement.setInt(1,entryId));
    }

    @Override
    public int deleteEntriesForTimetable(Connection connection, int timetableId) {
        return jdbc.update(connection, "DELETE FROM timetable_entries WHERE timetable_id = ?",
                statement -> statement.setInt(1, timetableId));
    }

    @Override
    public int deleteTimetable(Connection connection, int timetableId) {
        return jdbc.update(connection, "DELETE FROM timetables WHERE timetable_id = ?",
                statement -> statement.setInt(1, timetableId));
    }

    @Override
    public Optional<TimetableRow> findTimetable(int timetableId) {
        return jdbc.query("SELECT t.timetable_id, t.title, t.department_id, t.semester_id, t.level_no, t.course_offering_id, "
                        + "t.created_by_user_id, u.username, u.role FROM timetables t "
                        + "LEFT JOIN users u ON u.user_id = t.created_by_user_id WHERE t.timetable_id = ?",
                statement -> statement.setInt(1, timetableId),
                result -> new TimetableRow(result.getInt("timetable_id"), result.getString("title"),
                        result.getInt("department_id"), result.getInt("semester_id"),
                        result.getInt("level_no"), nullableLong(result, "created_by_user_id"),
                        result.getString("username"), role(result.getString("role")),nullableInt(result,"course_offering_id")))
                .stream().findFirst();
    }

    @Override
    public List<TimetableRow> findTimetables(int departmentId, int semesterId, int level) {
        return jdbc.query("SELECT t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no,t.course_offering_id,"
                        + "t.created_by_user_id,u.username,u.role FROM timetables t "
                        + "LEFT JOIN users u ON u.user_id=t.created_by_user_id "
                        + "WHERE t.department_id=? AND t.semester_id=? AND t.level_no=? AND t.is_active=TRUE ORDER BY t.title",
                statement -> { statement.setInt(1,departmentId); statement.setInt(2,semesterId); statement.setInt(3,level); },
                result -> new TimetableRow(result.getInt("timetable_id"),result.getString("title"),
                        result.getInt("department_id"),result.getInt("semester_id"),result.getInt("level_no"),
                        result.getObject("created_by_user_id",Long.class),result.getString("username"),
                        result.getString("role")==null?null:lk.ac.ruhuna.fot.ams.domain.enums.Role.valueOf(result.getString("role")),nullableInt(result,"course_offering_id")));
    }

    @Override
    public Optional<TimetableRow> findActiveTimetableForOffering(int offeringId){
        return jdbc.query("SELECT t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no,t.course_offering_id,t.created_by_user_id,u.username,u.role FROM timetables t LEFT JOIN users u ON u.user_id=t.created_by_user_id WHERE t.course_offering_id=? AND t.is_active=TRUE",
                statement->statement.setInt(1,offeringId),result->new TimetableRow(result.getInt("timetable_id"),result.getString("title"),result.getInt("department_id"),result.getInt("semester_id"),result.getInt("level_no"),nullableLong(result,"created_by_user_id"),result.getString("username"),role(result.getString("role")),nullableInt(result,"course_offering_id"))).stream().findFirst();
    }

    @Override
    public List<TimetableRow> findTimetablesForOfferings(List<Integer> offeringIds){
        if(offeringIds==null||offeringIds.isEmpty())return List.of();String p=String.join(",",java.util.Collections.nCopies(offeringIds.size(),"?"));
        return jdbc.query("SELECT t.timetable_id,t.title,t.department_id,t.semester_id,t.level_no,t.course_offering_id,t.created_by_user_id,u.username,u.role FROM timetables t LEFT JOIN users u ON u.user_id=t.created_by_user_id WHERE t.is_active=TRUE AND t.course_offering_id IN ("+p+") ORDER BY t.title",
                statement->{for(int i=0;i<offeringIds.size();i++)statement.setInt(i+1,offeringIds.get(i));},result->new TimetableRow(result.getInt("timetable_id"),result.getString("title"),result.getInt("department_id"),result.getInt("semester_id"),result.getInt("level_no"),nullableLong(result,"created_by_user_id"),result.getString("username"),role(result.getString("role")),nullableInt(result,"course_offering_id")));
    }

    @Override
    public Optional<EntryRow> findEntry(int entryId) {
        return jdbc.query("SELECT timetable_entry_id, timetable_id, offering_id, component_id, "
                        + "day_of_week, start_time, end_time, location "
                        + "FROM timetable_entries WHERE timetable_entry_id = ?",
                statement -> statement.setInt(1, entryId), this::map).stream().findFirst();
    }

    @Override
    public List<EntryRow> findEntries(int departmentId, int semesterId, int level, String dayOfWeek) {
        String sql = "SELECT e.timetable_entry_id, e.timetable_id, e.offering_id, e.component_id, "
                + "e.day_of_week, e.start_time, e.end_time, e.location "
                + "FROM timetable_entries e JOIN timetables t ON t.timetable_id = e.timetable_id "
                + "WHERE t.department_id = ? AND t.semester_id = ? AND t.level_no = ? AND t.is_active=TRUE AND e.is_active=TRUE "
                + "AND (? IS NULL OR e.day_of_week = ?) ORDER BY e.day_of_week, e.start_time";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, departmentId);
            statement.setInt(2, semesterId);
            statement.setInt(3, level);
            if (dayOfWeek == null) {
                statement.setNull(4, java.sql.Types.VARCHAR);
                statement.setNull(5, java.sql.Types.VARCHAR);
            } else {
                statement.setString(4, dayOfWeek);
                statement.setString(5, dayOfWeek);
            }
        }, this::map);
    }

    @Override
    public List<EntryRow> findEntries(int departmentId,int semesterId){
        String sql="SELECT e.timetable_entry_id,e.timetable_id,e.offering_id,e.component_id,e.day_of_week,e.start_time,e.end_time,e.location "
                +"FROM timetable_entries e JOIN timetables t ON t.timetable_id=e.timetable_id "
                +"WHERE t.department_id=? AND t.semester_id=? AND t.is_active=TRUE AND e.is_active=TRUE ORDER BY e.day_of_week,e.start_time";
        return jdbc.query(sql,statement->{statement.setInt(1,departmentId);statement.setInt(2,semesterId);},this::map);
    }

    @Override
    public List<EntryRow> findEntriesForOfferings(List<Integer> offeringIds) {
        if (offeringIds == null || offeringIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(offeringIds.size(), "?"));
        String sql = "SELECT DISTINCT e.timetable_entry_id, e.timetable_id, e.offering_id, e.component_id, "
                + "e.day_of_week, e.start_time, e.end_time, e.location FROM timetable_entries e "
                + "JOIN timetables t ON t.timetable_id=e.timetable_id "
                + "WHERE t.is_active=TRUE AND e.is_active=TRUE AND ((e.offering_id IN (" + placeholders + ")) "
                + "OR (e.offering_id IS NULL AND EXISTS (SELECT 1 FROM course_offerings so JOIN batches sb ON sb.batch_id=so.batch_id "
                + "WHERE so.offering_id IN (" + placeholders + ") AND so.semester_id=t.semester_id "
                + "AND sb.department_id=t.department_id AND sb.level_no=t.level_no))) "
                + "ORDER BY FIELD(e.day_of_week,'MON','TUE','WED','THU','FRI','SAT','SUN'), e.start_time";
        return jdbc.query(sql, statement -> {
            int at=1; for (int repeat=0; repeat<2; repeat++) for (int id : offeringIds) statement.setInt(at++, id);
        }, this::map);
    }

    @Override
    public List<EntryRow> findConflicts(
            int departmentId, int semesterId, EntryRow candidate, int lecturerId) {
        String sql = "SELECT e.timetable_entry_id, e.timetable_id, e.offering_id, e.component_id, "
                + "e.day_of_week, e.start_time, e.end_time, e.location "
                + "FROM timetable_entries e JOIN timetables t ON t.timetable_id = e.timetable_id "
                + "JOIN course_offerings o ON o.offering_id = e.offering_id "
                + "WHERE t.department_id = ? AND t.semester_id = ? AND e.day_of_week = ? AND t.is_active=TRUE AND e.is_active=TRUE "
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
        return !jdbc.query("SELECT e.timetable_entry_id FROM timetable_entries e "
                        + "JOIN timetables t ON t.timetable_id=e.timetable_id "
                        + "JOIN course_offerings o ON o.offering_id=? JOIN batches b ON b.batch_id=o.batch_id "
                        + "WHERE e.timetable_entry_id = ? AND t.is_active=TRUE AND e.is_active=TRUE "
                        + "AND ( (e.offering_id = ? AND e.component_id = ?) OR (e.offering_id IS NULL AND e.component_id IS NULL "
                        + "AND t.department_id=b.department_id AND t.semester_id=o.semester_id AND t.level_no=b.level_no) ) "
                        + "AND day_of_week = CASE DAYOFWEEK(?) "
                        + "WHEN 1 THEN 'SUN' WHEN 2 THEN 'MON' WHEN 3 THEN 'TUE' "
                        + "WHEN 4 THEN 'WED' WHEN 5 THEN 'THU' WHEN 6 THEN 'FRI' ELSE 'SAT' END",
                statement -> {
                    statement.setInt(1, offeringId);
                    statement.setInt(2, entryId);
                    statement.setInt(3, offeringId);
                    statement.setInt(4, componentId);
                    statement.setDate(5, java.sql.Date.valueOf(sessionDate));
                }, result -> result.getInt(1)).isEmpty();
    }

    @Override
    public boolean offeringMatchesTimetable(int timetableId, int offeringId, int componentId) {
        return !jdbc.query("SELECT e.timetable_id FROM course_offerings o "
                        + "JOIN courses c ON c.course_id = o.course_id "
                        + "JOIN batches b ON b.batch_id = o.batch_id "
                        + "JOIN course_components cc ON cc.course_id = c.course_id "
                        + "JOIN timetables t ON t.timetable_id = ? "
                        + "WHERE o.offering_id = ? AND cc.component_id = ? AND t.course_offering_id=o.offering_id "
                        + "AND t.department_id = b.department_id AND t.semester_id = o.semester_id "
                        + "AND t.level_no = b.level_no",
                statement -> {
                    statement.setInt(1, timetableId);
                    statement.setInt(2, offeringId);
                    statement.setInt(3, componentId);
                }, result -> result.getInt(1)).isEmpty();
    }

    private void bindEntry(java.sql.PreparedStatement statement, EntryRow row, boolean includeId)
            throws SQLException {
        statement.setInt(1, row.timetableId());
        if(row.offeringId()<=0) statement.setNull(2,java.sql.Types.INTEGER); else statement.setInt(2,row.offeringId());
        if(row.componentId()<=0) statement.setNull(3,java.sql.Types.INTEGER); else statement.setInt(3,row.componentId());
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
                nullableInt(result,"offering_id"), nullableInt(result,"component_id"),
                result.getString("day_of_week"), result.getTime("start_time").toLocalTime(),
                result.getTime("end_time").toLocalTime(), result.getString("location"));
    }

    private int nullableInt(ResultSet result,String column) throws SQLException {
        int value=result.getInt(column); return result.wasNull()?0:value;
    }

    private Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private lk.ac.ruhuna.fot.ams.domain.enums.Role role(String value) {
        return value == null ? null : lk.ac.ruhuna.fot.ams.domain.enums.Role.valueOf(value);
    }
}
