package lk.ac.ruhuna.fot.ams.domain.model;

import java.time.LocalDate;
import lk.ac.ruhuna.fot.ams.domain.enums.ApprovalStatus;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class MedicalRecord {
    private final LocalDate medicalDate;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String reason;
    private ApprovalStatus status;

    public MedicalRecord(LocalDate medicalDate, LocalDate startDate, LocalDate endDate, String reason) {
        if (medicalDate == null || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new ValidationException("Enter a valid medical date range.");
        }
        if (medicalDate.isAfter(LocalDate.now())) {
            throw new ValidationException("Medical date cannot be in the future.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Medical reason is required.");
        }
        this.medicalDate = medicalDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = ApprovalStatus.PENDING;
    }

    public ApprovalStatus status() { return status; }
    public void approve() { status = ApprovalStatus.APPROVED; }
    public void reject() { status = ApprovalStatus.REJECTED; }
    public LocalDate medicalDate() { return medicalDate; }
    public LocalDate startDate() { return startDate; }
    public LocalDate endDate() { return endDate; }
    public String reason() { return reason; }
}
