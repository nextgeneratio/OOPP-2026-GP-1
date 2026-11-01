package lk.ac.ruhuna.fot.ams.presentation.ui.undergraduate;

import java.util.List;
import java.util.function.Consumer;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.RoleDashboard;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class UndergraduateDashboard extends RoleDashboard {
    public UndergraduateDashboard(AuthenticatedSession session, DashboardController controller,
              ApplicationErrorHandler errors, Consumer<NavigationModule> navigate) {
        super("My Dashboard", "Your courses, attendance and results (read-only)",
                List.of("Student number", "Current courses", "Attendance", "Eligibility", "Latest SGPA / CGPA", "Notices"),
                List.of(NavigationModule.ATTENDANCE, NavigationModule.ELIGIBILITY, NavigationModule.GRADES_GPA, NavigationModule.COURSE_MATERIALS, NavigationModule.TIMETABLE, NavigationModule.NOTICES),
                session, controller, errors, navigate);
    }
}
