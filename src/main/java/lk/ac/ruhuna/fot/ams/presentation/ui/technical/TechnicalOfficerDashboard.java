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
        super("Technical Officer Dashboard", "Course, medical, timetable, notice and report tasks",
                List.of("Department", "Timetable"),
                List.of(NavigationModule.COURSE_MANAGEMENT, NavigationModule.COURSE_OFFERINGS, NavigationModule.MEDICAL, NavigationModule.TIMETABLE,
                        NavigationModule.NOTICES, NavigationModule.REPORTS, NavigationModule.PROFILE),
                session, controller, errors, navigate);
    }
}
