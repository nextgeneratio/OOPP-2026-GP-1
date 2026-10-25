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
    void adminSeesManagementModulesOnly() {
        assertThat(NavigationModule.forRole(Role.ADMIN)).contains(NavigationModule.USER_MANAGEMENT,
                NavigationModule.STUDENT_MANAGEMENT, NavigationModule.COURSE_MANAGEMENT,
                NavigationModule.ASSESSMENTS_MARKS, NavigationModule.REPORTS)
                .doesNotContain(NavigationModule.MEDICAL);
    }

    @Test
    void lecturerCanViewMedicalButCannotSeeAdminModules() {
        assertThat(NavigationModule.forRole(Role.LECTURER)).contains(NavigationModule.ASSESSMENTS_MARKS,
                NavigationModule.ELIGIBILITY, NavigationModule.GRADES_GPA, NavigationModule.MEDICAL)
                .doesNotContain(NavigationModule.USER_MANAGEMENT);
    }

    @Test
    void technicalOfficerSeesAttendanceAndMedical() {
        assertThat(NavigationModule.forRole(Role.TECHNICAL_OFFICER)).contains(NavigationModule.ATTENDANCE,
                NavigationModule.MEDICAL, NavigationModule.TIMETABLE)
                .doesNotContain(NavigationModule.COURSE_MANAGEMENT,
                        NavigationModule.ASSESSMENTS_MARKS, NavigationModule.COURSE_MATERIALS);
    }

    @Test
    void undergraduateIsReadOnlyModulesOnly() {
        assertThat(NavigationModule.forRole(Role.UNDERGRADUATE)).contains(NavigationModule.ATTENDANCE,
                NavigationModule.GRADES_GPA, NavigationModule.MEDICAL).doesNotContain(NavigationModule.USER_MANAGEMENT,
                NavigationModule.ASSESSMENTS_MARKS, NavigationModule.REPORTS);
    }
}
