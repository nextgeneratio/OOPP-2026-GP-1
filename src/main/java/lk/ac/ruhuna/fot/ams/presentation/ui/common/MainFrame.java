package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.LearningActivityController;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.admin.AdminDashboard;
import lk.ac.ruhuna.fot.ams.presentation.ui.admin.CourseManagementScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.admin.UserManagementScreen;
import lk.ac.ruhuna.fot.ams.presentation.ui.lecturer.LecturerDashboard;
import lk.ac.ruhuna.fot.ams.presentation.ui.technical.TechnicalOfficerDashboard;
import lk.ac.ruhuna.fot.ams.presentation.ui.undergraduate.UndergraduateDashboard;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Application shell: top bar, role-filtered navigation, content area and status bar. */
public final class MainFrame extends JFrame {
    private final AuthenticatedSession session;
    private final DashboardController dashboards;
    private final UserController users;
    private final ProfileController profiles;
    private final CourseController courses;
    private final TimetableController timetables;
    private final ContentController courseContent;
    private final AttendanceController attendance;
    private final LearningActivityController learningActivities;
    private final ApplicationErrorHandler errors;
    private final Runnable onLogout;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<NavigationModule> created = EnumSet.noneOf(NavigationModule.class);
    private final NavigationPanel navigation;
    private final StatusBar statusBar;

    public MainFrame(AuthenticatedSession session, DashboardController dashboards,
                     UserController users, ProfileController profiles,
                     CourseController courses, TimetableController timetables,
                     ContentController courseContent, AttendanceController attendance,
                     LearningActivityController learningActivities,
                     ApplicationErrorHandler errors, Runnable onLogout) {
        super("Faculty of Technology Academic Management System");
        this.session = session;
        this.dashboards = dashboards;
        this.users = users;
        this.profiles = profiles;
        this.courses = courses;
        this.timetables = timetables;
        this.courseContent = courseContent;
        this.attendance = attendance;
        this.learningActivities = learningActivities;
        this.errors = errors;
        this.onLogout = onLogout;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 640));
        setLocationByPlatform(true);

        navigation = new NavigationPanel(session.role(), this::navigateTo);
        statusBar = new StatusBar("Signed in as " + session.username());
        JPanel root = new JPanel(new BorderLayout());
        root.add(new TopBar(session, () -> navigateTo(NavigationModule.PROFILE), this::confirmLogout), BorderLayout.NORTH);
        root.add(navigation, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
        setContentPane(root);
        navigateTo(NavigationModule.DASHBOARD);
    }

    public void navigateTo(NavigationModule module) {
        if (!module.isAllowedFor(session.role())) {
            JOptionPane.showMessageDialog(this, "You do not have permission to open this screen.",
                    "Permission required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (created.add(module)) {
            content.add(createScreen(module), module.name());
        }
        cards.show(content, module.name());
        navigation.select(module);
        statusBar.setMessage(module.label());
    }

    private JComponent createScreen(NavigationModule module) {
        if (module == NavigationModule.USER_MANAGEMENT) {
            return new UserManagementScreen(session, users, errors, null);
        }
        if (module == NavigationModule.STUDENT_MANAGEMENT) {
            return new UserManagementScreen(session, users, errors,
                    lk.ac.ruhuna.fot.ams.domain.enums.Role.UNDERGRADUATE);
        }
        if (module == NavigationModule.PROFILE) {
            return new ProfileScreen(session, profiles, errors);
        }
        if (module == NavigationModule.COURSE_MANAGEMENT) {
            return new CourseManagementScreen(session, courses);
        }
        if (module == NavigationModule.TIMETABLE) {
            return new TimetableScreen(session, timetables);
        }
        if (module == NavigationModule.COURSE_OFFERINGS) {
            return new CourseOfferingsScreen(session, courses);
        }
        if (module == NavigationModule.ATTENDANCE) {
            return new AttendanceScreen(session, attendance);
        }
        if (module == NavigationModule.COURSE_MATERIALS) {
            javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
            tabs.addTab("Materials", new CourseMaterialsScreen(session, courseContent));
            tabs.addTab("Quizzes and assignments", new LearningActivitiesScreen(session, learningActivities));
            return tabs;
        }
        if (module != NavigationModule.DASHBOARD) {
            return new PlaceholderScreen(module);
        }
        return switch (session.role()) {
            case ADMIN -> new AdminDashboard(session, dashboards, errors, this::navigateTo);
            case LECTURER -> new LecturerDashboard(session, dashboards, errors, this::navigateTo);
            case TECHNICAL_OFFICER -> new TechnicalOfficerDashboard(session, dashboards, errors, this::navigateTo);
            case UNDERGRADUATE -> new UndergraduateDashboard(session, dashboards, errors, this::navigateTo);
        };
    }

    private void confirmLogout() {
        int choice = JOptionPane.showConfirmDialog(this, "Do you want to log out?", "Confirm log out",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            onLogout.run();
        }
    }
}
