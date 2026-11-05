package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.time.*;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.business.service.TimetableService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.*;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.EntryRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao.TimetableRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class WeeklyTimetableServiceTest {
    private final TimetableDao dao=mock(TimetableDao.class);private final CourseDao courses=mock(CourseDao.class);
    private final AuditLogDao audit=mock(AuditLogDao.class);private final ConnectionProvider provider=mock(ConnectionProvider.class);private final Connection connection=mock(Connection.class);
    private final TimetableService service=new TimetableService(dao,courses,audit,new TransactionManager(provider),new AuthorizationService(),Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"),ZoneOffset.UTC));
    private final AuthenticatedSession admin=new AuthenticatedSession(1,"admin",Role.ADMIN,"Administrator");

    @Test void createsCourseOfferingScheduleInOneTransactionWithSelectedComponents() throws Exception {
        baseOfferings();when(provider.getConnection()).thenReturn(connection);when(dao.insertTimetable(eq(connection),any())).thenReturn(40L);when(dao.findEntries(2,3)).thenReturn(List.of());
        long id=service.createWeekly(admin,header(70),List.of(slot("MON","08:00","09:00",5,"LH 01"),slot("MON","09:00","10:00",5,null),slot("WED","11:00","12:00",5,"Lab 1")));
        assertThat(id).isEqualTo(40);verify(dao).insertTimetable(eq(connection),argThat(row->row.offeringId()==70));
        verify(dao,times(3)).insertEntry(eq(connection),argThat(row->row.offeringId()==70&&row.componentId()==5));verify(connection).commit();
    }

    @Test void rejectsWrongCourseComponentAndDuplicateActiveTimetableBeforeSaving() throws Exception {
        baseOfferings();when(courses.findComponents(2)).thenReturn(List.of(new CourseDao.ComponentRow(5,2,ComponentType.THEORY,3,15)));
        assertThatThrownBy(()->service.createWeekly(admin,header(70),List.of(slot("MON","08:00","09:00",99,null)))).isInstanceOf(ValidationException.class);
        when(dao.findActiveTimetableForOffering(70)).thenReturn(Optional.of(header(70)));
        assertThatThrownBy(()->service.createWeekly(admin,header(70),List.of(slot("MON","08:00","09:00",5,null)))).isInstanceOf(ValidationException.class);
        verify(provider,never()).getConnection();
    }

    @Test void updateVersionsOnlySelectedCourseSlotsAndPreservesAttendanceIdentifiers() throws Exception {
        baseOfferings();when(provider.getConnection()).thenReturn(connection);when(dao.findTimetable(40)).thenReturn(Optional.of(header(70,40)));when(dao.isTimetableActive(40)).thenReturn(true);when(dao.findEntries(2,3)).thenReturn(List.of());
        service.updateWeekly(admin,40,List.of(slot("FRI","13:00","14:00",5,"Room 2")));
        verify(dao).deactivateEntriesForTimetable(connection,40);verify(dao).insertEntry(eq(connection),argThat(row->row.offeringId()==70&&row.componentId()==5));verify(dao,never()).deleteEntriesForTimetable(any(),anyInt());verify(connection).commit();
    }

    @Test void allowsParallelDifferentCoursesInDifferentRoomsButRejectsRoomAndLecturerConflicts() throws Exception {
        baseOfferings();when(dao.findActiveTimetableForOffering(70)).thenReturn(Optional.empty());when(dao.findEntries(2,3)).thenReturn(List.of(new EntryRow(9,90,71,6,"MON",LocalTime.parse("08:00"),LocalTime.parse("10:00"),"LH 02")));
        when(dao.findTimetable(90)).thenReturn(Optional.of(header(71,90)));when(courses.findOffering(71)).thenReturn(Optional.of(new CourseDao.OfferingRow(71,3,9,3,null,"OPEN")));when(courses.findLecturersForOffering(70)).thenReturn(List.of(4));when(courses.findLecturersForOffering(71)).thenReturn(List.of(8));
        when(provider.getConnection()).thenReturn(connection);when(dao.insertTimetable(eq(connection),any())).thenReturn(41L);
        assertThat(service.createWeekly(admin,header(70),List.of(slot("MON","08:00","10:00",5,"LH 01")))).isEqualTo(41L);
        verify(connection).commit();
        reset(provider,connection,dao,audit);when(provider.getConnection()).thenReturn(connection);baseOfferings();when(dao.findActiveTimetableForOffering(70)).thenReturn(Optional.empty());when(dao.findEntries(2,3)).thenReturn(List.of(new EntryRow(9,90,71,6,"MON",LocalTime.parse("08:00"),LocalTime.parse("10:00"),"LH 01")));when(dao.findTimetable(90)).thenReturn(Optional.of(header(71,90)));when(courses.findOffering(71)).thenReturn(Optional.of(new CourseDao.OfferingRow(71,3,9,3,null,"OPEN")));when(courses.findLecturersForOffering(70)).thenReturn(List.of(4));when(courses.findLecturersForOffering(71)).thenReturn(List.of(8));
        assertThatThrownBy(()->service.createWeekly(admin,header(70),List.of(slot("MON","08:00","10:00",5,"LH 01")))).isInstanceOf(ValidationException.class);
        verify(dao,never()).insertTimetable(any(),any());
    }

    @Test void rejectsInvalidWeekAndUnauthorizedManagementAndRollsBackSlotFailure() throws Exception {
        assertThatThrownBy(()->service.createWeekly(admin,header(70),List.of(slot("FUNDAY","08:00","09:00",5,null)))).isInstanceOf(ValidationException.class);
        AuthenticatedSession officer=new AuthenticatedSession(2,"te",Role.TECHNICAL_OFFICER,"Officer");assertThatThrownBy(()->service.createWeekly(officer,header(70),List.of())).isInstanceOf(AuthorizationException.class);
        baseOfferings();when(provider.getConnection()).thenReturn(connection);when(dao.insertTimetable(eq(connection),any())).thenReturn(40L);when(dao.findEntries(2,3)).thenReturn(List.of());doThrow(new IllegalStateException("slot failure")).when(dao).insertEntry(eq(connection),any());
        assertThatThrownBy(()->service.createWeekly(admin,header(70),List.of(slot("THU","08:00","09:00",5,null)))).isInstanceOf(IllegalStateException.class);verify(connection).rollback();verify(connection,never()).commit();
    }

    private void baseOfferings(){when(courses.findOffering(70)).thenReturn(Optional.of(new CourseDao.OfferingRow(70,2,8,3,null,"OPEN")));when(courses.findBatch(8)).thenReturn(Optional.of(new CourseDao.BatchRow(8,2,2024,"ICT","ICT-24",1)));when(courses.findCourse(2)).thenReturn(Optional.of(new CourseDao.CourseRow(2,2,"ICT2122","Object Oriented Programming",3,true)));when(courses.findComponents(2)).thenReturn(List.of(new CourseDao.ComponentRow(5,2,ComponentType.THEORY,3,15)));when(courses.findLecturersForOffering(70)).thenReturn(List.of());when(dao.findActiveTimetableForOffering(70)).thenReturn(Optional.empty());}
    private TimetableRow header(int offering){return new TimetableRow(0,"ICT2122 course timetable",2,3,1,null,null,null,offering);}private TimetableRow header(int offering,int id){return new TimetableRow(id,"ICT2122 course timetable",2,3,1,null,null,null,offering);}
    private EntryRow slot(String day,String start,String end,int component,String location){return new EntryRow(0,0,70,component,day,LocalTime.parse(start),LocalTime.parse(end),location);}
}
