package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;

public interface MedicalDao {
    record MedicalRow(
            int id,
            int studentId,
            Integer offeringId,
            LocalDate medicalDate,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            String evidenceReference,
            ApprovalStatus status,
            boolean affectsEligibility) {
    }

    long insert(Connection connection, MedicalRow row);

    int decide(Connection connection, int medicalId, ApprovalStatus status);

    Optional<MedicalRow> findById(int medicalId);

    List<MedicalRow> findForStudent(int studentId, Integer offeringId);

    List<MedicalRow> findOverlapping(
            int studentId, Integer offeringId, LocalDate startDate, LocalDate endDate);

    List<MedicalRow> findApprovedForRange(
            int studentId, int offeringId, LocalDate startDate, LocalDate endDate);
}
