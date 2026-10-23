package lk.ac.ruhuna.fot.ams.business.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.ResultSnapshot;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.StudentResult;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService.ReportType;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

/** Read-only grade and GPA figures for screens. All numbers are produced here, to two decimal places. */
public final class GradesService {
    private static final int SCALE = 2;

    public record CourseResultView(int enrollmentId, String courseCode, String semester, int attempt,
                                   BigDecimal credits, BigDecimal finalMark, String grade, BigDecimal gradePoint) {
    }

    public record SemesterOption(int id, String label) {
    }

    /** Semester figures are null when no semester was requested; cgpa is always present. */
    public record GpaReport(String semester, int countedCourses, BigDecimal credits, BigDecimal gradePointTotal,
                            BigDecimal weightedTotal, BigDecimal sgpa, BigDecimal cgpa) {
    }

    private final CourseResultDao resultDao;
    private final CourseDao courseDao;
    private final GpaService gpaService;
    private final AuthorizationService authorization;

    public GradesService(CourseResultDao resultDao, CourseDao courseDao, GpaService gpaService,
                         AuthorizationService authorization) {
        this.resultDao = Objects.requireNonNull(resultDao);
        this.courseDao = Objects.requireNonNull(courseDao);
        this.gpaService = Objects.requireNonNull(gpaService);
        this.authorization = Objects.requireNonNull(authorization);
    }

    public List<CourseResultView> courseResults(AuthenticatedSession actor, Integer requestedStudentId) {
        int studentId = authorizeStudent(actor, requestedStudentId, ReportType.GRADES_COURSE_RESULTS);
        Map<Integer, String> semesters = semesterLabels();
        return resultDao.findByStudent(studentId).stream()
                .sorted(Comparator.comparingInt(StudentResult::semesterId)
                        .thenComparing(StudentResult::courseCode)
                        .thenComparingInt(StudentResult::attemptNumber))
                .map(result -> toView(result, semesters)).toList();
    }

    public List<SemesterOption> semesters(AuthenticatedSession actor, Integer requestedStudentId) {
        int studentId = authorizeStudent(actor, requestedStudentId, ReportType.SGPA_CGPA);
        Map<Integer, String> labels = semesterLabels();
        return resultDao.findByStudent(studentId).stream().map(StudentResult::semesterId).distinct().sorted()
                .map(id -> new SemesterOption(id, labels.getOrDefault(id, "Semester " + id))).toList();
    }

    public GpaReport gpaReport(AuthenticatedSession actor, Integer requestedStudentId, Integer semesterId) {
        int studentId = authorizeStudent(actor, requestedStudentId, ReportType.SGPA_CGPA);
        if (semesterId == null) {
            return new GpaReport(null, 0, null, null, null, null, round(gpaService.calculateCgpa(actor, studentId)));
        }
        List<StudentResult> rows = resultDao.findByStudentAndSemester(studentId, semesterId);
        if (rows.isEmpty()) {
            throw new BusinessRuleException("No completed results exist for this semester.");
        }
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal gradePoints = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (StudentResult row : rows) {
            credits = credits.add(row.creditsCounted());
            gradePoints = gradePoints.add(row.gradePoint());
            weighted = weighted.add(row.gradePoint().multiply(row.creditsCounted()));
        }
        return new GpaReport(semesterLabels().getOrDefault(semesterId, "Semester " + semesterId), rows.size(),
                round(credits), round(gradePoints), round(weighted),
                round(gpaService.calculateSgpa(actor, studentId, semesterId)),
                round(gpaService.calculateCgpa(actor, studentId)));
    }

    /** Undergraduates always see their own record; a client-supplied id is ignored. Lecturers need an in-scope student. */
    private int authorizeStudent(AuthenticatedSession actor, Integer requestedStudentId, ReportType reportType) {
        authorization.requireReportAccess(actor, reportType);
        if (actor.role() == Role.UNDERGRADUATE) {
            return authorization.studentId(actor);
        }
        if (actor.role() == Role.LECTURER) {
            if (requestedStudentId == null) {
                throw new ValidationException("Select a student first.");
            }
            authorization.requireLecturerForStudent(actor, requestedStudentId);
            return requestedStudentId;
        }
        throw new AuthorizationException("You do not have permission to view these results.");
    }

    private CourseResultView toView(StudentResult result, Map<Integer, String> semesters) {
        ResultSnapshot snapshot = resultDao.findByEnrollment(result.enrollmentId()).orElse(null);
        return new CourseResultView(result.enrollmentId(), result.courseCode(),
                semesters.getOrDefault(result.semesterId(), "Semester " + result.semesterId()),
                result.attemptNumber(), round(result.creditsCounted()),
                snapshot == null ? null : round(snapshot.finalMark()),
                snapshot == null ? "-" : snapshot.gradeLetter(), round(result.gradePoint()));
    }

    private Map<Integer, String> semesterLabels() {
        Map<Integer, String> labels = new HashMap<>();
        courseDao.findSemesters().forEach(semester -> labels.put(semester.id(),
                semester.academicYear() + " Semester " + semester.semesterNumber()));
        return labels;
    }

    private static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
