package lk.ac.ruhuna.fot.ams.business.service;

import java.util.List;
import java.util.Objects;
import lk.ac.ruhuna.fot.ams.domain.enums.EnrollmentStatus;
import lk.ac.ruhuna.fot.ams.domain.enums.StudentStatus;
import lk.ac.ruhuna.fot.ams.domain.valueobject.GradeScheme;
import lk.ac.ruhuna.fot.ams.error.exception.BusinessRuleException;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class EnrollmentService {

    public record EnrollmentRecord(
            int enrollmentId,
            int studentId,
            int offeringId,
            int attemptNo,
            StudentStatus studentStatus,
            EnrollmentStatus enrollmentStatus
    ) {}

    public interface EnrollmentLookup {
        List<EnrollmentRecord> findByStudentAndOffering(int studentId, int offeringId);
        boolean existsActiveEnrollment(int studentId, int offeringId);
    }

    private final EnrollmentLookup enrollmentLookup;

    public EnrollmentService(EnrollmentLookup enrollmentLookup) {
        this.enrollmentLookup = Objects.requireNonNull(enrollmentLookup, "EnrollmentLookup must not be null");
    }

    /**
     * Enrolls a student in a course offering ensuring:
     * - Only one ACTIVE enrollment is allowed per offering (BR-003).
     * - Attempt number increments appropriately for REPEAT or BATCH_MISSED students.
     */
    public EnrollmentRecord enrollStudent(int studentId, int offeringId, StudentStatus status) {
        if (studentId <= 0 || offeringId <= 0) {
            throw new ValidationException("Invalid student ID or course offering ID.");
        }
        if (status == null) {
            throw new ValidationException("Student status cannot be null.");
        }

        if (enrollmentLookup.existsActiveEnrollment(studentId, offeringId)) {
            throw new BusinessRuleException("Student already has an active enrollment in this course offering.");
        }

        List<EnrollmentRecord> priorEnrollments = enrollmentLookup.findByStudentAndOffering(studentId, offeringId);
        int nextAttemptNo = 1;
        if (!priorEnrollments.isEmpty()) {
            int maxAttempt = priorEnrollments.stream()
                    .mapToInt(EnrollmentRecord::attemptNo)
                    .max()
                    .orElse(0);
            nextAttemptNo = maxAttempt + 1;
        }

        if (status == StudentStatus.NORMAL && nextAttemptNo > 1) {
            throw new BusinessRuleException("Subsequent attempts cannot be registered as NORMAL status. Must be REPEAT or BATCH_MISSED.");
        }

        return new EnrollmentRecord(
                0,
                studentId,
                offeringId,
                nextAttemptNo,
                status,
                EnrollmentStatus.ACTIVE
        );
    }

    /**
     * Determines grade for repeat attempt.
     * In Sri Lankan university technology stream regulations (and SRS BR-014), repeat attempts are capped at 'C' (grade point 2.00)
     * unless the attempt was approved with concession (e.g. medical / BATCH_MISSED).
     */
    public GradeScheme capGradeForAttempt(StudentStatus status, int attemptNo, GradeScheme earnedGrade) {
        if (earnedGrade == null) {
            throw new ValidationException("Earned grade cannot be null.");
        }
        if (status == StudentStatus.REPEAT && attemptNo > 1) {
            if (earnedGrade.gradePoint() > 2.00) {
                return GradeScheme.C; // Capped at C
            }
        }
        return earnedGrade;
    }
}
