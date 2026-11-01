package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController.EnrollmentAttendanceRequest;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.OfferingReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.StudentReportRequest;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportFormatters.AttendanceLine;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Adapts supported reports to existing controllers; service authorization remains authoritative. */
public final class ReportRunner {
    public record Selection(OfferingSummary offering, RosterRow student, List<RosterRow> roster) {
        public Selection { roster = roster == null ? List.of() : List.copyOf(roster); }
    }
    private final AuthenticatedSession session;
    private final UiServices services;

    public ReportRunner(AuthenticatedSession session, UiServices services) {
        this.session = session;
        this.services = services;
    }

    public ApiResponse<List<OfferingSummary>> offerings() {
        return services.courses().offeringSummariesForActor(session);
    }

    public ApiResponse<List<RosterRow>> roster(int offeringId) {
        return services.courses().rosterForOffering(session, offeringId);
    }

    public ApiResponse<ReportResult> run(ReportCategory category, Selection selection) {
        OfferingSummary offering = selection.offering();
        RosterRow student = selection.student();
        return switch (category) {
            case STUDENT_ATTENDANCE -> map(services.reports().studentAttendance(session,
                    new StudentReportRequest(student.studentId(), student.enrollmentId(), offering.offeringId())),
                    ReportFormatters::studentAttendance);
            case BATCH_COURSE_ATTENDANCE -> batchAttendance(offering, selection.roster());
            case CA_FINAL_MARKS -> map(services.reports().studentMarks(session,
                    new StudentReportRequest(student.studentId(), student.enrollmentId(), offering.offeringId())),
                    ReportFormatters::marks);
            case ELIGIBILITY -> map(services.eligibility().evaluate(session,
                    new EligibilityRequest(student.studentId(), student.enrollmentId(), offering.offeringId())),
                    ReportFormatters::eligibility);
        };
    }

    private ApiResponse<ReportResult> batchAttendance(OfferingSummary offering, List<RosterRow> roster) {
        var entries = services.reports().batchCourseAttendance(session,
                new OfferingReportRequest(offering.offeringId()));
        if (!entries.successful()) return ApiResponse.failure(entries.error());
        Map<Integer, RosterRow> byEnrollment = new HashMap<>();
        roster.forEach(row -> byEnrollment.put(row.enrollmentId(), row));
        List<AttendanceLine> lines = new ArrayList<>();
        for (StudentAttendanceReport entry : entries.data()) {
            var summary = services.attendance().calculateForEnrollment(session,
                    new EnrollmentAttendanceRequest(entry.studentId(), entry.enrollmentId(), offering.offeringId()));
            if (!summary.successful()) return ApiResponse.failure(summary.error());
            RosterRow who = byEnrollment.get(entry.enrollmentId());
            lines.add(new AttendanceLine(who == null ? "" : who.studentNumber(),
                    who == null ? "" : who.fullName(), summary.data()));
        }
        lines.sort(Comparator.comparing(AttendanceLine::studentNumber));
        return ApiResponse.success(ReportFormatters.batchAttendance(lines));
    }

    private static <T> ApiResponse<ReportResult> map(ApiResponse<T> response,
            Function<T, ReportResult> formatter) {
        return response.successful() ? ApiResponse.success(formatter.apply(response.data()))
                : ApiResponse.failure(response.error());
    }
}
