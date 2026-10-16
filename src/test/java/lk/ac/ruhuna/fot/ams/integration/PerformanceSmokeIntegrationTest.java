package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator;
import lk.ac.ruhuna.fot.ams.business.service.AuthService;
import lk.ac.ruhuna.fot.ams.domain.model.Admin;
import lk.ac.ruhuna.fot.ams.domain.model.User;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.security.password.PasswordHasher;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceSmokeIntegrationTest extends BaseIntegrationTest {

    private static final long MAX_ALLOWED_MILLIS = 3000L; // 3 seconds threshold

    @Test
    @DisplayName("7.1.1 - Performance Smoke: Authentication / Login response time (< 3 seconds)")
    void testAuthenticationPerformance() {
        PasswordHasher hasher = new PasswordHasher();
        String hashedPassword = hasher.hash("admin123".toCharArray());
        User adminUser = new Admin(1L, "admin", hashedPassword, "admin@fot.ruh.ac.lk", true);

        AuthService authService = new AuthService(
                username -> "admin".equals(username) ? Optional.of(adminUser) : Optional.empty(),
                hasher,
                log -> {}
        );

        Instant start = Instant.now();
        AuthenticatedSession result = authService.authenticate("admin", "admin123".toCharArray());
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("admin");
        assertThat(elapsed.toMillis())
                .withFailMessage("Authentication took %d ms, exceeding limit of %d ms", elapsed.toMillis(), MAX_ALLOWED_MILLIS)
                .isLessThan(MAX_ALLOWED_MILLIS);
    }

    @Test
    @DisplayName("7.1.2 - Performance Smoke: Student list query response time (< 3 seconds)")
    void testStudentListQueryPerformance() throws SQLException {
        Instant start = Instant.now();
        List<String> studentNos = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT u.user_id, u.email, ug.student_no, ug.full_name, b.batch_code " +
                     "FROM users u " +
                     "JOIN undergraduates ug ON u.user_id = ug.user_id " +
                     "JOIN batches b ON ug.batch_id = b.batch_id " +
                     "WHERE u.role = 'UNDERGRADUATE'")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    studentNos.add(rs.getString("student_no"));
                }
            }
        }
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(studentNos).isNotEmpty();
        assertThat(elapsed.toMillis())
                .withFailMessage("Student list query took %d ms, exceeding limit of %d ms", elapsed.toMillis(), MAX_ALLOWED_MILLIS)
                .isLessThan(MAX_ALLOWED_MILLIS);
    }

    @Test
    @DisplayName("7.1.3 - Performance Smoke: Marks summary list response time (< 3 seconds)")
    void testMarksSummaryQueryPerformance() throws SQLException {
        Instant start = Instant.now();
        int recordCount = 0;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT m.mark_id, ce.enrollment_id, ug.full_name, c.course_code, a.name AS assessment_name, m.score " +
                     "FROM marks m " +
                     "JOIN course_enrollments ce ON m.enrollment_id = ce.enrollment_id " +
                     "JOIN undergraduates ug ON ce.student_id = ug.student_id " +
                     "JOIN assessments a ON m.assessment_id = a.assessment_id " +
                     "JOIN course_offerings co ON a.offering_id = co.offering_id " +
                     "JOIN courses c ON co.course_id = c.course_id")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recordCount++;
                }
            }
        }
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed.toMillis())
                .withFailMessage("Marks summary query took %d ms, exceeding limit of %d ms", elapsed.toMillis(), MAX_ALLOWED_MILLIS)
                .isLessThan(MAX_ALLOWED_MILLIS);
    }

    @Test
    @DisplayName("7.1.4 - Performance Smoke: Attendance sheet view response time (< 3 seconds)")
    void testAttendanceSheetViewPerformance() throws SQLException {
        Instant start = Instant.now();
        int attendanceRows = 0;

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT ar.attendance_id, ass.session_date, ug.student_no, ug.full_name, ar.is_present " +
                     "FROM attendance_records ar " +
                     "JOIN attendance_sessions ass ON ar.session_id = ass.session_id " +
                     "JOIN course_enrollments ce ON ar.enrollment_id = ce.enrollment_id " +
                     "JOIN undergraduates ug ON ce.student_id = ug.student_id " +
                     "ORDER BY ass.session_date DESC")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attendanceRows++;
                }
            }
        }
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(elapsed.toMillis())
                .withFailMessage("Attendance sheet view took %d ms, exceeding limit of %d ms", elapsed.toMillis(), MAX_ALLOWED_MILLIS)
                .isLessThan(MAX_ALLOWED_MILLIS);
    }

    @Test
    @DisplayName("7.1.5 - Performance Smoke: Student grade report / result view generation time (< 3 seconds)")
    void testGradeReportGenerationPerformance() throws SQLException {
        Instant start = Instant.now();

        List<AcademicCalculator.CourseResult> results = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT c.total_credits FROM courses c LIMIT 10")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new AcademicCalculator.CourseResult(rs.getDouble("total_credits"), GradeScheme.B_PLUS));
                }
            }
        }

        double calculatedGpa = AcademicCalculator.calculateGpa(results);
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(calculatedGpa).isGreaterThan(0.0);
        assertThat(elapsed.toMillis())
                .withFailMessage("Grade report generation took %d ms, exceeding limit of %d ms", elapsed.toMillis(), MAX_ALLOWED_MILLIS)
                .isLessThan(MAX_ALLOWED_MILLIS);
    }
}
