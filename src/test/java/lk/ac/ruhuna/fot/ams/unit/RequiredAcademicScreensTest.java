package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JTextField;

import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.business.service.CourseService.OfferingView;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AssessmentsMarksScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AttendanceScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.EligibilityScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.GradesGpaScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiServices;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class RequiredAcademicScreensTest {
    private final AuthenticatedSession lecturer =
            new AuthenticatedSession(7, "lecturer", Role.LECTURER, "Lecturer");
    private final CourseController courses = mock(CourseController.class);
    private final AttendanceController attendance = mock(AttendanceController.class);

    @Test
    void requiredAcademicDestinationsCreateDedicatedSwingPanels() {
        assertThat(new AssessmentsMarksScreen(lecturer, courses,
                mock(AssessmentController.class), mock(MarksController.class)))
                .isInstanceOf(AssessmentsMarksScreen.class);
        var eligibility = mock(EligibilityController.class);
        when(eligibility.myOfferings(lecturer)).thenReturn(ApiResponse.success(List.of()));
        when(courses.offeringSummariesForActor(lecturer)).thenReturn(ApiResponse.success(List.of()));
        assertThat(new EligibilityScreen(lecturer, eligibility))
                .isInstanceOf(EligibilityScreen.class);
        assertThat(new GradesGpaScreen(lecturer, courses, attendance, mock(GpaController.class),
                mock(ResultController.class))).isInstanceOf(GradesGpaScreen.class);
        assertThat(new ReportScreen(lecturer, new UiServices(courses, attendance, eligibility,
                mock(ReportController.class)))).isInstanceOf(ReportScreen.class);
    }

    @Test
    void attendanceScreenDoesNotAskForInternalIds() {
        when(courses.offeringViewsForActor(lecturer)).thenReturn(ApiResponse.success(List.of()));
        AttendanceScreen screen = new AttendanceScreen(lecturer, mock(AttendanceController.class), courses);
        assertThat(descendants(screen)).noneMatch(JTextField.class::isInstance);
        assertThat(descendants(screen).stream().filter(JLabel.class::isInstance)
                .map(JLabel.class::cast).map(JLabel::getText).filter(java.util.Objects::nonNull))
                .noneMatch(label -> label.toLowerCase().contains("id"));
    }

    @Test
    void undergraduateAttendanceScreenDoesNotRequestOfferingWideSessionList() {
        AuthenticatedSession student = new AuthenticatedSession(9, "student", Role.UNDERGRADUATE, "Student");
        CourseDao.OfferingRow offering = new CourseDao.OfferingRow(8, 12, 1, 3, null, "OPEN");
        CourseDao.CourseRow course = new CourseDao.CourseRow(12, 1, "ICT2132", "OOP", 3.0, true);
        when(courses.offeringViewsForActor(student)).thenReturn(ApiResponse.success(
                List.of(new OfferingView(offering, course, true))));
        when(attendance.sessionSetup(student, offering.id())).thenReturn(ApiResponse.success(
                new AttendanceService.SessionSetup(
                        List.of(new CourseDao.ComponentRow(4, course.id(), ComponentType.THEORY, 2.0, 15)),
                        List.of(), List.of(new AttendanceService.RosterEntry(31, 19, "STU019", "Student")))));

        new AttendanceScreen(student, attendance, courses);

        verify(attendance).sessionSetup(student, offering.id());
        verify(attendance, never()).sessionsForOffering(student, offering.id());
    }

    private static List<Component> descendants(Container root) {
        java.util.ArrayList<Component> found = new java.util.ArrayList<>();
        for (Component child : root.getComponents()) {
            found.add(child);
            if (child instanceof Container nested) found.addAll(descendants(nested));
        }
        return found;
    }
}
