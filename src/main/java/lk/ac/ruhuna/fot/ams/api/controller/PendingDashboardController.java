package lk.ac.ruhuna.fot.ams.api.controller;

import lk.ac.ruhuna.fot.ams.api.response.DashboardSummary;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Temporary stand-in until dashboard services exist: every card shows "—". Replace in ApplicationComponents. */
public final class PendingDashboardController implements DashboardController {
    @Override
    public DashboardSummary load(AuthenticatedSession session) {
        return new DashboardSummary(null);
    }
}
