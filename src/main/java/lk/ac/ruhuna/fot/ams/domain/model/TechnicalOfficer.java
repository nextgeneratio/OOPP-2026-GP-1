package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public final class TechnicalOfficer extends User {
    private final int departmentId;

    public TechnicalOfficer(long id, String username, String passwordHash, String email, boolean active, int departmentId) {
        super(id, username, passwordHash, Role.TECHNICAL_OFFICER, email, active);
        this.departmentId = departmentId;
    }

    public int departmentId() { return departmentId; }
}
