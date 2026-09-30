package lk.ac.ruhuna.fot.ams.domain;

public interface Authenticatable {
    boolean isActive();
    String passwordHash();
}
