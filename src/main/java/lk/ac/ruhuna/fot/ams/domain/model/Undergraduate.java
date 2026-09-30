package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public final class Undergraduate extends User {
    private final int studentId;
    private final int batchId;
    private final String studentNumber;

    public Undergraduate(long id, String username, String passwordHash, String email, boolean active,
                         int studentId, int batchId, String studentNumber) {
        super(id, username, passwordHash, Role.UNDERGRADUATE, email, active);
        if (studentNumber == null || studentNumber.isBlank()) {
            throw new ValidationException("Student number is required.");
        }
        this.studentId = studentId;
        this.batchId = batchId;
        this.studentNumber = studentNumber;
    }

    public int studentId() { return studentId; }
    public int batchId() { return batchId; }
    public String studentNumber() { return studentNumber; }
}
