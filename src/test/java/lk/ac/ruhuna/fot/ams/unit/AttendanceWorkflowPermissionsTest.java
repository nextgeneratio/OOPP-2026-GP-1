package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttendanceWorkflowPermissionsTest {
    private final AttendanceDao attendanceDao = mock(AttendanceDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final TimetableDao timetableDao = mock(TimetableDao.class);
    private final MedicalDao medicalDao = mock(MedicalDao.class);
    private final AuditLogDao auditDao = mock(AuditLogDao.class);
    private final ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    private final Connection connection = mock(Connection.class);
    private final AttendanceService service = new AttendanceService(
            attendanceDao, courseDao, timetableDao, medicalDao, auditDao,
            new TransactionManager(connectionProvider), new AuthorizationService(),
            new AttendanceCalculator(), new BigDecimal("80"),
            Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void connectTransaction() throws Exception {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(attendanceDao.deleteSession(connection, 7)).thenReturn(1);
    }

    @Test
    void onlyAdminCanDeleteAttendanceRecordings() {
        AuthenticatedSession technicalOfficer =
                new AuthenticatedSession(2, "officer", Role.TECHNICAL_OFFICER, "Officer");

        assertThatThrownBy(() -> service.deleteSession(technicalOfficer, 7))
                .isInstanceOf(AuthorizationException.class);

        verify(attendanceDao, never()).deleteSession(connection, 7);
    }

    @Test
    void administratorCanDeleteAttendanceRecording() throws Exception {
        AuthenticatedSession admin = new AuthenticatedSession(1, "admin", Role.ADMIN, "Admin");

        service.deleteSession(admin, 7);

        verify(attendanceDao).deleteSession(connection, 7);
        verify(connection).commit();
    }

    @Test
    void lecturerCannotCreateAttendance() {
        AuthenticatedSession lecturer = new AuthenticatedSession(3, "lecturer", Role.LECTURER, "Lecturer");
        SessionRow request = new SessionRow(0, 7, 4, java.time.LocalDate.of(2026, 10, 18), 2, null, null);

        assertThatThrownBy(() -> service.saveSession(lecturer, request, java.util.Map.of()))
                .isInstanceOf(AuthorizationException.class);

        verify(attendanceDao, never()).insertSession(connection, request);
    }

    @Test
    void lecturerAndUndergraduateCannotUpdateAttendance() {
        AuthenticatedSession lecturer = new AuthenticatedSession(3, "lecturer", Role.LECTURER, "Lecturer");
        AuthenticatedSession student = new AuthenticatedSession(4, "student", Role.UNDERGRADUATE, "Student");
        AttendanceDao.AttendanceRow row = new AttendanceDao.AttendanceRow(0, 7, 9, true);

        assertThatThrownBy(() -> service.reviseRecord(lecturer, row)).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> service.reviseRecord(student, row)).isInstanceOf(AuthorizationException.class);

        verify(attendanceDao, never()).findSessionsForSessionId(7);
        verify(attendanceDao, never()).reviseRecord(connection, row);
    }

    @Test
    void administratorCannotUpdateAttendanceRecords() {
        AuthenticatedSession admin = new AuthenticatedSession(1, "admin", Role.ADMIN, "Admin");
        AttendanceDao.AttendanceRow row = new AttendanceDao.AttendanceRow(0, 7, 9, true);

        assertThatThrownBy(() -> service.reviseRecord(admin, row)).isInstanceOf(AuthorizationException.class);

        verify(attendanceDao, never()).findSessionsForSessionId(7);
        verify(attendanceDao, never()).reviseRecord(connection, row);
    }
}
