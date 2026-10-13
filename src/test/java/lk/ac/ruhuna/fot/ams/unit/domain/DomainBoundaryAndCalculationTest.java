package lk.ac.ruhuna.fot.ams.unit.domain;

import java.time.LocalDate;
import java.util.List;
import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator.AssessmentItem;
import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator.CourseResult;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.model.Assessment;
import lk.ac.ruhuna.fot.ams.domain.model.Course;
import lk.ac.ruhuna.fot.ams.domain.model.Mark;
import lk.ac.ruhuna.fot.ams.domain.model.MedicalRecord;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainBoundaryAndCalculationTest {

    @Nested
    @DisplayName("1. Marks Boundary Tests")
    class MarksBoundaryTests {

        @Test
        @DisplayName("Mark -0.01 is rejected")
        void negativeMarkRejected() {
            assertThatThrownBy(() -> new Mark(-0.01))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Marks must be between 0 and 100.");
        }

        @Test
        @DisplayName("Mark 0.00 is accepted")
        void minimumMarkAccepted() {
            Mark mark = new Mark(0.00);
            assertThat(mark.score()).isEqualTo(0.00);
        }

        @Test
        @DisplayName("Mark 100.00 is accepted")
        void maximumMarkAccepted() {
            Mark mark = new Mark(100.00);
            assertThat(mark.score()).isEqualTo(100.00);
        }

        @Test
        @DisplayName("Mark 100.01 is rejected")
        void markAboveHundredRejected() {
            assertThatThrownBy(() -> new Mark(100.01))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Marks must be between 0 and 100.");
        }
    }

    @Nested
    @DisplayName("2. Continuous Assessment (CA) Boundary Tests")
    class CaBoundaryTests {

        @Test
        @DisplayName("CA mark 39.99 fails eligibility")
        void caFailsJustBelowThreshold() {
            assertThat(AcademicCalculator.isCaEligible(39.99)).isFalse();
        }

        @Test
        @DisplayName("CA mark 40.00 passes eligibility")
        void caPassesAtExactThreshold() {
            assertThat(AcademicCalculator.isCaEligible(40.00)).isTrue();
        }

        @Test
        @DisplayName("CA mark 40.01 passes eligibility")
        void caPassesAboveThreshold() {
            assertThat(AcademicCalculator.isCaEligible(40.01)).isTrue();
        }
    }

    @Nested
    @DisplayName("3. Attendance Threshold & Denominator Zero Tests")
    class AttendanceBoundaryTests {

        @Test
        @DisplayName("Attendance 79.99% fails eligibility")
        void attendanceFailsJustBelowThreshold() {
            assertThat(AcademicCalculator.isAttendanceEligible(79.99)).isFalse();
        }

        @Test
        @DisplayName("Attendance 80.00% passes eligibility")
        void attendancePassesAtExactThreshold() {
            assertThat(AcademicCalculator.isAttendanceEligible(80.00)).isTrue();
        }

        @Test
        @DisplayName("Attendance 100.00% passes eligibility")
        void attendancePassesFull() {
            assertThat(AcademicCalculator.isAttendanceEligible(100.00)).isTrue();
        }

        @Test
        @DisplayName("Denominator zero handled safely without division by zero error")
        void denominatorZeroHandledSafely() {
            double percentage = AcademicCalculator.calculateAttendancePercentage(0, 0);
            assertThat(percentage).isEqualTo(0.0);
            assertThat(AcademicCalculator.isAttendanceEligible(percentage)).isFalse();
        }

        @Test
        @DisplayName("Attended sessions greater than scheduled throws validation error")
        void attendedGreaterThanScheduledFails() {
            assertThatThrownBy(() -> AcademicCalculator.calculateAttendancePercentage(16, 15))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("4. UGC Circular 12/2024 Grade Boundaries")
    class GradeBoundaryTests {

        @ParameterizedTest(name = "Mark {0} maps to Grade {1} with GP {2}")
        @CsvSource({
                "34.00, E,  0.00",
                "34.99, E,  0.00",
                "35.00, D,  1.00",
                "39.00, D,  1.00",
                "39.99, D,  1.00",
                "40.00, D+, 1.30",
                "44.00, D+, 1.30",
                "44.99, D+, 1.30",
                "45.00, C-, 1.70",
                "49.00, C-, 1.70",
                "49.99, C-, 1.70",
                "50.00, C,  2.00",
                "54.00, C,  2.00",
                "54.99, C,  2.00",
                "55.00, C+, 2.30",
                "59.00, C+, 2.30",
                "59.99, C+, 2.30",
                "60.00, B-, 2.70",
                "64.00, B-, 2.70",
                "64.99, B-, 2.70",
                "65.00, B,  3.00",
                "69.00, B,  3.00",
                "69.99, B,  3.00",
                "70.00, B+, 3.30",
                "74.00, B+, 3.30",
                "74.99, B+, 3.30",
                "75.00, A-, 3.70",
                "79.00, A-, 3.70",
                "79.99, A-, 3.70",
                "80.00, A,  4.00",
                "84.00, A,  4.00",
                "84.99, A,  4.00",
                "85.00, A+, 4.00",
                "100.0, A+, 4.00"
        })
        void gradeBoundariesMatchSpecification(double mark, String expectedGrade, double expectedPoint) {
            GradeScheme scheme = GradeScheme.forMark(mark);
            assertThat(scheme.letter()).isEqualTo(expectedGrade);
            assertThat(scheme.gradePoint()).isEqualTo(expectedPoint);
        }
    }

    @Nested
    @DisplayName("5. Property-Style Calculation Checks")
    class PropertyCalculationTests {

        @ParameterizedTest
        @ValueSource(doubles = {0.0, 15.5, 40.0, 75.25, 99.9, 100.0})
        @DisplayName("Percentage is bounded between 0 and 100")
        void percentageNeverOutOfBounds(double validScore) {
            Mark mark = new Mark(validScore);
            assertThat(mark.score()).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Final mark calculation never outside 0 to 100")
        void finalMarkNeverOutOfBounds() {
            List<AssessmentItem> items = List.of(
                    new AssessmentItem(75.0, 30.0),
                    new AssessmentItem(85.0, 70.0)
            );
            double finalMark = AcademicCalculator.calculateFinalMark(items);
            assertThat(finalMark).isBetween(0.0, 100.0);
            assertThat(finalMark).isEqualTo(82.00);
        }

        @Test
        @DisplayName("Monotonicity: Increasing an assessment mark never reduces the final mark")
        void increasingScoreMonotonicallyIncreasesOrPreservesFinalMark() {
            List<AssessmentItem> baseline = List.of(
                    new AssessmentItem(60.0, 40.0),
                    new AssessmentItem(70.0, 60.0)
            );
            List<AssessmentItem> improved = List.of(
                    new AssessmentItem(65.0, 40.0),
                    new AssessmentItem(70.0, 60.0)
            );
            double baselineFinal = AcademicCalculator.calculateFinalMark(baseline);
            double improvedFinal = AcademicCalculator.calculateFinalMark(improved);
            assertThat(improvedFinal).isGreaterThan(baselineFinal);
        }

        @Test
        @DisplayName("SGPA and CGPA values never fall outside 0.00 to 4.00")
        void gpaAlwaysWithinValidRange() {
            List<CourseResult> results = List.of(
                    new CourseResult(3.0, GradeScheme.forMark(88.0)), // A+ (4.00)
                    new CourseResult(2.0, GradeScheme.forMark(67.0)), // B (3.00)
                    new CourseResult(4.0, GradeScheme.forMark(52.0))  // C (2.00)
            );
            double gpa = AcademicCalculator.calculateGpa(results);
            assertThat(gpa).isBetween(0.00, 4.00);
            // (3*4.00 + 2*3.00 + 4*2.00) / (3+2+4) = (12 + 6 + 8) / 9 = 26 / 9 = 2.8888 -> 2.89
            assertThat(gpa).isEqualTo(2.89);
        }
    }

    @Nested
    @DisplayName("6. Other Domain Invariant Tests")
    class DomainInvariantTests {

        @Test
        @DisplayName("Assessment weight > 100 or <= 0 is rejected")
        void assessmentWeightBounds() {
            assertThatThrownBy(() -> new Assessment("Quiz 1", 0.0, true))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> new Assessment("Quiz 1", 100.01, true))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Course credits <= 0 is rejected")
        void courseCreditBounds() {
            assertThatThrownBy(() -> new Course("ICT2132", "OOP", 0.0))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> new Course("ICT2132", "OOP", -1.0))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Medical date in future or end date before start date is rejected")
        void medicalDateRangeValidity() {
            LocalDate today = LocalDate.now();
            assertThatThrownBy(() -> new MedicalRecord(today.plusDays(1), today, today, "Sick"))
                    .isInstanceOf(ValidationException.class);
            assertThatThrownBy(() -> new MedicalRecord(today, today, today.minusDays(1), "Sick"))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("Medical record status transitions properly")
        void medicalRecordStatusTransitions() {
            LocalDate today = LocalDate.now();
            MedicalRecord record = new MedicalRecord(today, today.minusDays(2), today, "Flu");
            assertThat(record.status()).isEqualTo(ApprovalStatus.PENDING);
            record.approve();
            assertThat(record.status()).isEqualTo(ApprovalStatus.APPROVED);
            record.reject();
            assertThat(record.status()).isEqualTo(ApprovalStatus.REJECTED);
        }
    }
}
