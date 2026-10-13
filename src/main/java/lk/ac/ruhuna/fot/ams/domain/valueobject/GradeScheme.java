package lk.ac.ruhuna.fot.ams.domain.valueobject;

import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class GradeScheme {
    private final String letter;
    private final double gradePoint;

    private GradeScheme(String letter, double gradePoint) {
        this.letter = letter;
        this.gradePoint = gradePoint;
    }

    public String letter() {
        return letter;
    }

    public double gradePoint() {
        return gradePoint;
    }

    public static GradeScheme forMark(double mark) {
        if (Double.isNaN(mark) || mark < 0.0 || mark > 100.0) {
            throw new ValidationException("Marks must be between 0 and 100.");
        }
        if (mark >= 85.0) {
            return new GradeScheme("A+", 4.00);
        } else if (mark >= 80.0) {
            return new GradeScheme("A", 4.00);
        } else if (mark >= 75.0) {
            return new GradeScheme("A-", 3.70);
        } else if (mark >= 70.0) {
            return new GradeScheme("B+", 3.30);
        } else if (mark >= 65.0) {
            return new GradeScheme("B", 3.00);
        } else if (mark >= 60.0) {
            return new GradeScheme("B-", 2.70);
        } else if (mark >= 55.0) {
            return new GradeScheme("C+", 2.30);
        } else if (mark >= 50.0) {
            return new GradeScheme("C", 2.00);
        } else if (mark >= 45.0) {
            return new GradeScheme("C-", 1.70);
        } else if (mark >= 40.0) {
            return new GradeScheme("D+", 1.30);
        } else if (mark >= 35.0) {
            return new GradeScheme("D", 1.00);
        } else {
            return new GradeScheme("E", 0.00);
        }
    }
}
