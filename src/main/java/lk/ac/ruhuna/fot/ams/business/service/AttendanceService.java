package lk.ac.ruhuna.fot.ams.business.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.AttendanceCalculator.AttendanceSummary;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceHistoryRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.AttendanceRow;
import lk.ac.ruhuna.fot.ams.data.dao.AttendanceDao.SessionRow;
import lk.ac.ruhuna.fot.ams.data.dao.AuditLogDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao;
import lk.ac.ruhuna.fot.ams.data.dao.MedicalDao.MedicalRow;
import lk.ac.ruhuna.fot.ams.data.dao.TimetableDao;
import lk.ac.ruhuna.fot.ams.data.transaction.TransactionManager;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class AttendanceService {
    private final AttendanceDao attendanceDao;
    private final CourseDao courseDao;
    private final TimetableDao timetableDao;
    private final MedicalDao medicalDao;
    private final AuditLogDao auditLogDao;
    private final TransactionManager transactions;
    private final AuthorizationService authorization;
    private final AttendanceCalculator calculator;
    private final BigDecimal attendanceThreshold;
    private final Clock clock;

    public AttendanceService(
            AttendanceDao attendanceDao,
            CourseDao courseDao,
            TimetableDao timetableDao,
            MedicalDao medicalDao,
            AuditLogDao auditLogDao,
            TransactionManager transactions,
            AuthorizationService authorization,
            AttendanceCalculator calculator,
            BigDecimal attendanceThreshold,
            Clock clock) {
        this.attendanceDao = Objects.requireNonNull(attendanceDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.timetableDao = Objects.requireNonNull(timetableDao);
        this.medicalDao = Objects.requireNonNull(medicalDao);
        this.auditLogDao = Objects.requireNonNull(auditLogDao);
        this.transactions = Objects.requireNonNull(transactions);
        this.authorization = Objects.requireNonNull(authorization);
        this.calculator = Objects.requireNonNull(calculator);
        this.attendanceThreshold = Objects.requireNonNull(attendanceThreshold);
        this.clock = Objects.requireNonNull(clock);
    }

    public long saveSession(
            AuthenticatedSession actor,
            SessionRow request,
            Map<Integer, Boolean> attendanceByEnrollment) {
        if (request == null) {
            throw new ValidationException("Attendance session details are required.");
        }
        authorization.requireTechnicalOfficerForOffering(actor, request.offeringId());
        if (request.date() == null || request.date().isAfter(LocalDate.now(clock))) {
            throw new ValidationException("Attendance session date cannot be in the future.");
        }
        if (request.timetableEntryId() == null
                || !timetableDao.entryMatchesOfferingAndComponent(
                        request.timetableEntryId(), request.offeringId(), request.componentId(), request.date())) {
            throw new ValidationException("Attendance session must reference its matching timetable entry.");
        }
        CourseDao.OfferingRow offering = courseDao.findOffering(request.offeringId())
                .orElseThrow(() -> new ValidationException("Course offering was not found."));
        boolean componentBelongsToCourse = courseDao.findComponents(offering.courseId()).stream()
                .anyMatch(component -> component.id() == request.componentId());
        if (!componentBelongsToCourse) {
            throw new ValidationException("Attendance component does not belong to the course offering.");
        }
        Set<Integer> requiredEnrollmentIds = new HashSet<>();
        courseDao.findActiveEnrollments(request.offeringId())
                .forEach(enrollment -> requiredEnrollmentIds.add(enrollment.id()));
        if (attendanceByEnrollment == null
                || !attendanceByEnrollment.keySet().equals(requiredEnrollmentIds)
                || attendanceByEnrollment.values().stream().anyMatch(Objects::isNull)) {
            throw new ValidationException("Attendance must be recorded for every active enrolment exactly once.");
        }
        int officerId = authorization.technicalOfficerId(actor);
        return transactions.execute(connection -> {
            long sessionId = attendanceDao.insertSession(connection,
                    new SessionRow(0, request.offeringId(), request.componentId(), request.date(),
                            request.timetableEntryId(), officerId));
            for (Map.Entry<Integer, Boolean> record : attendanceByEnrollment.entrySet()) {
                attendanceDao.insertRecord(connection,
                        new AttendanceRow(0, Math.toIntExact(sessionId), record.getKey(), record.getValue()));
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "ATTENDANCE_SESSION_CREATE", "ATTENDANCE_SESSION",
                    sessionId, LocalDateTime.now(clock)));
            return sessionId;
        });
    }

    public AttendanceSummary calculateForEnrollment(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.STUDENT_ATTENDANCE);
        if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
        } else if (actor.role() == Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        } else if (actor.role() == Role.UNDERGRADUATE) {
            authorization.requireUndergraduateForStudent(actor, studentId);
        } else {
            throw new AuthorizationException("You do not have permission to view this attendance report.");
        }
        boolean validEnrollment = courseDao.findEnrollments(offeringId).stream()
                .anyMatch(enrollment -> enrollment.id() == enrollmentId
                        && enrollment.studentId() == studentId
                        && enrollment.offeringId() == offeringId);
        if (!validEnrollment) {
            throw new AuthorizationException("Attendance filters do not match an authorised enrolment.");
        }

        CourseDao.OfferingRow offering = courseDao.findOffering(offeringId)
                .orElseThrow(() -> new ValidationException("Course offering was not found."));
        List<AttendanceHistoryRow> history = attendanceDao.findHistory(enrollmentId, offeringId);
        Set<LocalDate> excusedAbsenceDates = approvedMedicalAbsences(
                studentId, offeringId, history);
        EnumMap<ComponentType, AttendanceCalculator.ComponentAttendance> attendance =
                new EnumMap<>(ComponentType.class);
        for (CourseDao.ComponentRow component : courseDao.findComponents(offering.courseId())) {
            List<AttendanceHistoryRow> componentSessions = history.stream()
                    .filter(row -> row.componentType() == component.type())
                    .toList();
            int attended = (int) componentSessions.stream().filter(AttendanceHistoryRow::present).count();
            int excused = (int) componentSessions.stream()
                    .filter(row -> !row.present() && excusedAbsenceDates.contains(row.sessionDate()))
                    .count();
            int scheduled = Math.max(component.plannedSessions(), componentSessions.size());
            attendance.put(component.type(),
                    new AttendanceCalculator.ComponentAttendance(scheduled, attended, excused));
        }
        return calculator.calculate(attendance, attendanceThreshold);
    }

    private Set<LocalDate> approvedMedicalAbsences(
            int studentId, int offeringId, List<AttendanceHistoryRow> history) {
        if (history.isEmpty()) {
            return Set.of();
        }
        LocalDate first = history.stream().map(AttendanceHistoryRow::sessionDate).min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate last = history.stream().map(AttendanceHistoryRow::sessionDate).max(LocalDate::compareTo)
                .orElseThrow();
        List<MedicalRow> approved = medicalDao.findApprovedForRange(studentId, offeringId, first, last).stream()
                .filter(record -> record.status() == ApprovalStatus.APPROVED)
                .toList();
        return history.stream()
                .filter(row -> !row.present())
                .filter(row -> approved.stream().anyMatch(record ->
                        !row.sessionDate().isBefore(record.startDate())
                                && !row.sessionDate().isAfter(record.endDate())))
                .map(AttendanceHistoryRow::sessionDate)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
