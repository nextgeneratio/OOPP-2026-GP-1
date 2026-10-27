package lk.ac.ruhuna.fot.ams.security.authorization;

import java.util.OptionalInt;

public interface AuthorizationScopeLookup {
    boolean lecturerAssignedToOffering(long userId, int offeringId);

    boolean lecturerAssignedToStudent(long userId, int studentId);

    boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId);

    boolean technicalOfficerAuthorizedForStudent(long userId, int studentId);

    default boolean technicalOfficerAuthorizedForTimetable(long userId, int departmentId, int semesterId) {
        return false;
    }

    boolean undergraduateOwnsStudent(long userId, int studentId);

    boolean undergraduateEnrolledInOffering(long userId, int offeringId);

    OptionalInt lecturerIdForUser(long userId);

    OptionalInt technicalOfficerIdForUser(long userId);

    OptionalInt studentIdForUser(long userId);

    OptionalInt studentBatchIdForUser(long userId);
}
