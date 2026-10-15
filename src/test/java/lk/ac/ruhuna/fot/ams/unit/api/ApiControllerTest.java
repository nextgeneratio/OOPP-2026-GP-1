package lk.ac.ruhuna.fot.ams.unit.api;

import java.util.List;
import java.util.Map;
import lk.ac.ruhuna.fot.ams.api.controller.AssessmentController;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController;
import lk.ac.ruhuna.fot.ams.api.controller.EnrollmentController;
import lk.ac.ruhuna.fot.ams.api.request.CalculateGradeRequest;
import lk.ac.ruhuna.fot.ams.api.request.EligibilityEvaluationRequest;
import lk.ac.ruhuna.fot.ams.api.request.EnrollStudentRequest;
import lk.ac.ruhuna.fot.ams.api.request.LoginRequest;
import lk.ac.ruhuna.fot.ams.api.response.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.response.CalculateGradeResponse;
import lk.ac.ruhuna.fot.ams.api.response.EligibilityEvaluationResponse;
import lk.ac.ruhuna.fot.ams.api.response.EnrollStudentResponse;
import lk.ac.ruhuna.fot.ams.api.response.LoginResponse;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.business.service.EnrollmentService;
import lk.ac.ruhuna.fot.ams.domain.enums.EnrollmentStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.enums.StudentStatus;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Phase 4: API & Controller Tests")
class ApiControllerTest {

    @Nested
    @DisplayName("4.1 AuthController Tests")
    class AuthControllerTests {
        @Mock
        private AuthService authService;
        private AutoCloseable mocks;
        private AuthController authController;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);
            authController = new AuthController(authService, new ApplicationErrorHandler());
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        @DisplayName("Null payload is rejected before calling service")
        void nullRequestRejected() {
            ApiResponse<LoginResponse> response = authController.handleLogin(null);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Request payload cannot be null.");
            verify(authService, never()).authenticate(any(), any());
        }

        @Test
        @DisplayName("Blank username is rejected with validation message")
        void blankUsernameRejected() {
            LoginRequest req = new LoginRequest("   ", "pass123".toCharArray());
            ApiResponse<LoginResponse> response = authController.handleLogin(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Username is required.");
            verify(authService, never()).authenticate(any(), any());
        }

        @Test
        @DisplayName("Empty password is rejected with validation message")
        void emptyPasswordRejected() {
            LoginRequest req = new LoginRequest("user1", new char[0]);
            ApiResponse<LoginResponse> response = authController.handleLogin(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Password is required.");
            verify(authService, never()).authenticate(any(), any());
        }

        @Test
        @DisplayName("Valid login request calls service and returns mapped response")
        void validLoginReturnsMappedResponse() {
            LoginRequest req = new LoginRequest("adm001", "SecretPass123!".toCharArray());
            AuthenticatedSession session = new AuthenticatedSession(1L, "adm001", Role.ADMIN, "Administrator");
            when(authService.authenticate(any(), any())).thenReturn(session);

            ApiResponse<LoginResponse> response = authController.handleLogin(req);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().userId()).isEqualTo(1L);
            assertThat(response.data().username()).isEqualTo("adm001");
            assertThat(response.data().role()).isEqualTo(Role.ADMIN);
            assertThat(response.data().displayName()).isEqualTo("Administrator");
        }

        @Test
        @DisplayName("Service authentication exception is mapped to safe user message without leaking internals")
        void authExceptionMappedSafely() {
            LoginRequest req = new LoginRequest("adm001", "Wrong!".toCharArray());
            when(authService.authenticate(any(), any()))
                    .thenThrow(new AuthenticationException("Username or password is incorrect."));

            ApiResponse<LoginResponse> response = authController.handleLogin(req);

            assertThat(response.success()).isFalse();
            assertThat(response.data()).isNull();
            assertThat(response.message()).isEqualTo("Username or password is incorrect.");
        }
    }

    @Nested
    @DisplayName("4.2 EligibilityController Tests")
    class EligibilityControllerTests {
        @Mock
        private EligibilityService eligibilityService;
        private AutoCloseable mocks;
        private EligibilityController eligibilityController;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);
            eligibilityController = new EligibilityController(eligibilityService, new ApplicationErrorHandler());
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        @DisplayName("Null or empty components rejected before invoking service")
        void emptyComponentsRejected() {
            var req = new EligibilityEvaluationRequest(List.of(), 50.0);
            ApiResponse<EligibilityEvaluationResponse> response = eligibilityController.evaluateEligibility(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("At least one course component attendance must be provided.");
            verify(eligibilityService, never()).evaluateCourseEligibility(anyList(), anyDouble());
        }

        @Test
        @DisplayName("Null CA mark is rejected before invoking service")
        void nullCaMarkRejected() {
            var comp = new EligibilityEvaluationRequest.ComponentAttendanceRequest(12, 15, 0);
            var req = new EligibilityEvaluationRequest(List.of(comp), null);
            ApiResponse<EligibilityEvaluationResponse> response = eligibilityController.evaluateEligibility(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("CA mark is required.");
            verify(eligibilityService, never()).evaluateCourseEligibility(anyList(), anyDouble());
        }

        @Test
        @DisplayName("Valid eligibility request maps correctly to response model")
        void validEligibilityRequestMapped() {
            var comp = new EligibilityEvaluationRequest.ComponentAttendanceRequest(13, 15, 0);
            var req = new EligibilityEvaluationRequest(List.of(comp), 45.0);

            var serviceResult = new EligibilityService.CourseEligibilityResult(
                    true, true, true, 86.67, 45.0, List.of()
            );
            when(eligibilityService.evaluateCourseEligibility(anyList(), anyDouble())).thenReturn(serviceResult);

            ApiResponse<EligibilityEvaluationResponse> response = eligibilityController.evaluateEligibility(req);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().attendanceEligible()).isTrue();
            assertThat(response.data().caEligible()).isTrue();
            assertThat(response.data().finalExamEligible()).isTrue();
            assertThat(response.data().overallAttendancePercentage()).isEqualTo(86.67);
            assertThat(response.data().failedReasons()).isEmpty();
        }

        @Test
        @DisplayName("Service validation exception mapped safely")
        void validationExceptionMappedSafely() {
            var comp = new EligibilityEvaluationRequest.ComponentAttendanceRequest(13, 15, 0);
            var req = new EligibilityEvaluationRequest(List.of(comp), 45.0);

            when(eligibilityService.evaluateCourseEligibility(anyList(), anyDouble()))
                    .thenThrow(new ValidationException("Attended sessions cannot exceed scheduled sessions."));

            ApiResponse<EligibilityEvaluationResponse> response = eligibilityController.evaluateEligibility(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Attended sessions cannot exceed scheduled sessions.");
        }
    }

    @Nested
    @DisplayName("4.3 EnrollmentController Tests")
    class EnrollmentControllerTests {
        @Mock
        private EnrollmentService enrollmentService;
        private AutoCloseable mocks;
        private EnrollmentController enrollmentController;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);
            enrollmentController = new EnrollmentController(enrollmentService, new ApplicationErrorHandler());
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        @DisplayName("Invalid IDs rejected without invoking service")
        void invalidStudentOrOfferingIdRejected() {
            var req = new EnrollStudentRequest(0, 1, StudentStatus.NORMAL);
            ApiResponse<EnrollStudentResponse> response = enrollmentController.enroll(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Valid student ID is required.");
            verify(enrollmentService, never()).enrollStudent(anyInt(), anyInt(), any());
        }

        @Test
        @DisplayName("Valid enrollment invocation maps correctly to response")
        void validEnrollmentMapped() {
            var req = new EnrollStudentRequest(10, 2, StudentStatus.NORMAL);
            var serviceResult = new EnrollmentService.EnrollmentRecord(
                    1, 10, 2, 1, StudentStatus.NORMAL, EnrollmentStatus.ACTIVE
            );
            when(enrollmentService.enrollStudent(10, 2, StudentStatus.NORMAL)).thenReturn(serviceResult);

            ApiResponse<EnrollStudentResponse> response = enrollmentController.enroll(req);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().studentId()).isEqualTo(10);
            assertThat(response.data().offeringId()).isEqualTo(2);
            assertThat(response.data().attemptNo()).isEqualTo(1);
            assertThat(response.data().studentStatus()).isEqualTo(StudentStatus.NORMAL);
            assertThat(response.data().enrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        }

        @Test
        @DisplayName("Business rule exception mapped to clean error message")
        void businessRuleExceptionMappedSafely() {
            var req = new EnrollStudentRequest(10, 2, StudentStatus.NORMAL);
            when(enrollmentService.enrollStudent(10, 2, StudentStatus.NORMAL))
                    .thenThrow(new BusinessRuleException("Student already has an active enrollment in this course offering."));

            ApiResponse<EnrollStudentResponse> response = enrollmentController.enroll(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("The operation could not be completed.");
        }
    }

    @Nested
    @DisplayName("4.4 AssessmentController Tests")
    class AssessmentControllerTests {
        @Mock
        private AssessmentService assessmentService;
        private AutoCloseable mocks;
        private AssessmentController assessmentController;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);
            assessmentController = new AssessmentController(assessmentService, new ApplicationErrorHandler());
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }

        @Test
        @DisplayName("Empty student scores rejected before service invocation")
        void emptyScoresRejected() {
            var req = new CalculateGradeRequest(1, Map.of());
            ApiResponse<CalculateGradeResponse> response = assessmentController.calculateGrade(req);

            assertThat(response.success()).isFalse();
            assertThat(response.message()).isEqualTo("Student assessment scores map cannot be empty.");
            verify(assessmentService, never()).calculateStudentFinalGrade(anyInt(), anyMap());
        }

        @Test
        @DisplayName("Valid grade calculation mapped accurately to response")
        void validGradeCalculationMapped() {
            var scores = Map.of("Mid", 75.0, "Final", 85.0);
            var req = new CalculateGradeRequest(1, scores);
            when(assessmentService.calculateStudentFinalGrade(1, scores)).thenReturn(GradeScheme.A);

            ApiResponse<CalculateGradeResponse> response = assessmentController.calculateGrade(req);

            assertThat(response.success()).isTrue();
            assertThat(response.data()).isNotNull();
            assertThat(response.data().offeringId()).isEqualTo(1);
            assertThat(response.data().gradeLetter()).isEqualTo("A");
            assertThat(response.data().gradePoint()).isEqualTo(4.00);
        }

        @Test
        @DisplayName("Controllers contain no direct database queries or hidden business calculations")
        void controllersPureDispatchers() {
            // AssessmentController simply delegates score map & offering to AssessmentService
            var scores = Map.of("Mid", 75.0, "Final", 85.0);
            assessmentController.calculateGrade(new CalculateGradeRequest(1, scores));
            verify(assessmentService).calculateStudentFinalGrade(1, scores);
        }
    }
}
