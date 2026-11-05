package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
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
        authorization.requireAdmin(actor);
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

    public long createWeekly(AuthenticatedSession actor, TimetableRow timetable, List<EntryRow> slots) {
        authorization.requireAdmin(actor);
        validateHeader(timetable);
        validateWeeklySlots(slots);
        int offeringId=timetable.offeringId();
        if(offeringId<=0) throw new ValidationException("Choose a course offering for this timetable.");
        validateOfferingScope(timetable,offeringId,slots);
        if(timetableDao.findActiveTimetableForOffering(offeringId).isPresent()) throw new ValidationException("This course offering already has an active timetable. Use Update Timetable instead.");
        return transactions.execute(connection -> {
            TimetableRow owned = new TimetableRow(timetable.id(), timetable.title().trim(), timetable.departmentId(),
                    timetable.semesterId(), timetable.level(), actor.userId(), null, null,offeringId);
            long timetableId=timetableDao.insertTimetable(connection, owned);
            List<EntryRow> materialized=slots.stream().map(s -> new EntryRow(0,(int)timetableId,offeringId,s.componentId(),s.dayOfWeek(),s.start(),s.end(),blankToNull(s.location()))).toList();
            checkCourseConflicts(timetable,materialized,0);
            for(EntryRow slot:materialized) timetableDao.insertEntry(connection,slot);
            audit(connection,actor.userId(),"TIMETABLE_CREATE",timetableId);
            return timetableId;
        });
    }

    public void updateWeekly(AuthenticatedSession actor, int timetableId, List<EntryRow> slots) {
        authorization.requireAdmin(actor);
        TimetableRow header=timetableDao.findTimetable(timetableId).orElseThrow(()->new NotFoundException("Timetable was not found."));
        if(!timetableDao.isTimetableActive(timetableId)) throw new ValidationException("This timetable is inactive.");
        validateWeeklySlots(slots);
        if(header.offeringId()<=0) throw new ValidationException("This legacy timetable is unassigned. Create a course timetable to manage this offering.");
        validateOfferingScope(header,header.offeringId(),slots);
        transactions.execute(connection -> {
            List<EntryRow> materialized=slots.stream().map(s -> new EntryRow(0,timetableId,header.offeringId(),s.componentId(),s.dayOfWeek(),s.start(),s.end(),blankToNull(s.location()))).toList();
            checkCourseConflicts(header,materialized,timetableId);
            timetableDao.deactivateEntriesForTimetable(connection,timetableId);
            for(EntryRow slot:materialized) timetableDao.insertEntry(connection,slot);
            audit(connection,actor.userId(),"TIMETABLE_UPDATE",timetableId);
            return null;
        });
    }

    private void validateOfferingScope(TimetableRow timetable,int offeringId,List<EntryRow> slots){
        CourseDao.OfferingRow offering=courseDao.findOffering(offeringId).orElseThrow(()->new ValidationException("Course offering was not found."));
        CourseDao.BatchRow batch=courseDao.findBatch(offering.batchId()).orElseThrow(()->new ValidationException("Offering batch was not found."));
        CourseDao.CourseRow course=courseDao.findCourse(offering.courseId()).orElseThrow(()->new ValidationException("Offering course was not found."));
        if(batch.departmentId()!=timetable.departmentId()||offering.semesterId()!=timetable.semesterId()||batch.level()!=timetable.level()||course.departmentId()!=batch.departmentId())
            throw new ValidationException("Course offering does not match the selected department, academic term, and level.");
        List<Integer> componentIds=courseDao.findComponents(course.id()).stream().map(CourseDao.ComponentRow::id).toList();
        if(slots.stream().anyMatch(slot->slot.componentId()<=0||!componentIds.contains(slot.componentId())))
            throw new ValidationException("Every timetable slot must select a component belonging to the selected course.");
    }

    private void checkCourseConflicts(TimetableRow scope,List<EntryRow> proposed,int excludeTimetableId){
        List<EntryRow> existing=timetableDao.findEntries(scope.departmentId(),scope.semesterId()).stream().filter(e->e.timetableId()!=excludeTimetableId).toList();
        int selectedBatch=courseDao.findOffering(scope.offeringId()).map(CourseDao.OfferingRow::batchId).orElse(-1);
        List<Integer> lecturers=courseDao.findLecturersForOffering(scope.offeringId());
        for(EntryRow candidate:proposed)for(EntryRow old:existing){
            TimetableRow oldHeader=timetableDao.findTimetable(old.timetableId()).orElse(null);if(oldHeader==null)continue;
            int oldOffering=oldHeader.offeringId()>0?oldHeader.offeringId():old.offeringId();if(oldOffering<=0)continue;
            CourseDao.OfferingRow other=courseDao.findOffering(oldOffering).orElse(null);if(other==null)continue;
            boolean overlaps=overlaps(candidate,old);if(!overlaps)continue;
            boolean sameRoom=candidate.location()!=null&&!candidate.location().isBlank()&&candidate.location().equalsIgnoreCase(old.location()==null?"":old.location());
            boolean sameOffering=scope.offeringId()==oldOffering;
            boolean sameBatch=other.batchId()==selectedBatch;
            List<Integer> otherLecturers=courseDao.findLecturersForOffering(oldOffering);
            boolean lecturerOverlap=lecturers.stream().anyMatch(otherLecturers::contains);
            if(sameRoom||sameOffering||sameBatch||lecturerOverlap)
                throw new ValidationException(sameRoom?"The selected room is already allocated at this time.":lecturerOverlap?"An assigned lecturer is already teaching at this time.":"This course offering or batch already has a timetable slot at this time.");
        }
    }

    private void validateHeader(TimetableRow timetable) {
        if(timetable==null||timetable.title()==null||timetable.title().isBlank()||timetable.departmentId()<=0||timetable.semesterId()<=0||timetable.level()<1||timetable.level()>4)
            throw new ValidationException("Choose a department, academic term, and level.");
    }

    private void validateWeeklySlots(List<EntryRow> slots) {
        if(slots==null) throw new ValidationException("Weekly slots are required.");
        for(EntryRow slot:slots) if(slot==null||slot.dayOfWeek()==null||!List.of("MON","TUE","WED","THU","FRI","SAT","SUN").contains(slot.dayOfWeek())||slot.start()==null||slot.end()==null||!slot.start().isBefore(slot.end()))
            throw new ValidationException("Each slot needs a valid weekday, start time, and later end time.");
        for(int i=0;i<slots.size();i++) for(int j=i+1;j<slots.size();j++) if(overlaps(slots.get(i),slots.get(j)))
            throw new ValidationException("Weekly timetable slots overlap on "+slots.get(i).dayOfWeek()+".");
    }

    private boolean overlaps(EntryRow a,EntryRow b){return a.dayOfWeek().equals(b.dayOfWeek())&&a.start().isBefore(b.end())&&b.start().isBefore(a.end());}
    private String blankToNull(String value){return value==null||value.isBlank()?null:value.trim();}

    private void checkWeeklyConflicts(int timetableId,TimetableRow scope,List<EntryRow> proposed,int excludedTimetableId){
        List<EntryRow> existing=timetableDao.findEntries(scope.departmentId(),scope.semesterId()).stream()
                .filter(e->e.timetableId()!=excludedTimetableId).toList();
        for(EntryRow candidate:proposed) for(EntryRow old:existing) {
            boolean timeOverlap=overlaps(candidate,old);
            boolean sameRoom=candidate.location()!=null&&!candidate.location().isBlank()&&candidate.location().equalsIgnoreCase(old.location()==null?"":old.location());
            TimetableRow oldScope=timetableDao.findTimetable(old.timetableId()).orElse(null);
            boolean sameCohort=oldScope!=null&&oldScope.level()==scope.level();
            if(timeOverlap&&sameCohort||timeOverlap&&sameRoom)
                throw new ValidationException("The proposed timetable conflicts with another active timetable for this department, term, and level.");
        }
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
            timetableDao.deactivateEntry(connection,entry.id());
            timetableDao.insertEntry(connection,new EntryRow(0,entry.timetableId(),entry.offeringId(),entry.componentId(),entry.dayOfWeek(),entry.start(),entry.end(),blankToNull(entry.location())));
            audit(connection, actor.userId(), "TIMETABLE_ENTRY_VERSION", entry.id());
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
            authorization.requireAdmin(actor);
            return entries.stream().filter(e->e.offeringId()>0).toList();
        }
        if (actor.role() == Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForTimetable(actor,departmentId,semesterId);
            return entries;
        }
        if (actor.role() == Role.LECTURER) {
            int lecturerId = authorization.lecturerId(actor);
            List<Integer> offeringIds = courseDao.findOfferingsForLecturer(lecturerId, semesterId).stream()
                    .map(CourseDao.OfferingRow::id).toList();
            List<EntryRow> scoped=timetableDao.findEntriesForOfferings(offeringIds);
            var ids=scoped.stream().map(EntryRow::id).collect(java.util.stream.Collectors.toSet());
            return entries.stream().filter(entry -> ids.contains(entry.id())).toList();
        }
        if (actor.role() == Role.UNDERGRADUATE) {
            int studentId = authorization.studentId(actor);
            List<EntryRow> scoped=timetableDao.findEntriesForOfferings(courseDao.findOfferingsForStudent(studentId).stream().map(CourseDao.OfferingRow::id).toList());
            var ids=scoped.stream().map(EntryRow::id).collect(java.util.stream.Collectors.toSet());
            return entries.stream().filter(entry -> ids.contains(entry.id()) || entry.offeringId()>0 && courseDao.findActiveEnrollments(entry.offeringId()).stream()
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

    public List<TimetableRow> timetables(AuthenticatedSession actor, int departmentId, int semesterId, int level) {
        if (departmentId <= 0 || semesterId <= 0 || level < 1 || level > 4) {
            throw new ValidationException("Choose a valid department, semester, and level.");
        }
        if(actor.role()==Role.ADMIN) authorization.requireAdmin(actor);
        else authorization.requireTechnicalOfficerForTimetable(actor,departmentId,semesterId);
        return timetableDao.findTimetables(departmentId, semesterId, level);
    }

    public void deleteTimetable(AuthenticatedSession actor, int timetableId) {
        authorization.requireAdmin(actor);
        if (timetableId <= 0 || timetableDao.findTimetable(timetableId).isEmpty()) {
            throw new ValidationException("Timetable was not found.");
        }
        transactions.execute(connection -> {
            timetableDao.deactivateEntriesForTimetable(connection,timetableId);
            int deleted = timetableDao.deactivateTimetable(connection, timetableId);
            if (deleted != 1) {
                throw new ValidationException("Timetable was not found.");
            }
            audit(connection, actor.userId(), "TIMETABLE_DEACTIVATE", timetableId);
            return null;
        });
    }

    public List<EntryRow> entriesForTimetable(AuthenticatedSession actor,int timetableId){
        TimetableRow row=timetableDetails(actor,timetableId);
        if(actor.role()==Role.ADMIN) authorization.requireAdmin(actor);
        return timetableDao.findEntriesForTimetable(timetableId);
    }

    public int attendanceReferences(AuthenticatedSession actor,int timetableId){
        authorization.requireAdmin(actor);
        timetableDao.findTimetable(timetableId).orElseThrow(()->new NotFoundException("Timetable was not found."));
        return timetableDao.countAttendanceReferences(timetableId);
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

    private void authorizeTimetableEntryManager(AuthenticatedSession actor, int timetableId) {
        authorization.requireAdmin(actor);
        TimetableRow timetable = timetableDao.findTimetable(timetableId)
                .orElseThrow(() -> new NotFoundException("Timetable was not found."));
    }

    private void audit(java.sql.Connection connection, long userId, String action, long entityId)
            throws java.sql.SQLException {
        auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                0, userId, action, "TIMETABLE", entityId, LocalDateTime.now(clock)));
    }
}
