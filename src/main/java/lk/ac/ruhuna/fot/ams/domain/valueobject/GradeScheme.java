package lk.ac.ruhuna.fot.ams.domain.valueobject;

import java.util.Objects;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class GradeScheme {
    public static final GradeScheme A_PLUS = new GradeScheme("A+", 4.00);
    public static final GradeScheme A = new GradeScheme("A", 4.00);
    public static final GradeScheme A_MINUS = new GradeScheme("A-", 3.70);
    public static final GradeScheme B_PLUS = new GradeScheme("B+", 3.30);
    public static final GradeScheme B = new GradeScheme("B", 3.00);
    public static final GradeScheme B_MINUS = new GradeScheme("B-", 2.70);
    public static final GradeScheme C_PLUS = new GradeScheme("C+", 2.30);
    public static final GradeScheme C = new GradeScheme("C", 2.00);
    public static final GradeScheme C_MINUS = new GradeScheme("C-", 1.70);
    public static final GradeScheme D_PLUS = new GradeScheme("D+", 1.30);
    public static final GradeScheme D = new GradeScheme("D", 1.00);
    public static final GradeScheme E = new GradeScheme("E", 0.00);

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

    public static GradeScheme fromMark(double mark) {
        return forMark(mark);
    }

    public static GradeScheme forMark(double mark) {
        if (Double.isNaN(mark) || mark < 0.0 || mark > 100.0) {
            throw new ValidationException("Marks must be between 0 and 100.");
        }
        if (mark >= 85.0) {
            return A_PLUS;
        } else if (mark >= 80.0) {
            return A;
        } else if (mark >= 75.0) {
            return A_MINUS;
        } else if (mark >= 70.0) {
            return B_PLUS;
        } else if (mark >= 65.0) {
            return B;
        } else if (mark >= 60.0) {
            return B_MINUS;
        } else if (mark >= 55.0) {
            return C_PLUS;
        } else if (mark >= 50.0) {
            return C;
        } else if (mark >= 45.0) {
            return C_MINUS;
        } else if (mark >= 40.0) {
            return D_PLUS;
        } else if (mark >= 35.0) {
            return D;
        } else {
            return E;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GradeScheme that = (GradeScheme) o;
        return Double.compare(that.gradePoint, gradePoint) == 0 && Objects.equals(letter, that.letter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(letter, gradePoint);
    }

    @Override
    public String toString() {
        return letter + " (" + gradePoint + ")";
    }
}

