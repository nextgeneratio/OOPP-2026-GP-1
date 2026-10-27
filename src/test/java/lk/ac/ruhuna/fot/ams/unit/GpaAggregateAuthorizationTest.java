package lk.ac.ruhuna.fot.ams.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;
import lk.ac.ruhuna.fot.ams.business.calculator.GpaCalculator;
import lk.ac.ruhuna.fot.ams.business.service.GpaService;
import lk.ac.ruhuna.fot.ams.data.dao.CourseDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao;
import lk.ac.ruhuna.fot.ams.data.dao.CourseResultDao.StudentResult;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.AuthorizationException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationScopeLookup;
import lk.ac.ruhuna.fot.ams.security.authorization.AuthorizationService;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.Test;

class GpaAggregateAuthorizationTest {
    @Test
    void lecturerCanViewAggregateAfterSelectingAnAssignedOfferingWithoutDetailedCrossCourseResults() {
        CourseResultDao results = mock(CourseResultDao.class);
        CourseDao courses = mock(CourseDao.class);
        when(courses.findEnrollments(10)).thenReturn(List.of(new CourseDao.EnrollmentRow(
                1, 42, 10, 1, "NORMAL", "COMPLETED")));
        when(results.findByStudentAndSemester(42, 3)).thenReturn(List.of(
                result(1, "ICT101", "4.00"), result(2, "ICT102", "3.00")));
        GpaService service = service(results, courses);

        assertThat(service.calculateLecturerSgpa(lecturer(), 42, 10, 3))
                .isEqualByComparingTo("3.50");
    }

    @Test
    void lecturerAggregateRequiresStudentInTheSelectedAssignedOffering() {
        CourseResultDao results = mock(CourseResultDao.class);
        CourseDao courses = mock(CourseDao.class);
        when(courses.findEnrollments(10)).thenReturn(List.of());

        assertThatThrownBy(() -> service(results, courses).calculateLecturerCgpa(lecturer(), 42, 10))
                .isInstanceOf(AuthorizationException.class);
        verifyNoInteractions(results);
    }

    @Test
    void unassignedLecturerCannotQueryAggregateResults() {
        CourseResultDao results = mock(CourseResultDao.class);
        CourseDao courses = mock(CourseDao.class);

        assertThatThrownBy(() -> service(results, courses).calculateLecturerSgpa(lecturer(), 42, 11, 3))
                .isInstanceOf(AuthorizationException.class);

        verifyNoInteractions(courses, results);
    }

    @Test
    void lecturerCgpaReturnsOnlyTheAggregateForMultipleCompletedCourses() {
        CourseResultDao results = mock(CourseResultDao.class);
        CourseDao courses = assignedOfferingForStudent();
        when(results.findByStudent(42)).thenReturn(List.of(
                result(1, "ICT101", "4.00"), result(2, "ICT102", "3.00")));

        BigDecimal cgpa = service(results, courses).calculateLecturerCgpa(lecturer(), 42, 10);

        assertThat(cgpa).isEqualByComparingTo("3.50");
    }

    @Test
    void undergraduateCanCalculateOnlyTheirOwnAggregate() {
        CourseResultDao results = mock(CourseResultDao.class);
        when(results.findByStudentAndSemester(42, 3)).thenReturn(List.of(result(1, "ICT101", "4.00")));
        GpaService service = service(results, mock(CourseDao.class));

        assertThat(service.calculateOwnSgpa(student(), 3)).isEqualByComparingTo("4.00");
        assertThatThrownBy(() -> service.calculateSgpa(student(), 7, 3))
                .isInstanceOf(AuthorizationException.class);
    }

    @Test
    void undergraduateCannotUseLecturerSpecificAggregateMethods() {
        CourseResultDao results = mock(CourseResultDao.class);
        CourseDao courses = mock(CourseDao.class);
        GpaService service = service(results, courses);

        assertThatThrownBy(() -> service.calculateLecturerSgpa(student(), 42, 10, 3))
                .isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> service.calculateLecturerCgpa(student(), 42, 10))
                .isInstanceOf(AuthorizationException.class);

        verifyNoInteractions(courses, results);
    }

    @Test
    void aggregateReportsAUsefulMessageWhenThereAreNoCompletedResults() {
        CourseDao courses = mock(CourseDao.class);
        when(courses.findEnrollments(10)).thenReturn(List.of(new CourseDao.EnrollmentRow(
                1, 42, 10, 1, "NORMAL", "COMPLETED")));

        assertThatThrownBy(() -> service(mock(CourseResultDao.class), courses)
                .calculateLecturerCgpa(lecturer(), 42, 10))
                .isInstanceOf(ValidationException.class)
                .hasMessage("GPA cannot be calculated without completed course credits.");
    }

    private GpaService service(CourseResultDao results, CourseDao courses) {
        return new GpaService(new GpaCalculator(), GpaCalculator.RepeatSelectionRule.LATEST_COMPLETED_ATTEMPT,
                results, courses, new AuthorizationService(new Scope()));
    }

    private CourseDao assignedOfferingForStudent() {
        CourseDao courses = mock(CourseDao.class);
        when(courses.findEnrollments(10)).thenReturn(List.of(new CourseDao.EnrollmentRow(
                1, 42, 10, 1, "NORMAL", "COMPLETED")));
        return courses;
    }

    private StudentResult result(int enrollment, String code, String point) {
        return new StudentResult(enrollment, 42, code, 3, 1, BigDecimal.valueOf(3),
                new BigDecimal(point), LocalDateTime.of(2026, 6, 1, 9, 0));
    }

    private AuthenticatedSession lecturer() {
        return new AuthenticatedSession(1, "lecturer", Role.LECTURER, "Lecturer");
    }

    private AuthenticatedSession student() {
        return new AuthenticatedSession(2, "student", Role.UNDERGRADUATE, "Student");
    }

    private static final class Scope implements AuthorizationScopeLookup {
        @Override public boolean lecturerAssignedToOffering(long userId, int offeringId) { return offeringId == 10; }
        @Override public boolean lecturerAssignedToStudent(long userId, int studentId) { return true; }
        @Override public boolean technicalOfficerAuthorizedForOffering(long userId, int offeringId) { return false; }
        @Override public boolean technicalOfficerAuthorizedForStudent(long userId, int studentId) { return false; }
        @Override public boolean undergraduateOwnsStudent(long userId, int studentId) {
            return userId == 2 && studentId == 42;
        }
        @Override public boolean undergraduateEnrolledInOffering(long userId, int offeringId) { return false; }
        @Override public OptionalInt lecturerIdForUser(long userId) { return OptionalInt.of(1); }
        @Override public OptionalInt technicalOfficerIdForUser(long userId) { return OptionalInt.empty(); }
        @Override public OptionalInt studentIdForUser(long userId) {
            return userId == 2 ? OptionalInt.of(42) : OptionalInt.empty();
        }
        @Override public OptionalInt studentBatchIdForUser(long userId) { return OptionalInt.empty(); }
    }
}
