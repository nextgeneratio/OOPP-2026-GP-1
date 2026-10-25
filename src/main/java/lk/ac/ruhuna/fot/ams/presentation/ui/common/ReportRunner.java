package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.api.controller.AttendanceController.EnrollmentAttendanceRequest;
import lk.ac.ruhuna.fot.ams.api.controller.EligibilityController.EligibilityRequest;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController.GpaReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.GradesController.StudentScopeRequest;
import lk.ac.ruhuna.fot.ams.api.controller.MedicalController.MedicalSearchRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.OfferingReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.ReportController.StudentReportRequest;
import lk.ac.ruhuna.fot.ams.api.controller.UserController.SearchUsersRequest;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.GpaReport;
import lk.ac.ruhuna.fot.ams.business.service.GradesService.SemesterOption;
import lk.ac.ruhuna.fot.ams.business.service.ReportService.StudentAttendanceReport;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.OfferingSummary;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.RosterRow;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.ReportFormatters.AttendanceLine;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/**
 * Asks the right controllers for each report. It holds no rules: every call is authorised by the services, and a
 * refusal comes back as an ApiResponse failure that the screen shows safely. Free of Swing so it can be tested.
 */
public final class ReportRunner {
    /** What the user picked; offering and student are null when the category does not need them. */
    public record Selection(OfferingSummary offering, RosterRow student, List<RosterRow> roster) {
        public Selection {
            roster = roster == null ? List.of() : List.copyOf(roster);
        }
    }

    private final AuthenticatedSession session;
    private final UiServices services;

    public ReportRunner(AuthenticatedSession session, UiServices services) {
        this.session = session;
        this.services = services;
    }

    public ApiResponse<List<OfferingSummary>> offerings() {
        return session.role() == Role.TECHNICAL_OFFICER
                ? services.attendance().myOfferings(session)
                : services.eligibility().myOfferings(session);
    }

    public ApiResponse<List<RosterRow>> roster(int offeringId) {
        return session.role() == Role.TECHNICAL_OFFICER
                ? services.screens().medicalStudents(session, offeringId)
                : services.eligibility().roster(session, offeringId);
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
            case GRADES -> map(services.grades().courseResults(session, new StudentScopeRequest(student.studentId())),
                    ReportFormatters::courseResults);
            case SGPA_CGPA -> gpa(student);
            case MEDICAL -> map(services.medical().findForStudent(session,
                    new MedicalSearchRequest(student.studentId(), offering.offeringId())), ReportFormatters::medical);
            case STUDENT_PROFILE -> session.role() == Role.ADMIN
                    ? map(services.users().search(session, new SearchUsersRequest(null, Role.UNDERGRADUATE, null)),
                            ReportFormatters::profiles)
                    : map(services.eligibility().roster(session, offering.offeringId()), ReportFormatters::roster);
        };
    }

    private ApiResponse<ReportResult> batchAttendance(OfferingSummary offering, List<RosterRow> roster) {
        ApiResponse<List<StudentAttendanceReport>> entries =
                services.reports().batchCourseAttendance(session, new OfferingReportRequest(offering.offeringId()));
        if (!entries.successful()) {
            return ApiResponse.failure(entries.error());
        }
        Map<Integer, RosterRow> byEnrollment = new HashMap<>();
        roster.forEach(row -> byEnrollment.put(row.enrollmentId(), row));
        List<AttendanceLine> lines = new ArrayList<>();
        for (StudentAttendanceReport entry : entries.data()) {
            var summary = services.attendance().calculateForEnrollment(session,
                    new EnrollmentAttendanceRequest(entry.studentId(), entry.enrollmentId(), offering.offeringId()));
            if (!summary.successful()) {
                return ApiResponse.failure(summary.error());
            }
            RosterRow who = byEnrollment.get(entry.enrollmentId());
            lines.add(new AttendanceLine(who == null ? "#" + entry.studentId() : who.studentNumber(),
                    who == null ? "" : who.fullName(), summary.data()));
        }
        lines.sort(java.util.Comparator.comparing(AttendanceLine::studentNumber));
        return ApiResponse.success(ReportFormatters.batchAttendance(lines));
    }

    private ApiResponse<ReportResult> gpa(RosterRow student) {
        ApiResponse<List<SemesterOption>> semesters =
                services.grades().semesters(session, new StudentScopeRequest(student.studentId()));
        if (!semesters.successful()) {
            return ApiResponse.failure(semesters.error());
        }
        List<GpaReport> reports = new ArrayList<>();
        for (SemesterOption semester : semesters.data()) {
            ApiResponse<GpaReport> report = services.grades().gpaReport(session,
                    new GpaReportRequest(student.studentId(), semester.id()));
            if (!report.successful()) {
                return ApiResponse.failure(report.error());
            }
            reports.add(report.data());
        }
        return ApiResponse.success(ReportFormatters.gpa(reports));
    }

    private static <T> ApiResponse<ReportResult> map(ApiResponse<T> response, Function<T, ReportResult> formatter) {
        return response.successful()
                ? ApiResponse.success(formatter.apply(response.data()))
                : ApiResponse.failure(response.error());
    }
}
