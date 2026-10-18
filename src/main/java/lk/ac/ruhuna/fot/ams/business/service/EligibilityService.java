package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.business.policy.FinalExamEligibilityPolicy;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class EligibilityService {
    private final FinalExamEligibilityPolicy policy;
    private final MarksService marksService;
    private final AttendanceService attendanceService;
    private final AuthorizationService authorization;

    public EligibilityService(
            FinalExamEligibilityPolicy policy,
            MarksService marksService,
            AttendanceService attendanceService,
            AuthorizationService authorization) {
        this.policy = Objects.requireNonNull(policy);
        this.marksService = Objects.requireNonNull(marksService);
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.authorization = Objects.requireNonNull(authorization);
    }

    public FinalExamEligibilityPolicy.Result evaluate(BigDecimal caMark, AttendanceSummary attendance) {
        return policy.evaluate(new FinalExamEligibilityPolicy.Input(caMark, attendance));
    }

    public FinalExamEligibilityPolicy.Result evaluateForEnrollment(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.ELIGIBILITY);
        BigDecimal caMark = marksService.calculateCaMark(actor, offeringId, enrollmentId);
        AttendanceSummary attendance = attendanceService.calculateForEnrollment(
                actor, studentId, enrollmentId, offeringId);
        return evaluate(caMark, attendance);
    }
}
