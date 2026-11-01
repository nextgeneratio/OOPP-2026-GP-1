package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class AssessmentServiceSecurityTest {
    @Test
    void administratorCannotPublishAnAssessmentPlanThroughTheService() {
        AssessmentDao assessments = mock(AssessmentDao.class);
        AssessmentService service = new AssessmentService(assessments, mock(AuditLogDao.class),
                mock(TransactionManager.class), new AuthorizationService(), new AssessmentCalculator(), Clock.systemUTC());
        AuthenticatedSession admin = new AuthenticatedSession(1, "admin", Role.ADMIN, "Admin");

        assertThatThrownBy(() -> service.publishPlan(admin, 10, List.of(
                new AssessmentDao.AssessmentRow(0, 10, "Exam", "FINAL", BigDecimal.valueOf(100), false))))
                .isInstanceOf(AuthorizationException.class);

        verifyNoInteractions(assessments);
    }
}
