package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.service.AcademicResultService;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.business.service.GradeService;
import lk.ac.ruhuna.fot.ams.business.service.MarksService;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class AcademicResultScopeTest {
    private final CourseResultDao resultDao = mock(CourseResultDao.class);
    private final ScopeLookup scopes = new ScopeLookup();
    private final AcademicResultService service = new AcademicResultService(mock(CourseDao.class), resultDao,
            mock(MarksService.class), mock(EligibilityService.class), mock(GradeService.class),
            mock(AuditLogDao.class), mock(TransactionManager.class), new AuthorizationService(scopes),
            Clock.systemUTC(), "UGC-2024-v1");

    @Test
    void lecturerResultLookupIsBoundToTheSelectedAssignedOffering() {
        AuthenticatedSession lecturer = session(Role.LECTURER);
        when(resultDao.findByStudentAndOffering(42, 10)).thenReturn(List.of());

        assertThat(service.resultsForStudentInOffering(lecturer, 42, 10)).isEmpty();

        verify(resultDao).findByStudentAndOffering(42, 10);
        verify(resultDao, never()).findByStudent(42);
    }

    @Test
    void lecturerCannotUseTheUnscopedStudentResultLookup() {
        assertThatThrownBy(() -> service.resultsForStudent(session(Role.LECTURER), 42))
                .isInstanceOf(AuthorizationException.class);
        verify(resultDao, never()).findByStudent(anyInt());
    }

    @Test
    void lecturerCannotRetrieveDetailedResultsFromAnUnassignedOffering() {
        assertThatThrownBy(() -> service.resultsForStudentInOffering(session(Role.LECTURER), 42, 11))
                .isInstanceOf(AuthorizationException.class);

        verifyNoInteractions(resultDao);
    }

    @Test
    void studentCanReadOnlyTheirOwnCompletedResults() {
        AuthenticatedSession student = session(Role.UNDERGRADUATE);
        when(resultDao.findByStudent(7)).thenReturn(List.of());

        assertThat(service.resultsForCurrentStudent(student)).isEmpty();
        verify(resultDao).findByStudent(7);
        assertThatThrownBy(() -> service.resultsForStudent(student, 8))
                .isInstanceOf(AuthorizationException.class);
    }

    private AuthenticatedSession session(Role role) {
        return new AuthenticatedSession(1, "user", role, "Test User");
    }

    private static final class ScopeLookup implements AuthorizationScopeLookup {
        @Override public boolean lecturerAssignedToOffering(long userId, int offeringId) { return offeringId == 10; }
        @Override public boolean lecturerAssignedToStudent(long userId, int studentId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) { return false; }
        @Override public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) { return false; }
        @Override public boolean undergraduateOwnsStudent(long userId, int studentId) { return studentId == 7; }
        @Override public boolean undergraduateEnrolledInOffering(long userId, int offeringId) { return true; }
        @Override public OptionalInt lecturerIdForUser(long userId) { return OptionalInt.of(1); }
        @Override public OptionalInt technicalOfficerIdForUser(long userId) { return OptionalInt.empty(); }
        @Override public OptionalInt studentIdForUser(long userId) { return OptionalInt.of(7); }
        @Override public OptionalInt studentBatchIdForUser(long userId) { return OptionalInt.of(1); }
    }
}
