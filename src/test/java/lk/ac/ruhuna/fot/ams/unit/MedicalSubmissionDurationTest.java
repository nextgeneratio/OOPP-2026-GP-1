package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MedicalSubmissionDurationTest {
    private final MedicalDao medicalDao = mock(MedicalDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final AuditLogDao auditLogDao = mock(AuditLogDao.class);
    private final ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    private final Connection connection = mock(Connection.class);
    private final MedicalService service = new MedicalService(medicalDao, courseDao, auditLogDao,
            new TransactionManager(connectionProvider), new AuthorizationService(new Scopes()),
            Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC));
    private final AuthenticatedSession student =
            new AuthenticatedSession(2, "student", Role.UNDERGRADUATE, "Student");

    @BeforeEach
    void setUp() throws Exception {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(medicalDao.insert(eq(connection), any(MedicalRow.class))).thenReturn(41L);
    }

    @Test
    void dateGapOverFourteenDaysIsSavedRejectedWithReason() {
        LocalDate start = LocalDate.now().minusDays(30);
        MedicalRow request = request(start, start.plusDays(15));

        service.submit(student, request);

        ArgumentCaptor<MedicalRow> saved = ArgumentCaptor.forClass(MedicalRow.class);
        verify(medicalDao).insert(eq(connection), saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(saved.getValue().rejectionReason()).isEqualTo("The absence period cannot exceed 14 days.");
        assertThat(saved.getValue().affectsEligibility()).isFalse();
        verify(auditLogDao).insert(eq(connection), org.mockito.ArgumentMatchers.argThat(
                event -> event.action().equals("MEDICAL_REJECT")));
    }

    @Test
    void dateGapOfFourteenDaysRemainsPending() {
        LocalDate start = LocalDate.now().minusDays(30);

        service.submit(student, request(start, start.plusDays(14)));

        ArgumentCaptor<MedicalRow> saved = ArgumentCaptor.forClass(MedicalRow.class);
        verify(medicalDao).insert(eq(connection), saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(saved.getValue().rejectionReason()).isNull();
    }

    private MedicalRow request(LocalDate start, LocalDate end) {
        return new MedicalRow(0, 22, null, LocalDate.now().minusDays(1), start, end,
                "Synthetic medical request", "medical-evidence/00000000-0000-0000-0000-000000000001.pdf",
                ApprovalStatus.PENDING, false, null);
    }

    private static final class Scopes implements AuthorizationScopeLookup {
        @Override public boolean lecturerAssignedToOffering(long userId, int offeringId) { return true; }
        @Override public boolean lecturerAssignedToStudent(long userId, int studentId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) { return true; }
        @Override public boolean undergraduateOwnsStudent(long userId, int studentId) {
            return userId == 2 && studentId == 22;
        }
        @Override public boolean undergraduateEnrolledInOffering(long userId, int offeringId) { return true; }
        @Override public OptionalInt lecturerIdForUser(long userId) { return OptionalInt.of(3); }
        @Override public OptionalInt technicalOfficerIdForUser(long userId) { return OptionalInt.of(4); }
        @Override public OptionalInt studentIdForUser(long userId) { return OptionalInt.of(22); }
        @Override public OptionalInt studentBatchIdForUser(long userId) { return OptionalInt.of(2); }
    }
}
