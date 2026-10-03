package lk.ac.ruhuna.fot.ams.api.controller;

import lk.ac.ruhuna.fot.ams.api.response.DashboardSummary;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Contract the dashboards code against. The real implementation belongs to the business/API owners. */
public interface DashboardController {
    DashboardSummary load(AuthenticatedSession session);
}
