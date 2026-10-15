package lk.ac.ruhuna.fot.ams.api.request;

import lk.ac.ruhuna.fot.ams.domain.enums.StudentStatus;

public record EnrollStudentRequest(
        Integer studentId,
        Integer offeringId,
        StudentStatus studentStatus
) {}
