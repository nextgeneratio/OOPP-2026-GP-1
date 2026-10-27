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
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
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
        authorizeTimetableManager(actor, timetable == null ? 0 : timetable.departmentId(),
                timetable == null ? 0 : timetable.semesterId());
        if (timetable == null || timetable.title() == null || timetable.title().isBlank()
                || timetable.departmentId() <= 0 || timetable.semesterId() <= 0
                || timetable.level() < 1 || timetable.level() > 4) {
            throw new ValidationException("Enter valid timetable details.");
        }
        return transactions.execute(connection -> {
            TimetableRow owned = new TimetableRow(timetable.id(), timetable.title().trim(), timetable.departmentId(),
                    timetable.semesterId(), timetable.level(), actor.userId(), null, null);
            long id = timetableDao.insertTimetable(connection, owned);
            audit(connection, actor.userId(), "TIMETABLE_CREATE", id);
            return id;
        });
    }

    public long addEntry(AuthenticatedSession actor, EntryRow entry) {
        authorizeTimetableEntryManager(actor, entry == null ? 0 : entry.timetableId());
        validateEntry(entry);
        return transactions.execute(connection -> {
            checkConflicts(entry);
            long id = timetableDao.insertEntry(connection, entry);
            audit(connection, actor.userId(), "TIMETABLE_ENTRY_CREATE", id);
            return id;
        });
    }

    public void updateEntry(AuthenticatedSession actor, EntryRow entry) {
        authorizeTimetableEntryManager(actor, entry == null ? 0 : entry.timetableId());
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

    public List<EntryRow> listEntries(
            AuthenticatedSession actor, int departmentId, int semesterId, int level, String day) {
        if (actor == null || departmentId <= 0 || semesterId <= 0 || level < 1 || level > 4) {
            throw new ValidationException("A valid department, semester, and level are required.");
        }
        if (day != null && !List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").contains(day)) {
            throw new ValidationException("Timetable day must be MON, TUE, WED, THU, FRI, SAT, or SUN.");
        }
        List<EntryRow> entries = timetableDao.findEntries(departmentId, semesterId, level, day);
        if (actor.role() == Role.ADMIN) {
            return entries;
        }
        if (actor.role() == Role.TECHNICAL_OFFICER) {
            entries.forEach(entry -> authorization.requireTechnicalOfficerForOffering(actor, entry.offeringId()));
            return entries;
        }
        if (actor.role() == Role.LECTURER) {
            int lecturerId = authorization.lecturerId(actor);
            List<Integer> offeringIds = courseDao.findOfferingsForLecturer(lecturerId, semesterId).stream()
                    .map(CourseDao.OfferingRow::id).toList();
            return entries.stream().filter(entry -> offeringIds.contains(entry.offeringId())).toList();
        }
        if (actor.role() == Role.UNDERGRADUATE) {
            int studentId = authorization.studentId(actor);
            return entries.stream().filter(entry -> courseDao.findActiveEnrollments(entry.offeringId()).stream()
                    .anyMatch(enrollment -> enrollment.studentId() == studentId)).toList();
        }
        throw new AuthorizationException("You do not have permission to view this timetable.");
    }

    /** Signed-in non-admin users never supply department, semester, or level identifiers. */
    public List<EntryRow> myEntries(AuthenticatedSession actor) {
        if (actor == null || actor.role() == Role.ADMIN) {
            throw new AuthorizationException("Choose a department timetable from the administrator view.");
        }
        List<Integer> offeringIds = switch (actor.role()) {
            case UNDERGRADUATE -> courseDao.findOfferingsForStudent(authorization.studentId(actor)).stream()
                    .map(CourseDao.OfferingRow::id).toList();
            case LECTURER -> courseDao.findOfferingsForLecturer(authorization.lecturerId(actor), null).stream()
                    .map(CourseDao.OfferingRow::id).toList();
            case TECHNICAL_OFFICER -> courseDao.findOfferingsForTechnicalOfficer(actor.userId()).stream()
                    .map(CourseDao.OfferingRow::id).toList();
            default -> List.of();
        };
        return timetableDao.findEntriesForOfferings(offeringIds);
    }

    public TimetableRow timetableDetails(AuthenticatedSession actor, int timetableId) {
        TimetableRow timetable = timetableDao.findTimetable(timetableId)
                .orElseThrow(() -> new NotFoundException("Timetable was not found."));
        if (actor != null && actor.role() == Role.ADMIN) {
            authorization.requireAdmin(actor);
            return timetable;
        }
        authorization.requireTechnicalOfficerForTimetable(actor, timetable.departmentId(), timetable.semesterId());
        return timetable;
    }

    public void deleteTimetable(AuthenticatedSession actor, int timetableId) {
        authorization.requireAdmin(actor);
        if (timetableId <= 0 || timetableDao.findTimetable(timetableId).isEmpty()) {
            throw new ValidationException("Timetable was not found.");
        }
        transactions.execute(connection -> {
            timetableDao.deleteEntriesForTimetable(connection, timetableId);
            int deleted = timetableDao.deleteTimetable(connection, timetableId);
            if (deleted != 1) {
                throw new ValidationException("Timetable was not found.");
            }
            audit(connection, actor.userId(), "TIMETABLE_DELETE", timetableId);
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

    private void authorizeTimetableManager(AuthenticatedSession actor, int departmentId, int semesterId) {
        if (actor != null && actor.role() == Role.ADMIN) {
            authorization.requireAdmin(actor);
            return;
        }
        authorization.requireTechnicalOfficerForTimetable(actor, departmentId, semesterId);
    }

    private void authorizeTimetableEntryManager(AuthenticatedSession actor, int timetableId) {
        TimetableRow timetable = timetableDao.findTimetable(timetableId)
                .orElseThrow(() -> new NotFoundException("Timetable was not found."));
        authorizeTimetableManager(actor, timetable.departmentId(), timetable.semesterId());
        if (actor.role() == Role.TECHNICAL_OFFICER
                && !Long.valueOf(actor.userId()).equals(timetable.createdByUserId())) {
            throw new AuthorizationException("You may only change timetables that you created.");
        }
    }

    private void audit(java.sql.Connection connection, long userId, String action, long entityId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, userId, action, "TIMETABLE", entityId, LocalDateTime.now(clock)));
    }
}
