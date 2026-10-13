package lk.ac.ruhuna.fot.ams.business.calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class AcademicCalculator {

    public record AssessmentItem(double score, double weightPercent) {}
    public record CourseResult(double credits, GradeScheme gradeScheme) {}

    public static double calculateAttendancePercentage(int attendedSessions, int scheduledSessions) {
        if (scheduledSessions < 0 || attendedSessions < 0) {
            throw new ValidationException("Sessions count cannot be negative.");
        }
        if (attendedSessions > scheduledSessions) {
            throw new ValidationException("Attended sessions cannot exceed scheduled sessions.");
        }
        if (scheduledSessions == 0) {
            return 0.0;
        }
        double percentage = ((double) attendedSessions / scheduledSessions) * 100.0;
        return roundTwoDecimals(percentage);
    }

    public static boolean isAttendanceEligible(double percentage) {
        if (Double.isNaN(percentage) || percentage < 0.0 || percentage > 100.0) {
            throw new ValidationException("Attendance percentage must be between 0 and 100.");
        }
        return percentage >= 80.00;
    }

    public static boolean isCaEligible(double caMark) {
        if (Double.isNaN(caMark) || caMark < 0.0 || caMark > 100.0) {
            throw new ValidationException("CA mark must be between 0 and 100.");
        }
        return caMark >= 40.00;
    }

    public static boolean isFinalExamEligible(double attendancePercentage, double caMark) {
        return isAttendanceEligible(attendancePercentage) && isCaEligible(caMark);
    }

    public static double calculateFinalMark(List<AssessmentItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Assessments list cannot be empty.");
        }
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        for (AssessmentItem item : items) {
            if (Double.isNaN(item.score()) || item.score() < 0.0 || item.score() > 100.0) {
                throw new ValidationException("Marks must be between 0 and 100.");
            }
            if (Double.isNaN(item.weightPercent()) || item.weightPercent() <= 0.0 || item.weightPercent() > 100.0) {
                throw new ValidationException("Assessment weight must be greater than 0 and at most 100.");
            }
            totalWeight += item.weightPercent();
            weightedSum += item.score() * (item.weightPercent() / 100.0);
        }
        if (Math.abs(totalWeight - 100.0) > 0.01) {
            throw new ValidationException("Total assessment weights must equal 100%.");
        }
        return roundTwoDecimals(weightedSum);
    }

    public static double calculateGpa(List<CourseResult> courseResults) {
        if (courseResults == null || courseResults.isEmpty()) {
            return 0.0;
        }
        double totalCredits = 0.0;
        double totalQualityPoints = 0.0;
        for (CourseResult result : courseResults) {
            if (Double.isNaN(result.credits()) || result.credits() <= 0.0) {
                throw new ValidationException("Course credits must be greater than zero.");
            }
            if (result.gradeScheme() == null) {
                throw new ValidationException("Course grade must be provided.");
            }
            totalCredits += result.credits();
            totalQualityPoints += result.credits() * result.gradeScheme().gradePoint();
        }
        if (totalCredits == 0.0) {
            return 0.0;
        }
        double gpa = totalQualityPoints / totalCredits;
        return roundTwoDecimals(gpa);
    }

    private static double roundTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
