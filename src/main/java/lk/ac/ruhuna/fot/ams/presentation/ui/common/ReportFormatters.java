package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityResponse;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.ComponentResult;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentMarksReport;
import lk.ac.ruhuna.fot.ams.data.dao.AssessmentDao.MarkRow;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;

/** Arranges data the business layer already produced into report tables. Nothing here calculates or decides. */
public final class ReportFormatters {
    private ReportFormatters() {
    }

    public record AttendanceLine(String studentNumber, String fullName, AttendanceSummary summary) {
    }

    public static ReportResult studentAttendance(StudentAttendanceReport report) {
        List<List<String>> rows = report.sessions().stream()
                .sorted(Comparator.comparing(session -> session.sessionDate()))
                .map(session -> List.of(session.sessionDate().toString(), session.componentType().name(),
                        session.present() ? "PRESENT" : "ABSENT"))
                .toList();
        return new ReportResult("Student attendance", List.of("Date", "Component", "Status"), rows);
    }

    /** One row per student; the percentages and the threshold test come from AttendanceCalculator. */
    public static ReportResult batchAttendance(List<AttendanceLine> lines) {
        List<List<String>> rows = lines.stream().map(line -> {
            AttendanceSummary summary = line.summary();
            return List.of(line.studentNumber(), line.fullName(),
                    component(summary.components().get(ComponentType.THEORY)),
                    component(summary.components().get(ComponentType.PRACTICAL)),
                    component(summary.combined()),
                    summary.combined().meets(summary.threshold())
                            ? "MEETS " + summary.threshold().toPlainString() + "%"
                            : "BELOW " + summary.threshold().toPlainString() + "%");
        }).toList();
        return new ReportResult("Batch / course attendance",
                List.of("Student no.", "Name", "Theory", "Practical", "Combined", "Against threshold"), rows);
    }

    private static String component(ComponentResult result) {
        return result == null ? "-" : result.attendedSessions() + " / " + result.eligibleScheduledSessions()
                + " (" + result.percentage().toPlainString() + "%)";
    }

    public static ReportResult marks(StudentMarksReport report) {
        Map<Integer, MarkRow> markByAssessment = new java.util.HashMap<>();
        report.marks().forEach(mark -> markByAssessment.put(mark.assessmentId(), mark));
        List<List<String>> rows = report.assessments().stream().map(assessment -> {
            MarkRow mark = markByAssessment.get(assessment.id());
            return List.of(assessment.name(), assessment.type(), assessment.weight().toPlainString(),
                    assessment.caComponent() ? "Yes" : "No",
                    mark == null ? "Not entered" : mark.score().toPlainString());
        }).toList();
        return new ReportResult("CA and final marks",
                List.of("Assessment", "Type", "Weight (%)", "Counts for CA", "Mark"), rows);
    }

    public static ReportResult eligibility(EligibilityResponse response) {
        List<List<String>> rows = new java.util.ArrayList<>();
        rows.add(List.of("Final exam eligibility", response.eligible() ? "ELIGIBLE" : "NOT ELIGIBLE"));
        rows.add(List.of("CA eligibility", response.caEligible() ? "PASS" : "FAIL"));
        rows.add(List.of("Attendance eligibility", response.attendanceEligible() ? "PASS" : "FAIL"));
        response.failedConditions().forEach(reason -> rows.add(List.of("Reason", reason)));
        return new ReportResult("Final exam eligibility", List.of("Condition", "Result"), rows);
    }

}
