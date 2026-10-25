package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityResponse;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.ComponentResult;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentMarksReport;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.AssessmentRow;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.MarkRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceHistoryRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportFormatters;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportFormatters.AttendanceLine;
import org.junit.jupiter.api.Test;

class ReportFormattersTest {
    @Test
    void studentAttendanceListsSessionsByDateWithTextStatus() {
        var report = new StudentAttendanceReport(7, 7, 1, List.of(
                new AttendanceHistoryRow(2, 7, 1, ComponentType.THEORY, LocalDate.of(2026, 9, 5), false),
                new AttendanceHistoryRow(1, 7, 1, ComponentType.THEORY, LocalDate.of(2026, 9, 2), true)));

        var result = ReportFormatters.studentAttendance(report);

        assertThat(result.columns()).containsExactly("Date", "Component", "Status");
        assertThat(result.rows()).containsExactly(
                List.of("2026-09-02", "THEORY", "PRESENT"), List.of("2026-09-05", "THEORY", "ABSENT"));
    }

    @Test
    void marksShowNotEnteredWhenAnAssessmentHasNoMark() {
        var report = new StudentMarksReport(7, 7, 1,
                List.of(new AssessmentRow(10, 1, "Quiz 1", "QUIZ", new BigDecimal("10.00"), true),
                        new AssessmentRow(11, 1, "Final Exam", "FINAL", new BigDecimal("50.00"), false)),
                List.of(new MarkRow(1, 10, 7, new BigDecimal("88.00"), 1)));

        var result = ReportFormatters.marks(report);

        assertThat(result.rows()).containsExactly(
                List.of("Quiz 1", "QUIZ", "10.00", "Yes", "88.00"),
                List.of("Final Exam", "FINAL", "50.00", "No", "Not entered"));
    }

    @Test
    void eligibilityIsShownAsTextWithTheFailedReasons() {
        var result = ReportFormatters.eligibility(
                new EligibilityResponse(false, true, false, List.of("practical attendance below 80.00%.")));

        assertThat(result.rows()).containsExactly(
                List.of("Final exam eligibility", "NOT ELIGIBLE"), List.of("CA eligibility", "PASS"),
                List.of("Attendance eligibility", "FAIL"), List.of("Reason", "practical attendance below 80.00%."));
    }

    @Test
    void batchAttendanceUsesTheCalculatorToDecideAgainstTheThreshold() {
        BigDecimal threshold = new BigDecimal("80.00");
        var low = new ComponentResult(11, 15, new BigDecimal("73.33"));
        var high = new ComponentResult(14, 15, new BigDecimal("93.33"));
        var lines = List.of(
                new AttendanceLine("STU005", "Five", new AttendanceSummary(Map.of(ComponentType.THEORY, low), low, threshold)),
                new AttendanceLine("STU001", "One", new AttendanceSummary(Map.of(ComponentType.THEORY, high), high, threshold)));

        var result = ReportFormatters.batchAttendance(lines);

        assertThat(result.rows().get(0)).containsExactly("STU005", "Five", "11 / 15 (73.33%)", "-",
                "11 / 15 (73.33%)", "BELOW 80.00%");
        assertThat(result.rows().get(1).get(5)).isEqualTo("MEETS 80.00%");
    }
}
