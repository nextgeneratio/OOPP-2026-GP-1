package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.NavigationModule;
import org.junit.jupiter.api.Test;

class NavigationModuleTest {
    @Test
    void everyRoleSeesDashboardAndNotices() {
        for (Role role : Role.values()) {
            assertThat(NavigationModule.forRole(role))
                    .contains(NavigationModule.DASHBOARD, NavigationModule.NOTICES,
                            NavigationModule.COURSE_OFFERINGS);
        }
    }

    @Test
    void adminCannotSeeAcademicReportsOrSensitiveAssessmentModulesButCanReviewMedicalRequests() {
        assertThat(NavigationModule.forRole(Role.ADMIN)).contains(NavigationModule.USER_MANAGEMENT,
                NavigationModule.STUDENT_MANAGEMENT, NavigationModule.COURSE_MANAGEMENT,
                NavigationModule.MEDICAL)
                .doesNotContain(NavigationModule.ASSESSMENTS_MARKS, NavigationModule.REPORTS);
    }

    @Test
    void lecturerCannotSeeMedicalButCanSeeAssignedAcademicModules() {
        assertThat(NavigationModule.forRole(Role.LECTURER)).contains(NavigationModule.ASSESSMENTS_MARKS,
                NavigationModule.ELIGIBILITY, NavigationModule.GRADES_GPA)
                .doesNotContain(NavigationModule.USER_MANAGEMENT, NavigationModule.MEDICAL);
    }

    @Test
    void technicalOfficerCanMaintainAttendanceAlongWithCoursesAndMedical() {
        assertThat(NavigationModule.forRole(Role.TECHNICAL_OFFICER)).contains(NavigationModule.COURSE_MANAGEMENT,
                NavigationModule.ATTENDANCE, NavigationModule.MEDICAL, NavigationModule.TIMETABLE, NavigationModule.NOTICES,
                NavigationModule.REPORTS)
                .doesNotContain(NavigationModule.ASSESSMENTS_MARKS,
                        NavigationModule.COURSE_MATERIALS);
    }

    @Test
    void undergraduateIsReadOnlyModulesOnly() {
        assertThat(NavigationModule.forRole(Role.UNDERGRADUATE)).contains(NavigationModule.ATTENDANCE,
                NavigationModule.GRADES_GPA, NavigationModule.MEDICAL).doesNotContain(NavigationModule.USER_MANAGEMENT,
                NavigationModule.ASSESSMENTS_MARKS, NavigationModule.REPORTS);
    }
}
