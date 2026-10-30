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
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class AssessmentServiceWorkflowTest {
    private static final int OFFERING_ID = 11;
    private static final AuthenticatedSession LECTURER =
            new AuthenticatedSession(7, "lecturer", Role.LECTURER, "Lecturer");

    @Test
    void publishesOneHundredPercentPlanAndAuditsEachAssessmentInOneTransaction() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        AuditLogDao audit = mock(AuditLogDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connections.getConnection()).thenReturn(connection);
        when(assessments.insertAssessment(any(), any())).thenReturn(101L, 102L);
        AssessmentService service = service(assessments, audit, connections, true);
        List<AssessmentDao.AssessmentRow> plan = List.of(
                row("Coursework", "ASSIGNMENT", "40", true),
                row("Final examination", "FINAL", "60", false));

        assertThat(service.publishPlan(LECTURER, OFFERING_ID, plan)).containsExactly(101L, 102L);

        verify(assessments).insertAssessment(connection, plan.get(0));
        verify(assessments).insertAssessment(connection, plan.get(1));
        verify(audit, org.mockito.Mockito.times(2)).insert(any(), any());
        verify(connection).commit();
    }

    @Test
    void invalidTotalOrZeroCaWeightIsRejectedBeforeOpeningTransaction() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        AssessmentService service = service(assessments, mock(AuditLogDao.class), connections, true);

        assertThatThrownBy(() -> service.publishPlan(LECTURER, OFFERING_ID, List.of(
                row("Coursework", "ASSIGNMENT", "39", true),
                row("Final examination", "FINAL", "60", false))))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.publishPlan(LECTURER, OFFERING_ID, List.of(
                row("Coursework", "ASSIGNMENT", "40", false),
                row("Final examination", "FINAL", "60", false))))
                .isInstanceOf(ValidationException.class);

        verify(connections, never()).getConnection();
        verify(assessments, never()).insertAssessment(any(), any());
    }

    @Test
    void partialPlanFailureRollsBackPriorAssessmentAndAuditWrites() throws Exception {
        AssessmentDao assessments = mock(AssessmentDao.class);
        AuditLogDao audit = mock(AuditLogDao.class);
        ConnectionProvider connections = mock(ConnectionProvider.class);
        Connection connection = mock(Connection.class);
        when(connections.getConnection()).thenReturn(connection);
        when(assessments.insertAssessment(any(), any())).thenReturn(101L)
                .thenThrow(new IllegalStateException("simulated insert failure"));
        AssessmentService service = service(assessments, audit, connections, true);

        assertThatThrownBy(() -> service.publishPlan(LECTURER, OFFERING_ID, List.of(
                row("Coursework", "ASSIGNMENT", "40", true),
                row("Final examination", "FINAL", "60", false))))
                .isInstanceOf(IllegalStateException.class);

        verify(audit).insert(any(), any());
        verify(connection).rollback();
        verify(connection, never()).commit();
    }

    private static AssessmentDao.AssessmentRow row(String name, String type, String weight, boolean ca) {
        return new AssessmentDao.AssessmentRow(0, OFFERING_ID, name, type, new BigDecimal(weight), ca);
    }

    private static AssessmentService service(AssessmentDao assessments, AuditLogDao audit,
            ConnectionProvider connections, boolean assigned) {
        AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
        when(scope.lecturerAssignedToOffering(LECTURER.userId(), OFFERING_ID)).thenReturn(assigned);
        return new AssessmentService(assessments, audit, new TransactionManager(connections),
                new AuthorizationService(scope), new AssessmentCalculator(), Clock.systemUTC());
    }
}
