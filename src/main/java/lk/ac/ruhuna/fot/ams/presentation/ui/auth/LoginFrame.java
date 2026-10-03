package lk.ac.ruhuna.fot.ams.presentation.ui.auth;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import lk.ac.ruhuna.fot.ams.api.controller.AuthController;
import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.controller.PendingDashboardController;
import lk.ac.ruhuna.fot.ams.error.handler.ApplicationErrorHandler;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.AppButton;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.MainFrame;
import lk.ac.ruhuna.fot.ams.presentation.ui.common.UiTheme;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public final class LoginFrame extends JFrame {
    private final AuthController authController;
    private final DashboardController dashboardController;
    private final ApplicationErrorHandler errorHandler;
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel messageLabel = new JLabel(" ");
    private final AppButton loginButton = new AppButton("Login");

    public LoginFrame(AuthController authController, ApplicationErrorHandler errorHandler) {
        this(authController, new PendingDashboardController(), errorHandler);
    }

    public LoginFrame(AuthController authController, DashboardController dashboardController,
                      ApplicationErrorHandler errorHandler) {
        super("Academic Management System - Login");
        this.authController = authController;
        this.dashboardController = dashboardController;
        this.errorHandler = errorHandler;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContent());
        getRootPane().setDefaultButton(loginButton);
        usernameField.addActionListener(event -> passwordField.requestFocusInWindow());
        setMinimumSize(new Dimension(460, 420));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createContent() {
        JPanel page = new JPanel(new GridBagLayout());
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(UiTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG, UiTheme.SPACE_LG)));

        JLabel title = new JLabel("Faculty of Technology AMS");
        title.setFont(UiTheme.pageTitle());
        title.setForeground(UiTheme.PRIMARY);
        JLabel subtitle = new JLabel("Sign in to continue");
        subtitle.setFont(UiTheme.body());
        subtitle.setForeground(UiTheme.MUTED);
        row(card, title, 0, 0);
        row(card, subtitle, 0, 1);
        row(card, label("Username", usernameField), 0, 2);
        row(card, usernameField, 0, 3);
        row(card, label("Password", passwordField), 0, 4);
        row(card, passwordField, 0, 5);
        messageLabel.setFont(UiTheme.helper());
        messageLabel.setForeground(UiTheme.ERROR);
        messageLabel.getAccessibleContext().setAccessibleName("Login message");
        row(card, messageLabel, 0, 6);
        loginButton.addActionListener(event -> login());
        row(card, loginButton, 0, 7);
        usernameField.getAccessibleContext().setAccessibleName("Username");
        passwordField.getAccessibleContext().setAccessibleName("Password");
        page.add(card);
        return page;
    }

    private JLabel label(String text, java.awt.Component target) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.label());
        label.setLabelFor(target);
        return label;
    }

    private void row(JPanel panel, java.awt.Component component, int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(y == 0 ? 0 : UiTheme.SPACE_SM, 0, y == 1 || y == 3 || y == 5 ? UiTheme.SPACE_MD : 0, 0);
        panel.add(component, c);
    }

    private void login() {
        if (usernameField.getText().isBlank() || passwordField.getPassword().length == 0) {
            showMessage("Enter your username and password.", UiTheme.ERROR);
            return;
        }
        loginButton.setEnabled(false);
        showMessage("Signing in\u2026", UiTheme.MUTED);
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();
        passwordField.setText("");
        new SwingWorker<AuthenticatedSession, Void>() {
            @Override protected AuthenticatedSession doInBackground() {
                return authController.login(username, password);
            }

            @Override protected void done() {
                loginButton.setEnabled(true);
                try {
                    openShell(get());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException failure) {
                    errorHandler.log(failure.getCause());
                    showMessage("\u26A0 " + errorHandler.userMessage(failure.getCause()), UiTheme.ERROR);
                    passwordField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void openShell(AuthenticatedSession session) {
        dispose();
        new MainFrame(session, dashboardController, errorHandler,
                () -> new LoginFrame(authController, dashboardController, errorHandler).setVisible(true)).setVisible(true);
    }

    private void showMessage(String text, java.awt.Color color) {
        messageLabel.setForeground(color);
        messageLabel.setText(text);
    }
}
