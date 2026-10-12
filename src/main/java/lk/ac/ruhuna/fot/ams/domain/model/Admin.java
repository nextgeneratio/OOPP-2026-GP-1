package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.domain.enums.Role;

public final class Admin extends User {
    public Admin(long id, String username, String passwordHash, String email, boolean active) {
        super(id, username, passwordHash, Role.ADMIN, email, active);
    }
}
