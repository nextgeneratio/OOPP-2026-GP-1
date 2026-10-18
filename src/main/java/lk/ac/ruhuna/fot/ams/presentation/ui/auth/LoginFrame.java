package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.api.controller.ApiResponse;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.MainFrame;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class LoginFrame extends JFrame {
    private final AuthController authController;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    public LoginFrame(AuthController authController) {
        super("Academic Management System - Login");
        this.authController = authController;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(8, 8, 8, 8);
        constraints.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Faculty of Technology AMS");
        title.setFont(title.getFont().deriveFont(24f));
        add(panel, title, constraints, 0, 0, 2);
        add(panel, new JLabel("Username"), constraints, 0, 1, 1);
        add(panel, usernameField, constraints, 1, 1, 1);
        add(panel, new JLabel("Password"), constraints, 0, 2, 1);
        add(panel, passwordField, constraints, 1, 2, 1);
        errorLabel.setForeground(UiTheme.ERROR);
        add(panel, errorLabel, constraints, 0, 3, 2);
        JButton loginButton = new AppButton("Login");
        loginButton.addActionListener(event -> login());
        passwordField.addActionListener(event -> login());
        add(panel, loginButton, constraints, 0, 4, 2);
        return panel;
    }

    private void add(JPanel panel, java.awt.Component component, GridBagConstraints template,
                     int gridX, int gridY, int width) {
        GridBagConstraints constraints = (GridBagConstraints) template.clone();
        constraints.gridx = gridX;
        constraints.gridy = gridY;
        constraints.gridwidth = width;
        panel.add(component, constraints);
    }

    private void login() {
        ApiResponse<AuthController.LoginResponse> response = authController.login(
                new AuthController.LoginRequest(usernameField.getText(), passwordField.getPassword()));
        if (!response.successful()) {
            errorLabel.setText(response.error().message());
            return;
        }
        AuthenticatedSession session = response.data().session();
        dispose();
        MainFrame mainFrame = new MainFrame();
        mainFrame.setTitle(mainFrame.getTitle() + " - " + session.role());
        mainFrame.setVisible(true);
    }
}
