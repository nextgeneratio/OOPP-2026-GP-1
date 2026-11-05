package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public interface TimetableDao {
    record TimetableRow(int id, String title, int departmentId, int semesterId, int level,
                        Long createdByUserId, String createdByUsername, Role createdByRole, int offeringId) {
        public TimetableRow(int id,String title,int departmentId,int semesterId,int level,
                Long createdByUserId,String createdByUsername,Role createdByRole){
            this(id,title,departmentId,semesterId,level,createdByUserId,createdByUsername,createdByRole,0);
        }
        public TimetableRow(int id, String title, int departmentId, int semesterId, int level) {
            this(id, title, departmentId, semesterId, level, null, null, null,0);
        }
        public TimetableRow(int id, String title, int departmentId, int semesterId) {
            this(id, title, departmentId, semesterId, 1, null, null, null,0);
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

    List<EntryRow> findEntriesForTimetable(int timetableId);

    int deactivateEntriesForTimetable(Connection connection, int timetableId);

    int deactivateTimetable(Connection connection, int timetableId);

    boolean isTimetableActive(int timetableId);

    int countAttendanceReferences(int timetableId);

    int updateEntry(Connection connection, EntryRow row);
    int deactivateEntry(Connection connection, int entryId);
    int deleteEntriesForTimetable(Connection connection, int timetableId);
    int deleteTimetable(Connection connection, int timetableId);

    Optional<TimetableRow> findTimetable(int timetableId);

    List<TimetableRow> findTimetables(int departmentId, int semesterId, int level);

    Optional<TimetableRow> findActiveTimetableForOffering(int offeringId);

    List<TimetableRow> findTimetablesForOfferings(List<Integer> offeringIds);

    Optional<EntryRow> findEntry(int entryId);

    List<EntryRow> findEntries(int departmentId, int semesterId, int level, String dayOfWeek);

    List<EntryRow> findEntries(int departmentId,int semesterId);

    List<EntryRow> findEntriesForOfferings(List<Integer> offeringIds);

    List<EntryRow> findConflicts(
            int departmentId, int semesterId, EntryRow candidate, int lecturerId);

    boolean entryMatchesOfferingAndComponent(
            int entryId, int offeringId, int componentId, java.time.LocalDate sessionDate);

    boolean offeringMatchesTimetable(int timetableId, int offeringId, int componentId);
}
