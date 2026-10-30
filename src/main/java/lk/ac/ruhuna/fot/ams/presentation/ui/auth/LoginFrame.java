package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.LearningActivityController;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.MainFrame;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Login window containing the Nirmal banner/form UI; authenticated navigation stays on Amjad's wired MainFrame. */
public final class LoginFrame extends JFrame {
    private final AuthController authController;
    private final DashboardController dashboardController;
    private final UserController userController;
    private final ProfileController profileController;
    private final CourseController courseController;
    private final TimetableController timetableController;
    private final ContentController contentController;
    private final AttendanceController attendanceController;
    private final LearningActivityController learningActivityController;
    private final MedicalController medicalController;
    private final AssessmentController assessmentController;
    private final MarksController marksController;
    private final EligibilityController eligibilityController;
    private final GpaController gpaController;
    private final ReportController reportController;
    private final ResultController resultController;
    private final ApplicationErrorHandler errorHandler;

    public LoginFrame(AuthController authController, DashboardController dashboardController,
                      UserController userController, ProfileController profileController,
                      CourseController courseController, TimetableController timetableController,
                      ContentController contentController, AttendanceController attendanceController,
                      LearningActivityController learningActivityController, MedicalController medicalController,
                      AssessmentController assessmentController, MarksController marksController,
                      EligibilityController eligibilityController, GpaController gpaController,
                      ReportController reportController, ResultController resultController,
                      ApplicationErrorHandler errorHandler) {
        super("Academic Management System - Login");
        this.authController = authController;
        this.dashboardController = dashboardController;
        this.userController = userController;
        this.profileController = profileController;
        this.courseController = courseController;
        this.timetableController = timetableController;
        this.contentController = contentController;
        this.attendanceController = attendanceController;
        this.learningActivityController = learningActivityController;
        this.medicalController = medicalController;
        this.assessmentController = assessmentController;
        this.marksController = marksController;
        this.eligibilityController = eligibilityController;
        this.gpaController = gpaController;
        this.reportController = reportController;
        this.resultController = resultController;
        this.errorHandler = errorHandler;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LoginPanel panel = new LoginPanel(authController::login, errorHandler, this::openShell);
        setContentPane(panel);
        getRootPane().setDefaultButton(panel.defaultButton());
        setMinimumSize(new Dimension(900, 560));
        setSize(1100, 660);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent event) { panel.focusUsername(); }
        });
    }

    private void openShell(AuthenticatedSession session) {
        dispose();
        new MainFrame(session, dashboardController, userController, profileController, courseController,
                timetableController, contentController, attendanceController, learningActivityController,
                medicalController, assessmentController, marksController, eligibilityController, gpaController,
                reportController, resultController, errorHandler,
                () -> new LoginFrame(authController, dashboardController, userController, profileController,
                        courseController, timetableController, contentController, attendanceController,
                        learningActivityController, medicalController, assessmentController, marksController,
                        eligibilityController, gpaController, reportController, resultController, errorHandler)
                        .setVisible(true)).setVisible(true);
    }
}
