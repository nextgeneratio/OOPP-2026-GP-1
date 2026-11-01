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
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
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
    private final MedicalController medical;
    private final AssessmentController assessments;
    private final MarksController marks;
    private final EligibilityController eligibility;
    private final GpaController gpa;
    private final ReportController reports;
    private final ResultController results;
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
                     LearningActivityController learningActivities, MedicalController medical,
                     AssessmentController assessments, MarksController marks, EligibilityController eligibility,
                     GpaController gpa, ReportController reports, ResultController results,
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
        this.medical = medical;
        this.assessments = assessments; this.marks = marks; this.eligibility = eligibility;
        this.gpa = gpa; this.reports = reports; this.results = results;
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
            return new UserManagementScreen(session, users, courses, errors, null);
        }
        if (module == NavigationModule.STUDENT_MANAGEMENT) {
            return new UserManagementScreen(session, users, courses, errors,
                    lk.ac.ruhuna.fot.ams.domain.enums.Role.UNDERGRADUATE);
        }
        if (module == NavigationModule.PROFILE) {
            return new ProfileScreen(session, profiles, errors);
        }
        if (module == NavigationModule.COURSE_MANAGEMENT) {
            return new CourseManagementScreen(session, courses);
        }
        if (module == NavigationModule.TIMETABLE) {
            return new TimetableScreen(session, timetables, courses);
        }
        if (module == NavigationModule.COURSE_OFFERINGS) {
            return new CourseOfferingsScreen(session, courses);
        }
        if (module == NavigationModule.ATTENDANCE) {
            return new AttendanceScreen(session, attendance, courses);
        }
        if (module == NavigationModule.COURSE_MATERIALS) {
            javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
            tabs.addTab("Materials", new CourseMaterialsScreen(session, courseContent, courses));
            tabs.addTab("Quizzes and assignments", new LearningActivitiesScreen(session, learningActivities, courses));
            return tabs;
        }
        if (module == NavigationModule.NOTICES) {
            return new NoticesScreen(session, courseContent, courses);
        }
        if (module == NavigationModule.MEDICAL) {
            return new MedicalScreen(session, medical, courses);
        }
        if (module == NavigationModule.ASSESSMENTS_MARKS) {
            if (session.role() == lk.ac.ruhuna.fot.ams.domain.enums.Role.LECTURER) {
                javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
                tabs.addTab("Assessment plan", new AssessmentsMarksScreen(session, courses, assessments, marks));
                tabs.addTab("Marks sheet", new lk.ac.ruhuna.fot.ams.presentation.ui.lecturer.MarksScreen(session, marks));
                return tabs;
            }
            return new AssessmentsMarksScreen(session, courses, assessments, marks);
        }
        if (module == NavigationModule.ELIGIBILITY) {
            return new EligibilityScreen(session, eligibility);
        }
        if (module == NavigationModule.GRADES_GPA) {
            return new GradesGpaScreen(session, courses, attendance, gpa, results);
        }
        if (module == NavigationModule.REPORTS) {
            return new ReportScreen(session, new UiServices(courses, attendance, eligibility, reports));
        }
        if (module != NavigationModule.DASHBOARD) {
            throw new IllegalStateException("No Swing screen is registered for " + module);
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
