package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.QuestionRow;
import lk.ac.ruhuna.fot.ams.data.dao.LearningActivityDao.QuizRow;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.business.service.LearningActivityService;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LearningActivityServiceTest {
    private final LearningActivityDao learningDao = mock(LearningActivityDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final AuditLogDao auditDao = mock(AuditLogDao.class);
    private final ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    private final Connection connection = mock(Connection.class);
    private final PasswordHasher passwordHasher = new PasswordHasher();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);
    private final AuthorizationScopeLookup scopes = new TestScopes();
    private final LearningActivityService service = new LearningActivityService(
            learningDao, courseDao, auditDao, new TransactionManager(connectionProvider),
            new AuthorizationService(scopes), passwordHasher, clock);
    private final AuthenticatedSession student =
            new AuthenticatedSession(10, "student", Role.UNDERGRADUATE, "Test Student");
    private final char[] quizPassword = "quiz-secret-99".toCharArray();

    @BeforeEach
    void setUp() throws Exception {
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(courseDao.findActiveEnrollments(44)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(9, 22, 44, 1, "NORMAL", "ACTIVE")));
        when(learningDao.findQuiz(5)).thenReturn(java.util.Optional.of(new QuizRow(
                5, 44, 3, "Midterm", passwordHasher.hash(quizPassword), true,
                LocalDateTime.parse("2026-10-18T10:00:00"), LocalDateTime.parse("2026-10-18T18:00:00"))));
        when(learningDao.findQuestions(5)).thenReturn(List.of(
                new QuestionRow(101, 5, 1, "Two plus two?", List.of("3", "4", "5", "6"), 1),
                new QuestionRow(102, 5, 2, "Capital?", List.of("A", "B", "C", "D"), 0)));
    }

    @Test
    void checksQuizPasswordAndScoresAnswersOnServerForOneAttempt() throws Exception {
        var result = service.attemptQuiz(student, 5, quizPassword,
                Map.of(101, 1, 102, 3));

        assertThat(result.score()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2);
        verify(learningDao).insertAttempt(connection, 5, 22, 9, 1, 2);
        verify(connection).commit();
    }

    @Test
    void rejectsWrongPasswordBeforeSavingAttempt() {
        assertThatThrownBy(() -> service.attemptQuiz(student, 5, "incorrect".toCharArray(),
                Map.of(101, 1, 102, 0)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("password");

        verify(learningDao, never()).insertAttempt(any(), any(Integer.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    void requiresPasswordBeforeRevealingQuizQuestions() {
        assertThatThrownBy(() -> service.questionsForStudent(student, 5, "wrong-password".toCharArray()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("password");

        verify(learningDao, never()).findQuestions(5);
    }

    @Test
    void rejectsASecondAttemptByTheSameStudent() {
        when(learningDao.hasAttempt(5, 22)).thenReturn(true);

        assertThatThrownBy(() -> service.attemptQuiz(student, 5, quizPassword,
                Map.of(101, 1, 102, 0)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already used");

        verify(learningDao, never()).insertAttempt(any(), any(Integer.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class));
    }

    private static final class TestScopes implements AuthorizationScopeLookup {
        @Override public boolean lecturerAssignedToOffering(long userId, int offeringId) { return true; }
        @Override public boolean lecturerAssignedToStudent(long userId, int studentId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) { return true; }
        @Override public boolean undergraduateOwnsStudent(long userId, int studentId) { return studentId == 22; }
        @Override public boolean undergraduateEnrolledInOffering(long userId, int offeringId) { return offeringId == 44; }
        @Override public OptionalInt lecturerIdForUser(long userId) { return OptionalInt.of(3); }
        @Override public OptionalInt technicalOfficerIdForUser(long userId) { return OptionalInt.of(4); }
        @Override public OptionalInt studentIdForUser(long userId) { return OptionalInt.of(22); }
        @Override public OptionalInt studentBatchIdForUser(long userId) { return OptionalInt.of(2); }
    }
}
