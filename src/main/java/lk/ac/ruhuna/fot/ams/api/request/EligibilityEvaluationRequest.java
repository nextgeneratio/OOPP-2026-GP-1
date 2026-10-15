package lk.ac.ruhuna.fot.ams.api.request;

import java.util.List;

public record EligibilityEvaluationRequest(
        List<ComponentAttendanceRequest> components,
        Double caMark
) {
    public record ComponentAttendanceRequest(
            Integer attendedSessions,
            Integer scheduledSessions,
            Integer approvedMedicalSessions
    ) {}
}
