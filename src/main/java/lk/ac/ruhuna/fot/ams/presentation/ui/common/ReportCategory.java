package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

/** Only report categories backed by the current Amjad services are exposed. */
public enum ReportCategory {
    STUDENT_ATTENDANCE("Student attendance", true, EnumSet.of(Role.LECTURER, Role.TECHNICAL_OFFICER)),
    BATCH_COURSE_ATTENDANCE("Batch / course attendance", false, EnumSet.of(Role.LECTURER, Role.TECHNICAL_OFFICER)),
    CA_FINAL_MARKS("CA and final marks", true, EnumSet.of(Role.LECTURER)),
    ELIGIBILITY("Final exam eligibility", true, EnumSet.of(Role.LECTURER));

    private final String label;
    private final boolean needsStudent;
    private final Set<Role> roles;
    ReportCategory(String label, boolean needsStudent, Set<Role> roles) {
        this.label = label;
        this.needsStudent = needsStudent;
        this.roles = roles;
    }
    public boolean needsStudent() { return needsStudent; }
    public boolean needsOffering(Role role) { return true; }
    public static List<ReportCategory> forRole(Role role) {
        return Arrays.stream(values()).filter(category -> category.roles.contains(role)).toList();
    }
    @Override public String toString() { return label; }
}
