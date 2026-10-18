package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimetableDao {
    record TimetableRow(int id, String title, int departmentId, int semesterId) {
    }

    record EntryRow(
            int id,
            int timetableId,
            int offeringId,
            int componentId,
            String dayOfWeek,
            LocalTime start,
            LocalTime end,
            String location) {
    }

    long insertTimetable(Connection connection, TimetableRow row);

    long insertEntry(Connection connection, EntryRow row);

    int updateEntry(Connection connection, EntryRow row);

    Optional<TimetableRow> findTimetable(int timetableId);

    Optional<EntryRow> findEntry(int entryId);

    List<EntryRow> findEntries(int departmentId, int semesterId, String dayOfWeek);

    List<EntryRow> findConflicts(
            int departmentId, int semesterId, EntryRow candidate, int lecturerId);

    boolean entryMatchesOfferingAndComponent(
            int entryId, int offeringId, int componentId, java.time.LocalDate sessionDate);

    boolean offeringMatchesTimetable(int timetableId, int offeringId, int componentId);
}
