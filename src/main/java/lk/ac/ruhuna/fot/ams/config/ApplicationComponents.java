package lk.ac.ruhuna.fot.ams.config;

import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.api.controller.ApiControllerSupport;
import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController;
import lk.ac.ruhuna.fot.ams.api.controller.ContentController;
import lk.ac.ruhuna.fot.ams.api.controller.CourseController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.GpaController;
import lk.ac.ruhuna.fot.ams.api.controller.MarksController;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController;
import lk.ac.ruhuna.fot.ams.api.controller.ProfileController;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController;
import lk.ac.ruhuna.fot.ams.api.controller.ResultController;
import lk.ac.ruhuna.fot.ams.api.controller.TimetableController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy;
import lk.ac.ruhuna.fot.ams.business.service.AcademicResultService;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.business.service.AttendanceService;
import lk.ac.ruhuna.fot.ams.business.service.AuditService;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.ContentService;
import lk.ac.ruhuna.fot.ams.business.service.CourseService;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.business.service.GpaService;
import lk.ac.ruhuna.fot.ams.business.service.GradeService;
import lk.ac.ruhuna.fot.ams.business.service.MarksService;
import lk.ac.ruhuna.fot.ams.business.service.MedicalService;
import lk.ac.ruhuna.fot.ams.business.service.ProfileService;
import lk.ac.ruhuna.fot.ams.business.service.ReportService;
import lk.ac.ruhuna.fot.ams.business.service.TimetableService;
import lk.ac.ruhuna.fot.ams.business.service.UserService;
import lk.ac.ruhuna.fot.ams.data.connection.ConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcConnectionProvider;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcAuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcAuthorizationScopeDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcAssessmentDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcAttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcContentDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcCourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcCourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcGradeDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcMedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcTimetableDao;
import lk.ac.ruhuna.fot.ams.data.dao.JdbcUserDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;

public final class ApplicationComponents {
    private final ConnectionProvider connectionProvider;
    private final AuthController authController;
    private final ApplicationErrorHandler errorHandler;
    private final AuthorizationService authorizationService;
    private final AuditService auditService;
    private final CourseService courseService;
    private final AssessmentService assessmentService;
    private final MarksService marksService;
    private final AttendanceService attendanceService;
    private final MedicalService medicalService;
    private final ContentService contentService;
    private final TimetableService timetableService;
    private final GradeService gradeService;
    private final GpaService gpaService;
    private final EligibilityService eligibilityService;
    private final UserService userService;
    private final ProfileService profileService;
    private final ReportService reportService;
    private final AcademicResultService academicResultService;
    private final CourseController courseController;
    private final UserController userController;
    private final AssessmentController assessmentController;
    private final MarksController marksController;
    private final AttendanceController attendanceController;
    private final MedicalController medicalController;
    private final EligibilityController eligibilityController;
    private final GpaController gpaController;
    private final ProfileController profileController;
    private final TimetableController timetableController;
    private final ContentController contentController;
    private final ReportController reportController;
    private final ResultController resultController;

    private ApplicationComponents(ConnectionProvider connectionProvider,
                                 AuthController authController,
                                 ApplicationErrorHandler errorHandler,
                                 AuthorizationService authorizationService,
                                 AuditService auditService,
                                 CourseService courseService,
                                 AssessmentService assessmentService,
                                 MarksService marksService,
                                 AttendanceService attendanceService,
                                 MedicalService medicalService,
                                 ContentService contentService,
                                 TimetableService timetableService,
                                 GradeService gradeService,
                                 GpaService gpaService,
                                 EligibilityService eligibilityService,
                                 UserService userService,
                                 ProfileService profileService,
                                 ReportService reportService,
                                 AcademicResultService academicResultService,
                                 CourseController courseController,
                                 UserController userController,
                                 AssessmentController assessmentController,
                                 MarksController marksController,
                                 AttendanceController attendanceController,
                                 MedicalController medicalController,
                                 EligibilityController eligibilityController,
                                 GpaController gpaController,
                                 ProfileController profileController,
                                 TimetableController timetableController,
                                 ContentController contentController,
                                 ReportController reportController,
                                 ResultController resultController) {
        this.connectionProvider = connectionProvider;
        this.authController = authController;
        this.errorHandler = errorHandler;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.courseService = courseService;
        this.assessmentService = assessmentService;
        this.marksService = marksService;
        this.attendanceService = attendanceService;
        this.medicalService = medicalService;
        this.contentService = contentService;
        this.timetableService = timetableService;
        this.gradeService = gradeService;
        this.gpaService = gpaService;
        this.eligibilityService = eligibilityService;
        this.userService = userService;
        this.profileService = profileService;
        this.reportService = reportService;
        this.academicResultService = academicResultService;
        this.courseController = courseController;
        this.userController = userController;
        this.assessmentController = assessmentController;
        this.marksController = marksController;
        this.attendanceController = attendanceController;
        this.medicalController = medicalController;
        this.eligibilityController = eligibilityController;
        this.gpaController = gpaController;
        this.profileController = profileController;
        this.timetableController = timetableController;
        this.contentController = contentController;
        this.reportController = reportController;
        this.resultController = resultController;
    }

    public static ApplicationComponents create(AppConfig config) {
        ConnectionProvider connectionProvider = new JdbcConnectionProvider(config);
        JdbcExecutor jdbc = new JdbcExecutor(connectionProvider);
        TransactionManager transactionManager = new TransactionManager(connectionProvider);
        java.time.Clock clock = java.time.Clock.systemDefaultZone();
        PasswordHasher passwordHasher = new PasswordHasher();
        JdbcUserDao userDao = new JdbcUserDao(connectionProvider);
        JdbcCourseDao courseDao = new JdbcCourseDao(jdbc);
        JdbcAssessmentDao assessmentDao = new JdbcAssessmentDao(jdbc);
        JdbcAttendanceDao attendanceDao = new JdbcAttendanceDao(jdbc);
        JdbcMedicalDao medicalDao = new JdbcMedicalDao(jdbc);
        JdbcContentDao contentDao = new JdbcContentDao(jdbc);
        JdbcTimetableDao timetableDao = new JdbcTimetableDao(jdbc);
        JdbcGradeDao gradeDao = new JdbcGradeDao(jdbc);
        CourseResultDao courseResultDao = new JdbcCourseResultDao(jdbc);
        AuditLogDao auditLogDao = new JdbcAuditLogDao(jdbc);
        AssessmentCalculator assessmentCalculator = new AssessmentCalculator();
        AuditService auditService = new AuditService(
                auditLogDao, transactionManager, clock);
        AuthService authService = new AuthService(userDao, passwordHasher, auditService);
        AuthorizationService authorizationService =
                new AuthorizationService(new JdbcAuthorizationScopeDao(jdbc));
        CourseService courseService = new CourseService(
                courseDao, auditLogDao, transactionManager, authorizationService, clock);
        AssessmentService assessmentService = new AssessmentService(
                assessmentDao, auditLogDao, transactionManager, authorizationService,
                assessmentCalculator, clock);
        MarksService marksService = new MarksService(
                assessmentDao, courseDao, auditLogDao, transactionManager,
                authorizationService, assessmentCalculator, clock);
        AttendanceService attendanceService = new AttendanceService(
                attendanceDao, courseDao, timetableDao, medicalDao, auditLogDao, transactionManager,
                authorizationService, new AttendanceCalculator(), config.attendanceThreshold(), clock);
        MedicalService medicalService = new MedicalService(
                medicalDao, courseDao, auditLogDao, transactionManager,
                authorizationService, clock);
        ContentService contentService = new ContentService(
                contentDao, auditLogDao, transactionManager, authorizationService, clock);
        TimetableService timetableService = new TimetableService(
                timetableDao, courseDao, auditLogDao, transactionManager,
                authorizationService, clock);
        GradeService gradeService = new GradeService(gradeDao);
        GpaService gpaService = new GpaService(
                new GpaCalculator(),
                GpaCalculator.RepeatSelectionRule.valueOf(config.repeatSelectionRule()),
                courseResultDao, authorizationService);
        EligibilityService eligibilityService = new EligibilityService(
                new FinalExamEligibilityPolicy(), marksService, attendanceService, authorizationService);
        UserService userService = new UserService(
                userDao, auditLogDao, transactionManager, authorizationService,
                passwordHasher, clock);
        ProfileService profileService = new ProfileService(
                userDao, auditLogDao, transactionManager, authorizationService, clock);
        ReportService reportService = new ReportService(
                courseDao, attendanceDao, assessmentDao, authorizationService);
        AcademicResultService academicResultService = new AcademicResultService(
                courseDao, courseResultDao, marksService, eligibilityService, gradeService,
                auditLogDao, transactionManager, authorizationService, clock,
                config.gradeSchemeVersion());
        ApplicationErrorHandler errorHandler = new ApplicationErrorHandler();
        ApiControllerSupport apiSupport = new ApiControllerSupport(errorHandler);
        return new ApplicationComponents(connectionProvider, new AuthController(authService, apiSupport),
            errorHandler, authorizationService, auditService, courseService,
            assessmentService, marksService, attendanceService, medicalService, contentService,
            timetableService, gradeService, gpaService, eligibilityService, userService,
            profileService, reportService, academicResultService,
            new CourseController(courseService, apiSupport),
            new UserController(userService, apiSupport),
            new AssessmentController(assessmentService, apiSupport),
            new MarksController(marksService, apiSupport),
            new AttendanceController(attendanceService, apiSupport),
            new MedicalController(medicalService, apiSupport),
            new EligibilityController(eligibilityService, apiSupport),
            new GpaController(gpaService, apiSupport),
            new ProfileController(profileService, apiSupport),
            new TimetableController(timetableService, apiSupport),
            new ContentController(contentService, apiSupport),
            new ReportController(reportService, apiSupport),
            new ResultController(academicResultService, apiSupport));
    }

    public ConnectionProvider connectionProvider() { return connectionProvider; }
    public AuthController authController() { return authController; }
    public ApplicationErrorHandler errorHandler() { return errorHandler; }
    public AuthorizationService authorizationService() { return authorizationService; }
    public AuditService auditService() { return auditService; }
    public CourseService courseService() { return courseService; }
    public AssessmentService assessmentService() { return assessmentService; }
    public MarksService marksService() { return marksService; }
    public AttendanceService attendanceService() { return attendanceService; }
    public MedicalService medicalService() { return medicalService; }
    public ContentService contentService() { return contentService; }
    public TimetableService timetableService() { return timetableService; }
    public GradeService gradeService() { return gradeService; }
    public GpaService gpaService() { return gpaService; }
    public EligibilityService eligibilityService() { return eligibilityService; }
    public UserService userService() { return userService; }
    public ProfileService profileService() { return profileService; }
    public ReportService reportService() { return reportService; }
    public AcademicResultService academicResultService() { return academicResultService; }
    public CourseController courseController() { return courseController; }
    public UserController userController() { return userController; }
    public AssessmentController assessmentController() { return assessmentController; }
    public MarksController marksController() { return marksController; }
    public AttendanceController attendanceController() { return attendanceController; }
    public MedicalController medicalController() { return medicalController; }
    public EligibilityController eligibilityController() { return eligibilityController; }
    public GpaController gpaController() { return gpaController; }
    public ProfileController profileController() { return profileController; }
    public TimetableController timetableController() { return timetableController; }
    public ContentController contentController() { return contentController; }
    public ReportController reportController() { return reportController; }
    public ResultController resultController() { return resultController; }
}
