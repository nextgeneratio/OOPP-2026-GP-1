package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.service.TimetableService;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class TimetableController {
    public record WeeklyRequest(TimetableRow timetable, List<EntryRow> slots) { }
    public record WeeklyUpdateRequest(int timetableId, List<EntryRow> slots) { }
    public record EntryRequest(EntryRow entry) {
    }

    public record TimetableIdResponse(long id) {
    }
    public record TimetableDeletionSummary(int activeSlots,int attendanceReferences) { }

    private final TimetableService service;
    private final ApiControllerSupport support;

    public TimetableController(TimetableService service, ApiControllerSupport support) {
        this.service = Objects.requireNonNull(service);
        this.support = Objects.requireNonNull(support);
    }

    public ApiResponse<TimetableIdResponse> create(
            AuthenticatedSession actor, TimetableRow request) {
        return support.execute(request, () -> new TimetableIdResponse(service.createTimetable(actor, request)));
    }

    public ApiResponse<TimetableIdResponse> createWeekly(AuthenticatedSession actor,WeeklyRequest request){
        return support.execute(request,()->new TimetableIdResponse(service.createWeekly(actor,request.timetable(),request.slots())));
    }

    public ApiResponse<Void> updateWeekly(AuthenticatedSession actor,WeeklyUpdateRequest request){
        return support.execute(request,()->{service.updateWeekly(actor,request.timetableId(),request.slots());return null;});
    }

    public ApiResponse<List<EntryRow>> entriesForTimetable(AuthenticatedSession actor,int timetableId){
        return support.execute(()->service.entriesForTimetable(actor,timetableId));
    }

    public ApiResponse<TimetableDeletionSummary> deletionSummary(AuthenticatedSession actor,int timetableId){
        return support.execute(()->{var entries=service.entriesForTimetable(actor,timetableId);return new TimetableDeletionSummary(entries.size(),service.attendanceReferences(actor,timetableId));});
    }

    public ApiResponse<TimetableIdResponse> addEntry(
            AuthenticatedSession actor, EntryRequest request) {
        return support.execute(request, () -> new TimetableIdResponse(service.addEntry(actor, request.entry())));
    }

    public ApiResponse<Void> updateEntry(AuthenticatedSession actor, EntryRequest request) {
        return support.execute(request, () -> {
            service.updateEntry(actor, request.entry());
            return null;
        });
    }

    public ApiResponse<List<EntryRow>> listEntries(
            AuthenticatedSession actor, int departmentId, int semesterId, int level, String day) {
        return support.execute(() -> service.listEntries(actor, departmentId, semesterId, level, day));
    }

    public ApiResponse<List<EntryRow>> myEntries(AuthenticatedSession actor) {
        return support.execute(() -> service.myEntries(actor));
    }

    public ApiResponse<TimetableRow> details(AuthenticatedSession actor, int timetableId) {
        return support.execute(timetableId, () -> service.timetableDetails(actor, timetableId));
    }

    public ApiResponse<List<TimetableRow>> timetables(AuthenticatedSession actor, int departmentId,
            int semesterId, int level) {
        return support.execute(() -> service.timetables(actor, departmentId, semesterId, level));
    }

    public ApiResponse<Void> delete(AuthenticatedSession actor, int timetableId) {
        return support.execute(timetableId, () -> {
            service.deleteTimetable(actor, timetableId);
            return null;
        });
    }
}
