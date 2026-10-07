package com.ams.service;

import com.ams.model.User;

public interface AuthService {
    /**
     * Authenticates a user given a username and password.
     * @param username The username
     * @param password The password
     * @return The authenticated User object if successful
     * @throws AuthenticationException if authentication fails
     */
    User login(String username, String password) throws AuthenticationException;
    
    void logout(User user);
}
