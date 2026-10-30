package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class AttendanceSessionRosterTest {
    private static final int SESSION_ID = 14;
    private static final int OFFERING_ID = 8;

    @Test
    void authorizedLecturerLoadsAllSessionRecordsWithOneDaoRosterRead() {
        AttendanceDao attendance = mock(AttendanceDao.class);
        AuthenticatedSession lecturer = new AuthenticatedSession(2, "lec", Role.LECTURER, "Lecturer");
        when(attendance.findSessionsForSessionId(SESSION_ID)).thenReturn(Optional.of(
                new AttendanceDao.SessionRow(SESSION_ID, OFFERING_ID, 3,
                        java.time.LocalDate.of(2026, 10, 1), 7, 4)));
        List<AttendanceDao.AttendanceRow> rows = List.of(
                new AttendanceDao.AttendanceRow(1, SESSION_ID, 21, true),
                new AttendanceDao.AttendanceRow(2, SESSION_ID, 22, false));
        when(attendance.findRecordsForSession(SESSION_ID)).thenReturn(rows);
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(lecturer.userId(), OFFERING_ID)).thenReturn(true);
        AttendanceService service = service(attendance, new AuthorizationService(scope));

        assertThat(service.recordsForSession(lecturer, SESSION_ID)).containsExactlyElementsOf(rows);

        verify(attendance).findRecordsForSession(SESSION_ID);
    }

    @Test
    void undergraduateCannotReadWholeSessionRoster() {
        AttendanceDao attendance = mock(AttendanceDao.class);
        AuthenticatedSession student = new AuthenticatedSession(9, "stu", Role.UNDERGRADUATE, "Student");
        AttendanceService service = service(attendance, new AuthorizationService());

        assertThatThrownBy(() -> service.recordsForSession(student, SESSION_ID))
                .isInstanceOf(AuthorizationException.class);

        verifyNoInteractions(attendance);
    }

    private AttendanceService service(AttendanceDao attendance, AuthorizationService authorization) {
        return new AttendanceService(attendance, mock(CourseDao.class), mock(TimetableDao.class),
                mock(MedicalDao.class), mock(AuditLogDao.class), mock(TransactionManager.class),
                authorization, new AttendanceCalculator(), new BigDecimal("80.00"), Clock.systemUTC());
    }
}
