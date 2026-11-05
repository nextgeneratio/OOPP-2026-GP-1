package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.service.TimetableService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class WeeklyTimetableServiceTest {
    private final TimetableDao dao=mock(TimetableDao.class);
    private final CourseDao courses=mock(CourseDao.class);
    private final AuditLogDao audit=mock(AuditLogDao.class);
    private final ConnectionProvider provider=mock(ConnectionProvider.class);
    private final Connection connection=mock(Connection.class);
    private final TimetableService service=new TimetableService(dao,courses,audit,new TransactionManager(provider),
            new AuthorizationService(),Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"),ZoneOffset.UTC));
    private final AuthenticatedSession admin=new AuthenticatedSession(1,"admin",Role.ADMIN,"Administrator");

    @Test void createsMultipleWeekdaySlotsAsOneTransactionWithoutCourseReferences() throws Exception {
        when(provider.getConnection()).thenReturn(connection);when(dao.insertTimetable(eq(connection),any())).thenReturn(40L);
        when(dao.findEntries(2,3,1,null)).thenReturn(List.of());
        long id=service.createWeekly(admin,new TimetableRow(0,"ICT level 1",2,3,1),List.of(
                slot("MON","08:00","09:00",null),slot("MON","09:00","10:00","Lab 1"),slot("WED","11:00","12:00",null)));
        assertThat(id).isEqualTo(40);
        verify(dao,times(3)).insertEntry(eq(connection),argThat(row->row.offeringId()==0&&row.componentId()==0));
        verify(connection).commit();verify(dao,never()).deleteEntriesForTimetable(any(),anyInt());
    }

    @Test void rejectsOverlappingOrInvalidWeekBeforeSavingAnything() throws Exception {
        assertThatThrownBy(()->service.createWeekly(admin,new TimetableRow(0,"T",2,3,1),List.of(
                slot("TUE","08:00","10:00",null),slot("TUE","09:30","11:00",null))))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(()->service.createWeekly(admin,new TimetableRow(0,"T",2,3,1),List.of(
                slot("FUNDAY","08:00","09:00",null))))
                .isInstanceOf(ValidationException.class);
        verify(provider,never()).getConnection();
    }

    @Test void updateDeactivatesOldSlotRowsAndAddsNewVersionWithoutDeletingHistory() throws Exception {
        when(provider.getConnection()).thenReturn(connection);
        when(dao.findTimetable(40)).thenReturn(java.util.Optional.of(new TimetableRow(40,"T",2,3,1)));
        when(dao.isTimetableActive(40)).thenReturn(true);when(dao.findEntries(2,3,1,null)).thenReturn(List.of());
        service.updateWeekly(admin,40,List.of(slot("FRI","13:00","14:00",null)));
        verify(dao).deactivateEntriesForTimetable(connection,40);verify(dao).insertEntry(eq(connection),any());
        verify(dao,never()).deleteEntriesForTimetable(any(),anyInt());verify(connection).commit();
    }

    @Test void refusesManagementByTechnicalOfficerAndRollsBackOnFailedSlotInsert() throws Exception {
        AuthenticatedSession officer=new AuthenticatedSession(2,"te",Role.TECHNICAL_OFFICER,"Officer");
        assertThatThrownBy(()->service.createWeekly(officer,new TimetableRow(0,"T",2,3,1),List.of()))
                .isInstanceOf(AuthorizationException.class);
        when(provider.getConnection()).thenReturn(connection);when(dao.insertTimetable(eq(connection),any())).thenReturn(40L);
        when(dao.findEntries(2,3,1,null)).thenReturn(List.of());doThrow(new IllegalStateException("slot failure")).when(dao).insertEntry(eq(connection),any());
        assertThatThrownBy(()->service.createWeekly(admin,new TimetableRow(0,"T",2,3,1),List.of(slot("THU","08:00","09:00",null))))
                .isInstanceOf(IllegalStateException.class);
        verify(connection).rollback();verify(connection,never()).commit();
    }

    private EntryRow slot(String day,String start,String end,String location){return new EntryRow(0,0,0,0,day,LocalTime.parse(start),LocalTime.parse(end),location);}
}
