package lk.ac.ruhuna.fot.ams.business.service;

import java.util.List;
import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class EligibilityService {

    public record AttendanceComponentInput(
            int attendedSessions,
            int scheduledSessions,
            int approvedMedicalSessions
    ) {}

    public record ComponentEligibilityResult(
            int attendedSessions,
            int scheduledSessions,
            int approvedMedicalSessions,
            int effectiveScheduledSessions,
            double attendancePercentage,
            boolean eligible
    ) {}

    public record CourseEligibilityResult(
            boolean attendanceEligible,
            boolean caEligible,
            boolean finalExamEligible,
            double overallAttendancePercentage,
            double caMark,
            List<String> failedReasons
    ) {}

    /**
     * Evaluates component attendance based on SRS BR-006, BR-007, BR-008, BR-009:
     * Approved medical sessions are removed from the denominator (BR-009).
     * Effective scheduled sessions = scheduledSessions - approvedMedicalSessions.
     * Attendance percentage = (attendedSessions / effectiveScheduledSessions) * 100.
     * Must be >= 80.00% to be eligible (BR-008).
     */
    public ComponentEligibilityResult evaluateComponent(AttendanceComponentInput input) {
        if (input == null) {
            throw new ValidationException("Attendance input cannot be null.");
        }
        int attended = input.attendedSessions();
        int scheduled = input.scheduledSessions();
        int medical = input.approvedMedicalSessions();

        if (scheduled < 0 || attended < 0 || medical < 0) {
            throw new ValidationException("Session counts cannot be negative.");
        }
        if (attended > scheduled) {
            throw new ValidationException("Attended sessions cannot exceed scheduled sessions.");
        }
        if (medical > (scheduled - attended)) {
            // Cannot excuse more sessions than were missed
            throw new ValidationException("Approved medical sessions cannot exceed missed sessions.");
        }

        int effectiveScheduled = scheduled - medical;
        double percentage;
        if (effectiveScheduled <= 0) {
            percentage = attended > 0 ? 100.00 : 0.00;
        } else {
            percentage = AcademicCalculator.calculateAttendancePercentage(attended, effectiveScheduled);
        }

        boolean eligible = AcademicCalculator.isAttendanceEligible(percentage);
        return new ComponentEligibilityResult(
                attended,
                scheduled,
                medical,
                effectiveScheduled,
                percentage,
                eligible
        );
    }

    /**
     * Evaluates final exam eligibility (BR-010):
     * Requires attendance eligibility in all required components (or combined if applicable) and CA >= 40.00%.
     */
    public CourseEligibilityResult evaluateCourseEligibility(
            List<AttendanceComponentInput> components,
            double caMark
    ) {
        if (components == null || components.isEmpty()) {
            throw new ValidationException("Course components attendance cannot be empty.");
        }

        boolean allComponentsEligible = true;
        int totalAttended = 0;
        int totalEffectiveScheduled = 0;
        List<String> failedReasons = new java.util.ArrayList<>();

        for (int i = 0; i < components.size(); i++) {
            ComponentEligibilityResult compResult = evaluateComponent(components.get(i));
            totalAttended += compResult.attendedSessions();
            totalEffectiveScheduled += compResult.effectiveScheduledSessions();
            if (!compResult.eligible()) {
                allComponentsEligible = false;
                failedReasons.add("Component " + (i + 1) + " attendance (" + compResult.attendancePercentage() + "%) is below the 80.00% threshold.");
            }
        }

        double combinedPercentage = totalEffectiveScheduled > 0
                ? AcademicCalculator.calculateAttendancePercentage(totalAttended, totalEffectiveScheduled)
                : 0.00;

        boolean caEligible = AcademicCalculator.isCaEligible(caMark);
        if (!caEligible) {
            failedReasons.add("Continuous Assessment (CA) mark (" + caMark + ") is below the 40.00 hurdle.");
        }

        boolean finalEligible = allComponentsEligible && caEligible;

        return new CourseEligibilityResult(
                allComponentsEligible,
                caEligible,
                finalEligible,
                combinedPercentage,
                caMark,
                List.copyOf(failedReasons)
        );
    }
}
