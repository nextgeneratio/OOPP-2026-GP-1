package lk.ac.ruhuna.fot.ams.business.policy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.domain.EligibilityPolicy;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class FinalExamEligibilityPolicy
        implements EligibilityPolicy<FinalExamEligibilityPolicy.Input, FinalExamEligibilityPolicy.Result> {
    public record Input(BigDecimal caMark, AttendanceSummary attendance) {
        public Input {
            Objects.requireNonNull(caMark, "CA mark is required.");
            Objects.requireNonNull(attendance, "Attendance result is required.");
            if (caMark.signum() < 0 || caMark.compareTo(new BigDecimal("100")) > 0) {
                throw new ValidationException("CA mark must be between 0 and 100.");
            }
        }
    }

    public record Result(
            boolean eligible,
            boolean caEligible,
            boolean attendanceEligible,
            List<String> failedConditions) {
        public Result {
            failedConditions = List.copyOf(failedConditions);
        }
    }

    private static final BigDecimal CA_THRESHOLD = new BigDecimal("40.00");

    @Override
    public Result evaluate(Input input) {
        Objects.requireNonNull(input, "Eligibility input is required.");
        boolean caEligible = input.caMark().compareTo(CA_THRESHOLD) >= 0;
        boolean attendanceEligible = input.attendance().eligible();
        List<String> failures = new ArrayList<>();
        if (!caEligible) {
            failures.add("CA mark is below 40.00%.");
        }
        if (!attendanceEligible) {
            input.attendance().components().forEach((type, result) -> {
                if (!result.meets(input.attendance().threshold())) {
                    failures.add(type.name().toLowerCase() + " attendance below "
                            + input.attendance().threshold().toPlainString() + "%.");
                }
            });
        }
        return new Result(caEligible && attendanceEligible, caEligible, attendanceEligible, failures);
    }
}
