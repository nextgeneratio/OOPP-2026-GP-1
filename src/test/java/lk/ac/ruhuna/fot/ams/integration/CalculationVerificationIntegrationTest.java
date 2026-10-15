package lk.ac.ruhuna.fot.ams.integration;

import lk.ac.ruhuna.fot.ams.business.calculator.AcademicCalculator;
import lk.ac.ruhuna.fot.ams.business.service.EligibilityService;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationVerificationIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("5.4 - Verify calculation against persisted database schema and course offerings")
    void verifyAcademicCalculationsWithDatabaseState() throws SQLException {
        EligibilityService eligibilityService = new EligibilityService();

        try (Connection conn = connectionProvider.getConnection()) {
            // Read course details from database
            String courseCode;
            double credits;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT course_code, total_credits FROM courses WHERE course_id = 1")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    courseCode = rs.getString("course_code");
                    credits = rs.getDouble("total_credits");
                }
            }
            assertThat(courseCode).isEqualTo("ICT2132");
            assertThat(credits).isEqualTo(3.0);

            // Verify STU001 Attendance scenario using database-backed configuration
            EligibilityService.AttendanceComponentInput theory =
                    new EligibilityService.AttendanceComponentInput(13, 15, 0);
            EligibilityService.AttendanceComponentInput practical =
                    new EligibilityService.AttendanceComponentInput(14, 15, 0);

            EligibilityService.CourseEligibilityResult eligibility =
                    eligibilityService.evaluateCourseEligibility(List.of(theory, practical), 65.0);

            assertThat(eligibility.attendanceEligible()).isTrue();
            assertThat(eligibility.caEligible()).isTrue();
            assertThat(eligibility.finalExamEligible()).isTrue();
            assertThat(eligibility.overallAttendancePercentage()).isEqualTo(90.00);

            // Verify SGPA calculation with persisted courses
            List<AcademicCalculator.CourseResult> semesterResults = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT total_credits FROM courses WHERE course_id IN (1, 2) ORDER BY course_id")) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        double cr = rs.getDouble("total_credits");
                        semesterResults.add(new AcademicCalculator.CourseResult(cr, GradeScheme.A)); // 4.0 GP
                    }
                }
            }

            assertThat(semesterResults).hasSize(2);
            double calculatedGpa = AcademicCalculator.calculateGpa(semesterResults);
            assertThat(calculatedGpa).isEqualTo(4.00);
        }
    }
}
