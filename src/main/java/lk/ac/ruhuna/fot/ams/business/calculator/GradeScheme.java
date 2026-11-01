package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public record GradeScheme(String version, List<GradeBand> bands) {
    public record GradeBand(
            BigDecimal minimumMark,
            BigDecimal maximumMarkExclusive,
            String grade,
            BigDecimal gradePoint) {
        public GradeBand {
            Objects.requireNonNull(minimumMark, "Minimum mark is required.");
            Objects.requireNonNull(maximumMarkExclusive, "Exclusive maximum mark is required.");
            Objects.requireNonNull(gradePoint, "Grade point is required.");
            if (minimumMark.signum() < 0 || maximumMarkExclusive.compareTo(new BigDecimal("101")) > 0
                    || minimumMark.compareTo(maximumMarkExclusive) >= 0
                    || grade == null || grade.isBlank()
                    || gradePoint.signum() < 0 || gradePoint.compareTo(new BigDecimal("4.00")) > 0) {
                throw new ValidationException("Invalid grade band.");
            }
        }

        public boolean contains(BigDecimal mark) {
            return mark.compareTo(minimumMark) >= 0 && mark.compareTo(maximumMarkExclusive) < 0;
        }
    }

    public GradeScheme {
        if (version == null || version.isBlank() || bands == null || bands.isEmpty()) {
            throw new ValidationException("A versioned grade scheme with grade bands is required.");
        }
        bands = bands.stream()
                .sorted(Comparator.comparing(GradeBand::minimumMark))
                .toList();
        for (int index = 1; index < bands.size(); index++) {
            GradeBand previous = bands.get(index - 1);
            GradeBand current = bands.get(index);
            if (previous.maximumMarkExclusive().compareTo(current.minimumMark()) != 0) {
                throw new ValidationException("Grade bands must be contiguous and must not overlap.");
            }
        }
        if (bands.get(0).minimumMark().compareTo(BigDecimal.ZERO) != 0
                || bands.get(bands.size() - 1).maximumMarkExclusive().compareTo(new BigDecimal("101")) != 0) {
            throw new ValidationException("Grade scheme must cover every mark from 0 to 100.");
        }
    }

    public static GradeScheme technologyStreamDefault(String version) {
        return new GradeScheme(version, List.of(
                band("85", "101", "A+", "4.00"),
                band("80", "85", "A", "4.00"),
                band("75", "80", "A-", "3.70"),
                band("70", "75", "B+", "3.30"),
                band("65", "70", "B", "3.00"),
                band("60", "65", "B-", "2.70"),
                band("55", "60", "C+", "2.30"),
                band("50", "55", "C", "2.00"),
                band("45", "50", "C-", "1.70"),
                band("40", "45", "D+", "1.30"),
                band("35", "40", "D", "1.00"),
                band("0", "35", "E", "0.00")));
    }

    private static GradeBand band(String min, String max, String grade, String point) {
        return new GradeBand(new BigDecimal(min), new BigDecimal(max), grade, new BigDecimal(point));
    }
}
