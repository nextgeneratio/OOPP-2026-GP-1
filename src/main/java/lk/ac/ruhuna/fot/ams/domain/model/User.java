package lk.ac.ruhuna.fot.ams.domain.model;

import lk.ac.ruhuna.fot.ams.domain.Authenticatable;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.error.exception.ValidationException;

public abstract class User implements Authenticatable {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private String email;
    private boolean active;

    protected User(long id, String username, String passwordHash, Role role, String email, boolean active) {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username is required.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new ValidationException("Password hash is required.");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Enter a valid email address.");
        }
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.email = email;
        this.active = active;
    }

    public long id() { return id; }
    public String username() { return username; }
    public Role role() { return role; }
    public String email() { return email; }
    public void changeEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Enter a valid email address.");
        }
        this.email = email;
    }
    public void deactivate() { active = false; }
    @Override public boolean isActive() { return active; }
    @Override public String passwordHash() { return passwordHash; }
}
