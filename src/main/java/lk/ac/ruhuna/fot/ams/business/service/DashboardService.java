package lk.ac.ruhuna.fot.ams.business.service;

import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.DashboardDao;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class DashboardService {
    private final DashboardDao dashboardDao;

    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = Objects.requireNonNull(dashboardDao);
    }

    public Map<String, String> loadMetrics(AuthenticatedSession session) {
        return dashboardDao.loadMetrics(Objects.requireNonNull(session));
    }
}
