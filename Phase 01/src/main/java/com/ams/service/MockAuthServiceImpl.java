package com.ams.service;

import com.ams.model.User;

import java.util.HashMap;
import java.util.Map;

public class MockAuthServiceImpl implements AuthService {
    
    private final Map<String, User> mockUsers = new HashMap<>();
    
    public MockAuthServiceImpl() {
        // Initialize with some dummy users
        mockUsers.put("admin", new User("1", "admin", "admin123", "Admin", "System Admin"));
        mockUsers.put("lecturer", new User("2", "lecturer", "lecturer123", "Lecturer", "Dr. Jane Smith"));
        mockUsers.put("techofficer", new User("3", "techofficer", "tech123", "Tech Officer", "Mr. John Doe"));
        mockUsers.put("student", new User("4", "student", "student123", "Undergraduate", "Alice Johnson"));
    }

    @Override
    public User login(String username, String password) throws AuthenticationException {
        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new AuthenticationException("Username and password cannot be empty.");
        }

        User user = mockUsers.get(username);
        
        if (user != null && user.getPassword().equals(password)) {
            return user;
        } else {
            throw new AuthenticationException("Invalid username or password.");
        }
    }

    @Override
    public void logout(User user) {
        // Perform logout operations (e.g., clear session tokens if any)
    }
}
