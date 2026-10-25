package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

/**
 * The report categories and which roles are offered each one. This mirrors the report permissions the services
 * enforce; it only decides what the menu lists, the services still refuse anything outside a user's scope.
 */
public enum ReportCategory {
    STUDENT_ATTENDANCE("Student attendance", true, EnumSet.of(Role.LECTURER, Role.TECHNICAL_OFFICER)),
    BATCH_COURSE_ATTENDANCE("Batch / course attendance", false, EnumSet.of(Role.LECTURER, Role.TECHNICAL_OFFICER)),
    CA_FINAL_MARKS("CA and final marks", true, EnumSet.of(Role.LECTURER)),
    ELIGIBILITY("Final exam eligibility", true, EnumSet.of(Role.LECTURER)),
    GRADES("Course results (grades)", true, EnumSet.of(Role.LECTURER)),
    SGPA_CGPA("SGPA and CGPA", true, EnumSet.of(Role.LECTURER)),
    MEDICAL("Medical records", true, EnumSet.of(Role.LECTURER, Role.TECHNICAL_OFFICER)),
    STUDENT_PROFILE("Student profile", false, EnumSet.of(Role.ADMIN, Role.LECTURER));

    private final String label;
    private final boolean needsStudent;
    private final Set<Role> roles;

    ReportCategory(String label, boolean needsStudent, Set<Role> roles) {
        this.label = label;
        this.needsStudent = needsStudent;
        this.roles = roles;
    }

    public String label() {
        return label;
    }

    /** True when the report is about one student, so the screen asks for a student as well as an offering. */
    public boolean needsStudent() {
        return needsStudent;
    }

    /** Administrators have no offerings of their own; their profile report lists all students. */
    public boolean needsOffering(Role role) {
        return !(this == STUDENT_PROFILE && role == Role.ADMIN);
    }

    public static List<ReportCategory> forRole(Role role) {
        return Arrays.stream(values()).filter(category -> category.roles.contains(role)).toList();
    }

    @Override
    public String toString() {
        return label;
    }
}
