package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
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
        EligibilityController eligibility = mock(EligibilityController.class);
        when(eligibility.myOfferings(lecturer)).thenReturn(ApiResponse.success(java.util.List.of()));
        assertThat(new AssessmentsMarksScreen(lecturer, courses, attendance,
                mock(AssessmentController.class), mock(MarksController.class)))
                .isInstanceOf(AssessmentsMarksScreen.class);
        assertThat(new EligibilityScreen(lecturer, eligibility))
                .isInstanceOf(EligibilityScreen.class);
        assertThat(new GradesGpaScreen(lecturer, mock(GradesController.class),
                eligibility)).isInstanceOf(GradesGpaScreen.class);
        assertThat(new ReportsScreen(lecturer, mock(ReportController.class),
                eligibility, attendance))
                .isInstanceOf(ReportsScreen.class);
    }
}
