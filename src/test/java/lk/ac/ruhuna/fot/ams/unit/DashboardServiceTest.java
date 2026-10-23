package lk.ac.ruhuna.fot.ams.unit;

import java.util.Map;
import lk.ac.ruhuna.fot.ams.business.service.DashboardService;
import lk.ac.ruhuna.fot.ams.data.dao.DashboardDao;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {
    @Test
    void loadsRoleDashboardMetricsFromTheDataAccessLayer() {
        DashboardDao dashboardDao = session -> Map.of(
                "Current courses", session.role() == Role.UNDERGRADUATE ? "2" : "0");
        DashboardService service = new DashboardService(dashboardDao);

        Map<String, String> metrics = service.loadMetrics(
                new AuthenticatedSession(11, "STU001", Role.UNDERGRADUATE, "Undergraduate 001"));

        assertThat(metrics).containsEntry("Current courses", "2");
    }
}
