package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class AttendanceCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("80.00");

    public record ComponentAttendance(int scheduledSessions, int attendedSessions, int medicallyExcusedSessions) {
        public ComponentAttendance {
            if (scheduledSessions < 0 || attendedSessions < 0 || medicallyExcusedSessions < 0
                    || attendedSessions + medicallyExcusedSessions > scheduledSessions) {
                throw new ValidationException("Attendance session counts are inconsistent.");
            }
        }
    }

    public record ComponentResult(int attendedSessions, int eligibleScheduledSessions, BigDecimal percentage) {
        public boolean meets(BigDecimal threshold) {
            return percentage.compareTo(threshold) >= 0;
        }
    }

    public record AttendanceSummary(
            Map<ComponentType, ComponentResult> components,
            ComponentResult combined,
            BigDecimal threshold) {
        public AttendanceSummary {
            EnumMap<ComponentType, ComponentResult> copy = new EnumMap<>(ComponentType.class);
            copy.putAll(components);
            components = Collections.unmodifiableMap(copy);
            Objects.requireNonNull(combined, "Combined attendance result is required.");
            Objects.requireNonNull(threshold, "Attendance threshold is required.");
        }

        public boolean eligible() {
            return !components.isEmpty() && components.values().stream().allMatch(result -> result.meets(threshold));
        }
    }

    public AttendanceSummary calculate(Map<ComponentType, ComponentAttendance> attendance) {
        return calculate(attendance, DEFAULT_THRESHOLD);
    }

    public AttendanceSummary calculate(
            Map<ComponentType, ComponentAttendance> attendance,
            BigDecimal threshold) {
        if (attendance == null || attendance.isEmpty()) {
            throw new ValidationException("Attendance is required for at least one course component.");
        }
        if (threshold == null || threshold.signum() < 0 || threshold.compareTo(ONE_HUNDRED) > 0) {
            throw new ValidationException("Attendance threshold must be between 0 and 100.");
        }

        EnumMap<ComponentType, ComponentResult> results = new EnumMap<>(ComponentType.class);
        int attended = 0;
        int eligibleScheduled = 0;
        for (Map.Entry<ComponentType, ComponentAttendance> entry : attendance.entrySet()) {
            ComponentType type = Objects.requireNonNull(entry.getKey(), "Component type is required.");
            ComponentAttendance value = Objects.requireNonNull(entry.getValue(), "Attendance values are required.");
            int eligibleCount = value.scheduledSessions() - value.medicallyExcusedSessions();
            results.put(type, result(value.attendedSessions(), eligibleCount));
            attended += value.attendedSessions();
            eligibleScheduled += eligibleCount;
        }
        return new AttendanceSummary(results, result(attended, eligibleScheduled), threshold);
    }

    private ComponentResult result(int attended, int eligibleScheduled) {
        BigDecimal percentage = eligibleScheduled == 0
                ? ONE_HUNDRED.setScale(2)
                : BigDecimal.valueOf(attended)
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(eligibleScheduled), 2, RoundingMode.HALF_UP);
        return new ComponentResult(attended, eligibleScheduled, percentage);
    }
}
