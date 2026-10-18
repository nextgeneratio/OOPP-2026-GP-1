package lk.ac.ruhuna.fot.ams.presentation.ui.technical;

import java.util.List;
import java.util.function.Consumer;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.RoleDashboard;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class TechnicalOfficerDashboard extends RoleDashboard {
    public TechnicalOfficerDashboard(AuthenticatedSession session, DashboardController controller,
              ApplicationErrorHandler errors, Consumer<NavigationModule> navigate) {
        super("Technical Officer Dashboard", "Attendance, medical and timetable tasks",
                List.of("Department", "Attendance tasks", "Medical approvals pending", "Timetable"),
                List.of(NavigationModule.ATTENDANCE, NavigationModule.MEDICAL, NavigationModule.TIMETABLE, NavigationModule.REPORTS, NavigationModule.PROFILE),
                session, controller, errors, navigate);
    }
}
