package lk.ac.ruhuna.fot.ams.presentation.ui.admin;

import java.util.List;
import java.util.function.Consumer;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.RoleDashboard;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AdminDashboard extends RoleDashboard {
    public AdminDashboard(AuthenticatedSession session, DashboardController controller,
              ApplicationErrorHandler errors, Consumer<NavigationModule> navigate) {
        super("Administrator Dashboard", "System-wide overview",
                List.of("Users", "Departments / Batches", "Courses", "Offerings", "Notices", "Timetable status"),
                List.of(NavigationModule.USER_MANAGEMENT, NavigationModule.STUDENT_MANAGEMENT, NavigationModule.COURSE_MANAGEMENT, NavigationModule.TIMETABLE, NavigationModule.NOTICES, NavigationModule.REPORTS),
                session, controller, errors, navigate);
    }
}
