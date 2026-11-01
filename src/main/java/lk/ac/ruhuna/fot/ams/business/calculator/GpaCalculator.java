package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class GpaCalculator {
    public enum RepeatSelectionRule {
        LATEST_COMPLETED_ATTEMPT,
        ALL_COMPLETED_ATTEMPTS
    }

    public record CourseResult(
            String courseCode,
            BigDecimal credits,
            BigDecimal gradePoint,
            boolean completed,
            int attemptNumber,
            LocalDate completedOn) {
        public CourseResult {
            if (courseCode == null || courseCode.isBlank()
                    || credits == null || credits.signum() <= 0
                    || gradePoint == null || gradePoint.signum() < 0
                    || gradePoint.compareTo(new BigDecimal("4.00")) > 0
                    || attemptNumber < 1
                    || (completed && completedOn == null)) {
                throw new ValidationException("Invalid course result for GPA calculation.");
            }
        }
    }

    private static final int DISPLAY_SCALE = 2;

    public BigDecimal calculateSgpa(Collection<CourseResult> semesterResults) {
        return weightedGpa(completedResults(semesterResults));
    }

    public BigDecimal calculateCgpa(
            Collection<CourseResult> allResults,
            RepeatSelectionRule repeatSelectionRule) {
        Objects.requireNonNull(repeatSelectionRule, "Repeat selection rule is required.");
        List<CourseResult> completed = completedResults(allResults);
        Collection<CourseResult> counted = repeatSelectionRule == RepeatSelectionRule.ALL_COMPLETED_ATTEMPTS
                ? completed
                : latestAttemptPerCourse(completed);
        return weightedGpa(counted);
    }

    private List<CourseResult> completedResults(Collection<CourseResult> results) {
        if (results == null) {
            throw new ValidationException("Course results are required.");
        }
        return results.stream().filter(CourseResult::completed).toList();
    }

    private Collection<CourseResult> latestAttemptPerCourse(List<CourseResult> results) {
        Map<String, CourseResult> latest = new LinkedHashMap<>();
        results.stream()
                .sorted(Comparator.comparing(CourseResult::completedOn)
                        .thenComparingInt(CourseResult::attemptNumber))
                .forEach(result -> latest.put(result.courseCode(), result));
        return latest.values();
    }

    private BigDecimal weightedGpa(Collection<CourseResult> results) {
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal weightedPoints = BigDecimal.ZERO;
        for (CourseResult result : results) {
            totalCredits = totalCredits.add(result.credits());
            weightedPoints = weightedPoints.add(result.credits().multiply(result.gradePoint()));
        }
        if (totalCredits.signum() == 0) {
            throw new ValidationException("GPA cannot be calculated without completed course credits.");
        }
        return weightedPoints.divide(totalCredits, DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
