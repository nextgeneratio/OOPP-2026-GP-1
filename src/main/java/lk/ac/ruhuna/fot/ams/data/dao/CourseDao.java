package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;

public interface CourseDao {
    record DepartmentRow(int id, String code, String name, String officeLocation, boolean active) {
    }

    record BatchRow(int id, int departmentId, int intakeYear, String programme, String code) {
    }

    record CourseRow(int id, int departmentId, String code, String title, double credits, boolean active) {
    }

    record ComponentRow(int id, int courseId, ComponentType type, double credits, int plannedSessions) {
    }

    record SemesterRow(int id, String academicYear, int semesterNumber, java.time.LocalDate start, java.time.LocalDate end) {
    }

    record OfferingRow(
            int id, int courseId, int batchId, int semesterId, Integer coordinatorId, String status) {
    }

    record EnrollmentRow(
            int id, int studentId, int offeringId, int attemptNumber, String studentStatus, String enrollmentStatus) {
    }

    record RosterRow(int enrollmentId, int studentId, String studentNumber, String fullName) {
    }

    record OfferingSummary(
            int offeringId, int courseId, String courseCode, String courseTitle,
            int departmentId, int semesterId) {
    }

    /** Active enrolled students with display identity. Default keeps existing implementations compiling. */
    default List<RosterRow> findRoster(int offeringId) {
        throw new UnsupportedOperationException("findRoster is not implemented");
    }

    /** Offerings a lecturer teaches, with course code and title for selectors. */
    default List<OfferingSummary> findOfferingSummariesForLecturer(int lecturerId) {
        throw new UnsupportedOperationException("findOfferingSummariesForLecturer is not implemented");
    }

    /** Current-semester offerings in a technical officer's department, for attendance session selectors. */
    default List<OfferingSummary> findOfferingSummariesForTechnicalOfficer(int technicalOfficerId) {
        throw new UnsupportedOperationException("findOfferingSummariesForTechnicalOfficer is not implemented");
    }

    /** A student's own active enrolments, across every offering. */
    default List<EnrollmentRow> findEnrollmentsForStudent(int studentId) {
        throw new UnsupportedOperationException("findEnrollmentsForStudent is not implemented");
    }

    long insertDepartment(Connection connection, DepartmentRow row);

    long insertBatch(Connection connection, BatchRow row);

    long insertCourse(Connection connection, CourseRow row);

    long insertComponent(Connection connection, ComponentRow row);

    long insertSemester(Connection connection, SemesterRow row);

    long insertOffering(Connection connection, OfferingRow row);

    int assignLecturer(Connection connection, int offeringId, int lecturerId);

    long insertEnrollment(Connection connection, EnrollmentRow row);

    int updateEnrollmentStatus(Connection connection, int enrollmentId, String status);

    int updateCourse(Connection connection, CourseRow row);

    int updateDepartmentActive(Connection connection, int departmentId, boolean active);

    int updateOfferingStatus(Connection connection, int offeringId, String status);

    int unassignLecturer(Connection connection, int offeringId, int lecturerId);

    List<DepartmentRow> findDepartments(boolean activeOnly);

    List<BatchRow> findBatches(int departmentId);

    List<SemesterRow> findSemesters();

    Optional<CourseRow> findCourseByCode(String courseCode);

    Optional<CourseRow> findCourse(int courseId);

    Optional<EnrollmentRow> findEnrollment(int enrollmentId);

    Optional<OfferingRow> findOffering(int offeringId);

    List<OfferingRow> findOfferings(Integer batchId, Integer semesterId);

    List<CourseRow> searchCourses(String term, Integer departmentId, boolean activeOnly);

    List<ComponentRow> findComponents(int courseId);

    List<OfferingRow> findOfferingsForLecturer(int lecturerId, Integer semesterId);

    List<Integer> findLecturersForOffering(int offeringId);

    List<EnrollmentRow> findActiveEnrollments(int offeringId);

    List<EnrollmentRow> findEnrollments(int offeringId);

    boolean hasActiveEnrollment(int studentId, int offeringId);
}
