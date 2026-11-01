package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class AttendanceTechnicalOfficerPermissionsTest {
    private final AttendanceDao attendanceDao = mock(AttendanceDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final TimetableDao timetableDao = mock(TimetableDao.class);
    private final MedicalDao medicalDao = mock(MedicalDao.class);
    private final AuditLogDao auditDao = mock(AuditLogDao.class);
    private final ConnectionProvider provider = mock(ConnectionProvider.class);
    private final Connection connection = mock(Connection.class);
    private final AuthorizationScopeLookup scopes = mock(AuthorizationScopeLookup.class);
    private final AuthenticatedSession officer =
            new AuthenticatedSession(2, "officer", Role.TECHNICAL_OFFICER, "Officer");
    private final AttendanceService service = new AttendanceService(attendanceDao, courseDao, timetableDao,
            medicalDao, auditDao, new TransactionManager(provider), new AuthorizationService(scopes),
            new AttendanceCalculator(), new BigDecimal("80"),
            Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void technicalOfficerCanCreateReadAndUpdateAttendance() throws Exception {
        when(provider.getConnection()).thenReturn(connection);
        when(scopes.technicalOfficerAuthorizedForOffering(2, 7)).thenReturn(true);
        when(scopes.technicalOfficerIdForUser(2)).thenReturn(OptionalInt.of(22));
        when(timetableDao.entryMatchesOfferingAndComponent(5, 7, 3, LocalDate.of(2026, 10, 18))).thenReturn(true);
        when(courseDao.findOffering(7)).thenReturn(Optional.of(new CourseDao.OfferingRow(7, 10, 2, 4, null, "OPEN")));
        when(courseDao.findComponents(10)).thenReturn(List.of(
                new CourseDao.ComponentRow(3, 10, ComponentType.THEORY, 2, 12)));
        when(courseDao.findActiveEnrollments(7)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(9, 44, 7, 1, "NORMAL", "ACTIVE")));
        when(attendanceDao.insertSession(eq(connection), any())).thenReturn(15L);
        when(attendanceDao.findSessions(7)).thenReturn(List.of(
                new AttendanceDao.SessionRow(15, 7, 3, LocalDate.of(2026, 10, 18), 5, 22, null)));
        when(attendanceDao.findSessionsForSessionId(15)).thenReturn(Optional.of(
                new AttendanceDao.SessionRow(15, 7, 3, LocalDate.of(2026, 10, 18), 5, 22, null)));
        when(courseDao.findEnrollments(7)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(9, 44, 7, 1, "NORMAL", "ACTIVE")));
        when(attendanceDao.reviseRecord(eq(connection), any())).thenReturn(1);

        long id = service.saveSession(officer,
                new AttendanceDao.SessionRow(0, 7, 3, LocalDate.of(2026, 10, 18), 5, null, null),
                Map.of(9, true));
        assertThat(id).isEqualTo(15);
        assertThat(service.sessionsForOffering(officer, 7)).hasSize(1);
        service.reviseRecord(officer, new AttendanceDao.AttendanceRow(0, 15, 9, false));

        verify(attendanceDao).insertRecord(eq(connection), argThat(row -> row.enrollmentId() == 9 && row.present()));
        verify(attendanceDao).reviseRecord(eq(connection), argThat(row -> !row.present()));
        verify(connection, times(2)).commit();
    }

    @Test
    void undergraduateCannotReadAnotherStudentsAttendance() {
        AuthenticatedSession student = new AuthenticatedSession(4, "student", Role.UNDERGRADUATE, "Student");
        when(scopes.studentIdForUser(4)).thenReturn(OptionalInt.of(44));
        when(scopes.undergraduateOwnsStudent(4, 44)).thenReturn(true);
        when(courseDao.findEnrollments(7)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(9, 44, 7, 1, "NORMAL", "ACTIVE"),
                new CourseDao.EnrollmentRow(10, 45, 7, 1, "NORMAL", "ACTIVE")));

        assertThatThrownBy(() -> service.historyForEnrollment(student, 10, 7))
                .isInstanceOf(AuthorizationException.class);
        verify(attendanceDao, never()).findHistory(10, 7);
    }

    @Test
    void undergraduateCanReadOwnAttendance() {
        AuthenticatedSession student = new AuthenticatedSession(4, "student", Role.UNDERGRADUATE, "Student");
        when(scopes.studentIdForUser(4)).thenReturn(OptionalInt.of(44));
        when(scopes.undergraduateOwnsStudent(4, 44)).thenReturn(true);
        when(courseDao.findEnrollments(7)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(9, 44, 7, 1, "NORMAL", "ACTIVE")));
        when(attendanceDao.findHistory(9, 7)).thenReturn(List.of());

        assertThat(service.historyForEnrollment(student, 9, 7)).isEmpty();

        verify(attendanceDao).findHistory(9, 7);
    }

    @Test
    void lecturerCannotReadAnUnassignedOfferingOrMismatchedStudentEnrollment() {
        AuthenticatedSession lecturer = new AuthenticatedSession(3, "lecturer", Role.LECTURER, "Lecturer");
        when(scopes.lecturerAssignedToOffering(3, 7)).thenReturn(false);
        assertThatThrownBy(() -> service.historyForEnrollment(lecturer, 9, 7))
                .isInstanceOf(AuthorizationException.class);
        verify(courseDao, never()).findEnrollments(7);

        when(scopes.lecturerAssignedToOffering(3, 7)).thenReturn(true);
        when(courseDao.findEnrollments(7)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(10, 45, 7, 1, "NORMAL", "ACTIVE")));
        assertThatThrownBy(() -> service.calculateForEnrollment(lecturer, 44, 10, 7))
                .isInstanceOf(AuthorizationException.class);
        verify(attendanceDao, never()).findHistory(10, 7);
    }
}
