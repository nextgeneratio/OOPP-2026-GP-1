package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.ScreenLookupController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.LearningActivityController;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;

/** The API controllers the screens may call. Swing never receives services, DAOs or connections. */
public record UiServices(
        DashboardController dashboard, UserController users, MarksController marks,
        AttendanceController attendance, EligibilityController eligibility, GradesController grades,
        ContentController content, MedicalController medical, TimetableController timetable,
        ScreenLookupController screens, ProfileController profiles, CourseController courses,
        LearningActivityController learningActivities) {
}
