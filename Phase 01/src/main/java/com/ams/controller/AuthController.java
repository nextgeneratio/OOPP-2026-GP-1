package com.ams.controller;

import com.ams.model.User;
import com.ams.service.AuthService;
import com.ams.service.AuthenticationException;
import com.ams.ui.views.LoginView;
import com.ams.Main;

import javax.swing.*;

public class AuthController {
    
    private final AuthService authService;
    private final LoginView loginView;
    private final MainApplication app; // We need a reference to the main application to handle routing

    public interface MainApplication {
        void onLoginSuccess(User user);
    }

    public AuthController(AuthService authService, LoginView loginView, MainApplication app) {
        this.authService = authService;
        this.loginView = loginView;
        this.app = app;
        
        initController();
    }

    private void initController() {
        loginView.addLoginListener(e -> attemptLogin());
    }

    private void attemptLogin() {
        String username = loginView.getUsername();
        String password = loginView.getPassword();
        
        loginView.setLoading(true);
        loginView.clearError();

        // Run authentication in a separate thread to avoid blocking the EDT
        SwingWorker<User, Void> worker = new SwingWorker<>() {
            @Override
            protected User doInBackground() throws Exception {
                return authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    User user = get();
                    loginView.setLoading(false);
                    app.onLoginSuccess(user);
                } catch (Exception ex) {
                    loginView.setLoading(false);
                    Throwable cause = ex.getCause();
                    if (cause instanceof AuthenticationException) {
                        loginView.showError(cause.getMessage());
                    } else {
                        loginView.showError("An unexpected error occurred. Please try again.");
                    }
                }
            }
        };
        worker.execute();
    }
}
