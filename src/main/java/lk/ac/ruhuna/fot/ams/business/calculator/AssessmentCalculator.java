package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class AssessmentCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int CALCULATION_SCALE = 8;

    public record WeightedMark(BigDecimal mark, BigDecimal weight, boolean continuousAssessment) {
        public WeightedMark {
            requireRange(mark, "Mark");
            requireRange(weight, "Weight");
        }
    }

    public void validatePublishedPlan(Collection<WeightedMark> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            throw new ValidationException("A published assessment plan must contain assessments.");
        }
        BigDecimal totalWeight = assessments.stream()
                .map(WeightedMark::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(ONE_HUNDRED) != 0) {
            throw new ValidationException("Published assessment weights must total 100%.");
        }
    }

    public BigDecimal calculateFinalMark(Collection<WeightedMark> assessments) {
        validatePublishedPlan(assessments);
        return weightedAverage(assessments, 2);
    }

    public BigDecimal calculateCaMark(Collection<WeightedMark> assessments) {
        if (assessments == null) {
            throw new ValidationException("Assessment marks are required.");
        }
        List<WeightedMark> caAssessments = assessments.stream()
                .filter(WeightedMark::continuousAssessment)
                .toList();
        if (caAssessments.isEmpty()) {
            throw new ValidationException("At least one CA assessment is required.");
        }
        BigDecimal totalWeight = caAssessments.stream()
                .map(WeightedMark::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() == 0) {
            throw new ValidationException("CA assessment weights must total more than zero.");
        }
        return weightedAverage(caAssessments, CALCULATION_SCALE);
    }

    private BigDecimal weightedAverage(Collection<WeightedMark> assessments, int scale) {
        BigDecimal totalWeight = assessments.stream()
                .map(WeightedMark::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.signum() == 0) {
            throw new ValidationException("Assessment weights must total more than zero.");
        }
        BigDecimal weightedMarks = assessments.stream()
                .map(item -> item.mark().multiply(item.weight()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return weightedMarks.divide(totalWeight, scale, RoundingMode.HALF_UP);
    }

    private static void requireRange(BigDecimal value, String label) {
        Objects.requireNonNull(value, label + " is required.");
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(ONE_HUNDRED) > 0) {
            throw new ValidationException(label + " must be between 0 and 100.");
        }
    }
}
