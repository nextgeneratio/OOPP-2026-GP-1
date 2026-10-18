package lk.ac.ruhuna.fot.ams.presentation.ui.lecturer;

import java.util.List;
import java.util.function.Consumer;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.RoleDashboard;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LecturerDashboard extends RoleDashboard {
    public LecturerDashboard(AuthenticatedSession session, DashboardController controller,
              ApplicationErrorHandler errors, Consumer<NavigationModule> navigate) {
        super("Lecturer Dashboard", "Your course offerings and academic tasks",
                List.of("Assigned offerings", "Recent notices", "Pending mark tasks", "Eligibility views"),
                List.of(NavigationModule.COURSE_MATERIALS, NavigationModule.ASSESSMENTS_MARKS, NavigationModule.ELIGIBILITY, NavigationModule.GRADES_GPA, NavigationModule.REPORTS, NavigationModule.PROFILE),
                session, controller, errors, navigate);
    }
}
