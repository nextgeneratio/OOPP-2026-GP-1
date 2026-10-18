package lk.ac.ruhuna.fot.ams.security.authorization;

import java.util.Arrays;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AuthorizationService {
    public enum ReportType {
        STUDENT_ATTENDANCE,
        BATCH_COURSE_ATTENDANCE,
        CA_FINAL_MARKS,
        ELIGIBILITY,
        GRADES_COURSE_RESULTS,
        SGPA_CGPA,
        MEDICAL_RECORDS,
        STUDENT_PROFILE
    }

    private final AuthorizationScopeLookup scopeLookup;

    public AuthorizationService() {
        this(null);
    }

    public AuthorizationService(AuthorizationScopeLookup scopeLookup) {
        this.scopeLookup = scopeLookup;
    }

    public void requireRole(AuthenticatedSession session, Role... allowedRoles) {
        if (session == null || Arrays.stream(allowedRoles).noneMatch(session.role()::equals)) {
            throw new AuthorizationException("You do not have permission for this action.");
        }
    }

    public void requireSelf(AuthenticatedSession session, long userId) {
        if (session == null || session.userId() != userId) {
            throw new AuthorizationException("You do not have permission for this action.");
        }
    }

    public void requireAdmin(AuthenticatedSession session) {
        requireRole(session, Role.ADMIN);
    }

    public void requireLecturerForOffering(AuthenticatedSession session, int offeringId) {
        requireRole(session, Role.LECTURER);
        if (offeringId <= 0 || !lookup().lecturerAssignedToOffering(session.userId(), offeringId)) {
            throw new AuthorizationException("You do not have access to this course offering.");
        }
    }

    public void requireLecturerForStudent(AuthenticatedSession session, int studentId) {
        requireRole(session, Role.LECTURER);
        if (studentId <= 0 || !lookup().lecturerAssignedToStudent(session.userId(), studentId)) {
            throw new AuthorizationException("You do not have access to this student's academic records.");
        }
    }

    public void requireTechnicalOfficerForOffering(AuthenticatedSession session, int offeringId) {
        requireRole(session, Role.TECHNICAL_OFFICER);
        if (offeringId <= 0 || !lookup().technicalOfficerAuthorizedForOffering(session.userId(), offeringId)) {
            throw new AuthorizationException("You do not have access to this department or current semester.");
        }
    }

    public void requireTechnicalOfficerForStudent(AuthenticatedSession session, int studentId) {
        requireRole(session, Role.TECHNICAL_OFFICER);
        if (studentId <= 0 || !lookup().technicalOfficerAuthorizedForStudent(session.userId(), studentId)) {
            throw new AuthorizationException("You do not have access to this student or current semester.");
        }
    }

    public void requireUndergraduateForStudent(AuthenticatedSession session, int studentId) {
        requireRole(session, Role.UNDERGRADUATE);
        if (studentId <= 0 || !lookup().undergraduateOwnsStudent(session.userId(), studentId)) {
            throw new AuthorizationException("You may only access your own student records.");
        }
    }

    public void requireUndergraduateForOffering(AuthenticatedSession session, int offeringId) {
        requireRole(session, Role.UNDERGRADUATE);
        if (offeringId <= 0 || !lookup().undergraduateEnrolledInOffering(session.userId(), offeringId)) {
            throw new AuthorizationException("You may only access materials for your enrolled courses.");
        }
    }

    public int lecturerId(AuthenticatedSession session) {
        requireRole(session, Role.LECTURER);
        return roleProfileId(lookup().lecturerIdForUser(session.userId()), "Lecturer profile is unavailable.");
    }

    public int technicalOfficerId(AuthenticatedSession session) {
        requireRole(session, Role.TECHNICAL_OFFICER);
        return roleProfileId(
                lookup().technicalOfficerIdForUser(session.userId()),
                "Technical Officer profile is unavailable.");
    }

    public int studentId(AuthenticatedSession session) {
        requireRole(session, Role.UNDERGRADUATE);
        return roleProfileId(lookup().studentIdForUser(session.userId()), "Student profile is unavailable.");
    }

    public int studentBatchId(AuthenticatedSession session) {
        requireRole(session, Role.UNDERGRADUATE);
        return roleProfileId(lookup().studentBatchIdForUser(session.userId()), "Student batch is unavailable.");
    }

    public void requireReportAccess(AuthenticatedSession session, ReportType reportType) {
        if (reportType == null) {
            throw new AuthorizationException("You do not have permission for this report.");
        }
        Role role = session == null ? null : session.role();
        boolean allowed = switch (reportType) {
            case STUDENT_ATTENDANCE -> role == Role.LECTURER
                    || role == Role.TECHNICAL_OFFICER || role == Role.UNDERGRADUATE;
            case BATCH_COURSE_ATTENDANCE -> role == Role.LECTURER || role == Role.TECHNICAL_OFFICER;
            case CA_FINAL_MARKS, ELIGIBILITY, GRADES_COURSE_RESULTS, SGPA_CGPA ->
                    role == Role.LECTURER || role == Role.UNDERGRADUATE;
            case MEDICAL_RECORDS -> role == Role.LECTURER
                    || role == Role.TECHNICAL_OFFICER || role == Role.UNDERGRADUATE;
            case STUDENT_PROFILE -> role == Role.ADMIN || role == Role.LECTURER || role == Role.UNDERGRADUATE;
        };
        if (!allowed) {
            throw new AuthorizationException("You do not have permission for this report.");
        }
    }

    private AuthorizationScopeLookup lookup() {
        if (scopeLookup == null) {
            throw new IllegalStateException("Authorization scope lookup has not been configured.");
        }
        return scopeLookup;
    }

    private int roleProfileId(OptionalInt profileId, String message) {
        if (profileId.isEmpty()) {
            throw new AuthorizationException(message);
        }
        return profileId.getAsInt();
    }
}
