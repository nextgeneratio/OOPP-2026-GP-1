package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportCategory;
import org.junit.jupiter.api.Test;

class ReportCategoryTest {
    @Test
    void eachRoleIsOfferedOnlyItsOwnReports() {
        assertThat(ReportCategory.forRole(Role.ADMIN)).containsExactly(ReportCategory.STUDENT_PROFILE);
        assertThat(ReportCategory.forRole(Role.TECHNICAL_OFFICER)).containsExactly(ReportCategory.STUDENT_ATTENDANCE,
                ReportCategory.BATCH_COURSE_ATTENDANCE, ReportCategory.MEDICAL);
        assertThat(ReportCategory.forRole(Role.LECTURER)).containsExactlyInAnyOrder(ReportCategory.values());
        assertThat(ReportCategory.forRole(Role.UNDERGRADUATE)).isEmpty();
    }

    @Test
    void onlyStudentLevelReportsAskForAStudentAndAdministratorsNeedNoOffering() {
        assertThat(ReportCategory.BATCH_COURSE_ATTENDANCE.needsStudent()).isFalse();
        assertThat(ReportCategory.ELIGIBILITY.needsStudent()).isTrue();
        assertThat(ReportCategory.STUDENT_PROFILE.needsOffering(Role.ADMIN)).isFalse();
        assertThat(ReportCategory.STUDENT_PROFILE.needsOffering(Role.LECTURER)).isTrue();
    }
}
