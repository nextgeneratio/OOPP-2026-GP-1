package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;

public interface AttendanceDao {
    record SessionRow(
            int id, int offeringId, int componentId, LocalDate date, Integer timetableEntryId,
            Integer recordedBy, Integer recordedByLecturer) {
        public SessionRow(
                int id, int offeringId, int componentId, LocalDate date,
                Integer timetableEntryId, int recordedBy) {
            this(id, offeringId, componentId, date, timetableEntryId, recordedBy, null);
        }
    }

    record AttendanceRow(int id, int sessionId, int enrollmentId, boolean present) {
    }

    record AttendanceHistoryRow(
            int sessionId,
            int enrollmentId,
            int componentId,
            ComponentType componentType,
            LocalDate sessionDate,
            boolean present) {
    }

    long insertSession(Connection connection, SessionRow row);

    long insertRecord(Connection connection, AttendanceRow row);

    int reviseRecord(Connection connection, AttendanceRow row);

    int deleteSession(Connection connection, int sessionId);

    List<SessionRow> findSessions(int offeringId);

    java.util.Optional<SessionRow> findSessionsForSessionId(int sessionId);

    List<AttendanceRow> findRecordsForSession(int sessionId);

    List<AttendanceHistoryRow> findHistory(int enrollmentId, int offeringId);
}
