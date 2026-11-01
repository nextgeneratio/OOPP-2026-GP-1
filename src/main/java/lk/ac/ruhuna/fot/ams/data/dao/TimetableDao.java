package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public interface TimetableDao {
    record TimetableRow(int id, String title, int departmentId, int semesterId, int level,
                        Long createdByUserId, String createdByUsername, Role createdByRole) {
        public TimetableRow(int id, String title, int departmentId, int semesterId, int level) {
            this(id, title, departmentId, semesterId, level, null, null, null);
        }
        public TimetableRow(int id, String title, int departmentId, int semesterId) {
            this(id, title, departmentId, semesterId, 1, null, null, null);
        }
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
    int deleteEntriesForTimetable(Connection connection, int timetableId);
    int deleteTimetable(Connection connection, int timetableId);

    Optional<TimetableRow> findTimetable(int timetableId);

    List<TimetableRow> findTimetables(int departmentId, int semesterId, int level);

    Optional<EntryRow> findEntry(int entryId);

    List<EntryRow> findEntries(int departmentId, int semesterId, int level, String dayOfWeek);

    List<EntryRow> findEntriesForOfferings(List<Integer> offeringIds);

    List<EntryRow> findConflicts(
            int departmentId, int semesterId, EntryRow candidate, int lecturerId);

    boolean entryMatchesOfferingAndComponent(
            int entryId, int offeringId, int componentId, java.time.LocalDate sessionDate);

    boolean offeringMatchesTimetable(int timetableId, int offeringId, int componentId);
}
