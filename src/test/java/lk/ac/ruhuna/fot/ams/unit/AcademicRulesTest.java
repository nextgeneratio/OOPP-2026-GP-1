package lk.ac.ruhuna.fot.ams.unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lk.ac.ruhuna.fot.ams.business.calculator.AssessmentCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GradeCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GradeScheme;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy;
import lk.ac.ruhuna.fot.ams.business.validator.TimetableConflictValidator;
import lk.ac.ruhuna.fot.ams.business.validator.TimetableConflictValidator.TimetableSlot;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcademicRulesTest {
    private final AssessmentCalculator assessmentCalculator = new AssessmentCalculator();
    private final AttendanceCalculator attendanceCalculator = new AttendanceCalculator();

    @Test
    void validatesPublishedAssessmentWeightsAndCalculatesFinalAndCaMarks() {
        List<AssessmentCalculator.WeightedMark> assessments = List.of(
                weighted("80", "40", true),
                weighted("60", "60", false));

        assessmentCalculator.validatePublishedPlan(assessments);
        assertThat(assessmentCalculator.calculateFinalMark(assessments))
                .isEqualByComparingTo("68.00000000");
        assertThat(assessmentCalculator.calculateCaMark(assessments))
                .isEqualByComparingTo("80.00000000");
    }

    @Test
    void acceptsMarkAndAssessmentWeightBoundariesAndRejectsInvalidPlans() {
        assessmentCalculator.validatePublishedPlan(List.of(weighted("0", "0", true), weighted("100", "100", false)));
        assertThatThrownBy(() -> assessmentCalculator.validatePublishedPlan(List.of(weighted("50", "99.99", true))))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> weighted("-0.01", "1", true)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> weighted("100.01", "1", true)).isInstanceOf(ValidationException.class);
    }

    @Test
    void caEligibilityUsesFortyPercentBoundary() {
        assertThat(caEligible("39.99")).isFalse();
        assertThat(caEligible("40.00")).isTrue();
        assertThat(caEligible("40.01")).isTrue();
    }

    @Test
    void calculatesTheoryPracticalAndCombinedAttendanceSeparately() {
        AttendanceCalculator.AttendanceSummary summary = attendanceCalculator.calculate(Map.of(
                ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 14, 0),
                ComponentType.PRACTICAL, new AttendanceCalculator.ComponentAttendance(15, 11, 0)));

        assertThat(summary.components().get(ComponentType.THEORY).percentage()).isEqualByComparingTo("93.33");
        assertThat(summary.components().get(ComponentType.PRACTICAL).percentage()).isEqualByComparingTo("73.33");
        assertThat(summary.combined().percentage()).isEqualByComparingTo("83.33");
        assertThat(summary.eligible()).isFalse();
    }

    @Test
    void attendanceUsesEightyPercentBoundaryAndApprovedMedicalExclusions() {
        AttendanceCalculator.AttendanceSummary exactlyEighty = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 12, 0)));
        AttendanceCalculator.AttendanceSummary aboveEighty = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 13, 0)));
        AttendanceCalculator.AttendanceSummary belowEighty = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 10, 0)));
        AttendanceCalculator.AttendanceSummary excused = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 10, 3)));
        AttendanceCalculator.AttendanceSummary notExcused = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 10, 0)));

        assertThat(exactlyEighty.eligible()).isTrue();
        assertThat(aboveEighty.eligible()).isTrue();
        assertThat(belowEighty.eligible()).isFalse();
        assertThat(excused.components().get(ComponentType.THEORY).eligibleScheduledSessions()).isEqualTo(12);
        assertThat(excused.components().get(ComponentType.THEORY).percentage()).isEqualByComparingTo("83.33");
        assertThat(excused.eligible()).isTrue();
        assertThat(notExcused.eligible()).isFalse();
    }

    @Test
    void allMedicallyExcusedSessionsAreEligibleAtOneHundredPercent() {
        AttendanceCalculator.AttendanceSummary summary = attendanceCalculator.calculate(
                Map.of(ComponentType.PRACTICAL, new AttendanceCalculator.ComponentAttendance(15, 0, 15)));

        assertThat(summary.components().get(ComponentType.PRACTICAL).percentage()).isEqualByComparingTo("100.00");
        assertThat(summary.eligible()).isTrue();
    }

    @Test
    void attendanceRejectsInconsistentSessionCounts() {
        assertThatThrownBy(() -> new AttendanceCalculator.ComponentAttendance(10, 8, 3))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void finalExamEligibilityReturnsFailedConditions() {
        AttendanceCalculator.AttendanceSummary failedAttendance = attendanceCalculator.calculate(Map.of(
                ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 12, 0),
                ComponentType.PRACTICAL, new AttendanceCalculator.ComponentAttendance(15, 10, 0)));
        FinalExamEligibilityPolicy policy = new FinalExamEligibilityPolicy();

        FinalExamEligibilityPolicy.Result result = policy.evaluate(
                new FinalExamEligibilityPolicy.Input(new BigDecimal("39.99"), failedAttendance));

        assertThat(result.eligible()).isFalse();
        assertThat(result.failedConditions()).containsExactly(
                "CA mark is below 40.00%.",
                "practical attendance below 80.00%.");
        assertThat(policy.evaluate(new FinalExamEligibilityPolicy.Input(
                new BigDecimal("40"), attendanceCalculator.calculate(
                        Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 12, 0)))))
                .eligible()).isTrue();
    }

    @Test
    void mapsEveryGradeBoundaryAndPreservesSchemeVersion() {
        GradeCalculator calculator = new GradeCalculator(GradeScheme.technologyStreamDefault("UGC-2024-v1"));
        assertGrade(calculator, "100", "A+", "4.00");
        assertGrade(calculator, "85", "A+", "4.00");
        assertGrade(calculator, "84", "A", "4.00");
        assertGrade(calculator, "84.99", "A", "4.00");
        assertGrade(calculator, "80", "A", "4.00");
        assertGrade(calculator, "79", "A-", "3.70");
        assertGrade(calculator, "79.99", "A-", "3.70");
        assertGrade(calculator, "75", "A-", "3.70");
        assertGrade(calculator, "74", "B+", "3.30");
        assertGrade(calculator, "74.99", "B+", "3.30");
        assertGrade(calculator, "70", "B+", "3.30");
        assertGrade(calculator, "69", "B", "3.00");
        assertGrade(calculator, "69.99", "B", "3.00");
        assertGrade(calculator, "65", "B", "3.00");
        assertGrade(calculator, "64", "B-", "2.70");
        assertGrade(calculator, "64.99", "B-", "2.70");
        assertGrade(calculator, "60", "B-", "2.70");
        assertGrade(calculator, "59", "C+", "2.30");
        assertGrade(calculator, "59.99", "C+", "2.30");
        assertGrade(calculator, "55", "C+", "2.30");
        assertGrade(calculator, "54", "C", "2.00");
        assertGrade(calculator, "54.99", "C", "2.00");
        assertGrade(calculator, "50", "C", "2.00");
        assertGrade(calculator, "49", "C-", "1.70");
        assertGrade(calculator, "49.99", "C-", "1.70");
        assertGrade(calculator, "45", "C-", "1.70");
        assertGrade(calculator, "44", "D+", "1.30");
        assertGrade(calculator, "44.99", "D+", "1.30");
        assertGrade(calculator, "40", "D+", "1.30");
        assertGrade(calculator, "39", "D", "1.00");
        assertGrade(calculator, "39.99", "D", "1.00");
        assertGrade(calculator, "35", "D", "1.00");
        assertGrade(calculator, "34", "E", "0.00");
        assertGrade(calculator, "34.99", "E", "0.00");
        assertGrade(calculator, "0", "E", "0.00");
        assertThatThrownBy(() -> calculator.grade(new BigDecimal("100.01")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void calculatesWeightedSgpaAndCgpaWithConfigurableRepeatRule() {
        GpaCalculator calculator = new GpaCalculator();
        List<GpaCalculator.CourseResult> results = List.of(
                course("ICT1", "3", "4.00", true, 1, "2026-01-10"),
                course("ICT2", "2", "0.00", true, 1, "2026-01-11"),
                course("ICT3", "4", "3.00", false, 1, null),
                course("ICT1", "3", "2.00", true, 2, "2026-08-10"));

        assertThat(calculator.calculateSgpa(results.subList(0, 3))).isEqualByComparingTo("2.40");
        assertThat(calculator.calculateCgpa(results, GpaCalculator.RepeatSelectionRule.LATEST_COMPLETED_ATTEMPT))
                .isEqualByComparingTo("1.20");
        assertThat(calculator.calculateCgpa(results, GpaCalculator.RepeatSelectionRule.ALL_COMPLETED_ATTEMPTS))
                .isEqualByComparingTo("2.25");
    }

    @Test
    void rejectsGpaWhenThereAreNoCompletedCredits() {
        GpaCalculator calculator = new GpaCalculator();
        assertThatThrownBy(() -> calculator.calculateSgpa(
                List.of(course("ICT1", "3", "4.00", false, 1, null))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void validatesTimetableIntervalsAndResourceConflicts() {
        TimetableConflictValidator validator = new TimetableConflictValidator();
        TimetableSlot existing = slot(1, "09:00", "10:00", "R1", 5, 10);
        TimetableSlot adjacent = slot(2, "10:00", "11:00", "R1", 6, 11);
        TimetableSlot lecturerConflict = slot(3, "09:30", "10:30", "R2", 5, 12);

        validator.validateNoConflict(adjacent, List.of(existing));
        assertThatThrownBy(() -> validator.validateNoConflict(lecturerConflict, List.of(existing)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> slot(4, "10:00", "10:00", "R3", 6, 13))
                .isInstanceOf(ValidationException.class);
    }

    private boolean caEligible(String score) {
        AttendanceCalculator.AttendanceSummary attendance = attendanceCalculator.calculate(
                Map.of(ComponentType.THEORY, new AttendanceCalculator.ComponentAttendance(15, 15, 0)));
        return new FinalExamEligibilityPolicy().evaluate(
                new FinalExamEligibilityPolicy.Input(new BigDecimal(score), attendance)).caEligible();
    }

    private AssessmentCalculator.WeightedMark weighted(String mark, String weight, boolean ca) {
        return new AssessmentCalculator.WeightedMark(new BigDecimal(mark), new BigDecimal(weight), ca);
    }

    private void assertGrade(GradeCalculator calculator, String mark, String grade, String points) {
        GradeCalculator.GradeResult result = calculator.grade(new BigDecimal(mark));
        assertThat(result.grade()).isEqualTo(grade);
        assertThat(result.gradePoint()).isEqualByComparingTo(points);
        assertThat(result.schemeVersion()).isEqualTo("UGC-2024-v1");
    }

    private GpaCalculator.CourseResult course(
            String code, String credits, String points, boolean completed, int attempt, String date) {
        return new GpaCalculator.CourseResult(
                code,
                new BigDecimal(credits),
                new BigDecimal(points),
                completed,
                attempt,
                date == null ? null : LocalDate.parse(date));
    }

    private TimetableSlot slot(long id, String start, String end, String room, long lecturer, long offering) {
        return new TimetableSlot(
                id, LocalDate.parse("2026-10-05"), LocalTime.parse(start), LocalTime.parse(end),
                room, lecturer, offering, 1, 1, ComponentType.THEORY);
    }
}
