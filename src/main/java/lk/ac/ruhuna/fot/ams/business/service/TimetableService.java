package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.error.exception.NotFoundException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class TimetableService {
    private final TimetableDao timetableDao;
    private final CourseDao courseDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final Clock clock;

    public TimetableService(
            TimetableDao timetableDao,
            CourseDao courseDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            Clock clock) {
        this.timetableDao = Objects.requireNonNull(timetableDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
    }

    public long createTimetable(AuthenticatedSession actor, TimetableRow timetable) {
        authorization.requireAdmin(actor);
        if (timetable == null || timetable.title() == null || timetable.title().isBlank()
                || timetable.departmentId() <= 0 || timetable.semesterId() <= 0) {
            throw new ValidationException("Enter valid timetable details.");
        }
        return transactions.execute(connection -> {
            long id = timetableDao.insertTimetable(connection, timetable);
            audit(connection, actor.userId(), "TIMETABLE_CREATE", id);
            return id;
        });
    }

    public long addEntry(AuthenticatedSession actor, EntryRow entry) {
        authorization.requireAdmin(actor);
        validateEntry(entry);
        return transactions.execute(connection -> {
            checkConflicts(entry);
            long id = timetableDao.insertEntry(connection, entry);
            audit(connection, actor.userId(), "TIMETABLE_ENTRY_CREATE", id);
            return id;
        });
    }

    public void updateEntry(AuthenticatedSession actor, EntryRow entry) {
        authorization.requireAdmin(actor);
        validateEntry(entry);
        transactions.execute(connection -> {
            EntryRow existing = timetableDao.findEntry(entry.id())
                    .orElseThrow(() -> new NotFoundException("Timetable entry was not found."));
            if (existing.timetableId() != entry.timetableId()) {
                throw new ValidationException("A timetable entry cannot be moved to another timetable.");
            }
            checkConflicts(entry);
            timetableDao.updateEntry(connection, entry);
            audit(connection, actor.userId(), "TIMETABLE_ENTRY_UPDATE", entry.id());
            return null;
        });
    }

    private void checkConflicts(EntryRow entry) {
        TimetableRow timetable = timetableDao.findTimetable(entry.timetableId())
                .orElseThrow(() -> new NotFoundException("Timetable was not found."));
        if (!timetableDao.offeringMatchesTimetable(entry.timetableId(), entry.offeringId(), entry.componentId())) {
            throw new ValidationException("Offering and component do not match this department/semester timetable.");
        }
        List<Integer> lecturerIds = courseDao.findLecturersForOffering(entry.offeringId());
        if (lecturerIds.isEmpty()) {
            lecturerIds = List.of(0);
        }
        List<EntryRow> conflicts = lecturerIds.stream()
                .flatMap(lecturerId -> timetableDao.findConflicts(
                        timetable.departmentId(), timetable.semesterId(), entry, lecturerId).stream())
                .filter(existing -> existing.id() != entry.id())
                .distinct()
                .toList();
        if (!conflicts.isEmpty()) {
            throw new ValidationException(
                    "The timetable entry conflicts with an existing room, lecturer, or course slot.");
        }
    }

    private void validateEntry(EntryRow entry) {
        if (entry == null || entry.timetableId() <= 0 || entry.offeringId() <= 0 || entry.componentId() <= 0
                || entry.dayOfWeek() == null || entry.start() == null || entry.end() == null
                || entry.location() == null || entry.location().isBlank()
                || !entry.start().isBefore(entry.end())
                || !List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").contains(entry.dayOfWeek())) {
            throw new ValidationException("Enter a valid timetable entry.");
        }
    }

    private void audit(java.sql.Connection connection, long userId, String action, long entityId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, userId, action, "TIMETABLE", entityId, LocalDateTime.now(clock)));
    }
}
