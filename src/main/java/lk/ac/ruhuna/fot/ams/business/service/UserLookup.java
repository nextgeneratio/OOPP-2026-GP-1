package lk.ac.ruhuna.fot.ams.business.service;

import java.util.Optional;
import lk.ac.ruhuna.fot.ams.domain.model.User;

@FunctionalInterface
public interface UserLookup {
    Optional<User> findActiveByUsername(String username);
}
