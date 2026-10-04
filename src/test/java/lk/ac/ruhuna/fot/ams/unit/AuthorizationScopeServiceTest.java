package lk.ac.ruhuna.fot.ams.unit;

import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationScopeServiceTest {
    private final ScopeLookup scopes = new ScopeLookup();
    private final AuthorizationService authorization = new AuthorizationService(scopes);

    @Test
    void enforcesLecturerAssignmentAndTechnicalOfficerCurrentOfferingScope() {
        AuthenticatedSession lecturer = session(Role.LECTURER);
        AuthenticatedSession officer = session(Role.TECHNICAL_OFFICER);

        authorization.requireLecturerForOffering(lecturer, 10);
        authorization.requireTechnicalOfficerForOffering(officer, 10);

        scopes.lecturerAssigned = false;
        scopes.technicalOfficerAuthorized = false;
        assertThatThrownBy(() -> authorization.requireLecturerForOffering(lecturer, 10))
                .isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> authorization.requireTechnicalOfficerForOffering(officer, 10))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void enforcesStudentOwnershipAndCourseEnrollment() {
        AuthenticatedSession student = session(Role.UNDERGRADUATE);

        authorization.requireUndergraduateForStudent(student, 7);
        authorization.requireUndergraduateForOffering(student, 10);

        scopes.studentOwner = false;
        scopes.studentEnrolled = false;
        assertThatThrownBy(() -> authorization.requireUndergraduateForStudent(student, 7))
                .isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> authorization.requireUndergraduateForOffering(student, 10))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void restrictsEachReportByTheSrsRoleMatrix() {
        assertReportAllowed(ReportType.STUDENT_ATTENDANCE,
                Role.LECTURER, Role.TECHNICAL_OFFICER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.BATCH_COURSE_ATTENDANCE,
                Role.LECTURER, Role.TECHNICAL_OFFICER);
        assertReportAllowed(ReportType.CA_FINAL_MARKS, Role.LECTURER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.ELIGIBILITY, Role.LECTURER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.GRADES_COURSE_RESULTS, Role.LECTURER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.SGPA_CGPA, Role.LECTURER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.MEDICAL_RECORDS,
                Role.LECTURER, Role.TECHNICAL_OFFICER, Role.UNDERGRADUATE);
        assertReportAllowed(ReportType.STUDENT_PROFILE,
                Role.ADMIN, Role.LECTURER, Role.UNDERGRADUATE);
    }

    @Test
    void resolvesRoleSpecificProfileIdentifiers() {
        assertThat(authorization.lecturerId(session(Role.LECTURER))).isEqualTo(3);
        assertThat(authorization.technicalOfficerId(session(Role.TECHNICAL_OFFICER))).isEqualTo(4);
        assertThat(authorization.studentId(session(Role.UNDERGRADUATE))).isEqualTo(7);
        assertThat(authorization.studentBatchId(session(Role.UNDERGRADUATE))).isEqualTo(2);
    }

    private void assertReportAllowed(ReportType type, Role... allowed) {
        for (Role role : Role.values()) {
            if (java.util.Arrays.asList(allowed).contains(role)) {
                authorization.requireReportAccess(session(role), type);
            } else {
                assertThatThrownBy(() -> authorization.requireReportAccess(session(role), type))
                        .isInstanceOf(AuthorizationException.class);
            }
        }
    }

    private AuthenticatedSession session(Role role) {
        return new AuthenticatedSession(1, "user", role, "Test User");
    }

    private static final class ScopeLookup implements AuthorizationScopeLookup {
        private boolean lecturerAssigned = true;
        private boolean technicalOfficerAuthorized = true;
        private boolean studentOwner = true;
        private boolean studentEnrolled = true;

        @Override
        public boolean lecturerAssignedToOffering(long userId, int offeringId) {
            return lecturerAssigned;
        }

        @Override
        public boolean lecturerAssignedToStudent(long userId, int studentId) {
            return lecturerAssigned;
        }

        @Override
        public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) {
            return technicalOfficerAuthorized;
        }

        @Override
        public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) {
            return technicalOfficerAuthorized;
        }

        @Override
        public boolean undergraduateOwnsStudent(long userId, int studentId) {
            return studentOwner;
        }

        @Override
        public boolean undergraduateEnrolledInOffering(long userId, int offeringId) {
            return studentEnrolled;
        }

        @Override
        public OptionalInt lecturerIdForUser(long userId) {
            return OptionalInt.of(3);
        }

        @Override
        public OptionalInt technicalOfficerIdForUser(long userId) {
            return OptionalInt.of(4);
        }

        @Override
        public OptionalInt studentIdForUser(long userId) {
            return OptionalInt.of(7);
        }

        @Override
        public OptionalInt studentBatchIdForUser(long userId) {
            return OptionalInt.of(2);
        }
    }
}
