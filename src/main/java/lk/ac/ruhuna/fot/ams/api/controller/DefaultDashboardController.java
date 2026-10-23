package lk.ac.ruhuna.fot.ams.api.controller;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.api.response.DashboardSummary;
import lk.ac.ruhuna.fot.ams.business.service.DashboardService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class DefaultDashboardController implements DashboardController {
    private final DashboardService service;

    public DefaultDashboardController(DashboardService service) {
        this.service = Objects.requireNonNull(service);
    }

    @Override
    public DashboardSummary load(AuthenticatedSession session) {
        return new DashboardSummary(service.loadMetrics(session));
    }
}
