package lk.ac.ruhuna.fot.ams.api.response;

import lk.ac.ruhuna.fot.ams.domain.enums.EnrollmentStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.StudentStatus;

public record EnrollStudentResponse(
        int studentId,
        int offeringId,
        int attemptNo,
        StudentStatus studentStatus,
        EnrollmentStatus enrollmentStatus
) {}
