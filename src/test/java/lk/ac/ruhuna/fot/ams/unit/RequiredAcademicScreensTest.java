package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AssessmentsMarksScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.EligibilityScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.GradesGpaScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportsScreen;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class RequiredAcademicScreensTest {
    private final AuthenticatedSession lecturer =
            new AuthenticatedSession(7, "lecturer", Role.LECTURER, "Lecturer");
    private final CourseController courses = mock(CourseController.class);
    private final AttendanceController attendance = mock(AttendanceController.class);

    @Test
    void requiredAcademicDestinationsCreateDedicatedSwingPanels() {
        assertThat(new AssessmentsMarksScreen(lecturer, courses, attendance,
                mock(AssessmentController.class), mock(MarksController.class)))
                .isInstanceOf(AssessmentsMarksScreen.class);
        assertThat(new EligibilityScreen(lecturer, courses, attendance, mock(EligibilityController.class)))
                .isInstanceOf(EligibilityScreen.class);
        assertThat(new GradesGpaScreen(lecturer, courses, attendance, mock(GpaController.class),
                mock(ResultController.class))).isInstanceOf(GradesGpaScreen.class);
        assertThat(new ReportsScreen(lecturer, courses, attendance, mock(ReportController.class)))
                .isInstanceOf(ReportsScreen.class);
    }
}
