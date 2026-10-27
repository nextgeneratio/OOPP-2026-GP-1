package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator.RepeatSelectionRule;
import lk.ac.ruhuna.fot.ams.business.service.GpaService;
import lk.ac.ruhuna.fot.ams.business.service.GradesService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.StudentResult;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

/** Uses the real authorization and GPA services; only the data access interfaces are mocked. */
class GradesServiceTest {
    private static final LocalDateTime DONE = LocalDateTime.of(2026, 5, 1, 10, 0);

    private final CourseResultDao resultDao = mock(CourseResultDao.class);
    private final CourseDao courseDao = mock(CourseDao.class);
    private final AuthorizationScopeLookup scope = mock(AuthorizationScopeLookup.class);
    private final AuthorizationService authorization = new AuthorizationService(scope);
    private final GpaService gpaService = new GpaService(new GpaCalculator(),
            RepeatSelectionRule.LATEST_COMPLETED_ATTEMPT, resultDao, courseDao, authorization);
    private final GradesService service = new GradesService(resultDao, courseDao, gpaService, authorization);

    private final AuthenticatedSession student = new AuthenticatedSession(10L, "STU001", Role.UNDERGRADUATE, "Student");
    private final AuthenticatedSession lecturer = new AuthenticatedSession(11L, "LEC001", Role.LECTURER, "Lecturer");

    private static StudentResult result(int enrollment, String code, String credits, String gradePoint) {
        return new StudentResult(enrollment, 7, code, 1, 1, new BigDecimal(credits), new BigDecimal(gradePoint), DONE);
    }

    @Test
    void undergraduateAlwaysReadsOwnRecordWhateverIdIsSupplied() {
        when(scope.studentIdForUser(anyLong())).thenReturn(OptionalInt.of(7));
        when(resultDao.findByStudent(7)).thenReturn(List.of());

        assertThat(service.courseResults(student, 99)).isEmpty();

        verify(resultDao).findByStudent(7);
        verify(resultDao, never()).findByStudent(99);
    }

    @Test
    void gpaReportTotalsCreditsGradePointsAndWeightedTotalToTwoDecimals() {
        List<StudentResult> rows = List.of(result(1, "TICT1113", "3.0", "4.0"), result(2, "TICT1122", "2.0", "3.0"));
        when(scope.studentIdForUser(anyLong())).thenReturn(OptionalInt.of(7));
        when(scope.undergraduateOwnsStudent(anyLong(), eq(7))).thenReturn(true);
        when(resultDao.findByStudentAndSemester(7, 1)).thenReturn(rows);
        when(resultDao.findByStudent(7)).thenReturn(rows);

        var report = service.gpaReport(student, null, 1);

        assertThat(report.countedCourses()).isEqualTo(2);
        assertThat(report.credits()).isEqualByComparingTo("5.00");
        assertThat(report.gradePointTotal()).isEqualByComparingTo("7.00");
        assertThat(report.weightedTotal()).isEqualByComparingTo("18.00");
        assertThat(report.sgpa()).isEqualByComparingTo("3.60");
        assertThat(report.cgpa().scale()).isEqualTo(2);
    }

    @Test
    void lecturerCannotReadAStudentOutsideTheirScope() {
        when(scope.lecturerAssignedToStudent(anyLong(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.courseResults(lecturer, 99)).isInstanceOf(AuthorizationException.class);
    }
}
