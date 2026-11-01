package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.service.MedicalService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MedicalServiceSecurityTest {
    private final MedicalDao medicalDao = mock(MedicalDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final AuditLogDao auditDao = mock(AuditLogDao.class);
    private final ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    private final Connection connection = mock(Connection.class);
    private final MedicalService service = new MedicalService(medicalDao, courseDao, auditDao,
            new TransactionManager(connectionProvider), new AuthorizationService(new Scopes()),
            Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC));
    private final MedicalRow pending = new MedicalRow(8, 22, 44, LocalDate.of(2026, 10, 20),
            LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21), "Synthetic test request",
            "medical-evidence/00000000-0000-0000-0000-000000000008.pdf", ApprovalStatus.PENDING, false, null);

    @BeforeEach
    void setUp() throws Exception {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(medicalDao.findById(8)).thenReturn(Optional.of(pending));
    }

    @Test
    void lecturerCannotReadOrDecideMedicalRequests() {
        AuthenticatedSession lecturer = new AuthenticatedSession(3, "lecturer", Role.LECTURER, "Lecturer");

        assertThatThrownBy(() -> service.findForStudent(lecturer, 22, 44))
                .isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> service.decide(lecturer, 8, ApprovalStatus.APPROVED, null))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void rejectionRequiresReasonBeforeDatabaseMutation() {
        AuthenticatedSession officer = new AuthenticatedSession(4, "officer", Role.TECHNICAL_OFFICER, "Officer");

        assertThatThrownBy(() -> service.decide(officer, 8, ApprovalStatus.REJECTED, "  "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reason");

        verify(medicalDao, never()).decide(any(), any(Integer.class), any(), any());
    }

    @Test
    void proofResolutionRejectsTamperedReferenceAfterAuthorization() {
        MedicalRow tampered = new MedicalRow(9, 22, 44, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21), "Synthetic test request",
                "../outside.pdf", ApprovalStatus.PENDING, false, null);
        when(medicalDao.findById(9)).thenReturn(Optional.of(tampered));
        AuthenticatedSession officer = new AuthenticatedSession(4, "officer", Role.TECHNICAL_OFFICER, "Officer");

        assertThatThrownBy(() -> service.evidenceFor(officer, 9))
                .isInstanceOf(ValidationException.class);
    }

    private static final class Scopes implements AuthorizationScopeLookup {
        @Override public boolean lecturerAssignedToOffering(long userId, int offeringId) { return true; }
        @Override public boolean lecturerAssignedToStudent(long userId, int studentId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) { return true; }
        @Override public boolean undergraduateOwnsStudent(long userId, int studentId) { return userId == 2 && studentId == 22; }
        @Override public boolean undergraduateEnrolledInOffering(long userId, int offeringId) { return offeringId == 44; }
        @Override public OptionalInt lecturerIdForUser(long userId) { return OptionalInt.of(3); }
        @Override public OptionalInt technicalOfficerIdForUser(long userId) { return OptionalInt.of(4); }
        @Override public OptionalInt studentIdForUser(long userId) { return OptionalInt.of(22); }
        @Override public OptionalInt studentBatchIdForUser(long userId) { return OptionalInt.of(2); }
    }
}
