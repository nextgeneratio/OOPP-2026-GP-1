package com.ams.ui.views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginView extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel errorLabel;
    private JLabel loadingLabel;

    public LoginView() {
        initComponents();
        layoutComponents();
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Login");
        
        // Custom styling for login button
        loginButton.setBackground(new Color(18, 59, 93)); // Primary color
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        errorLabel = new JLabel("");
        errorLabel.setForeground(Color.RED);
        errorLabel.setVisible(false);
        
        loadingLabel = new JLabel("Authenticating...");
        loadingLabel.setVisible(false);

        // Add Enter key listener
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        };
        usernameField.addKeyListener(enterKeyAdapter);
        passwordField.addKeyListener(enterKeyAdapter);
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
        ));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints fgbc = new GridBagConstraints();
        fgbc.fill = GridBagConstraints.HORIZONTAL;
        fgbc.insets = new Insets(10, 10, 10, 10);

        // Title
        fgbc.gridx = 0; fgbc.gridy = 0;
        fgbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("AMS Login");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(titleLabel, fgbc);

        // Error Label
        fgbc.gridy = 1;
        formPanel.add(errorLabel, fgbc);

        // Username
        fgbc.gridy = 2;
        fgbc.gridwidth = 1;
        formPanel.add(new JLabel("Username:"), fgbc);
        
        fgbc.gridx = 1;
        formPanel.add(usernameField, fgbc);

        // Password
        fgbc.gridx = 0; fgbc.gridy = 3;
        formPanel.add(new JLabel("Password:"), fgbc);
        
        fgbc.gridx = 1;
        formPanel.add(passwordField, fgbc);

        // Login Button
        fgbc.gridx = 0; fgbc.gridy = 4;
        fgbc.gridwidth = 2;
        fgbc.insets = new Insets(20, 10, 10, 10);
        formPanel.add(loginButton, fgbc);
        
        // Loading Label
        fgbc.gridy = 5;
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(loadingLabel, fgbc);

        gbc.gridx = 0; gbc.gridy = 0;
        add(formPanel, gbc);
    }

    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public void addLoginListener(ActionListener listener) {
        loginButton.addActionListener(listener);
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        revalidate();
    }

    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        revalidate();
    }

    public void setLoading(boolean isLoading) {
        loadingLabel.setVisible(isLoading);
        loginButton.setEnabled(!isLoading);
        usernameField.setEnabled(!isLoading);
        passwordField.setEnabled(!isLoading);
    }
}
