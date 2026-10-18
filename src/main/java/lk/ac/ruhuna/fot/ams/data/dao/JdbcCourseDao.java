package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.data.connection.JdbcExecutor;
import lk.ac.ruhuna.fot.ams.domain.enums.ComponentType;
import lk.ac.ruhuna.fot.ams.error.exception.DataAccessException;

public final class JdbcCourseDao implements CourseDao {
    private final JdbcExecutor jdbc;

    public JdbcCourseDao(JdbcExecutor jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long insertDepartment(Connection connection, DepartmentRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO departments (code, name, office_location, is_active) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, row.code());
                    statement.setString(2, row.name());
                    statement.setString(3, row.officeLocation());
                    statement.setBoolean(4, row.active());
                });
    }

    @Override
    public long insertBatch(Connection connection, BatchRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO batches (department_id, intake_year, programme, batch_code) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.departmentId());
                    statement.setInt(2, row.intakeYear());
                    statement.setString(3, row.programme());
                    statement.setString(4, row.code());
                });
    }

    @Override
    public long insertCourse(Connection connection, CourseRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO courses (department_id, course_code, title, total_credits, is_active) VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.departmentId());
                    statement.setString(2, row.code());
                    statement.setString(3, row.title());
                    statement.setDouble(4, row.credits());
                    statement.setBoolean(5, row.active());
                });
    }

    @Override
    public long insertComponent(Connection connection, ComponentRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_components (course_id, component_type, credits, planned_sessions) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.courseId());
                    statement.setString(2, row.type().name());
                    statement.setDouble(3, row.credits());
                    statement.setInt(4, row.plannedSessions());
                });
    }

    @Override
    public long insertSemester(Connection connection, SemesterRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO semesters (academic_year, semester_no, start_date, end_date) VALUES (?, ?, ?, ?)",
                statement -> {
                    statement.setString(1, row.academicYear());
                    statement.setInt(2, row.semesterNumber());
                    statement.setDate(3, Date.valueOf(row.start()));
                    statement.setDate(4, Date.valueOf(row.end()));
                });
    }

    @Override
    public long insertOffering(Connection connection, OfferingRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_offerings (course_id, batch_id, semester_id, coordinator_id, status) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.courseId());
                    statement.setInt(2, row.batchId());
                    statement.setInt(3, row.semesterId());
                    if (row.coordinatorId() == null) {
                        statement.setNull(4, java.sql.Types.INTEGER);
                    } else {
                        statement.setInt(4, row.coordinatorId());
                    }
                    statement.setString(5, row.status());
                });
    }

    @Override
    public int assignLecturer(Connection connection, int offeringId, int lecturerId) {
        return jdbc.update(connection,
                "INSERT INTO course_offering_lecturers (offering_id, lecturer_id) VALUES (?, ?)",
                statement -> {
                    statement.setInt(1, offeringId);
                    statement.setInt(2, lecturerId);
                });
    }

    @Override
    public long insertEnrollment(Connection connection, EnrollmentRow row) {
        return jdbc.insertAndReturnKey(connection,
                "INSERT INTO course_enrollments "
                        + "(student_id, offering_id, attempt_no, student_status, enrollment_status) "
                        + "VALUES (?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, row.studentId());
                    statement.setInt(2, row.offeringId());
                    statement.setInt(3, row.attemptNumber());
                    statement.setString(4, row.studentStatus());
                    statement.setString(5, row.enrollmentStatus());
                });
    }

    @Override
    public int updateEnrollmentStatus(Connection connection, int enrollmentId, String status) {
        return jdbc.update(connection,
                "UPDATE course_enrollments SET enrollment_status = ? WHERE enrollment_id = ?",
                statement -> {
                    statement.setString(1, status);
                    statement.setInt(2, enrollmentId);
                });
    }

    @Override
    public int updateCourse(Connection connection, CourseRow row) {
        return jdbc.update(connection,
                "UPDATE courses SET department_id = ?, course_code = ?, title = ?, total_credits = ?, is_active = ? "
                        + "WHERE course_id = ?",
                statement -> {
                    statement.setInt(1, row.departmentId());
                    statement.setString(2, row.code());
                    statement.setString(3, row.title());
                    statement.setDouble(4, row.credits());
                    statement.setBoolean(5, row.active());
                    statement.setInt(6, row.id());
                });
    }

    @Override
    public int updateDepartmentActive(Connection connection, int departmentId, boolean active) {
        return jdbc.update(connection, "UPDATE departments SET is_active = ? WHERE department_id = ?",
                statement -> {
                    statement.setBoolean(1, active);
                    statement.setInt(2, departmentId);
                });
    }

    @Override
    public int updateOfferingStatus(Connection connection, int offeringId, String status) {
        return jdbc.update(connection, "UPDATE course_offerings SET status = ? WHERE offering_id = ?",
                statement -> {
                    statement.setString(1, status);
                    statement.setInt(2, offeringId);
                });
    }

    @Override
    public int unassignLecturer(Connection connection, int offeringId, int lecturerId) {
        return jdbc.update(connection,
                "DELETE FROM course_offering_lecturers WHERE offering_id = ? AND lecturer_id = ?",
                statement -> {
                    statement.setInt(1, offeringId);
                    statement.setInt(2, lecturerId);
                });
    }

    @Override
    public List<DepartmentRow> findDepartments(boolean activeOnly) {
        return jdbc.query("SELECT department_id, code, name, office_location, is_active FROM departments "
                        + "WHERE (? = FALSE OR is_active = TRUE) ORDER BY code",
                statement -> statement.setBoolean(1, activeOnly),
                result -> new DepartmentRow(result.getInt("department_id"), result.getString("code"),
                        result.getString("name"), result.getString("office_location"),
                        result.getBoolean("is_active")));
    }

    @Override
    public List<BatchRow> findBatches(int departmentId) {
        return jdbc.query("SELECT batch_id, department_id, intake_year, programme, batch_code "
                        + "FROM batches WHERE department_id = ? ORDER BY intake_year DESC, batch_code",
                statement -> statement.setInt(1, departmentId),
                result -> new BatchRow(result.getInt("batch_id"), result.getInt("department_id"),
                        result.getInt("intake_year"), result.getString("programme"),
                        result.getString("batch_code")));
    }

    @Override
    public List<SemesterRow> findSemesters() {
        return jdbc.query("SELECT semester_id, academic_year, semester_no, start_date, end_date "
                        + "FROM semesters ORDER BY academic_year DESC, semester_no DESC",
                statement -> {
                }, result -> new SemesterRow(result.getInt("semester_id"),
                        result.getString("academic_year"), result.getInt("semester_no"),
                        result.getDate("start_date").toLocalDate(), result.getDate("end_date").toLocalDate()));
    }

    @Override
    public Optional<CourseRow> findCourseByCode(String courseCode) {
        return jdbc.query("SELECT course_id, department_id, course_code, title, total_credits, is_active "
                        + "FROM courses WHERE course_code = ?",
                statement -> statement.setString(1, courseCode), this::mapCourse).stream().findFirst();
    }

    @Override
    public Optional<CourseRow> findCourse(int courseId) {
        return jdbc.query("SELECT course_id, department_id, course_code, title, total_credits, is_active "
                        + "FROM courses WHERE course_id = ?",
                statement -> statement.setInt(1, courseId), this::mapCourse).stream().findFirst();
    }

    @Override
    public Optional<EnrollmentRow> findEnrollment(int enrollmentId) {
        return jdbc.query("SELECT enrollment_id, student_id, offering_id, attempt_no, "
                        + "student_status, enrollment_status FROM course_enrollments WHERE enrollment_id = ?",
                statement -> statement.setInt(1, enrollmentId), this::mapEnrollment).stream().findFirst();
    }

    @Override
    public Optional<OfferingRow> findOffering(int offeringId) {
        return jdbc.query("SELECT offering_id, course_id, batch_id, semester_id, coordinator_id, status "
                        + "FROM course_offerings WHERE offering_id = ?",
                statement -> statement.setInt(1, offeringId), this::mapOffering).stream().findFirst();
    }

    @Override
    public List<OfferingRow> findOfferings(Integer batchId, Integer semesterId) {
        return jdbc.query("SELECT offering_id, course_id, batch_id, semester_id, coordinator_id, status "
                        + "FROM course_offerings WHERE (? IS NULL OR batch_id = ?) "
                        + "AND (? IS NULL OR semester_id = ?) ORDER BY semester_id, batch_id, course_id",
                statement -> {
                    setOptionalInt(statement, 1, 2, batchId);
                    setOptionalInt(statement, 3, 4, semesterId);
                }, this::mapOffering);
    }

    @Override
    public List<CourseRow> searchCourses(String term, Integer departmentId, boolean activeOnly) {
        String sql = "SELECT course_id, department_id, course_code, title, total_credits, is_active FROM courses "
                + "WHERE (? IS NULL OR department_id = ?) AND (? = FALSE OR is_active = TRUE) "
                + "AND (? IS NULL OR course_code LIKE ? OR title LIKE ?) ORDER BY course_code";
        return jdbc.query(sql, statement -> {
            if (departmentId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, departmentId);
                statement.setInt(2, departmentId);
            }
            statement.setBoolean(3, activeOnly);
            if (term == null || term.isBlank()) {
                statement.setNull(4, java.sql.Types.VARCHAR);
                statement.setNull(5, java.sql.Types.VARCHAR);
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                String pattern = "%" + term.trim() + "%";
                statement.setString(4, pattern);
                statement.setString(5, pattern);
                statement.setString(6, pattern);
            }
        }, this::mapCourse);
    }

    @Override
    public List<ComponentRow> findComponents(int courseId) {
        return jdbc.query("SELECT component_id, course_id, component_type, credits, planned_sessions "
                        + "FROM course_components WHERE course_id = ? ORDER BY component_type",
                statement -> statement.setInt(1, courseId), this::mapComponent);
    }

    @Override
    public List<OfferingRow> findOfferingsForLecturer(int lecturerId, Integer semesterId) {
        String sql = "SELECT DISTINCT o.offering_id, o.course_id, o.batch_id, o.semester_id, "
                + "o.coordinator_id, o.status FROM course_offerings o "
                + "LEFT JOIN course_offering_lecturers a ON a.offering_id = o.offering_id "
                + "WHERE (o.coordinator_id = ? OR a.lecturer_id = ?) "
                + "AND (? IS NULL OR o.semester_id = ?) ORDER BY o.semester_id, o.offering_id";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, lecturerId);
            statement.setInt(2, lecturerId);
            if (semesterId == null) {
                statement.setNull(3, java.sql.Types.INTEGER);
                statement.setNull(4, java.sql.Types.INTEGER);
            } else {
                statement.setInt(3, semesterId);
                statement.setInt(4, semesterId);
            }
        }, this::mapOffering);
    }

    @Override
    public List<Integer> findLecturersForOffering(int offeringId) {
        String sql = "SELECT lecturer_id FROM course_offering_lecturers WHERE offering_id = ? "
                + "UNION SELECT coordinator_id FROM course_offerings "
                + "WHERE offering_id = ? AND coordinator_id IS NOT NULL ORDER BY lecturer_id";
        return jdbc.query(sql, statement -> {
            statement.setInt(1, offeringId);
            statement.setInt(2, offeringId);
        }, result -> result.getInt(1));
    }

    @Override
    public List<EnrollmentRow> findActiveEnrollments(int offeringId) {
        return jdbc.query("SELECT enrollment_id, student_id, offering_id, attempt_no, "
                        + "student_status, enrollment_status FROM course_enrollments "
                        + "WHERE offering_id = ? AND enrollment_status = 'ACTIVE' ORDER BY student_id",
                statement -> statement.setInt(1, offeringId), this::mapEnrollment);
    }

    @Override
    public List<EnrollmentRow> findEnrollments(int offeringId) {
        return jdbc.query("SELECT enrollment_id, student_id, offering_id, attempt_no, "
                        + "student_status, enrollment_status FROM course_enrollments "
                        + "WHERE offering_id = ? ORDER BY student_id, attempt_no",
                statement -> statement.setInt(1, offeringId), this::mapEnrollment);
    }

    @Override
    public boolean hasActiveEnrollment(int studentId, int offeringId) {
        return !jdbc.query("SELECT enrollment_id FROM course_enrollments "
                        + "WHERE student_id = ? AND offering_id = ? AND enrollment_status = 'ACTIVE' LIMIT 1",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, offeringId);
                }, result -> result.getInt(1)).isEmpty();
    }

    private CourseRow mapCourse(ResultSet result) throws SQLException {
        return new CourseRow(result.getInt("course_id"), result.getInt("department_id"),
                result.getString("course_code"), result.getString("title"),
                result.getDouble("total_credits"), result.getBoolean("is_active"));
    }

    private ComponentRow mapComponent(ResultSet result) throws SQLException {
        return new ComponentRow(result.getInt("component_id"), result.getInt("course_id"),
                ComponentType.valueOf(result.getString("component_type")),
                result.getDouble("credits"), result.getInt("planned_sessions"));
    }

    private OfferingRow mapOffering(ResultSet result) throws SQLException {
        int coordinator = result.getInt("coordinator_id");
        Integer coordinatorId = result.wasNull() ? null : coordinator;
        return new OfferingRow(result.getInt("offering_id"), result.getInt("course_id"),
                result.getInt("batch_id"), result.getInt("semester_id"), coordinatorId,
                result.getString("status"));
    }

    private EnrollmentRow mapEnrollment(ResultSet result) throws SQLException {
        return new EnrollmentRow(result.getInt("enrollment_id"), result.getInt("student_id"),
                result.getInt("offering_id"), result.getInt("attempt_no"),
                result.getString("student_status"), result.getString("enrollment_status"));
    }

    private void setOptionalInt(java.sql.PreparedStatement statement, int first, int second, Integer value)
            throws SQLException {
        if (value == null) {
            statement.setNull(first, java.sql.Types.INTEGER);
            statement.setNull(second, java.sql.Types.INTEGER);
        } else {
            statement.setInt(first, value);
            statement.setInt(second, value);
        }
    }
}
