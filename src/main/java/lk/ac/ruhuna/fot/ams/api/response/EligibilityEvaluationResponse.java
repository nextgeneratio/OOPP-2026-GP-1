package lk.ac.ruhuna.fot.ams.api.response;

import java.util.List;

public record EligibilityEvaluationResponse(
        boolean attendanceEligible,
        boolean caEligible,
        boolean finalExamEligible,
        double overallAttendancePercentage,
        double caMark,
        List<String> failedReasons
) {}
