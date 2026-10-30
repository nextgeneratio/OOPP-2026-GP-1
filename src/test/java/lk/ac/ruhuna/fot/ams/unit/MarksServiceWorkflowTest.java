package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.util.List;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.business.service.MarksService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class MarksServiceWorkflowTest {
    private static final int OFFERING_ID = 11;
    private static final int ASSESSMENT_ID = 42;
    private static final int ENROLLMENT_ID = 21;
    private static final AuthenticatedSession LECTURER =
            new AuthenticatedSession(7, "lecturer", Role.LECTURER, "Lecturer");

    @Test
    void validMarkIsInsertedAndCommittedAtomically() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        CourseDao courses = mock(CourseDao.class);
        AuditLogDao audit = mock(AuditLogDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connections.getConnection()).thenReturn(connection);
        when(assessments.findAssessments(OFFERING_ID)).thenReturn(List.of(
                new AssessmentDao.AssessmentRow(ASSESSMENT_ID, OFFERING_ID,
                        "Quiz", "QUIZ", new BigDecimal("20"), true)));
        when(courses.findEnrollments(OFFERING_ID)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(ENROLLMENT_ID, 31, OFFERING_ID, 1, "NORMAL", "ACTIVE")));
        when(assessments.findMarks(connection, OFFERING_ID, ENROLLMENT_ID)).thenReturn(List.of());
        when(assessments.insertMark(any(), any())).thenReturn(99L);
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(LECTURER.userId(), OFFERING_ID)).thenReturn(true);
        when(scope.lecturerIdForUser(LECTURER.userId())).thenReturn(OptionalInt.of(5));
        MarksService service = new MarksService(assessments, courses, audit,
                new TransactionManager(connections), new AuthorizationService(scope),
                new AssessmentCalculator(), Clock.systemUTC());

        int saved = service.save(LECTURER, OFFERING_ID, List.of(
                new MarksService.MarkInput(ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("87.5"))));

        assertThat(saved).isEqualTo(1);
        verify(assessments).insertMark(connection,
                new AssessmentDao.MarkRow(0, ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("87.5"), 5));
        verify(audit).insert(any(), any());
        verify(connection).commit();
    }

    @Test
    void invalidMarkDoesNotOpenATransactionOrWrite() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        CourseDao courses = mock(CourseDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        when(assessments.findAssessments(OFFERING_ID)).thenReturn(List.of(
                new AssessmentDao.AssessmentRow(ASSESSMENT_ID, OFFERING_ID,
                        "Quiz", "QUIZ", new BigDecimal("20"), true)));
        when(courses.findEnrollments(OFFERING_ID)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(ENROLLMENT_ID, 31, OFFERING_ID, 1, "NORMAL", "ACTIVE")));
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(LECTURER.userId(), OFFERING_ID)).thenReturn(true);
        when(scope.lecturerIdForUser(LECTURER.userId())).thenReturn(OptionalInt.of(5));
        MarksService service = new MarksService(assessments, courses, mock(AuditLogDao.class),
                new TransactionManager(connections), new AuthorizationService(scope),
                new AssessmentCalculator(), Clock.systemUTC());

        assertThatThrownBy(() -> service.save(LECTURER, OFFERING_ID, List.of(
                new MarksService.MarkInput(ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("101")))))
                .isInstanceOf(ValidationException.class);

        verify(connections, never()).getConnection();
        verify(assessments, never()).insertMark(any(), any());
    }

    @Test
    void existingMarkIsRevisedAndCommittedWithAudit() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        CourseDao courses = mock(CourseDao.class);
        AuditLogDao audit = mock(AuditLogDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connections.getConnection()).thenReturn(connection);
        when(assessments.findAssessments(OFFERING_ID)).thenReturn(List.of(
                new AssessmentDao.AssessmentRow(ASSESSMENT_ID, OFFERING_ID,
                        "Quiz", "QUIZ", new BigDecimal("20"), true)));
        when(courses.findEnrollments(OFFERING_ID)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(ENROLLMENT_ID, 31, OFFERING_ID, 1, "NORMAL", "ACTIVE")));
        when(assessments.findMarks(connection, OFFERING_ID, ENROLLMENT_ID)).thenReturn(List.of(
                new AssessmentDao.MarkRow(99, ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("72"), 5)));
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(LECTURER.userId(), OFFERING_ID)).thenReturn(true);
        when(scope.lecturerIdForUser(LECTURER.userId())).thenReturn(OptionalInt.of(5));
        MarksService service = new MarksService(assessments, courses, audit,
                new TransactionManager(connections), new AuthorizationService(scope),
                new AssessmentCalculator(), Clock.systemUTC());

        assertThat(service.save(LECTURER, OFFERING_ID, List.of(
                new MarksService.MarkInput(ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("88"))))).isEqualTo(1);

        verify(assessments).reviseMark(connection,
                new AssessmentDao.MarkRow(99, ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("88"), 5));
        verify(audit).insert(any(), any());
        verify(connection).commit();
    }

    @Test
    void partialMarksFailureRollsBackPriorWrites() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        CourseDao courses = mock(CourseDao.class);
        AuditLogDao audit = mock(AuditLogDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connections.getConnection()).thenReturn(connection);
        when(assessments.findAssessments(OFFERING_ID)).thenReturn(List.of(
                new AssessmentDao.AssessmentRow(ASSESSMENT_ID, OFFERING_ID,
                        "Quiz", "QUIZ", new BigDecimal("20"), true),
                new AssessmentDao.AssessmentRow(43, OFFERING_ID,
                        "Assignment", "ASSIGNMENT", new BigDecimal("20"), true)));
        when(courses.findEnrollments(OFFERING_ID)).thenReturn(List.of(
                new CourseDao.EnrollmentRow(ENROLLMENT_ID, 31, OFFERING_ID, 1, "NORMAL", "ACTIVE")));
        when(assessments.findMarks(connection, OFFERING_ID, ENROLLMENT_ID)).thenReturn(List.of());
        when(assessments.insertMark(any(), any())).thenReturn(99L)
                .thenThrow(new IllegalStateException("simulated insert failure"));
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(LECTURER.userId(), OFFERING_ID)).thenReturn(true);
        when(scope.lecturerIdForUser(LECTURER.userId())).thenReturn(OptionalInt.of(5));
        MarksService service = new MarksService(assessments, courses, audit,
                new TransactionManager(connections), new AuthorizationService(scope),
                new AssessmentCalculator(), Clock.systemUTC());

        assertThatThrownBy(() -> service.save(LECTURER, OFFERING_ID, List.of(
                new MarksService.MarkInput(ASSESSMENT_ID, ENROLLMENT_ID, new BigDecimal("88")),
                new MarksService.MarkInput(43, ENROLLMENT_ID, new BigDecimal("91")))))
                .isInstanceOf(IllegalStateException.class);

        verify(audit).insert(any(), any());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }
}
