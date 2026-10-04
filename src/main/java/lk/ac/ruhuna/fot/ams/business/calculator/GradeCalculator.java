package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.GradeScheme.GradeBand;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class GradeCalculator {
    public record GradeResult(String schemeVersion, String grade, BigDecimal gradePoint) {
    }

    private final GradeScheme scheme;

    public GradeCalculator(GradeScheme scheme) {
        this.scheme = Objects.requireNonNull(scheme, "Grade scheme is required.");
    }

    public GradeResult grade(BigDecimal mark) {
        if (mark == null || mark.signum() < 0 || mark.compareTo(new BigDecimal("100")) > 0) {
            throw new ValidationException("Final mark must be between 0 and 100.");
        }
        GradeBand band = scheme.bands().stream()
                .filter(candidate -> candidate.contains(mark))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Mark is not covered by the configured grade scheme."));
        return new GradeResult(scheme.version(), band.grade(), band.gradePoint());
    }
}
