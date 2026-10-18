package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;

/** Navigable modules and the roles whose menu shows them. Display filter only: services still authorize every call. */
public enum NavigationModule {
    DASHBOARD("Dashboard", Role.values()),
    USER_MANAGEMENT("User Management", Role.ADMIN),
    STUDENT_MANAGEMENT("Student Management", Role.ADMIN),
    COURSE_MANAGEMENT("Course Management", Role.ADMIN),
    COURSE_MATERIALS("Course Materials", Role.LECTURER, Role.UNDERGRADUATE),
    ASSESSMENTS_MARKS("Assessments & Marks", Role.LECTURER),
    ATTENDANCE("Attendance", Role.TECHNICAL_OFFICER, Role.UNDERGRADUATE),
    MEDICAL("Medical", Role.TECHNICAL_OFFICER),
    ELIGIBILITY("Eligibility", Role.LECTURER, Role.UNDERGRADUATE),
    GRADES_GPA("Grades & GPA", Role.LECTURER, Role.UNDERGRADUATE),
    TIMETABLE("Timetable", Role.ADMIN, Role.TECHNICAL_OFFICER, Role.UNDERGRADUATE),
    NOTICES("Notices", Role.values()),
    REPORTS("Reports", Role.ADMIN, Role.LECTURER, Role.TECHNICAL_OFFICER),
    PROFILE("Profile", Role.LECTURER, Role.TECHNICAL_OFFICER, Role.UNDERGRADUATE);

    private final String label;
    private final Set<Role> roles;

    NavigationModule(String label, Role... roles) {
        this.label = label;
        this.roles = EnumSet.copyOf(Arrays.asList(roles));
    }

    public String label() {
        return label;
    }

    public boolean isAllowedFor(Role role) {
        return roles.contains(role);
    }

    public static List<NavigationModule> forRole(Role role) {
        return Arrays.stream(values()).filter(module -> module.isAllowedFor(role)).toList();
    }

    public static String roleLabel(Role role) {
        return switch (role) {
            case ADMIN -> "Administrator";
            case LECTURER -> "Lecturer";
            case TECHNICAL_OFFICER -> "Technical Officer";
            case UNDERGRADUATE -> "Undergraduate";
        };
    }
}
