package lk.ac.ruhuna.fot.ams.data.dao;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import lk.ac.ruhuna.fot.ams.business.service.UserLookup;
import lk.ac.ruhuna.fot.ams.domain.enums.Role;
import lk.ac.ruhuna.fot.ams.domain.model.User;

public interface UserDao extends UserLookup {
    record UserProfile(
            long id,
            String username,
            String passwordHash,
            Role role,
            String email,
            boolean active,
            String fullName,
            String phone,
            Integer departmentId,
            Integer batchId,
            String studentNumber,
            String profilePicture) {
    }

    long create(Connection connection, UserProfile profile);

    int update(Connection connection, UserProfile profile);

    int deactivate(Connection connection, long userId);

    Optional<User> findByUsername(String username);

    Optional<User> findById(long userId);

    List<UserProfile> searchUsers(String term, Role role, Boolean active);

    int updateStaffContact(
            Connection connection, long userId, Role role, String email, String fullName, String phone);

    int updateUndergraduateContact(
            Connection connection, long userId, String email, String profilePicture);
}
