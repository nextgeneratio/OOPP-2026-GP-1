package lk.ac.ruhuna.fot.ams.data.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseResultDao {
    record ResultSnapshot(
            int enrollmentId,
            BigDecimal finalMark,
            String schemeVersion,
            String gradeLetter,
            BigDecimal gradePoint,
            BigDecimal creditsCounted,
            LocalDateTime completedAt,
            int recordedBy) {
    }

    record StudentResult(
            int enrollmentId,
            int studentId,
            String courseCode,
            int semesterId,
            int attemptNumber,
            BigDecimal creditsCounted,
            BigDecimal gradePoint,
            LocalDateTime completedAt) {
    }

    int insert(Connection connection, ResultSnapshot snapshot);

    int archiveCurrent(Connection connection, int enrollmentId);

    Optional<ResultSnapshot> findByEnrollment(int enrollmentId);

    List<StudentResult> findByStudent(int studentId);

    List<StudentResult> findByStudentAndSemester(int studentId, int semesterId);
}
