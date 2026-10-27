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
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao.EnrollmentRow;
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
    /** One row the officer marks present/absent for. */
    public record RosterEntry(int enrollmentId, String studentNumber, String fullName) {
    }

    /** Everything the attendance screen needs to build and submit one session. */
    public record SessionSetup(
            List<CourseDao.ComponentRow> components,
            List<TimetableDao.EntryRow> entries,
            List<RosterEntry> roster) {
        public SessionSetup {
            components = List.copyOf(components);
            entries = List.copyOf(entries);
            roster = List.copyOf(roster);
        }
    }

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

    public List<CourseDao.OfferingSummary> offeringsForTechnicalOfficer(AuthenticatedSession actor) {
        authorization.requireRole(actor, Role.TECHNICAL_OFFICER);
        return courseDao.findOfferingSummariesForTechnicalOfficer(authorization.technicalOfficerId(actor));
    }

    public SessionSetup sessionSetup(AuthenticatedSession actor, int offeringId) {
        authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        CourseDao.OfferingRow offering = courseDao.findOffering(offeringId)
                .orElseThrow(() -> new ValidationException("Course offering was not found."));
        List<CourseDao.ComponentRow> components = courseDao.findComponents(offering.courseId());
        int departmentId = authorizedDepartmentId(actor);
        int level = courseDao.findBatches(departmentId).stream()
                .filter(batch -> batch.id() == offering.batchId())
                .map(CourseDao.BatchRow::level).findFirst().orElse(1);
        List<TimetableDao.EntryRow> entries = timetableDao
                .findEntries(departmentId, offering.semesterId(), level, null).stream()
                .filter(entry -> entry.offeringId() == offeringId)
                .toList();
        List<RosterEntry> roster = courseDao.findRoster(offeringId).stream()
                .map(row -> new RosterEntry(row.enrollmentId(), row.studentNumber(), row.fullName()))
                .toList();
        return new SessionSetup(components, entries, roster);
    }

    private int authorizedDepartmentId(AuthenticatedSession actor) {
        int technicalOfficerId = authorization.technicalOfficerId(actor);
        return courseDao.findOfferingSummariesForTechnicalOfficer(technicalOfficerId).stream()
                .findFirst()
                .map(CourseDao.OfferingSummary::departmentId)
                .orElseThrow(() -> new ValidationException("Technical officer department could not be resolved."));
    }

    public long saveSession(
            AuthenticatedSession actor,
            SessionRow request,
            Map<Integer, Boolean> attendanceByEnrollment) {
        if (actor == null) {
            throw new AuthorizationException("A signed-in user is required to record attendance.");
        }
        if (request == null) {
            throw new ValidationException("Attendance session details are required.");
        }
        authorization.requireLecturerForOffering(actor, request.offeringId());
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
        Integer officerId = null;
        Integer lecturerId = authorization.lecturerId(actor);
        return transactions.execute(connection -> {
            long sessionId = attendanceDao.insertSession(connection,
                    new SessionRow(0, request.offeringId(), request.componentId(), request.date(),
                            request.timetableEntryId(), officerId, lecturerId));
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

    public void reviseRecord(AuthenticatedSession actor, AttendanceRow record) {
        if (actor == null) {
            throw new AuthorizationException("A signed-in user is required to revise attendance.");
        }
        if (record == null || record.sessionId() <= 0 || record.enrollmentId() <= 0) {
            throw new ValidationException("Attendance record details are invalid.");
        }
        SessionRow session = attendanceDao.findSessionsForSessionId(record.sessionId())
                .orElseThrow(() -> new ValidationException("Attendance session was not found."));
        if (actor.role() == Role.ADMIN) {
            authorization.requireAdmin(actor);
        } else {
            throw new AuthorizationException("Only an administrator may revise attendance.");
        }
        if (courseDao.findEnrollments(session.offeringId()).stream()
                .noneMatch(enrollment -> enrollment.id() == record.enrollmentId())) {
            throw new ValidationException("Attendance record does not belong to this offering.");
        }
        transactions.execute(connection -> {
            int updated = attendanceDao.reviseRecord(connection, record);
            if (updated != 1) {
                throw new ValidationException("Attendance record was not found.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "ATTENDANCE_RECORD_UPDATE", "ATTENDANCE_SESSION",
                    record.sessionId(), LocalDateTime.now(clock)));
            return null;
        });
    }

    public void deleteSession(AuthenticatedSession actor, int sessionId) {
        authorization.requireAdmin(actor);
        if (sessionId <= 0) {
            throw new ValidationException("Attendance session identifier is invalid.");
        }
        transactions.execute(connection -> {
            int deleted = attendanceDao.deleteSession(connection, sessionId);
            if (deleted != 1) {
                throw new ValidationException("Attendance session was not found.");
            }
            auditLogDao.insert(connection, new AuditLogDao.AuditEvent(
                    0, actor.userId(), "ATTENDANCE_SESSION_DELETE", "ATTENDANCE_SESSION",
                    sessionId, LocalDateTime.now(clock)));
            return null;
        });
    }

    public List<SessionRow> sessionsForOffering(AuthenticatedSession actor, int offeringId) {
        if (actor == null) {
            throw new AuthorizationException("Sign in to view attendance sessions.");
        }
        if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
        } else if (actor.role() == Role.TECHNICAL_OFFICER) {
            authorization.requireTechnicalOfficerForOffering(actor, offeringId);
        } else if (actor.role() != Role.ADMIN) {
            throw new AuthorizationException("You do not have permission to view attendance sessions.");
        }
        return attendanceDao.findSessions(offeringId);
    }

    public List<EnrollmentRow> activeEnrollments(AuthenticatedSession actor, int offeringId) {
        if (actor == null) {
            throw new AuthorizationException("Sign in to view course enrolments.");
        }
        if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
        } else if (actor.role() == Role.UNDERGRADUATE) {
            authorization.requireUndergraduateForOffering(actor, offeringId);
            int studentId = authorization.studentId(actor);
            return courseDao.findActiveEnrollments(offeringId).stream()
                    .filter(enrollment -> enrollment.studentId() == studentId).toList();
        } else if (actor.role() != Role.ADMIN) {
            throw new AuthorizationException("You do not have permission to view course enrolments.");
        }
        return courseDao.findActiveEnrollments(offeringId);
    }

    public AttendanceSummary calculateForCurrentStudent(AuthenticatedSession actor, int offeringId) {
        authorization.requireRole(actor, Role.UNDERGRADUATE);
        int studentId = authorization.studentId(actor);
        EnrollmentRow enrollment = courseDao.findActiveEnrollments(offeringId).stream()
                .filter(row -> row.studentId() == studentId)
                .findFirst().orElseThrow(() -> new AuthorizationException(
                        "You do not have an active enrolment in this offering."));
        return calculateForEnrollment(actor, studentId, enrollment.id(), offeringId);
    }

    public AttendanceSummary calculateForEnrollment(
            AuthenticatedSession actor, int studentId, int enrollmentId, int offeringId) {
        authorization.requireReportAccess(actor, ReportType.STUDENT_ATTENDANCE);
        if (actor.role() == Role.LECTURER) {
            authorization.requireLecturerForOffering(actor, offeringId);
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
