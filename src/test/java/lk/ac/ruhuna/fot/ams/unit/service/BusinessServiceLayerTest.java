package lk.ac.ruhuna.fot.ams.unit.service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.audit.AuditLog;
import lk.ac.ruhuna.fot.ams.audit.AuditService;
import lk.ac.ruhuna.fot.ams.business.service.AssessmentService;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.business.service.EnrollmentService;
import lk.ac.ruhuna.fot.ams.business.service.TimetableService;
import lk.ac.ruhuna.fot.ams.business.service.UserLookup;
import lk.ac.ruhuna.fot.ams.domain.enums.EnrollmentStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.enums.StudentStatus;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.Assessment;
import lk.ac.ruhuna.fot.ams.domain.model.Undergraduate;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.AuthenticationException;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Phase 3: Business & Service Layer Unit Tests (Mocking DAOs)")
class BusinessServiceLayerTest {

    @Nested
    @DisplayName("3.1 Authentication & Authorization Rules")
    class AuthAndAuthzTests {

        @Mock
        private UserLookup userLookup;
        @Mock
        private PasswordHasher passwordHasher;
        @Mock
        private AuditService auditService;

        private AutoCloseable mocks;
        private AuthService authService;
        private AuthorizationService authorizationService;

        @BeforeEach
        void setUp() {
            mocks = MockitoAnnotations.openMocks(this);
            authService = new AuthService(userLookup, passwordHasher, auditService);
            authorizationService = new AuthorizationService();
        }

        @AfterEach
        void tearDown() throws Exception {
            if (mocks != null) {
                mocks.close();
            }
        }


        @Test
        @DisplayName("Active user with valid credentials authenticates successfully and logs audit")
        void activeValidCredentialsSucceed() {
            User mockUser = new Admin(101L, "adm001", "hashed_pwd", "admin@univ.ac.lk", true);
            when(userLookup.findActiveByUsername("adm001")).thenReturn(Optional.of(mockUser));
            when(passwordHasher.verify(any(char[].class), eq("hashed_pwd"))).thenReturn(true);

            AuthenticatedSession session = authService.authenticate("adm001", "CorrectPass123!".toCharArray());

            assertThat(session).isNotNull();
            assertThat(session.userId()).isEqualTo(101L);
            assertThat(session.username()).isEqualTo("adm001");
            assertThat(session.role()).isEqualTo(Role.ADMIN);

            ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditService).log(logCaptor.capture());
            assertThat(logCaptor.getValue().action()).isEqualTo("LOGIN_SUCCESS");
            assertThat(logCaptor.getValue().userId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("Invalid password fails and records failed login audit log")
        void invalidPasswordFailsAndAudits() {
            User mockUser = new Admin(101L, "adm001", "hashed_pwd", "admin@univ.ac.lk", true);
            when(userLookup.findActiveByUsername("adm001")).thenReturn(Optional.of(mockUser));
            when(passwordHasher.verify(any(char[].class), eq("hashed_pwd"))).thenReturn(false);

            assertThatThrownBy(() -> authService.authenticate("adm001", "WrongPass!".toCharArray()))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Username or password is incorrect.");

            ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditService).log(logCaptor.capture());
            assertThat(logCaptor.getValue().action()).isEqualTo("LOGIN_FAILED");
            assertThat(logCaptor.getValue().userId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("Unknown username fails authentication and triggers failure audit")
        void unknownUsernameFailsAndAudits() {
            when(userLookup.findActiveByUsername("unknown_user")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.authenticate("unknown_user", "password".toCharArray()))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Username or password is incorrect.");

            ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditService).log(logCaptor.capture());
            assertThat(logCaptor.getValue().action()).isEqualTo("LOGIN_FAILED");
            assertThat(logCaptor.getValue().userId()).isNull();
        }

        @Test
        @DisplayName("Inactive or deactivated user is denied access")
        void inactiveUserDeniedAccess() {
            User mockInactiveUser = new Admin(102L, "inactive_adm", "hashed_pwd", "old@univ.ac.lk", false);
            when(userLookup.findActiveByUsername("inactive_adm")).thenReturn(Optional.of(mockInactiveUser));


            assertThatThrownBy(() -> authService.authenticate("inactive_adm", "AnyPass".toCharArray()))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("User account is inactive.");

            verify(passwordHasher, never()).verify(any(), any());
            ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
            verify(auditService).log(logCaptor.capture());
            assertThat(logCaptor.getValue().action()).isEqualTo("LOGIN_FAILED");
            assertThat(logCaptor.getValue().userId()).isEqualTo(102L);
        }

        @Test
        @DisplayName("Role-based authorization: Student attempting admin/lecturer operation throws AuthorizationException")
        void studentAttemptingAdminOrLecturerOperationThrowsException() {
            AuthenticatedSession studentSession = new AuthenticatedSession(200L, "stu001", Role.UNDERGRADUATE, "stu001");

            assertThatThrownBy(() -> authorizationService.requireRole(studentSession, Role.ADMIN, Role.LECTURER))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessage("You do not have permission for this action.");
        }

        @Test
        @DisplayName("Role-based authorization: Lecturer allowed for lecturer-scoped operation")
        void lecturerAllowedForLecturerAction() {
            AuthenticatedSession lecturerSession = new AuthenticatedSession(300L, "lec001", Role.LECTURER, "lec001");
            authorizationService.requireRole(lecturerSession, Role.LECTURER);
        }

        @Test
        @DisplayName("Self authorization: Accessing own record succeeds, accessing others fails")
        void selfRecordAuthorizationCheck() {
            AuthenticatedSession session = new AuthenticatedSession(500L, "stu005", Role.UNDERGRADUATE, "stu005");

            authorizationService.requireSelf(session, 500L);

            assertThatThrownBy(() -> authorizationService.requireSelf(session, 999L))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessage("You do not have permission for this action.");
        }
    }

    @Nested
    @DisplayName("3.2 Academic Rules & SRS Appendix A Scenarios (EligibilityService)")
    class SrsAppendixAScenariosTest {

        private EligibilityService eligibilityService;

        @BeforeEach
        void setUp() {
            eligibilityService = new EligibilityService();
        }

        @Test
        @DisplayName("STU001: Theory 13/15, Practical 14/15 -> Full eligibility without medical")
        void stu001FullEligibility() {
            var theory = new EligibilityService.AttendanceComponentInput(13, 15, 0);
            var practical = new EligibilityService.AttendanceComponentInput(14, 15, 0);

            var theoryResult = eligibilityService.evaluateComponent(theory);
            var practicalResult = eligibilityService.evaluateComponent(practical);

            assertThat(theoryResult.attendancePercentage()).isEqualTo(86.67);
            assertThat(theoryResult.eligible()).isTrue();

            assertThat(practicalResult.attendancePercentage()).isEqualTo(93.33);
            assertThat(practicalResult.eligible()).isTrue();

            // Course eligibility with passing CA mark (e.g. 50.00)
            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory, practical), 50.0);
            assertThat(courseResult.attendanceEligible()).isTrue();
            assertThat(courseResult.caEligible()).isTrue();
            assertThat(courseResult.finalExamEligible()).isTrue();
            assertThat(courseResult.failedReasons()).isEmpty();
        }

        @Test
        @DisplayName("STU002: Theory 12/15, Practical 12/15 -> Exactly 80% boundary eligibility")
        void stu002ExactBoundaryEligibility() {
            var theory = new EligibilityService.AttendanceComponentInput(12, 15, 0);
            var practical = new EligibilityService.AttendanceComponentInput(12, 15, 0);

            var theoryResult = eligibilityService.evaluateComponent(theory);
            var practicalResult = eligibilityService.evaluateComponent(practical);

            assertThat(theoryResult.attendancePercentage()).isEqualTo(80.00);
            assertThat(theoryResult.eligible()).isTrue();

            assertThat(practicalResult.attendancePercentage()).isEqualTo(80.00);
            assertThat(practicalResult.eligible()).isTrue();

            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory, practical), 40.0);
            assertThat(courseResult.attendanceEligible()).isTrue();
            assertThat(courseResult.caEligible()).isTrue();
            assertThat(courseResult.finalExamEligible()).isTrue();
        }

        @Test
        @DisplayName("STU003: Theory 10/15, Practical 10/15 -> Attendance failure (< 80%, no medical)")
        void stu003AttendanceFailure() {
            var theory = new EligibilityService.AttendanceComponentInput(10, 15, 0);
            var practical = new EligibilityService.AttendanceComponentInput(10, 15, 0);

            var theoryResult = eligibilityService.evaluateComponent(theory);
            var practicalResult = eligibilityService.evaluateComponent(practical);

            // 10 / 15 = 66.67%
            assertThat(theoryResult.attendancePercentage()).isEqualTo(66.67);
            assertThat(theoryResult.eligible()).isFalse();

            assertThat(practicalResult.attendancePercentage()).isEqualTo(66.67);
            assertThat(practicalResult.eligible()).isFalse();

            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory, practical), 65.0);
            assertThat(courseResult.attendanceEligible()).isFalse();
            assertThat(courseResult.finalExamEligible()).isFalse();
            assertThat(courseResult.failedReasons()).hasSize(2);
        }

        @Test
        @DisplayName("STU004: Theory 13/15 plus approved medical absence -> Attendance adjusted to satisfy requirement (> 80%)")
        void stu004AttendanceWithApprovedMedicalAdjustment() {
            // Missed 2 sessions (15 - 13 = 2). Both excused by approved medical.
            // Effective scheduled = 15 - 2 = 13. Attended = 13 -> 13/13 = 100.0%
            var theory = new EligibilityService.AttendanceComponentInput(13, 15, 2);
            var practical = new EligibilityService.AttendanceComponentInput(12, 15, 0); // 80.0%

            var theoryResult = eligibilityService.evaluateComponent(theory);
            assertThat(theoryResult.effectiveScheduledSessions()).isEqualTo(13);
            assertThat(theoryResult.attendancePercentage()).isEqualTo(100.00);
            assertThat(theoryResult.eligible()).isTrue();

            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory, practical), 55.0);
            assertThat(courseResult.attendanceEligible()).isTrue();
            assertThat(courseResult.finalExamEligible()).isTrue();
        }

        @Test
        @DisplayName("STU005: Theory 8/15, approved medical does not bring percentage to 80% -> Fails threshold")
        void stu005AttendanceWithApprovedMedicalStillFails() {
            // Missed 7 sessions (15 - 8 = 7). Excused 2 sessions by medical.
            // Effective scheduled = 15 - 2 = 13. Attended = 8 -> 8 / 13 = 61.54% < 80.00%
            var theory = new EligibilityService.AttendanceComponentInput(8, 15, 2);

            var theoryResult = eligibilityService.evaluateComponent(theory);
            assertThat(theoryResult.effectiveScheduledSessions()).isEqualTo(13);
            assertThat(theoryResult.attendancePercentage()).isEqualTo(61.54);
            assertThat(theoryResult.eligible()).isFalse();

            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory), 50.0);
            assertThat(courseResult.attendanceEligible()).isFalse();
            assertThat(courseResult.finalExamEligible()).isFalse();
            assertThat(courseResult.failedReasons()).anyMatch(reason -> reason.contains("below the 80.00% threshold"));
        }

        @Test
        @DisplayName("Final Exam Ineligibility: Passing attendance but failing CA (< 40.00) reports CA reason")
        void passingAttendanceFailingCaFailsFinalExam() {
            var theory = new EligibilityService.AttendanceComponentInput(15, 15, 0);
            var courseResult = eligibilityService.evaluateCourseEligibility(List.of(theory), 39.5);

            assertThat(courseResult.attendanceEligible()).isTrue();
            assertThat(courseResult.caEligible()).isFalse();
            assertThat(courseResult.finalExamEligible()).isFalse();
            assertThat(courseResult.failedReasons()).containsExactly(
                    "Continuous Assessment (CA) mark (39.5) is below the 40.00 hurdle."
            );
        }
    }

    @Nested
    @DisplayName("3.3 Enrolment, Timetable & Assessment Scenarios")
    class EnrolmentTimetableAssessmentTest {

        @Nested
        @DisplayName("Enrolment & Grade Capping Scenarios")
        class EnrolmentTests {
            @Mock
            private EnrollmentService.EnrollmentLookup enrollmentLookup;
            private AutoCloseable mocks;
            private EnrollmentService enrollmentService;

            @BeforeEach
            void setUp() {
                mocks = MockitoAnnotations.openMocks(this);
                enrollmentService = new EnrollmentService(enrollmentLookup);
            }

            @AfterEach
            void tearDown() throws Exception {
                if (mocks != null) {
                    mocks.close();
                }
            }


            @Test
            @DisplayName("Duplicate active enrolment is rejected per BR-003")
            void duplicateActiveEnrolmentRejected() {
                when(enrollmentLookup.existsActiveEnrollment(10, 1)).thenReturn(true);

                assertThatThrownBy(() -> enrollmentService.enrollStudent(10, 1, StudentStatus.NORMAL))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("already has an active enrollment");
            }

            @Test
            @DisplayName("First-time normal student enrolment assigns attempt_no = 1")
            void normalStudentFirstEnrolment() {
                when(enrollmentLookup.existsActiveEnrollment(10, 1)).thenReturn(false);
                when(enrollmentLookup.findByStudentAndOffering(10, 1)).thenReturn(List.of());

                var record = enrollmentService.enrollStudent(10, 1, StudentStatus.NORMAL);
                assertThat(record.attemptNo()).isEqualTo(1);
                assertThat(record.studentStatus()).isEqualTo(StudentStatus.NORMAL);
                assertThat(record.enrollmentStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
            }

            @Test
            @DisplayName("Repeat student attempt auto-increments attempt number")
            void repeatStudentAttemptIncrementsAttemptNumber() {
                when(enrollmentLookup.existsActiveEnrollment(17, 2)).thenReturn(false);
                when(enrollmentLookup.findByStudentAndOffering(17, 2)).thenReturn(List.of(
                        new EnrollmentService.EnrollmentRecord(1, 17, 2, 1, StudentStatus.NORMAL, EnrollmentStatus.COMPLETED)
                ));

                var record = enrollmentService.enrollStudent(17, 2, StudentStatus.REPEAT);
                assertThat(record.attemptNo()).isEqualTo(2);
                assertThat(record.studentStatus()).isEqualTo(StudentStatus.REPEAT);
            }

            @Test
            @DisplayName("Batch-missed student enrolment can register subsequent attempt as BATCH_MISSED")
            void batchMissedStudentSubsequentAttempt() {
                when(enrollmentLookup.existsActiveEnrollment(21, 2)).thenReturn(false);
                when(enrollmentLookup.findByStudentAndOffering(21, 2)).thenReturn(List.of(
                        new EnrollmentService.EnrollmentRecord(1, 21, 2, 1, StudentStatus.BATCH_MISSED, EnrollmentStatus.WITHDRAWN)
                ));

                var record = enrollmentService.enrollStudent(21, 2, StudentStatus.BATCH_MISSED);
                assertThat(record.attemptNo()).isEqualTo(2);
                assertThat(record.studentStatus()).isEqualTo(StudentStatus.BATCH_MISSED);
            }

            @Test
            @DisplayName("Subsequent attempt registered incorrectly as NORMAL throws BusinessRuleException")
            void subsequentAttemptAsNormalFails() {
                when(enrollmentLookup.existsActiveEnrollment(17, 2)).thenReturn(false);
                when(enrollmentLookup.findByStudentAndOffering(17, 2)).thenReturn(List.of(
                        new EnrollmentService.EnrollmentRecord(1, 17, 2, 1, StudentStatus.NORMAL, EnrollmentStatus.COMPLETED)
                ));

                assertThatThrownBy(() -> enrollmentService.enrollStudent(17, 2, StudentStatus.NORMAL))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Subsequent attempts cannot be registered as NORMAL");
            }

            @Test
            @DisplayName("Repeat enrolment grade capping: Repeat attempt grade higher than 'C' is capped at 'C'")
            void repeatEnrolmentGradeCappingAtC() {
                // If a repeat student earns an 'A' (4.00), it is capped to 'C' (2.00)
                GradeScheme earnedA = GradeScheme.A;
                GradeScheme capped = enrollmentService.capGradeForAttempt(StudentStatus.REPEAT, 2, earnedA);
                assertThat(capped).isEqualTo(GradeScheme.C);
                assertThat(capped.gradePoint()).isEqualTo(2.00);

                // If a repeat student earns a 'D' (1.00), it remains 'D'
                GradeScheme earnedD = GradeScheme.D;
                GradeScheme cappedD = enrollmentService.capGradeForAttempt(StudentStatus.REPEAT, 2, earnedD);
                assertThat(cappedD).isEqualTo(GradeScheme.D);
            }

            @Test
            @DisplayName("Batch-missed enrolment is concessionary: Grade is not capped at 'C'")
            void batchMissedGradeNotCapped() {
                GradeScheme earnedA = GradeScheme.A;
                GradeScheme result = enrollmentService.capGradeForAttempt(StudentStatus.BATCH_MISSED, 2, earnedA);
                assertThat(result).isEqualTo(GradeScheme.A);
            }
        }

        @Nested
        @DisplayName("Timetable Conflict Scenarios")
        class TimetableTests {
            @Mock
            private TimetableService.TimetableLookup timetableLookup;
            private AutoCloseable mocks;
            private TimetableService timetableService;

            @BeforeEach
            void setUp() {
                mocks = MockitoAnnotations.openMocks(this);
                timetableService = new TimetableService(timetableLookup);
            }

            @AfterEach
            void tearDown() throws Exception {
                if (mocks != null) {
                    mocks.close();
                }
            }

            @Test
            @DisplayName("Detect overlapping time slot conflict for lecturer on the same day")
            void lecturerTimeSlotConflictDetected() {
                var existingSlot = new TimetableService.TimeSlot(
                        1, 101, 5, "Lab 1",
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(11, 0)
                );
                when(timetableLookup.findSlotsByLecturer(5)).thenReturn(List.of(existingSlot));
                when(timetableLookup.findSlotsByLocation("LT 2")).thenReturn(List.of());

                // Overlapping slot: 10:00 to 12:00
                var conflictSlot = new TimetableService.TimeSlot(
                        2, 102, 5, "LT 2",
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0), LocalTime.of(12, 0)
                );

                assertThatThrownBy(() -> timetableService.validateNoConflicts(conflictSlot))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Lecturer timetable conflict");
            }

            @Test
            @DisplayName("Detect room / location conflict on the same day and time")
            void roomLocationConflictDetected() {
                var existingSlot = new TimetableService.TimeSlot(
                        1, 101, 5, "Computer Lab 02",
                        DayOfWeek.TUESDAY,
                        LocalTime.of(14, 0), LocalTime.of(16, 0)
                );
                when(timetableLookup.findSlotsByLecturer(6)).thenReturn(List.of());
                when(timetableLookup.findSlotsByLocation("Computer Lab 02")).thenReturn(List.of(existingSlot));

                // Another lecturer tries to book the same room at 15:00 - 17:00
                var conflictingRoomSlot = new TimetableService.TimeSlot(
                        2, 102, 6, "Computer Lab 02",
                        DayOfWeek.TUESDAY,
                        LocalTime.of(15, 0), LocalTime.of(17, 0)
                );

                assertThatThrownBy(() -> timetableService.validateNoConflicts(conflictingRoomSlot))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Room / location conflict");
            }

            @Test
            @DisplayName("Non-overlapping slots on the same day pass validation")
            void nonOverlappingSlotsPass() {
                var existingSlot = new TimetableService.TimeSlot(
                        1, 101, 5, "Lab 1",
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(8, 0), LocalTime.of(10, 0)
                );
                when(timetableLookup.findSlotsByLecturer(5)).thenReturn(List.of(existingSlot));
                when(timetableLookup.findSlotsByLocation("Lab 1")).thenReturn(List.of(existingSlot));

                // Next session starts at 10:00
                var nextSlot = new TimetableService.TimeSlot(
                        2, 102, 5, "Lab 1",
                        DayOfWeek.WEDNESDAY,
                        LocalTime.of(10, 0), LocalTime.of(12, 0)
                );

                timetableService.validateNoConflicts(nextSlot);
            }
        }

        @Nested
        @DisplayName("Assessment Configuration & Incomplete Assessment Scenarios")
        class AssessmentTests {
            @Mock
            private AssessmentService.AssessmentLookup assessmentLookup;
            private AutoCloseable mocks;
            private AssessmentService assessmentService;

            @BeforeEach
            void setUp() {
                mocks = MockitoAnnotations.openMocks(this);
                assessmentService = new AssessmentService(assessmentLookup);
            }

            @AfterEach
            void tearDown() throws Exception {
                if (mocks != null) {
                    mocks.close();
                }
            }


            @Test
            @DisplayName("Incomplete assessment weights (< 100%) triggers BusinessRuleException")
            void assessmentWeightsNotSummingTo100Fails() {
                when(assessmentLookup.findAssessmentsByOffering(1)).thenReturn(List.of(
                        new Assessment("Quiz 1", 20.0, true),
                        new Assessment("Final Exam", 60.0, false)
                )); // Sum is 80.0%, not 100.0%

                assertThatThrownBy(() -> assessmentService.validateAssessmentPlan(1))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Assessment weights do not sum to 100%");
            }

            @Test
            @DisplayName("Attempting final grade calculation with missing student scores triggers BusinessRuleException")
            void missingStudentAssessmentScoreFailsCalculation() {
                when(assessmentLookup.findAssessmentsByOffering(1)).thenReturn(List.of(
                        new Assessment("Quiz 1", 20.0, true),
                        new Assessment("Assignment 1", 20.0, true),
                        new Assessment("Final Exam", 60.0, false)
                ));

                // Student has only Quiz 1 and Final Exam; Assignment 1 is missing
                Map<String, Double> incompleteScores = Map.of(
                        "Quiz 1", 85.0,
                        "Final Exam", 70.0
                );

                assertThatThrownBy(() -> assessmentService.calculateStudentFinalGrade(1, incompleteScores))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("Missing mark for mandatory component 'Assignment 1'");
            }

            @Test
            @DisplayName("Complete assessment calculation computes accurate weighted grade")
            void completeAssessmentCalculatesCorrectGrade() {
                when(assessmentLookup.findAssessmentsByOffering(1)).thenReturn(List.of(
                        new Assessment("Mid", 30.0, true),
                        new Assessment("Final", 70.0, false)
                ));

                // 80 * 0.3 + 90 * 0.7 = 24 + 63 = 87 -> Grade A+
                Map<String, Double> completeScores = Map.of(
                        "Mid", 80.0,
                        "Final", 90.0
                );

                GradeScheme grade = assessmentService.calculateStudentFinalGrade(1, completeScores);
                assertThat(grade).isEqualTo(GradeScheme.A_PLUS);
            }
        }
    }
}
