package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;

/** Controller-only adapter for the Nirmal report UI, limited to Amjad's supported report APIs. */
public record UiServices(CourseController courses, AttendanceController attendance,
        EligibilityController eligibility, ReportController reports) { }
