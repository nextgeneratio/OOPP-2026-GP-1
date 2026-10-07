package com.ams;

import com.ams.controller.AuthController;
import com.ams.model.User;
import com.ams.service.AuthService;
import com.ams.service.MockAuthServiceImpl;
import com.ams.ui.shared.MainFrame;
import com.ams.ui.views.LoginView;
import com.ams.ui.views.dashboard.AdminDashboard;
import com.ams.ui.views.dashboard.LecturerDashboard;
import com.ams.ui.views.dashboard.TechOfficerDashboard;
import com.ams.ui.views.dashboard.UndergraduateDashboard;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;

public class Main implements AuthController.MainApplication {
    
    private MainFrame mainFrame;
    private AuthService authService;
    private User currentUser;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Main().start();
        });
    }
    
    public void start() {
        mainFrame = new MainFrame();
        authService = new MockAuthServiceImpl();
        
        mainFrame.getTopBar().addLogoutListener(e -> logout());
        
        showLoginScreen();
        mainFrame.setVisible(true);
    }
    
    private void showLoginScreen() {
        currentUser = null;
        mainFrame.setTopBarVisible(false);
        mainFrame.setNavigationVisible(false);
        
        LoginView loginView = new LoginView();
        new AuthController(authService, loginView, this);
        
        mainFrame.setContent(loginView);
    }
    
    @Override
    public void onLoginSuccess(User user) {
        this.currentUser = user;
        
        mainFrame.getTopBar().setUserInfo(user.getName(), user.getRole());
        mainFrame.setTopBarVisible(true);
        mainFrame.setNavigationVisible(true);
        
        setupRoleRouting(user);
    }
    
    private void setupRoleRouting(User user) {
        JPanel dashboard;
        java.util.List<String> navOptions;
        
        switch (user.getRole()) {
            case "Admin":
                dashboard = new AdminDashboard();
                navOptions = Arrays.asList("Dashboard", "Users", "Courses", "Timetable", "Reports");
                break;
            case "Lecturer":
                dashboard = new LecturerDashboard();
                navOptions = Arrays.asList("Dashboard", "My Courses", "Attendance", "Marks", "Notices");
                break;
            case "Tech Officer":
                dashboard = new TechOfficerDashboard();
                navOptions = Arrays.asList("Dashboard", "Attendance", "Medical Approvals", "Timetable");
                break;
            case "Undergraduate":
                dashboard = new UndergraduateDashboard();
                navOptions = Arrays.asList("Dashboard", "My Courses", "Eligibility", "Grades", "Timetable");
                break;
            default:
                dashboard = new JPanel();
                dashboard.add(new JLabel("Unknown Role"));
                navOptions = Collections.emptyList();
        }
        
        mainFrame.getNavigationPanel().setMenuOptions(navOptions);
        mainFrame.setContent(dashboard);
    }
    
    private void logout() {
        if (currentUser != null) {
            authService.logout(currentUser);
        }
        showLoginScreen();
    }
}
